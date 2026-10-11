package main

import (
	"log"
	"net/http"
	"os"
	// "gvw/gvw-deployment/internal/client"
	"gvw/gvw-deployment/internal/handler"
	"gvw/gvw-deployment/internal/service"
)

func main() {
	javaBaseURL := os.Getenv("JAVA_API_URL")
	if javaBaseURL == "" {
		javaBaseURL = "http://localhost:3500/api"
	}
	// internalToken := os.Getenv("GVW_INTERNAL_API_TOKEN")
	// javaClient := client.NewJavaClient(javaBaseURL, internalToken)

	// Setup services
	healthService := *service.NewHealthService();
	
	// Setup handlers
	healthHandler := *handler.NewHealthHandler(&healthService)

	// Setup router and register all routes
	mux := http.NewServeMux()
	healthHandler.RegisterRoutes(mux)

	log.Println("Go backend starting on 127.0.0.1:3501...")
	if err := http.ListenAndServe("127.0.0.1:3501", mux); err != nil {
		log.Fatalf("Server failed: %v", err)
	}
}