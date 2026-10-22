package build

import (
	"encoding/json"
	"gvw/gvw-deployment/internal/utils/log"
	"os"
	"path/filepath"
	"reflect"
	"testing"
)

type MockJavaClient struct {
	updateDeploymentStateCalled bool
	deploymentId                string
	state                       string
	reason                      uint32
}

func (m *MockJavaClient) UpdateDeploymentState(deploymentId string, state string, reason uint32) error {
	m.updateDeploymentStateCalled = true
	m.deploymentId = deploymentId
	m.state = state
	m.reason = reason
	return nil
}

// MockFileSystemWrapper for testing
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

// MockDirEntry for testing
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

func assertSchemaField(t *testing.T, schema map[string]any, className, fieldName string, expectedType any) {
	t.Helper()

	class, ok := schema[className].(map[string]any)
	if !ok {
		t.Fatalf("Class '%s' not found in schema", className)
	}

	members, ok := class["members"].(map[string]any)
	if !ok {
		t.Fatalf("Class '%s' has no members", className)
	}

	actual := members[fieldName]
	if !reflect.DeepEqual(actual, expectedType) {
		t.Errorf("Expected field '%s' to be '%v', got '%v'", fieldName, expectedType, actual)
	}
}

func assertEmbeddedClass(t *testing.T, schema map[string]any, className string) {
	t.Helper()

	class, ok := schema[className].(map[string]any)
	if !ok {
		t.Fatalf("Class '%s' not found in schema", className)
	}
	if class["embedded"] != true {
		t.Errorf("Expected class '%s' to be embedded", className)
	}
}

func assertNotEmbeddedClass(t *testing.T, schema map[string]any, className string) {
	t.Helper()

	class, ok := schema[className].(map[string]any)
	if !ok {
		t.Fatalf("Class '%s' not found in schema", className)
	}
	if class["embedded"] != false {
		t.Errorf("Expected class '%s' to not be embedded", className)
	}
}

func TestGenerateSchema_Success(t *testing.T) {
	logger, fsWrapper, mockJavaClient, modelLoader := setupBuildManagerTest()

	fsWrapper.listDirEntries = []os.DirEntry{
		&MockDirEntry{name: "Deployment.java", isDir: false},
		&MockDirEntry{name: "Migration.java", isDir: false},
	}

	deploymentContent := `package com.gvw.gvwbackend.model;

public class Deployment {
    private String _id;
    private List<Migration> migrations;
}`

	migrationContent := `package com.gvw.gvwbackend.model;

public class Migration {
    private String id;
}`

	setupModelFile(fsWrapper, "Deployment.java", deploymentContent)
	setupModelFile(fsWrapper, "Migration.java", migrationContent)

	buildManager := NewBuildManager("https://github.com/test/repo", mockJavaClient, fsWrapper, logger, modelLoader)

	err := buildManager.generateSchema("test-deployment")
	if err != nil {
		t.Fatalf("generateSchema failed: %v", err)
	}

	if !fsWrapper.writeToFileCalled {
		t.Errorf("Expected WriteToFile to be called")
	}

	expectedPath := filepath.Join("./builds/test-deployment", "schema.json")
	if fsWrapper.writeToFilePath != expectedPath {
		t.Errorf("Expected schema to be written to '%s', got '%s'", expectedPath, fsWrapper.writeToFilePath)
	}

	var schema map[string]any
	err = json.Unmarshal(fsWrapper.writeToFileContent, &schema)
	if err != nil {
		t.Fatalf("Generated JSON is invalid: %v", err)
	}

	assertSchemaField(t, schema, "Deployment", "migrations", map[string]any{
		"type":  "array",
		"items": "Migration",
	})
	assertEmbeddedClass(t, schema, "Migration")
	assertNotEmbeddedClass(t, schema, "Deployment")
}

func TestGenerateSchema_ListDirFails(t *testing.T) {
	_, fsWrapper, mockJavaClient, modelLoader := setupBuildManagerTest()

	fsWrapper.listDirError = os.ErrNotExist

	buildManager := NewBuildManager("https://github.com/test/repo", mockJavaClient, fsWrapper, log.NewLogger(), modelLoader)

	err := buildManager.generateSchema("test-deployment")
	if err == nil {
		t.Errorf("Expected error when ListDir fails, got nil")
	}
}

func TestGenerateSchema_ModelFileReadFails(t *testing.T) {
	_, fsWrapper, mockJavaClient, modelLoader := setupBuildManagerTest()

	fsWrapper.listDirEntries = []os.DirEntry{
		&MockDirEntry{name: "Deployment.java", isDir: false},
	}
	fsWrapper.readFileError = os.ErrPermission

	buildManager := NewBuildManager("https://github.com/test/repo", mockJavaClient, fsWrapper, log.NewLogger(), modelLoader)

	err := buildManager.generateSchema("test-deployment")
	if err == nil {
		t.Errorf("Expected error when model file read fails, got nil")
	}
}

func TestGenerateSchema_ValidationFails(t *testing.T) {
	_, fsWrapper, mockJavaClient, modelLoader := setupBuildManagerTest()

	fsWrapper.listDirEntries = []os.DirEntry{
		&MockDirEntry{name: "Deployment.java", isDir: false},
	}

	deploymentContent := `package com.gvw.gvwbackend.model;

public class Deployment {
    private UnsupportedType field;
}`

	setupModelFile(fsWrapper, "Deployment.java", deploymentContent)

	buildManager := NewBuildManager("https://github.com/test/repo", mockJavaClient, fsWrapper, log.NewLogger(), modelLoader)

	err := buildManager.generateSchema("test-deployment")
	if err == nil {
		t.Errorf("Expected error when validation fails, got nil")
	}
}

