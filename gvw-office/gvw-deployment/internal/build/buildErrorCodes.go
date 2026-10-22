package build

import (
	"errors"
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
	var appErr *gvwErrors.AppError
	if errors.As(err, &appErr) {
		switch appErr.Code {
		case "BuildError_Build_Failed":
			return BUILD_CodeBuildFailed
		case "BuildError_Test_Failed":
			return BUILD_CodeTestExecutionFailed
		case "BuildError_Extract_Binaries_Copy_Frontend", "BuildError_Extract_Binaries_Find_Backend", "BuildError_Extract_Binaries_Copy_Backend":
			return BUILD_CodeBinaryExtractionFailed
		default:
			return BUILD_CodeBuildFailed
		}
	}

	return BUILD_CodeBuildFailed
}
