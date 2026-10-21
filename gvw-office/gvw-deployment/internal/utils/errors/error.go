package errors

import (
	"fmt"
)

type AppError struct {
	Code string
	Err  error
}

func (e *AppError) Error() string {
	if e.Err != nil {
		return fmt.Sprintf("%s: %v", e.Code, e.Err)
	}

	return e.Code
}

func (e *AppError) Unwrap() error {
	return e.Err
}

func NewError(code string) *AppError {
	return &AppError{Code: code}
}

func Wrap(code string, err error) *AppError {
	return &AppError{Code: code, Err: err}
}
