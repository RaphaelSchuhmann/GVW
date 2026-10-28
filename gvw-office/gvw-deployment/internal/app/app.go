package app

import (
	"errors"
	"gvw/gvw-deployment/internal/api/client"
	"gvw/gvw-deployment/internal/api/handler"
	"gvw/gvw-deployment/internal/api/middleware"
	"gvw/gvw-deployment/internal/build"
	"gvw/gvw-deployment/internal/config"
	"gvw/gvw-deployment/internal/migration"
	"gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/filesystem"
	"gvw/gvw-deployment/internal/utils/log"
	"net/http"
	"time"
)

type App struct {
	Config     *config.Config
	Logger     *log.Logger
	HttpServer *http.Server
}

func NewApp(cfg *config.Config, logger *log.Logger, fsWrapper *filesystem.FileSystem, configManager *config.ConfigManager) (*App, error) {
	javaBaseURL := cfg.JavaBaseURL
	if javaBaseURL == "" {
		javaBaseURL = "http://localhost:3500/api"
	}

	javaClient := client.NewHttpJavaClient(javaBaseURL, cfg.InternalToken)
	dbClient := client.NewDbClient(cfg.DatabaseURL, cfg.DatabaseUsername, cfg.DatabasePassword)

	// Build System
	modelLoader := build.NewModelLoader(logger, fsWrapper)
	buildManager := build.NewBuildManager(cfg.GithubRepoURL, javaClient, fsWrapper, logger, modelLoader)

	// Migration System
	migrationValidator := migration.NewMigrationValidator()
	migrationEngine := migration.NewMigrationEngine(logger, fsWrapper, migrationValidator, dbClient)
	migrationManager := migration.NewMigrationManager(logger, fsWrapper, javaClient, migrationEngine)

	// Handlers
	configHandler := handler.NewConfigHandler(configManager)
	buildHandler := handler.NewBuildHandler(buildManager)
	migrationHandler := handler.NewMigrationHandler(migrationManager)

	mux := http.NewServeMux()
	configHandler.RegisterRoutes(mux)
	buildHandler.RegisterRoutes(mux)
	migrationHandler.RegisterRoutes(mux)

	var handler http.Handler = mux
	handler = middleware.LoggingMiddleware(logger, handler)
	handler = middleware.LocalhostOnlyMiddleware(logger, handler)

	server := &http.Server{
		Addr:    "127.0.0.1:" + cfg.Port,
		Handler: handler,
		ReadHeaderTimeout: 10 * time.Second,
	}

	return &App{
		Config:     cfg,
		Logger:     logger,
		HttpServer: server,
	}, nil
}

func (a *App) Start() error {
	a.Logger.Println("Go backend starting on " + a.HttpServer.Addr + "...")

	err := a.HttpServer.ListenAndServe()
	if err != nil && !errors.Is(err, http.ErrServerClosed) {
		return gvwErrors.Wrap("StartUpError", err)
	}

	return nil
}
