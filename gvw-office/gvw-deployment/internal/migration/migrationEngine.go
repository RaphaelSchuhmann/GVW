package migration

import (
	"context"
	"errors"
	"fmt"
	"gvw/gvw-deployment/internal/api/client"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/filesystem"
	"gvw/gvw-deployment/internal/utils/log"
	"reflect"
	"slices"
	"strconv"
	"strings"
)

const bulkWriteBatchSize = 100

type migrationStats struct {
	Processed int
	Changed   int
}

type MigrationEngine struct {
	logger    *log.Logger
	fsWrapper filesystem.FileSystemWrapper
	validator *MigrationValidator
	db        *client.DbClient
}

// dbPlan is the concrete work for one database: the root models stored in it and the
// migrations after model prefixes and embedded models have been expanded into plain paths.
type dbPlan struct {
	Models     []string
	Migrations []Migration
}

func NewMigrationEngine(logger *log.Logger, fsWrapper filesystem.FileSystemWrapper, validator *MigrationValidator, dbClient *client.DbClient) *MigrationEngine {
	return &MigrationEngine{
		logger:    logger,
		fsWrapper: fsWrapper,
		validator: validator,
		db:        dbClient,
	}
}

func (e *MigrationEngine) LoadSchema(ctx context.Context, deploymentId string) (*Schema, error) {
	schemaPath := "./builds/" + deploymentId + "/schema.json"

	if err := ctx.Err(); err != nil {
		return nil, err
	}

	if !e.fsWrapper.DirOrFileExists(schemaPath) {
		e.logger.Printf("[MIGRATION] No schema found for deployment %s at path %s", deploymentId, schemaPath)
		return nil, gvwErrors.NewError(gvwErrors.CodeMigrationEngineNoBuildFound)
	}

	data, err := e.fsWrapper.ReadFile(schemaPath)
	if err != nil {
		e.logger.Printf("[MIGRATION] Error while reading schema for deployment %s: %v", deploymentId, err)
		return nil, gvwErrors.Wrap(gvwErrors.CodeMigrationEngineReadError, err)
	}

	schema, err := ParseSchema(data)
	if err != nil {
		e.logger.Printf("[MIGRATION] Error while parsing schema for deployment %s: %v", deploymentId, err)
		return nil, gvwErrors.Wrap(gvwErrors.CodeMigrationEngineJsonError, err)
	}

	return schema, nil
}

func (e *MigrationEngine) ValidateMigrations(ctx context.Context, migrations []Migration, schema *Schema) error {
	if err := ctx.Err(); err != nil {
		return err
	}

	if schema == nil || len(schema.Models) == 0 {
		return gvwErrors.Wrap(gvwErrors.CodeMigrationEngineJsonError, errors.New("schema contains no models"))
	}

	if e.validator.AreMigrationFieldsValid(migrations) {
		return gvwErrors.NewError(gvwErrors.CodeMigrationEngineInvalidField)
	}

	if e.validator.IsMigrationSequenceValid(migrations) {
		return gvwErrors.NewError(gvwErrors.CodeMigrationEngineInvalidSequence)
	}

	return nil
}

func (e *MigrationEngine) ExtractDatabasesFromMigrations(migrations []Migration) []string {
	var affectedDatabases []string

	for _, migration := range migrations {
		if slices.Contains(affectedDatabases, migration.Database) {
			continue
		}

		affectedDatabases = append(affectedDatabases, migration.Database)
	}

	return affectedDatabases
}

func (e *MigrationEngine) GenerateMigrationToDbMap(migrations []Migration, affectedDatabases []string) (map[string][]Migration, error) {
	result := make(map[string][]Migration, len(affectedDatabases))

	for _, key := range affectedDatabases {
		result[key] = nil
	}

	for _, value := range migrations {
		if _, ok := result[value.Database]; !ok {
			return nil, gvwErrors.NewError(gvwErrors.CodeMigrationEngineBadAffectedDBs)
		}
		result[value.Database] = append(result[value.Database], value)
	}

	return result, nil
}

