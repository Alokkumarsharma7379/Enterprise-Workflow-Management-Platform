package com.example.workflow.organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    Optional<Membership> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);
    List<Membership> findByOrganizationIdAndStatusOrderByCreatedAt(UUID organizationId, MembershipStatus status);
    List<Membership> findByUserIdAndStatus(UUID userId, MembershipStatus status);
}
