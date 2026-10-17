package main

import (
	"gvw/gvw-deployment/internal/api/handler"
	"gvw/gvw-deployment/internal/api/middleware"
	"gvw/gvw-deployment/internal/build"
	"gvw/gvw-deployment/internal/config"
	"gvw/gvw-deployment/internal/utils"
	"net/http"
)

func main() {
	var logger *utils.Logger = utils.NewLogger()
	var fsWrapper *utils.FileSytemWrapper = utils.NewFileSystemWrapper(logger)

	cfg, err := config.LoadConfig("./config.json", fsWrapper)

	if err != nil {
		logger.Fatalf("Failed to load server config: %v", err)
		return
	}

	javaBaseURL := cfg.JavaBaseURL
	if javaBaseURL == "" {
		javaBaseURL = "http://localhost:3500/api"
	}

	// TODO: This will be used later on
	// internalToken := cfg.InternalToken
	// if internalToken == "" {
	// 	logger.Fatal("Failed to lead internal token from config")
	// 	return
	// }

	// Setup services
	buildManager := build.NewBuildManager()

	// Setup handlers
	buildHandler := *handler.NewBuildHandler(buildManager)

	// Setup router and register all routes
	mux := http.NewServeMux()
	buildHandler.RegisterRoutes(mux)

	// Register middlewares
	var handler http.Handler = mux
	handler = middleware.LoggingMiddleware(logger, handler)
	handler = middleware.LocalhostOnlyMiddleware(logger, handler)

	logger.Println("Go backend starting on 127.0.0.1:" + cfg.Port + "...")
	if err := http.ListenAndServe("127.0.0.1:" + cfg.Port, handler); err != nil {
		logger.Fatalf("Server failed: %v", err)
	}
}
