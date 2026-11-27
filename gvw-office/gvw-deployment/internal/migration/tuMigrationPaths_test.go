package migration

import (
	"reflect"
	"testing"
)

func TestSplitPath(t *testing.T) {
	tests := []struct {
		name string
		path string
		want []string
	}{
		{"simple", "title", []string{"title"}},
		{"nested", "recurrence.weekDay", []string{"recurrence", "weekDay"}},
		{"deep", "a.b.c", []string{"a", "b", "c"}},
		{"escaped dot", `a\.b.c`, []string{"a.b", "c"}},
		{"escaped backslash", `a\\b`, []string{`a\b`}},
		{"trailing dot", "a.", []string{"a", ""}},
		{"empty", "", []string{""}},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := splitPath(tt.path)
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("splitPath(%q) = %v, want %v", tt.path, got, tt.want)
			}
		})
	}
}

func TestParentPath(t *testing.T) {
	tests := []struct {
		path string
		want string
	}{
		{"title", ""},
		{"recurrence.weekDay", "recurrence"},
		{"a.b.c", "a.b"},
		{"", ""},
	}

	for _, tt := range tests {
		if got := parentPath(tt.path); got != tt.want {
			t.Errorf("parentPath(%q) = %q, want %q", tt.path, got, tt.want)
		}
	}
}

func TestLeafOf(t *testing.T) {
	tests := []struct {
		path string
		want string
	}{
		{"title", "title"},
		{"recurrence.weekDay", "weekDay"},
		{"a.b.c", "c"},
		{"", ""},
	}

	for _, tt := range tests {
		if got := leafOf(tt.path); got != tt.want {
			t.Errorf("leafOf(%q) = %q, want %q", tt.path, got, tt.want)
		}
	}
}

func TestSiblingPath(t *testing.T) {
	tests := []struct {
		path    string
		newLeaf string
		want    string
	}{
		{"title", "name", "name"},
		{"recurrence.weekDay", "month", "recurrence.month"},
		{"a.b.c", "d", "a.b.d"},
		{"title", "with.dot", `with\.dot`},
	}

	for _, tt := range tests {
		if got := siblingPath(tt.path, tt.newLeaf); got != tt.want {
			t.Errorf("siblingPath(%q, %q) = %q, want %q", tt.path, tt.newLeaf, got, tt.want)
		}
	}
}

func TestJoinPath(t *testing.T) {
	tests := []struct {
		parent string
		name   string
		want   string
	}{
		{"", "title", "title"},
		{"recurrence", "weekDay", "recurrence.weekDay"},
		{"a", "b.c", `a.b\.c`},
		{"", "a.b", `a\.b`},
	}

	for _, tt := range tests {
		if got := joinPath(tt.parent, tt.name); got != tt.want {
			t.Errorf("joinPath(%q, %q) = %q, want %q", tt.parent, tt.name, got, tt.want)
		}
	}
}

func TestResolvePath(t *testing.T) {
	doc := map[string]any{
		"title": "hello",
		"recurrence": map[string]any{
			"weekDay": "mon",
		},
		"items": []any{
			map[string]any{"name": "first"},
			map[string]any{"name": "second"},
		},
		"categories": map[string]any{
			"a": map[string]any{"icon": "i1"},
			"b": map[string]any{"icon": "i2"},
		},
	}

	t.Run("top level field", func(t *testing.T) {
		parents, leaf := resolvePath(doc, "title")
		if leaf != "title" {
			t.Fatalf("leaf = %q, want title", leaf)
		}
		if len(parents) != 1 || parents[0]["title"] != "hello" {
			t.Errorf("unexpected parents: %v", parents)
		}
	})

	t.Run("nested field", func(t *testing.T) {
		parents, leaf := resolvePath(doc, "recurrence.weekDay")
		if leaf != "weekDay" {
			t.Fatalf("leaf = %q, want weekDay", leaf)
		}
		if len(parents) != 1 || parents[0]["weekDay"] != "mon" {
			t.Errorf("unexpected parents: %v", parents)
		}
	})

	t.Run("array transparency", func(t *testing.T) {
		parents, leaf := resolvePath(doc, "items.name")
		if leaf != "name" {
			t.Fatalf("leaf = %q, want name", leaf)
		}
		if len(parents) != 2 {
			t.Fatalf("expected 2 parents, got %d", len(parents))
		}
		if parents[0]["name"] != "first" || parents[1]["name"] != "second" {
			t.Errorf("unexpected parents: %v", parents)
		}
	})

	t.Run("wildcard map", func(t *testing.T) {
		parents, leaf := resolvePath(doc, "categories.*.icon")
		if leaf != "icon" {
			t.Fatalf("leaf = %q, want icon", leaf)
		}
		if len(parents) != 2 {
			t.Fatalf("expected 2 parents, got %d", len(parents))
		}
	})

	t.Run("missing intermediate yields no parents", func(t *testing.T) {
		parents, _ := resolvePath(doc, "missing.field")
		if len(parents) != 0 {
			t.Errorf("expected no parents, got %v", parents)
		}
	})
}

