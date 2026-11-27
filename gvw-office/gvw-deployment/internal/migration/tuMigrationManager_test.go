package migration

import (
	"gvw/gvw-deployment/internal/api/client"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"testing"
	"time"
)

// =========
// StartMigration
// =========

func TestStartMigration_InvalidDeploymentId(t *testing.T) {
	m := newTestManager(t, newMockFileSystem(), newMockJavaClient())

	invalidIds := []string{"", "has space", "has/slash", "has.dot", "../escape", string(make([]byte, 100))}
	for _, id := range invalidIds {
		_, err := m.StartMigration(id, []Migration{addMigration("title")})
		if err == nil {
			t.Errorf("expected error for invalid deployment id %q", id)
			continue
		}
		if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationManagerInvalidDeploymentId) {
			t.Errorf("expected InvalidDeploymentId code for %q, got %v", id, err)
		}
	}
}

func TestStartMigration_ValidIdFormat(t *testing.T) {
	m := newTestManager(t, newMockFileSystem(), newMockJavaClient())

	// These pass the id pattern; with no migrations the pipeline returns early
	// before touching the filesystem or database.
	for _, id := range []string{"dep-1", "DEP_123", "a", "A-b_c-9"} {
		backups, err := m.StartMigration(id, nil)
		if err != nil {
			t.Errorf("expected no error for id %q with no migrations, got %v", id, err)
		}
		if backups != nil {
			t.Errorf("expected nil backups for id %q, got %v", id, backups)
		}
	}
}

func TestStartMigration_NoMigrations_ReturnsEarly(t *testing.T) {
	fs := newMockFileSystem()
	java := newMockJavaClient()
	m := newTestManager(t, fs, java)

	backups, err := m.StartMigration("dep-1", []Migration{})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if backups != nil {
		t.Errorf("expected nil backups, got %v", backups)
	}
	// No schema file was provided, but the pipeline must have returned before reading it
	if len(fs.readCalls) != 0 {
		t.Errorf("expected no file reads for empty migrations, got %v", fs.readCalls)
	}
	// DefaultMigration mode does not report to the java client
	select {
	case call := <-java.callCh:
		t.Errorf("expected no java client call, got %+v", call)
	case <-time.After(50 * time.Millisecond):
	}
}

// =========
// TriggerMigrationTest (dry run)
// =========

func TestTriggerMigrationTest_NoMigrations_ReportsSuccess(t *testing.T) {
	fs := newMockFileSystem()
	java := newMockJavaClient()
	m := newTestManager(t, fs, java)

	m.TriggerMigrationTest("dep-1", nil)

	call := java.waitForCall(t, 2*time.Second)
	if call.deploymentId != "dep-1" {
		t.Errorf("deploymentId = %q, want dep-1", call.deploymentId)
	}
	if call.status != client.StatusSuccess {
		t.Errorf("status = %q, want %q", call.status, client.StatusSuccess)
	}
	if call.reason != gvwErrors.MIGRATION_CodeSuccess {
		t.Errorf("reason = %v, want MIGRATION_CodeSuccess", call.reason)
	}
}

func TestTriggerMigrationTest_MissingSchema_ReportsBadSchema(t *testing.T) {
	fs := newMockFileSystem() // no schema file
	java := newMockJavaClient()
	m := newTestManager(t, fs, java)

	m.TriggerMigrationTest("dep-1", []Migration{addMigration("title")})

	call := java.waitForCall(t, 2*time.Second)
	if call.status != client.StatusCancelled {
		t.Errorf("status = %q, want %q", call.status, client.StatusCancelled)
	}
	if call.reason != gvwErrors.MIGRATION_CodeBadSchema {
		t.Errorf("reason = %v, want MIGRATION_CodeBadSchema", call.reason)
	}
}

func TestTriggerMigrationTest_SchemaPresent_BadMigration_ReportsBadMigration(t *testing.T) {
	fs := newMockFileSystem()
	fs.addFile("./builds/dep-1/schema.json", []byte(testSchemaJSON))
	java := newMockJavaClient()
	m := newTestManager(t, fs, java)

	// Migration references a field that does not exist in the schema;
	// planning fails inside executeDryRunPipeline -> BadMigrationFound
	m.TriggerMigrationTest("dep-1", []Migration{addMigration("Event.bogus")})

	call := java.waitForCall(t, 2*time.Second)
	if call.status != client.StatusCancelled {
		t.Errorf("status = %q, want %q", call.status, client.StatusCancelled)
	}
	if call.reason != gvwErrors.MIGRATION_CodeBadMigrationFound {
		t.Errorf("reason = %v, want MIGRATION_CodeBadMigrationFound", call.reason)
	}
}

func TestTriggerMigrationTest_SecondCallCancelsFirst(t *testing.T) {
	fs := newMockFileSystem()
	java := newMockJavaClient()
	m := newTestManager(t, fs, java)

	// Trigger twice quickly; the first run is cancelled/superseded.
	// Both eventually report (cancelled context -> early return may skip reporting),
	// so we only assert the manager does not deadlock or panic.
	m.TriggerMigrationTest("dep-1", nil)
	m.TriggerMigrationTest("dep-1", nil)

	// At least one success report must arrive
	call := java.waitForCall(t, 2*time.Second)
	if call.status != client.StatusSuccess {
		t.Errorf("status = %q, want %q", call.status, client.StatusSuccess)
	}
}

// =========
// RestoreDatabasesFromBackup / ClearDatabaseBackups
// =========

// These methods require a real *client.DbClient (concrete struct, not an interface),
// so they are covered by integration tests rather than unit tests.
