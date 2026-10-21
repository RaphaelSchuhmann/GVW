package build

import (
	"gvw/gvw-deployment/internal/utils/log"
	"strings"
	"testing"
)

func TestParseLines_PublicClassWithFields(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	lines := []string{
		"public class Person {",
		"    private String name;",
		"    private int age;",
		"}",
	}

	result := loader.parseLines(lines)

	if len(result) != 1 {
		t.Fatalf("Expected 1 class, got %d", len(result))
	}

	person, ok := result["Person"].(map[string]any)
	if !ok {
		t.Fatalf("Person not found or not a map")
	}

	if person["objType"] != "class" {
		t.Errorf("Expected objType 'class', got '%v'", person["objType"])
	}

	members, ok := person["members"].(map[string]any)
	if !ok {
		t.Fatalf("Members not found or not a map")
	}

	if len(members) != 2 {
		t.Fatalf("Expected 2 members, got %d", len(members))
	}

	if members["name"] != "String" {
		t.Errorf("Expected name field to be String, got '%v'", members["name"])
	}

	if members["age"] != "int" {
		t.Errorf("Expected age field to be int, got '%v'", members["age"])
	}
}

func TestParseLines_ModifiersIgnored(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	lines := []string{
		"public class TestClass {",
		"    private String privateField;",
		"    public String publicField;",
		"    protected String protectedField;",
		"    static String staticField;",
		"    final String finalField;",
		"}",
	}

	result := loader.parseLines(lines)

	class, ok := result["TestClass"].(map[string]any)
	if !ok {
		t.Fatalf("TestClass not found")
	}

	members := class["members"].(map[string]any)
	if len(members) != 5 {
		t.Fatalf("Expected 5 members, got %d", len(members))
	}

	// Verify modifiers are not part of field names
	expectedFields := []string{"privateField", "publicField", "protectedField", "staticField", "finalField"}
	for _, field := range expectedFields {
		if _, ok := members[field]; !ok {
			t.Errorf("Expected field '%s' not found", field)
		}
	}
}

func TestParseLines_BuilderDefaultAnnotation(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	lines := []string{
		"public class TestClass {",
		"    @Builder.Default",
		"    private String name;",
		"}",
	}

	result := loader.parseLines(lines)

	class := result["TestClass"].(map[string]any)
	members := class["members"].(map[string]any)

	if members["name"] != "String" {
		t.Errorf("Expected name field to be String, got '%v'", members["name"])
	}
}

func TestParseLines_JsonPropertyAnnotation(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	lines := []string{
		"public class TestClass {",
		"    @JsonProperty(\"jsonName\")",
		"    private String javaName;",
		"}",
	}

	result := loader.parseLines(lines)

	class := result["TestClass"].(map[string]any)
	members := class["members"].(map[string]any)

	// Should use jsonName, not javaName
	if _, ok := members["jsonName"]; !ok {
		t.Errorf("Expected field 'jsonName' not found")
	}

	if _, ok := members["javaName"]; ok {
		t.Errorf("Field 'javaName' should not exist, should be 'jsonName'")
	}

	if members["jsonName"] != "String" {
		t.Errorf("Expected jsonName field to be String, got '%v'", members["jsonName"])
	}
}

func TestParseLines_GenericDeclarations(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	lines := []string{
		"public class TestClass {",
		"    private List<String> stringList;",
		"    private Map<String, Migration> migrationMap;",
		"}",
	}

	result := loader.parseLines(lines)

	class := result["TestClass"].(map[string]any)
	members := class["members"].(map[string]any)

	if members["stringList"] != "List<String>" {
		t.Errorf("Expected stringList to be 'List<String>', got '%v'", members["stringList"])
	}

	if members["migrationMap"] != "Map<String,Migration>" {
		t.Errorf("Expected migrationMap to be 'Map<String,Migration>', got '%v'", members["migrationMap"])
	}
}

func TestParseLines_CommentsAndAnnotations(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	lines := []string{
		"// This is a comment",
		"/* Block comment */",
		"public class TestClass {",
		"    /**",
		"     * Javadoc comment",
		"     */",
		"    @Deprecated",
		"    private String name;",
		"}",
	}

	result := loader.parseLines(lines)

	class := result["TestClass"].(map[string]any)
	members := class["members"].(map[string]any)

	if len(members) != 1 {
		t.Fatalf("Expected 1 member, got %d", len(members))
	}

	if members["name"] != "String" {
		t.Errorf("Expected name field to be String, got '%v'", members["name"])
	}
}

