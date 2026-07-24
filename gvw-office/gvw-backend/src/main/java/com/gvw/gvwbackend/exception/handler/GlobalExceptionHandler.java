package com.gvw.gvwbackend.exception.handler;

import com.gvw.gvwbackend.dto.response.ErrorResponseDTO;
import com.gvw.gvwbackend.exception.*;
import com.gvw.gvwbackend.service.EventService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(EventService.class);

  @ExceptionHandler(DatabaseConnectionException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<ErrorResponseDTO> handleDatabaseConnection(DatabaseConnectionException ex) {
    return generateResponse("0000500", null, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(DatabaseMappingException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<ErrorResponseDTO> handleDatabaseMapping(DatabaseMappingException ex) {
    return generateResponse("0000500", null, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(BadRequestException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponseDTO> handleBadRequest(BadRequestException ex) {
    return generateResponse(ex.getMessage(), null, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponseDTO> handleBadRequestFromValidation(
      MethodArgumentNotValidException ex) {
    ErrorDomain domain;
    ErrorAction action;
    ErrorResource resource;

    java.lang.reflect.Method executable =
        (java.lang.reflect.Method) ex.getParameter().getExecutable();

    ErrorContext context = executable.getAnnotation(ErrorContext.class);

    if (context == null) {
      context = executable.getDeclaringClass().getAnnotation(ErrorContext.class);
    }

    if (context != null) {
      domain = context.domain();
      action = context.action();
      resource = context.resource();

      long code = domain.createCode(action, 400, resource);
      return generateResponse(String.valueOf(code), null, HttpStatus.BAD_REQUEST);
    } else {
      log.warn("Missing @ErrorContext annotation");
      ;
      return generateResponse("0000400", null, HttpStatus.BAD_REQUEST);
    }
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ResponseEntity<ErrorResponseDTO> handleInvalidCredentials(InvalidCredentialsException ex) {
    return generateResponse(ex.getMessage(), null, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseEntity<ErrorResponseDTO> handleNotFound(NotFoundException ex) {
    return generateResponse(ex.getMessage(), null, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(HttpStatusCodeException.class)
  public ResponseEntity<ErrorResponseDTO> handleNotFoundFromDB(HttpStatusCodeException ex) {
    // This should be handled as a generic 500 error by the frontend!
    return generateResponse("0000500", null, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(ConflictException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ResponseEntity<ErrorResponseDTO> handleConflict(ConflictException ex) {
    return generateResponse(ex.getMessage(), null, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(TooManyRequestsException.class)
  @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
  public ResponseEntity<ErrorResponseDTO> handleTooManyRequests(TooManyRequestsException ex) {
    return generateResponse(
        ex.getMessage(),
        Map.of("retryAfter", ex.getRetryAfterSeconds()),
        HttpStatus.TOO_MANY_REQUESTS);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex) {
    return generateResponse("0000500", null, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ErrorResponseDTO> generateResponse(
      String code, Map<String, Object> details, HttpStatus status) {
    ErrorResponseDTO dto = new ErrorResponseDTO(code, details);

    HttpHeaders headers = new HttpHeaders();
    headers.add("X-GVW-Error-Code", code);

    headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-GVW-Error-Code");

    return new ResponseEntity<>(dto, headers, status);
  }
}
