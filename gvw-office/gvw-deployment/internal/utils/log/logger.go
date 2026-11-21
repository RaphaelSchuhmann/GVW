package log

import (
	"fmt"
	"io"
	"log"
	"os"
	"path/filepath"
	"sync"

	"gopkg.in/natefinch/lumberjack.v2"
)

type Logger struct {
	mu               sync.Mutex
	lumberjackLogger *lumberjack.Logger
	logObj           *log.Logger
}

func NewLogger() *Logger {
	return &Logger{
		// console only until a log file is configured
		logObj: log.New(os.Stdout, "[GVW-DEPLOYMENT] ", log.Ldate|log.Ltime|log.Lshortfile),
	}
}

func (l *Logger) SetLogFile(dir string) error {
	// lumberjack only fails at write time (and the errors are swallowed by Output),
	// so check up front that the path is usable
	if err := os.MkdirAll(dir, 0o755); err != nil {
		l.Printf("[FATAL] Failed to create log directory with path %s; error: %v", dir, err)
		return err
	}

	filePath := filepath.Join(dir, "gvw-deployment.log")

	f, err := os.OpenFile(filePath, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0o644)
	if err != nil {
		l.Printf("[FATAL] Failed to open log file with path %s; error: %v", filePath, err)
		return err
	}
	_ = f.Close()

	rotatingFile := &lumberjack.Logger{
		Filename:   filePath,
		MaxSize:    10, // megabytes
		MaxBackups: 3,
		MaxAge:     28,
		Compress:   true,
	}

	l.mu.Lock()
	defer l.mu.Unlock()

	l.logObj.SetOutput(io.MultiWriter(os.Stdout, rotatingFile))

	if l.lumberjackLogger != nil {
		_ = l.lumberjackLogger.Close() // only matters if SetLogFile is called twice
	}
	l.lumberjackLogger = rotatingFile
	return nil
}

func (l *Logger) Println(v ...any) {
	_ = l.logObj.Output(2, fmt.Sprintln(v...))
}

func (l *Logger) Printf(format string, v ...any) {
	_ = l.logObj.Output(2, fmt.Sprintf(format, v...))
}

func (l *Logger) Fatal(v ...any) {
	l.logObj.Fatal(v...)
}

func (l *Logger) Fatalf(format string, v ...any) {
	l.logObj.Fatalf(format, v...)
}
