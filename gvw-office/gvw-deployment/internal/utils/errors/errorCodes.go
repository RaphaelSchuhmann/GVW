package gvwErrors

type Code string

const (
	// Database client error codes
	CodeDbEncodeBody                Code = "DbClientError_Encode_Body"
	CodeDbBuildRequest              Code = "DbClientError_Build_Request"
	CodeDbSendRequest               Code = "DbClientError_Send_Request"
	CodeDbBadStatus                 Code = "DbClientError_Bad_Status"
	CodeDbDecodeBody                Code = "DbClientError_Decode_Body"
	CodeBulkUpdateContainedError    Code = "DbClientError_Bulk_Update_Contained_Error"
	CodeDbBackupDbCreationFailed    Code = "DbClientError_Backup_Db_Creation_Failed"
	CodeDbBackupDbReplicationFailed Code = "DbClientError_Backup_Db_Replication_Failed"
	CodeDbBackupRestorationFailed   Code = "DbClientError_Backup_Restoration_Failed"
	CodeDbBackupDbDeletionFailed    Code = "DbClientError_Backup_Db_Deletion_Failed"
	CodeDbFindAllFailed             Code = "DbClientError_Find_All_Failed"

	// HTTP client error codes
	CodeHttpError Code = "HttpError"

	// Migration engine codes
	CodeMigrationEngineNoBuildFound    Code = "MigrationEngineError_No_Build_Found"
	CodeMigrationEngineReadError       Code = "MigrationEngineError_Read_Error"
	CodeMigrationEngineJsonError       Code = "MigrationEngineError_Json_Error"
	CodeMigrationEngineInvalidField    Code = "MigrationEngineError_Invalid_Migration_Field"
	CodeMigrationEngineInvalidSequence Code = "MigrationEngineError_Invalid_Migration_Sequence"
	CodeMigrationEngineBadAffectedDBs  Code = "MigrationEngineError_Bad_Affected_DBs"
	CodeMigrationEngineDbQueryError    Code = "MigrationEngineError_Db_Query_Error"
	CodeMigrationEngineMigrationFailed Code = "MigrationEngineError_Migration_Failed"
	CodeMigrationEngineMissingDbInMap  Code = "MigrationEngineError_Missing_Db_In_Map"

	// Migration application codes
	CodeMigrationEngineApplyMigrationsFailed            Code = "MigrationEngineError_Apply_Migrations_Failed"
	CodeMigrationEngineAddFieldAlreadyExists            Code = "MigrationEngineError_Add_Field_Already_Exists"
	CodeMigrationEngineRenamingFieldFailed              Code = "MigrationEngineError_Renaming_Field_Failed"
	CodeMigrationEngineInvalidAction                    Code = "MigrationEngineError_Invalid_Action"
	CodeMigrationEngineRenameTargetExists               Code = "MigrationEngineError_Rename_Target_Exists"
	CodeMigrationEngineConditionalValueInvalid          Code = "MigrationEngineError_Conditional_Value_Invalid"
	CodeMigrationEngineErrorApplicationValidationFailed Code = "MigrationEngineError_Application_Validation_Failed"
	CodeMigrationEngineSchemaMismatch                   Code = "CodeMigrationEngine_Schema_Mismatch"
	CodeMigrationEngineInvalidFieldPath                 Code = "CodeMigrationEngine_Invalid_Field_Path"
	CodeMigrationEngineEmbeddedModelNotUsed             Code = "CodeMigrationEngine_Embedded_Model_Not_Used"
	CodeMigrationEngineConditionalScopeMismatch         Code = "CodeMigrationEngine_Conditional_Scope_Mismatch"
	CodeMigrationEngineFieldNotInSchema                 Code = "CodeMigrationEngine_Field_Not_In_Schema"
	CodeMigrationEngineDbRestoreFailed                  Code = "CodeMigrationEngine_Db_Restore_Failed"

	// Migration manager codes
	CodeMigrationManagerInvalidDeploymentId Code = "MigrationManagerError_Invalid_Deployment_Id"
	CodeMigrationManagerError               Code = "MigrationManagerError"

	// Service manager codes
	CodeServiceManagerBadServiceName Code = "ServiceManagerError_Bad_Service_Name"
	CodeServiceManagerJobTimedout    Code = "ServiceManagerError_Job_Timedout"
	CodeServiceManagerJobFailed      Code = "ServiceManagerError_Job_Failed"

	// Nginx maintenance mode controller
	CodeNginxEmptyFlagPath       Code = "NginxMaintenanceModeError_Empty_Flag_Path"
	CodeNginxNonAbsoluteFlagPath Code = "NginxMaintenanceModeError_Non_Absolute_Flag_Path"
	CodeNginxFailedToSetFlag     Code = "NginxMaintenanceModeError_Failed_To_Set_Flag"
	CodeNginxFailedToUnsetFlag   Code = "NginxMaintenanceModeError_Failed_To_Unset_Flag"

	// Deployment manager codes
	CodeDeploymentManagerBuildDoesNotExist Code = "DeploymentManager_Build_Does_Not_Exist"
	CodeDeploymentManagerFatalError        Code = "DeploymentManager_Fatal"

	// Notify codes
	CodeNotifyErrorSendingEmail Code = "Notify_Error_Sending_Email"

	// Config error codes
	CodeConfigError              Code = "ConfigError"
	CodeInvalidConfigNoStruct    Code = "InvalidConfig_No_Struct"
	CodeInvalidConfigEmptyField  Code = "InvalidConfig_Empty_Field"
	CodeInvalidConfigBadModelMap Code = "InvalidConfig_Bad_Model_Map"

	// Command executor error codes
	CodeCommandExecutionErrorCancelledOrTimedout Code = "CommandExecutionError_Cancelled_Or_Timedout"
	CodeCommandExecutionErrorCommandFailed       Code = "CommandExecutionError_Command_Failed"

	// Filesystem error codes
	CodeFileSystemError             Code = "FileSystemError"
	CodeFileSystemErrorFileNotFound Code = "FileSystemError_File_Not_Found"

	// Model loader error codes
	CodeModelLoaderInvalidPath               Code = "ModelLoader_Invalid_Path"
	CodeModelLoaderFailedReadingFile         Code = "ModelLoder_Failed_Reading_File"
	CodeModelLoaderInvalidClassStructure     Code = "ModelLoder_Invalid_Class_Structure"
	CodeModelLoaderInvalidTypeRepresentation Code = "ModelLoader_Invalid_Type_Representation"
	CodeModelLoaderNormalizeType             Code = "ModelLoader_Normalize_Type"
	CodeModelLoaderNormalizingCollections    Code = "ModelLoder_Normalizing_Collections"
	CodeModelLoaderNormalizingArrays         Code = "ModelLoder_Normalizing_Arrays"
	CodeModelLoaderInvalidMapFormat          Code = "ModelLoder_Invalid_Map_Format"
	CodeModelLoaderNormalizeMap              Code = "ModelLoder_Normalize_Map"
	CodeModelLoaderUnsupportedType           Code = "ModelLoder_Unsupported_Type"

	// Build error codes
	CodeBuildErrorCloneRepo   Code = "BuildError_Clone_Repo"
	CodeBuildErrorBuildFailed Code = "BuildError_Build_Failed"
	CodeBuildErrorTestFailed  Code = "BuildError_Test_Failed"

	// General codes
	CodeCancelledError Code = "TaskCancelled"
	CodeTimeoutError   Code = "TaskTimedout"
)

