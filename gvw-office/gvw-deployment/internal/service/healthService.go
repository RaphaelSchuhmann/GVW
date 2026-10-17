package service

import (
	"gvw/gvw-deployment/internal/model"
)

type HealthService struct{}

func NewHealthService() *HealthService {
	return &HealthService{}
}

func (h *HealthService) CheckHealth() model.HealthResponse {
	return model.HealthResponse{
		OK: true,
	}
}
