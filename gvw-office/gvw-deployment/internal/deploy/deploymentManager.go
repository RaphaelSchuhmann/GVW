package deploy

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"gvw/gvw-deployment/internal/api/client"
	"gvw/gvw-deployment/internal/build"
	"gvw/gvw-deployment/internal/config"
	"gvw/gvw-deployment/internal/migration"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/filesystem"
	"gvw/gvw-deployment/internal/utils/log"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"
)

const (
	backendService = "gvw-backend.service"
	buildsDir      = "./builds"
	backendJarName = "gvw-backend.jar"
	backendEnvKey  = "BACKEND_JAR"

	// stateFilePath persists the pipeline state across crashes and restarts.
	stateFilePath = "./deployment-state.json"
)

// stage tracks how far a deployment got, so rollback only undoes what was done.
type stage int

const (
	stageNone stage = iota
	stageMaintenance
	stageBackendStopped
	stageArtifactsInstalled
	stageMigrated
	stageBackendSwitched
)

// runState is the per-deployment state. Nothing deployment specific lives on DeploymentManager.
type runState struct {
	reached          stage
	prevJarPath      string
	restoreDBs       func() error
	recoveryRequired bool
	recoveryReason   string
}

// markRecoveryRequired flags that the server needs manual intervention.
// The first reason wins, later ones are usually just consequences.
func (rs *runState) markRecoveryRequired(reason string) {
	if rs.recoveryRequired {
		return
	}
	rs.recoveryRequired = true
	rs.recoveryReason = reason
}

type pipelineStatus string

const (
	// statusInProgress left behind on disk while no pipeline is running means the process crashed mid-deployment.
	statusInProgress       pipelineStatus = "IN_PROGRESS"
	statusRecoveryRequired pipelineStatus = "RECOVERY_REQUIRED"
)

type persistedState struct {
	Status       pipelineStatus `json:"status"`
	DeploymentId string         `json:"deploymentId"`
	Reason       string         `json:"reason,omitempty"`
	UpdatedAt    time.Time      `json:"updatedAt"`
}

type DeploymentManagerDeps struct {
	Config           *config.ConfigManager
	BuildManager     *build.BuildManager
	MigrationManager *migration.MigrationManager
	Logger           *log.Logger
	JavaClient       client.JavaClient
	ServiceManager   *ServiceManager
	FsWrapper        filesystem.FileSystemWrapper
	NginxController  *NginxMaintenanceController
}

type DeploymentManager struct {
	cfg              *config.ConfigManager
	buildManager     *build.BuildManager
	migrationManager *migration.MigrationManager
	logger           *log.Logger
	javaClient       client.JavaClient
	serviceManager   *ServiceManager
	fsWrapper        filesystem.FileSystemWrapper
	nginxController  *NginxMaintenanceController

	// runMu allows only one pipeline at a time. activeId (a string) is only used to avoid
	// reporting a duplicate request for the running deployment as cancelled.
	runMu    sync.Mutex
	activeId sync.Map
}

func NewDeploymentManager(deps DeploymentManagerDeps) *DeploymentManager {
	return &DeploymentManager{
		cfg:              deps.Config,
		buildManager:     deps.BuildManager,
		migrationManager: deps.MigrationManager,
		logger:           deps.Logger,
		javaClient:       deps.JavaClient,
		serviceManager:   deps.ServiceManager,
		fsWrapper:        deps.FsWrapper,
		nginxController:  deps.NginxController,
	}
}

