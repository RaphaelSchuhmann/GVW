package migration

import (
	"context"
	"encoding/json"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/filesystem"
	"gvw/gvw-deployment/internal/utils/log"
)

type MigrationEngine struct {
	logger    *log.Logger
	fsWrapper filesystem.FileSystemWrapper
	validator *MigrationValidator
}

func NewMigrationEngine(logger *log.Logger, fsWrapper filesystem.FileSystemWrapper, validator *MigrationValidator) *MigrationEngine {
	return &MigrationEngine{
		logger:    logger,
		fsWrapper: fsWrapper,
		validator: validator,
	}
}

func (e *MigrationEngine) LoadSchema(ctx context.Context, deploymentId string) (map[string]any, error) {
	schemaPath := "./builds/" + deploymentId + "/schema.json"

	if err := ctx.Err(); err != nil {
		return nil, err
	}

	if !e.fsWrapper.DirOrFileExists(schemaPath) {
		e.logger.Printf("[MIGRATION] No schema found for deployment %s at path %s", deploymentId, schemaPath)
		return nil, gvwErrors.NewError("MigrationEngineError_No_Build_Found")
	}

	data, err := e.fsWrapper.ReadFile(schemaPath)
	if err != nil {
		e.logger.Printf("[MIGRATION] Error while reading schema for deployment %s: %v", deploymentId, err)
		return nil, gvwErrors.Wrap("MigrationEngineError_Read_Error", err)
	}

	var result map[string]any

	if err := json.Unmarshal(data, &result); err != nil {
		e.logger.Printf("[MIGRATION] Error while unmarshaling schema for deployment %s: %v", deploymentId, err)
		return nil, gvwErrors.Wrap("MigrationEngineError_Json_Error", err)
	}

	return result, nil
}

func (e *MigrationEngine) ValidateMigrations(ctx context.Context, migrations []Migration, schema map[string]any) error {
	if err := ctx.Err(); err != nil {
		return err
	}

	if isValid := e.validator.AreMigrationFieldsValid(migrations); !isValid {
		return gvwErrors.NewError("MigrationEngineError_Invalid_Migration_Field")
	}

	if isValid := e.validator.IsMigrationSequenceValid(migrations); !isValid {
		return gvwErrors.NewError("MigrationEngineError_Invalid_Migration_Sequence")
	}

	return nil
}
