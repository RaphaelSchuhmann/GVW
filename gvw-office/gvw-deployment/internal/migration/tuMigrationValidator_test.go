package migration

import (
	"context"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"testing"
)

// =========
// AreMigrationFieldsValid / isMigrationValid
// =========

func TestAreMigrationFieldsValid_EmptyList(t *testing.T) {
	v := newTestValidator()
	if !v.AreMigrationFieldsValid(nil) {
		t.Error("expected empty migration list to be valid")
	}
	if !v.AreMigrationFieldsValid([]Migration{}) {
		t.Error("expected empty migration list to be valid")
	}
}

func TestAreMigrationFieldsValid_AddDelete(t *testing.T) {
	v := newTestValidator()

	valid := []Migration{
		addMigration("title"),
		addMigration("recurrence.weekDay"),
		deleteMigration("meta.key"),
	}
	if !v.AreMigrationFieldsValid(valid) {
		t.Error("expected valid ADD/DELETE migrations to pass")
	}

	invalid := []Migration{
		{Action: MigrationAdd, Field: ""},
		{Action: MigrationAdd, Field: "a..b"},
		{Action: MigrationAdd, Field: "a. b"},
		{Action: MigrationAdd, Field: "_id"},
		{Action: MigrationAdd, Field: "_rev"},
		{Action: MigrationAdd, Field: "*"},
		{Action: MigrationAdd, Field: "a._id"},
		{Action: MigrationDelete, Field: ""},
		{Action: "UNKNOWN", Field: "title"},
		{Action: "", Field: "title"},
	}
	for _, m := range invalid {
		if v.AreMigrationFieldsValid([]Migration{m}) {
			t.Errorf("expected migration %+v to be invalid", m)
		}
	}
}

func TestAreMigrationFieldsValid_Rename(t *testing.T) {
	v := newTestValidator()

	if !v.AreMigrationFieldsValid([]Migration{renameMigration("oldName", "newName")}) {
		t.Error("expected valid rename to pass")
	}
	if !v.AreMigrationFieldsValid([]Migration{renameMigration("a.b.old", "new")}) {
		t.Error("expected valid nested rename to pass")
	}

	invalid := []Migration{
		renameMigration("", "newName"),
		renameMigration("old", ""),
		renameMigration("old", "has.dot"),
		renameMigration("old", "*"),
		renameMigration("old", "_id"),
		renameMigration("old", " padded "),
		renameMigration("_id", "newName"),
	}
	for _, m := range invalid {
		if v.AreMigrationFieldsValid([]Migration{m}) {
			t.Errorf("expected rename %+v to be invalid", m)
		}
	}
}

func TestAreMigrationFieldsValid_Conditional(t *testing.T) {
	v := newTestValidator()

	valid := conditionalMigration("type", "a", "kind", "x", "kind", "y", false)
	if !v.AreMigrationFieldsValid([]Migration{valid}) {
		t.Error("expected valid conditional migration to pass")
	}

	// Empty branch fields are allowed (branch does nothing)
	partial := conditionalMigration("type", "a", "", "", "kind", "y", false)
	if !v.AreMigrationFieldsValid([]Migration{partial}) {
		t.Error("expected conditional with empty true branch to pass")
	}

	invalid := []Migration{
		// condition field must be a valid read path
		conditionalMigration("", "a", "kind", "x", "kind", "y", false),
		conditionalMigration("a..b", "a", "kind", "x", "kind", "y", false),
		// branch fields must be valid write paths when present
		conditionalMigration("type", "a", "_id", "x", "kind", "y", false),
		conditionalMigration("type", "a", "kind", "x", "a..b", "y", false),
	}
	for _, m := range invalid {
		if v.AreMigrationFieldsValid([]Migration{m}) {
			t.Errorf("expected conditional %+v to be invalid", m)
		}
	}
}

// =========
// IsMigrationSequenceValid
// =========

func TestIsMigrationSequenceValid_EmptyAndSingle(t *testing.T) {
	v := newTestValidator()
	if !v.IsMigrationSequenceValid(nil) {
		t.Error("expected nil sequence to be valid")
	}
	if !v.IsMigrationSequenceValid([]Migration{addMigration("title")}) {
		t.Error("expected single migration to be valid")
	}
}

func TestIsMigrationSequenceValid_NoOverlap(t *testing.T) {
	v := newTestValidator()
	migrations := []Migration{
		addMigration("title"),
		addMigration("count"),
		deleteMigration("recurrence.weekDay"),
	}
	if !v.IsMigrationSequenceValid(migrations) {
		t.Error("expected non-overlapping migrations to be valid")
	}
}

