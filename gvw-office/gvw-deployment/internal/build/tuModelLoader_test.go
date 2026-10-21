package build

import (
	"gvw/gvw-deployment/internal/utils/log"
	"strings"
	"testing"
)

func setupTestLoader(t *testing.T) (*log.Logger, *MockFileSystemWrapper, *ModelLoader) {
	logger := log.NewLogger()
	fsWrapper := NewMockFileSystemWrapper(logger)
	loader := NewModelLoader(logger, fsWrapper)
	return logger, fsWrapper, loader
}

func TestParseLines_ClassParsing(t *testing.T) {
	_, _, loader := setupTestLoader(t)

	// Test basic class with fields
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

	// Test that modifiers are ignored
	lines = []string{
		"public class TestClass {",
		"    private String privateField;",
		"    public String publicField;",
		"    protected String protectedField;",
		"    static String staticField;",
		"    final String finalField;",
		"}",
	}

	result = loader.parseLines(lines)

	class, ok := result["TestClass"].(map[string]any)
	if !ok {
		t.Fatalf("TestClass not found")
	}

	members = class["members"].(map[string]any)
	if len(members) != 5 {
		t.Fatalf("Expected 5 members, got %d", len(members))
	}

	expectedFields := []string{"privateField", "publicField", "protectedField", "staticField", "finalField"}
	for _, field := range expectedFields {
		if _, ok := members[field]; !ok {
			t.Errorf("Expected field '%s' not found", field)
		}
	}
}

func TestParseLines_Annotations(t *testing.T) {
	_, _, loader := setupTestLoader(t)

	tests := []struct {
		name           string
		lines          []string
		wantField      string
		wantFieldType  string
		wantFieldAbsent string
	}{
		{
			name: "@Builder.Default annotation is ignored",
			lines: []string{
				"public class TestClass {",
				"    @Builder.Default",
				"    private String name;",
				"}",
			},
			wantField:     "name",
			wantFieldType: "String",
		},
		{
			name: "@JsonProperty annotation uses jsonName",
			lines: []string{
				"public class TestClass {",
				"    @JsonProperty(\"jsonName\")",
				"    private String javaName;",
				"}",
			},
			wantField:       "jsonName",
			wantFieldType:   "String",
			wantFieldAbsent: "javaName",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := loader.parseLines(tt.lines)
			class := result["TestClass"].(map[string]any)
			members := class["members"].(map[string]any)

			if members[tt.wantField] != tt.wantFieldType {
				t.Errorf("Expected field '%s' to be '%s', got '%v'", tt.wantField, tt.wantFieldType, members[tt.wantField])
			}

			if tt.wantFieldAbsent != "" {
				if _, ok := members[tt.wantFieldAbsent]; ok {
					t.Errorf("Field '%s' should not exist", tt.wantFieldAbsent)
				}
			}
		})
	}
}

func TestParseLines_ComplexParsing(t *testing.T) {
	_, _, loader := setupTestLoader(t)

	tests := []struct {
		name         string
		lines        []string
		wantMembers  map[string]string
		wantCount    int
	}{
		{
			name: "generic declarations",
			lines: []string{
				"public class TestClass {",
				"    private List<String> stringList;",
				"    private Map<String, Migration> migrationMap;",
				"}",
			},
			wantMembers: map[string]string{
				"stringList":   "List<String>",
				"migrationMap": "Map<String,Migration>",
			},
			wantCount: 2,
		},
		{
			name: "comments and annotations are ignored",
			lines: []string{
				"// This is a comment",
				"/* Block comment */",
				"public class TestClass {",
				"    /**",
				"     * Javadoc comment",
				"     */",
				"    @Deprecated",
				"    private String name;",
				"}",
			},
			wantMembers: map[string]string{
				"name": "String",
			},
			wantCount: 1,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := loader.parseLines(tt.lines)
			class := result["TestClass"].(map[string]any)
			members := class["members"].(map[string]any)

			if len(members) != tt.wantCount {
				t.Fatalf("Expected %d members, got %d", tt.wantCount, len(members))
			}

			for field, expectedType := range tt.wantMembers {
				if members[field] != expectedType {
					t.Errorf("Expected field '%s' to be '%s', got '%v'", field, expectedType, members[field])
				}
			}
		})
	}
}

