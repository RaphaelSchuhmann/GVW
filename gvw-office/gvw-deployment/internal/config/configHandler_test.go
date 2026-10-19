package config

import (
	"encoding/json"
	"gvw/gvw-deployment/internal/utils"
	"os"
	"reflect"
	"testing"
)

func TestValidateConfig_ValidConfig(t *testing.T) {
	config := Config{
		Port:          "8080",
		InternalToken: "test-token",
		JavaBaseURL:   "http://localhost:8080",
		GithubRepoURL: "https://github.com/test/repo",
		ModelDatabaseMap: map[string]string{
			"model1": "db1",
		},
	}

	err := ValidateConfig(config)
	if err != nil {
		t.Errorf("Expected no error for valid config, got: %v", err)
	}
}

func TestValidateConfig_EmptyPort(t *testing.T) {
	config := Config{
		Port:          "",
		InternalToken: "test-token",
		JavaBaseURL:   "http://localhost:8080",
		GithubRepoURL: "https://github.com/test/repo",
		ModelDatabaseMap: map[string]string{
			"model1": "db1",
		},
	}

	err := ValidateConfig(config)
	if err == nil {
		t.Error("Expected error for empty port")
	}
}

func TestValidateConfig_EmptyInternalToken(t *testing.T) {
	config := Config{
		Port:          "8080",
		InternalToken: "",
		JavaBaseURL:   "http://localhost:8080",
		GithubRepoURL: "https://github.com/test/repo",
		ModelDatabaseMap: map[string]string{
			"model1": "db1",
		},
	}

	err := ValidateConfig(config)
	if err == nil {
		t.Error("Expected error for empty internal token")
	}
}

func TestValidateConfig_EmptyJavaBaseURL(t *testing.T) {
	config := Config{
		Port:          "8080",
		InternalToken: "test-token",
		JavaBaseURL:   "",
		GithubRepoURL: "https://github.com/test/repo",
		ModelDatabaseMap: map[string]string{
			"model1": "db1",
		},
	}

	err := ValidateConfig(config)
	if err == nil {
		t.Error("Expected error for empty Java base URL")
	}
}

func TestValidateConfig_EmptyGithubRepoURL(t *testing.T) {
	config := Config{
		Port:          "8080",
		InternalToken: "test-token",
		JavaBaseURL:   "http://localhost:8080",
		GithubRepoURL: "",
		ModelDatabaseMap: map[string]string{
			"model1": "db1",
		},
	}

	err := ValidateConfig(config)
	if err == nil {
		t.Error("Expected error for empty GitHub repo URL")
	}
}

func TestValidateConfig_EmptyModelDatabaseMap(t *testing.T) {
	config := Config{
		Port:             "8080",
		InternalToken:    "test-token",
		JavaBaseURL:      "http://localhost:8080",
		GithubRepoURL:    "https://github.com/test/repo",
		ModelDatabaseMap: map[string]string{},
	}

	err := ValidateConfig(config)
	if err == nil {
		t.Error("Expected error for empty model database map")
	}
}

func TestValidateConfig_NilModelDatabaseMap(t *testing.T) {
	config := Config{
		Port:             "8080",
		InternalToken:    "test-token",
		JavaBaseURL:      "http://localhost:8080",
		GithubRepoURL:    "https://github.com/test/repo",
		ModelDatabaseMap: nil,
	}

	err := ValidateConfig(config)
	if err == nil {
		t.Error("Expected error for nil model database map")
	}
}

func TestIsConfigValueInvalid_ZeroString(t *testing.T) {
	type TestStruct struct {
		Field1 string
		Field2 string
	}

	val := TestStruct{Field1: "value", Field2: ""}
	result := IsConfigValueInvalid(reflect.ValueOf(val))

	if !result {
		t.Error("Expected true for struct with zero string field")
	}
}

func TestIsConfigValueInvalid_AllNonZero(t *testing.T) {
	type TestStruct struct {
		Field1 string
		Field2 int
	}

	val := TestStruct{Field1: "value", Field2: 42}
	result := IsConfigValueInvalid(reflect.ValueOf(val))

	if result {
		t.Error("Expected false for struct with all non-zero fields")
	}
}

func TestIsConfigValueInvalid_NilPointer(t *testing.T) {
	type TestStruct struct {
		Field1 *string
	}

	val := TestStruct{Field1: nil}
	result := IsConfigValueInvalid(reflect.ValueOf(val).Field(0))

	if !result {
		t.Error("Expected true for nil pointer")
	}
}

func TestIsConfigValueInvalid_NonStruct(t *testing.T) {
	val := "test string"
	result := IsConfigValueInvalid(reflect.ValueOf(val))

	if result {
		t.Error("Expected false for non-struct type")
	}
}

func TestLoadConfig_ValidFile(t *testing.T) {
	// Create a temporary config file
	configData := Config{
		Port:          "8080",
		InternalToken: "test-token",
		JavaBaseURL:   "http://localhost:8080",
		GithubRepoURL: "https://github.com/test/repo",
		ModelDatabaseMap: map[string]string{
			"model1": "db1",
		},
	}

	jsonData, _ := json.Marshal(configData)
	tmpFile, err := os.CreateTemp("", "config-*.json")
	if err != nil {
		t.Fatalf("Failed to create temp file: %v", err)
	}
	defer os.Remove(tmpFile.Name())

	tmpFile.Write(jsonData)
	tmpFile.Close()

	logger := utils.NewLogger()
	filesystemWrapper := utils.NewFileSystem(logger)

	config, err := LoadConfig(tmpFile.Name(), filesystemWrapper)
	if err != nil {
		t.Errorf("Expected no error loading valid config, got: %v", err)
	}

	if config.Port != "8080" {
		t.Errorf("Expected port 8080, got: %s", config.Port)
	}
}

func TestLoadConfig_InvalidJSON(t *testing.T) {
	// Create a temporary file with invalid JSON
	tmpFile, err := os.CreateTemp("", "config-*.json")
	if err != nil {
		t.Fatalf("Failed to create temp file: %v", err)
	}
	defer os.Remove(tmpFile.Name())

	tmpFile.WriteString("{invalid json}")
	tmpFile.Close()

	logger := utils.NewLogger()
	filesystemWrapper := utils.NewFileSystem(logger)

	_, err = LoadConfig(tmpFile.Name(), filesystemWrapper)
	if err == nil {
		t.Error("Expected error for invalid JSON")
	}
}

func TestLoadConfig_FileNotFound(t *testing.T) {
	logger := utils.NewLogger()
	filesystemWrapper := utils.NewFileSystem(logger)

	_, err := LoadConfig("/nonexistent/path/config.json", filesystemWrapper)
	if err == nil {
		t.Error("Expected error for non-existent file")
	}
}

func TestLoadConfig_InvalidConfigValues(t *testing.T) {
	// Create a config file with missing required fields
	configData := map[string]interface{}{
		"port": "8080",
		// Missing other required fields
	}

	jsonData, _ := json.Marshal(configData)
	tmpFile, err := os.CreateTemp("", "config-*.json")
	if err != nil {
		t.Fatalf("Failed to create temp file: %v", err)
	}
	defer os.Remove(tmpFile.Name())

	tmpFile.Write(jsonData)
	tmpFile.Close()

	logger := utils.NewLogger()
	filesystemWrapper := utils.NewFileSystem(logger)

	_, err = LoadConfig(tmpFile.Name(), filesystemWrapper)
	if err == nil {
		t.Error("Expected error for invalid config values")
	}
}