func TestIsMigrationSequenceValid_OverlapSameAction(t *testing.T) {
	v := newTestValidator()
	migrations := []Migration{
		addMigration("title"),
		deleteMigration("title"),
	}
	if v.IsMigrationSequenceValid(migrations) {
		t.Error("expected overlapping non-conditional migrations to be invalid")
	}
}

func TestIsMigrationSequenceValid_OverlapDifferentDatabases(t *testing.T) {
	v := newTestValidator()
	m1 := addMigration("title")
	m2 := addMigration("title")
	m2.Database = "members-db"
	migrations := []Migration{m1, m2}
	if !v.IsMigrationSequenceValid(migrations) {
		t.Error("expected overlapping migrations on different databases to be valid")
	}
}

func TestIsMigrationSequenceValid_ConditionalOverlap(t *testing.T) {
	v := newTestValidator()

	// Two conditionals on the same field are allowed
	c1 := conditionalMigration("type", "a", "kind", "x", "kind", "y", false)
	c2 := conditionalMigration("type", "b", "other", "x", "other", "y", false)
	if !v.IsMigrationSequenceValid([]Migration{c1, c2}) {
		t.Error("expected two overlapping conditionals to be valid")
	}

	// Conditional + non-conditional overlap is a conflict
	if v.IsMigrationSequenceValid([]Migration{c1, addMigration("kind")}) {
		t.Error("expected conditional + add overlap to be invalid")
	}
}

func TestIsMigrationSequenceValid_PathPrefixOverlap(t *testing.T) {
	v := newTestValidator()
	// "recurrence" overlaps with "recurrence.weekDay"
	migrations := []Migration{
		addMigration("recurrence"),
		deleteMigration("recurrence.weekDay"),
	}
	if v.IsMigrationSequenceValid(migrations) {
		t.Error("expected prefix-overlapping paths to conflict")
	}
}

func TestIsMigrationSequenceValid_RenameOverlap(t *testing.T) {
	v := newTestValidator()
	// rename "old" -> "new" references both old and new paths
	migrations := []Migration{
		renameMigration("old", "new"),
		addMigration("new"),
	}
	if v.IsMigrationSequenceValid(migrations) {
		t.Error("expected rename target overlap to conflict")
	}
}

func TestIsMigrationSequenceValid_EmptyDatabaseSkipped(t *testing.T) {
	v := newTestValidator()
	m1 := addMigration("title")
	m1.Database = ""
	m2 := addMigration("title")
	if !v.IsMigrationSequenceValid([]Migration{m1, m2}) {
		t.Error("expected migration with empty database to be skipped in sequence check")
	}
}

// =========
// AllValuesPresent
// =========

func TestAllValuesPresent(t *testing.T) {
	v := newTestValidator()
	modelToDb := map[string]string{"A": "db1", "B": "db2", "C": "db1"}

	if !v.AllValuesPresent(modelToDb, []string{"db1", "db2"}) {
		t.Error("expected all present values to pass")
	}
	if v.AllValuesPresent(modelToDb, []string{"db1", "db3"}) {
		t.Error("expected missing value to fail")
	}
	if !v.AllValuesPresent(modelToDb, nil) {
		t.Error("expected empty items to pass")
	}
}

// =========
// ValidateMigrationAgainstSchema
// =========