func TestParseLines_Enum(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	lines := []string{
		"public enum Status {",
		"    ACTIVE,",
		"    INACTIVE,",
		"    PENDING",
		"}",
	}

	result := loader.parseLines(lines)

	if len(result) != 1 {
		t.Fatalf("Expected 1 entry, got %d", len(result))
	}

	status, ok := result["Status"].(map[string]any)
	if !ok {
		t.Fatalf("Status not found or not a map")
	}

	if status["objType"] != "enum" {
		t.Errorf("Expected objType 'enum', got '%v'", status["objType"])
	}

	// Enums should not have members
	if _, ok := status["members"]; ok {
		t.Errorf("Enum should not have members")
	}
}

func TestLoadModel_InvalidPaths(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	// Test empty path
	_, err := loader.LoadModel("")
	if err == nil {
		t.Errorf("Expected error for empty path, got nil")
	}
	if err.Error() != "ModelLoader_Invalid_Path" {
		t.Errorf("Expected error 'ModelLoader_Invalid_Path', got '%v'", err)
	}

	// Test nonexistent path
	fsWrapper.dirOrFileExistsMap["/nonexistent/path.java"] = false
	_, err = loader.LoadModel("/nonexistent/path.java")
	if err == nil {
		t.Errorf("Expected error for nonexistent path, got nil")
	}
	if err.Error() != "ModelLoader_Invalid_Path" {
		t.Errorf("Expected error 'ModelLoader_Invalid_Path', got '%v'", err)
	}
}

func TestNormalizeType_PrimitiveTypes(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)

	// Test string types
	stringTypes := []string{"String", "char", "Character", "Instant", "LocalDate", "LocalDateTime", "LocalTime"}
	for _, typeName := range stringTypes {
		result, err := normalizeType(typeName, classes, enums)
		if err != nil {
			t.Errorf("normalizeType(%s) failed: %v", typeName, err)
		}
		if result != "string" {
			t.Errorf("normalizeType(%s) expected 'string', got '%v'", typeName, result)
		}
	}

	// Test integer types
	intTypes := []string{"byte", "Byte", "short", "Short", "int", "Integer", "long", "Long", "BigInteger"}
	for _, typeName := range intTypes {
		result, err := normalizeType(typeName, classes, enums)
		if err != nil {
			t.Errorf("normalizeType(%s) failed: %v", typeName, err)
		}
		if result != "int" {
			t.Errorf("normalizeType(%s) expected 'int', got '%v'", typeName, result)
		}
	}

	// Test float types
	floatTypes := []string{"float", "Float", "double", "Double", "BigDecimal"}
	for _, typeName := range floatTypes {
		result, err := normalizeType(typeName, classes, enums)
		if err != nil {
			t.Errorf("normalizeType(%s) failed: %v", typeName, err)
		}
		if result != "float" {
			t.Errorf("normalizeType(%s) expected 'float', got '%v'", typeName, result)
		}
	}

	// Test boolean types
	boolTypes := []string{"boolean", "Boolean"}
	for _, typeName := range boolTypes {
		result, err := normalizeType(typeName, classes, enums)
		if err != nil {
			t.Errorf("normalizeType(%s) failed: %v", typeName, err)
		}
		if result != "bool" {
			t.Errorf("normalizeType(%s) expected 'bool', got '%v'", typeName, result)
		}
	}
}

func TestNormalizeType_KnownTypes(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)

	// Test known enum
	enums["Status"] = true
	result, err := normalizeType("Status", classes, enums)
	if err != nil {
		t.Errorf("normalizeType(Status) failed: %v", err)
	}
	if result != "string" {
		t.Errorf("normalizeType(Status) expected 'string', got '%v'", result)
	}

	// Test known model class
	classes["Migration"] = true
	result, err = normalizeType("Migration", classes, enums)
	if err != nil {
		t.Errorf("normalizeType(Migration) failed: %v", err)
	}
	if result != "Migration" {
		t.Errorf("normalizeType(Migration) expected 'Migration', got '%v'", result)
	}
}

