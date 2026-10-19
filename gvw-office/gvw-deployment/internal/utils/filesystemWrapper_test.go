package utils

import (
	"os"
	"path/filepath"
	"testing"
)

func TestNewFileSystem(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	if fsw == nil {
		t.Error("Expected non-nil FileSystemWrapper")
	}

	if fsw.logger == nil {
		t.Error("Expected logger to be set")
	}
}

func TestDirOrFileExists_ExistingFile(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	tmpFile, err := os.CreateTemp("", "test-*.txt")
	if err != nil {
		t.Fatalf("Failed to create temp file: %v", err)
	}
	defer os.Remove(tmpFile.Name())
	tmpFile.Close()

	exists := fsw.DirOrFileExists(tmpFile.Name())
	if !exists {
		t.Error("Expected file to exist")
	}
}

func TestDirOrFileExists_NonExistent(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	exists := fsw.DirOrFileExists("/nonexistent/path/file.txt")
	if exists {
		t.Error("Expected file to not exist")
	}
}

func TestCreateDir_Success(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	tmpDir, err := os.MkdirTemp("", "testdir-")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tmpDir)

	newDir := filepath.Join(tmpDir, "subdir", "nested")
	err = fsw.CreateDir(newDir, 0755)
	if err != nil {
		t.Errorf("Expected no error creating directory, got: %v", err)
	}

	if !fsw.DirOrFileExists(newDir) {
		t.Error("Expected directory to exist after creation")
	}
}

func TestCreateDir_ExistingDir(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	tmpDir, err := os.MkdirTemp("", "testdir-")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tmpDir)

	// Creating existing directory should not error
	err = fsw.CreateDir(tmpDir, 0755)
	if err != nil {
		t.Errorf("Expected no error creating existing directory, got: %v", err)
	}
}

func TestCleanDir_Success(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	tmpDir, err := os.MkdirTemp("", "testdir-")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}

	// Create a file in the directory
	testFile := filepath.Join(tmpDir, "test.txt")
	err = os.WriteFile(testFile, []byte("test content"), 0644)
	if err != nil {
		t.Fatalf("Failed to create test file: %v", err)
	}

	fsw.CleanDir(tmpDir)

	if fsw.DirOrFileExists(tmpDir) {
		t.Error("Expected directory to be cleaned (not exist)")
	}
}

func TestWriteToFile_Success(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	tmpFile, err := os.CreateTemp("", "test-*.txt")
	if err != nil {
		t.Fatalf("Failed to create temp file: %v", err)
	}
	defer os.Remove(tmpFile.Name())
	tmpFile.Close()

	content := []byte("test content")
	err = fsw.WriteToFile(tmpFile.Name(), content)
	if err != nil {
		t.Errorf("Expected no error writing to file, got: %v", err)
	}

	readContent, err := os.ReadFile(tmpFile.Name())
	if err != nil {
		t.Fatalf("Failed to read file: %v", err)
	}

	if string(readContent) != string(content) {
		t.Errorf("Expected content %s, got %s", string(content), string(readContent))
	}
}

func TestReadFile_Success(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	tmpFile, err := os.CreateTemp("", "test-*.txt")
	if err != nil {
		t.Fatalf("Failed to create temp file: %v", err)
	}
	defer os.Remove(tmpFile.Name())

	expectedContent := []byte("test content")
	tmpFile.Write(expectedContent)
	tmpFile.Close()

	content, err := fsw.ReadFile(tmpFile.Name())
	if err != nil {
		t.Errorf("Expected no error reading file, got: %v", err)
	}

	if string(content) != string(expectedContent) {
		t.Errorf("Expected content %s, got %s", string(expectedContent), string(content))
	}
}

func TestReadFile_NonExistent(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	_, err := fsw.ReadFile("/nonexistent/path/file.txt")
	if err == nil {
		t.Error("Expected error for non-existent file")
	}
}

func TestCopyFile_Success(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	tmpDir, err := os.MkdirTemp("", "testdir-")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tmpDir)

	srcFile := filepath.Join(tmpDir, "source.txt")
	dstFile := filepath.Join(tmpDir, "dest", "destination.txt")

	content := []byte("test content")
	err = os.WriteFile(srcFile, content, 0644)
	if err != nil {
		t.Fatalf("Failed to create source file: %v", err)
	}

	err = fsw.CopyFile(srcFile, dstFile)
	if err != nil {
		t.Errorf("Expected no error copying file, got: %v", err)
	}

	readContent, err := os.ReadFile(dstFile)
	if err != nil {
		t.Fatalf("Failed to read destination file: %v", err)
	}

	if string(readContent) != string(content) {
		t.Errorf("Expected content %s, got %s", string(content), string(readContent))
	}
}

