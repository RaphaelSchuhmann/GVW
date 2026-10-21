package commandExecutor

import (
	"context"
	"strings"
	"testing"
	"time"
)

func TestRunCommand_Success(t *testing.T) {
	ctx := context.Background()
	output, err := RunCommand(ctx, "", "echo", "hello")

	if err != nil {
		t.Errorf("Expected no error, got: %v", err)
	}

	if !strings.Contains(output, "hello") {
		t.Errorf("Expected output to contain 'hello', got: %s", output)
	}
}

func TestRunCommand_WithWorkDir(t *testing.T) {
	ctx := context.Background()
	output, err := RunCommand(ctx, "/tmp", "pwd")

	if err != nil {
		t.Errorf("Expected no error, got: %v", err)
	}

	if !strings.Contains(output, "/tmp") {
		t.Errorf("Expected output to contain '/tmp', got: %s", output)
	}
}

func TestRunCommand_CommandFails(t *testing.T) {
	ctx := context.Background()
	output, err := RunCommand(ctx, "", "ls", "/nonexistent/path")

	if err == nil {
		t.Error("Expected error for failing command")
	}

	if !strings.Contains(err.Error(), "command failed") {
		t.Errorf("Expected error message to contain 'command failed', got: %v", err)
	}

	// Output should still be returned even on error
	if output == "" {
		t.Error("Expected output even on command failure")
	}
}

func TestRunCommand_CommandNotFound(t *testing.T) {
	ctx := context.Background()
	_, err := RunCommand(ctx, "", "nonexistentcommand12345")

	if err == nil {
		t.Error("Expected error for non-existent command")
	}

	if !strings.Contains(err.Error(), "command failed") {
		t.Errorf("Expected error message to contain 'command failed', got: %v", err)
	}
}

func TestRunCommand_ContextCancelled(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	cancel()

	_, err := RunCommand(ctx, "", "sleep", "10")

	if err == nil {
		t.Error("Expected error for cancelled context")
	}

	if !strings.Contains(err.Error(), "COMMAND_CANCELLED_OR_TIMEDOUT") {
		t.Errorf("Expected error message to contain 'COMMAND_CANCELLED_OR_TIMEDOUT', got: %v", err)
	}
}

func TestRunCommand_ContextTimeout(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 100*time.Millisecond)
	defer cancel()

	_, err := RunCommand(ctx, "", "sleep", "1")

	if err == nil {
		t.Error("Expected error for timed out context")
	}

	if !strings.Contains(err.Error(), "COMMAND_CANCELLED_OR_TIMEDOUT") {
		t.Errorf("Expected error message to contain 'COMMAND_CANCELLED_OR_TIMEDOUT', got: %v", err)
	}
}

func TestRunCommand_MultipleArgs(t *testing.T) {
	ctx := context.Background()
	output, err := RunCommand(ctx, "", "echo", "hello", "world", "test")

	if err != nil {
		t.Errorf("Expected no error, got: %v", err)
	}

	if !strings.Contains(output, "hello") || !strings.Contains(output, "world") || !strings.Contains(output, "test") {
		t.Errorf("Expected output to contain all arguments, got: %s", output)
	}
}