func TestNormalizeType_ListOfString(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)

	result, err := normalizeType("List<String>", classes, enums)
	if err != nil {
		t.Errorf("normalizeType(List<String>) failed: %v", err)
	}

	resultMap, ok := result.(map[string]any)
	if !ok {
		t.Fatalf("Expected map, got %T", result)
	}

	if resultMap["type"] != "array" {
		t.Errorf("Expected type 'array', got '%v'", resultMap["type"])
	}

	if resultMap["items"] != "string" {
		t.Errorf("Expected items 'string', got '%v'", resultMap["items"])
	}
}

func TestNormalizeType_CollectionTypes(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)
	classes["Migration"] = true

	// Test List
	result, err := normalizeType("List<Migration>", classes, enums)
	if err != nil {
		t.Errorf("normalizeType(List<Migration>) failed: %v", err)
	}
	resultMap, ok := result.(map[string]any)
	if !ok {
		t.Fatalf("Expected map for List, got %T", result)
	}
	if resultMap["type"] != "array" || resultMap["items"] != "Migration" {
		t.Errorf("List<Migration> not normalized correctly")
	}

	// Test Set
	result, err = normalizeType("Set<Migration>", classes, enums)
	if err != nil {
		t.Errorf("normalizeType(Set<Migration>) failed: %v", err)
	}
	resultMap, ok = result.(map[string]any)
	if !ok {
		t.Fatalf("Expected map for Set, got %T", result)
	}
	if resultMap["type"] != "array" || resultMap["items"] != "Migration" {
		t.Errorf("Set<Migration> not normalized correctly")
	}

	// Test array syntax
	result, err = normalizeType("Migration[]", classes, enums)
	if err != nil {
		t.Errorf("normalizeType(Migration[]) failed: %v", err)
	}
	resultMap, ok = result.(map[string]any)
	if !ok {
		t.Fatalf("Expected map for array syntax, got %T", result)
	}
	if resultMap["type"] != "array" || resultMap["items"] != "Migration" {
		t.Errorf("Migration[] not normalized correctly")
	}
}

func TestNormalizeType_MapStringString(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)

	result, err := normalizeType("Map<String, String>", classes, enums)
	if err != nil {
		t.Errorf("normalizeType(Map<String, String>) failed: %v", err)
	}

	resultMap, ok := result.(map[string]any)
	if !ok {
		t.Fatalf("Expected map, got %T", result)
	}

	if resultMap["type"] != "object" {
		t.Errorf("Expected type 'object', got '%v'", resultMap["type"])
	}

	if resultMap["values"] != "string" {
		t.Errorf("Expected values 'string', got '%v'", resultMap["values"])
	}
}

func TestNormalizeType_MapStringModel(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)
	classes["Migration"] = true

	result, err := normalizeType("Map<String, Migration>", classes, enums)
	if err != nil {
		t.Errorf("normalizeType(Map<String, Migration>) failed: %v", err)
	}

	resultMap, ok := result.(map[string]any)
	if !ok {
		t.Fatalf("Expected map, got %T", result)
	}

	if resultMap["type"] != "object" {
		t.Errorf("Expected type 'object', got '%v'", resultMap["type"])
	}

	if resultMap["values"] != "Migration" {
		t.Errorf("Expected values 'Migration', got '%v'", resultMap["values"])
	}
}

func TestNormalizeType_NestedStructures(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)
	classes["Migration"] = true

	// Test nested list
	result, err := normalizeType("List<List<Migration>>", classes, enums)
	if err != nil {
		t.Errorf("normalizeType(List<List<Migration>>) failed: %v", err)
	}
	resultMap, ok := result.(map[string]any)
	if !ok {
		t.Fatalf("Expected map for nested list, got %T", result)
	}
	if resultMap["type"] != "array" {
		t.Errorf("Expected type 'array', got '%v'", resultMap["type"])
	}
	itemsMap, ok := resultMap["items"].(map[string]any)
	if !ok {
		t.Fatalf("Expected items to be a map, got %T", resultMap["items"])
	}
	if itemsMap["type"] != "array" || itemsMap["items"] != "Migration" {
		t.Errorf("Nested list structure not normalized correctly")
	}

	// Test map with list values
	result, err = normalizeType("Map<String, List<Migration>>", classes, enums)
	if err != nil {
		t.Errorf("normalizeType(Map<String, List<Migration>>) failed: %v", err)
	}
	resultMap, ok = result.(map[string]any)
	if !ok {
		t.Fatalf("Expected map for map with list values, got %T", result)
	}
	if resultMap["type"] != "object" {
		t.Errorf("Expected type 'object', got '%v'", resultMap["type"])
	}
	valuesMap, ok := resultMap["values"].(map[string]any)
	if !ok {
		t.Fatalf("Expected values to be a map, got %T", resultMap["values"])
	}
	if valuesMap["type"] != "array" || valuesMap["items"] != "Migration" {
		t.Errorf("Map with list values not normalized correctly")
	}
}

