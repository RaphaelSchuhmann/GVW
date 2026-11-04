package main

import (
	"gvw/gvw-deployment/internal/app"
	"gvw/gvw-deployment/internal/config"
	"gvw/gvw-deployment/internal/utils/log"
	"gvw/gvw-deployment/internal/utils/filesystem"
)

func main() {
	var logger *log.Logger = log.NewLogger()
	var fsWrapper *filesystem.FileSystem = filesystem.NewFileSystem(logger)
	var configManager *config.ConfigManager = config.NewConfigManager(fsWrapper, "./config.json")

	if err := configManager.LoadConfig(false); err != nil {
		logger.Fatalf("Failed to load server config: %v", err)
		return
	}

	application, err := app.NewApp(logger, fsWrapper, configManager)
	if err != nil {
		logger.Fatalf("Failed to initialize app: %v", err)
		return
	}

	if err := application.Start(); err != nil {
		logger.Fatalf("Server failed: %v", err)
	}
}
