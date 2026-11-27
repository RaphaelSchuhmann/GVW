package client

import (
	"bytes"
	"context"
	"encoding/json"
	"gvw/gvw-deployment/internal/config"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/log"
	"io"
	"net/http"
	"net/url"
	"time"
)

var _ JavaClient = (*HttpJavaClient)(nil)

type Status string

const (
	StatusSuccess   Status = "SUCCESSFUL"
	StatusFailed    Status = "FAILED"
	StatusCancelled Status = "CANCELLED"
)

type HealthStatus string

const (
	Healthy   HealthStatus = "HEALTHY"
	Unhealthy HealthStatus = "UNHEALTHY"
)

type StatusPayload struct {
	Status Status              `json:"status"`
	Reason gvwErrors.ErrorCode `json:"reason"`
}

type HealthCheckResponse struct {
	HttpStatusCode int          `json:"-"`
	Application    HealthStatus `json:"app"`
	Database       HealthStatus `json:"database"`
}

type JavaClient interface {
	UpdateDeploymentState(deploymentId string, status Status, reason gvwErrors.ErrorCode, ctx context.Context) error
	RunHealthCheck(ctx context.Context) (HealthCheckResponse, error)
}

var InternalTokenHeader string = "X-Internal-Auth"

type HttpJavaClient struct {
	cfg        *config.ConfigManager
	httpClient *http.Client
	log        *log.Logger
}

func NewHttpJavaClient(cfg *config.ConfigManager, log *log.Logger) *HttpJavaClient {
	return &HttpJavaClient{
		cfg: cfg,
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
		log: log,
	}
}

func (c *HttpJavaClient) UpdateDeploymentState(deploymentId string, status Status, reason gvwErrors.ErrorCode, ctx context.Context) error {
	endpoint := c.cfg.Config.JavaBaseURL + "/internal/deployment/" + url.PathEscape(deploymentId) + "/status"

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
	req.Header.Set(InternalTokenHeader, c.cfg.Config.InternalToken)

	c.log.Printf("[CLIENT] Calling update deployment state for deploymentId: %s", deploymentId)

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

func (c *HttpJavaClient) RunHealthCheck(ctx context.Context) (HealthCheckResponse, error) {
	endpoint := c.cfg.Config.JavaBaseURL + "/internal/health"

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, endpoint, nil)
	if err != nil {
		return HealthCheckResponse{}, gvwErrors.Wrap(gvwErrors.CodeHttpError, err)
	}

	req.Header.Set(InternalTokenHeader, c.cfg.Config.InternalToken)

	c.log.Println("[CLIENT] Calling health check endpoint")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return HealthCheckResponse{}, gvwErrors.Wrap(gvwErrors.CodeHttpError, err)
	}
	defer resp.Body.Close()

	var payload HealthCheckResponse

	payload.HttpStatusCode = resp.StatusCode

	if err := json.NewDecoder(resp.Body).Decode(&payload); err != nil {
		return payload, err
	}

	return payload, nil
}
