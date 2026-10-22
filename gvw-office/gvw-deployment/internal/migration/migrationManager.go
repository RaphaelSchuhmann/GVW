package migration

import (
	"gvw/gvw-deployment/internal/api/client"
	"gvw/gvw-deployment/internal/utils/filesystem"
	"gvw/gvw-deployment/internal/utils/log"
)

type MigrationManager struct {
	logger    *log.Logger
	fsWrapper filesystem.FileSystemWrapper
	javaClient client.JavaClient
}

func NewMigrationManager(logger *log.Logger, fsWrapper filesystem.FileSystemWrapper, javaClient client.JavaClient) *MigrationManager {
	return &MigrationManager{
		logger:     logger,
		fsWrapper:  fsWrapper,
		javaClient: javaClient,
	}
}

// TODO: Implement migration handling
