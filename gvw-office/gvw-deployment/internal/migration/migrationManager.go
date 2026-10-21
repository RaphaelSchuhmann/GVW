package migration

import (
	"gvw/gvw-deployment/internal/utils/filesystem"
	"gvw/gvw-deployment/internal/utils/log"
)

type MigrationManager struct {
	logger    *log.Logger
	fsWrapper filesystem.FileSystemWrapper
}

func NewMigrationManager(logger *log.Logger, fsWrapper filesystem.FileSystemWrapper) *MigrationManager {
	return &MigrationManager{
		logger:    logger,
		fsWrapper: fsWrapper,
	}
}


