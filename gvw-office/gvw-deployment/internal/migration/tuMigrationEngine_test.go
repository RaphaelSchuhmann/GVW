package migration

import (
	"context"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"testing"
)

// =========
// ExtractDatabasesFromMigrations
// =========

func TestExtractDatabasesFromMigrations(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())

	migrations := []Migration{
		{Database: "db1"},
		{Database: "db2"},
		{Database: "db1"},
		{Database: "db3"},
		{Database: "db2"},
	}

	got := e.ExtractDatabasesFromMigrations(migrations)
	want := []string{"db1", "db2", "db3"}
	if len(got) != len(want) {
		t.Fatalf("got %v, want %v", got, want)
	}
	for i := range want {
		if got[i] != want[i] {
			t.Fatalf("got %v, want %v", got, want)
		}
	}
}

func TestExtractDatabasesFromMigrations_Empty(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	if got := e.ExtractDatabasesFromMigrations(nil); len(got) != 0 {
		t.Errorf("expected empty, got %v", got)
	}
}

// =========
// GenerateMigrationToDbMap
// =========

func TestGenerateMigrationToDbMap(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())

	migrations := []Migration{
		{Id: "1", Database: "db1", Field: "a"},
		{Id: "2", Database: "db2", Field: "b"},
		{Id: "3", Database: "db1", Field: "c"},
	}

	result, err := e.GenerateMigrationToDbMap(migrations, []string{"db1", "db2"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(result["db1"]) != 2 || len(result["db2"]) != 1 {
		t.Errorf("unexpected grouping: %v", result)
	}
}

func TestGenerateMigrationToDbMap_UnknownDatabase(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())

	migrations := []Migration{{Id: "1", Database: "dbX", Field: "a"}}
	_, err := e.GenerateMigrationToDbMap(migrations, []string{"db1"})
	if err == nil {
		t.Fatal("expected error for migration with unknown database")
	}
	if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineBadAffectedDBs) {
		t.Errorf("expected BadAffectedDBs code, got %v", err)
	}
}

// =========
// LoadSchema
// =========

func TestLoadSchema_NotFound(t *testing.T) {
	fs := newMockFileSystem()
	e := newTestEngine(t, fs)

	_, err := e.LoadSchema(context.Background(), "dep-1")
	if err == nil {
		t.Fatal("expected error for missing schema")
	}
	if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineNoBuildFound) {
		t.Errorf("expected NoBuildFound code, got %v", err)
	}
}

func TestLoadSchema_ReadError(t *testing.T) {
	fs := newMockFileSystem()
	path := "./builds/dep-1/schema.json"
	fs.exists[path] = true
	fs.readErr[path] = gvwErrors.NewError(gvwErrors.CodeFileSystemError)
	e := newTestEngine(t, fs)

	_, err := e.LoadSchema(context.Background(), "dep-1")
	if err == nil {
		t.Fatal("expected error for unreadable schema")
	}
	if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineReadError) {
		t.Errorf("expected ReadError code, got %v", err)
	}
}

func TestLoadSchema_InvalidJSON(t *testing.T) {
	fs := newMockFileSystem()
	fs.addFile("./builds/dep-1/schema.json", []byte("{broken"))
	e := newTestEngine(t, fs)

	_, err := e.LoadSchema(context.Background(), "dep-1")
	if err == nil {
		t.Fatal("expected error for invalid schema JSON")
	}
	if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineJsonError) {
		t.Errorf("expected JsonError code, got %v", err)
	}
}

func TestLoadSchema_Success(t *testing.T) {
	fs := newMockFileSystem()
	fs.addFile("./builds/dep-1/schema.json", []byte(testSchemaJSON))
	e := newTestEngine(t, fs)

	schema, err := e.LoadSchema(context.Background(), "dep-1")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if schema.Models["Event"] == nil {
		t.Error("expected Event model in schema")
	}
}

func TestLoadSchema_CancelledContext(t *testing.T) {
	fs := newMockFileSystem()
	e := newTestEngine(t, fs)

	ctx, cancel := context.WithCancel(context.Background())
	cancel()

	_, err := e.LoadSchema(ctx, "dep-1")
	if err == nil {
		t.Fatal("expected error for cancelled context")
	}
}

// =========
// ValidateMigrations
// =========

func TestValidateMigrations_EmptySchema(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())

	if err := e.ValidateMigrations(context.Background(), []Migration{addMigration("title")}, nil); err == nil {
		t.Fatal("expected error for nil schema")
	}
	if err := e.ValidateMigrations(context.Background(), []Migration{addMigration("title")}, &Schema{}); err == nil {
		t.Fatal("expected error for schema without models")
	}
}

