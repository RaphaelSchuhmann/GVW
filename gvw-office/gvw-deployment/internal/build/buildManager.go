package build

import (
	"context"
	"sync"
)

type BuildManager struct {
	mutex         sync.Mutex
	activeCancels map[string]context.CancelFunc
	activeRunIds  map[string]uint64
	nextId        uint64
}

func NewBuildManager() *BuildManager {
	return &BuildManager{
		activeCancels: make(map[string]context.CancelFunc),
		activeRunIds:  make(map[string]uint64),
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
	
}
