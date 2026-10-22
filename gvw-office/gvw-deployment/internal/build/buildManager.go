package build

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"gvw/gvw-deployment/internal/api/client"
	"gvw/gvw-deployment/internal/utils/commandExecutor"
	"gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/filesystem"
	"gvw/gvw-deployment/internal/utils/log"
	"os"
	"path/filepath"
	"slices"
	"strings"
	"sync"
)

type activeBuild struct {
	cancel context.CancelFunc
	done   chan struct{}
}

type BuildManager struct {
	mutex         sync.Mutex
	activeBuilds  map[string]*activeBuild
	githubRepoURL string
	javaClient    client.JavaClient
	fsWrapper     filesystem.FileSystemWrapper
	logger        *log.Logger
	modelLoader   *ModelLoader
}

const pipelineCancelledLogMessage = "[BUILD] Pipeline cancelled for deployment %s: %v"
const buildBaseDir = "./builds/"

func NewBuildManager(githubRepoURL string, javaClient client.JavaClient, fsWrapper filesystem.FileSystemWrapper, logger *log.Logger, modelLoader *ModelLoader) *BuildManager {
	return &BuildManager{
		activeBuilds:  make(map[string]*activeBuild),
		githubRepoURL: githubRepoURL,
		javaClient:    javaClient,
		fsWrapper:     fsWrapper,
		logger:        logger,
		modelLoader:   modelLoader,
	}
}

func (b *BuildManager) TriggerBuild(deploymentId, commit string) {
	b.mutex.Lock()

	previous := b.activeBuilds[deploymentId]
	if previous != nil {
		b.logger.Printf("[BUILD] Cancelling build for deployment %s", deploymentId)
		previous.cancel()
	}

	ctx, cancel := context.WithCancel(context.Background())
	current := &activeBuild{
		cancel: cancel,
		done:   make(chan struct{}),
	}

	b.activeBuilds[deploymentId] = current

	b.mutex.Unlock()

	// Start build pipeline asynchronously in a goroutine
	go func() {
		defer func() {
			b.mutex.Lock()
			if b.activeBuilds[deploymentId] == current {
				delete(b.activeBuilds, deploymentId)
			}
			b.mutex.Unlock()

			close(current.done)
		}()

		if previous != nil {
			<-previous.done
		}

		b.mutex.Lock()
		isCurrent := b.activeBuilds[deploymentId] == current
		b.mutex.Unlock()

		if !isCurrent {
			return
		}

		b.runPipeline(ctx, deploymentId, commit)
	}()
}

func (b *BuildManager) runPipeline(ctx context.Context, deploymentId, commit string) {
	if ctx.Err() != nil {
		return
	}

	// Setup build environment
	b.fsWrapper.CreateDir(buildBaseDir+deploymentId, 0755)
	b.fsWrapper.CreateDir(buildBaseDir+deploymentId+"/repo", 0755)

	if err := b.cloneRepo(ctx, deploymentId, commit); err != nil {
		b.logger.Printf("[BUILD] Failed to clone repo for deployment %s with commit %s", deploymentId, commit)
		b.javaClient.UpdateDeploymentState(deploymentId, "CANCELLED", BUILD_CodeCommitInvalid)
		return
	}

	if err := b.executeBuilds(ctx, deploymentId); err != nil {
		if errors.Is(err, context.Canceled) || errors.Is(err, context.DeadlineExceeded) {
			b.logger.Printf(pipelineCancelledLogMessage, deploymentId, err)
			return
		}

		errorCode := MapBuildErrorsToCode(err)
		b.logger.Printf("[BUILD] Build for deployment %s with commit %s failed: %v", deploymentId, commit, err)

		if updateErr := b.javaClient.UpdateDeploymentState(deploymentId, "CANCELLED", errorCode); updateErr != nil {
			b.logger.Printf("[BUILD] Failed to update dpeloyment state: %v", updateErr)
		}
		return
	}

	if err := b.generateSchema(deploymentId); err != nil {
		b.logger.Printf("[BUILD] Schema generation failed for deployment %s: %v", deploymentId, err)
		if updateErr := b.javaClient.UpdateDeploymentState(deploymentId, "CANCELLED", BUILD_CodeSchemaGenerationFailed); updateErr != nil {
			b.logger.Printf("[BUILD] Failed to update dpeloyment state: %v", updateErr)
		}
		return
	}

	b.logger.Printf("[BUILD] Build pipeline completed successfully for deployment %s", deploymentId)
}

