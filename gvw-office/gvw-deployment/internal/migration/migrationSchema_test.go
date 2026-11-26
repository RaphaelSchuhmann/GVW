package migration

import (
	"strings"
	"testing"
)

func TestParseSchema_Valid(t *testing.T) {
	schema := mustParseSchema(t, testSchemaJSON)

	if len(schema.Models) != 4 {
		t.Fatalf("expected 4 models, got %d", len(schema.Models))
	}

	event := schema.Models["Event"]
	if event == nil {
		t.Fatal("Event model missing")
	}
	if event.Embedded {
		t.Error("Event should not be embedded")
	}
	if len(event.Members) != 7 {
		t.Errorf("expected 7 members on Event, got %d", len(event.Members))
	}

	rec := schema.Models["Recurrence"]
	if rec == nil || !rec.Embedded {
		t.Error("Recurrence should exist and be embedded")
	}

	status := schema.Models["Status"]
	if status == nil || !status.IsEnum {
		t.Error("Status should exist and be an enum")
	}
}

func TestParseSchema_InvalidJSON(t *testing.T) {
	if _, err := ParseSchema([]byte("{not json")); err == nil {
		t.Fatal("expected error for invalid JSON")
	}
}

func TestParseSchema_SkipsNonModelEntries(t *testing.T) {
	data := `{
		"Event": {"objType": "class", "members": {"title": "string"}},
		"SomeEnum": {"objType": "enum", "constants": ["A", "B"]},
		"NotAModel": {"foo": "bar"}
	}`
	schema, err := ParseSchema([]byte(data))
	if err != nil {
		t.Fatalf("ParseSchema failed: %v", err)
	}
	if _, ok := schema.Models["NotAModel"]; ok {
		t.Error("NotAModel should have been skipped")
	}
	if _, ok := schema.Models["SomeEnum"]; !ok {
		t.Error("SomeEnum should be kept as enum")
	}
}

func TestParseSchema_TypeRefs(t *testing.T) {
	schema := mustParseSchema(t, testSchemaJSON)
	event := schema.Models["Event"]

	check := func(member string, wantKind typeKind) {
		t.Helper()
		tr := event.Members[member]
		if tr == nil {
			t.Fatalf("member %s missing", member)
		}
		if tr.Kind != wantKind {
			t.Errorf("member %s kind = %v, want %v", member, tr.Kind, wantKind)
		}
	}

	check("title", kindString)
	check("count", kindNumber)
	check("score", kindNumber)
	check("active", kindBool)
	check("recurrence", kindModel)
	check("tags", kindArray)
	check("meta", kindMap)

	if !event.Members["count"].IntOnly {
		t.Error("count should be IntOnly")
	}
	if event.Members["score"].IntOnly {
		t.Error("score should not be IntOnly")
	}
	if event.Members["recurrence"].Model != "Recurrence" {
		t.Errorf("recurrence model = %q", event.Members["recurrence"].Model)
	}
	if event.Members["tags"].Items == nil || event.Members["tags"].Items.Kind != kindString {
		t.Error("tags should be array of string")
	}
	if event.Members["meta"].Values == nil || event.Members["meta"].Values.Kind != kindString {
		t.Error("meta should be map of string")
	}
}

func TestRootModelsFor(t *testing.T) {
	schema := mustParseSchema(t, testSchemaJSON)
	m := testModelToDbMap()

	roots := schema.RootModelsFor("events-db", m)
	if len(roots) != 1 || roots[0] != "Event" {
		t.Errorf("events-db roots = %v, want [Event]", roots)
	}

	roots = schema.RootModelsFor("members-db", m)
	if len(roots) != 1 || roots[0] != "Member" {
		t.Errorf("members-db roots = %v, want [Member]", roots)
	}

	roots = schema.RootModelsFor("unknown-db", m)
	if len(roots) != 0 {
		t.Errorf("unknown-db roots = %v, want empty", roots)
	}
}