func TestFlattenDoc(t *testing.T) {
	doc := map[string]any{
		"title": "hello",
		"nested": map[string]any{
			"count": float64(3),
		},
		"items": []any{
			map[string]any{"name": "first"},
			map[string]any{"name": "second"},
		},
		"emptyArr": []any{},
	}

	flat := flattenDoc(doc)

	if !reflect.DeepEqual(flat["title"], []any{"hello"}) {
		t.Errorf("title = %v", flat["title"])
	}
	if !reflect.DeepEqual(flat["nested.count"], []any{float64(3)}) {
		t.Errorf("nested.count = %v", flat["nested.count"])
	}
	if !reflect.DeepEqual(flat["items.name"], []any{"first", "second"}) {
		t.Errorf("items.name = %v", flat["items.name"])
	}
	if !reflect.DeepEqual(flat["emptyArr"], []any{[]any{}}) {
		t.Errorf("emptyArr = %v", flat["emptyArr"])
	}
}

func TestPathAllowed(t *testing.T) {
	tests := []struct {
		path     string
		patterns []string
		want     bool
	}{
		{"title", []string{"title"}, true},
		{"title", []string{"name"}, false},
		{"recurrence.weekDay", []string{"recurrence"}, true},
		{"recurrence.weekDay", []string{"recurrence.weekDay"}, true},
		{"recurrence.weekDay", []string{"recurrence.month"}, false},
		{"a.b.c", []string{"a.b"}, true},
		{"a.b.c", []string{"a.b.c.d"}, false},
		{"categories.icon", []string{"categories.*"}, true},
		{"categories.sub.icon", []string{"categories.*.icon"}, true},
		{"categories.icon", []string{"categories.*.icon"}, false}, // pattern deeper than path
		{"other.icon", []string{"categories.*.icon"}, false},
	}

	for _, tt := range tests {
		if got := pathAllowed(tt.path, tt.patterns); got != tt.want {
			t.Errorf("pathAllowed(%q, %v) = %v, want %v", tt.path, tt.patterns, got, tt.want)
		}
	}
}

func TestPathsOverlap(t *testing.T) {
	tests := []struct {
		a, b string
		want bool
	}{
		{"title", "title", true},
		{"title", "name", false},
		{"recurrence", "recurrence.weekDay", true},
		{"recurrence.weekDay", "recurrence", true},
		{"recurrence.weekDay", "recurrence.month", false},
		{"a.b.c", "a.b", true},
		{"categories.*.icon", "categories.foo.icon", true},
		{"categories.*.icon", "categories.foo.name", false},
		{"items.name", "items.*", true},
	}

	for _, tt := range tests {
		if got := pathsOverlap(tt.a, tt.b); got != tt.want {
			t.Errorf("pathsOverlap(%q, %q) = %v, want %v", tt.a, tt.b, got, tt.want)
		}
		// overlap must be symmetric
		if got := pathsOverlap(tt.b, tt.a); got != tt.want {
			t.Errorf("pathsOverlap(%q, %q) = %v, want %v (symmetric)", tt.b, tt.a, got, tt.want)
		}
	}
}

func TestDeepCloneMap(t *testing.T) {
	original := map[string]any{
		"a": float64(1),
		"nested": map[string]any{
			"b": "x",
		},
		"list": []any{
			map[string]any{"c": true},
		},
	}

	clone := deepCloneMap(original)

	if !reflect.DeepEqual(original, clone) {
		t.Fatalf("clone differs from original: %v vs %v", clone, original)
	}

	// Mutating the clone must not affect the original
	clone["nested"].(map[string]any)["b"] = "changed"
	clone["list"].([]any)[0].(map[string]any)["c"] = false

	if original["nested"].(map[string]any)["b"] != "x" {
		t.Error("nested map was not deep cloned")
	}
	if original["list"].([]any)[0].(map[string]any)["c"] != true {
		t.Error("slice elements were not deep cloned")
	}
}

func TestDeepCloneMap_Nil(t *testing.T) {
	if deepCloneMap(nil) != nil {
		t.Error("expected nil for nil input")
	}
}
