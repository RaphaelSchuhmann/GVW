package log

import (
	"bytes"
	"io"
	"log"
	"os"
	"testing"
)

func TestNewLogger(t *testing.T) {
	logger := NewLogger()

	if logger == nil {
		t.Error("Expected non-nil Logger")
	}

	if logger.lumberjackLogger == nil {
		t.Error("Expected lumberjackLogger to be initialized")
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