func (b *BuildManager) executeBuilds(ctx context.Context, deploymentId string) error {
	var lastErr error

	// Try three times to run the build
	for i := 0; i < 3; i++ {
		if err := ctx.Err(); err != nil {
			return err
		}

		b.setupBuildDirs(deploymentId)

		// Frontend
		if err := b.buildFrontend(ctx, deploymentId); err != nil {
			lastErr = err
			continue
		}
		if err := ctx.Err(); err != nil {
			return err
		}

		// Backend
		if err := b.buildBackend(ctx, deploymentId); err != nil {
			lastErr = err
			continue
		}
		if err := ctx.Err(); err != nil {
			return err
		}

		// Extract binaries
		if err := b.extractBinaries(deploymentId); err != nil {
			lastErr = err
			continue
		}
		if err := ctx.Err(); err != nil {
			return err
		}

		b.logger.Printf("[BUILD] Build successful for deployment %s", deploymentId)
		return nil
	}
	return lastErr
}

func (b *BuildManager) setupBuildDirs(deploymentId string) {
	b.fsWrapper.CleanDir(buildBaseDir + deploymentId + "/bin")
	b.fsWrapper.CleanDir(buildBaseDir + deploymentId + "/schema.json")

	b.fsWrapper.CreateDir(buildBaseDir+deploymentId+"/bin", 0755)
}

