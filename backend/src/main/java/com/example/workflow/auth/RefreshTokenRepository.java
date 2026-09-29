package com.example.workflow.auth;

import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface RefreshTokenRepository
  extends JpaRepository<RefreshToken, UUID>
{
  Optional<RefreshToken> findByTokenHash(String hash);

  @Modifying
  @Query(
    "update RefreshToken r set r.revokedAt = :now where r.familyId = :family"
  )
  void revokeFamily(UUID family, Instant now);
}