func TestPathExists(t *testing.T) {
	schema := mustParseSchema(t, testSchemaJSON)
	roots := []string{"Event"}

	exists := []string{
		"title",
		"recurrence.weekDay",
		"recurrence.interval",
		"tags",
		"meta.anything", // map values are dynamic
	}
	for _, p := range exists {
		if !schema.PathExists(roots, p) {
			t.Errorf("expected path %q to exist", p)
		}
	}

	notExists := []string{
		"bogus",
		"recurrence.bogus",
		"title.deeper", // scalar has no sub-paths
		"count.foo",
	}
	for _, p := range notExists {
		if schema.PathExists(roots, p) {
			t.Errorf("expected path %q to not exist", p)
		}
	}
}

func TestFieldType(t *testing.T) {
	schema := mustParseSchema(t, testSchemaJSON)
	roots := []string{"Event"}

	if tr := schema.FieldType(roots, "title"); tr == nil || tr.Kind != kindString {
		t.Errorf("title type = %v", tr)
	}
	if tr := schema.FieldType(roots, "count"); tr == nil || tr.Kind != kindNumber || !tr.IntOnly {
		t.Errorf("count type = %v", tr)
	}
	if tr := schema.FieldType(roots, "recurrence.weekDay"); tr == nil || tr.Kind != kindString {
		t.Errorf("recurrence.weekDay type = %v", tr)
	}
	if tr := schema.FieldType(roots, "tags"); tr == nil || tr.Kind != kindArray {
		t.Errorf("tags type = %v", tr)
	}
	if tr := schema.FieldType(roots, "bogus"); tr != nil {
		t.Errorf("bogus type should be nil, got %v", tr)
	}
}

func TestCoerceValue(t *testing.T) {
	schema := mustParseSchema(t, testSchemaJSON)
	roots := []string{"Event"}

	intType := schema.FieldType(roots, "count")
	if got := schema.CoerceValue(intType, "42"); got != int64(42) {
		t.Errorf("CoerceValue int = %v (%T), want int64(42)", got, got)
	}
	// IntOnly falls back to float parsing when the string is not a whole number
	if got := schema.CoerceValue(intType, "4.5"); got != 4.5 {
		t.Errorf("CoerceValue non-int numeric should become float, got %v", got)
	}
	if got := schema.CoerceValue(intType, "abc"); got != "abc" {
		t.Errorf("CoerceValue unparseable should stay string, got %v", got)
	}

	floatType := schema.FieldType(roots, "score")
	if got := schema.CoerceValue(floatType, "4.5"); got != 4.5 {
		t.Errorf("CoerceValue float = %v, want 4.5", got)
	}

	boolType := schema.FieldType(roots, "active")
	if got := schema.CoerceValue(boolType, "true"); got != true {
		t.Errorf("CoerceValue bool = %v, want true", got)
	}

	arrType := schema.FieldType(roots, "tags")
	if got := schema.CoerceValue(arrType, `["a","b"]`); got == nil {
		t.Error("CoerceValue array should parse JSON")
	} else if arr, ok := got.([]any); !ok || len(arr) != 2 {
		t.Errorf("CoerceValue array = %v", got)
	}

	// Non-string values pass through
	if got := schema.CoerceValue(intType, 7); got != 7 {
		t.Errorf("CoerceValue non-string = %v", got)
	}
	// nil type passes through
	if got := schema.CoerceValue(nil, "x"); got != "x" {
		t.Errorf("CoerceValue nil type = %v", got)
	}
}

func TestDefaultValue(t *testing.T) {
	schema := mustParseSchema(t, testSchemaJSON)
	roots := []string{"Event"}

	check := func(path string, want any) {
		t.Helper()
		tr := schema.FieldType(roots, path)
		got := schema.DefaultValue(tr)
		switch w := want.(type) {
		case string:
			if got != w {
				t.Errorf("DefaultValue(%s) = %v, want %v", path, got, w)
			}
		case int:
			if got != w {
				t.Errorf("DefaultValue(%s) = %v (%T), want %v", path, got, got, w)
			}
		case float64:
			if got != w {
				t.Errorf("DefaultValue(%s) = %v, want %v", path, got, w)
			}
		case bool:
			if got != w {
				t.Errorf("DefaultValue(%s) = %v, want %v", path, got, w)
			}
		}
	}

	check("title", "")
	check("count", 0)
	check("score", float64(0))
	check("active", false)

	if got := schema.DefaultValue(schema.FieldType(roots, "tags")); got == nil {
		t.Error("DefaultValue(tags) should be empty array, got nil")
	} else if arr, ok := got.([]any); !ok || len(arr) != 0 {
		t.Errorf("DefaultValue(tags) = %v", got)
	}

	if got := schema.DefaultValue(schema.FieldType(roots, "meta")); got == nil {
		t.Error("DefaultValue(meta) should be empty map, got nil")
	} else if m, ok := got.(map[string]any); !ok || len(m) != 0 {
		t.Errorf("DefaultValue(meta) = %v", got)
	}

	if got := schema.DefaultValue(schema.FieldType(roots, "recurrence")); got == nil {
		t.Error("DefaultValue(recurrence) should be empty map for model type")
	}

	if got := schema.DefaultValue(nil); got != nil {
		t.Errorf("DefaultValue(nil) = %v, want nil", got)
	}
}

