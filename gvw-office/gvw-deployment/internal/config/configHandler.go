package config

import (
	"encoding/json"
	"gvw/gvw-deployment/internal/utils"
)

type Config struct {
	Port          string `json:"port"`
	InternalToken string `json:"internal_token"`
	JavaBaseURL   string `json:"java_base_url"`
}

func LoadConfig(path string, filesystemWrapper *utils.FileSytemWrapper) (*Config, error) {
	data, err := filesystemWrapper.ReadFile(path)
	if err != nil {
		return nil, err
	}

	var config Config
	err = json.Unmarshal(data, &config)
	if err != nil {
		return nil, err
	}

	return &config, nil
}
