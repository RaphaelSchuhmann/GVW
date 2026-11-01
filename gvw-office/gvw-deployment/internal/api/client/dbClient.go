package client

import (
	"bytes"
	"context"
	"encoding/json"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"io"
	"net/http"
	"time"
)

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

func (d *DbClient) executeDbRequest(ctx context.Context, path, method string, in, out any) error {
	var body io.Reader
	if in != nil {
		b, err := json.Marshal(in)
		if err != nil {
			return gvwErrors.Wrap(gvwErrors.CodeDbEncodeBody, err)
		}
		body = bytes.NewReader(b)
	}

	req, err := http.NewRequestWithContext(ctx, method, d.baseUrl+path, body)
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
