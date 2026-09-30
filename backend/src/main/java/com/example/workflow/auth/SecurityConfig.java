package com.example.workflow.auth;

import com.example.workflow.common.ApiErrors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {

  @Bean
  SecretKey jwtKey(@Value("${app.jwt-secret}") String secret) {
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32) throw new IllegalArgumentException(
      "JWT_SECRET must have at least 32 bytes"
    );
    return new SecretKeySpec(bytes, "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey key) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  @Bean
  JwtDecoder jwtDecoder(SecretKey key, @Value("${app.issuer}") String issuer) {
    var decoder = NimbusJwtDecoder.withSecretKey(key)
      .macAlgorithm(MacAlgorithm.HS256)
      .build();
    OAuth2TokenValidator<Jwt> requiredClaims = jwt -> {
      try {
        UUID.fromString(jwt.getSubject());
        if (
          jwt.getExpiresAt() != null &&
          jwt.getAudience().contains("workflow-api")
        ) {
          return OAuth2TokenValidatorResult.success();
        }
      } catch (IllegalArgumentException | NullPointerException invalidSubject) {
        // Reject signed tokens that do not contain an application user identity.
      }
      return OAuth2TokenValidatorResult.failure(
        new OAuth2Error("invalid_token")
      );
    };
    decoder.setJwtValidator(
      new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(issuer),
        requiredClaims
      )
    );
    return decoder;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  SecurityFilterChain security(
    HttpSecurity http,
    ObjectMapper mapper,
    @Value("${app.allowed-origin}") String origin,
    @Value("${app.cookie-secure}") boolean secure
  ) throws Exception {
    var cors = new CorsConfiguration();
    cors.setAllowedOrigins(List.of(origin));
    cors.setAllowedMethods(
      List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
    );
    cors.setAllowedHeaders(
      List.of("Content-Type", "Authorization", "X-XSRF-TOKEN")
    );
    cors.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cors);
    var csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    csrfRepository.setCookieCustomizer(cookie ->
      cookie.secure(secure).sameSite("Strict").path("/")
    );
    http
      .cors(c -> c.configurationSource(source))
      .sessionManagement(s ->
        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .csrf(c ->
        c
          .csrfTokenRepository(csrfRepository)
          .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
          .requireCsrfProtectionMatcher(
            request ->
              request.getRequestURI().startsWith("/api/auth/") &&
              !List.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())
          )
      )
      .authorizeHttpRequests(a ->
        a
          .requestMatchers(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/csrf",
            "/actuator/health",
            "/swagger-ui",
            "/swagger-ui/**",
            "/v3/api-docs/**"
          )
          .permitAll()
          .anyRequest()
          .authenticated()
      )
      .oauth2ResourceServer(o ->
        o
          .jwt(j -> {})
          .authenticationEntryPoint((request, response, error) -> {
            response.setStatus(401);
            response.setContentType("application/json");
            mapper.writeValue(
              response.getOutputStream(),
              ApiErrors.body(
                HttpStatus.UNAUTHORIZED,
                "Authentication required",
                request.getRequestURI()
              )
            );
          })
      )
      .exceptionHandling(e ->
        e
          .authenticationEntryPoint((request, response, error) -> {
            response.setStatus(401);
            response.setContentType("application/json");
            mapper.writeValue(
              response.getOutputStream(),
              ApiErrors.body(
                HttpStatus.UNAUTHORIZED,
                "Authentication required",
                request.getRequestURI()
              )
            );
          })
          .accessDeniedHandler((request, response, error) -> {
            response.setStatus(403);
            response.setContentType("application/json");
            mapper.writeValue(
              response.getOutputStream(),
              ApiErrors.body(
                HttpStatus.FORBIDDEN,
                "Access denied or missing CSRF token",
                request.getRequestURI()
              )
            );
          })
      );
    return http.build();
  }

  @Bean
  OpenAPI openAPI() {
    return new OpenAPI()
      .components(
        new Components().addSecuritySchemes(
          "bearerAuth",
          new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
        )
      )
      .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }

  @Bean
  OpenApiCustomizer authenticationDocumentation() {
    return api ->
      api.getPaths().forEach((path, item) -> {
        if (path.startsWith("/api/auth/") && !path.endsWith("/me")) {
          item
            .readOperations()
            .forEach(operation -> operation.setSecurity(List.of()));
          if (item.getPost() != null) {
            item
              .getPost()
              .addParametersItem(
                new Parameter()
                  .in("header")
                  .name("X-XSRF-TOKEN")
                  .required(true)
                  .schema(new StringSchema())
                  .description(
                    "First call GET /api/auth/csrf in this browser, then copy its token here. The cookie is sent automatically."
                  )
              );
          }
        }
      });
  }
}