func TestParseLines_Enum(t *testing.T) {
	_, _, loader := setupTestLoader(t)

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
	_, fsWrapper, loader := setupTestLoader(t)

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

	tests := []struct {
		name     string
		input    string
		expected string
	}{
		// String types
		{"String", "String", "string"},
		{"char", "char", "string"},
		{"Character", "Character", "string"},
		{"Instant", "Instant", "string"},
		{"LocalDate", "LocalDate", "string"},
		{"LocalDateTime", "LocalDateTime", "string"},
		{"LocalTime", "LocalTime", "string"},
		// Integer types
		{"byte", "byte", "int"},
		{"Byte", "Byte", "int"},
		{"short", "short", "int"},
		{"Short", "Short", "int"},
		{"int", "int", "int"},
		{"Integer", "Integer", "int"},
		{"long", "long", "int"},
		{"Long", "Long", "int"},
		{"BigInteger", "BigInteger", "int"},
		// Float types
		{"float", "float", "float"},
		{"Float", "Float", "float"},
		{"double", "double", "float"},
		{"Double", "Double", "float"},
		{"BigDecimal", "BigDecimal", "float"},
		// Boolean types
		{"boolean", "boolean", "bool"},
		{"Boolean", "Boolean", "bool"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result, err := normalizeType(tt.input, classes, enums)
			if err != nil {
				t.Errorf("normalizeType(%s) failed: %v", tt.input, err)
			}
			if result != tt.expected {
				t.Errorf("normalizeType(%s) expected '%s', got '%v'", tt.input, tt.expected, result)
			}
		})
	}
}

func TestNormalizeType_KnownTypes(t *testing.T) {
	tests := []struct {
		name          string
		input         string
		classes       map[string]bool
		enums         map[string]bool
		expected      string
	}{
		{
			name:     "known enum",
			input:    "Status",
			classes:  map[string]bool{},
			enums:    map[string]bool{"Status": true},
			expected: "string",
		},
		{
			name:     "known model class",
			input:    "Migration",
			classes:  map[string]bool{"Migration": true},
			enums:    map[string]bool{},
			expected: "Migration",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result, err := normalizeType(tt.input, tt.classes, tt.enums)
			if err != nil {
				t.Errorf("normalizeType(%s) failed: %v", tt.input, err)
			}
			if result != tt.expected {
				t.Errorf("normalizeType(%s) expected '%s', got '%v'", tt.input, tt.expected, result)
			}
		})
	}
}

func TestNormalizeType_CollectionTypes(t *testing.T) {
	tests := []struct {
		name       string
		input      string
		classes    map[string]bool
		enums      map[string]bool
		wantType   string
		wantItems  string
	}{
		{
			name:      "List with primitive",
			input:     "List<String>",
			classes:   map[string]bool{},
			enums:     map[string]bool{},
			wantType:  "array",
			wantItems: "string",
		},
		{
			name:      "List with model",
			input:     "List<Migration>",
			classes:   map[string]bool{"Migration": true},
			enums:     map[string]bool{},
			wantType:  "array",
			wantItems: "Migration",
		},
		{
			name:      "Set with model",
			input:     "Set<Migration>",
			classes:   map[string]bool{"Migration": true},
			enums:     map[string]bool{},
			wantType:  "array",
			wantItems: "Migration",
		},
		{
			name:      "Array syntax",
			input:     "Migration[]",
			classes:   map[string]bool{"Migration": true},
			enums:     map[string]bool{},
			wantType:  "array",
			wantItems: "Migration",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result, err := normalizeType(tt.input, tt.classes, tt.enums)
			if err != nil {
				t.Errorf("normalizeType(%s) failed: %v", tt.input, err)
			}
			resultMap, ok := result.(map[string]any)
			if !ok {
				t.Fatalf("Expected map for %s, got %T", tt.input, result)
			}
			if resultMap["type"] != tt.wantType || resultMap["items"] != tt.wantItems {
				t.Errorf("%s not normalized correctly: got type=%v, items=%v, want type=%v, items=%v",
					tt.input, resultMap["type"], resultMap["items"], tt.wantType, tt.wantItems)
			}
		})
	}
}

