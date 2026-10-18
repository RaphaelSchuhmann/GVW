package build

import (
	"gvw/gvw-deployment/internal/utils"
	"strings"
	"testing"
)

func TestParseLines_PublicClassWithFields(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	lines := []string{
		"public class Person {",
		"    private String name;",
		"    private int age;",
		"}",
	}

	result, err := loader.ParseLines(lines)
	if err != nil {
		t.Fatalf("ParseLines failed: %v", err)
	}

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
	logger := utils.NewLogger()
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

	result, err := loader.ParseLines(lines)
	if err != nil {
		t.Fatalf("ParseLines failed: %v", err)
	}

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
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	lines := []string{
		"public class TestClass {",
		"    @Builder.Default",
		"    private String name;",
		"}",
	}

	result, err := loader.ParseLines(lines)
	if err != nil {
		t.Fatalf("ParseLines failed: %v", err)
	}

	class := result["TestClass"].(map[string]any)
	members := class["members"].(map[string]any)

	if members["name"] != "String" {
		t.Errorf("Expected name field to be String, got '%v'", members["name"])
	}
}

func TestParseLines_JsonPropertyAnnotation(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	lines := []string{
		"public class TestClass {",
		"    @JsonProperty(\"jsonName\")",
		"    private String javaName;",
		"}",
	}

	result, err := loader.ParseLines(lines)
	if err != nil {
		t.Fatalf("ParseLines failed: %v", err)
	}

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

func TestParseLines_JsonPropertySeparateLines(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	lines := []string{
		"public class TestClass {",
		"    @JsonProperty(\"customName\")",
		"    private String fieldName;",
		"}",
	}

	result, err := loader.ParseLines(lines)
	if err != nil {
		t.Fatalf("ParseLines failed: %v", err)
	}

	class := result["TestClass"].(map[string]any)
	members := class["members"].(map[string]any)

	if _, ok := members["customName"]; !ok {
		t.Errorf("Expected field 'customName' not found")
	}
}

func TestParseLines_GenericDeclarations(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	lines := []string{
		"public class TestClass {",
		"    private List<String> stringList;",
		"    private Map<String, Migration> migrationMap;",
		"}",
	}

	result, err := loader.ParseLines(lines)
	if err != nil {
		t.Fatalf("ParseLines failed: %v", err)
	}

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
	logger := utils.NewLogger()
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

	result, err := loader.ParseLines(lines)
	if err != nil {
		t.Fatalf("ParseLines failed: %v", err)
	}

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
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	lines := []string{
		"public enum Status {",
		"    ACTIVE,",
		"    INACTIVE,",
		"    PENDING",
		"}",
	}

	result, err := loader.ParseLines(lines)
	if err != nil {
		t.Fatalf("ParseLines failed: %v", err)
	}

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

func TestLoadModel_NonexistentPath(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	// Explicitly set the path to not exist
	fsWrapper.dirOrFileExistsMap["/nonexistent/path.java"] = false

	_, err := loader.LoadModel("/nonexistent/path.java")
	if err == nil {
		t.Errorf("Expected error for nonexistent path, got nil")
	}

	if err.Error() != "INVALID_PATH" {
		t.Errorf("Expected error 'INVALID_PATH', got '%v'", err)
	}
}

func TestLoadModel_EmptyPath(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	_, err := loader.LoadModel("")
	if err == nil {
		t.Errorf("Expected error for empty path, got nil")
	}

	if err.Error() != "INVALID_PATH" {
		t.Errorf("Expected error 'INVALID_PATH', got '%v'", err)
	}
}

func TestNormalizeType_StringTypes(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)

	stringTypes := []string{"String", "char", "Character", "Instant", "LocalDate", "LocalDateTime", "LocalTime"}
	for _, typeName := range stringTypes {
		result, err := NormalizeType(typeName, classes, enums)
		if err != nil {
			t.Errorf("NormalizeType(%s) failed: %v", typeName, err)
		}
		if result != "string" {
			t.Errorf("NormalizeType(%s) expected 'string', got '%v'", typeName, result)
		}
	}
}

func TestNormalizeType_IntegerTypes(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)

	intTypes := []string{"byte", "Byte", "short", "Short", "int", "Integer", "long", "Long", "BigInteger"}
	for _, typeName := range intTypes {
		result, err := NormalizeType(typeName, classes, enums)
		if err != nil {
			t.Errorf("NormalizeType(%s) failed: %v", typeName, err)
		}
		if result != "int" {
			t.Errorf("NormalizeType(%s) expected 'int', got '%v'", typeName, result)
		}
	}
}

