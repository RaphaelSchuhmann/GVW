package deploy

import (
	"gvw/gvw-deployment/internal/api/client"
	"gvw/gvw-deployment/internal/build"
	"gvw/gvw-deployment/internal/config"
	"gvw/gvw-deployment/internal/migration"
	"gvw/gvw-deployment/internal/utils/log"
)

type DeploymentManager struct {
	cfg              *config.ConfigManager
	buildManager     *build.BuildManager
	migrationManager *migration.MigrationManager
	logger           *log.Logger
	javaClient       client.JavaClient
	serviceManager   *ServiceManager
}

func NewBuildManager(cfg *config.ConfigManager, build *build.BuildManager, migration *migration.MigrationManager, logger *log.Logger, client client.JavaClient, service *ServiceManager) *DeploymentManager {
	return &DeploymentManager{
		cfg:              cfg,
		buildManager:     build,
		migrationManager: migration,
		logger:           logger,
		javaClient:       client,
		serviceManager:   service,
	}
}

func (d *DeploymentManager) StartDeploymentPipeline(deploymentId string, migrations []migration.Migration) {

}