func (d *DeploymentManager) StartDeploymentPipeline(deploymentId, commit, appVersion string, migrations []migration.Migration) {
	if !isValidDeploymentId(deploymentId) {
		d.updateDeployment(deploymentId, client.StatusCancelled, gvwErrors.DEPLOY_CodeBadDeploymentId)
		return
	}

	// Concurrency lock: only one deployment may run at a time
	if !d.runMu.TryLock() {
		d.logger.Printf("[DEPLOY] Deployment %s rejected: another deployment is running", deploymentId)

		// A duplicate request for the running deployment must not overwrite its status
		if _, running := d.activeId.Load(deploymentId); !running {
			d.updateDeployment(deploymentId, client.StatusCancelled, gvwErrors.DEPLOY_CodeDeploymentInProgress)
		}
		return
	}
	defer d.runMu.Unlock()

	d.activeId.Store(deploymentId, struct{}{})
	defer d.activeId.Delete(deploymentId)

	// RECOVERY_REQUIRED gate: refuse to deploy onto a server that needs manual intervention.
	// A leftover IN_PROGRESS state (no pipeline holds the lock) means a previous run crashed.
	// An unreadable state file blocks as well, since the real state is unknown.
	state, err := d.readState()
	if err != nil {
		d.logger.Printf("[DEPLOY] Deployment %s rejected: unable to read pipeline state: %v", deploymentId, err)
		d.updateDeployment(deploymentId, client.StatusCancelled, gvwErrors.DEPLOY_CodeRecoveryRequired)
		return
	}
	if state != nil {
		d.logger.Printf("[DEPLOY] Deployment %s rejected: server is in state %s (deployment %s, reason: %s)",
			deploymentId, state.Status, state.DeploymentId, state.Reason)
		d.updateDeployment(deploymentId, client.StatusCancelled, gvwErrors.DEPLOY_CodeRecoveryRequired)
		return
	}

	if err := d.writeState(statusInProgress, deploymentId, ""); err != nil {
		d.logger.Printf("[DEPLOY] Deployment %s rejected: unable to persist pipeline state: %v", deploymentId, err)
		d.updateDeployment(deploymentId, client.StatusCancelled, gvwErrors.DEPLOY_CodeDeploymentInProgress)
		return
	}

	rs := &runState{}
	defer d.finalizeState(deploymentId, rs)

	// Get build
	if code, ok := d.ensureBuild(deploymentId, commit); !ok {
		d.updateDeployment(deploymentId, client.StatusCancelled, code)
		return
	}

	// Set maintenance flag
	if !d.enableNginxFlag() {
		d.updateDeployment(deploymentId, client.StatusFailed, gvwErrors.DEPLOY_CodeNginxMaintenanceFailed)
		return
	}
	rs.reached = stageMaintenance

	// Stop Java backend
	if err := d.serviceManager.StopService(backendService, d.cfg.Config.ShutdownTimeout); err != nil {
		d.logger.Printf("[DEPLOY] Error while shutting down java backend service: %v", err)
		d.rollbackAndAbort(deploymentId, gvwErrors.DEPLOY_CodeStopBackendFailed, rs)
		return
	}
	rs.reached = stageBackendStopped

	// Install new build artifacts
	if err := d.installArtifacts(deploymentId, appVersion); err != nil {
		d.logger.Printf("[DEPLOY] Error while installing build artifacts for deployment %s: %v", deploymentId, err)

		if gvwErrors.HasCode(err, gvwErrors.CodeDeploymentManagerFatalError) {
			rs.markRecoveryRequired("frontend could not be restored after failed artifact installation")
			// TODO: Notify
			return
		}

		d.rollbackAndAbort(deploymentId, gvwErrors.DEPLOY_CodeArtifcatInstallFailed, rs)
		return
	}
	rs.reached = stageArtifactsInstalled

	// Execute migrations (restores the databases itself if a migration fails)
	backupDbs, err := d.migrationManager.StartMigration(deploymentId, migrations)
	if err != nil {
		d.logger.Printf("[DEPLOY] Migration execution failed: %v", err)

		if gvwErrors.HasCode(err, gvwErrors.CodeMigrationEngineDbRestoreFailed) {
			d.logger.Println("[DEPLOY] Fatal: Failed to restore databases after failed migration execution")
			rs.markRecoveryRequired("database restore failed after failed migration")
			// TODO: Notify
			return
		}

		d.rollbackAndAbort(deploymentId, gvwErrors.DEPLOY_CodeMigrationsFailed, rs)
		return
	}
	rs.reached = stageMigrated
	rs.restoreDBs = func() error {
		return d.migrationManager.RestoreDatabasesFromBackup(context.Background(), backupDbs)
	}

	// Switch to new installs
	jarPath := filepath.Join(d.cfg.Config.BackendDir, appVersion, backendJarName)
	prevJarPath, err := d.switchBackend(jarPath)
	if err != nil {
		// Any errors here are potentially fatal and handled as such
		d.logger.Printf("[DEPLOY] Fatal: Failed to switch to new backend: %v", err)
		rs.markRecoveryRequired("failed to switch backend in env file")
		// TODO: Notify
		return
	}
	rs.prevJarPath = prevJarPath
	rs.reached = stageBackendSwitched

	// Start backend
	if err := d.serviceManager.StartService(backendService, d.cfg.Config.StartupTimeout); err != nil {
		d.logger.Printf("[DEPLOY] Error while starting java backend service: %v", err)
		d.rollbackAndAbort(deploymentId, gvwErrors.DEPLOY_CodeAppUnhealthy, rs)
		return
	}

	// Health check
	if healthy, data := d.isApplicationHealthy(); !healthy {
		d.logger.Printf("[DEPLOY] Deployment unhealthy with data: %s. Starting rollback", data)
		d.rollbackAndAbort(deploymentId, gvwErrors.DEPLOY_CodeAppUnhealthy, rs)
		return
	}

	// Clear db backups (a failure here is a cleanup problem, not a failed deployment)
	d.migrationManager.ClearDatabaseBackups(context.Background(), backupDbs)

	// Leave maintenance mode, only then is the deployment successful
	if !d.disableNginxFlag() {
		rs.markRecoveryRequired("maintenance mode could not be disabled after successful deployment")
		// TODO: Notify -> stuck in maintenance mode
		d.updateDeployment(deploymentId, client.StatusFailed, gvwErrors.DEPLOY_CodeNginxMaintenanceFailed)
		return
	}

	d.updateDeployment(deploymentId, client.StatusSuccess, gvwErrors.CodeNone)
}

