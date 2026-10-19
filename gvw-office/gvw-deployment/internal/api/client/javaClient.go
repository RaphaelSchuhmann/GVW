package client

import (
	"bytes"
	"encoding/json"
	"net/http"
	"time"
	"fmt"
)

var _ JavaClient = (*HttpJavaClient)(nil)

type StatusPayload struct {
	Status string `json:"status"`
	Reason uint32 `json:"reason"`
}

type JavaClient interface {
	UpdateDeploymentState(deploymentId string, state string, reason uint32) error
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

func (c *HttpJavaClient) UpdateDeploymentState(deploymentId string, status string, reason uint32) error {
	url := fmt.Sprintf("%s/internal/deployment/%s/status", c.baseUrl, deploymentId)

	payload := StatusPayload{
		Status: status,
		Reason: reason,
	}

	jsonBody, err := json.Marshal(payload)
	if err != nil {
		return fmt.Errorf("failed to marshal status payload: %w", err)
	}

	req, err := http.NewRequest(http.MethodPatch, url, bytes.NewBuffer(jsonBody))
	if err != nil {
		return fmt.Errorf("failed to create http request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Internal-Auth", c.token)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("failed to execute request: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return fmt.Errorf("java api returned non-sucecss status: %d", resp.StatusCode)
	}

	return nil
}