// BuildPlans builds the plan of every affected database with the same steps the dry run uses,
// so a real run applies exactly the migrations that were tested.
func (e *MigrationEngine) BuildPlans(migrations []Migration, schema *Schema, modelToDbMap map[string]string) (map[string]dbPlan, error) {
	databases := e.ExtractDatabasesFromMigrations(migrations)

	if !e.validator.AllValuesPresent(modelToDbMap, databases) {
		return nil, gvwErrors.NewError(gvwErrors.CodeMigrationEngineMissingDbInMap)
	}

	dbToMigrations, err := e.GenerateMigrationToDbMap(migrations, databases)
	if err != nil {
		return nil, err
	}

	plans := make(map[string]dbPlan, len(dbToMigrations))
	for _, database := range databases {
		plan, err := e.PlanDatabaseMigrations(database, dbToMigrations[database], schema, modelToDbMap)
		if err != nil {
			return nil, fmt.Errorf("database %s: %w", database, err)
		}
		plans[database] = plan
	}

	return plans, nil
}

// PlanDatabaseMigrations expands the migrations of one database into concrete paths (model prefixes,
// embedded models), validates them against the schema and re-checks the sequence on the expanded list.
// Every error carries the id of the migration that caused it.
func (e *MigrationEngine) PlanDatabaseMigrations(database string, migrations []Migration, schema *Schema, modelToDbMap map[string]string) (dbPlan, error) {
	roots := schema.RootModelsFor(database, modelToDbMap)
	plan := dbPlan{Models: roots}

	if len(roots) == 0 {
		e.logger.Printf("[MIGRATION] No model of the schema is mapped to database %s: schema checks are skipped for it", database)
	}

	for _, migration := range migrations {
		expanded, err := schema.ExpandMigration(migration, roots)
		if err != nil {
			return dbPlan{}, fmt.Errorf("migration %s: %w", migration.Id, err)
		}

		for _, concrete := range expanded {
			if !e.validator.isMigrationValid(concrete) {
				return dbPlan{}, fmt.Errorf("migration %s: %w", migration.Id, gvwErrors.NewError(gvwErrors.CodeMigrationEngineInvalidField))
			}
			if err := e.validator.ValidateMigrationAgainstSchema(concrete, roots, schema); err != nil {
				return dbPlan{}, fmt.Errorf("migration %s: %w", migration.Id, err)
			}
			plan.Migrations = append(plan.Migrations, concrete)
		}
	}

	if !e.validator.IsMigrationSequenceValid(plan.Migrations) {
		return dbPlan{}, gvwErrors.NewError(gvwErrors.CodeMigrationEngineInvalidSequence)
	}

	return plan, nil
}

// TestMigrationsAgainstDatabase is the read-only run (dry run / pre-flight): nothing is written.
func (e *MigrationEngine) TestMigrationsAgainstDatabase(ctx context.Context, database string, plan dbPlan, schema *Schema) error {
	_, err := e.processDatabase(ctx, database, plan, schema, false)
	return err
}

// ExecuteMigrationsOnDatabase migrates every document of the database and writes the changed ones back.
// Documents are validated and checked against the schema before they are written, and unchanged
// documents are not written at all (their _rev stays the same).
func (e *MigrationEngine) ExecuteMigrationsOnDatabase(ctx context.Context, database string, plan dbPlan, schema *Schema) (migrationStats, error) {
	return e.processDatabase(ctx, database, plan, schema, true)
}

func (e *MigrationEngine) processDatabase(ctx context.Context, database string, plan dbPlan, schema *Schema, write bool) (migrationStats, error) {
	var stats migrationStats

	if err := ctx.Err(); err != nil {
		return stats, &gvwErrors.MigrationError{
			Err:        gvwErrors.NewError(gvwErrors.CodeCancelledError),
			DocumentId: "",
		}
	}

	query := map[string]any{
		"selector": map[string]any{"_id": map[string]any{"$gt": nil}},
	}

	batcher := NewBatchProcessor(e, database, &stats)

	for doc, err := range e.db.FindEachByQuery(ctx, database, query, 100) {
		if err != nil {
			return stats, &gvwErrors.MigrationError{
				Err:        gvwErrors.Wrap(gvwErrors.CodeMigrationEngineDbQueryError, err),
				DocumentId: "",
			}
		}

		updated, shouldProcess, documentId, err := e.evaluateAndPrepareDoc(ctx, doc, plan, schema, write)
		if err != nil {
			return stats, err
		}
		if documentId == "" {
			continue // Design document
		}
		stats.Processed++

		if shouldProcess {
			if err := batcher.Add(ctx, updated); err != nil {
				return stats, err
			}
		}
	}

	if write {
		if err := batcher.Flush(ctx); err != nil {
			return stats, err
		}
	}

	return stats, nil
}

