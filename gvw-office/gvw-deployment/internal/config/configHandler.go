package config

import (
	"encoding/json"
	"fmt"
	"gvw/gvw-deployment/internal/utils/filesystem"
	"reflect"
)

type Config struct {
	Port             string            `json:"port"`
	InternalToken    string            `json:"internal_token"`
	JavaBaseURL      string            `json:"java_base_url"`
	GithubRepoURL    string            `json:"github_repo_url"`
	ModelDatabaseMap map[string]string `json:"model_db_map"`
}

func LoadConfig(path string, filesystemWrapper filesystem.FileSystemWrapper) (*Config, error) {
	data, err := filesystemWrapper.ReadFile(path)
	if err != nil {
		return nil, err
	}

	var config Config
	err = json.Unmarshal(data, &config)
	if err != nil {
		return nil, err
	}

	if err := ValidateConfig(config); err != nil {
		return nil, err
	}

	return &config, nil
}

func ValidateConfig(config Config) error {
	val := reflect.ValueOf(config)

	if val.Kind() != reflect.Struct {
		return fmt.Errorf("INVALID_CONFIG_NO_STRUCT")
	}

	for i := 0; i < val.NumField(); i++ {
		if IsConfigValueInvalid(val.Field(i)) {
			return fmt.Errorf("INVALID_CONFIG")
		}
	}

	// Ensure that model to db map always has at least one value
	if len(config.ModelDatabaseMap) <= 0 {
		return fmt.Errorf("INVALID_CONFIG")
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
