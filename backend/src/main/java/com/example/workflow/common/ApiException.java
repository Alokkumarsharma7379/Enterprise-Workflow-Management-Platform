package com.example.workflow.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

  private final HttpStatus status;

  public ApiException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatus status() {
    return status;
  }

  public static ApiException missing(String resource) {
    return new ApiException(HttpStatus.NOT_FOUND, resource + " not found");
  }

  public static ApiException forbidden() {
    return new ApiException(
      HttpStatus.FORBIDDEN,
      "You do not have permission for this action"
    );
  }

  public static ApiException invalid(String message) {
    return new ApiException(HttpStatus.BAD_REQUEST, message);
  }

  public static ApiException conflict(String message) {
    return new ApiException(HttpStatus.CONFLICT, message);
  }

  public static ApiException unauthenticated() {
    return new ApiException(
      HttpStatus.UNAUTHORIZED,
      "Invalid or expired credentials"
    );
  }
}