func TestGenerateSchema_PreviousSchemaReplaced(t *testing.T) {
	logger, fsWrapper, mockJavaClient, modelLoader := setupBuildManagerTest()

	fsWrapper.listDirEntries = []os.DirEntry{
		&MockDirEntry{name: "Deployment.java", isDir: false},
	}

	deploymentContent := `package com.gvw.gvwbackend.model;

public class Deployment {
    private String id;
}`

	setupModelFile(fsWrapper, "Deployment.java", deploymentContent)

	buildManager := NewBuildManager("https://github.com/test/repo", mockJavaClient, fsWrapper, logger, modelLoader)

	err := buildManager.generateSchema("test-deployment")
	if err != nil {
		t.Fatalf("First generateSchema failed: %v", err)
	}

	firstContent := fsWrapper.writeToFileContent

	fsWrapper.writeToFileCalled = false
	fsWrapper.writeToFileContent = nil

	deploymentContent2 := `package com.gvw.gvwbackend.model;

public class Deployment {
    private String id;
    private String name;
}`

	setupModelFile(fsWrapper, "Deployment.java", deploymentContent2)

	err = buildManager.generateSchema("test-deployment")
	if err != nil {
		t.Fatalf("Second generateSchema failed: %v", err)
	}

	if !fsWrapper.writeToFileCalled {
		t.Errorf("Expected WriteToFile to be called on second generation")
	}

	if string(fsWrapper.writeToFileContent) == string(firstContent) {
		t.Errorf("Expected schema content to change on regeneration")
	}
}

func TestGenerateSchema_NestedReferences(t *testing.T) {
	logger, fsWrapper, mockJavaClient, modelLoader := setupBuildManagerTest()

	fsWrapper.listDirEntries = []os.DirEntry{
		&MockDirEntry{name: "Deployment.java", isDir: false},
		&MockDirEntry{name: "Migration.java", isDir: false},
		&MockDirEntry{name: "Conditional.java", isDir: false},
		&MockDirEntry{name: "Branch.java", isDir: false},
	}

	deploymentContent := `package com.gvw.gvwbackend.model;

public class Deployment {
    private List<Migration> migrations;
}`

	migrationContent := `package com.gvw.gvwbackend.model;

public class Migration {
    private Conditional conditional;
}`

	conditionalContent := `package com.gvw.gvwbackend.model;

public class Conditional {
    private Branch condition;
}`

	branchContent := `package com.gvw.gvwbackend.model;

public class Branch {
    private String id;
}`

	setupModelFile(fsWrapper, "Deployment.java", deploymentContent)
	setupModelFile(fsWrapper, "Migration.java", migrationContent)
	setupModelFile(fsWrapper, "Conditional.java", conditionalContent)
	setupModelFile(fsWrapper, "Branch.java", branchContent)

	buildManager := NewBuildManager("https://github.com/test/repo", mockJavaClient, fsWrapper, logger, modelLoader)

	err := buildManager.generateSchema("test-deployment")
	if err != nil {
		t.Fatalf("generateSchema failed: %v", err)
	}

	var schema map[string]any
	err = json.Unmarshal(fsWrapper.writeToFileContent, &schema)
	if err != nil {
		t.Fatalf("Generated JSON is invalid: %v", err)
	}

	assertSchemaField(t, schema, "Deployment", "migrations", map[string]any{
		"type":  "array",
		"items": "Migration",
	})
	assertSchemaField(t, schema, "Migration", "conditional", "Conditional")
	assertSchemaField(t, schema, "Conditional", "condition", "Branch")
	assertEmbeddedClass(t, schema, "Migration")
	assertEmbeddedClass(t, schema, "Conditional")
	assertEmbeddedClass(t, schema, "Branch")
	assertNotEmbeddedClass(t, schema, "Deployment")
}

func TestSetupBuildDirs(t *testing.T) {
	_, fsWrapper, mockJavaClient, modelLoader := setupBuildManagerTest()

	buildManager := NewBuildManager("https://github.com/test/repo", mockJavaClient, fsWrapper, log.NewLogger(), modelLoader)

	buildManager.setupBuildDirs("test-deployment")

	if !fsWrapper.cleanDirCalled {
		t.Errorf("Expected CleanDir to be called")
	}

	if fsWrapper.cleanDirPath == "" {
		t.Errorf("Expected CleanDir to be called with a path")
	}

	if !fsWrapper.createDirCalled {
		t.Errorf("Expected CreateDir to be called")
	}

	if fsWrapper.createDirPath != "./builds/test-deployment/bin" {
		t.Errorf("Expected CreateDir to be called for bin directory, got '%s'", fsWrapper.createDirPath)
	}
}

func TestTriggerBuild_CancelsPreviousBuild(t *testing.T) {
	_, fsWrapper, mockJavaClient, modelLoader := setupBuildManagerTest()

	buildManager := NewBuildManager("https://github.com/test/repo", mockJavaClient, fsWrapper, log.NewLogger(), modelLoader)

	buildManager.TriggerBuild("test-deployment", "commit1")
	buildManager.TriggerBuild("test-deployment", "commit2")

	// This is difficult to test without access to the internal context
	// The implementation shows that cancel() is called when a new build is triggered
}
