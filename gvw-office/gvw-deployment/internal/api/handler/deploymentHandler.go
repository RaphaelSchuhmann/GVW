package handler

import (
	"encoding/json"
	"gvw/gvw-deployment/internal/deploy"
	"net/http"
)

type DeploymentHandler struct {
	deploymentManager *deploy.DeploymentManager
}

func NewDeploymentHandler(deploymentManager *deploy.DeploymentManager) *DeploymentHandler {
	return &DeploymentHandler{
		deploymentManager: deploymentManager,
	}
}

func (d *DeploymentHandler) RegisterRoutes(mux *http.ServeMux) {
	mux.HandleFunc("POST /deploy/{deploymentId}/{commit}", d.StartDeployment)
}

func (d *DeploymentHandler) StartDeployment(resp http.ResponseWriter, req *http.Request) {
	deploymentId := req.PathValue("deploymentId")
	commit := req.PathValue("commit")

	if !deploymentIdPattern.MatchString(deploymentId) || !commitHashPattern.MatchString(commit) {
		resp.WriteHeader(http.StatusBadRequest)
		return
	}

	var body migrationRequestBody
	req.Body = http.MaxBytesReader(resp, req.Body, 2<<20) // max body size 2 MiB (2<<20 == 2 MiB)
	decoder := json.NewDecoder(req.Body)
	if err := decoder.Decode(&body); err != nil {
		http.Error(resp, "INVALID_BODY", http.StatusBadRequest)
		return
	}

	d.deploymentManager.StartDeploymentPipeline(deploymentId, commit, body.Migrations)

	resp.WriteHeader(http.StatusAccepted)
}
