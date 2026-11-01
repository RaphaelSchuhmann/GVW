package migration

import (
	"context"
	"encoding/json"
	"gvw/gvw-deployment/internal/api/client"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/filesystem"
	"gvw/gvw-deployment/internal/utils/log"
	"slices"
)

type MigrationEngine struct {
	logger    *log.Logger
	fsWrapper filesystem.FileSystemWrapper
	validator *MigrationValidator
	dbClient  *client.DbClient
}

func NewMigrationEngine(logger *log.Logger, fsWrapper filesystem.FileSystemWrapper, validator *MigrationValidator, dbClient *client.DbClient) *MigrationEngine {
	return &MigrationEngine{
		logger:    logger,
		fsWrapper: fsWrapper,
		validator: validator,
		dbClient:  dbClient,
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

func (e *MigrationEngine) TestMigrationsAgainstDatabase(database string, migrations []Migration) (error, string) {

	return nil, ""
}