func TestNormalizeType_MapTypes(t *testing.T) {
	tests := []struct {
		name       string
		input      string
		classes    map[string]bool
		enums      map[string]bool
		wantType   string
		wantValues string
	}{
		{
			name:       "Map with primitive values",
			input:      "Map<String, String>",
			classes:    map[string]bool{},
			enums:      map[string]bool{},
			wantType:   "object",
			wantValues: "string",
		},
		{
			name:       "Map with model values",
			input:      "Map<String, Migration>",
			classes:    map[string]bool{"Migration": true},
			enums:      map[string]bool{},
			wantType:   "object",
			wantValues: "Migration",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result, err := normalizeType(tt.input, tt.classes, tt.enums)
			if err != nil {
				t.Errorf("normalizeType(%s) failed: %v", tt.input, err)
			}
			resultMap, ok := result.(map[string]any)
			if !ok {
				t.Fatalf("Expected map for %s, got %T", tt.input, result)
			}
			if resultMap["type"] != tt.wantType || resultMap["values"] != tt.wantValues {
				t.Errorf("%s not normalized correctly: got type=%v, values=%v, want type=%v, values=%v",
					tt.input, resultMap["type"], resultMap["values"], tt.wantType, tt.wantValues)
			}
		})
	}
}

func TestNormalizeType_NestedStructures(t *testing.T) {
	classes := map[string]bool{"Migration": true}
	enums := map[string]bool{}

	tests := []struct {
		name          string
		input         string
		wantType      string
		wantNested    bool
		wantNestedType string
		wantNestedItem string
	}{
		{
			name:           "nested list",
			input:          "List<List<Migration>>",
			wantType:       "array",
			wantNested:     true,
			wantNestedType: "array",
			wantNestedItem: "Migration",
		},
		{
			name:           "map with list values",
			input:          "Map<String, List<Migration>>",
			wantType:       "object",
			wantNested:     true,
			wantNestedType: "array",
			wantNestedItem: "Migration",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result, err := normalizeType(tt.input, classes, enums)
			if err != nil {
				t.Errorf("normalizeType(%s) failed: %v", tt.input, err)
			}
			resultMap, ok := result.(map[string]any)
			if !ok {
				t.Fatalf("Expected map for %s, got %T", tt.input, result)
			}
			if resultMap["type"] != tt.wantType {
				t.Errorf("%s: expected type '%v', got '%v'", tt.input, tt.wantType, resultMap["type"])
			}
			if tt.wantNested {
				var nestedMap map[string]any
				if tt.wantType == "array" {
					nestedMap, ok = resultMap["items"].(map[string]any)
				} else {
					nestedMap, ok = resultMap["values"].(map[string]any)
				}
				if !ok {
					t.Fatalf("%s: expected nested structure to be a map", tt.input)
				}
				if nestedMap["type"] != tt.wantNestedType || nestedMap["items"] != tt.wantNestedItem {
					t.Errorf("%s: nested structure not normalized correctly", tt.input)
				}
			}
		})
	}
}

func TestNormalizeType_ErrorCases(t *testing.T) {
	classes := map[string]bool{}
	enums := map[string]bool{}

	tests := []struct {
		name        string
		input       string
		wantError   string
	}{
		{
			name:      "unknown class",
			input:     "UnknownClass",
			wantError: "ModelLoder_Unsupported_Type",
		},
		{
			name:      "list of unknown model",
			input:     "List<UnknownModel>",
			wantError: "ModelLoder_Normalizing_Collections",
		},
		{
			name:      "invalid map declaration",
			input:     "Map<String>",
			wantError: "ModelLoder_Invalid_Map_Format",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := normalizeType(tt.input, classes, enums)
			if err == nil {
				t.Errorf("Expected error for %s, got nil", tt.input)
			}
			if err.Error() != tt.wantError {
				t.Errorf("Expected error '%s', got '%v'", tt.wantError, err)
			}
		})
	}
}

