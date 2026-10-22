package build

import (
	"context"
	"gvw/gvw-deployment/internal/utils/log"
	"os"
	"path/filepath"
	"reflect"
	"testing"
)

// =========
//   MOCKS
// =========

// MockJavaClient
type MockJavaClient struct {
	updateDeploymentStateCalled bool
	deploymentId                string
	state                       string
	reason                      uint32
	ctx                         context.Context
}

func (m *MockJavaClient) UpdateDeploymentState(deploymentId string, state string, reason uint32, ctx context.Context) error {
	m.updateDeploymentStateCalled = true
	m.deploymentId = deploymentId
	m.state = state
	m.reason = reason
	m.ctx = ctx
	return nil
}

// MockFileSystemWrapper
type MockFileSystemWrapper struct {
	logger                  *log.Logger
	createDirCalled         bool
	createDirPath           string
	writeToFileCalled       bool
	writeToFilePath         string
	writeToFileContent      []byte
	readFileCalled          bool
	readFilePath            string
	readFileContent         map[string][]byte
	readFileError           error
	listDirCalled           bool
	listDirPath             string
	listDirEntries          []os.DirEntry
	listDirError            error
	dirOrFileExists         bool
	dirOrFileExistsMap      map[string]bool
	cleanDirCalled          bool
	cleanDirPath            string
	copyDirContentsCalled   bool
	copyDirContentsSrc      string
	copyDirContentsDst      string
	copyFileCalled          bool
	copyFileSrc             string
	copyFileDst             string
	getFileByPatternCalled  bool
	getFileByPatternDir     string
	getFileByPatternPattern string
	getFileByPatternResult  string
	getFileByPatternError   error
}

func NewMockFileSystemWrapper(logger *log.Logger) *MockFileSystemWrapper {
	return &MockFileSystemWrapper{
		logger:             logger,
		dirOrFileExists:    true,
		readFileContent:    make(map[string][]byte),
		dirOrFileExistsMap: make(map[string]bool),
	}
}

func (m *MockFileSystemWrapper) DirOrFileExists(path string) bool {
	if val, ok := m.dirOrFileExistsMap[path]; ok {
		return val
	}
	return m.dirOrFileExists
}

func (m *MockFileSystemWrapper) CreateDir(path string, permissions os.FileMode) error {
	m.createDirCalled = true
	m.createDirPath = path
	return nil
}

func (m *MockFileSystemWrapper) CleanDir(path string) {
	m.cleanDirCalled = true
	m.cleanDirPath = path
}

func (m *MockFileSystemWrapper) WriteToFile(path string, content []byte) error {
	m.writeToFileCalled = true
	m.writeToFilePath = path
	m.writeToFileContent = content
	return nil
}

func (m *MockFileSystemWrapper) ReadFile(path string) ([]byte, error) {
	m.readFileCalled = true
	m.readFilePath = path
	if m.readFileError != nil {
		return nil, m.readFileError
	}
	if content, ok := m.readFileContent[path]; ok {
		return content, nil
	}
	return []byte(""), nil
}

func (m *MockFileSystemWrapper) CopyFile(src, dst string) error {
	m.copyFileCalled = true
	m.copyFileSrc = src
	m.copyFileDst = dst
	return nil
}

func (m *MockFileSystemWrapper) ListDir(path string) ([]os.DirEntry, error) {
	m.listDirCalled = true
	m.listDirPath = path
	if m.listDirError != nil {
		return nil, m.listDirError
	}
	return m.listDirEntries, nil
}

func (m *MockFileSystemWrapper) CopyDirContents(srcDir, dstDir string) error {
	m.copyDirContentsCalled = true
	m.copyDirContentsSrc = srcDir
	m.copyDirContentsDst = dstDir
	return nil
}

func (m *MockFileSystemWrapper) GetFileByPattern(dir, filePattern string) (string, error) {
	m.getFileByPatternCalled = true
	m.getFileByPatternDir = dir
	m.getFileByPatternPattern = filePattern
	if m.getFileByPatternError != nil {
		return "", m.getFileByPatternError
	}
	return m.getFileByPatternResult, nil
}

