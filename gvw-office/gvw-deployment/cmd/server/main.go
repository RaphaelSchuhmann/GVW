package main

import (
	"gvw/gvw-deployment/internal/app"
	"gvw/gvw-deployment/internal/config"
	"gvw/gvw-deployment/internal/utils"
)

func main() {
	var logger *utils.Logger = utils.NewLogger()
	var fsWrapper *utils.FileSystem = utils.NewFileSystem(logger)

	cfg, err := config.LoadConfig("./config.json", fsWrapper)
	if err != nil {
		logger.Fatalf("Failed to load server config: %v", err)
		return
	}

	application, err := app.NewApp(cfg, logger, fsWrapper)
	if err != nil {
		logger.Fatalf("Failed to initialize app: %v", err)
		return
	}

	if err := application.Start(); err != nil {
		logger.Fatalf("Server failed: %v", err)
	}
}
