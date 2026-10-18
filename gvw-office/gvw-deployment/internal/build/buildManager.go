package build

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"gvw/gvw-deployment/internal/utils"
	"os"
	"path/filepath"
	"slices"
	"strings"
	"sync"
)

type JavaClient interface {
	UpdateDeploymentState(deploymentId string, state string, reason uint32) error
}

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

type BuildManager struct {
	mutex         sync.Mutex
	activeCancels map[string]context.CancelFunc
	activeRunIds  map[string]uint64
	nextId        uint64
	githubRepoURL string
	javaClient    JavaClient
	fsWrapper     FileSystemWrapper
	logger        *utils.Logger
	modelLoader   *ModelLoader
}

func NewBuildManager(githubRepoURL string, javaClient JavaClient, fsWrapper FileSystemWrapper, logger *utils.Logger, modelLoader *ModelLoader) *BuildManager {
	return &BuildManager{
		activeCancels: make(map[string]context.CancelFunc),
		activeRunIds:  make(map[string]uint64),
		githubRepoURL: githubRepoURL,
		javaClient:    javaClient,
		fsWrapper:     fsWrapper,
		logger:        logger,
		modelLoader:   modelLoader,
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

	// !!!!! TODO: RE-ENABLE THIS BEFORE PUSHING!!!! THIS IS JUST FOR TESTING!!!!
	// err := b.cloneRepo(ctx, deploymentId, commit)
	// if err != nil {
	// 	b.logger.Printf("[BUILD] Failed to clone repo for deployment %s with commit %s", deploymentId, commit)
	// 	b.javaClient.UpdateDeploymentState(deploymentId, "CANCELLED", CodeCommitInvalid)
	// 	return
	// }

	cancelledReason := 0

	// Try three times to run the build
	for i := 0; i < 3; i++ {
		b.setupBuildDirs(deploymentId)

		// Frontend
		err := b.buildFrontend(ctx, deploymentId)
		if err != nil {
			if err.Error() == "TEST_FAIL" {
				cancelledReason = BUILD_CodeTestExecutionFailed
			} else {
				cancelledReason = BUILD_CodeBuildFailed
			}
			continue
		}

		// Backend
		err = b.buildBackend(ctx, deploymentId)
		if err != nil {
			if err.Error() == "TEST_FAIL" {
				cancelledReason = BUILD_CodeTestExecutionFailed
			} else {
				cancelledReason = BUILD_CodeBuildFailed
			}
			continue
		}

		// Extract binaries
		err = b.extractBinaries(deploymentId)
		if err != nil {
			cancelledReason = BUILD_CodeBinaryExtractionFailed
			continue
		}

		b.logger.Printf("Build successful for deployment %s", deploymentId)
		cancelledReason = 0
		break
	}

	if cancelledReason == 0 {
		err := b.generateSchema(deploymentId)
		if err != nil {
			cancelledReason = BUILD_CodeSchemaGenerationFailed
		}
	}

	if cancelledReason != 0 {
		b.logger.Printf("[BUILD] Build for deployment %s with commit %s failed", deploymentId, commit)
		err := b.javaClient.UpdateDeploymentState(deploymentId, "CANCELLED", uint32(cancelledReason))
		if err != nil {
			b.logger.Printf("[BUILD] Failed to updated deployment state: %v", err)
		}
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
	if err != nil {
		b.logger.Printf("[BUILD] Frontend build failed for deployment %s with error %v", deploymentId, err)
		return fmt.Errorf("BUILD_FAIL")
	}

	// Test
	b.logger.Printf("[BUILD] Testing frontend for deployment %s", deploymentId)
	output, err = utils.RunCommand(ctx, workingDir, "npm", "run", "test")

	b.logger.Printf("[BUILD] Test execution output for deployment %s:\n%s", deploymentId, output)

	if err != nil || strings.Contains(output, "fail") {
		b.logger.Printf("[BUILD] Frontend tests failed for deployment %s with error %v", deploymentId, err)
		return fmt.Errorf("TEST_FAIL")
	}

	return nil
}

func (b *BuildManager) buildBackend(ctx context.Context, deploymentId string) error {
	workingDir := "./builds/" + deploymentId + "/repo/gvw-office/gvw-backend"

	b.fsWrapper.CleanDir(workingDir + "/target")

	// Test
	b.logger.Printf("[BUILD] Testing backend for deployment %s", deploymentId)
	output, err := utils.RunCommand(ctx, workingDir, "./mvnw", "test")

	b.logger.Printf("[BUILD] Test execution output for deployment %s:\n%s", deploymentId, output)

	if err != nil || !strings.Contains(output, "BUILD SUCCESS") {
		b.logger.Printf("[BUILD] Backend tests failed for deployment %s with error %v", deploymentId, err)
		return fmt.Errorf("TEST_FAIL")
	}

	// Package
	b.logger.Printf("[BUILD] Building backend for deployment %s", deploymentId)
	_, err = utils.RunCommand(ctx, workingDir, "./mvnw", "package")
	if err != nil {
		b.logger.Printf("[BUILD] Backend build failed for deployment %s with error %v", deploymentId, err)
		return fmt.Errorf("BUILD_FAIL")
	}

	return nil
}

func (b *BuildManager) extractBinaries(deploymentId string) error {
	workingDir := "./builds/" + deploymentId

	b.logger.Printf("[BUILD] Extracting binaries for deployment %s", deploymentId)

	b.fsWrapper.CreateDir(workingDir+"/bin/frontend", 0755)
	err := b.fsWrapper.CopyDirContents(workingDir+"/repo/gvw-office/frontend/dist", workingDir+"/bin/frontend")
	if err != nil {
		b.logger.Printf("[BUILD] Failed extracting frontend binaries for deployment %s", deploymentId)
		return err
	}

	backendArtifact, err := b.fsWrapper.GetFileByPattern(workingDir+"/repo/gvw-office/gvw-backend/target", "gvw-backend-*.jar")
	if err != nil {
		b.logger.Printf("[BUILD] Failed locating backend binary for deployment %s", deploymentId)
		return err
	}

	dstFile := filepath.Join(workingDir+"/bin", "gvw-backend.jar")

	err = b.fsWrapper.CopyFile(backendArtifact, dstFile)
	if err != nil {
		b.logger.Printf("[BUILD] Failed extracting backend binary for deployment %s", deploymentId)
		return err
	}

	return nil
}

func (b *BuildManager) generateSchema(deploymentId string) error {
	workingDir := "./builds/" + deploymentId + "/repo/gvw-office/gvw-backend/src/main/java/com/gvw/gvwbackend/model"

	models, err := b.fsWrapper.ListDir(workingDir)
	if err != nil {
		b.logger.Printf("[BUILD] Failed to list model directory for deployment %s: %v", deploymentId, err)
		return err
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

	schemaPath := filepath.Join("./builds/" + deploymentId, "schema.json")
	if err := b.fsWrapper.WriteToFile(schemaPath, buf.Bytes()); err != nil {
		b.logger.Printf("[BUILD] Failed to write schema.json for deployment %s: %v", deploymentId, err)
		return err
	}

	b.logger.Printf("[BUILD] Successfully generated schema.json for deployment %s", deploymentId)

	return nil
}