func (e *MigrationEngine) evaluateAndPrepareDoc(ctx context.Context, doc map[string]any, plan dbPlan, schema *Schema, write bool) (updated map[string]any, shouldBatch bool, documentId string, err error) {
	documentId, ok := doc["_id"].(string)
	if !ok {
		return nil, false, "", &gvwErrors.MigrationError{
			Err:        gvwErrors.NewError(gvwErrors.CodeMigrationEngineDbQueryError),
			DocumentId: "",
		}
	}

	// Design documents are not model documents.
	if strings.HasPrefix(documentId, "_design/") {
		return nil, false, "", nil
	}

	updated, runErr := e.runMigrationsOnDoc(ctx, doc, plan.Migrations)
	if runErr != nil {
		if gvwErrors.HasCode(runErr, gvwErrors.CodeCancelledError) {
			return nil, false, "", &gvwErrors.MigrationError{Err: runErr, DocumentId: ""}
		}
		return nil, false, documentId, &gvwErrors.MigrationError{Err: runErr, DocumentId: documentId}
	}

	// The migrated document has to match the latest models.
	if confErr := e.checkConformance(updated, plan.Models, schema); confErr != nil {
		return nil, false, documentId, &gvwErrors.MigrationError{Err: confErr, DocumentId: documentId}
	}

	if !write || reflect.DeepEqual(doc, updated) {
		return updated, false, documentId, nil
	}

	return updated, true, documentId, nil
}

// checkConformance accepts the document if it matches at least one of the models stored in the database.
func (e *MigrationEngine) checkConformance(doc map[string]any, models []string, schema *Schema) error {
	if !enforceSchemaConformance || schema == nil || len(models) == 0 {
		return nil
	}

	var firstErr error
	for _, model := range models {
		err := schema.CheckDocument(model, doc)
		if err == nil {
			return nil
		}
		if firstErr == nil {
			firstErr = err
		}
	}
	return firstErr
}

func (e *MigrationEngine) runMigrationsOnDoc(ctx context.Context, doc map[string]any, migrations []Migration) (map[string]any, error) {
	if ctx.Err() != nil {
		return nil, gvwErrors.NewError(gvwErrors.CodeCancelledError)
	}

	updatedDoc := doc

	// Apply migrations on document
	for _, migration := range migrations {
		before := updatedDoc

		// Deep clone: migrations may change nested objects and `before` must stay untouched for validation.
		after, err := e.applyMigrationOnDoc(ctx, deepCloneMap(before), migration)
		if err != nil {
			if gvwErrors.HasCode(err, gvwErrors.CodeCancelledError) {
				return nil, err
			}
			return nil, gvwErrors.Wrap(gvwErrors.CodeMigrationEngineApplyMigrationsFailed, err)
		}

		// Validate that migration was successful
		if err := e.validator.ValidateMigrationApplication(ctx, before, after, migration); err != nil {
			if gvwErrors.HasCode(err, gvwErrors.CodeCancelledError) {
				return nil, err
			}
			return nil, gvwErrors.Wrap(gvwErrors.CodeMigrationEngineApplyMigrationsFailed, err)
		}

		updatedDoc = after
	}

	return updatedDoc, nil
}

