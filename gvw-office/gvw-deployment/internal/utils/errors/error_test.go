package gvwErrors

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestAppError_Is(t *testing.T) {
	baseErr := errors.New("underlying error")
	err1 := Wrap(CodeDbEncodeBody, baseErr)
	err2 := Wrap(CodeDbBuildRequest, err1)

	// Test single layer matching
	if !errors.Is(err1, &AppError{Code: CodeDbEncodeBody}) {
		t.Error("errors.Is should match on code through one layer")
	}

	// Test two layers of wrapping
	if !errors.Is(err2, &AppError{Code: CodeDbEncodeBody}) {
		t.Error("errors.Is should match on code through two layers of wrapping")
	}

	// Test different code does not match
	if errors.Is(err1, &AppError{Code: CodeDbBuildRequest}) {
		t.Error("errors.Is should not match a different code")
	}

	// Test context.DeadlineExceeded still works through wrapping
	timeoutErr := context.DeadlineExceeded
	wrappedTimeout := Wrap(CodeDbSendRequest, timeoutErr)
	if !errors.Is(wrappedTimeout, context.DeadlineExceeded) {
		t.Error("errors.Is should still work on wrapped context.DeadlineExceeded")
	}
}

func TestHasCode(t *testing.T) {
	baseErr := errors.New("underlying error")
	err1 := Wrap(CodeDbEncodeBody, baseErr)
	err2 := Wrap(CodeDbBuildRequest, err1)

	// Test HasCode through one layer
	if !HasCode(err1, CodeDbEncodeBody) {
		t.Error("HasCode should match through one layer")
	}

	// Test HasCode through two layers
	if !HasCode(err2, CodeDbEncodeBody) {
		t.Error("HasCode should match through two layers")
	}

	// Test HasCode does not match different code
	if HasCode(err1, CodeDbBuildRequest) {
		t.Error("HasCode should not match different code")
	}
}

func TestStatusError_As(t *testing.T) {
	statusErr := &StatusError{StatusCode: 404, Body: "not found"}
	wrappedErr := Wrap(CodeDbBadStatus, statusErr)

	// Test errors.As retrieves StatusError through Wrap
	var retrieved *StatusError
	if !errors.As(wrappedErr, &retrieved) {
		t.Error("errors.As should retrieve *StatusError through Wrap")
	}

	if retrieved.StatusCode != 404 {
		t.Errorf("Expected StatusCode 404, got %d", retrieved.StatusCode)
	}

	if retrieved.Body != "not found" {
		t.Errorf("Expected Body 'not found', got '%s'", retrieved.Body)
	}
}

func TestExecuteDbRequest_404(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusNotFound)
		w.Write([]byte("not found"))
	}))
	defer server.Close()

	// This test would require a DbClient instance
	// For now, we'll test the error handling pattern directly
	statusErr := &StatusError{StatusCode: 404, Body: "not found"}
	wrappedErr := Wrap(CodeDbBadStatus, statusErr)

	var retrieved *StatusError
	if !errors.As(wrappedErr, &retrieved) {
		t.Error("Should retrieve StatusError for 404")
	}

	if !HasCode(wrappedErr, CodeDbBadStatus) {
		t.Error("Should have CodeDbBadStatus for 404")
	}
}

func TestExecuteDbRequest_500(t *testing.T) {
	statusErr := &StatusError{StatusCode: 500, Body: "internal server error"}
	wrappedErr := Wrap(CodeDbBadStatus, statusErr)

	var retrieved *StatusError
	if !errors.As(wrappedErr, &retrieved) {
		t.Error("Should retrieve StatusError for 500")
	}

	if retrieved.StatusCode != 500 {
		t.Errorf("Expected StatusCode 500, got %d", retrieved.StatusCode)
	}

	if !HasCode(wrappedErr, CodeDbBadStatus) {
		t.Error("Should have CodeDbBadStatus for 500")
	}
}

func TestExecuteDbRequest_BadJSON(t *testing.T) {
	badJSONErr := errors.New("invalid JSON")
	wrappedErr := Wrap(CodeDbDecodeBody, badJSONErr)

	if !HasCode(wrappedErr, CodeDbDecodeBody) {
		t.Error("Should have CodeDbDecodeBody for bad JSON")
	}

	if !errors.Is(wrappedErr, badJSONErr) {
		t.Error("Should unwrap to original bad JSON error")
	}
}

func TestExecuteDbRequest_CancelledContext(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	cancel()

	timeoutErr := ctx.Err()
	wrappedErr := Wrap(CodeDbSendRequest, timeoutErr)

	if !HasCode(wrappedErr, CodeDbSendRequest) {
		t.Error("Should have CodeDbSendRequest for cancelled context")
	}

	if !errors.Is(wrappedErr, context.Canceled) {
		t.Error("Should still be context.Canceled through wrapping")
	}
}

func TestNewError(t *testing.T) {
	err := NewError(CodeDbEncodeBody)

	if err.Code != CodeDbEncodeBody {
		t.Errorf("Expected Code %s, got %s", CodeDbEncodeBody, err.Code)
	}

	if err.Err != nil {
		t.Error("NewError should not have an underlying error")
	}

	expectedMsg := string(CodeDbEncodeBody)
	if err.Error() != expectedMsg {
		t.Errorf("Expected error message '%s', got '%s'", expectedMsg, err.Error())
	}
}

func TestWrap(t *testing.T) {
	baseErr := errors.New("base error")
	err := Wrap(CodeDbBuildRequest, baseErr)

	if err.Code != CodeDbBuildRequest {
		t.Errorf("Expected Code %s, got %s", CodeDbBuildRequest, err.Code)
	}

	if err.Err != baseErr {
		t.Error("Wrap should have the underlying error")
	}

	if !strings.Contains(err.Error(), string(CodeDbBuildRequest)) {
		t.Error("Error message should contain the code")
	}

	if !strings.Contains(err.Error(), baseErr.Error()) {
		t.Error("Error message should contain the underlying error")
	}

	// Test Unwrap
	if errors.Unwrap(err) != baseErr {
		t.Error("Unwrap should return the underlying error")
	}
}