// rollbackAndAbort undoes everything up to rs.reached, then leaves maintenance mode
// and reports the failure. If the rollback itself fails, maintenance mode stays enabled
// and the server is flagged as RECOVERY_REQUIRED.
func (d *DeploymentManager) rollbackAndAbort(deploymentId string, code gvwErrors.ErrorCode, rs *runState) {
	if !d.rollback(rs) {
		d.logger.Println("[DEPLOY] Fatal: rollback failed, keeping maintenance mode enabled")
		rs.markRecoveryRequired(fmt.Sprintf("rollback failed after deployment error %v", code))
		// TODO: Notify
		return
	}

	if !d.disableNginxFlag() {
		rs.markRecoveryRequired(fmt.Sprintf("maintenance mode could not be disabled after rollback (deployment error %v)", code))
		d.updateDeployment(deploymentId, client.StatusFailed, gvwErrors.CombineErrors(gvwErrors.DEPLOY_CodeNginxMaintenanceFailed, code))
		return
	}

	d.updateDeployment(deploymentId, client.StatusFailed, code)
}

// rollback returns true if the previous version is running again (or was never stopped).
func (d *DeploymentManager) rollback(rs *runState) bool {
	if rs.reached >= stageBackendSwitched && rs.prevJarPath == "" {
		d.logger.Println("[DEPLOY] Unable to rollback to previous backend, previous path is empty")
		// TODO: Notify
		return false
	}

	if rs.reached >= stageBackendSwitched {
		if err := d.serviceManager.StopService(backendService, d.cfg.Config.ShutdownTimeout); err != nil {
			d.logger.Printf("[DEPLOY] Error while shutting down java backend service: %v", err)
			// TODO: Notify
			return false
		}
	}

	if rs.reached >= stageMigrated && rs.restoreDBs != nil {
		if err := rs.restoreDBs(); err != nil {
			d.logger.Printf("[DEPLOY] Fatal error while restoring dbs from backup: %v", err)
			// TODO: Notify
			return false
		}
	}

	if rs.reached >= stageArtifactsInstalled {
		if err := d.restoreFrontend(); err != nil {
			d.logger.Printf("[DEPLOY] Fatal: Failed to restore previous frontend: %v", err)
			// TODO: Notify
			return false
		}
	}

	if rs.reached >= stageBackendSwitched {
		if _, err := d.switchBackend(rs.prevJarPath); err != nil {
			d.logger.Printf("[DEPLOY] Fatal: Failed to switch to previous backend: %v", err)
			// TODO: Notify
			return false
		}
	}

	if rs.reached >= stageBackendStopped {
		if err := d.serviceManager.StartService(backendService, d.cfg.Config.StartupTimeout); err != nil {
			d.logger.Printf("[DEPLOY] Switch to previous backend was unsuccessful: %v", err)
			// TODO: Notify
			return false
		}
	}

	return true
}

// RecoveryRequired reports whether the server is blocked from further deployments
// (RECOVERY_REQUIRED, a crashed run, or an unreadable state file). Useful for startup checks.
func (d *DeploymentManager) RecoveryRequired() (bool, string) {
	state, err := d.readState()
	if err != nil {
		return true, fmt.Sprintf("unable to read pipeline state: %v", err)
	}
	if state == nil {
		return false, ""
	}

	return true, fmt.Sprintf("%s (deployment %s): %s", state.Status, state.DeploymentId, state.Reason)
}