// MockDirEntry
type MockDirEntry struct {
	name  string
	isDir bool
}

func (m *MockDirEntry) Name() string {
	return m.name
}

func (m *MockDirEntry) IsDir() bool {
	return m.isDir
}

func (m *MockDirEntry) Type() os.FileMode {
	return 0
}

func (m *MockDirEntry) Info() (os.FileInfo, error) {
	return nil, nil
}

// ===========
//   HELPERS
// ===========

func setupTestLoader() (*log.Logger, *MockFileSystemWrapper, *ModelLoader) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)
	return logger, fsWrapper, loader
}

func setupBuildManagerTest() (*log.Logger, *MockFileSystemWrapper, *MockJavaClient, *ModelLoader) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	mockJavaClient := &MockJavaClient{}
	modelLoader := NewModelLoader(logger, fsWrapper)
	return logger, fsWrapper, mockJavaClient, modelLoader
}

func setupModelFile(fsWrapper *MockFileSystemWrapper, filename, content string) string {
	workingDir := "./builds/test-deployment/repo/gvw-office/gvw-backend/src/main/java/com/gvw/gvwbackend/model"
	fsWrapper.dirOrFileExistsMap[workingDir] = true
	filePath := filepath.Join(workingDir, filename)
	fsWrapper.readFileContent[filePath] = []byte(content)
	fsWrapper.dirOrFileExistsMap[filePath] = true
	return filePath
}

// Schema Builder Helpers

func createClassSchema(objType string, members map[string]any) map[string]any {
	if objType == "enum" {
		return map[string]any{"objType": objType}
	}
	return map[string]any{
		"objType": objType,
		"members": members,
	}
}

func createFieldSchema(fieldType string) map[string]any {
	return map[string]any{
		"type":  "array",
		"items": fieldType,
	}
}

func createMapSchema(valueType string) map[string]any {
	return map[string]any{
		"type":   "object",
		"values": valueType,
	}
}

// Assertion Helpers

func getClassMap(t *testing.T, schema map[string]any, className string) map[string]any {
	t.Helper()
	classMap, ok := schema[className].(map[string]any)
	if !ok {
		t.Fatalf("Class '%s' not found in schema", className)
	}
	return classMap
}

func getFieldMap(t *testing.T, classMap map[string]any, fieldName string) map[string]any {
	t.Helper()
	members, ok := classMap["members"].(map[string]any)
	if !ok {
		t.Fatalf("Class members map not found")
	}
	fieldMap, ok := members[fieldName].(map[string]any)
	if !ok {
		t.Fatalf("Field '%s' not found or is not a map", fieldName)
	}
	return fieldMap
}

func assertClassExists(t *testing.T, schema map[string]any, className string) map[string]any {
	t.Helper()
	class, ok := schema[className].(map[string]any)
	if !ok {
		t.Fatalf("Class '%s' not found or not a map", className)
	}
	return class
}

func assertEmbedded(t *testing.T, classMap map[string]any, expected bool) {
	t.Helper()
	if classMap["embedded"] != expected {
		t.Errorf("Expected embedded to be %v, got %v", expected, classMap["embedded"])
	}
}

func assertClassEmbedded(t *testing.T, schema map[string]any, className string) {
	t.Helper()
	class := assertClassExists(t, schema, className)
	assertEmbedded(t, class, true)
}

func assertClassNotEmbedded(t *testing.T, schema map[string]any, className string) {
	t.Helper()
	class := assertClassExists(t, schema, className)
	assertEmbedded(t, class, false)
}

func assertSchemaField(t *testing.T, schema map[string]any, className, fieldName string, expectedType any) {
	t.Helper()

	class := assertClassExists(t, schema, className)
	members, ok := class["members"].(map[string]any)
	if !ok {
		t.Fatalf("Class '%s' has no members", className)
	}

	actual := members[fieldName]
	if !reflect.DeepEqual(actual, expectedType) {
		t.Errorf("Expected field '%s' to be '%v', got '%v'", fieldName, expectedType, actual)
	}
}
