package deploy

import (
	"fmt"
	"gvw/gvw-deployment/internal/config"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/log"
	"net/smtp"
)

type Notifyer struct {
	log *log.Logger
	cfg *config.ConfigManager
}

func NewNotifyer(log *log.Logger, cfg *config.ConfigManager) *Notifyer {
	return &Notifyer{
		log: log,
		cfg: cfg,
	}
}

func (n *Notifyer) SendNotification(message string) error {
	n.log.Println("[NOTIFY] Trying to send message")
	auth := smtp.PlainAuth("", n.cfg.Config.SenderEmail, n.cfg.Config.SenderPassword, n.cfg.Config.SmtpHost)

	subject := "Deployment Notification\r\n"
	mime := "MIME-version: 1.0;\nContent-Type: text/html; charset=\"UTF-8\";\r\n"
	body := fmt.Sprintf("<html><body><h3>%s</h3></body></html>", message)

	msg := []byte(subject + mime + "\r\n" + body)

	err := smtp.SendMail(n.cfg.Config.SmtpHost+":"+n.cfg.Config.SmtpPort, auth, n.cfg.Config.SenderEmail, []string{n.cfg.Config.NotificationRecipient}, msg)
	if err != nil {
		n.log.Printf("[NOTIFY] Failed to send email: %v", err)
		return gvwErrors.Wrap(gvwErrors.CodeNotifyErrorSendingEmail, err)
	}

	n.log.Println("[NOTIFY] Sent message successfully!")
	return nil
}
