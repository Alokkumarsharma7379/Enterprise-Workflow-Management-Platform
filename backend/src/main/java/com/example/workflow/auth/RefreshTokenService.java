package com.example.workflow.auth;

import com.example.workflow.user.AppUser;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import static com.example.workflow.auth.AuthDtos.*;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository tokens;
    private final EntityManager em;
    private final JwtEncoder encoder;
    private final String issuer;
    private final long minutes;
    private final long days;
    private final SecureRandom random = new SecureRandom();
    public RefreshTokenService(RefreshTokenRepository tokens, EntityManager em, JwtEncoder encoder,
        @Value("${app.issuer}") String issuer, @Value("${app.access-minutes}") long minutes,
        @Value("${app.refresh-days}") long days) {
        this.tokens=tokens; this.em=em; this.encoder=encoder; this.issuer=issuer; this.minutes=minutes; this.days=days;
    }
    public Session create(AppUser user) { return issue(user, UUID.randomUUID(), Instant.now().plus(Duration.ofDays(days))); }
    @Transactional
    public Optional<Session> rotate(String raw) {
        var found = lookup(raw);
        if (found.isEmpty()) return Optional.empty();
        var token = found.get();
        // All family operations lock the user first, so rotation and replay revocation serialize.
        var user = em.find(AppUser.class, token.getUserId(), LockModeType.PESSIMISTIC_WRITE);
        em.refresh(token);
        if (token.getUsedAt() != null || token.getRevokedAt() != null) {
            tokens.revokeFamily(token.getFamilyId(), Instant.now());
            return Optional.empty(); // Commit revocation before the controller returns 401.
        }
        if (!token.getExpiresAt().isAfter(Instant.now())) return Optional.empty();
        token.setUsedAt(Instant.now());
        return Optional.of(issue(user, token.getFamilyId(), token.getExpiresAt()));
    }
    @Transactional
    public void logout(String raw) {
        lookup(raw).ifPresent(token -> {
            em.find(AppUser.class, token.getUserId(), LockModeType.PESSIMISTIC_WRITE);
            tokens.revokeFamily(token.getFamilyId(), Instant.now());
        });
    }
    private Optional<RefreshToken> lookup(String raw) {
        if (raw == null || raw.length() != 43) return Optional.empty();
        return tokens.findByTokenHash(hash(raw));
    }
    private Session issue(AppUser user, UUID family, Instant expires) {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var token = new RefreshToken();
        token.setUserId(user.getId()); token.setFamilyId(family); token.setTokenHash(hash(raw)); token.setExpiresAt(expires);
        tokens.save(token);
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(user.getId().toString()).audience(List.of("workflow-api"))
            .issuedAt(now).expiresAt(now.plus(Duration.ofMinutes(minutes))).id(UUID.randomUUID().toString()).build();
        String access = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new Session(new AuthResponse(access, minutes * 60, new Me(user.getId(), user.getEmail(), user.getDisplayName())), raw);
    }
    private String hash(String raw) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
