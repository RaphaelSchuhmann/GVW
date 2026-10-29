package client

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"gvw/gvw-deployment/internal/utils/errors"
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
	endpoint := fmt.Sprintf("%s/internal/deployment/%s/status", c.baseUrl, url.PathEscape(deploymentId))

	payload := StatusPayload{
		Status: status,
		Reason: reason,
	}

	jsonBody, err := json.Marshal(payload)
	if err != nil {
		return gvwErrors.Wrap("HttpError", err)
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPatch, endpoint, bytes.NewBuffer(jsonBody))
	if err != nil {
		return gvwErrors.Wrap("HttpError", err)
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Internal-Auth", c.token)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return gvwErrors.Wrap("HttpError", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return gvwErrors.NewError(fmt.Sprintf("HttpError_%d", resp.StatusCode))
	}

	return nil
}

func (c *HttpJavaClient) UpdateMigrationDryRunState(deploymentId string, status Status, database string, documentId string, reason uint32, ctx context.Context) error {
	endpoint := fmt.Sprintf("%s/internal/migration/dry-run/%s/status", c.baseUrl, url.PathEscape(deploymentId))

	payload := MigrationDryRunPayload{
		DeploymentId: deploymentId,
		Status:       status,
		Database:     database,
		DocumentId:   documentId,
		Reason:       reason,
	}

	jsonBody, err := json.Marshal(payload)
	if err != nil {
		return gvwErrors.Wrap("HttpError", err)
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPatch, endpoint, bytes.NewBuffer(jsonBody))
	if err != nil {
		return gvwErrors.Wrap("HttpError", err)
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Internal-Auth", c.token)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return gvwErrors.Wrap("HttpError", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return gvwErrors.NewError(fmt.Sprintf("HttpError_%d", resp.StatusCode))
	}

	return nil
}
