package filesystem

import (
	"gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/log"
	"io"
	"os"
	"path/filepath"
)

var _ FileSystemWrapper = (*FileSystem)(nil)

type FileSystemWrapper interface {
	DirOrFileExists(path string) bool
	CreateDir(path string, permissions os.FileMode) error
	CleanDir(path string)
	WriteToFile(path string, content []byte) error
	ReadFile(path string) ([]byte, error)
	CopyFile(src, dst string) error
	ListDir(path string) ([]os.DirEntry, error)
	CopyDirContents(srcDir, dstDir string) error
	GetFileByPattern(dir string, filePattern string) (string, error)
}

type FileSystem struct {
	logger *log.Logger
}

func NewFileSystem(logger *log.Logger) *FileSystem {
	return &FileSystem{
		logger: logger,
	}
}

func (f *FileSystem) DirOrFileExists(path string) bool {
	_, err := os.Stat(path)

	if err != nil {
		f.logger.Printf("Failed to check directory or file existance: %v", err)
	}

	return err == nil
}

func (f *FileSystem) CreateDir(path string, permissions os.FileMode) error {
	err := os.MkdirAll(path, permissions)
	if err != nil {
		f.logger.Printf("Failed to create directory: %v", err)
		return errors.Wrap("FileSystemError", err)
	}

	return nil
}

func (f *FileSystem) CleanDir(path string) {
	err := os.RemoveAll(path)
	if err != nil {
		f.logger.Printf("Failed to clean directory: %v", err)
	}
}

func (f *FileSystem) WriteToFile(path string, content []byte) error {
	err := os.WriteFile(path, content, 0644)
	if err != nil {
		f.logger.Printf("Failed to write to file: %v", err)
		return errors.Wrap("FileSystemError", err)
	}
	return nil
}

func (f *FileSystem) ReadFile(path string) ([]byte, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		f.logger.Printf("Faield to read file: %v", err)
		return nil, errors.Wrap("FileSystemError", err)
	}
	return data, nil
}

func (f *FileSystem) CopyFile(src, dst string) error {
	sourceFile, err := os.Open(src)
	if err != nil {
		return errors.Wrap("FileSystemError", err)
	}
	defer sourceFile.Close()

	destDir := filepath.Dir(dst)
	err = f.CreateDir(destDir, 0755)
	if err != nil {
		return errors.Wrap("FileSystemError", err)
	}

	destFile, err := os.Create(dst)
	if err != nil {
		return errors.Wrap("FileSystemError", err)
	}
	defer destFile.Close()

	_, err = io.Copy(destFile, sourceFile)
	if err != nil {
		return errors.Wrap("FileSystemError", err)
	}

	err = destFile.Sync()
	if err != nil {
		return err
	}

	return nil
}

func (f *FileSystem) ListDir(path string) ([]os.DirEntry, error) {
	entries, err := os.ReadDir(path)
	if err != nil {
		f.logger.Printf("Failed to read directory: %v", err)
		return nil, errors.Wrap("FileSystemError", err)
	}

	return entries, nil
}

func (f *FileSystem) CopyDirContents(srcDir, dstDir string) error {
	entries, err := os.ReadDir(srcDir)
	if err != nil {
		return errors.Wrap("FileSystemError", err)
	}

	if err := os.MkdirAll(dstDir, 0755); err != nil {
		return errors.Wrap("FileSystemError", err)
	}

	for _, entry := range entries {
		srcPath := filepath.Join(srcDir, entry.Name())
		dstPath := filepath.Join(dstDir, entry.Name())

		if entry.IsDir() {
			if err := f.CopyDirContents(srcPath, dstPath); err != nil {
				return errors.Wrap("FileSystemError", err)
			}
		} else {
			if err := f.CopyFile(srcPath, dstPath); err != nil {
				return errors.Wrap("FileSystemError", err)
			}
		}
	}

	return nil
}

func (f *FileSystem) GetFileByPattern(dir string, filePattern string) (string, error) {
	pattern := filepath.Join(dir, filePattern)

	matches, err := filepath.Glob(pattern)
	if err != nil {
		return "", errors.Wrap("FileSystemError", err)
	}

	if len(matches) == 0 {
		return "", errors.NewError("FileSystemError_File_Not_Found")
	}

	return matches[0], nil
}
