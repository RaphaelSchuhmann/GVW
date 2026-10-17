package service

import (
	"gopkg.in/natefinch/lumberjack.v2"
	"io"
	"log"
	"os"
)

type Logger struct {
	lumberjackLogger *lumberjack.Logger
	logObj           *log.Logger
}

func NewLogger() *Logger {
	rotatingFile := &lumberjack.Logger{
		Filename:   "logs/gvw-deployment.log",
		MaxSize:    10, // megabytes
		MaxBackups: 3,
		MaxAge:     28,
		Compress:   true,
	}

	multiWriter := io.MultiWriter(os.Stdout, rotatingFile)

	return &Logger{
		lumberjackLogger: rotatingFile,
		logObj:           log.New(multiWriter, "[GVW-DEPLOYMENT] ", log.Ldate|log.Ltime|log.Lshortfile),
	}
}

func (l *Logger) Println(v ...any) {
	l.logObj.Println(v...)
}

func (l *Logger) Printf(format string, v ...any) {
	l.logObj.Printf(format, v...)
}

func (l *Logger) Fatal(v ...any) {
	l.logObj.Fatal(v...)
}

func (l *Logger) Fatalf(format string, v ...any) {
	l.logObj.Fatalf(format, v...)
}
