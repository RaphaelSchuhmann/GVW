package log

import (
	"bytes"
	"io"
	"log"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestNewLogger(t *testing.T) {
	logger := NewLogger()

	if logger == nil {
		t.Error("Expected non-nil Logger")
	}

	if logger.lumberjackLogger != nil {
		t.Error("Expected lumberjackLogger to be nil before SetLogFile is called")
	}

	if logger.logObj == nil {
		t.Error("Expected logObj to be initialized")
	}
}

func TestLogger_Println(t *testing.T) {
	var buf bytes.Buffer
	testLogger := &Logger{
		lumberjackLogger: nil,
		logObj:           log.New(&buf, "[TEST] ", log.Ldate|log.Ltime|log.Lshortfile),
	}

	testLogger.Println("test message")

	output := buf.String()
	if output == "" {
		t.Error("Expected output from Println")
	}

	if !bytes.Contains(buf.Bytes(), []byte("test message")) {
		t.Errorf("Expected output to contain 'test message', got: %s", output)
	}
}

func TestLogger_Printf(t *testing.T) {
	var buf bytes.Buffer
	testLogger := &Logger{
		lumberjackLogger: nil,
		logObj:           log.New(&buf, "[TEST] ", log.Ldate|log.Ltime|log.Lshortfile),
	}

	testLogger.Printf("test message with %s", "format")

	output := buf.String()
	if output == "" {
		t.Error("Expected output from Printf")
	}

	if !bytes.Contains(buf.Bytes(), []byte("test message with format")) {
		t.Errorf("Expected output to contain formatted message, got: %s", output)
	}
}

func TestLogger_Println_MultipleArgs(t *testing.T) {
	var buf bytes.Buffer
	testLogger := &Logger{
		lumberjackLogger: nil,
		logObj:           log.New(&buf, "[TEST] ", log.Ldate|log.Ltime|log.Lshortfile),
	}

	testLogger.Println("arg1", "arg2", 123, true)

	output := buf.String()
	if output == "" {
		t.Error("Expected output from Println with multiple args")
	}

	if !bytes.Contains(buf.Bytes(), []byte("arg1")) {
		t.Errorf("Expected output to contain 'arg1', got: %s", output)
	}
}

func TestLogger_Printf_VariousFormats(t *testing.T) {
	var buf bytes.Buffer
	testLogger := &Logger{
		lumberjackLogger: nil,
		logObj:           log.New(&buf, "[TEST] ", log.Ldate|log.Ltime|log.Lshortfile),
	}

	testLogger.Printf("String: %s", "test")
	if !bytes.Contains(buf.Bytes(), []byte("String: test")) {
		t.Errorf("Expected formatted string output")
	}

	buf.Reset()
	testLogger.Printf("Integer: %d", 42)
	if !bytes.Contains(buf.Bytes(), []byte("Integer: 42")) {
		t.Errorf("Expected formatted integer output")
	}

	buf.Reset()
	testLogger.Printf("Float: %f", 3.14)
	if !bytes.Contains(buf.Bytes(), []byte("Float: 3.14")) {
		t.Errorf("Expected formatted float output")
	}

	buf.Reset()
	testLogger.Printf("Boolean: %t", true)
	if !bytes.Contains(buf.Bytes(), []byte("Boolean: true")) {
		t.Errorf("Expected formatted boolean output")
	}

	buf.Reset()
	testLogger.Printf("Multiple: %s %d %f", "test", 42, 3.14)
	if !bytes.Contains(buf.Bytes(), []byte("Multiple: test 42")) {
		t.Errorf("Expected formatted multiple values output")
	}
}

func TestLogger_WritesToStdout(t *testing.T) {
	oldStdout := os.Stdout
	r, w, _ := os.Pipe()
	os.Stdout = w

	logger := NewLogger()
	logger.Println("stdout test")

	w.Close()
	os.Stdout = oldStdout

	var buf bytes.Buffer
	io.Copy(&buf, r)
	output := buf.String()

	if output == "" {
		t.Error("Expected output to stdout")
	}
}

func TestSetLogFile_CreatesLogFile(t *testing.T) {
	dir := t.TempDir()

	logger := NewLogger()
	if err := logger.SetLogFile(dir); err != nil {
		t.Fatalf("Expected no error from SetLogFile, got: %v", err)
	}

	if logger.lumberjackLogger == nil {
		t.Error("Expected lumberjackLogger to be set after SetLogFile")
	}

	logPath := filepath.Join(dir, "gvw-deployment.log")
	if _, err := os.Stat(logPath); err != nil {
		t.Errorf("Expected log file to exist at %s, got: %v", logPath, err)
	}
}

func TestSetLogFile_WritesToFile(t *testing.T) {
	dir := t.TempDir()

	logger := NewLogger()
	if err := logger.SetLogFile(dir); err != nil {
		t.Fatalf("Expected no error from SetLogFile, got: %v", err)
	}

	logger.Println("file log test message")

	content, err := os.ReadFile(filepath.Join(dir, "gvw-deployment.log"))
	if err != nil {
		t.Fatalf("Expected to read log file, got: %v", err)
	}

	if !strings.Contains(string(content), "file log test message") {
		t.Errorf("Expected log file to contain message, got: %s", string(content))
	}
}

func TestSetLogFile_CalledTwice(t *testing.T) {
	firstDir := t.TempDir()
	secondDir := t.TempDir()

	logger := NewLogger()
	if err := logger.SetLogFile(firstDir); err != nil {
		t.Fatalf("Expected no error from first SetLogFile, got: %v", err)
	}
	firstLogger := logger.lumberjackLogger

	if err := logger.SetLogFile(secondDir); err != nil {
		t.Fatalf("Expected no error from second SetLogFile, got: %v", err)
	}

	if logger.lumberjackLogger == nil {
		t.Error("Expected lumberjackLogger to be set after second SetLogFile")
	}
	if logger.lumberjackLogger == firstLogger {
		t.Error("Expected lumberjackLogger to be replaced on second SetLogFile")
	}

	logger.Println("second file test message")

	secondContent, err := os.ReadFile(filepath.Join(secondDir, "gvw-deployment.log"))
	if err != nil {
		t.Fatalf("Expected to read second log file, got: %v", err)
	}
	if !strings.Contains(string(secondContent), "second file test message") {
		t.Errorf("Expected second log file to contain message, got: %s", string(secondContent))
	}
}

func TestSetLogFile_InvalidPath(t *testing.T) {
	logger := NewLogger()

	// a path inside a file (not a directory) cannot be used as a log dir
	blocker := filepath.Join(t.TempDir(), "blocker")
	if err := os.WriteFile(blocker, []byte("not a dir"), 0o644); err != nil {
		t.Fatalf("Failed to create blocker file: %v", err)
	}
	invalidDir := filepath.Join(blocker, "logs")

	if err := logger.SetLogFile(invalidDir); err == nil {
		t.Error("Expected error from SetLogFile with invalid path")
	}

	if logger.lumberjackLogger != nil {
		t.Error("Expected lumberjackLogger to remain nil after failed SetLogFile")
	}
}