func TestNormalizeType_FloatTypes(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)

	floatTypes := []string{"float", "Float", "double", "Double", "BigDecimal"}
	for _, typeName := range floatTypes {
		result, err := NormalizeType(typeName, classes, enums)
		if err != nil {
			t.Errorf("NormalizeType(%s) failed: %v", typeName, err)
		}
		if result != "float" {
			t.Errorf("NormalizeType(%s) expected 'float', got '%v'", typeName, result)
		}
	}
}

func TestNormalizeType_BooleanTypes(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)

	boolTypes := []string{"boolean", "Boolean"}
	for _, typeName := range boolTypes {
		result, err := NormalizeType(typeName, classes, enums)
		if err != nil {
			t.Errorf("NormalizeType(%s) failed: %v", typeName, err)
		}
		if result != "bool" {
			t.Errorf("NormalizeType(%s) expected 'bool', got '%v'", typeName, result)
		}
	}
}

func TestNormalizeType_KnownEnum(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)
	enums["Status"] = true

	result, err := NormalizeType("Status", classes, enums)
	if err != nil {
		t.Errorf("NormalizeType(Status) failed: %v", err)
	}
	if result != "string" {
		t.Errorf("NormalizeType(Status) expected 'string', got '%v'", result)
	}
}

func TestNormalizeType_KnownModelClass(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)
	classes["Migration"] = true

	result, err := NormalizeType("Migration", classes, enums)
	if err != nil {
		t.Errorf("NormalizeType(Migration) failed: %v", err)
	}
	if result != "Migration" {
		t.Errorf("NormalizeType(Migration) expected 'Migration', got '%v'", result)
	}
}

func TestNormalizeType_ListOfString(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)

	result, err := NormalizeType("List<String>", classes, enums)
	if err != nil {
		t.Errorf("NormalizeType(List<String>) failed: %v", err)
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

func TestNormalizeType_ListOfModel(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)
	classes["Migration"] = true

	result, err := NormalizeType("List<Migration>", classes, enums)
	if err != nil {
		t.Errorf("NormalizeType(List<Migration>) failed: %v", err)
	}

	resultMap, ok := result.(map[string]any)
	if !ok {
		t.Fatalf("Expected map, got %T", result)
	}

	if resultMap["type"] != "array" {
		t.Errorf("Expected type 'array', got '%v'", resultMap["type"])
	}

	if resultMap["items"] != "Migration" {
		t.Errorf("Expected items 'Migration', got '%v'", resultMap["items"])
	}
}

func TestNormalizeType_SetOfModel(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)
	classes["Migration"] = true

	result, err := NormalizeType("Set<Migration>", classes, enums)
	if err != nil {
		t.Errorf("NormalizeType(Set<Migration>) failed: %v", err)
	}

	resultMap, ok := result.(map[string]any)
	if !ok {
		t.Fatalf("Expected map, got %T", result)
	}

	if resultMap["type"] != "array" {
		t.Errorf("Expected type 'array', got '%v'", resultMap["type"])
	}

	if resultMap["items"] != "Migration" {
		t.Errorf("Expected items 'Migration', got '%v'", resultMap["items"])
	}
}

func TestNormalizeType_ArraySyntax(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)
	classes["Migration"] = true

	result, err := NormalizeType("Migration[]", classes, enums)
	if err != nil {
		t.Errorf("NormalizeType(Migration[]) failed: %v", err)
	}

	resultMap, ok := result.(map[string]any)
	if !ok {
		t.Fatalf("Expected map, got %T", result)
	}

	if resultMap["type"] != "array" {
		t.Errorf("Expected type 'array', got '%v'", resultMap["type"])
	}

	if resultMap["items"] != "Migration" {
		t.Errorf("Expected items 'Migration', got '%v'", resultMap["items"])
	}
}

