package commandExecutor

import (
	"context"
	"fmt"
	"os/exec"
)

func RunCommand(ctx context.Context, workDir string, name string, args ...string) (string, error) {
    cmd := exec.CommandContext(ctx, name, args...)

    if workDir != "" {
        cmd.Dir = workDir
    }

    outputBytes, err := cmd.CombinedOutput()
    outputStr := string(outputBytes)

    if err != nil {
        if ctx.Err() != nil {
            return outputStr, fmt.Errorf("COMMAND_CANCELLED_OR_TIMEDOUT: %w", ctx.Err())
        }

        return outputStr, fmt.Errorf("command failed: %w", err)
    }

    return outputStr, nil
}