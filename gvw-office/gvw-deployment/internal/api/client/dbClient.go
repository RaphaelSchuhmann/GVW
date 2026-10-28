package client

import (
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
		baseUrl: baseUrl,
		username: username,
		password: password,
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

