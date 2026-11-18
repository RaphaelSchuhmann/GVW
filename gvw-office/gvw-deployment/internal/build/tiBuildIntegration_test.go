package build

import (
    "encoding/json"
    "gvw/gvw-deployment/internal/utils/log"
    "gvw/gvw-deployment/internal/utils/filesystem"
    "os"
    "testing"   
)

// loadRealNormalizedSchema handles setup, loading, normalizing, and marking embedded classes.
// It skips the test automatically if the backend source directory is missing.
func loadRealNormalizedSchema(t *testing.T) map[string]any {
    t.Helper()
    
    modelDir := "../../../gvw-backend/src/main/java/com/gvw/gvwbackend/model"
    if _, err := os.Stat(modelDir); os.IsNotExist(err) {
        t.Skip("Backend model directory not found, skipping integration test")
    }

    logger := log.NewLogger()
    fsWrapper := filesystem.NewFileSystem(logger)
    modelLoader := NewModelLoader(logger, fsWrapper)

    rawSchema, err := modelLoader.LoadModelsFromDir(modelDir)
    if err != nil {
        t.Fatalf("Failed to load model directory: %v", err)
    }

    normalizedSchema, err := modelLoader.NormalizeAndValidateSchema(rawSchema)
    if err != nil {
        t.Fatalf("Failed to normalize schema: %v", err)
    }

    modelLoader.MarkEmbeddedClasses(normalizedSchema)
    return normalizedSchema
}

func TestIntegration_RealModelSources_ClassHierarchy(t *testing.T) {
    schema := loadRealNormalizedSchema(t)

    // Verify Deployment structure and embedding
    deployment := getClassMap(t, schema, "Deployment")
    assertEmbedded(t, deployment, false)

    migrations := getFieldMap(t, deployment, "migrations")
    if migrations["type"] != "array" || migrations["items"] != "Migration" {
        t.Errorf("Expected migrations to be an array of Migration, got %v", migrations)
    }

    // Verify Migration structure and embedding
    migration := getClassMap(t, schema, "Migration")
    assertEmbedded(t, migration, true)

    migrationMembers := migration["members"].(map[string]any)
    if migrationMembers["conditional"] != "Conditional" {
        t.Errorf("Expected Migration.conditional to reference Conditional, got '%v'", migrationMembers["conditional"])
    }

    // Verify Conditional structure and embedding
    conditional := getClassMap(t, schema, "Conditional")
    assertEmbedded(t, conditional, true)

    conditionalMembers := conditional["members"].(map[string]any)
    for _, field := range []string{"condition", "truePath", "falsePath"} {
        if ref, ok := conditionalMembers[field]; ok && ref != "Branch" {
            t.Errorf("Expected Conditional.%s to reference Branch, got '%v'", field, ref)
        }
    }

    // Verify Branch structure and embedding
    branch := getClassMap(t, schema, "Branch")
    assertEmbedded(t, branch, true)
}

func TestIntegration_RealModelSources_EnumsExcluded(t *testing.T) {
    schema := loadRealNormalizedSchema(t)

    for className, classData := range schema {
        if classMap, ok := classData.(map[string]any); ok {
            if objType, ok := classMap["objType"].(string); ok && objType == "enum" {
                t.Errorf("Enum %s should not appear in normalized schema", className)
            }
        }
    }
}

func TestIntegration_RealModelSources_JsonRoundtrip(t *testing.T) {
    schema := loadRealNormalizedSchema(t)

    buf, err := json.MarshalIndent(schema, "", "  ")
    if err != nil {
        t.Fatalf("Failed to encode schema to JSON: %v", err)
    }

    var decodedSchema map[string]any
    if err := json.Unmarshal(buf, &decodedSchema); err != nil {
        t.Fatalf("Failed to decode JSON back to schema: %v", err)
    }

    if len(decodedSchema) != len(schema) {
        t.Errorf("Decoded schema length mismatch: got %d, expected %d", len(decodedSchema), len(schema))
    }
}
