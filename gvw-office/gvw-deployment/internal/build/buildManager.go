package build

import (
	"context"
	"fmt"
	"gvw/gvw-deployment/internal/api/client"
	"gvw/gvw-deployment/internal/utils"
	"path/filepath"
	"strings"
	"sync"
)

type BuildManager struct {
	mutex         sync.Mutex
	activeCancels map[string]context.CancelFunc
	activeRunIds  map[string]uint64
	nextId        uint64
	githubRepoURL string
	javaClient    *client.JavaClient
	fsWrapper     *utils.FileSytemWrapper
	logger        *utils.Logger
}

func NewBuildManager(githubRepoURL string, javaClient *client.JavaClient, fsWrapper *utils.FileSytemWrapper, logger *utils.Logger) *BuildManager {
	return &BuildManager{
		activeCancels: make(map[string]context.CancelFunc),
		activeRunIds:  make(map[string]uint64),
		githubRepoURL: githubRepoURL,
		javaClient:    javaClient,
		fsWrapper:     fsWrapper,
		logger:        logger,
	}
}

func (b *BuildManager) TriggerBuild(deploymentId string, commit string) {
	b.mutex.Lock()

	if cancel, exists := b.activeCancels[deploymentId]; exists {
		cancel()
	}

	ctx, cancel := context.WithCancel(context.Background())
	b.activeCancels[deploymentId] = cancel

	b.nextId++
	currentRunId := b.nextId
	b.activeRunIds[deploymentId] = currentRunId

	b.mutex.Unlock()

	// Start build pipeline asynchronously in a goroutine
	go func() {
		defer func() {
			b.mutex.Lock()
			if b.activeRunIds[deploymentId] == currentRunId {
				delete(b.activeCancels, deploymentId)
			}
			b.mutex.Unlock()
		}()

		b.runPipeline(ctx, deploymentId, commit)
	}()
}

func (b *BuildManager) runPipeline(ctx context.Context, deploymentId string, commit string) {
	// Note: ctx just needs to get passed to command executions

	// Setup build environment
	b.fsWrapper.CreateDir("./builds/"+deploymentId, 0755)
	b.fsWrapper.CreateDir("./builds/"+deploymentId+"/repo", 0755)

	err := b.cloneRepo(ctx, deploymentId, commit)
	if err != nil {
		b.logger.Printf("[BUILD] Failed to clone repo for deployment %s with commit %s", deploymentId, commit)
		b.javaClient.UpdateDeploymentState(deploymentId, "CANCELLED", CodeCommitInvalid)
		return
	}

	cancelledReason := 0

	// Try three times to run the build
	for i := 0; i < 3; i++ {
		b.setupBuildDirs(deploymentId)

		// Frontend
		err := b.buildFrontend(ctx, deploymentId)
		if err != nil {
			if err.Error() == "TEST_FAIL" {
				cancelledReason = CodeTestExecutionFailed
			} else {
				cancelledReason = CodeBuildFailed
			}
			continue
		}

		// Backend
		err = b.buildBackend(ctx, deploymentId)
		if err != nil {
			if err.Error() == "TEST_FAIL" {
				cancelledReason = CodeTestExecutionFailed
			} else {
				cancelledReason = CodeBuildFailed
			}
			continue
		}

		// Extract binaries
		err = b.extractBinaries(deploymentId)
		if err != nil {
			cancelledReason = CodeBinaryExtractionFailed
			continue
		}
	}

	if cancelledReason == 0 {

	}

	if cancelledReason != 0 {
		b.logger.Fatalf("[BUILD] Build for deployment %s with commit %s failed", deploymentId, commit)
		b.javaClient.UpdateDeploymentState(deploymentId, "CANCELLED", uint32(cancelledReason))
	}
}

func (b *BuildManager) setupBuildDirs(deploymentId string) {
	b.fsWrapper.CleanDir("./builds/" + deploymentId + "/bin")
	b.fsWrapper.CleanDir("./builds/" + deploymentId + "/schema.json")

	b.fsWrapper.CreateDir("./builds/"+deploymentId+"/bin", 0755)
}