func (b *BuildManager) cloneRepo(ctx context.Context, deploymentId string, commit string) error {
	workingDir := buildBaseDir + deploymentId + "/repo"

	run := func(name string, args ...string) error {
		_, err := commandExecutor.RunCommand(ctx, workingDir, name, args...)
		return gvwErrors.Wrap("BuildError_Clone_Repo", err)
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
	workingDir := buildBaseDir + deploymentId + "/repo/gvw-office/frontend"

	b.fsWrapper.CleanDir(workingDir + "/dist")

	// Build
	b.logger.Printf("[BUILD] Building frontend for deployment %s", deploymentId)
	output, err := commandExecutor.RunCommand(ctx, workingDir, "npm", "run", "build")
	if err != nil {
		b.logger.Printf("[BUILD] Frontend build failed for deployment %s with error %v", deploymentId, err)
		return gvwErrors.Wrap("BuildError_Build_Failed", err)
	}

	// Test
	b.logger.Printf("[BUILD] Testing frontend for deployment %s", deploymentId)
	output, err = commandExecutor.RunCommand(ctx, workingDir, "npm", "run", "test")

	b.logger.Printf("[BUILD] Test execution output for deployment %s:\n%s", deploymentId, output)

	if err != nil || strings.Contains(output, "fail") {
		b.logger.Printf("[BUILD] Frontend tests failed for deployment %s with error %v", deploymentId, err)
		return gvwErrors.Wrap("BuildError_Test_Failed", err)
	}

	return nil
}

func (b *BuildManager) buildBackend(ctx context.Context, deploymentId string) error {
	workingDir := buildBaseDir + deploymentId + "/repo/gvw-office/gvw-backend"

	b.fsWrapper.CleanDir(workingDir + "/target")

	// Test
	b.logger.Printf("[BUILD] Testing backend for deployment %s", deploymentId)
	output, err := commandExecutor.RunCommand(ctx, workingDir, "./mvnw", "test")

	b.logger.Printf("[BUILD] Test execution output for deployment %s:\n%s", deploymentId, output)

	if err != nil || !strings.Contains(output, "BUILD SUCCESS") {
		b.logger.Printf("[BUILD] Backend tests failed for deployment %s with error %v", deploymentId, err)
		return gvwErrors.Wrap("BuildError_Test_Failed", err)
	}

	// Package
	b.logger.Printf("[BUILD] Building backend for deployment %s", deploymentId)
	_, err = commandExecutor.RunCommand(ctx, workingDir, "./mvnw", "package")
	if err != nil {
		b.logger.Printf("[BUILD] Backend build failed for deployment %s with error %v", deploymentId, err)
		return gvwErrors.Wrap("BuildError_Build_Failed", err)
	}

	return nil
}

func (b *BuildManager) extractBinaries(deploymentId string) error {
	workingDir := buildBaseDir + deploymentId

	b.logger.Printf("[BUILD] Extracting binaries for deployment %s", deploymentId)

	b.fsWrapper.CreateDir(workingDir+"/bin/frontend", 0755)
	err := b.fsWrapper.CopyDirContents(workingDir+"/repo/gvw-office/frontend/dist", workingDir+"/bin/frontend")
	if err != nil {
		b.logger.Printf("[BUILD] Failed extracting frontend binaries for deployment %s", deploymentId)
		return gvwErrors.Wrap("BuildError_Extract_Binaries_Copy_Frontend", err)
	}

	backendArtifact, err := b.fsWrapper.GetFileByPattern(workingDir+"/repo/gvw-office/gvw-backend/target", "gvw-backend-*.jar")
	if err != nil {
		b.logger.Printf("[BUILD] Failed locating backend binary for deployment %s", deploymentId)
		return gvwErrors.Wrap("BuildError_Extract_Binaries_Find_Backend", err)
	}

	dstFile := filepath.Join(workingDir+"/bin", "gvw-backend.jar")

	err = b.fsWrapper.CopyFile(backendArtifact, dstFile)
	if err != nil {
		b.logger.Printf("[BUILD] Failed extracting backend binary for deployment %s", deploymentId)
		return gvwErrors.Wrap("BuildError_Extract_Binaries_Copy_Backend", err)
	}

	return nil
}

func (b *BuildManager) generateSchema(deploymentId string) error {
	workingDir := buildBaseDir + deploymentId + "/repo/gvw-office/gvw-backend/src/main/java/com/gvw/gvwbackend/model"

	models, err := b.fsWrapper.ListDir(workingDir)
	if err != nil {
		b.logger.Printf("[BUILD] Failed to list model directory for deployment %s: %v", deploymentId, err)
		return gvwErrors.Wrap("BuildError_Generate_Schema_Get_Models", err)
	}

	models = slices.DeleteFunc(models, func(m os.DirEntry) bool {
		return m.IsDir()
	})

	rawSchema := make(map[string]any)

	for _, model := range models {
		modelMap, err := b.modelLoader.LoadModel(filepath.Join(workingDir, model.Name()))
		if err != nil {
			b.logger.Printf("[BUILD] Error while generating model schema for deployment %s in model %s: %v", deploymentId, model.Name(), err)
			return err
		}

		for k, v := range modelMap {
			rawSchema[k] = v
		}
	}

	validatedSchema, err := b.modelLoader.NormalizeAndValidateSchema(rawSchema)
	if err != nil {
		b.logger.Printf("[BUILD] Schema validation failed: %v", err)
		return err
	}

	b.modelLoader.MarkEmbeddedClasses(validatedSchema)

	var buf bytes.Buffer
	encoder := json.NewEncoder(&buf)

	encoder.SetEscapeHTML(false)
	encoder.SetIndent("", "  ")

	if err := encoder.Encode(validatedSchema); err != nil {
		b.logger.Printf("[BUILD] Failed to encode models map to JSON for deployment %s: %v", deploymentId, err)
		return err
	}

	schemaPath := filepath.Join(buildBaseDir+deploymentId, "schema.json")
	if err := b.fsWrapper.WriteToFile(schemaPath, buf.Bytes()); err != nil {
		b.logger.Printf("[BUILD] Failed to write schema.json for deployment %s: %v", deploymentId, err)
		return err
	}

	b.logger.Printf("[BUILD] Successfully generated schema.json for deployment %s", deploymentId)

	return nil
}