func (e *MigrationEngine) applyMigrationOnDoc(ctx context.Context, doc map[string]any, migration Migration) (map[string]any, error) {
	if ctx.Err() != nil {
		return nil, gvwErrors.NewError(gvwErrors.CodeCancelledError)
	}

	switch migration.Action {
	case MigrationAdd:
		return e.applyAddMigration(doc, migration)
	case MigrationDelete:
		return e.applyDeleteMigration(doc, migration)
	case MigrationRename:
		return e.applyRenameMigration(doc, migration)
	case MigrationConditional:
		return e.applyConditionalMigration(doc, migration)
	default:
		return nil, gvwErrors.NewError(gvwErrors.CodeMigrationEngineInvalidAction)
	}
}

// For all actions below, a path whose intermediate objects do not exist (e.g. "recurrence.weekDay" on a document
// without recurrence) resolves to no objects and the migration is a no-op for that document.

func (e *MigrationEngine) applyAddMigration(doc map[string]any, migration Migration) (map[string]any, error) {
	parents, leaf := resolvePath(doc, migration.Field)

	for _, p := range parents {
		if _, ok := p[leaf]; ok {
			return nil, gvwErrors.NewError(gvwErrors.CodeMigrationEngineAddFieldAlreadyExists)
		}
	}
	for _, p := range parents {
		p[leaf] = nil
	}

	return doc, nil
}

func (e *MigrationEngine) applyDeleteMigration(doc map[string]any, migration Migration) (map[string]any, error) {
	parents, leaf := resolvePath(doc, migration.Field)

	// If the field does not exist it is technically still valid!
	for _, p := range parents {
		delete(p, leaf)
	}

	return doc, nil
}

func (e *MigrationEngine) applyRenameMigration(doc map[string]any, migration Migration) (map[string]any, error) {
	parents, leaf := resolvePath(doc, migration.Field)
	newName := migration.Value

	// Check everything first so a failing document is never half-renamed.
	for _, p := range parents {
		if _, ok := p[leaf]; !ok {
			return nil, gvwErrors.NewError(gvwErrors.CodeMigrationEngineRenamingFieldFailed)
		}
		if _, exists := p[newName]; exists && newName != leaf {
			return nil, gvwErrors.NewError(gvwErrors.CodeMigrationEngineRenameTargetExists)
		}
	}

	for _, p := range parents {
		val := p[leaf]
		delete(p, leaf)
		p[newName] = val
	}

	return doc, nil
}

// applyConditionalMigration evaluates the condition on every object that contains the condition field and
// sets the field of the chosen branch in that same object (the plan guarantees both live in the same object).
func (e *MigrationEngine) applyConditionalMigration(doc map[string]any, migration Migration) (map[string]any, error) {
	c := migration.Conditional
	scope := parentPath(c.Condition.Field)
	parents, condLeaf := resolvePath(doc, c.Condition.Field)

	for _, parent := range parents {
		cur, exists := parent[condLeaf]
		matches := exists && valueEquals(cur, c.Condition.Value)
		if c.Negated {
			matches = !matches
		}

		branch := c.FalseBranch
		if matches {
			branch = c.TrueBranch
		}

		if branch.Field == "" {
			continue
		}
		if parentPath(branch.Field) != scope {
			return nil, gvwErrors.NewError(gvwErrors.CodeMigrationEngineConditionalScopeMismatch)
		}

		leaf := leafOf(branch.Field)
		newVal, err := coerceLike(parent[leaf], branch.Value)
		if err != nil {
			return nil, gvwErrors.Wrap(gvwErrors.CodeMigrationEngineConditionalValueInvalid, err)
		}
		parent[leaf] = newVal
	}

	return doc, nil
}

func valueEquals(actual any, expected string) bool {
	switch v := actual.(type) {
	case string:
		return v == expected
	case float64:
		f, err := strconv.ParseFloat(expected, 64)
		return err == nil && v == f
	case bool:
		b, err := strconv.ParseBool(expected)
		return err == nil && v == b
	case nil:
		return expected == "" || expected == "null"
	default: // objects and arrays never equal a plain string
		return false
	}
}

func coerceLike(existing any, raw string) (any, error) {
	if raw == "" {
		switch existing.(type) {
		case float64, bool:
			return nil, nil
		}
		return "", nil
	}
	switch existing.(type) {
	case float64:
		return strconv.ParseFloat(raw, 64)
	case bool:
		return strconv.ParseBool(raw)
	default:
		return raw, nil
	}
}
