package handler

import (
	"gvw/gvw-deployment/internal/config"
	"net/http"
)

type ConfigHandler struct {
	ConfigManager *config.ConfigManager
}

func NewConfigHandler(configManager *config.ConfigManager) *ConfigHandler {
	return &ConfigHandler{
		ConfigManager: configManager,
	}
}

func (c *ConfigHandler) RegisterRoutes(mux *http.ServeMux) {
	mux.HandleFunc("GET /config/reload", c.ReloadConfig)
}

func (c *ConfigHandler) ReloadConfig(resp http.ResponseWriter, req *http.Request) {
	if err := c.ConfigManager.LoadConfig(true); err != nil {
		resp.WriteHeader(http.StatusBadRequest) // BadRequest cause the config.json that gets reloaded likely is invalid or non existant
	} else {
		resp.WriteHeader(http.StatusOK)
	}
}
