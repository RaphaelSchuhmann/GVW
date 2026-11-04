package migration

import (
	"errors"
	"gvw/gvw-deployment/internal/utils/errors"
)

const (
	MIGRATION_CodeSuccess = 2001
)

const (
	MIGRATION_CodeBadSchema      = 2002
	MIGRATION_CodeMissingDbInMap = 2003
	MIGRATION_CodeBadAffectedDbs = 2004
	MIGRATION_CodeDryRunFailed   = 2005
)

func MapMigrationErrorsToCode(err error) uint32 {
	var appErr *gvwErrors.AppError
	if errors.As(err, &appErr) {
		switch appErr.Code {
		case "MigrationError_":
			return MIGRATION_CodeSuccess
		default:
			return MIGRATION_CodeDryRunFailed
		}
	}

	return MIGRATION_CodeDryRunFailed
}