func TestNormalizeType_MapStringString(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)

	result, err := NormalizeType("Map<String, String>", classes, enums)
	if err != nil {
		t.Errorf("NormalizeType(Map<String, String>) failed: %v", err)
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

	result, err := NormalizeType("Map<String, Migration>", classes, enums)
	if err != nil {
		t.Errorf("NormalizeType(Map<String, Migration>) failed: %v", err)
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

func TestNormalizeType_NestedList(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)
	classes["Migration"] = true

	result, err := NormalizeType("List<List<Migration>>", classes, enums)
	if err != nil {
		t.Errorf("NormalizeType(List<List<Migration>>) failed: %v", err)
	}

	resultMap, ok := result.(map[string]any)
	if !ok {
		t.Fatalf("Expected map, got %T", result)
	}

	if resultMap["type"] != "array" {
		t.Errorf("Expected type 'array', got '%v'", resultMap["type"])
	}

	// Check nested structure
	itemsMap, ok := resultMap["items"].(map[string]any)
	if !ok {
		t.Fatalf("Expected items to be a map, got %T", resultMap["items"])
	}

	if itemsMap["type"] != "array" {
		t.Errorf("Expected nested type 'array', got '%v'", itemsMap["type"])
	}

	if itemsMap["items"] != "Migration" {
		t.Errorf("Expected nested items 'Migration', got '%v'", itemsMap["items"])
	}
}

func TestNormalizeType_MapWithListValues(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)
	classes["Migration"] = true

	result, err := NormalizeType("Map<String, List<Migration>>", classes, enums)
	if err != nil {
		t.Errorf("NormalizeType(Map<String, List<Migration>>) failed: %v", err)
	}

	resultMap, ok := result.(map[string]any)
	if !ok {
		t.Fatalf("Expected map, got %T", result)
	}

	if resultMap["type"] != "object" {
		t.Errorf("Expected type 'object', got '%v'", resultMap["type"])
	}

	// Check nested structure
	valuesMap, ok := resultMap["values"].(map[string]any)
	if !ok {
		t.Fatalf("Expected values to be a map, got %T", resultMap["values"])
	}

	if valuesMap["type"] != "array" {
		t.Errorf("Expected values type 'array', got '%v'", valuesMap["type"])
	}

	if valuesMap["items"] != "Migration" {
		t.Errorf("Expected values items 'Migration', got '%v'", valuesMap["items"])
	}
}

func TestNormalizeType_UnknownClass(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)

	_, err := NormalizeType("UnknownClass", classes, enums)
	if err == nil {
		t.Errorf("Expected error for unknown class, got nil")
	}

	if err.Error() != "UNSUPPORTED_TYPE" {
		t.Errorf("Expected error 'UNSUPPORTED_TYPE', got '%v'", err)
	}
}

func TestNormalizeType_ListOfUnknownModel(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)

	_, err := NormalizeType("List<UnknownModel>", classes, enums)
	if err == nil {
		t.Errorf("Expected error for List<UnknownModel>, got nil")
	}

	if err.Error() != "UNSUPPORTED_TYPE" {
		t.Errorf("Expected error 'UNSUPPORTED_TYPE', got '%v'", err)
	}
}

func TestNormalizeType_InvalidMapDeclaration(t *testing.T) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)

	_, err := NormalizeType("Map<String>", classes, enums)
	if err == nil {
		t.Errorf("Expected error for invalid map declaration, got nil")
	}

	if err.Error() != "INVALID_MAP_FORMAT" {
		t.Errorf("Expected error 'INVALID_MAP_FORMAT', got '%v'", err)
	}
}