func TestValidateMigrations_InvalidFields(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	schema := mustParseSchema(t, testSchemaJSON)

	err := e.ValidateMigrations(context.Background(), []Migration{{Action: "BOGUS"}}, schema)
	if err == nil {
		t.Fatal("expected error for invalid migration fields")
	}
	if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineInvalidField) {
		t.Errorf("expected InvalidField code, got %v", err)
	}
}

func TestValidateMigrations_InvalidSequence(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	schema := mustParseSchema(t, testSchemaJSON)

	migrations := []Migration{
		addMigration("title"),
		deleteMigration("title"),
	}
	err := e.ValidateMigrations(context.Background(), migrations, schema)
	if err == nil {
		t.Fatal("expected error for invalid sequence")
	}
	if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineInvalidSequence) {
		t.Errorf("expected InvalidSequence code, got %v", err)
	}
}

func TestValidateMigrations_Valid(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	schema := mustParseSchema(t, testSchemaJSON)

	migrations := []Migration{
		addMigration("title"),
		deleteMigration("count"),
	}
	if err := e.ValidateMigrations(context.Background(), migrations, schema); err != nil {
		t.Errorf("expected valid migrations, got %v", err)
	}
}

func TestValidateMigrations_CancelledContext(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	schema := mustParseSchema(t, testSchemaJSON)

	ctx, cancel := context.WithCancel(context.Background())
	cancel()

	if err := e.ValidateMigrations(ctx, []Migration{addMigration("title")}, schema); err == nil {
		t.Fatal("expected error for cancelled context")
	}
}

// =========
// BuildPlans / PlanDatabaseMigrations
// =========

func TestBuildPlans_MissingDbInMap(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	schema := mustParseSchema(t, testSchemaJSON)

	migrations := []Migration{addMigration("title")} // database: events-db
	_, err := e.BuildPlans(migrations, schema, map[string]string{"Event": "other-db"})
	if err == nil {
		t.Fatal("expected error for missing db in map")
	}
	if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineMissingDbInMap) {
		t.Errorf("expected MissingDbInMap code, got %v", err)
	}
}

func TestBuildPlans_Success(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	schema := mustParseSchema(t, testSchemaJSON)

	migrations := []Migration{
		addMigration("title"),
		{Id: "m2", Database: "members-db", Action: MigrationAdd, Field: "Member.name"},
	}

	plans, err := e.BuildPlans(migrations, schema, testModelToDbMap())
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(plans) != 2 {
		t.Fatalf("expected 2 plans, got %d", len(plans))
	}
	if plans["events-db"].Models[0] != "Event" {
		t.Errorf("events-db models = %v", plans["events-db"].Models)
	}
	if plans["members-db"].Models[0] != "Member" {
		t.Errorf("members-db models = %v", plans["members-db"].Models)
	}
}

func TestPlanDatabaseMigrations_PlainField(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	schema := mustParseSchema(t, testSchemaJSON)
	roots := schema.RootModelsFor("events-db", testModelToDbMap())

	plan, err := e.PlanDatabaseMigrations("events-db", []Migration{addMigration("title")}, schema, testModelToDbMap())
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(plan.Migrations) != 1 || plan.Migrations[0].Field != "title" {
		t.Errorf("unexpected plan migrations: %+v", plan.Migrations)
	}
	if len(plan.Defaults) != 1 || plan.Defaults[0] != "" {
		t.Errorf("expected string default \"\", got %v", plan.Defaults)
	}
	_ = roots
}

func TestPlanDatabaseMigrations_ModelPrefixStripped(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	schema := mustParseSchema(t, testSchemaJSON)

	m := addMigration("Event.count")
	plan, err := e.PlanDatabaseMigrations("events-db", []Migration{m}, schema, testModelToDbMap())
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if plan.Migrations[0].Field != "count" {
		t.Errorf("expected count, got %q", plan.Migrations[0].Field)
	}
	// int field default is 0 (int)
	if plan.Defaults[0] != 0 {
		t.Errorf("expected int default 0, got %v (%T)", plan.Defaults[0], plan.Defaults[0])
	}
}

func TestPlanDatabaseMigrations_EmbeddedExpansion(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	schema := mustParseSchema(t, testSchemaJSON)

	m := addMigration("Recurrence.weekDay")
	plan, err := e.PlanDatabaseMigrations("events-db", []Migration{m}, schema, testModelToDbMap())
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(plan.Migrations) != 1 || plan.Migrations[0].Field != "recurrence.weekDay" {
		t.Errorf("unexpected expanded migrations: %+v", plan.Migrations)
	}
}