func TestValidateMigrationAgainstSchema(t *testing.T) {
	v := newTestValidator()
	schema := mustParseSchema(t, testSchemaJSON)
	roots := []string{"Event"}

	t.Run("nil schema is a no-op", func(t *testing.T) {
		if err := v.ValidateMigrationAgainstSchema(addMigration("bogus"), roots, nil); err != nil {
			t.Errorf("expected nil schema to skip validation, got %v", err)
		}
	})

	t.Run("empty roots is a no-op", func(t *testing.T) {
		if err := v.ValidateMigrationAgainstSchema(addMigration("bogus"), nil, schema); err != nil {
			t.Errorf("expected empty roots to skip validation, got %v", err)
		}
	})

	t.Run("add existing field passes", func(t *testing.T) {
		if err := v.ValidateMigrationAgainstSchema(addMigration("title"), roots, schema); err != nil {
			t.Errorf("expected existing field to pass, got %v", err)
		}
	})

	t.Run("add nested existing field passes", func(t *testing.T) {
		if err := v.ValidateMigrationAgainstSchema(addMigration("recurrence.weekDay"), roots, schema); err != nil {
			t.Errorf("expected nested existing field to pass, got %v", err)
		}
	})

	t.Run("add unknown field fails", func(t *testing.T) {
		err := v.ValidateMigrationAgainstSchema(addMigration("bogus"), roots, schema)
		if err == nil {
			t.Fatal("expected error for unknown field")
		}
		if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineFieldNotInSchema) {
			t.Errorf("expected FieldNotInSchema code, got %v", err)
		}
	})

	t.Run("rename to existing sibling passes", func(t *testing.T) {
		if err := v.ValidateMigrationAgainstSchema(renameMigration("title", "count"), roots, schema); err != nil {
			t.Errorf("expected rename to existing field to pass, got %v", err)
		}
	})

	t.Run("rename to unknown sibling fails", func(t *testing.T) {
		err := v.ValidateMigrationAgainstSchema(renameMigration("title", "bogus"), roots, schema)
		if err == nil {
			t.Fatal("expected error for rename to unknown field")
		}
		if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineFieldNotInSchema) {
			t.Errorf("expected FieldNotInSchema code, got %v", err)
		}
	})

	t.Run("conditional fields checked", func(t *testing.T) {
		good := conditionalMigration("title", "a", "count", "1", "active", "true", false)
		if err := v.ValidateMigrationAgainstSchema(good, roots, schema); err != nil {
			t.Errorf("expected valid conditional to pass, got %v", err)
		}

		bad := conditionalMigration("bogus", "a", "count", "1", "active", "true", false)
		err := v.ValidateMigrationAgainstSchema(bad, roots, schema)
		if err == nil {
			t.Fatal("expected error for conditional with unknown condition field")
		}
		if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineFieldNotInSchema) {
			t.Errorf("expected FieldNotInSchema code, got %v", err)
		}
	})

	t.Run("delete is not schema-checked", func(t *testing.T) {
		if err := v.ValidateMigrationAgainstSchema(deleteMigration("bogus"), roots, schema); err != nil {
			t.Errorf("expected delete to skip schema check, got %v", err)
		}
	})
}

// =========
// ValidateMigrationApplication
// =========

func TestValidateMigrationApplication_UnknownAction(t *testing.T) {
	v := newTestValidator()
	m := Migration{Action: "BOGUS", Field: "x"}
	err := v.ValidateMigrationApplication(context.Background(), map[string]any{}, map[string]any{}, m, nil)
	if err == nil {
		t.Fatal("expected error for unknown action")
	}
	if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineErrorApplicationValidationFailed) {
		t.Errorf("unexpected error code: %v", err)
	}
}

func TestValidateMigrationApplication_CancelledContext(t *testing.T) {
	v := newTestValidator()
	ctx, cancel := context.WithCancel(context.Background())
	cancel()
	err := v.ValidateMigrationApplication(ctx, map[string]any{}, map[string]any{}, addMigration("x"), nil)
	if !gvwErrors.HasCode(err, gvwErrors.CodeCancelledError) {
		t.Errorf("expected cancelled error, got %v", err)
	}
}

func TestValidateMigrationApplication_Add(t *testing.T) {
	v := newTestValidator()
	m := addMigration("b")

	t.Run("valid add", func(t *testing.T) {
		before := map[string]any{"a": float64(1)}
		after := map[string]any{"a": float64(1), "b": "default"}
		if err := v.ValidateMigrationApplication(context.Background(), before, after, m, "default"); err != nil {
			t.Errorf("expected valid add, got %v", err)
		}
	})

	t.Run("field already existed", func(t *testing.T) {
		before := map[string]any{"a": float64(1), "b": "old"}
		after := map[string]any{"a": float64(1), "b": "default"}
		if err := v.ValidateMigrationApplication(context.Background(), before, after, m, "default"); err == nil {
			t.Error("expected error when field already existed")
		}
	})

	t.Run("field missing after", func(t *testing.T) {
		before := map[string]any{"a": float64(1)}
		after := map[string]any{"a": float64(1)}
		if err := v.ValidateMigrationApplication(context.Background(), before, after, m, "default"); err == nil {
			t.Error("expected error when field missing after migration")
		}
	})

	t.Run("wrong default value", func(t *testing.T) {
		before := map[string]any{"a": float64(1)}
		after := map[string]any{"a": float64(1), "b": "other"}
		if err := v.ValidateMigrationApplication(context.Background(), before, after, m, "default"); err == nil {
			t.Error("expected error for wrong default value")
		}
	})

	t.Run("unrelated change detected", func(t *testing.T) {
		before := map[string]any{"a": float64(1)}
		after := map[string]any{"a": float64(2), "b": "default"}
		if err := v.ValidateMigrationApplication(context.Background(), before, after, m, "default"); err == nil {
			t.Error("expected error for unrelated change")
		}
	})

	t.Run("nested add", func(t *testing.T) {
		before := map[string]any{"o": map[string]any{"x": float64(1)}}
		after := map[string]any{"o": map[string]any{"x": float64(1), "b": float64(0)}}
		nested := addMigration("o.b")
		if err := v.ValidateMigrationApplication(context.Background(), before, after, nested, float64(0)); err != nil {
			t.Errorf("expected valid nested add, got %v", err)
		}
	})
}

