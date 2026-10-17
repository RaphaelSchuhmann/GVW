package main

import (
	"net/http"
	"os"
	// "gvw/gvw-deployment/internal/api/client"
	"gvw/gvw-deployment/internal/api/handler"
	"gvw/gvw-deployment/internal/api/middleware"
	"gvw/gvw-deployment/internal/utils"
)

func main() {
	javaBaseURL := os.Getenv("JAVA_API_URL")
	if javaBaseURL == "" {
		javaBaseURL = "http://localhost:3500/api"
	}
	// internalToken := os.Getenv("GVW_INTERNAL_API_TOKEN")
	// javaClient := client.NewJavaClient(javaBaseURL, internalToken)

	var logger *utils.Logger = utils.NewLogger()

	// Setup handlers
	buildHandler := *handler.NewBuildHandler()

	// Setup router and register all routes
	mux := http.NewServeMux()
	buildHandler.RegisterRoutes(mux)

	// Register middlewares
	var handler http.Handler = mux
	handler = middleware.LoggingMiddleware(logger, handler)
	handler = middleware.LocalhostOnlyMiddleware(logger, handler)

	logger.Println("Go backend starting on 127.0.0.1:3501...")
	if err := http.ListenAndServe("127.0.0.1:3501", handler); err != nil {
		logger.Fatalf("Server failed: %v", err)
	}
}
