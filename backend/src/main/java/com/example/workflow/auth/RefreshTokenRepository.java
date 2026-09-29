package com.example.workflow.auth;
import org.springframework.data.jpa.repository.*;
import java.util.*;
import java.time.Instant;
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String hash);
    @Modifying
    @Query("update RefreshToken r set r.revokedAt = :now where r.familyId = :family")
    void revokeFamily(UUID family, Instant now);
}
