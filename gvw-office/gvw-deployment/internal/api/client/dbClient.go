package client

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"iter"
	"maps"
	"net/http"
	"net/url"
	"time"

	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
)

type FindResponse struct {
	Docs     []map[string]any `json:"docs"`
	Bookmark string           `json:"bookmark"`
	Warning  string           `json:"warning"`
}

type BulkFailure struct {
	ID     string
	Error  string
	Reason string
}

type bulkResult struct {
	ID     string `json:"id"`
	OK     bool   `json:"ok"`
	Error  string `json:"error"`
	Reason string `json:"reason"`
}

type BulkError struct {
	Failed []BulkFailure
}

func (e *BulkError) Error() string {
	if len(e.Failed) == 0 {
		return "bulk update failed"
	}
	first := e.Failed[0]
	return fmt.Sprintf("%d document(s) failed to update, first: %s (%s: %s)", len(e.Failed), first.ID, first.Error, first.Reason)
}

type DocRow struct {
	ID  string         `json:"id"`
	Key string         `json:"key"`
	Doc map[string]any `json:"doc"`
}

type AllDocsResponse struct {
	TotalRows int      `json:"total_rows"`
	Offset    int      `json:"offset"`
	Rows      []DocRow `json:"rows"`
}

type DbClient struct {
	baseUrl    string
	username   string
	password   string
	httpClient *http.Client
}

func NewDbClient(baseUrl, username, password string) *DbClient {
	return &DbClient{
		baseUrl:  baseUrl,
		username: username,
		password: password,
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

func (d *DbClient) FindByQuery(ctx context.Context, database string, query map[string]any, limit int, bookmark string) (*FindResponse, error) {
	endpoint := fmt.Sprintf("%s/%s/_find", d.baseUrl, url.PathEscape(database))

	q := make(map[string]any, len(query)+2)
	maps.Copy(q, query)

	// Set default selector for valid query
	if _, ok := q["selector"]; !ok {
		q["selector"] = map[string]any{"_id": map[string]any{"$gt": nil}}
	}

	q["limit"] = limit
	if bookmark != "" {
		q["bookmark"] = bookmark
	}

	var page FindResponse
	if err := d.executeDbRequest(ctx, endpoint, http.MethodPost, q, &page); err != nil {
		return nil, err
	}
	return &page, nil
}

func (d *DbClient) FindEachByQuery(ctx context.Context, database string, query map[string]any, pageSize int) iter.Seq2[map[string]any, error] {
	return func(yield func(map[string]any, error) bool) {
		bookmark := ""
		for {
			page, err := d.FindByQuery(ctx, database, query, pageSize, bookmark)
			if err != nil {
				yield(nil, err)
				return
			}

			for _, doc := range page.Docs {
				if !yield(doc, nil) {
					return
				}
			}

			if len(page.Docs) == 0 || page.Bookmark == "" || page.Bookmark == bookmark {
				return
			}
			bookmark = page.Bookmark
		}
	}
}

func (d *DbClient) FetchAllDocsWithBody(ctx context.Context, dbName string) ([]map[string]any, error) {
	endpoint := fmt.Sprintf("%s/%s/_all_docs?include_docs=true", d.baseUrl, url.PathEscape(dbName))

	var resp AllDocsResponse
	if err := d.executeDbRequest(ctx, endpoint, http.MethodGet, nil, &resp); err != nil {
		return nil, fmt.Errorf("failed to fetch all docs: %w", err)
	}

	var docs []map[string]any
	for _, row := range resp.Rows {
		if row.Doc != nil {
			docs = append(docs, row.Doc)
		}
	}

	return docs, nil
}

// BulkUpdate writes the given documents (each must contain _id and the current _rev) with one request.
//
// CouchDB answers 201 even when single documents were rejected, so the result array is inspected:
// any entry with an error turns into a *BulkError wrapped in CodeDbBulkUpdateFailed.
func (d *DbClient) BulkUpdate(ctx context.Context, database string, docs []map[string]any) error {
	if len(docs) == 0 {
		return nil
	}

	endpoint := fmt.Sprintf("%s/%s/_bulk_docs", d.baseUrl, url.PathEscape(database))

	var results []bulkResult
	body := map[string]any{"docs": docs}
	if err := d.executeDbRequest(ctx, endpoint, http.MethodPost, body, &results); err != nil {
		return err
	}

	var failed []BulkFailure
	for _, r := range results {
		if r.Error != "" || !r.OK {
			failed = append(failed, BulkFailure{ID: r.ID, Error: r.Error, Reason: r.Reason})
		}
	}
	if len(failed) > 0 {
		return gvwErrors.Wrap(gvwErrors.CodeBulkUpdateContainedError, &BulkError{Failed: failed})
	}

	return nil
}

func (d *DbClient) executeDbRequest(ctx context.Context, url, method string, in, out any) error {
	return d.doDbRequest(ctx, d.httpClient, url, method, in, out)
}

func (d *DbClient) doDbRequest(ctx context.Context, httpClient *http.Client, url, method string, in, out any) error {
	var body io.Reader
	if in != nil {
		b, err := json.Marshal(in)
		if err != nil {
			return gvwErrors.Wrap(gvwErrors.CodeDbEncodeBody, err)
		}
		body = bytes.NewReader(b)
	}

	req, err := http.NewRequestWithContext(ctx, method, url, body)
	if err != nil {
		return gvwErrors.Wrap(gvwErrors.CodeDbBuildRequest, err)
	}

	req.Header.Set("Accept", "application/json")
	req.SetBasicAuth(d.username, d.password)
	if in != nil {
		req.Header.Set("Content-Type", "application/json")
	}

	resp, err := httpClient.Do(req)
	if err != nil {
		return gvwErrors.Wrap(gvwErrors.CodeDbSendRequest, err)
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		b, _ := io.ReadAll(io.LimitReader(resp.Body, 4096))
		return gvwErrors.Wrap(gvwErrors.CodeDbBadStatus,
			&gvwErrors.StatusError{StatusCode: resp.StatusCode, Body: string(b)})
	}

	if out == nil {
		return nil
	}
	if err := json.NewDecoder(resp.Body).Decode(out); err != nil {
		return gvwErrors.Wrap(gvwErrors.CodeDbDecodeBody, err)
	}
	return nil
}