type ErrorCode uint32

const (
	// No Error
	CodeNone ErrorCode = 0

	// General Code
	UNKOWN ErrorCode = 1 << 0

	// BUILD Codes
	BUILD_CodeBuildFailed            ErrorCode = 1 << 1
	BUILD_CodeCommitInvalid          ErrorCode = 1 << 2
	BUILD_CodeTestExecutionFailed    ErrorCode = 1 << 3
	BUILD_CodeBinaryExtractionFailed ErrorCode = 1 << 4
	BUILD_CodeSchemaGenerationFailed ErrorCode = 1 << 5

	// Migration Codes
	MIGRATION_CodeSuccess           ErrorCode = 1 << 6
	MIGRATION_CodeBadSchema         ErrorCode = 1 << 7
	MIGRATION_CodeMissingDbInMap    ErrorCode = 1 << 8
	MIGRATION_CodeBadAffectedDbs    ErrorCode = 1 << 9
	MIGRATION_CodeDryRunFailed      ErrorCode = 1 << 10
	MIGRATION_CodeBadMigrationFound ErrorCode = 1 << 11

	// Deploy Codes
	DEPLOY_CodeBadDeploymentId        ErrorCode = 1 << 12
	DEPLOY_CodeNoBuildFound           ErrorCode = 1 << 13
	DEPLOY_CodeNginxMaintenanceFailed ErrorCode = 1 << 14
	DEPLOY_CodeStopBackendFailed      ErrorCode = 1 << 15
	DEPLOY_CodeStartBackendFailed     ErrorCode = 1 << 16
	DEPLOY_CodeArtifcatInstallFailed  ErrorCode = 1 << 17
	DEPLOY_CodeMigrationsFailed       ErrorCode = 1 << 18
	DEPLOY_CodeAppUnhealthy           ErrorCode = 1 << 19
	DEPLOY_CodeRecoveryRequired       ErrorCode = 1 << 20
	DEPLOY_CodeDeploymentInProgress   ErrorCode = 1 << 21
)

func CombineErrors(errs ...ErrorCode) ErrorCode {
	var combined ErrorCode = 0
	for _, e := range errs {
		combined |= e
	}
	return combined
}

func HasErrorFlag(combined ErrorCode, flag ErrorCode) bool {
	return (combined & flag) != 0
}

func MapErrorsToCode(err error) ErrorCode {
	switch {
	case HasCode(err, CodeBuildErrorBuildFailed):
		return BUILD_CodeBuildFailed
	case HasCode(err, CodeBuildErrorTestFailed):
		return BUILD_CodeTestExecutionFailed
	default:
		return UNKOWN
	}
}
