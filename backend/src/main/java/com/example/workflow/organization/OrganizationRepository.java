package com.example.workflow.organization;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Organization o where o.id = :id")
    Optional<Organization> lock(UUID id);
    @Query("select o from Organization o, Membership m where m.organizationId = o.id and m.userId = :user and m.status = com.example.workflow.organization.MembershipStatus.ACTIVE order by o.name, o.id")
    List<Organization> accessible(UUID user);
}