// ClearRecoveryRequired lifts the gate. Call it only after the server was fixed manually.
func (d *DeploymentManager) ClearRecoveryRequired() error {
	if !d.runMu.TryLock() {
		return errors.New("a deployment is currently running")
	}
	defer d.runMu.Unlock()

	return d.clearState()
}

// finalizeState runs when the pipeline ends. A clean end removes the state file, a fatal end
// persists RECOVERY_REQUIRED. A panic is treated as fatal and then re-raised. If persisting
// fails, the IN_PROGRESS file stays and keeps blocking, which is the safe outcome.
func (d *DeploymentManager) finalizeState(deploymentId string, rs *runState) {
	r := recover()
	if r != nil {
		d.logger.Printf("[DEPLOY] Fatal: pipeline panicked: %v", r)
		rs.markRecoveryRequired(fmt.Sprintf("pipeline panicked: %v", r))
	}

	if rs.recoveryRequired {
		d.logger.Printf("[DEPLOY] Server is now RECOVERY_REQUIRED: %s", rs.recoveryReason)
		if err := d.writeState(statusRecoveryRequired, deploymentId, rs.recoveryReason); err != nil {
			d.logger.Printf("[DEPLOY] Failed to persist RECOVERY_REQUIRED state: %v", err)
		}
	} else if err := d.clearState(); err != nil {
		d.logger.Printf("[DEPLOY] Failed to clear pipeline state, further deployments stay blocked: %v", err)
	}

	if r != nil {
		panic(r)
	}
}

// readState returns nil if no state file exists.
func (d *DeploymentManager) readState() (*persistedState, error) {
	data, err := os.ReadFile(stateFilePath)
	if errors.Is(err, os.ErrNotExist) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}

	var state persistedState
	if err := json.Unmarshal(data, &state); err != nil {
		return nil, fmt.Errorf("corrupt state file: %w", err)
	}

	return &state, nil
}

// writeState writes atomically (temp file + rename) so a crash never leaves a half-written file.
func (d *DeploymentManager) writeState(status pipelineStatus, deploymentId, reason string) error {
	data, err := json.MarshalIndent(persistedState{
		Status:       status,
		DeploymentId: deploymentId,
		Reason:       reason,
		UpdatedAt:    time.Now().UTC(),
	}, "", "  ")
	if err != nil {
		return err
	}

	tmpPath := stateFilePath + ".tmp"
	if err := os.WriteFile(tmpPath, data, 0644); err != nil {
		return err
	}

	return os.Rename(tmpPath, stateFilePath)
}

func (d *DeploymentManager) clearState() error {
	if err := os.Remove(stateFilePath); err != nil && !errors.Is(err, os.ErrNotExist) {
		return err
	}

	return nil
}

