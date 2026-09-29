package com.example.workflow.common;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiErrors {

  private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);

  public record ErrorBody(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, String> fields
  ) {}

  public static ErrorBody body(HttpStatus status, String message, String path) {
    return new ErrorBody(
      Instant.now(),
      status.value(),
      status.getReasonPhrase(),
      message,
      path,
      Map.of()
    );
  }

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ErrorBody> api(ApiException e, HttpServletRequest request) {
    return ResponseEntity.status(e.status()).body(
      body(e.status(), e.getMessage(), request.getRequestURI())
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorBody> validation(
    MethodArgumentNotValidException e,
    HttpServletRequest request
  ) {
    Map<String, String> fields = new LinkedHashMap<>();
    e.getBindingResult()
      .getFieldErrors()
      .forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.badRequest().body(
      new ErrorBody(
        Instant.now(),
        400,
        "Bad Request",
        "Check the highlighted fields",
        request.getRequestURI(),
        fields
      )
    );
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
  })
  ResponseEntity<ErrorBody> malformed(Exception e, HttpServletRequest request) {
    return ResponseEntity.badRequest().body(
      body(
        HttpStatus.BAD_REQUEST,
        "Malformed request or invalid field value",
        request.getRequestURI()
      )
    );
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ErrorBody> notFound(
    NoResourceFoundException e,
    HttpServletRequest request
  ) {
    return ResponseEntity.status(404).body(
      body(HttpStatus.NOT_FOUND, "Resource not found", request.getRequestURI())
    );
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ErrorBody> methodNotAllowed(
    HttpRequestMethodNotSupportedException e,
    HttpServletRequest request
  ) {
    return ResponseEntity.status(405).body(
      body(
        HttpStatus.METHOD_NOT_ALLOWED,
        "Method not allowed",
        request.getRequestURI()
      )
    );
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    ObjectOptimisticLockingFailureException.class,
  })
  ResponseEntity<ErrorBody> conflict(Exception e, HttpServletRequest request) {
    return ResponseEntity.status(409).body(
      body(
        HttpStatus.CONFLICT,
        "Conflicting data or a concurrent update; refresh and try again",
        request.getRequestURI()
      )
    );
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorBody> unexpected(
    Exception e,
    HttpServletRequest request
  ) {
    log.error("Unexpected failure on {}", request.getRequestURI(), e);
    return ResponseEntity.internalServerError().body(
      body(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred",
        request.getRequestURI()
      )
    );
  }
}