func TestNormalizeType_ErrorCases(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)

	// Test unknown class
	_, err := normalizeType("UnknownClass", classes, enums)
	if err == nil {
		t.Errorf("Expected error for unknown class, got nil")
	}
	if err.Error() != "ModelLoder_Unsupported_Type" {
		t.Errorf("Expected error 'ModelLoder_Unsupported_Type', got '%v'", err)
	}

	// Test list of unknown model
	_, err = normalizeType("List<UnknownModel>", classes, enums)
	if err == nil {
		t.Errorf("Expected error for List<UnknownModel>, got nil")
	}
	if err.Error() != "ModelLoder_Normalizing_Collections" {
		t.Errorf("Expected error 'ModelLoder_Normalizing_Collections', got '%v'", err)
	}

	// Test invalid map declaration
	_, err = normalizeType("Map<String>", classes, enums)
	if err == nil {
		t.Errorf("Expected error for invalid map declaration, got nil")
	}
	if err.Error() != "ModelLoder_Invalid_Map_Format" {
		t.Errorf("Expected error 'ModelLoder_Invalid_Map_Format', got '%v'", err)
	}
}

func TestNormalizeAndValidateSchema_ValidClassWithPrimitives(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	rawSchema := map[string]any{
		"Person": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"name": "String",
				"age":  "int",
			},
		},
	}

	result, err := loader.NormalizeAndValidateSchema(rawSchema)
	if err != nil {
		t.Fatalf("NormalizeAndValidateSchema failed: %v", err)
	}

	person := result["Person"].(map[string]any)
	members := person["members"].(map[string]any)

	if members["name"] != "string" {
		t.Errorf("Expected name to be normalized to 'string', got '%v'", members["name"])
	}

	if members["age"] != "int" {
		t.Errorf("Expected age to be normalized to 'int', got '%v'", members["age"])
	}
}

func TestNormalizeAndValidateSchema_ClassReferencingAnotherClass(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	rawSchema := map[string]any{
		"Deployment": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"migration": "Migration",
			},
		},
		"Migration": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"id": "String",
			},
		},
	}

	result, err := loader.NormalizeAndValidateSchema(rawSchema)
	if err != nil {
		t.Fatalf("NormalizeAndValidateSchema failed: %v", err)
	}

	deployment := result["Deployment"].(map[string]any)
	members := deployment["members"].(map[string]any)

	if members["migration"] != "Migration" {
		t.Errorf("Expected migration reference to be preserved, got '%v'", members["migration"])
	}
}

func TestNormalizeAndValidateSchema_ClassReferencingEnum(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	rawSchema := map[string]any{
		"Task": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"status": "Status",
			},
		},
		"Status": map[string]any{
			"objType": "enum",
		},
	}

	result, err := loader.NormalizeAndValidateSchema(rawSchema)
	if err != nil {
		t.Fatalf("NormalizeAndValidateSchema failed: %v", err)
	}

	task := result["Task"].(map[string]any)
	members := task["members"].(map[string]any)

	if members["status"] != "string" {
		t.Errorf("Expected status to be normalized to 'string', got '%v'", members["status"])
	}

	// Enum should not appear in output
	if _, ok := result["Status"]; ok {
		t.Errorf("Enum should not appear in normalized schema")
	}
}

func TestNormalizeAndValidateSchema_CollectionOfKnownModel(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	rawSchema := map[string]any{
		"Deployment": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"migrations": "List<Migration>",
			},
		},
		"Migration": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"id": "String",
			},
		},
	}

	result, err := loader.NormalizeAndValidateSchema(rawSchema)
	if err != nil {
		t.Fatalf("NormalizeAndValidateSchema failed: %v", err)
	}

	deployment := result["Deployment"].(map[string]any)
	members := deployment["members"].(map[string]any)

	migrations := members["migrations"].(map[string]any)
	if migrations["type"] != "array" {
		t.Errorf("Expected migrations type 'array', got '%v'", migrations["type"])
	}

	if migrations["items"] != "Migration" {
		t.Errorf("Expected migrations items 'Migration', got '%v'", migrations["items"])
	}
}