func (b *BuildManager) cloneRepo(ctx context.Context, deploymentId string, commit string) error {
	workingDir := "./builds/" + deploymentId + "/repo"

	run := func(name string, args ...string) error {
		_, err := utils.RunCommand(ctx, workingDir, name, args...)
		return err
	}

	if err := run("git", "init"); err != nil {
		return err
	}

	if err := run("git", "remote", "add", "origin", b.githubRepoURL); err != nil {
		return err
	}

	if err := run("git", "fetch", "--depth", "1", "origin", commit); err != nil {
		return err
	}

	if err := run("git", "checkout", "FETCH_HEAD"); err != nil {
		return err
	}

	return nil
}

func (b *BuildManager) buildFrontend(ctx context.Context, deploymentId string) error {
	workingDir := "./builds/" + deploymentId + "/repo/gvw-office/frontend"

	b.fsWrapper.CleanDir(workingDir + "/dist")

	// Build
	b.logger.Printf("[BUILD] Building frontend for deployment %s", deploymentId)
	output, err := utils.RunCommand(ctx, workingDir, "npm", "run", "build")
	if err != nil || strings.Contains(output, "error") {
		b.logger.Printf("[BUILD] Frontend build failed for deployment %s", deploymentId)
		return fmt.Errorf("BUILD_FAIL")
	}

	// Test
	b.logger.Printf("[BUILD] Testing frontend for deployment %s", deploymentId)
	output, err = utils.RunCommand(ctx, workingDir, "npm", "run", "test")

	b.logger.Printf("[BUILD] Test execution output for deployment %s:\n%s", deploymentId, output)

	if err != nil || strings.Contains(output, "fail") {
		b.logger.Printf("[BUILD] Frontend tests failed for deployment %s", deploymentId)
		return fmt.Errorf("TEST_FAIL")
	}

	return nil
}

func (b *BuildManager) buildBackend(ctx context.Context, deploymentId string) error {
	workingDir := "./builds/" + deploymentId + "/repo/gvw-office/gvw-backend"

	b.fsWrapper.CleanDir(workingDir + "/target")

	// Build
	b.logger.Printf("[BUILD] Building backend for deployment %s", deploymentId)
	output, err := utils.RunCommand(ctx, workingDir, "./mvnw", "build")
	if err != nil || strings.Contains(output, "error") {
		b.logger.Printf("[BUILD] Backend build failed for deployment %s", deploymentId)
		return fmt.Errorf("BUILD_FAIL")
	}

	// Test
	b.logger.Printf("[BUILD] Testing backend for deployment %s", deploymentId)
	output, err = utils.RunCommand(ctx, workingDir, "./mvnw", "test")

	b.logger.Printf("[BUILD] Test execution output for deployment %s:\n%s", deploymentId, output)

	if err != nil || strings.Contains(output, "fail") || strings.Contains(output, "error") {
		b.logger.Printf("[BUILD] Backend tests failed for deployment %s", deploymentId)
		return fmt.Errorf("TEST_FAIL")
	}

	return nil
}

func (b *BuildManager) extractBinaries(deploymentId string) error {
	workingDir := "./builds/" + deploymentId

	b.fsWrapper.CreateDir(workingDir+"/bin/frontend", 0755)
	b.fsWrapper.CopyDirContents(workingDir+"/repo/gvw-office/frontend/dist", workingDir+"/bin/frontend")

	backendArtifact, err := b.fsWrapper.GetFileByPattern(workingDir+"/repo/gvw-office/gvw-backend/target", "gvw-backend-*.jar")
	if err != nil {
		return err
	}

	dstFile := filepath.Join(workingDir+"/bin", "gvw-backend.jar")

	err = b.fsWrapper.CopyFile(backendArtifact, dstFile)
	if err != nil {
		return err
	}

	return nil
}

func (b *BuildManager) generateSchema(deploymentId string) error {
	

	return nil
}
