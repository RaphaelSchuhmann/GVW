package migration

import (
	"context"
	"errors"
	"fmt"
	"gvw/gvw-deployment/internal/api/client"
	"gvw/gvw-deployment/internal/config"
	"gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/filesystem"
	"gvw/gvw-deployment/internal/utils/log"
	"maps"
	"regexp"
	"slices"
	"sync"
)

var (
	deploymentIdPattern = regexp.MustCompile(`^[A-Za-z0-9_-]{1,64}$`)
)

const DbCleanupMessage = "[MIGRATION] Starting clean up of backup dbs..."

type ExecutionMode int

const (
	DefaultMigration ExecutionMode = iota
	DryRun
)

type activeMigrationTest struct {
	cancel context.CancelFunc
	done   chan struct{}
}

type MigrationManager struct {
	logger               *log.Logger
	fsWrapper            filesystem.FileSystemWrapper
	javaClient           client.JavaClient
	engine               *MigrationEngine
	mutex                sync.Mutex
	activeMigrationTests map[string]*activeMigrationTest
	configManager        *config.ConfigManager
	validator            *MigrationValidator
	db                   *client.DbClient
}

func NewMigrationManager(logger *log.Logger, fsWrapper filesystem.FileSystemWrapper, javaClient client.JavaClient, engine *MigrationEngine, configManager *config.ConfigManager, migrationValidator *MigrationValidator, db *client.DbClient) *MigrationManager {
	return &MigrationManager{
		logger:               logger,
		fsWrapper:            fsWrapper,
		javaClient:           javaClient,
		engine:               engine,
		activeMigrationTests: make(map[string]*activeMigrationTest),
		configManager:        configManager,
		validator:            migrationValidator,
		db:                   db,
	}
}

func (m *MigrationManager) TriggerMigrationTest(deploymentId string, migrations []Migration) {
	m.mutex.Lock()

	previous := m.activeMigrationTests[deploymentId]
	if previous != nil {
		m.logger.Printf("[BUILD] Cancelling migration test for deployment %s", deploymentId)
		previous.cancel()
	}

	ctx, cancel := context.WithCancel(context.Background())
	current := &activeMigrationTest{
		cancel: cancel,
		done:   make(chan struct{}),
	}

	m.activeMigrationTests[deploymentId] = current

	m.mutex.Unlock()

	// Start migration test pipeline asynchronously in a goroutine
	go func() {
		defer func() {
			m.mutex.Lock()
			if m.activeMigrationTests[deploymentId] == current {
				delete(m.activeMigrationTests, deploymentId)
			}
			m.mutex.Unlock()

			close(current.done)
		}()

		if previous != nil {
			<-previous.done
		}

		m.mutex.Lock()
		isCurrent := m.activeMigrationTests[deploymentId] == current
		m.mutex.Unlock()

		if !isCurrent {
			return
		}

		m.startMigrationPipeline(ctx, deploymentId, DryRun, migrations)
	}()
}

func (m *MigrationManager) StartMigration(deploymentId string, migrations []Migration) error {
	if deploymentId == "" || !deploymentIdPattern.MatchString(deploymentId) {
		return gvwErrors.NewError(gvwErrors.CodeMigrationManagerInvalidDeploymentId)
	}

	ctx := context.Background()
	if err := m.startMigrationPipeline(ctx, deploymentId, DefaultMigration, migrations); err != nil {
		m.logger.Printf("[MIGRATION] Error while executing normal migration for deployent %s: %v", deploymentId, err)
		return err
	}

	return nil
}

