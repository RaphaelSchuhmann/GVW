package gvwErrors

import (
	"errors"
	"fmt"
)

type AppError struct {
	Code Code
	Err  error
}

func (e *AppError) Error() string {
	if e.Err != nil {
		return fmt.Sprintf("%s: %v", string(e.Code), e.Err)
	}

	return string(e.Code)
}

func (e *AppError) Unwrap() error {
	return e.Err
}

func NewError(code Code) *AppError {
	return &AppError{Code: code}
}

func Wrap(code Code, err error) *AppError {
	return &AppError{Code: code, Err: err}
}

func (e *AppError) Is(target error) bool {
	t, ok := target.(*AppError)
	return ok && t.Code == e.Code
}

func HasCode(err error, code Code) bool {
	return errors.Is(err, &AppError{Code: code})
}

type StatusError struct {
	StatusCode int
	Body       string
}

func (e *StatusError) Error() string {
	return fmt.Sprintf("unexpected status %d", e.StatusCode)
}