func TestExpandMigration_PlainPath(t *testing.T) {
	schema := mustParseSchema(t, testSchemaJSON)
	roots := []string{"Event"}

	out, err := schema.ExpandMigration(addMigration("title"), roots)
	if err != nil {
		t.Fatalf("ExpandMigration failed: %v", err)
	}
	if len(out) != 1 || out[0].Field != "title" {
		t.Errorf("expected unchanged migration, got %+v", out)
	}
}

func TestExpandMigration_RootModelPrefix(t *testing.T) {
	schema := mustParseSchema(t, testSchemaJSON)
	roots := []string{"Event"}

	out, err := schema.ExpandMigration(addMigration("Event.title"), roots)
	if err != nil {
		t.Fatalf("ExpandMigration failed: %v", err)
	}
	if len(out) != 1 || out[0].Field != "title" {
		t.Errorf("expected model prefix stripped, got %+v", out)
	}
}

func TestExpandMigration_EmbeddedModel(t *testing.T) {
	schema := mustParseSchema(t, testSchemaJSON)
	roots := []string{"Event"}

	out, err := schema.ExpandMigration(addMigration("Recurrence.weekDay"), roots)
	if err != nil {
		t.Fatalf("ExpandMigration failed: %v", err)
	}
	if len(out) != 1 {
		t.Fatalf("expected 1 expanded migration, got %d", len(out))
	}
	if out[0].Field != "recurrence.weekDay" {
		t.Errorf("expected recurrence.weekDay, got %q", out[0].Field)
	}
}

func TestExpandMigration_EmbeddedModelLeaf(t *testing.T) {
	schema := mustParseSchema(t, testSchemaJSON)
	roots := []string{"Event"}

	// "Recurrence" alone expands to the prefix itself
	out, err := schema.ExpandMigration(addMigration("Recurrence"), roots)
	if err != nil {
		t.Fatalf("ExpandMigration failed: %v", err)
	}
	if len(out) != 1 || out[0].Field != "recurrence" {
		t.Errorf("expected recurrence, got %+v", out)
	}
}

func TestExpandMigration_Errors(t *testing.T) {
	schema := mustParseSchema(t, testSchemaJSON)
	roots := []string{"Event"}

	t.Run("model not in database", func(t *testing.T) {
		_, err := schema.ExpandMigration(addMigration("Member.name"), roots)
		if err == nil {
			t.Fatal("expected error for model not stored in this database")
		}
	})

	t.Run("root model without field", func(t *testing.T) {
		_, err := schema.ExpandMigration(addMigration("Event"), roots)
		if err == nil {
			t.Fatal("expected error for bare model name")
		}
	})

	t.Run("embedded model not used", func(t *testing.T) {
		// Status is an enum, not embedded; use a schema where embedded model is unused
		data := `{
			"Event": {"objType": "class", "members": {"title": "string"}},
			"Unused": {"embedded": true, "objType": "class", "members": {"x": "string"}}
		}`
		s := mustParseSchema(t, data)
		_, err := s.ExpandMigration(addMigration("Unused.x"), []string{"Event"})
		if err == nil {
			t.Fatal("expected error for unused embedded model")
		}
	})

	t.Run("mixed embedded and plain paths in conditional", func(t *testing.T) {
		m := conditionalMigration("Recurrence.weekDay", "mon", "title", "x", "title", "y", false)
		_, err := schema.ExpandMigration(m, roots)
		if err == nil {
			t.Fatal("expected error for mixed embedded/plain paths")
		}
	})
}