func (m *MigrationManager) startMigrationPipeline(ctx context.Context, deploymentId string, executionMode ExecutionMode, migrations []Migration) error {
	// Do an early return without an error as there are no migrations -> this can be intentional
	if len(migrations) <= 0 || ctx.Err() != nil {
		m.logger.Printf("[MIGRATION] No migrations found for deployment %s...", deploymentId)
		if executionMode == DryRun {
			m.sendDryRunResponse(ctx, deploymentId, MIGRATION_CodeSuccess, client.StatusSuccess, "", "")
		}
		return nil
	}

	m.logger.Printf("[MIGRATION] Starting migration pipeline for deployment %s", deploymentId)

	schema, err := m.prepareMigrationRun(ctx, deploymentId, migrations)
	if err != nil {
		m.logger.Printf("[MIGRATION] Error while preparing migration run for deployment %s: %v", deploymentId, err)

		if executionMode == DryRun {
			m.sendDryRunResponse(ctx, deploymentId, MIGRATION_CodeBadSchema, client.StatusCancelled, "", "")
		}

		return err
	}

	if executionMode == DryRun {
		if ctx.Err() != nil {
			return ctx.Err()
		}

		m.logger.Printf("[MIGRATION] Starting migration dry run for deploymentId %s", deploymentId)
		successful := m.executeDryRunPipeline(ctx, deploymentId, schema, migrations)
		if !successful {
			return nil
		}

		m.sendDryRunResponse(ctx, deploymentId, MIGRATION_CodeSuccess, client.StatusSuccess, "", "")
		m.logger.Printf("[MIGRATION] Migration dry run for deploymentId %s finished successfully", deploymentId)
		return nil
	} else {
		m.logger.Printf("[MIGRATION] Starting migration execution for deploymentId %s", deploymentId)
		return m.executeMigrationPipeline(deploymentId, schema, migrations)
	}
}

func (m *MigrationManager) prepareMigrationRun(ctx context.Context, deploymentId string, migrations []Migration) (*Schema, error) {
	if err := ctx.Err(); err != nil {
		return nil, err
	}

	schema, err := m.engine.LoadSchema(ctx, deploymentId)
	if err != nil {
		return nil, err
	}

	if err := m.engine.ValidateMigrations(ctx, migrations, schema); err != nil {
		return nil, err
	}

	return schema, nil
}

func (m *MigrationManager) executeDryRunPipeline(ctx context.Context, deploymentId string, schema *Schema, migrations []Migration) bool {
	if ctx.Err() != nil {
		return false
	}

	affectedDatabases := m.engine.ExtractDatabasesFromMigrations(migrations)
	modelToDbMap := m.configManager.Config.ModelDatabaseMap

	if !m.validator.AllValuesPresent(modelToDbMap, affectedDatabases) {
		m.logger.Printf("[MIGRATION] Found missing db in map for deployment: %s", deploymentId)
		m.sendDryRunResponse(ctx, deploymentId, MIGRATION_CodeMissingDbInMap, client.StatusCancelled, "", "")
		return false
	}

	dbToMigrationMap, err := m.engine.GenerateMigrationToDbMap(migrations, affectedDatabases)
	if err != nil {
		m.logger.Printf("[MIGRATION] Error while generating migration to db map for deployment: %s", deploymentId)
		m.sendDryRunResponse(ctx, deploymentId, MIGRATION_CodeBadAffectedDbs, client.StatusCancelled, "", "")
		return false
	}

	// Plan every database first (expand model prefixes / embedded models, check against the schema)
	// so that bad migrations are reported before any document is read.
	plans := make(map[string]dbPlan, len(dbToMigrationMap))
	for database, dbMigrations := range dbToMigrationMap {
		plan, err := m.engine.PlanDatabaseMigrations(database, dbMigrations, schema, modelToDbMap)
		if err != nil {
			m.logger.Printf("[MIGRATION] Bad migration found while planning deployment %s in db %s with err: %v", deploymentId, database, err)
			m.sendDryRunResponse(ctx, deploymentId, MapMigrationErrorsToCode(err), client.StatusFailed, database, "")
			return false
		}
		plans[database] = plan
	}

	for database, plan := range plans {
		if err := m.engine.TestMigrationsAgainstDatabase(ctx, database, plan, schema); err != nil {
			if gvwErrors.HasCode(err, gvwErrors.CodeCancelledError) {
				return false
			}

			documentId := ""

			var me *gvwErrors.MigrationError
			if errors.As(err, &me) {
				documentId = me.DocumentId
			}

			m.logger.Printf("[MIGRATION] Bad migration found for deployment %s in db %s with err: %v", deploymentId, database, err)
			m.sendDryRunResponse(ctx, deploymentId, MapMigrationErrorsToCode(err), client.StatusFailed, database, documentId)
			return false
		}
	}

	return true
}

