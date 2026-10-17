package client

import (
	// "bytes"
	// "encoding/json"
	"net/http"
	"time"
	// "gvw/gvw-deployment/internal/model"
)

type JavaClient struct {
	baseUrl    string
	token      string
	httpClient *http.Client
}

func NewJavaClient(baseUrl, token string) *JavaClient {
	return &JavaClient{
		baseUrl: baseUrl,
		token:   token,
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}