func TestExpandMigration_ConditionalAllEmbedded(t *testing.T) {
	schema := mustParseSchema(t, testSchemaJSON)
	roots := []string{"Event"}

	// All field refs reference the same embedded model -> expands per prefix
	m := conditionalMigration("Recurrence.weekDay", "mon", "Recurrence.interval", "1", "Recurrence.interval", "2", false)
	out, err := schema.ExpandMigration(m, roots)
	if err != nil {
		t.Fatalf("ExpandMigration failed: %v", err)
	}
	if len(out) != 1 {
		t.Fatalf("expected 1 migration, got %d", len(out))
	}
	c := out[0].Conditional
	if c.Condition.Field != "recurrence.weekDay" {
		t.Errorf("condition field = %q", c.Condition.Field)
	}
	if c.TrueBranch.Field != "recurrence.interval" {
		t.Errorf("true branch field = %q", c.TrueBranch.Field)
	}
}

func TestCheckDocument(t *testing.T) {
	schema := mustParseSchema(t, testSchemaJSON)

	valid := map[string]any{
		"_id":        "doc1",
		"_rev":       "1-abc",
		"title":      "hello",
		"count":      float64(3),
		"active":     true,
		"score":      1.5,
		"recurrence": map[string]any{"weekDay": "mon", "interval": float64(1)},
		"tags":       []any{"a", "b"},
		"meta":       map[string]any{"k": "v"},
	}
	if err := schema.CheckDocument("Event", valid); err != nil {
		t.Errorf("expected valid document, got %v", err)
	}

	t.Run("unknown member rejected", func(t *testing.T) {
		doc := map[string]any{"bogus": "x"}
		if err := schema.CheckDocument("Event", doc); err == nil {
			t.Error("expected error for unknown member")
		} else if !strings.Contains(err.Error(), "bogus") {
			t.Errorf("error should mention bogus member: %v", err)
		}
	})

	t.Run("underscore members allowed", func(t *testing.T) {
		doc := map[string]any{"_custom": "x", "title": "t"}
		if err := schema.CheckDocument("Event", doc); err != nil {
			t.Errorf("expected _-prefixed member to be allowed, got %v", err)
		}
	})

	t.Run("wrong type rejected", func(t *testing.T) {
		doc := map[string]any{"count": "not a number"}
		if err := schema.CheckDocument("Event", doc); err == nil {
			t.Error("expected error for wrong type")
		}
	})

	t.Run("fractional int rejected", func(t *testing.T) {
		doc := map[string]any{"count": 3.5}
		if err := schema.CheckDocument("Event", doc); err == nil {
			t.Error("expected error for fractional int")
		}
	})

	t.Run("null values allowed", func(t *testing.T) {
		doc := map[string]any{"title": nil, "count": nil}
		if err := schema.CheckDocument("Event", doc); err != nil {
			t.Errorf("expected null values to pass, got %v", err)
		}
	})

	t.Run("nested model checked", func(t *testing.T) {
		doc := map[string]any{"recurrence": map[string]any{"weekDay": 42}}
		if err := schema.CheckDocument("Event", doc); err == nil {
			t.Error("expected error for wrong nested type")
		}
	})

	t.Run("unknown model is a no-op", func(t *testing.T) {
		if err := schema.CheckDocument("NoSuchModel", map[string]any{"x": 1}); err != nil {
			t.Errorf("expected unknown model to pass, got %v", err)
		}
	})

	t.Run("enum member must be string", func(t *testing.T) {
		// Status enum used as member type would need a model referencing it;
		// check directly via a schema where Event has a Status member
		data := `{
			"Event": {"objType": "class", "members": {"state": "Status"}},
			"Status": {"objType": "enum"}
		}`
		s := mustParseSchema(t, data)
		if err := s.CheckDocument("Event", map[string]any{"state": "ACTIVE"}); err != nil {
			t.Errorf("expected string enum value to pass, got %v", err)
		}
		if err := s.CheckDocument("Event", map[string]any{"state": 1}); err == nil {
			t.Error("expected error for non-string enum value")
		}
	})
}
