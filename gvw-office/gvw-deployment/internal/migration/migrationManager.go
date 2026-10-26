package migration

import (
	"context"
	"gvw/gvw-deployment/internal/api/client"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/filesystem"
	"gvw/gvw-deployment/internal/utils/log"
	"regexp"
	"sync"
)

var (
	deploymentIdPattern = regexp.MustCompile(`^[A-Za-z0-9_-]{1,64}$`)
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
}

func NewMigrationManager(logger *log.Logger, fsWrapper filesystem.FileSystemWrapper, javaClient client.JavaClient, engine *MigrationEngine) *MigrationManager {
	return &MigrationManager{
		logger:               logger,
		fsWrapper:            fsWrapper,
		javaClient:           javaClient,
		engine:               engine,
		activeMigrationTests: make(map[string]*activeMigrationTest),
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

		m.startMigrationPipeline(ctx, deploymentId, true, migrations)
	}()
}

func (m *MigrationManager) StartMigration(deploymentId string, migrations []Migration) error {
	if deploymentId == "" || !deploymentIdPattern.MatchString(deploymentId) {
		return gvwErrors.NewError("MigrationManagerError_Invalid_Deployment_Id")
	}

	ctx := context.Background()
	if err := m.startMigrationPipeline(ctx, deploymentId, false, migrations); err != nil {
		m.logger.Printf("[MIGRATION] Error while executing normal migration for deployent %s: %v", deploymentId, err)
		return gvwErrors.Wrap("MigrationManagerError", err)
	}

	return nil
}

func (m *MigrationManager) startMigrationPipeline(ctx context.Context, deploymentId string, isDryRun bool, migrations []Migration) error {
	// Do an early return without an error as there are no migrations -> this can be intentional
	if len(migrations) <= 0 || ctx.Err() != nil {
		return nil
	}

	m.logger.Printf("[MIGRATION] Starting migration pipeline for deployment %s", deploymentId)
	
	schema, err := m.prepareMigrationRun(ctx, deploymentId, migrations)
	if err != nil {
		return err
	}
	
	if isDryRun {
		m.logger.Printf("[MIGRATION] Starting migration dry run for deploymentId %s", deploymentId)
		m.executeDryRunPipeline(ctx, deploymentId, schema)
	} else {
		return m.executeMigrationPipeline(deploymentId, schema)
	}

	return nil
}

func (m *MigrationManager) prepareMigrationRun(ctx context.Context, deploymentId string, migrations []Migration) (map[string]any, error) {
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

func (m *MigrationManager) executeDryRunPipeline(ctx context.Context, deploymentId string, schema map[string]any) {
	
}

func (m *MigrationManager) executeMigrationPipeline(deploymentId string, schema map[string]any) error {
	return nil
}
