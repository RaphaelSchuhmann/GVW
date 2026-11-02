package client

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"io"
	"iter"
	"maps"
	"net/http"
	"net/url"
	"time"
)

type FindResponse struct {
	Docs     []map[string]any `json:"docs"`
	Bookmark string           `json:"bookmark"`
	Warning  string           `json:"warning"`
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

func (d *DbClient) executeDbRequest(ctx context.Context, url, method string, in, out any) error {
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

	resp, err := d.httpClient.Do(req)
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
