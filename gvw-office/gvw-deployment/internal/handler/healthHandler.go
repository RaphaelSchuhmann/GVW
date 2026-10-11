package handler

import (
	"encoding/json"
	"net/http"
	"gvw/gvw-deployment/internal/service"
)

type HealthHandler struct {
	healthService *service.HealthService
}

func NewHealthHandler(healthService *service.HealthService) *HealthHandler {
	return &HealthHandler{
		healthService: healthService,
	}
}

func (h *HealthHandler) RegisterRoutes(mux *http.ServeMux) {
	mux.HandleFunc("GET /health", h.CheckHealth)
}

func (h *HealthHandler) CheckHealth(w http.ResponseWriter, r *http.Request) {
	response := h.healthService.CheckHealth()

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(response)
}