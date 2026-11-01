package config

import (
	"encoding/json"
	"gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/filesystem"
	"reflect"
)

type Config struct {
	Port             string            `json:"port"`
	InternalToken    string            `json:"internal_token"`
	JavaBaseURL      string            `json:"java_base_url"`
	GithubRepoURL    string            `json:"github_repo_url"`
	ModelDatabaseMap map[string]string `json:"model_db_map"`
	DatabaseURL      string            `json:"database_url"`
	DatabaseUsername string            `json:"database_username"`
	DatabasePassword string            `json:"database_password"`
}

type ConfigManager struct {
	Config            Config
	FileSystemWrapper filesystem.FileSystemWrapper
	FilePath          string
}

func NewConfigManager(filesystemWrapper filesystem.FileSystemWrapper, path string) *ConfigManager {
	return &ConfigManager{
		FileSystemWrapper: filesystemWrapper,
		FilePath:          path,
	}
}

func (c *ConfigManager) LoadConfig(revertOnError bool) error {
	var originalConfig Config

	if revertOnError {
		originalConfig = c.Config
	}

	data, err := c.FileSystemWrapper.ReadFile(c.FilePath)
	if err != nil {
		c.revertOnError(revertOnError, originalConfig)
		return gvwErrors.Wrap(gvwErrors.CodeConfigError, err)
	}

	var config Config
	err = json.Unmarshal(data, &config)
	if err != nil {
		c.revertOnError(revertOnError, originalConfig)
		return gvwErrors.Wrap(gvwErrors.CodeConfigError, err)
	}

	if err := ValidateConfig(config); err != nil {
		c.revertOnError(revertOnError, originalConfig)
		return gvwErrors.Wrap(gvwErrors.CodeConfigError, err)
	}

	c.Config = config

	return nil
}

func (c *ConfigManager) revertOnError(shouldRevert bool, originalConfig Config) {
	if shouldRevert {
		c.Config = originalConfig
	}
}

func ValidateConfig(config Config) error {
	val := reflect.ValueOf(config)

	if val.Kind() != reflect.Struct {
		return gvwErrors.NewError(gvwErrors.CodeInvalidConfigNoStruct)
	}

	for i := 0; i < val.NumField(); i++ {
		if IsConfigValueInvalid(val.Field(i)) {
			return gvwErrors.NewError(gvwErrors.CodeInvalidConfigEmptyField)
		}
	}

	// Ensure that model to db map always has at least one value
	if len(config.ModelDatabaseMap) <= 0 {
		return gvwErrors.NewError(gvwErrors.CodeInvalidConfigBadModelMap)
	}

	return nil
}

func IsConfigValueInvalid(member reflect.Value) bool {
	if member.Kind() == reflect.Ptr {
		if member.IsNil() {
			return true
		}
		member = member.Elem()
	}

	if member.Kind() != reflect.Struct {
		return member.IsZero()
	}

	for i := 0; i < member.NumField(); i++ {
		field := member.Field(i)

		if field.IsZero() {
			return true
		}
	}

	return false
}
