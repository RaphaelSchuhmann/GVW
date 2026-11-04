package handler

import (
	"encoding/json"
	"gvw/gvw-deployment/internal/migration"
	"net/http"
)

type migrationRequestBody struct {
	Migrations []migration.Migration `json:"migrations"`
}

type MigrationHandler struct {
	migrationManager *migration.MigrationManager
}

func NewMigrationHandler(migrationManager *migration.MigrationManager) *MigrationHandler {
	return &MigrationHandler{
		migrationManager: migrationManager,
	}
}

func (m *MigrationHandler) RegisterRoutes(mux *http.ServeMux) {
	mux.HandleFunc("POST /migration/test/{deploymentId}", m.StartMigrationTest)
}

func (m *MigrationHandler) StartMigrationTest(resp http.ResponseWriter, req *http.Request) {
	deploymentId := req.PathValue("deploymentId")

	if !deploymentIdPattern.MatchString(deploymentId) {
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

	m.migrationManager.TriggerMigrationTest(deploymentId, body.Migrations)

	resp.WriteHeader(http.StatusAccepted)
}