func TestPlanDatabaseMigrations_FieldNotInSchema(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	schema := mustParseSchema(t, testSchemaJSON)

	m := addMigration("Event.bogus")
	_, err := e.PlanDatabaseMigrations("events-db", []Migration{m}, schema, testModelToDbMap())
	if err == nil {
		t.Fatal("expected error for field not in schema")
	}
	if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineFieldNotInSchema) {
		t.Errorf("expected FieldNotInSchema code, got %v", err)
	}
}

func TestPlanDatabaseMigrations_InvalidSequenceAfterExpansion(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	schema := mustParseSchema(t, testSchemaJSON)

	// Two migrations that expand to overlapping paths
	migrations := []Migration{
		addMigration("title"),
		deleteMigration("title"),
	}
	_, err := e.PlanDatabaseMigrations("events-db", migrations, schema, testModelToDbMap())
	if err == nil {
		t.Fatal("expected error for invalid sequence")
	}
	if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineInvalidSequence) {
		t.Errorf("expected InvalidSequence code, got %v", err)
	}
}

func TestPlanDatabaseMigrations_DefaultValueUsed(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	schema := mustParseSchema(t, testSchemaJSON)

	m := Migration{
		Id:           "m1",
		Database:     "events-db",
		Action:       MigrationAdd,
		Field:        "count",
		DefaultValue: "42",
	}
	plan, err := e.PlanDatabaseMigrations("events-db", []Migration{m}, schema, testModelToDbMap())
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if plan.Defaults[0] != int64(42) {
		t.Errorf("expected coerced default int64(42), got %v (%T)", plan.Defaults[0], plan.Defaults[0])
	}
}

func TestPlanDatabaseMigrations_NoRootsSkipsSchemaCheck(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	schema := mustParseSchema(t, testSchemaJSON)

	// Database with no mapped models: schema checks are skipped
	m := addMigration("anything.at.all")
	plan, err := e.PlanDatabaseMigrations("empty-db", []Migration{m}, schema, testModelToDbMap())
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(plan.Migrations) != 1 {
		t.Errorf("expected 1 migration, got %d", len(plan.Migrations))
	}
}

// =========
// applyMigrationOnDoc / runMigrationsOnDoc
// =========

func TestApplyAddMigration(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())

	t.Run("adds field to root", func(t *testing.T) {
		doc := map[string]any{"a": float64(1)}
		out, err := e.applyAddMigration(doc, addMigration("b"), "default")
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if out["b"] != "default" {
			t.Errorf("expected b=default, got %v", out["b"])
		}
	})

	t.Run("adds field to nested object", func(t *testing.T) {
		doc := map[string]any{"o": map[string]any{"x": float64(1)}}
		out, err := e.applyAddMigration(doc, addMigration("o.b"), float64(0))
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		nested := out["o"].(map[string]any)
		if nested["b"] != float64(0) {
			t.Errorf("expected o.b=0, got %v", nested["b"])
		}
	})

	t.Run("adds field to every array element", func(t *testing.T) {
		doc := map[string]any{
			"items": []any{
				map[string]any{"x": float64(1)},
				map[string]any{"x": float64(2)},
			},
		}
		out, err := e.applyAddMigration(doc, addMigration("items.tag"), "t")
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		items := out["items"].([]any)
		for i, item := range items {
			if item.(map[string]any)["tag"] != "t" {
				t.Errorf("item %d missing tag", i)
			}
		}
	})

	t.Run("existing field fails", func(t *testing.T) {
		doc := map[string]any{"b": "old"}
		_, err := e.applyAddMigration(doc, addMigration("b"), "new")
		if err == nil {
			t.Fatal("expected error for existing field")
		}
		if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineAddFieldAlreadyExists) {
			t.Errorf("expected AddFieldAlreadyExists code, got %v", err)
		}
	})

	t.Run("missing intermediate is a no-op", func(t *testing.T) {
		doc := map[string]any{"a": float64(1)}
		out, err := e.applyAddMigration(doc, addMigration("missing.field"), "x")
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if _, ok := out["missing"]; ok {
			t.Error("expected no 'missing' key to be created")
		}
	})
}

