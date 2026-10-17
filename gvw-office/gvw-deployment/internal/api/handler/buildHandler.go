package handler

import (
	"net/http"
)

type BuildHandler struct {}

func NewBuildHandler() *BuildHandler {
	return &BuildHandler{}
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

	// TODO: call start build process here

	resp.WriteHeader(http.StatusAccepted)
}