func TestNormalizeAndValidateSchema_ValidSchemas(t *testing.T) {
	_, _, loader := setupTestLoader(t)

	// Test class with primitives
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

	// Test class referencing another class
	rawSchema = map[string]any{
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

	result, err = loader.NormalizeAndValidateSchema(rawSchema)
	if err != nil {
		t.Fatalf("NormalizeAndValidateSchema failed: %v", err)
	}

	deployment := result["Deployment"].(map[string]any)
	members = deployment["members"].(map[string]any)

	if members["migration"] != "Migration" {
		t.Errorf("Expected migration reference to be preserved, got '%v'", members["migration"])
	}

	// Test class referencing enum
	rawSchema = map[string]any{
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

	result, err = loader.NormalizeAndValidateSchema(rawSchema)
	if err != nil {
		t.Fatalf("NormalizeAndValidateSchema failed: %v", err)
	}

	task := result["Task"].(map[string]any)
	members = task["members"].(map[string]any)

	if members["status"] != "string" {
		t.Errorf("Expected status to be normalized to 'string', got '%v'", members["status"])
	}

	// Enum should not appear in output
	if _, ok := result["Status"]; ok {
		t.Errorf("Enum should not appear in normalized schema")
	}
}


func TestNormalizeAndValidateSchema_UnknownTypes(t *testing.T) {
	_, _, loader := setupTestLoader(t)

	tests := []struct {
		name   string
		schema map[string]any
	}{
		{
			name: "collection of unknown model",
			schema: map[string]any{
				"Deployment": map[string]any{
					"objType": "class",
					"members": map[string]any{
						"items": "List<UnknownModel>",
					},
				},
			},
		},
		{
			name: "map with unknown value type",
			schema: map[string]any{
				"Deployment": map[string]any{
					"objType": "class",
					"members": map[string]any{
						"metadata": "Map<String,UnknownModel>",
					},
				},
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := loader.NormalizeAndValidateSchema(tt.schema)
			if err == nil {
				t.Errorf("Expected error for %s, got nil", tt.name)
			}
			if !strings.Contains(err.Error(), "ModelLoader_Normalize_Type") {
				t.Errorf("Expected error to contain 'ModelLoader_Normalize_Type', got '%v'", err)
			}
		})
	}
}

func TestNormalizeAndValidateSchema_InvalidStructures(t *testing.T) {
	_, _, loader := setupTestLoader(t)

	tests := []struct {
		name      string
		schema    map[string]any
		wantError string
	}{
		{
			name: "malformed class structure",
			schema: map[string]any{
				"Person": "not a map",
			},
			wantError: "ModelLoder_Invalid_Class_Structure",
		},
		{
			name: "non-string field type",
			schema: map[string]any{
				"Person": map[string]any{
					"objType": "class",
					"members": map[string]any{
						"name": 123,
					},
				},
			},
			wantError: "ModelLoader_Invalid_Type_Representation",
		},
		{
			name: "unsupported field type",
			schema: map[string]any{
				"Person": map[string]any{
					"objType": "class",
					"members": map[string]any{
						"data": "UnsupportedType",
					},
				},
			},
			wantError: "ModelLoader_Normalize_Type",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := loader.NormalizeAndValidateSchema(tt.schema)
			if err == nil {
				t.Errorf("Expected error for %s, got nil", tt.name)
			}
			if strings.Contains(tt.wantError, "ModelLoader_Normalize_Type") {
				if !strings.Contains(err.Error(), tt.wantError) {
					t.Errorf("Expected error to contain '%s', got '%v'", tt.wantError, err)
				}
			} else {
				if err.Error() != tt.wantError {
					t.Errorf("Expected error '%s', got '%v'", tt.wantError, err)
				}
			}
		})
	}
}

func TestMarkEmbeddedClasses_ReferenceTypes(t *testing.T) {
	_, _, loader := setupTestLoader(t)

	tests := []struct {
		name     string
		schema   map[string]any
		check    func(schema map[string]any)
	}{
		{
			name: "direct reference",
			schema: map[string]any{
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
			},
			check: func(schema map[string]any) {
				deployment := schema["Deployment"].(map[string]any)
				migration := schema["Migration"].(map[string]any)
				if deployment["embedded"] != false {
					t.Errorf("Expected Deployment to not be embedded, got '%v'", deployment["embedded"])
				}
				if migration["embedded"] != true {
					t.Errorf("Expected Migration to be embedded, got '%v'", migration["embedded"])
				}
			},
		},
		{
			name: "reference through array",
			schema: map[string]any{
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
			},
			check: func(schema map[string]any) {
				migration := schema["Migration"].(map[string]any)
				if migration["embedded"] != true {
					t.Errorf("Expected Migration to be embedded through array, got '%v'", migration["embedded"])
				}
			},
		},
		{
			name: "reference through map values",
			schema: map[string]any{
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
			},
			check: func(schema map[string]any) {
				migration := schema["Migration"].(map[string]any)
				if migration["embedded"] != true {
					t.Errorf("Expected Migration to be embedded through map, got '%v'", migration["embedded"])
				}
			},
		},
		{
			name: "nested reference",
			schema: map[string]any{
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
			},
			check: func(schema map[string]any) {
				migration := schema["Migration"].(map[string]any)
				if migration["embedded"] != true {
					t.Errorf("Expected Migration to be embedded through nested structure, got '%v'", migration["embedded"])
				}
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			loader.MarkEmbeddedClasses(tt.schema)
			tt.check(tt.schema)
		})
	}
}

func TestMarkEmbeddedClasses_NoEmbeddingScenarios(t *testing.T) {
	_, _, loader := setupTestLoader(t)

	tests := []struct {
		name   string
		schema map[string]any
		check  func(schema map[string]any)
	}{
		{
			name: "unreferenced class",
			schema: map[string]any{
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
			},
			check: func(schema map[string]any) {
				deployment := schema["Deployment"].(map[string]any)
				migration := schema["Migration"].(map[string]any)
				if deployment["embedded"] != false {
					t.Errorf("Expected Deployment to not be embedded, got '%v'", deployment["embedded"])
				}
				if migration["embedded"] != false {
					t.Errorf("Expected Migration to not be embedded, got '%v'", migration["embedded"])
				}
			},
		},
		{
			name: "primitive types (no embedding)",
			schema: map[string]any{
				"Person": map[string]any{
					"objType": "class",
					"members": map[string]any{
						"name": "string",
						"age":  "int",
					},
				},
			},
			check: func(schema map[string]any) {
				person := schema["Person"].(map[string]any)
				if person["embedded"] != false {
					t.Errorf("Expected Person to not be embedded, got '%v'", person["embedded"])
				}
			},
		},
		{
			name: "enum not embedded",
			schema: map[string]any{
				"Task": map[string]any{
					"objType": "class",
					"members": map[string]any{
						"status": "string",
					},
				},
				"Status": map[string]any{
					"objType": "enum",
				},
			},
			check: func(schema map[string]any) {
				task := schema["Task"].(map[string]any)
				status := schema["Status"].(map[string]any)
				if task["embedded"] != false {
					t.Errorf("Expected Task to not be embedded, got '%v'", task["embedded"])
				}
				if _, ok := status["embedded"]; ok {
					t.Errorf("Enum should not have embedded flag")
				}
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			loader.MarkEmbeddedClasses(tt.schema)
			tt.check(tt.schema)
		})
	}
}

func TestMarkEmbeddedClasses_ComplexScenarios(t *testing.T) {
	_, _, loader := setupTestLoader(t)

	// Test self-reference
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

	// Test multiple references to same model
	schema = map[string]any{
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