func TestCopyFile_SourceNotFound(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	tmpDir, err := os.MkdirTemp("", "testdir-")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tmpDir)

	srcFile := filepath.Join(tmpDir, "nonexistent.txt")
	dstFile := filepath.Join(tmpDir, "dest.txt")

	err = fsw.CopyFile(srcFile, dstFile)
	if err == nil {
		t.Error("Expected error for non-existent source file")
	}
}

func TestListDir_Success(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	tmpDir, err := os.MkdirTemp("", "testdir-")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tmpDir)

	// Create some files
	os.WriteFile(filepath.Join(tmpDir, "file1.txt"), []byte("content1"), 0644)
	os.WriteFile(filepath.Join(tmpDir, "file2.txt"), []byte("content2"), 0644)

	entries, err := fsw.ListDir(tmpDir)
	if err != nil {
		t.Errorf("Expected no error listing directory, got: %v", err)
	}

	if len(entries) != 2 {
		t.Errorf("Expected 2 entries, got %d", len(entries))
	}
}

func TestListDir_NonExistent(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	_, err := fsw.ListDir("/nonexistent/path")
	if err == nil {
		t.Error("Expected error for non-existent directory")
	}
}

func TestCopyDirContents_Success(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	tmpDir, err := os.MkdirTemp("", "testdir-")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tmpDir)

	srcDir := filepath.Join(tmpDir, "source")
	dstDir := filepath.Join(tmpDir, "dest")

	// Create source directory structure
	os.MkdirAll(filepath.Join(srcDir, "subdir"), 0755)
	os.WriteFile(filepath.Join(srcDir, "file1.txt"), []byte("content1"), 0644)
	os.WriteFile(filepath.Join(srcDir, "subdir", "file2.txt"), []byte("content2"), 0644)

	err = fsw.CopyDirContents(srcDir, dstDir)
	if err != nil {
		t.Errorf("Expected no error copying directory contents, got: %v", err)
	}

	// Verify files were copied
	if !fsw.DirOrFileExists(filepath.Join(dstDir, "file1.txt")) {
		t.Error("Expected file1.txt to exist in destination")
	}

	if !fsw.DirOrFileExists(filepath.Join(dstDir, "subdir", "file2.txt")) {
		t.Error("Expected file2.txt to exist in destination subdirectory")
	}
}

func TestCopyDirContents_SourceNotFound(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	tmpDir, err := os.MkdirTemp("", "testdir-")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tmpDir)

	srcDir := filepath.Join(tmpDir, "nonexistent")
	dstDir := filepath.Join(tmpDir, "dest")

	err = fsw.CopyDirContents(srcDir, dstDir)
	if err == nil {
		t.Error("Expected error for non-existent source directory")
	}
}

func TestGetFileByPattern_Success(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	tmpDir, err := os.MkdirTemp("", "testdir-")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tmpDir)

	testFile := filepath.Join(tmpDir, "test-123.txt")
	os.WriteFile(testFile, []byte("content"), 0644)

	matchedFile, err := fsw.GetFileByPattern(tmpDir, "test-*.txt")
	if err != nil {
		t.Errorf("Expected no error, got: %v", err)
	}

	if matchedFile != testFile {
		t.Errorf("Expected %s, got %s", testFile, matchedFile)
	}
}

func TestGetFileByPattern_NoMatch(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	tmpDir, err := os.MkdirTemp("", "testdir-")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tmpDir)

	_, err = fsw.GetFileByPattern(tmpDir, "nonexistent-*.txt")
	if err == nil {
		t.Error("Expected error for no matching files")
	}
}

func TestGetFileByPattern_InvalidPattern(t *testing.T) {
	logger := NewLogger()
	fsw := NewFileSystem(logger)

	tmpDir, err := os.MkdirTemp("", "testdir-")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tmpDir)

	_, err = fsw.GetFileByPattern(tmpDir, "[invalid")
	if err == nil {
		t.Error("Expected error for invalid pattern")
	}
}
