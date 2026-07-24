package com.gvw.gvwbackend.exception;

public class DatabaseConnectionException extends AppException {
  public DatabaseConnectionException(String message) {
    super(message);
  }

  public DatabaseConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
