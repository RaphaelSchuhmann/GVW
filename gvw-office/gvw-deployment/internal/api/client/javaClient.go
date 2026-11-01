package client

import (
	"bytes"
	"context"
	"encoding/json"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"io"
	"net/http"
	"net/url"
	"time"
)

var _ JavaClient = (*HttpJavaClient)(nil)

type Status string

const (
	StatusSuccess   Status = "SUCCESS"
	StatusFailed    Status = "FAILED"
	StatusCancelled Status = "CANCELLED"
)

type StatusPayload struct {
	Status Status `json:"status"`
	Reason uint32 `json:"reason"`
}

type MigrationDryRunPayload struct {
	DeploymentId string `json:"deployment"`
	Status       Status `json:"status"`
	Database     string `json:"database"`
	DocumentId   string `json:"documentId"`
	Reason       uint32 `json:"reason"`
}

type JavaClient interface {
	UpdateDeploymentState(deploymentId string, status Status, reason uint32, ctx context.Context) error
	UpdateMigrationDryRunState(deploymentId string, status Status, database string, documentId string, reason uint32, ctx context.Context) error
}

type HttpJavaClient struct {
	baseUrl    string
	token      string
	httpClient *http.Client
}

func NewHttpJavaClient(baseUrl, token string) *HttpJavaClient {
	return &HttpJavaClient{
		baseUrl: baseUrl,
		token:   token,
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

func (c *HttpJavaClient) UpdateDeploymentState(deploymentId string, status Status, reason uint32, ctx context.Context) error {
	endpoint := c.baseUrl + "/internal/deployment/" + url.PathEscape(deploymentId) + "/status"

	payload := StatusPayload{
		Status: status,
		Reason: reason,
	}

	jsonBody, err := json.Marshal(payload)
	if err != nil {
		return gvwErrors.Wrap(gvwErrors.CodeHttpError, err)
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPatch, endpoint, bytes.NewBuffer(jsonBody))
	if err != nil {
		return gvwErrors.Wrap(gvwErrors.CodeHttpError, err)
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Internal-Auth", c.token)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return gvwErrors.Wrap(gvwErrors.CodeHttpError, err)
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		b, _ := io.ReadAll(io.LimitReader(resp.Body, 4096))
		return gvwErrors.Wrap(gvwErrors.CodeHttpError,
			&gvwErrors.StatusError{StatusCode: resp.StatusCode, Body: string(b)})
	}

	return nil
}

func (c *HttpJavaClient) UpdateMigrationDryRunState(deploymentId string, status Status, database string, documentId string, reason uint32, ctx context.Context) error {
	endpoint := c.baseUrl + "/internal/migration/dry-run/" + url.PathEscape(deploymentId) + "/status"

	payload := MigrationDryRunPayload{
		DeploymentId: deploymentId,
		Status:       status,
		Database:     database,
		DocumentId:   documentId,
		Reason:       reason,
	}

	jsonBody, err := json.Marshal(payload)
	if err != nil {
		return gvwErrors.Wrap(gvwErrors.CodeHttpError, err)
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPatch, endpoint, bytes.NewBuffer(jsonBody))
	if err != nil {
		return gvwErrors.Wrap(gvwErrors.CodeHttpError, err)
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Internal-Auth", c.token)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return gvwErrors.Wrap(gvwErrors.CodeHttpError, err)
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		b, _ := io.ReadAll(io.LimitReader(resp.Body, 4096))
		return gvwErrors.Wrap(gvwErrors.CodeHttpError,
			&gvwErrors.StatusError{StatusCode: resp.StatusCode, Body: string(b)})
	}

	return nil
}
