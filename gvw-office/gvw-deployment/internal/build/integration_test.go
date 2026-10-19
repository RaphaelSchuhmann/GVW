package build

import (
	"encoding/json"
	"gvw/gvw-deployment/internal/utils"
	"os"
	"path/filepath"
	"testing"
)

// TestIntegration_RealModelSources runs the generator against the actual backend model sources
// This is an integration test that verifies the real-world Deployment/Migration/Conditional/Branch chain
func TestIntegration_RealModelSources(t *testing.T) {
	// Skip if the backend model directory doesn't exist (e.g., running in a different environment)
	modelDir := "../gvw-backend/src/main/java/com/gvw/gvwbackend/model"
	if _, err := os.Stat(modelDir); os.IsNotExist(err) {
		t.Skip("Backend model directory not found, skipping integration test")
	}

	logger := utils.NewLogger()
	fsWrapper := utils.NewFileSystem(logger)
	modelLoader := NewModelLoader(logger, fsWrapper)

	// Load all model files from the real backend
	entries, err := fsWrapper.ListDir(modelDir)
	if err != nil {
		t.Fatalf("Failed to list model directory: %v", err)
	}

	rawSchema := make(map[string]any)
	for _, entry := range entries {
		if entry.IsDir() {
			continue
		}

		modelPath := filepath.Join(modelDir, entry.Name())
		modelMap, err := modelLoader.LoadModel(modelPath)
		if err != nil {
			t.Logf("Warning: Failed to load model %s: %v", entry.Name(), err)
			continue
		}

		for k, v := range modelMap {
			rawSchema[k] = v
		}
	}

	// Normalize and validate
	normalizedSchema, err := modelLoader.NormalizeAndValidateSchema(rawSchema)
	if err != nil {
		t.Fatalf("Failed to normalize schema: %v", err)
	}

	// Mark embedded classes
	modelLoader.MarkEmbeddedClasses(normalizedSchema)

	// Verify Deployment exists
	deployment, ok := normalizedSchema["Deployment"].(map[string]any)
	if !ok {
		t.Fatalf("Deployment not found in schema")
	}

	// Verify Deployment.members.migrations exists and is an array
	deploymentMembers := deployment["members"].(map[string]any)
	migrations, ok := deploymentMembers["migrations"].(map[string]any)
	if !ok {
		t.Fatalf("Deployment.members.migrations not found or not an array")
	}

	if migrations["type"] != "array" {
		t.Errorf("Expected migrations type 'array', got '%v'", migrations["type"])
	}

	// Verify its items reference is Migration
	if migrations["items"] != "Migration" {
		t.Errorf("Expected migrations items to reference Migration, got '%v'", migrations["items"])
	}

	// Verify Migration exists
	migration, ok := normalizedSchema["Migration"].(map[string]any)
	if !ok {
		t.Fatalf("Migration not found in schema")
	}

	// Verify Migration is marked embedded
	if migration["embedded"] != true {
		t.Errorf("Expected Migration to be marked embedded, got '%v'", migration["embedded"])
	}

	// Verify Migration.members.conditional references Conditional
	migrationMembers := migration["members"].(map[string]any)
	conditionalRef, ok := migrationMembers["conditional"].(string)
	if !ok {
		t.Fatalf("Migration.members.conditional not found or not a string")
	}

	if conditionalRef != "Conditional" {
		t.Errorf("Expected Migration.members.conditional to reference Conditional, got '%s'", conditionalRef)
	}

	// Verify Conditional exists
	conditional, ok := normalizedSchema["Conditional"].(map[string]any)
	if !ok {
		t.Fatalf("Conditional not found in schema")
	}

	// Verify Conditional is marked embedded
	if conditional["embedded"] != true {
		t.Errorf("Expected Conditional to be marked embedded, got '%v'", conditional["embedded"])
	}

	// Verify Conditional.members.condition, truePath, and falsePath reference Branch
	conditionalMembers := conditional["members"].(map[string]any)

	conditionRef, ok := conditionalMembers["condition"].(string)
	if !ok {
		t.Fatalf("Conditional.members.condition not found or not a string")
	}
	if conditionRef != "Branch" {
		t.Errorf("Expected Conditional.members.condition to reference Branch, got '%s'", conditionRef)
	}

	truePathRef, ok := conditionalMembers["truePath"].(string)
	if ok && truePathRef != "Branch" {
		t.Errorf("Expected Conditional.members.truePath to reference Branch, got '%s'", truePathRef)
	}

	falsePathRef, ok := conditionalMembers["falsePath"].(string)
	if ok && falsePathRef != "Branch" {
		t.Errorf("Expected Conditional.members.falsePath to reference Branch, got '%s'", falsePathRef)
	}

	// Verify Branch exists
	branch, ok := normalizedSchema["Branch"].(map[string]any)
	if !ok {
		t.Fatalf("Branch not found in schema")
	}

	// Verify Branch is marked embedded
	if branch["embedded"] != true {
		t.Errorf("Expected Branch to be marked embedded, got '%v'", branch["embedded"])
	}

	// Verify Deployment remains non-embedded
	if deployment["embedded"] != false {
		t.Errorf("Expected Deployment to not be embedded, got '%v'", deployment["embedded"])
	}

	// Verify no enum definitions appear as standalone schema entries
	for className, classData := range normalizedSchema {
		if classMap, ok := classData.(map[string]any); ok {
			if objType, ok := classMap["objType"].(string); ok && objType == "enum" {
				t.Errorf("Enum %s should not appear in normalized schema", className)
			}
		}
	}

	// Verify the schema can be encoded to JSON without errors
	var buf []byte
	buf, err = json.MarshalIndent(normalizedSchema, "", "  ")
	if err != nil {
		t.Fatalf("Failed to encode schema to JSON: %v", err)
	}

	// Verify the JSON can be decoded back
	var decodedSchema map[string]any
	err = json.Unmarshal(buf, &decodedSchema)
	if err != nil {
		t.Fatalf("Failed to decode JSON back to schema: %v", err)
	}

	// Verify there are no unresolved model references
	// This is implicitly verified by the NormalizeAndValidateSchema step which would fail
	// if any unknown types were referenced

	t.Logf("Integration test passed. Schema contains %d model classes", len(normalizedSchema))
}
