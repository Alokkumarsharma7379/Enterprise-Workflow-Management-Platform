package com.example.workflow.auth;

import com.example.workflow.common.ApiException;
import com.example.workflow.user.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static com.example.workflow.auth.AuthDtos.*;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final RefreshTokenService tokens;
    private final String dummyHash;
    public AuthService(UserRepository users, PasswordEncoder passwords, RefreshTokenService tokens) {
        this.users = users; this.passwords = passwords; this.tokens = tokens;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }
    @Transactional
    public Session register(Register request) {
        checkPassword(request.password());
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) throw ApiException.conflict("An account with this email already exists");
        var user = new AppUser();
        user.setEmail(email); user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(passwords.encode(request.password()));
        users.saveAndFlush(user);
        return tokens.create(user);
    }
    @Transactional
    public Session login(Login request) {
        checkPassword(request.password());
        var user = users.findByEmail(request.email().trim().toLowerCase(Locale.ROOT));
        boolean matches = passwords.matches(request.password(), user.map(AppUser::getPasswordHash).orElse(dummyHash));
        if (!matches || user.isEmpty()) throw ApiException.unauthenticated();
        return tokens.create(user.get());
    }
    @Transactional(readOnly=true)
    public Me me(UUID id) {
        var user = users.findById(id).orElseThrow(ApiException::unauthenticated);
        return new Me(user.getId(), user.getEmail(), user.getDisplayName());
    }
    private void checkPassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) throw ApiException.invalid("Password must not exceed 72 UTF-8 bytes");
    }
}
