package deploy

import (
	"gvw/gvw-deployment/internal/config"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/filesystem"
	"gvw/gvw-deployment/internal/utils/log"
	"path/filepath"
	"strings"
)

type NginxMaintenanceController struct {
	log       *log.Logger
	cfg       *config.ConfigManager
	fsWrapper filesystem.FileSystemWrapper
}

func NewNginxMaintenanceController(log *log.Logger, cfg *config.ConfigManager, fsWrapper filesystem.FileSystemWrapper) *NginxMaintenanceController {
	return &NginxMaintenanceController{
		log:       log,
		cfg:       cfg,
		fsWrapper: fsWrapper,
	}
}

func (n *NginxMaintenanceController) EnableFlag() error {
	n.log.Println("[NGINX] EnableFlag was called")

	alreadySet, err := n.GetFlagState()
	if err != nil {
		return err
	}

	if alreadySet {
		n.log.Println("[NGINX] Flag was already enabled")
		return nil
	}

	flagPath, err := n.getFlagPath()
	if err != nil {
		return err
	}

	if err := n.fsWrapper.WriteToFile(flagPath, []byte("1")); err != nil {
		n.log.Printf("[NGINX] Error while enabling flag: %v", err)
		return gvwErrors.Wrap(gvwErrors.CodeNginxFailedToSetFlag, err)
	}

	n.log.Println("[NGINX] Flag enabled")

	return nil
}

func (n *NginxMaintenanceController) DisableFlag() error {
	n.log.Println("[NGINX] DisableFlag was called")

	alreadySet, err := n.GetFlagState()
	if err != nil {
		return err
	}

	if !alreadySet {
		n.log.Println("[NGINX] Flag was already disabled")
		return nil
	}

	flagPath, err := n.getFlagPath()
	if err != nil {
		return err
	}

	if err := n.fsWrapper.RemoveFile(flagPath); err != nil {
		n.log.Printf("[NGINX] Error while disabling flag: %v", err)
		return gvwErrors.Wrap(gvwErrors.CodeNginxFailedToUnsetFlag, err)
	}

	n.log.Println("[NGINX] Flag disabled")

	return nil
}

func (n *NginxMaintenanceController) GetFlagState() (bool, error) {
	n.log.Println("[NGINX] Getting flag state")
	flagPath, err := n.getFlagPath()
	if err != nil {
		return false, err
	}

	return n.fsWrapper.DirOrFileExists(flagPath), nil
}

func (n *NginxMaintenanceController) getFlagPath() (string, error) {
	n.log.Println("[NGINX] Getting flag path")

	flagPath := n.cfg.Config.MaintenanceFlagDir
	if strings.TrimSpace(flagPath) == "" {
		n.log.Println("[NGINX] Invalid flag path: Flag path is empty!")
		return "", gvwErrors.NewError(gvwErrors.CodeNginxEmptyFlagPath)
	}

	if !filepath.IsAbs(flagPath) {
		n.log.Println("[NGINX] Invalid flag path: Flag path is not absolute!")
		return "", gvwErrors.NewError(gvwErrors.CodeNginxNonAbsoluteFlagPath)
	}

	return flagPath, nil
}
