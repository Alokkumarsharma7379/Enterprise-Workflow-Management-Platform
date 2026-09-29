package com.example.workflow.organization;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
  Optional<Membership> findByOrganizationIdAndUserId(
    UUID organizationId,
    UUID userId
  );
  List<Membership> findByOrganizationIdAndStatusOrderByCreatedAt(
    UUID organizationId,
    MembershipStatus status
  );
  List<Membership> findByUserIdAndStatus(UUID userId, MembershipStatus status);
}
