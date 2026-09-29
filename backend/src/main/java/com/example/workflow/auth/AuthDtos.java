package com.example.workflow.auth;

import jakarta.validation.constraints.*;
import java.util.UUID;

public final class AuthDtos {

  private AuthDtos() {}

  public record Register(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 12, max = 72) String password,
    @NotBlank @Size(max = 100) String displayName
  ) {}

  public record Login(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 72) String password
  ) {}

  public record Me(UUID id, String email, String displayName) {}

  public record AuthResponse(String accessToken, long expiresIn, Me user) {}

  public record Session(AuthResponse response, String refreshToken) {}
}