func isValidDeploymentId(id string) bool {
	return id != "" && id != "." && id != ".." && !strings.ContainsAny(id, `/\`)
}

// ensureBuild makes sure a usable build exists, triggering a fallback build if not.
func (d *DeploymentManager) ensureBuild(deploymentId, commit string) (gvwErrors.ErrorCode, bool) {
	if d.buildExists(deploymentId) {
		return gvwErrors.CodeNone, true
	}

	d.logger.Printf("[DEPLOY] Build directory not found for %s. Triggering fallback build...", deploymentId)

	if err := d.buildManager.TriggerBuild(deploymentId, commit, true); err != nil {
		return gvwErrors.MapErrorsToCode(err), false
	}

	if !d.buildExists(deploymentId) {
		return gvwErrors.DEPLOY_CodeNoBuildFound, false
	}

	return gvwErrors.CodeNone, true
}

func (d *DeploymentManager) buildExists(deploymentId string) bool {
	buildDir := filepath.Join(buildsDir, deploymentId)
	return d.fsWrapper.DirOrFileExists(buildDir) && d.fsWrapper.DirOrFileExists(filepath.Join(buildDir, "schema.json"))
}

func (d *DeploymentManager) updateDeployment(deploymentId string, status client.Status, errorCode gvwErrors.ErrorCode) {
	if err := d.javaClient.UpdateDeploymentState(deploymentId, status, errorCode, context.Background()); err != nil {
		d.logger.Printf("[DEPLOY] Failed to update deployment state: %v", err)
	}
}

func (d *DeploymentManager) enableNginxFlag() bool {
	if err := d.nginxController.EnableFlag(); err != nil {
		d.logger.Printf("[DEPLOY] Error while enabling maintenance flag: %v", err)
		return false
	}

	return true
}

func (d *DeploymentManager) disableNginxFlag() bool {
	if err := d.nginxController.DisableFlag(); err != nil {
		d.logger.Printf("[DEPLOY] Error while disabling maintenance flag: %v", err)

		// TODO: Notify

		return false
	}

	return true
}

// installArtifacts installs backend and frontend. It only returns CodeDeploymentManagerFatalError
// if the frontend could not be restored after a failed install; any other error is recoverable.
func (d *DeploymentManager) installArtifacts(deploymentId, appVersion string) error {
	binDir := filepath.Join(buildsDir, deploymentId, "bin")

	// Install backend
	backendWorkingDir := filepath.Join(d.cfg.Config.BackendDir, appVersion)
	if err := d.fsWrapper.CreateDir(backendWorkingDir, 0755); err != nil {
		return gvwErrors.Wrap(gvwErrors.CodeFileSystemError, err)
	}
	if err := d.fsWrapper.CopyFile(filepath.Join(binDir, backendJarName), backendWorkingDir); err != nil {
		return gvwErrors.Wrap(gvwErrors.CodeFileSystemError, err)
	}

	// Install frontend with safe rollback
	if err := d.backupFrontend(); err != nil {
		return gvwErrors.Wrap(gvwErrors.CodeFileSystemError, err)
	}

	installErr := d.fsWrapper.EmptyDir(d.cfg.Config.FrontendDir)
	if installErr == nil {
		installErr = d.fsWrapper.CopyDirContents(filepath.Join(binDir, "frontend"), d.cfg.Config.FrontendDir)
	}
	if installErr != nil {
		if restoreErr := d.restoreFrontend(); restoreErr != nil {
			d.logger.Printf("[DEPLOY] Failed to restore frontend after failed install: %v", restoreErr)
			return gvwErrors.Wrap(gvwErrors.CodeDeploymentManagerFatalError, restoreErr)
		}
		return gvwErrors.Wrap(gvwErrors.CodeFileSystemError, installErr)
	}

	return nil
}

// backupFrontend replaces the frontend backup with a copy of the current production frontend.
func (d *DeploymentManager) backupFrontend() error {
	backupDir := d.cfg.Config.FrontendBackupDir

	if !d.fsWrapper.DirOrFileExists(backupDir) {
		if err := d.fsWrapper.CreateDir(backupDir, 0755); err != nil {
			return err
		}
	}

	// Clear first so a stale backup is never merged with the current version
	if err := d.fsWrapper.EmptyDir(backupDir); err != nil {
		return err
	}

	return d.fsWrapper.CopyDirContents(d.cfg.Config.FrontendDir, backupDir)
}

func (d *DeploymentManager) restoreFrontend() error {
	if err := d.fsWrapper.EmptyDir(d.cfg.Config.FrontendDir); err != nil {
		return err
	}

	return d.fsWrapper.CopyDirContents(d.cfg.Config.FrontendBackupDir, d.cfg.Config.FrontendDir)
}

// switchBackend points the env file at jarPath and returns the jar path it replaced
// (empty if there was none).
func (d *DeploymentManager) switchBackend(jarPath string) (string, error) {
	envFilePath := d.cfg.Config.BackendEnv
	prefix := backendEnvKey + "="

	var lines []string
	if d.fsWrapper.DirOrFileExists(envFilePath) {
		content, err := d.fsWrapper.ReadFile(envFilePath)
		if err != nil {
			d.logger.Printf("[DEPLOY] Failed to read backend env file: %v", err)
			return "", gvwErrors.Wrap(gvwErrors.CodeFileSystemError, err)
		}
		lines = strings.Split(string(content), "\n")
	}

	prev := ""
	found := false

	for i, line := range lines {
		trimmed := strings.TrimSpace(line)
		if strings.HasPrefix(trimmed, prefix) {
			prev = strings.Trim(strings.TrimPrefix(trimmed, prefix), `"`)
			lines[i] = prefix + jarPath
			found = true
			break
		}
	}

	if !found {
		lines = append(lines, prefix+jarPath)
	}

	if err := d.fsWrapper.WriteToFile(envFilePath, []byte(strings.Join(lines, "\n"))); err != nil {
		return "", err
	}

	return prev, nil
}

func (d *DeploymentManager) isApplicationHealthy() (bool, string) {
	response, err := d.javaClient.RunHealthCheck(context.Background())
	if err != nil {
		return false, "ERROR"
	}

	return response.HttpStatusCode == 200, fmt.Sprintf("Application: %s, Database: %s", response.Status, response.Database)
}
