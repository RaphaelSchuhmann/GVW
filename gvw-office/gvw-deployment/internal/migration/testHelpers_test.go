package migration

import (
	"context"
	"gvw/gvw-deployment/internal/api/client"
	"gvw/gvw-deployment/internal/config"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/log"
	"os"
	"sync"
	"testing"
	"time"
)

// =========
//   MOCKS
// =========

// mockFileSystem implements filesystem.FileSystemWrapper for tests.
type mockFileSystem struct {
	exists    map[string]bool
	files     map[string][]byte
	readErr   map[string]error
	mu        sync.Mutex
	readCalls []string
}

func newMockFileSystem() *mockFileSystem {
	return &mockFileSystem{
		exists:  make(map[string]bool),
		files:   make(map[string][]byte),
		readErr: make(map[string]error),
	}
}

func (m *mockFileSystem) addFile(path string, content []byte) {
	m.files[path] = content
	m.exists[path] = true
}

func (m *mockFileSystem) DirOrFileExists(path string) bool {
	return m.exists[path]
}

func (m *mockFileSystem) CreateDir(path string, permissions os.FileMode) error { return nil }
func (m *mockFileSystem) CleanDir(path string)                                 {}
func (m *mockFileSystem) WriteToFile(path string, content []byte) error        { return nil }

func (m *mockFileSystem) ReadFile(path string) ([]byte, error) {
	m.mu.Lock()
	m.readCalls = append(m.readCalls, path)
	m.mu.Unlock()
	if err, ok := m.readErr[path]; ok {
		return nil, err
	}
	if content, ok := m.files[path]; ok {
		return content, nil
	}
	return nil, os.ErrNotExist
}

func (m *mockFileSystem) CopyFile(src, dst string) error              { return nil }
func (m *mockFileSystem) ListDir(path string) ([]os.DirEntry, error)  { return nil, nil }
func (m *mockFileSystem) CopyDirContents(srcDir, dstDir string) error { return nil }
func (m *mockFileSystem) GetFileByPattern(dir, pattern string) (string, error) {
	return "", nil
}
func (m *mockFileSystem) RemoveFile(path string) error { return nil }
func (m *mockFileSystem) EmptyDir(path string) error   { return nil }

// mockJavaClient implements client.JavaClient and records UpdateDeploymentState calls.
type mockJavaClient struct {
	mu        sync.Mutex
	calls     []deploymentStateCall
	callCh    chan deploymentStateCall
	healthErr error
}

type deploymentStateCall struct {
	deploymentId string
	status       client.Status
	reason       gvwErrors.ErrorCode
}

func newMockJavaClient() *mockJavaClient {
	return &mockJavaClient{callCh: make(chan deploymentStateCall, 16)}
}

func (m *mockJavaClient) UpdateDeploymentState(deploymentId string, status client.Status, reason gvwErrors.ErrorCode, ctx context.Context) error {
	call := deploymentStateCall{deploymentId: deploymentId, status: status, reason: reason}
	m.mu.Lock()
	m.calls = append(m.calls, call)
	m.mu.Unlock()
	select {
	case m.callCh <- call:
	default:
	}
	return nil
}

func (m *mockJavaClient) RunHealthCheck(ctx context.Context) (client.HealthCheckResponse, error) {
	if m.healthErr != nil {
		return client.HealthCheckResponse{}, m.healthErr
	}
	return client.HealthCheckResponse{
		Application: client.Healthy,
		Database:    client.Healthy,
	}, nil
}

func (m *mockJavaClient) waitForCall(t *testing.T, timeout time.Duration) deploymentStateCall {
	t.Helper()
	select {
	case call := <-m.callCh:
		return call
	case <-time.After(timeout):
		t.Fatal("timed out waiting for UpdateDeploymentState call")
		return deploymentStateCall{}
	}
}

// =========
//   HELPERS
// =========

func newTestLogger(t *testing.T) *log.Logger {
	t.Helper()
	return log.NewLogger()
}

// testSchemaJSON is a small schema used across migration tests:
//
//	Event (root, stored in "events-db"): title:string, count:int, active:bool,
//	  recurrence:Recurrence (embedded), tags:array<string>, meta:map<string>
//	Recurrence (embedded): weekDay:string, interval:int
//	Status (enum)
//	Member (root, stored in "members-db"): name:string, age:int
const testSchemaJSON = `{
  "Event": {
    "objType": "class",
    "members": {
      "title": "string",
      "count": "int",
      "active": "bool",
      "score": "float",
      "recurrence": "Recurrence",
      "tags": {"type": "array", "items": "string"},
      "meta": {"type": "object", "values": "string"}
    }
  },
  "Recurrence": {
    "embedded": true,
    "objType": "class",
    "members": {
      "weekDay": "string",
      "interval": "int"
    }
  },
  "Status": {"objType": "enum"},
  "Member": {
    "objType": "class",
    "members": {
      "name": "string",
      "age": "int"
    }
  }
}`

func mustParseSchema(t *testing.T, data string) *Schema {
	t.Helper()
	schema, err := ParseSchema([]byte(data))
	if err != nil {
		t.Fatalf("ParseSchema failed: %v", err)
	}
	return schema
}

func testModelToDbMap() map[string]string {
	return map[string]string{
		"Event":  "events-db",
		"Member": "members-db",
	}
}

func newTestValidator() *MigrationValidator {
	return NewMigrationValidator()
}

func newTestEngine(t *testing.T, fs *mockFileSystem) *MigrationEngine {
	t.Helper()
	return NewMigrationEngine(newTestLogger(t), fs, newTestValidator(), nil)
}

func newTestManager(t *testing.T, fs *mockFileSystem, java *mockJavaClient) *MigrationManager {
	t.Helper()
	logger := newTestLogger(t)
	validator := newTestValidator()
	engine := NewMigrationEngine(logger, fs, validator, nil)
	cfg := &config.ConfigManager{
		Config: config.Config{
			ModelDatabaseMap: testModelToDbMap(),
		},
	}
	return NewMigrationManager(logger, fs, java, engine, cfg, validator, nil)
}

func addMigration(field string) Migration {
	return Migration{
		Id:       "m-add",
		Database: "events-db",
		Action:   MigrationAdd,
		Field:    field,
	}
}

func deleteMigration(field string) Migration {
	return Migration{
		Id:       "m-del",
		Database: "events-db",
		Action:   MigrationDelete,
		Field:    field,
	}
}

func renameMigration(field, value string) Migration {
	return Migration{
		Id:       "m-ren",
		Database: "events-db",
		Action:   MigrationRename,
		Field:    field,
		Value:    value,
	}
}

func conditionalMigration(condField, condValue, trueField, trueValue, falseField, falseValue string, negated bool) Migration {
	return Migration{
		Id:       "m-cond",
		Database: "events-db",
		Action:   MigrationConditional,
		Conditional: ConditionalMigration{
			Condition:   Branch{Field: condField, Value: condValue},
			TrueBranch:  Branch{Field: trueField, Value: trueValue},
			FalseBranch: Branch{Field: falseField, Value: falseValue},
			Negated:     negated,
		},
	}
}