// executeMigrationPipeline runs the real migration:
//  1. plan every database and run a read-only pre-flight against the current data,
//  2. create a new ephemeral replication db which exists during the migration process for each touched database
//  3. write the migrated documents database by database.
func (m *MigrationManager) executeMigrationPipeline(deploymentId string, schema *Schema, migrations []Migration) error {
	// Never cancelled: an aborted write run would leave the data half-migrated.
	ctx := context.Background()

	plans, err := m.engine.BuildPlans(migrations, schema, m.configManager.Config.ModelDatabaseMap)
	if err != nil {
		return fmt.Errorf("planning migrations for deployment %s: %w", deploymentId, err)
	}
	databases := slices.Sorted(maps.Keys(plans))

	// The data may have changed since the last dry run, so test everything again before touching it.
	for _, database := range databases {
		if err := m.engine.TestMigrationsAgainstDatabase(ctx, database, plans[database], schema); err != nil {
			return fmt.Errorf("pre-flight for database %s failed: %w", database, err)
		}
	}

	backupDbs := make(map[string]string)

	for _, database := range databases {
		backupDb, err := m.db.CreateBackupOfDatabase(ctx, database)
		if err != nil {
			m.logger.Printf("[MIGRATION] Error while creating backup for db %s: %v", database, err)

			m.clearDatabaseBackups(ctx, backupDbs)
			return err
		}

		backupDbs[database] = backupDb
	}

	for _, database := range databases {
		m.logger.Printf("[MIGRATION] Writing migrations for deployment %s to database %s", deploymentId, database)

		stats, err := m.engine.ExecuteMigrationsOnDatabase(ctx, database, plans[database], schema)
		if err != nil {
			m.logger.Printf("[MIGRATION] Migration of database %s failed after %d processed / %d written documents: %v", database, stats.Processed, stats.Changed, err)

			if err := m.restoreDatabasesFromBackup(ctx, backupDbs); err != nil {
				return err
			}

			m.clearDatabaseBackups(ctx, backupDbs)

			return fmt.Errorf("migrating database %s failed: %w", database, err)
		}

		m.logger.Printf("[MIGRATION] Database %s migrated: %d documents processed, %d written", database, stats.Processed, stats.Changed)
	}

	// Cleanup backup dbs after migration
	m.clearDatabaseBackups(ctx, backupDbs)

	return nil
}

func (m *MigrationManager) sendDryRunResponse(ctx context.Context, deploymentId string, reason uint32, status client.Status, database string, documentId string) {
	if err := m.javaClient.UpdateMigrationDryRunState(deploymentId, status, database, documentId, reason, ctx); err != nil {
		m.logger.Printf("[MIGRATION] Error updating migration dry run status for deployment %s with status %s: %v", deploymentId, string(status), err)
	}
}

func (m *MigrationManager) clearDatabaseBackups(ctx context.Context, backups map[string]string) {
	m.logger.Println(DbCleanupMessage)
	if err := m.db.ClearDatabaseBackups(ctx, backups); err != nil {
		m.logger.Printf("[MIGRATION] Error while backup clean up: %v", err)
	}
}

func (m *MigrationManager) restoreDatabasesFromBackup(ctx context.Context, backups map[string]string) error {
	m.logger.Println("[MIGRATION] Starting rollback for all databases...")

	for prod, backup := range backups {
		if err := m.db.RestoreDatabaseFromBackup(ctx, prod, backup); err != nil {
			// TODO: This should trigger an email notification in the deployment pipeline
			m.logger.Printf("[MIGRATION] CRITICAL: Error while restoring backup of database %s: %v", prod, err)
			return err
		}
	}

	return nil
}
