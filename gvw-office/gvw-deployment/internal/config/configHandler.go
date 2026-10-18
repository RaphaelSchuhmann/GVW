package config

import (
	"encoding/json"
	"fmt"
	"gvw/gvw-deployment/internal/utils"
)

type Config struct {
	Port          string `json:"port"`
	InternalToken string `json:"internal_token"`
	JavaBaseURL   string `json:"java_base_url"`
	GithubRepoURL string `json:"github_repo_url"`
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

	if config.InternalToken == "" || config.JavaBaseURL == "" || config.Port == "" || config.GithubRepoURL == "" {
		return nil, fmt.Errorf("Invalid config")
	}

	return &config, nil
}