func TestApplyDeleteMigration(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())

	t.Run("deletes existing field", func(t *testing.T) {
		doc := map[string]any{"a": float64(1), "b": "x"}
		out, err := e.applyDeleteMigration(doc, deleteMigration("b"))
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if _, ok := out["b"]; ok {
			t.Error("expected b to be deleted")
		}
	})

	t.Run("deletes from array elements", func(t *testing.T) {
		doc := map[string]any{
			"items": []any{
				map[string]any{"tag": "x", "keep": float64(1)},
				map[string]any{"tag": "y", "keep": float64(2)},
			},
		}
		out, err := e.applyDeleteMigration(doc, deleteMigration("items.tag"))
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		for _, item := range out["items"].([]any) {
			if _, ok := item.(map[string]any)["tag"]; ok {
				t.Error("expected tag to be deleted from all items")
			}
		}
	})

	t.Run("missing field is a no-op", func(t *testing.T) {
		doc := map[string]any{"a": float64(1)}
		out, err := e.applyDeleteMigration(doc, deleteMigration("b"))
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if out["a"] != float64(1) {
			t.Error("expected doc unchanged")
		}
	})
}

func TestApplyRenameMigration(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())

	t.Run("renames field", func(t *testing.T) {
		doc := map[string]any{"old": "value", "keep": float64(1)}
		out, err := e.applyRenameMigration(doc, renameMigration("old", "new"))
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if out["new"] != "value" {
			t.Errorf("expected new=value, got %v", out["new"])
		}
		if _, ok := out["old"]; ok {
			t.Error("expected old to be removed")
		}
	})

	t.Run("missing source fails", func(t *testing.T) {
		doc := map[string]any{"keep": float64(1)}
		_, err := e.applyRenameMigration(doc, renameMigration("old", "new"))
		if err == nil {
			t.Fatal("expected error for missing source")
		}
		if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineRenamingFieldFailed) {
			t.Errorf("expected RenamingFieldFailed code, got %v", err)
		}
	})

	t.Run("existing target fails", func(t *testing.T) {
		doc := map[string]any{"old": "v1", "new": "v2"}
		_, err := e.applyRenameMigration(doc, renameMigration("old", "new"))
		if err == nil {
			t.Fatal("expected error for existing target")
		}
		if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineRenameTargetExists) {
			t.Errorf("expected RenameTargetExists code, got %v", err)
		}
	})

	t.Run("rename to same name is allowed", func(t *testing.T) {
		doc := map[string]any{"same": "v"}
		out, err := e.applyRenameMigration(doc, renameMigration("same", "same"))
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if out["same"] != "v" {
			t.Errorf("expected same=v, got %v", out["same"])
		}
	})
}

