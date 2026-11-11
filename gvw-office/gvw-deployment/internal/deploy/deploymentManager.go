package deploy

import (
	"context"
	"gvw/gvw-deployment/internal/api/client"
	"gvw/gvw-deployment/internal/build"
	"gvw/gvw-deployment/internal/config"
	"gvw/gvw-deployment/internal/migration"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/filesystem"
	"gvw/gvw-deployment/internal/utils/log"
)

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

func (d *DeploymentManager) StartDeploymentPipeline(deploymentId, commit string, migrations []migration.Migration) {
	if deploymentId == "" {
		d.updateDeployment(deploymentId, client.StatusCancelled, gvwErrors.DEPLOY_CodeBadDeploymentId)
		return
	}

	// Get build
	buildDir, err := d.getBuildDir(deploymentId)

	if err != nil || buildDir == "" {
		d.logger.Printf("[DEPLOY] Build directory not found (%v). Triggering fallback build...", err)

		if buildErr := d.buildManager.TriggerBuild(deploymentId, commit, true); buildErr != nil {
			d.updateDeployment(deploymentId, client.StatusCancelled, gvwErrors.MapErrorsToCode(buildErr))
			return
		}

		buildDir, err = d.getBuildDir(deploymentId)
		if err != nil || buildDir == "" {
			d.updateDeployment(deploymentId, client.StatusCancelled, gvwErrors.DEPLOY_CodeNoBuildFound)
			return
		}
	}

	// Set maintenance flag
	if !d.enableNginxFlag() {
		d.updateDeployment(deploymentId, client.StatusFailed, gvwErrors.DEPLOY_CodeNginxMaintenanceFailed)
		return
	}

	// Stop Java Backend
	if err := d.serviceManager.StopService("gvw-backend.service", d.cfg.Config.ShutdownTimeout); err != nil {
		d.logger.Printf("[DEPLOY] Error while shuting down java backend service: %v", err)

		if !d.disableNginxFlag() {
			d.updateDeployment(deploymentId, client.StatusFailed, gvwErrors.CombineErrors(gvwErrors.DEPLOY_CodeNginxMaintenanceFailed, gvwErrors.DEPLOY_CodeStopBackendFailed))
			return
		}

		d.updateDeployment(deploymentId, client.StatusFailed, gvwErrors.DEPLOY_CodeStopBackendFailed)
		return
	}

	// Create database backup

	// Install new build artifacts

	// Execute migrations

	// 
}

func (d *DeploymentManager) getBuildDir(deploymentId string) (string, error) {
	buildDir := "./builds/" + deploymentId
	if !d.fsWrapper.DirOrFileExists(buildDir) || !d.fsWrapper.DirOrFileExists(buildDir+"/schema.json") {
		return "", gvwErrors.NewError(gvwErrors.CodeDeploymentManagerBuildDoesNotExist)
	}

	return buildDir, nil
}

func (d *DeploymentManager) updateDeployment(deploymentId string, status client.Status, errorCode gvwErrors.ErrorCode) {
	if updateErr := d.javaClient.UpdateDeploymentState(deploymentId, status, errorCode, context.Background()); updateErr != nil {
		d.logger.Printf("[DEPLOY] Failed to update dpeloyment state: %v", updateErr)
	}
}

func (d *DeploymentManager) enableNginxFlag() bool {
	if err := d.nginxController.EnableFlag(); err != nil {
		d.logger.Println("[DEPLOY] Error while enabling maintenance flag")
		return false
	}

	return true
}

func (d *DeploymentManager) disableNginxFlag() bool {
	if err := d.nginxController.DisableFlag(); err != nil {
		d.logger.Println("[DEPLOY] Error while disabling maintenance flag")

		// TODO: Add notify call here

		return false
	}

	return true
}
