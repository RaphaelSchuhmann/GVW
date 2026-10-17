package utils

import (
	"os"
)

type FileSytemWrapper struct {
	logger *Logger
}

func NewFileSystemWrapper(logger *Logger) *FileSytemWrapper {
	return &FileSytemWrapper{
		logger: logger,
	}
}

func (f *FileSytemWrapper) DirOrFileExists(path string) bool {
	_, err := os.Stat(path)

	if err != nil {
		f.logger.Printf("Failed to check directory or file existance: %v", err)
	}

	return err == nil
}

func (f *FileSytemWrapper) CreateDir(path string, permissions os.FileMode) {
	err := os.MkdirAll(path, permissions)
	if err != nil {
		f.logger.Printf("Failed to create directory: %v", err)
	}
}

func (f *FileSytemWrapper) CleanDir(path string) {
	err := os.RemoveAll(path)
	if err != nil {
		f.logger.Printf("Failed to clean directory: %v", err)
	}
}

func (f *FileSytemWrapper) WriteToFile(path string, content []byte) {
	err := os.WriteFile(path, content, 0644)
	if err != nil {
		f.logger.Printf("Failed to write to file: %v", err)
	}
}

func (f *FileSytemWrapper) ReadFile(path string) ([]byte, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		f.logger.Printf("Faield to read file: %v", err)
		return nil, err
	}
	return data, nil
}
