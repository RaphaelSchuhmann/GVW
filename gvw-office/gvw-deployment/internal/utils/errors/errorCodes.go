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

	// Migration manager codes
	CodeMigrationManagerInvalidDeploymentId Code = "MigrationManagerError_Invalid_Deployment_Id"
	CodeMigrationManagerError               Code = "MigrationManagerError"

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
