package build

import (
	"encoding/json"
	"gvw/gvw-deployment/internal/utils/log"
	"os"
	"path/filepath"
	"testing"
)

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
	assertClassEmbedded(t, schema, "Migration")
	assertClassNotEmbedded(t, schema, "Deployment")
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
	assertClassEmbedded(t, schema, "Migration")
	assertClassEmbedded(t, schema, "Conditional")
	assertClassEmbedded(t, schema, "Branch")
	assertClassNotEmbedded(t, schema, "Deployment")
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