func TestNormalizeAndValidateSchema_UnknownTypes(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	// Test collection of unknown model
	rawSchema := map[string]any{
		"Deployment": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"items": "List<UnknownModel>",
			},
		},
	}

	_, err := loader.NormalizeAndValidateSchema(rawSchema)
	if err == nil {
		t.Errorf("Expected error for collection of unknown model, got nil")
	}
	if !strings.Contains(err.Error(), "ModelLoader_Normalize_Type") {
		t.Errorf("Expected error to contain 'ModelLoader_Normalize_Type', got '%v'", err)
	}

	// Test map with unknown value type
	rawSchema = map[string]any{
		"Deployment": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"metadata": "Map<String,UnknownModel>",
			},
		},
	}

	_, err = loader.NormalizeAndValidateSchema(rawSchema)
	if err == nil {
		t.Errorf("Expected error for map with unknown value type, got nil")
	}
	if !strings.Contains(err.Error(), "ModelLoader_Normalize_Type") {
		t.Errorf("Expected error to contain 'ModelLoader_Normalize_Type', got '%v'", err)
	}
}

func TestNormalizeAndValidateSchema_InvalidStructures(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	// Test malformed class structure
	rawSchema := map[string]any{
		"Person": "not a map",
	}

	_, err := loader.NormalizeAndValidateSchema(rawSchema)
	if err == nil {
		t.Errorf("Expected error for malformed class structure, got nil")
	}
	if err.Error() != "ModelLoder_Invalid_Class_Structure" {
		t.Errorf("Expected error 'ModelLoder_Invalid_Class_Structure', got '%v'", err)
	}

	// Test non-string field type
	rawSchema = map[string]any{
		"Person": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"name": 123, // Not a string
			},
		},
	}

	_, err = loader.NormalizeAndValidateSchema(rawSchema)
	if err == nil {
		t.Errorf("Expected error for non-string field type, got nil")
	}
	if err.Error() != "ModelLoader_Invalid_Type_Representation" {
		t.Errorf("Expected error 'ModelLoader_Invalid_Type_Representation', got '%v'", err)
	}

	// Test unsupported field type
	rawSchema = map[string]any{
		"Person": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"data": "UnsupportedType",
			},
		},
	}

	_, err = loader.NormalizeAndValidateSchema(rawSchema)
	if err == nil {
		t.Errorf("Expected error for unsupported field type, got nil")
	}
	if !strings.Contains(err.Error(), "ModelLoader_Normalize_Type") {
		t.Errorf("Expected error to contain 'ModelLoader_Normalize_Type', got '%v'", err)
	}
}

func TestMarkEmbeddedClasses_ReferenceTypes(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	// Test direct reference
	schema := map[string]any{
		"Deployment": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"migration": "Migration",
			},
		},
		"Migration": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"id": "String",
			},
		},
	}

	loader.MarkEmbeddedClasses(schema)

	deployment := schema["Deployment"].(map[string]any)
	migration := schema["Migration"].(map[string]any)

	if deployment["embedded"] != false {
		t.Errorf("Expected Deployment to not be embedded, got '%v'", deployment["embedded"])
	}
	if migration["embedded"] != true {
		t.Errorf("Expected Migration to be embedded, got '%v'", migration["embedded"])
	}

	// Test reference through array
	schema = map[string]any{
		"Deployment": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"migrations": map[string]any{
					"type":  "array",
					"items": "Migration",
				},
			},
		},
		"Migration": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"id": "String",
			},
		},
	}

	loader.MarkEmbeddedClasses(schema)

	migration = schema["Migration"].(map[string]any)
	if migration["embedded"] != true {
		t.Errorf("Expected Migration to be embedded through array, got '%v'", migration["embedded"])
	}

	// Test reference through map values
	schema = map[string]any{
		"Deployment": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"metadata": map[string]any{
					"type":   "object",
					"values": "Migration",
				},
			},
		},
		"Migration": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"id": "String",
			},
		},
	}

	loader.MarkEmbeddedClasses(schema)

	migration = schema["Migration"].(map[string]any)
	if migration["embedded"] != true {
		t.Errorf("Expected Migration to be embedded through map, got '%v'", migration["embedded"])
	}

	// Test nested reference
	schema = map[string]any{
		"Deployment": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"migrations": map[string]any{
					"type":  "array",
					"items": map[string]any{
						"type":  "array",
						"items": "Migration",
					},
				},
			},
		},
		"Migration": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"id": "String",
			},
		},
	}

	loader.MarkEmbeddedClasses(schema)

	migration = schema["Migration"].(map[string]any)
	if migration["embedded"] != true {
		t.Errorf("Expected Migration to be embedded through nested structure, got '%v'", migration["embedded"])
	}
}

