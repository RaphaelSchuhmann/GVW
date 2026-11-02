package client

import (
	"context"
	"errors"
	"fmt"
	"net/http"
	"net/url"

	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
)

const (
	// CouchDB database names must be lowercase.
	backupSuffix     = "_backup"
	restoreBatchSize = 100
)

// longRunningClient has no timeout: replication and reading a whole database easily take longer than
// the 10s of d.httpClient. Cancellation still works through the context.
var longRunningClient = &http.Client{}

type dbInfo struct {
	DocCount int `json:"doc_count"`
}

type replicationResult struct {
	OK      bool `json:"ok"`
	History []struct {
		DocWriteFailures int `json:"doc_write_failures"`
	} `json:"history"`
}

type revRow struct {
	ID    string `json:"id"`
	Value struct {
		Rev string `json:"rev"`
	} `json:"value"`
}

type revsResponse struct {
	Rows []revRow `json:"rows"`
}

func (d *DbClient) dbURL(name string) string {
	return fmt.Sprintf("%s/%s", d.baseUrl, url.PathEscape(name))
}

// CreateBackupOfDatabase copies the database into "<database>_backup" and verifies the copy.
// An already existing backup database is never overwritten or deleted: it may be the only good copy
// of an earlier failed run and has to be checked by hand.
func (d *DbClient) CreateBackupOfDatabase(ctx context.Context, database string) (string, error) {
	backupDb := database + backupSuffix

	if err := d.executeDbRequest(ctx, d.dbURL(backupDb), http.MethodPut, nil, nil); err != nil {
		var se *gvwErrors.StatusError
		if errors.As(err, &se) && se.StatusCode == http.StatusPreconditionFailed {
			err = fmt.Errorf("backup database %s already exists (left over from an earlier run, check it before deleting it): %w", backupDb, err)
		}
		return "", gvwErrors.Wrap(gvwErrors.CodeDbBackupDbCreationFailed, err)
	}

	if err := d.replicateAndVerify(ctx, database, backupDb); err != nil {
		// Best effort, so a half-filled backup does not block the next run.
		_ = d.deleteDatabase(ctx, backupDb)
		return "", gvwErrors.Wrap(gvwErrors.CodeDbBackupDbReplicationFailed, err)
	}

	return backupDb, nil
}

func (d *DbClient) replicateAndVerify(ctx context.Context, source, target string) error {
	var result replicationResult
	payload := map[string]any{"source": source, "target": target}
	if err := d.doDbRequest(ctx, longRunningClient, d.baseUrl+"/_replicate", http.MethodPost, payload, &result); err != nil {
		return err
	}

	if !result.OK {
		return errors.New("replication did not report success")
	}
	for _, h := range result.History {
		if h.DocWriteFailures > 0 {
			return fmt.Errorf("replication reported %d document write failure(s)", h.DocWriteFailures)
		}
	}

	srcCount, err := d.docCount(ctx, source)
	if err != nil {
		return err
	}
	dstCount, err := d.docCount(ctx, target)
	if err != nil {
		return err
	}
	if srcCount != dstCount {
		return fmt.Errorf("backup has %d documents but the source has %d", dstCount, srcCount)
	}

	return nil
}

func (d *DbClient) docCount(ctx context.Context, database string) (int, error) {
	var info dbInfo
	if err := d.executeDbRequest(ctx, d.dbURL(database), http.MethodGet, nil, &info); err != nil {
		return 0, err
	}
	return info.DocCount, nil
}

func (d *DbClient) deleteDatabase(ctx context.Context, name string) error {
	err := d.executeDbRequest(ctx, d.dbURL(name), http.MethodDelete, nil, nil)

	var se *gvwErrors.StatusError
	if errors.As(err, &se) && se.StatusCode == http.StatusNotFound {
		return nil // already gone
	}
	return err
}

// RestoreDatabaseFromBackup rolls the target database back to the state of the backup.
//
// Writing the old revisions with new_edits=false does not work: the migrated revision is a descendant of the
// backup revision and keeps winning. Instead every changed document is written again as a normal new edit on
// top of its current revision, carrying the body of the backup. Documents whose current revision equals the
// backup revision were never touched and are skipped.
//
// Not covered: documents that were created in the target after the backup was taken are not deleted.
func (d *DbClient) RestoreDatabaseFromBackup(ctx context.Context, targetDatabase, backupDatabase string) error {
	var revs revsResponse
	if err := d.doDbRequest(ctx, longRunningClient, d.dbURL(targetDatabase)+"/_all_docs", http.MethodGet, nil, &revs); err != nil {
		return gvwErrors.Wrap(gvwErrors.CodeDbBackupRestorationFailed, fmt.Errorf("failed to read current revisions: %w", err))
	}
	currentRevs := make(map[string]string, len(revs.Rows))
	for _, row := range revs.Rows {
		currentRevs[row.ID] = row.Value.Rev
	}

	var backup AllDocsResponse
	if err := d.doDbRequest(ctx, longRunningClient, d.dbURL(backupDatabase)+"/_all_docs?include_docs=true", http.MethodGet, nil, &backup); err != nil {
		return gvwErrors.Wrap(gvwErrors.CodeDbBackupRestorationFailed, fmt.Errorf("failed to read backup docs: %w", err))
	}

	var toWrite []map[string]any
	for _, row := range backup.Rows {
		doc := row.Doc
		if doc == nil {
			continue
		}

		id, _ := doc["_id"].(string)
		backupRev, _ := doc["_rev"].(string)

		if current, exists := currentRevs[id]; exists {
			if current == backupRev {
				continue // untouched since the backup
			}
			doc["_rev"] = current
		} else {
			delete(doc, "_rev") // deleted in the target: recreate it
		}
		toWrite = append(toWrite, doc)
	}

	for start := 0; start < len(toWrite); start += restoreBatchSize {
		end := min(start+restoreBatchSize, len(toWrite))
		if err := d.BulkUpdate(ctx, targetDatabase, toWrite[start:end]); err != nil {
			return gvwErrors.Wrap(gvwErrors.CodeDbBackupRestorationFailed, fmt.Errorf("failed to restore docs: %w", err))
		}
	}

	return nil
}

// ClearDatabaseBackups deletes all backup databases. It keeps going after a failure and reports all of them.
func (d *DbClient) ClearDatabaseBackups(ctx context.Context, backups map[string]string) error {
	var errs []error

	for _, backup := range backups {
		if backup == "" {
			continue
		}
		if err := d.deleteDatabase(ctx, backup); err != nil {
			errs = append(errs, fmt.Errorf("deleting %s: %w", backup, err))
		}
	}

	if len(errs) > 0 {
		return gvwErrors.Wrap(gvwErrors.CodeDbBackupDbDeletionFailed, errors.Join(errs...))
	}
	return nil
}
