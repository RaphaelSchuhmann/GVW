package deploy

import (
	"context"
	"gvw/gvw-deployment/internal/config"
	"gvw/gvw-deployment/internal/utils/commandExecutor"
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

	s.log.Printf("[SERVICE] Stopping service %s", serviceName)

	var duration time.Duration

	if timeoutDuration <= 0 {
		duration = time.Duration(s.cfg.Config.ShutdownTimeout)
	} else {
		duration = time.Duration(timeoutDuration)
	}

	if err := s.executeServiceCommand(duration, serviceName, "stop"); err != nil {
		return err
	}

	s.log.Printf("[SERVICE] Stopped service %s successfully", serviceName)

	return nil
}

func (s *ServiceManager) StartService(serviceName string, timeoutDuration int) error {
	if serviceName == "" {
		return gvwErrors.NewError(gvwErrors.CodeServiceManagerBadServiceName)
	}

	s.log.Printf("[SERVICE] Starting service %s", serviceName)

	var duration time.Duration

	if timeoutDuration <= 0 {
		duration = time.Duration(s.cfg.Config.StartupTimeout)
	} else {
		duration = time.Duration(timeoutDuration)
	}

	if err := s.executeServiceCommand(duration, serviceName, "start"); err != nil {
		return err
	}

	if serviceName == backendService {
		// Give the backend time to fully start
		time.Sleep(time.Duration(s.cfg.Config.StartupDelay) * time.Second)
	}

	s.log.Printf("[SERVICE] Started service %s successfully", serviceName)

	return nil
}

func (s *ServiceManager) executeServiceCommand(duration time.Duration, serviceName, action string) error {
	ctx, cancel := context.WithTimeout(context.Background(), duration*time.Second)
	defer cancel()

	_, err := commandExecutor.RunCommand(ctx, "", "systemctl", action, serviceName)
	if err != nil {
		if gvwErrors.HasCode(err, gvwErrors.CodeCommandExecutionErrorCancelledOrTimedout) {
			return gvwErrors.NewError(gvwErrors.CodeServiceManagerJobTimedout)
		}

		return gvwErrors.Wrap(gvwErrors.CodeServiceManagerJobFailed, err)
	}

	return nil
}
