package commandExecutor

import (
	"context"
	"gvw/gvw-deployment/internal/utils/errors"
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
			return outputStr, gvwErrors.Wrap("CommandExecutionError_Cancelled_Or_Timedout", ctx.Err())
		}

		return outputStr, gvwErrors.Wrap("CommandExecutionError_Command_Failed", err)
	}

	return outputStr, nil
}
