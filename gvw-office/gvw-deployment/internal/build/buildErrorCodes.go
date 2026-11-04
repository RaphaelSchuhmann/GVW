package build

import (
	"gvw/gvw-deployment/internal/utils/errors"
)

const (
	BUILD_CodeBuildFailed            = 1001
	BUILD_CodeCommitInvalid          = 1002
	BUILD_CodeTestExecutionFailed    = 1003
	BUILD_CodeBinaryExtractionFailed = 1004
	BUILD_CodeSchemaGenerationFailed = 1005
)

func MapBuildErrorsToCode(err error) uint32 {
	switch {
	case gvwErrors.HasCode(err, gvwErrors.CodeBuildErrorBuildFailed):
		return BUILD_CodeBuildFailed
	case gvwErrors.HasCode(err, gvwErrors.CodeBuildErrorTestFailed):
		return BUILD_CodeTestExecutionFailed
	default:
		return BUILD_CodeBuildFailed
	}
}
