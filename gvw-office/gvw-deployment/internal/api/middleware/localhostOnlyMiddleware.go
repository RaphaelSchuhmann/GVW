package middleware

import (
	"gvw/gvw-deployment/internal/utils/log"
	"net"
	"net/http"
)

func LocalhostOnlyMiddleware(logger *log.Logger, next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		host, _, err := net.SplitHostPort(r.RemoteAddr)
		if err != nil {
			logger.Printf("Faialed to parse RemoteAddr: %v", err)
			http.Error(w, "Forbidden", http.StatusForbidden)
			return
		}

		ip := net.ParseIP(host)
		if ip == nil || !ip.IsLoopback() {
			logger.Printf("Blocked non-local request from IP: %s", host)
			http.Error(w, "Forbidden", http.StatusForbidden)
			return
		}

		next.ServeHTTP(w, r)
	})
}