func TestNormalizeAndValidateSchema_ValidClassWithPrimitives(t *testing.T) {
	logger := utils.NewLogger()
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
	logger := utils.NewLogger()
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
	logger := utils.NewLogger()
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
	logger := utils.NewLogger()
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

func TestNormalizeAndValidateSchema_CollectionOfUnknownModel(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

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

	if !strings.Contains(err.Error(), "ERROR_NORMALIZING_TYPE") {
		t.Errorf("Expected error to contain 'ERROR_NORMALIZING_TYPE', got '%v'", err)
	}
}

func TestNormalizeAndValidateSchema_MapWithUnknownValueType(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	rawSchema := map[string]any{
		"Deployment": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"metadata": "Map<String,UnknownModel>",
			},
		},
	}

	_, err := loader.NormalizeAndValidateSchema(rawSchema)
	if err == nil {
		t.Errorf("Expected error for map with unknown value type, got nil")
	}

	if !strings.Contains(err.Error(), "ERROR_NORMALIZING_TYPE") {
		t.Errorf("Expected error to contain 'ERROR_NORMALIZING_TYPE', got '%v'", err)
	}
}

func TestNormalizeAndValidateSchema_MalformedClassStructure(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	rawSchema := map[string]any{
		"Person": "not a map",
	}

	_, err := loader.NormalizeAndValidateSchema(rawSchema)
	if err == nil {
		t.Errorf("Expected error for malformed class structure, got nil")
	}

	if err.Error() != "INVALID_CLASS_STRUCTURE" {
		t.Errorf("Expected error 'INVALID_CLASS_STRUCTURE', got '%v'", err)
	}
}

func TestNormalizeAndValidateSchema_NonStringFieldType(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	rawSchema := map[string]any{
		"Person": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"name": 123, // Not a string
			},
		},
	}

	_, err := loader.NormalizeAndValidateSchema(rawSchema)
	if err == nil {
		t.Errorf("Expected error for non-string field type, got nil")
	}

	if err.Error() != "INVALID_TYPE_REPRESENTATION" {
		t.Errorf("Expected error 'INVALID_TYPE_REPRESENTATION', got '%v'", err)
	}
}

func TestNormalizeAndValidateSchema_UnsupportedFieldType(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	rawSchema := map[string]any{
		"Person": map[string]any{
			"objType": "class",
			"members": map[string]any{
				"data": "UnsupportedType",
			},
		},
	}

	_, err := loader.NormalizeAndValidateSchema(rawSchema)
	if err == nil {
		t.Errorf("Expected error for unsupported field type, got nil")
	}

	if !strings.Contains(err.Error(), "ERROR_NORMALIZING_TYPE") {
		t.Errorf("Expected error to contain 'ERROR_NORMALIZING_TYPE', got '%v'", err)
	}
}

func TestMarkEmbeddedClasses_DirectReference(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

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
}

func TestMarkEmbeddedClasses_ReferenceThroughArray(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	schema := map[string]any{
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

	migration := schema["Migration"].(map[string]any)

	if migration["embedded"] != true {
		t.Errorf("Expected Migration to be embedded, got '%v'", migration["embedded"])
	}
}

func TestMarkEmbeddedClasses_ReferenceThroughMapValues(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	schema := map[string]any{
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

	migration := schema["Migration"].(map[string]any)

	if migration["embedded"] != true {
		t.Errorf("Expected Migration to be embedded, got '%v'", migration["embedded"])
	}
}

func TestMarkEmbeddedClasses_NestedReference(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	schema := map[string]any{
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

	migration := schema["Migration"].(map[string]any)

	if migration["embedded"] != true {
		t.Errorf("Expected Migration to be embedded, got '%v'", migration["embedded"])
	}
}

func TestMarkEmbeddedClasses_UnreferencedClass(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

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
}

func TestMarkEmbeddedClasses_SelfReference(t *testing.T) {
	logger := utils.NewLogger()
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

func TestMarkEmbeddedClasses_PrimitiveTypeNoEmbedding(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	schema := map[string]any{
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
}

func TestMarkEmbeddedClasses_EnumNotEmbedded(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	schema := map[string]any{
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

	// Enums should not have embedded flag
	if _, ok := status["embedded"]; ok {
		t.Errorf("Enum should not have embedded flag")
	}
}

func TestMarkEmbeddedClasses_MultipleReferencesSameModel(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

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
}

func TestMarkEmbeddedClasses_EmbeddedClassReferencingAnother(t *testing.T) {
	logger := utils.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)

	schema := map[string]any{
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

	migration := schema["Migration"].(map[string]any)
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
