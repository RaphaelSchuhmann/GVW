package migration

import (
	"context"
	"encoding/json"
	"gvw/gvw-deployment/internal/api/client"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/filesystem"
	"gvw/gvw-deployment/internal/utils/log"
	"maps"
	"slices"
	"strconv"
)

type MigrationEngine struct {
	logger    *log.Logger
	fsWrapper filesystem.FileSystemWrapper
	validator *MigrationValidator
	db        *client.DbClient
}

func NewMigrationEngine(logger *log.Logger, fsWrapper filesystem.FileSystemWrapper, validator *MigrationValidator, dbClient *client.DbClient) *MigrationEngine {
	return &MigrationEngine{
		logger:    logger,
		fsWrapper: fsWrapper,
		validator: validator,
		db:        dbClient,
	}
}

func (e *MigrationEngine) LoadSchema(ctx context.Context, deploymentId string) (map[string]any, error) {
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

	var result map[string]any

	if err := json.Unmarshal(data, &result); err != nil {
		e.logger.Printf("[MIGRATION] Error while unmarshaling schema for deployment %s: %v", deploymentId, err)
		return nil, gvwErrors.Wrap(gvwErrors.CodeMigrationEngineJsonError, err)
	}

	return result, nil
}

func (e *MigrationEngine) ValidateMigrations(ctx context.Context, migrations []Migration, schema map[string]any) error {
	if err := ctx.Err(); err != nil {
		return err
	}

	if isValid := e.validator.AreMigrationFieldsValid(migrations); !isValid {
		return gvwErrors.NewError(gvwErrors.CodeMigrationEngineInvalidField)
	}

	if isValid := e.validator.IsMigrationSequenceValid(migrations); !isValid {
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

func (e *MigrationEngine) TestMigrationsAgainstDatabase(ctx context.Context, database string, migrations []Migration) error {
	if ctx.Err() != nil {
		return &gvwErrors.MigrationError{
			Err:        gvwErrors.NewError(gvwErrors.CodeCancelledError),
			DocumentId: "",
		}
	}

	query := make(map[string]any)

	for doc, err := range e.db.FindEachByQuery(ctx, database, query, 100) {
		if err != nil {
			return &gvwErrors.MigrationError{
				Err:        gvwErrors.Wrap(gvwErrors.CodeMigrationEngineDbQueryError, err),
				DocumentId: "",
			}
		}

		var documentId string
		if id, ok := doc["_id"].(string); ok {
			documentId = id
		} else {
			return &gvwErrors.MigrationError{
				Err:        gvwErrors.NewError(gvwErrors.CodeMigrationEngineDbQueryError),
				DocumentId: "",
			}
		}

		_, err := e.runMigrationsOnDoc(ctx, doc, migrations)
		if err != nil {
			if gvwErrors.HasCode(err, gvwErrors.CodeCancelledError) {
				return &gvwErrors.MigrationError{
					Err:        err,
					DocumentId: "",
				}
			}
			return &gvwErrors.MigrationError{
				Err:        err,
				DocumentId: documentId,
			}
		}
	}

	return nil
}

func (e *MigrationEngine) runMigrationsOnDoc(ctx context.Context, doc map[string]any, migrations []Migration) (map[string]any, error) {
	if ctx.Err() != nil {
		return nil, gvwErrors.NewError(gvwErrors.CodeCancelledError)
	}

	updatedDoc := doc

	// Apply migrations on document
	for _, migration := range migrations {
		before := updatedDoc

		after, err := e.applyMigrationOnDoc(ctx, maps.Clone(before), migration)
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
		if _, ok := doc[migration.Field]; ok {
			return nil, gvwErrors.NewError(gvwErrors.CodeMigrationEngineAddFieldAlreadyExists)
		}
		doc[migration.Field] = nil
	case MigrationDelete:
		delete(doc, migration.Field)
	case MigrationRename:
		return e.applyRenameMigration(ctx, doc, migration)
	case MigrationConditional:
		return e.applyConditionalMigration(ctx, doc, migration)
	default:
		return nil, gvwErrors.NewError(gvwErrors.CodeMigrationEngineInvalidAction)
	}

	return doc, nil
}

func (e *MigrationEngine) applyRenameMigration(ctx context.Context, doc map[string]any, migration Migration) (map[string]any, error) {
	if ctx.Err() != nil {
		return nil, gvwErrors.NewError(gvwErrors.CodeCancelledError)
	}

	if _, exists := doc[migration.Value]; exists && migration.Value != migration.Field {
		return nil, gvwErrors.NewError(gvwErrors.CodeMigrationEngineRenameTargetExists)
	}

	val, ok := doc[migration.Field]
	if !ok {
		return nil, gvwErrors.NewError(gvwErrors.CodeMigrationEngineRenamingFieldFailed)
	}
	delete(doc, migration.Field)
	doc[migration.Value] = val

	return doc, nil
}

func (e *MigrationEngine) applyConditionalMigration(ctx context.Context, doc map[string]any, migration Migration) (map[string]any, error) {
	if ctx.Err() != nil {
		return nil, gvwErrors.NewError(gvwErrors.CodeCancelledError)
	}

	c := migration.Conditional

	cur, exists := doc[c.Condition.Field]
	matches := exists && valueEquals(cur, c.Condition.Value)
	if c.Negated {
		matches = !matches
	}

	branch := c.FalseBranch
	if matches {
		branch = c.TrueBranch
	}

	if branch.Field != "" {
		newVal, err := coerceLike(doc[branch.Field], branch.Value)
		if err != nil {
			return nil, gvwErrors.Wrap(gvwErrors.CodeMigrationEngineConditionalValueInvalid, err)
		}
		doc[branch.Field] = newVal
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
