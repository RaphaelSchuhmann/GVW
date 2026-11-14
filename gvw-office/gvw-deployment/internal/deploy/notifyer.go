package deploy

import (
	"fmt"
	"gvw/gvw-deployment/internal/config"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/log"
	"net"
	"net/smtp"
	"time"
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

	subject := "Subject: Deployment Notification\r\n"
	mime := "MIME-version: 1.0;\nContent-Type: text/html; charset=\"UTF-8\";\r\n"
	body := fmt.Sprintf("<html><body><h3>%s</h3></body></html>", message)

	msg := []byte(subject + mime + "\r\n" + body)
	addr := fmt.Sprintf("%s:%s", n.cfg.Config.SmtpHost, n.cfg.Config.SmtpPort)

	dialer := net.Dialer{Timeout: 10 * time.Second}
	conn, err := dialer.Dial("tcp", addr)
	if err != nil {
		n.log.Printf("[NOTIFY] Failed to connect to SMTP server: %v", err)
		return gvwErrors.Wrap(gvwErrors.CodeNotifyErrorSendingEmail, err)
	}
	defer conn.Close()

	if err := conn.SetDeadline(time.Now().Add(30 * time.Second)); err != nil {
		n.log.Printf("[NOTIFY] Failed to set SMTP connection deadline: %v", err)
		return gvwErrors.Wrap(gvwErrors.CodeNotifyErrorSendingEmail, err)
	}

	client, err := smtp.NewClient(conn, n.cfg.Config.SmtpHost)
	if err != nil {
		n.log.Printf("[NOTIFY] Failed to create SMTP client: %v", err)
		return gvwErrors.Wrap(gvwErrors.CodeNotifyErrorSendingEmail, err)
	}
	defer client.Quit()

	if auth != nil {
		if err = client.Auth(auth); err != nil {
			n.log.Printf("[NOTIFY] SMTP authentication failed: %v", err)
			return gvwErrors.Wrap(gvwErrors.CodeNotifyErrorSendingEmail, err)
		}
	}

	if err = client.Mail(n.cfg.Config.SenderEmail); err != nil {
		n.log.Printf("[NOTIFY] Failed to set sender: %v", err)
		return gvwErrors.Wrap(gvwErrors.CodeNotifyErrorSendingEmail, err)
	}

	if err = client.Rcpt(n.cfg.Config.NotificationRecipient); err != nil {
		n.log.Printf("[NOTIFY] Failed to set recipient: %v", err)
		return gvwErrors.Wrap(gvwErrors.CodeNotifyErrorSendingEmail, err)
	}

	w, err := client.Data()
	if err != nil {
		n.log.Printf("[NOTIFY] Failed to open data writer: %v", err)
		return gvwErrors.Wrap(gvwErrors.CodeNotifyErrorSendingEmail, err)
	}

	if _, err = w.Write(msg); err != nil {
		n.log.Printf("[NOTIFY] Failed to write message content: %v", err)
		return gvwErrors.Wrap(gvwErrors.CodeNotifyErrorSendingEmail, err)
	}

	if err = w.Close(); err != nil {
		n.log.Printf("[NOTIFY] Failed to close data writer: %v", err)
		return gvwErrors.Wrap(gvwErrors.CodeNotifyErrorSendingEmail, err)
	}

	n.log.Println("[NOTIFY] Sent message successfully!")
	return nil
}
