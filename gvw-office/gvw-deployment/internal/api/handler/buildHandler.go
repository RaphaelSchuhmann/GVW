package handler

import (
	"gvw/gvw-deployment/internal/build"
	"net/http"
	"regexp"
)

var (
	deploymentIdPattern = regexp.MustCompile(`^[A-Za-z0-9_-]{1,64}$`)
	commitHashPattern   = regexp.MustCompile(`^[0-9a-fA-F]{7,64}$`)
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

	if !deploymentIdPattern.MatchString(deploymentId) || !commitHashPattern.MatchString(commitHash) {
 		resp.WriteHeader(http.StatusBadRequest)
 		return
 	}

	b.buildManager.TriggerBuild(deploymentId, commitHash)

	resp.WriteHeader(http.StatusAccepted)
}
