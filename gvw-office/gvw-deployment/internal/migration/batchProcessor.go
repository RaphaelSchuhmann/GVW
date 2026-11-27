package migration

import (
	"context"
	"errors"
	"gvw/gvw-deployment/internal/api/client"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
)

type batchProcessor struct {
	engine   *MigrationEngine
	database string
	stats    *migrationStats
	batch    []map[string]any
}

func NewBatchProcessor(e *MigrationEngine, database string, stats *migrationStats) *batchProcessor {
	return &batchProcessor{
		engine:   e,
		database: database,
		stats:    stats,
	}
}

func (b *batchProcessor) Add(ctx context.Context, doc map[string]any) error {
	b.batch = append(b.batch, doc)
	if len(b.batch) >= bulkWriteBatchSize {
		return b.Flush(ctx)
	}
	return nil
}

func (b *batchProcessor) Flush(ctx context.Context) error {
	if len(b.batch) == 0 {
		return nil
	}

	if err := b.engine.db.BulkUpdate(ctx, b.database, b.batch); err != nil {
		documentId := ""
		var be *client.BulkError
		if errors.As(err, &be) && len(be.Failed) > 0 {
			documentId = be.Failed[0].ID
		}
		return &gvwErrors.MigrationError{Err: err, DocumentId: documentId}
	}

	b.stats.Changed += len(b.batch)
	b.batch = b.batch[:0]
	return nil
}
