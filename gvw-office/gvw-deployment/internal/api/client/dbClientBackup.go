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
	Key   string `json:"key"`
	Value struct {
		Rev string `json:"rev"`
	} `json:"value"`
}

type revsResponse struct {
	Rows []revRow `json:"rows"`
}

func (d *DbClient) dbURL(name string) string {
	return fmt.Sprintf("%s/%s", d.configManager.Config.DatabaseURL, url.PathEscape(name))
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
	if err := d.doDbRequest(ctx, longRunningClient, d.configManager.Config.DatabaseURL+"/_replicate", http.MethodPost, payload, &result); err != nil {
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
func (d *DbClient) RestoreDatabaseFromBackup(ctx context.Context, targetDatabase string, backupDatabase string) error {
	// Read the current target revisions page-by-page.
	var revPage revsResponse
	var revPageIndex int
	var revPageStartKey *string
	revPageLoaded := false
	revPagesDone := false

	nextCurrentRevision := func() (revRow, bool, error) {
		for {
			if revPageIndex < len(revPage.Rows) {
				row := revPage.Rows[revPageIndex]
				revPageIndex++
				return row, true, nil
			}

			if revPagesDone {
				return revRow{}, false, nil
			}

			if revPageLoaded {
				if len(revPage.Rows) < restoreBatchSize {
					revPagesDone = true
					return revRow{}, false, nil
				}

				lastKey := revPage.Rows[len(revPage.Rows)-1].Key
				revPageStartKey = &lastKey
			}

			if err := d.fetchAllDocsPage(
				ctx,
				targetDatabase,
				false,
				revPageStartKey,
				&revPage,
			); err != nil {
				return revRow{}, false, gvwErrors.Wrap(
					gvwErrors.CodeDbBackupRestorationFailed,
					fmt.Errorf("failed to read current revisions: %w", err),
				)
			}

			revPageLoaded = true
			revPageIndex = 0

			if len(revPage.Rows) == 0 {
				revPagesDone = true
				return revRow{}, false, nil
			}
		}
	}

	currentRev, hasCurrentRev, err := nextCurrentRevision()
	if err != nil {
		return err
	}

	// Backup documents and target revisions are both ordered by document ID.
	// Walk them together instead of retaining every target revision in memory.
	var backupPage AllDocsResponse
	var backupPageStartKey *string

	toWrite := make([]map[string]any, 0, restoreBatchSize)

	flushBatch := func() error {
		if len(toWrite) == 0 {
			return nil
		}

		if err := d.BulkUpdate(ctx, targetDatabase, toWrite); err != nil {
			return gvwErrors.Wrap(
				gvwErrors.CodeDbBackupRestorationFailed,
				fmt.Errorf("failed to restore docs: %w", err),
			)
		}

		toWrite = toWrite[:0]
		return nil
	}

	for {
		if err := d.fetchAllDocsPage(ctx, backupDatabase, true, backupPageStartKey, &backupPage); err != nil {
			return gvwErrors.Wrap(
				gvwErrors.CodeDbBackupRestorationFailed,
				fmt.Errorf("failed to read backup docs: %w", err),
			)
		}

		for _, row := range backupPage.Rows {
			doc := row.Doc
			if doc == nil {
				continue
			}

			backupRev, _ := doc["_rev"].(string)

			// Advance the target revision stream until it reaches this
			// backup document or passes it.
			for hasCurrentRev && currentRev.Key < row.Key {
				currentRev, hasCurrentRev, err = nextCurrentRevision()
				if err != nil {
					return err
				}
			}

			if hasCurrentRev && currentRev.Key == row.Key {
				if currentRev.Value.Rev == backupRev {
					continue // untouched since the backup
				}

				doc["_rev"] = currentRev.Value.Rev
			} else {
				delete(doc, "_rev") // deleted in the target: recreate it
			}

			toWrite = append(toWrite, doc)

			if len(toWrite) == restoreBatchSize {
				if err := flushBatch(); err != nil {
					return err
				}
			}
		}

		if len(backupPage.Rows) < restoreBatchSize {
			break
		}

		lastKey := backupPage.Rows[len(backupPage.Rows)-1].Key
		backupPageStartKey = &lastKey
	}

	return flushBatch()
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
