package com.example.workflow.auth;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import java.util.UUID;
import com.example.workflow.common.ApiException;
@Component
public class Actor {
    public UUID id() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) throw ApiException.unauthenticated();
        try { return UUID.fromString(jwt.getSubject()); }
        catch (IllegalArgumentException e) { throw ApiException.unauthenticated(); }
    }
}
