package utils

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
        if ctx.Err() == context.Canceled {
            return outputStr, fmt.Errorf("command cancelled: %w", ctx.Err())
        }

        return outputStr, fmt.Errorf("command failed: %w", err)
    }

    return outputStr, nil
}