package utils

import (
	"fmt"
	"io"
	"os"
	"path/filepath"
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

func (f *FileSytemWrapper) CreateDir(path string, permissions os.FileMode) error {
	err := os.MkdirAll(path, permissions)
	if err != nil {
		f.logger.Printf("Failed to create directory: %v", err)
		return err
	}

	return nil
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

func (f *FileSytemWrapper) CopyFile(src, dst string) error {
	sourceFile, err := os.Open(src)
	if err != nil {
		return err
	}
	defer sourceFile.Close()

	destDir := filepath.Dir(dst)
	err = f.CreateDir(destDir, 0755)
	if err != nil {
		return err
	}

	destFile, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer destFile.Close()

	_, err = io.Copy(destFile, sourceFile)
	if err != nil {
		return err
	}

	err = destFile.Sync()
	if err != nil {
		return err
	}

	return nil
}

func (f *FileSytemWrapper) ListDir(path string) ([]os.DirEntry, error) {
	entries, err := os.ReadDir(path)
	if err != nil {
		f.logger.Printf("Failed to read directory: %v", err)
		return nil, err
	}

	return entries, nil
}

func (f *FileSytemWrapper) CopyDirContents(srcDir, dstDir string) error {
	entries, err := os.ReadDir(srcDir)
	if err != nil {
		return err
	}

	if err := os.MkdirAll(dstDir, 0755); err != nil {
		return err
	}

	for _, entry := range entries {
		srcPath := filepath.Join(srcDir, entry.Name())
		dstPath := filepath.Join(dstDir, entry.Name())

		if entry.IsDir() {
			if err := f.CopyDirContents(srcPath, dstPath); err != nil {
				return err
			}
		} else {
			if err := f.CopyFile(srcPath, dstPath); err != nil {
				return err
			}
		}
	}

	return nil
}

func (f *FileSytemWrapper) GetFileByPattern(dir string, filePattern string) (string, error) {
	pattern := filepath.Join(dir, filePattern)

	matches, err := filepath.Glob(pattern)
	if err != nil {
		return "", err
	}

	if len(matches) == 0 {
		return "", fmt.Errorf("FILE_NOT_FOUND")
	}

	return matches[0], nil
}