func TestValidateMigrationApplication_Delete(t *testing.T) {
	v := newTestValidator()
	m := deleteMigration("b")

	t.Run("valid delete", func(t *testing.T) {
		before := map[string]any{"a": float64(1), "b": "x"}
		after := map[string]any{"a": float64(1)}
		if err := v.ValidateMigrationApplication(context.Background(), before, after, m, nil); err != nil {
			t.Errorf("expected valid delete, got %v", err)
		}
	})

	t.Run("field still present", func(t *testing.T) {
		before := map[string]any{"a": float64(1), "b": "x"}
		after := map[string]any{"a": float64(1), "b": "x"}
		if err := v.ValidateMigrationApplication(context.Background(), before, after, m, nil); err == nil {
			t.Error("expected error when field still present")
		}
	})

	t.Run("delete of missing field is valid", func(t *testing.T) {
		before := map[string]any{"a": float64(1)}
		after := map[string]any{"a": float64(1)}
		if err := v.ValidateMigrationApplication(context.Background(), before, after, m, nil); err != nil {
			t.Errorf("expected delete of absent field to pass, got %v", err)
		}
	})
}

func TestValidateMigrationApplication_Rename(t *testing.T) {
	v := newTestValidator()
	m := renameMigration("old", "new")

	t.Run("valid rename", func(t *testing.T) {
		before := map[string]any{"old": "value", "keep": float64(1)}
		after := map[string]any{"new": "value", "keep": float64(1)}
		if err := v.ValidateMigrationApplication(context.Background(), before, after, m, nil); err != nil {
			t.Errorf("expected valid rename, got %v", err)
		}
	})

	t.Run("source missing", func(t *testing.T) {
		before := map[string]any{"keep": float64(1)}
		after := map[string]any{"new": "value", "keep": float64(1)}
		if err := v.ValidateMigrationApplication(context.Background(), before, after, m, nil); err == nil {
			t.Error("expected error when source field missing")
		}
	})

	t.Run("value changed during rename", func(t *testing.T) {
		before := map[string]any{"old": "value"}
		after := map[string]any{"new": "different"}
		if err := v.ValidateMigrationApplication(context.Background(), before, after, m, nil); err == nil {
			t.Error("expected error when value changed during rename")
		}
	})

	t.Run("old field still present", func(t *testing.T) {
		before := map[string]any{"old": "value"}
		after := map[string]any{"old": "value", "new": "value"}
		if err := v.ValidateMigrationApplication(context.Background(), before, after, m, nil); err == nil {
			t.Error("expected error when old field still present")
		}
	})
}

func TestValidateMigrationApplication_Conditional(t *testing.T) {
	v := newTestValidator()
	m := conditionalMigration("type", "a", "kind", "x", "kind", "y", false)

	t.Run("true branch applied", func(t *testing.T) {
		before := map[string]any{"type": "a"}
		after := map[string]any{"type": "a", "kind": "x"}
		if err := v.ValidateMigrationApplication(context.Background(), before, after, m, nil); err != nil {
			t.Errorf("expected valid conditional, got %v", err)
		}
	})

	t.Run("false branch applied", func(t *testing.T) {
		before := map[string]any{"type": "b"}
		after := map[string]any{"type": "b", "kind": "y"}
		if err := v.ValidateMigrationApplication(context.Background(), before, after, m, nil); err != nil {
			t.Errorf("expected valid conditional, got %v", err)
		}
	})

	t.Run("value matching neither branch", func(t *testing.T) {
		before := map[string]any{"type": "a"}
		after := map[string]any{"type": "a", "kind": "z"}
		if err := v.ValidateMigrationApplication(context.Background(), before, after, m, nil); err == nil {
			t.Error("expected error for value matching neither branch")
		}
	})

	t.Run("change outside allowed fields", func(t *testing.T) {
		before := map[string]any{"type": "a"}
		after := map[string]any{"type": "a", "kind": "x", "other": "changed"}
		if err := v.ValidateMigrationApplication(context.Background(), before, after, m, nil); err == nil {
			t.Error("expected error for change outside allowed fields")
		}
	})

	t.Run("no change at all is valid", func(t *testing.T) {
		before := map[string]any{"type": "a"}
		after := map[string]any{"type": "a"}
		if err := v.ValidateMigrationApplication(context.Background(), before, after, m, nil); err != nil {
			t.Errorf("expected no-op conditional to pass, got %v", err)
		}
	})
}
