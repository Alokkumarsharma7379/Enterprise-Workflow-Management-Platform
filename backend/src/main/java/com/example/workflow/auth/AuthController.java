package com.example.workflow.auth;

import com.example.workflow.common.ApiException;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.Map;
import static com.example.workflow.auth.AuthDtos.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final RefreshTokenService tokens;
    private final Actor actor;
    private final boolean secure;
    private final long days;
    public AuthController(AuthService auth, RefreshTokenService tokens, Actor actor,
        @Value("${app.cookie-secure}") boolean secure, @Value("${app.refresh-days}") long days) {
        this.auth=auth; this.tokens=tokens; this.actor=actor; this.secure=secure; this.days=days;
    }
    @GetMapping("/csrf") Map<String,String> csrf(CsrfToken token) { return Map.of("token", token.getToken()); }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    AuthResponse register(@Valid @RequestBody Register request, HttpServletResponse response) { return respond(auth.register(request), response); }
    @PostMapping("/login") AuthResponse login(@Valid @RequestBody Login request, HttpServletResponse response) { return respond(auth.login(request), response); }
    @PostMapping("/refresh") AuthResponse refresh(@CookieValue(name="refresh_token", required=false) String raw, HttpServletResponse response) {
        var session = tokens.rotate(raw);
        if (session.isEmpty()) { cookie(response, "", Duration.ZERO); throw ApiException.unauthenticated(); }
        return respond(session.get(), response);
    }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@CookieValue(name="refresh_token", required=false) String raw, HttpServletResponse response) {
        tokens.logout(raw); cookie(response, "", Duration.ZERO);
    }
    @GetMapping("/me") Me me() { return auth.me(actor.id()); }
    private AuthResponse respond(Session session, HttpServletResponse response) {
        cookie(response, session.refreshToken(), Duration.ofDays(days));
        response.setHeader("Cache-Control", "no-store");
        return session.response();
    }
    private void cookie(HttpServletResponse response, String value, Duration age) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("refresh_token", value)
            .httpOnly(true).secure(secure).sameSite("Strict").path("/api/auth").maxAge(age).build().toString());
    }
}
