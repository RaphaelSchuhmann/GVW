package build

import (
	"strings"
	"testing"
)

func TestParseLines_BasicClassWithFields(t *testing.T) {
	_, _, loader := setupTestLoader()

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

	person := assertClassExists(t, result, "Person")

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

	assertSchemaField(t, result, "Person", "name", "String")
	assertSchemaField(t, result, "Person", "age", "int")
}

func TestParseLines_ModifiersIgnored(t *testing.T) {
	_, _, loader := setupTestLoader()

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

	class := assertClassExists(t, result, "TestClass")

	members, ok := class["members"].(map[string]any)
	if !ok {
		t.Fatalf("TestClass has no members map")
	}

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
	_, _, loader := setupTestLoader()

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
	_, _, loader := setupTestLoader()

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
	_, _, loader := setupTestLoader()

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

func TestLoadModel_EmptyPath(t *testing.T) {
	_, _, loader := setupTestLoader()

	_, err := loader.LoadModel("")
	if err == nil {
		t.Fatalf("Expected error for empty path, got nil")
	}
	if err.Error() != "ModelLoader_Invalid_Path" {
		t.Errorf("Expected error 'ModelLoader_Invalid_Path', got '%v'", err)
	}
}

func TestLoadModel_NonexistentPath(t *testing.T) {
	_, fsWrapper, loader := setupTestLoader()

	fsWrapper.dirOrFileExistsMap["/nonexistent/path.java"] = false
	_, err := loader.LoadModel("/nonexistent/path.java")
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

func assertNestedStructure(t *testing.T, resultMap map[string]any, wantType, wantNestedType, wantNestedItem string) {
	var nestedMap map[string]any
	var ok bool
	if wantType == "array" {
		nestedMap, ok = resultMap["items"].(map[string]any)
	} else {
		nestedMap, ok = resultMap["values"].(map[string]any)
	}
	if !ok {
		t.Fatalf("expected nested structure to be a map")
	}
	if nestedMap["type"] != wantNestedType || nestedMap["items"] != wantNestedItem {
		t.Errorf("nested structure not normalized correctly")
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
				assertNestedStructure(t, resultMap, tt.wantType, tt.wantNestedType, tt.wantNestedItem)
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

func TestNormalizeAndValidateSchema_ClassWithPrimitives(t *testing.T) {
	_, _, loader := setupTestLoader()

	rawSchema := map[string]any{
		"Person": createClassSchema("class", map[string]any{
			"name": "String",
			"age":  "int",
		}),
	}

	result, err := loader.NormalizeAndValidateSchema(rawSchema)
	if err != nil {
		t.Fatalf("NormalizeAndValidateSchema failed: %v", err)
	}

	assertClassExists(t, result, "Person")
	assertSchemaField(t, result, "Person", "name", "string")
	assertSchemaField(t, result, "Person", "age", "int")
}

func TestNormalizeAndValidateSchema_ClassReferencingAnotherClass(t *testing.T) {
	_, _, loader := setupTestLoader()

	rawSchema := map[string]any{
		"Deployment": createClassSchema("class", map[string]any{"migration": "Migration"}),
		"Migration": createClassSchema("class", map[string]any{"id": "String"}),
	}

	result, err := loader.NormalizeAndValidateSchema(rawSchema)
	if err != nil {
		t.Fatalf("NormalizeAndValidateSchema failed: %v", err)
	}

	assertClassExists(t, result, "Deployment")
	assertSchemaField(t, result, "Deployment", "migration", "Migration")
}

func TestNormalizeAndValidateSchema_ClassReferencingEnum(t *testing.T) {
	_, _, loader := setupTestLoader()

	rawSchema := map[string]any{
		"Task": createClassSchema("class", map[string]any{"status": "Status"}),
		"Status": createClassSchema("enum", nil),
	}

	result, err := loader.NormalizeAndValidateSchema(rawSchema)
	if err != nil {
		t.Fatalf("NormalizeAndValidateSchema failed: %v", err)
	}

	assertClassExists(t, result, "Task")
	assertSchemaField(t, result, "Task", "status", "string")

	if _, ok := result["Status"]; ok {
		t.Errorf("Enum should not appear in normalized schema")
	}
}


func TestNormalizeAndValidateSchema_UnknownTypes(t *testing.T) {
	_, _, loader := setupTestLoader()

	tests := []struct {
		name   string
		schema map[string]any
	}{
		{
			name: "collection of unknown model",
			schema: map[string]any{
				"Deployment": createClassSchema("class", map[string]any{
					"items": "List<UnknownModel>",
				}),
			},
		},
		{
			name: "map with unknown value type",
			schema: map[string]any{
				"Deployment": createClassSchema("class", map[string]any{
					"metadata": "Map<String,UnknownModel>",
				}),
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

func assertErrorContains(t *testing.T, err error, wantError string) {
	if err == nil {
		t.Errorf("Expected error, got nil")
	}
	if strings.Contains(wantError, "ModelLoader_Normalize_Type") {
		if !strings.Contains(err.Error(), wantError) {
			t.Errorf("Expected error to contain '%s', got '%v'", wantError, err)
		}
	} else {
		if err.Error() != wantError {
			t.Errorf("Expected error '%s', got '%v'", wantError, err)
		}
	}
}

func TestNormalizeAndValidateSchema_InvalidStructures(t *testing.T) {
	_, _, loader := setupTestLoader()

	tests := []struct {
		name      string
		schema    map[string]any
		wantError string
	}{
		{
			name:      "malformed class structure",
			schema:    map[string]any{"Person": "not a map"},
			wantError: "ModelLoder_Invalid_Class_Structure",
		},
		{
			name: "non-string field type",
			schema: map[string]any{
				"Person": createClassSchema("class", map[string]any{"name": 123}),
			},
			wantError: "ModelLoader_Invalid_Type_Representation",
		},
		{
			name: "unsupported field type",
			schema: map[string]any{
				"Person": createClassSchema("class", map[string]any{"data": "UnsupportedType"}),
			},
			wantError: "ModelLoader_Normalize_Type",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := loader.NormalizeAndValidateSchema(tt.schema)
			assertErrorContains(t, err, tt.wantError)
		})
	}
}

func TestMarkEmbeddedClasses_ReferenceTypes(t *testing.T) {
	_, _, loader := setupTestLoader()

	tests := []struct {
		name   string
		schema map[string]any
		check  func(schema map[string]any)
	}{
		{
			name: "direct reference",
			schema: map[string]any{
				"Deployment": createClassSchema("class", map[string]any{"migration": "Migration"}),
				"Migration": createClassSchema("class", map[string]any{"id": "String"}),
			},
			check: func(schema map[string]any) {
				assertClassNotEmbedded(t, schema, "Deployment")
				assertClassEmbedded(t, schema, "Migration")
			},
		},
		{
			name: "reference through array",
			schema: map[string]any{
				"Deployment": createClassSchema("class", map[string]any{
					"migrations": createFieldSchema("Migration"),
				}),
				"Migration": createClassSchema("class", map[string]any{"id": "String"}),
			},
			check: func(schema map[string]any) {
				assertClassEmbedded(t, schema, "Migration")
			},
		},
		{
			name: "reference through map values",
			schema: map[string]any{
				"Deployment": createClassSchema("class", map[string]any{
					"metadata": createMapSchema("Migration"),
				}),
				"Migration": createClassSchema("class", map[string]any{"id": "String"}),
			},
			check: func(schema map[string]any) {
				assertClassEmbedded(t, schema, "Migration")
			},
		},
		{
			name: "nested reference",
			schema: map[string]any{
				"Deployment": createClassSchema("class", map[string]any{
					"migrations": map[string]any{
						"type":  "array",
						"items": createFieldSchema("Migration"),
					},
				}),
				"Migration": createClassSchema("class", map[string]any{"id": "String"}),
			},
			check: func(schema map[string]any) {
				assertClassEmbedded(t, schema, "Migration")
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
	_, _, loader := setupTestLoader()

	tests := []struct {
		name   string
		schema map[string]any
		check  func(schema map[string]any)
	}{
		{
			name: "unreferenced class",
			schema: map[string]any{
				"Deployment": createClassSchema("class", map[string]any{"id": "String"}),
				"Migration": createClassSchema("class", map[string]any{"id": "String"}),
			},
			check: func(schema map[string]any) {
				assertClassNotEmbedded(t, schema, "Deployment")
				assertClassNotEmbedded(t, schema, "Migration")
			},
		},
		{
			name: "primitive types (no embedding)",
			schema: map[string]any{
				"Person": createClassSchema("class", map[string]any{
					"name": "string",
					"age":  "int",
				}),
			},
			check: func(schema map[string]any) {
				assertClassNotEmbedded(t, schema, "Person")
			},
		},
		{
			name: "enum not embedded",
			schema: map[string]any{
				"Task": createClassSchema("class", map[string]any{"status": "string"}),
				"Status": createClassSchema("enum", nil),
			},
			check: func(schema map[string]any) {
				assertClassNotEmbedded(t, schema, "Task")
				status := assertClassExists(t, schema, "Status")
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

func TestMarkEmbeddedClasses_SelfReference(t *testing.T) {
	_, _, loader := setupTestLoader()

	schema := map[string]any{
		"Node": createClassSchema("class", map[string]any{"parent": "Node"}),
	}

	loader.MarkEmbeddedClasses(schema)
	assertClassEmbedded(t, schema, "Node")
}

func TestMarkEmbeddedClasses_MultipleReferencesToSameModel(t *testing.T) {
	_, _, loader := setupTestLoader()

	schema := map[string]any{
		"Deployment": createClassSchema("class", map[string]any{"migration": "Migration"}),
		"Backup": createClassSchema("class", map[string]any{"migration": "Migration"}),
		"Migration": createClassSchema("class", map[string]any{"id": "String"}),
	}

	loader.MarkEmbeddedClasses(schema)
	assertClassNotEmbedded(t, schema, "Deployment")
	assertClassNotEmbedded(t, schema, "Backup")
	assertClassEmbedded(t, schema, "Migration")
}

func TestMarkEmbeddedClasses_TransitiveEmbedding(t *testing.T) {
	_, _, loader := setupTestLoader()

	schema := map[string]any{
		"Deployment": createClassSchema("class", map[string]any{
			"migrations": createFieldSchema("Migration"),
		}),
		"Migration": createClassSchema("class", map[string]any{"conditional": "Conditional"}),
		"Conditional": createClassSchema("class", map[string]any{"condition": "Branch"}),
		"Branch": createClassSchema("class", map[string]any{"id": "String"}),
	}

	loader.MarkEmbeddedClasses(schema)
	assertClassEmbedded(t, schema, "Migration")
	assertClassEmbedded(t, schema, "Conditional")
	assertClassEmbedded(t, schema, "Branch")
}