func TestMarkEmbeddedClasses_NoEmbeddingScenarios(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	// Test unreferenced class
	schema := map[string]any{
		"Deployment": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"id": "String",
			},
		},
		"Migration": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"id": "String",
			},
		},
	}

	loader.MarkEmbeddedClasses(schema)

	deployment := schema["Deployment"].(map[string]any)
	migration := schema["Migration"].(map[string]any)

	if deployment["embedded"] != false {
		t.Errorf("Expected Deployment to not be embedded, got '%v'", deployment["embedded"])
	}
	if migration["embedded"] != false {
		t.Errorf("Expected Migration to not be embedded, got '%v'", migration["embedded"])
	}

	// Test primitive types (no embedding)
	schema = map[string]any{
		"Person": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"name": "string",
				"age":  "int",
			},
		},
	}

	loader.MarkEmbeddedClasses(schema)

	person := schema["Person"].(map[string]any)
	if person["embedded"] != false {
		t.Errorf("Expected Person to not be embedded, got '%v'", person["embedded"])
	}

	// Test enum not embedded
	schema = map[string]any{
		"Task": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"status": "string",
			},
		},
		"Status": map[string]any{
			"objType": "enum",
		},
	}

	loader.MarkEmbeddedClasses(schema)

	task := schema["Task"].(map[string]any)
	status := schema["Status"].(map[string]any)

	if task["embedded"] != false {
		t.Errorf("Expected Task to not be embedded, got '%v'", task["embedded"])
	}
	if _, ok := status["embedded"]; ok {
		t.Errorf("Enum should not have embedded flag")
	}
}

func TestMarkEmbeddedClasses_SelfReference(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	schema := map[string]any{
		"Node": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"parent": "Node",
			},
		},
	}

	loader.MarkEmbeddedClasses(schema)

	node := schema["Node"].(map[string]any)

	if node["embedded"] != true {
		t.Errorf("Expected Node to be embedded (self-reference), got '%v'", node["embedded"])
	}
}

func TestMarkEmbeddedClasses_ComplexScenarios(t *testing.T) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	// Test multiple references to same model
	schema := map[string]any{
		"Deployment": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"migration": "Migration",
			},
		},
		"Backup": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"migration": "Migration",
			},
		},
		"Migration": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"id": "String",
			},
		},
	}

	loader.MarkEmbeddedClasses(schema)

	deployment := schema["Deployment"].(map[string]any)
	backup := schema["Backup"].(map[string]any)
	migration := schema["Migration"].(map[string]any)

	if deployment["embedded"] != false {
		t.Errorf("Expected Deployment to not be embedded, got '%v'", deployment["embedded"])
	}
	if backup["embedded"] != false {
		t.Errorf("Expected Backup to not be embedded, got '%v'", backup["embedded"])
	}
	if migration["embedded"] != true {
		t.Errorf("Expected Migration to be embedded, got '%v'", migration["embedded"])
	}

	// Test embedded class referencing another (transitive embedding)
	schema = map[string]any{
		"Deployment": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"migrations": map[string]any{
					"type":  "array",
					"items": "Migration",
				},
			},
		},
		"Migration": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"conditional": "Conditional",
			},
		},
		"Conditional": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"condition": "Branch",
			},
		},
		"Branch": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"id": "String",
			},
		},
	}

	loader.MarkEmbeddedClasses(schema)

	migration = schema["Migration"].(map[string]any)
	conditional := schema["Conditional"].(map[string]any)
	branch := schema["Branch"].(map[string]any)

	if migration["embedded"] != true {
		t.Errorf("Expected Migration to be embedded, got '%v'", migration["embedded"])
	}
	if conditional["embedded"] != true {
		t.Errorf("Expected Conditional to be embedded, got '%v'", conditional["embedded"])
	}
	if branch["embedded"] != true {
		t.Errorf("Expected Branch to be embedded, got '%v'", branch["embedded"])
	}
}
