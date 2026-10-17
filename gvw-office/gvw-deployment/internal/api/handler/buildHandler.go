package handler

import (
	"gvw/gvw-deployment/internal/build"
	"net/http"
)

type BuildHandler struct {
	buildManager *build.BuildManager
}

func NewBuildHandler(buildManager *build.BuildManager) *BuildHandler {
	return &BuildHandler{
		buildManager: buildManager,
	}
}

func (b *BuildHandler) RegisterRoutes(mux *http.ServeMux) {
	mux.HandleFunc("POST /build/{deploymentId}/{commitHash}", b.StartBuild)
}

func (b *BuildHandler) StartBuild(resp http.ResponseWriter, req *http.Request) {
	deploymentId := req.PathValue("deploymentId")
	commitHash := req.PathValue("commitHash")

	if deploymentId == "" || commitHash == "" {
		resp.WriteHeader(http.StatusBadRequest)
		return
	}

	b.buildManager.TriggerBuild(deploymentId, commitHash)

	resp.WriteHeader(http.StatusAccepted)
}