func TestApplyConditionalMigration(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())

	t.Run("true branch sets value", func(t *testing.T) {
		doc := map[string]any{"type": "a"}
		m := conditionalMigration("type", "a", "kind", "x", "kind", "y", false)
		out, err := e.applyConditionalMigration(doc, m)
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if out["kind"] != "x" {
			t.Errorf("expected kind=x, got %v", out["kind"])
		}
	})

	t.Run("false branch sets value", func(t *testing.T) {
		doc := map[string]any{"type": "b"}
		m := conditionalMigration("type", "a", "kind", "x", "kind", "y", false)
		out, err := e.applyConditionalMigration(doc, m)
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if out["kind"] != "y" {
			t.Errorf("expected kind=y, got %v", out["kind"])
		}
	})

	t.Run("negated condition flips branches", func(t *testing.T) {
		doc := map[string]any{"type": "a"}
		m := conditionalMigration("type", "a", "kind", "x", "kind", "y", true)
		out, err := e.applyConditionalMigration(doc, m)
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if out["kind"] != "y" {
			t.Errorf("expected kind=y with negation, got %v", out["kind"])
		}
	})

	t.Run("numeric condition value", func(t *testing.T) {
		doc := map[string]any{"count": float64(5)}
		m := conditionalMigration("count", "5", "kind", "big", "kind", "small", false)
		out, err := e.applyConditionalMigration(doc, m)
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if out["kind"] != "big" {
			t.Errorf("expected kind=big, got %v", out["kind"])
		}
	})

	t.Run("bool condition value", func(t *testing.T) {
		doc := map[string]any{"active": true}
		m := conditionalMigration("active", "true", "kind", "on", "kind", "off", false)
		out, err := e.applyConditionalMigration(doc, m)
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if out["kind"] != "on" {
			t.Errorf("expected kind=on, got %v", out["kind"])
		}
	})

	t.Run("missing condition field uses false branch", func(t *testing.T) {
		doc := map[string]any{}
		m := conditionalMigration("type", "a", "kind", "x", "kind", "y", false)
		out, err := e.applyConditionalMigration(doc, m)
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if out["kind"] != "y" {
			t.Errorf("expected kind=y, got %v", out["kind"])
		}
	})

	t.Run("empty branch field is skipped", func(t *testing.T) {
		doc := map[string]any{"type": "a"}
		m := conditionalMigration("type", "a", "", "", "kind", "y", false)
		out, err := e.applyConditionalMigration(doc, m)
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if _, ok := out["kind"]; ok {
			t.Error("expected no kind to be set when true branch is empty")
		}
	})

	t.Run("scope mismatch fails", func(t *testing.T) {
		doc := map[string]any{"type": "a"}
		// condition at root, branch nested -> scope mismatch
		m := Migration{
			Id:       "m1",
			Database: "events-db",
			Action:   MigrationConditional,
			Conditional: ConditionalMigration{
				Condition:   Branch{Field: "type", Value: "a"},
				TrueBranch:  Branch{Field: "nested.kind", Value: "x"},
				FalseBranch: Branch{Field: "", Value: ""},
			},
		}
		_, err := e.applyConditionalMigration(doc, m)
		if err == nil {
			t.Fatal("expected scope mismatch error")
		}
		if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineConditionalScopeMismatch) {
			t.Errorf("expected ConditionalScopeMismatch code, got %v", err)
		}
	})

	t.Run("per-element evaluation in arrays", func(t *testing.T) {
		doc := map[string]any{
			"items": []any{
				map[string]any{"type": "a"},
				map[string]any{"type": "b"},
			},
		}
		m := conditionalMigration("items.type", "a", "items.kind", "x", "items.kind", "y", false)
		out, err := e.applyConditionalMigration(doc, m)
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		items := out["items"].([]any)
		if items[0].(map[string]any)["kind"] != "x" {
			t.Errorf("item 0 kind = %v, want x", items[0].(map[string]any)["kind"])
		}
		if items[1].(map[string]any)["kind"] != "y" {
			t.Errorf("item 1 kind = %v, want y", items[1].(map[string]any)["kind"])
		}
	})
}

func TestApplyMigrationOnDoc_UnknownAction(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	_, err := e.applyMigrationOnDoc(context.Background(), map[string]any{}, Migration{Action: "BOGUS"}, nil)
	if err == nil {
		t.Fatal("expected error for unknown action")
	}
	if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineInvalidAction) {
		t.Errorf("expected InvalidAction code, got %v", err)
	}
}

func TestRunMigrationsOnDoc_SequentialApplication(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	ctx := context.Background()

	// ADD then DELETE on the same doc
	plan := dbPlan{
		Migrations: []Migration{
			addMigration("temp"),
			deleteMigration("temp"),
		},
		Defaults: []any{"value", nil},
	}

	doc := map[string]any{"_id": "d1", "a": float64(1)}
	out, err := e.runMigrationsOnDoc(ctx, doc, plan)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if _, ok := out["temp"]; ok {
		t.Error("expected temp to be added then deleted")
	}
	if out["a"] != float64(1) {
		t.Error("expected original fields preserved")
	}
	// original doc must be untouched (deep clone per migration)
	if _, ok := doc["temp"]; ok {
		t.Error("original doc was mutated")
	}
}

func TestRunMigrationsOnDoc_ValidationFailure(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	ctx := context.Background()

	// ADD a field that already exists -> apply fails
	plan := dbPlan{
		Migrations: []Migration{addMigration("a")},
		Defaults:   []any{"x"},
	}
	doc := map[string]any{"_id": "d1", "a": float64(1)}

	_, err := e.runMigrationsOnDoc(ctx, doc, plan)
	if err == nil {
		t.Fatal("expected error for add on existing field")
	}
	if !gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineApplyMigrationsFailed) {
		t.Errorf("expected ApplyMigrationsFailed code, got %v", err)
	}
}

func TestRunMigrationsOnDoc_CancelledContext(t *testing.T) {
	e := newTestEngine(t, newMockFileSystem())
	ctx, cancel := context.WithCancel(context.Background())
	cancel()

	plan := dbPlan{Migrations: []Migration{addMigration("x")}, Defaults: []any{"v"}}
	_, err := e.runMigrationsOnDoc(ctx, map[string]any{}, plan)
	if !gvwErrors.HasCode(err, gvwErrors.CodeCancelledError) {
		t.Errorf("expected cancelled error, got %v", err)
	}
}
