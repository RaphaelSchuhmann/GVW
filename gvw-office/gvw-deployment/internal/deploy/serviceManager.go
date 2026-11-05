package deploy

import (
	"context"
	"gvw/gvw-deployment/internal/config"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/log"
	"time"
)

type ServiceManager struct {
	log *log.Logger
	cfg *config.ConfigManager
}

func NewServiceManager(log *log.Logger, cfg *config.ConfigManager) *ServiceManager {
	return &ServiceManager{
		log: log,
		cfg: cfg,
	}
}

func (s *ServiceManager) StopService(serviceName string, timeoutDuration int) error {
	if serviceName == "" {
		return gvwErrors.NewError(gvwErrors.CodeServiceManagerBadServiceName)
	}

	var duration time.Duration
	if timeoutDuration <= 0 {
		duration = time.Duration(s.cfg.Config.ShutdownTimeout)
	} else {
		duration = time.Duration(timeoutDuration)
	}

	ctx, cancel := context.WithTimeout(context.Background(), duration*time.Second)
	defer cancel()

	s.log.Printf("[SERVICE] Stopping service %s", serviceName)



	return nil
}

func (s *ServiceManager) StartService(serviceName string, timeoutDuration int) error {
	if serviceName == "" {
		return gvwErrors.NewError(gvwErrors.CodeServiceManagerBadServiceName)
	}

	var duration time.Duration
	if timeoutDuration <= 0 {
		duration = time.Duration(s.cfg.Config.StartupTimeout)
	} else {
		duration = time.Duration(timeoutDuration)
	}

	ctx, cancel := context.WithTimeout(context.Background(), duration*time.Second)
	defer cancel()

	s.log.Printf("[SERVICE] Starting service %s", serviceName)


}
