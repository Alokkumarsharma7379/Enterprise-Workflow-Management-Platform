package com.example.workflow.organization;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "organization_members")
public class Membership {
    @Id
    private UUID id = UUID.randomUUID();
    private UUID organizationId;
    private UUID userId;
    @Enumerated(EnumType.STRING)
    private Role role;
    @Enumerated(EnumType.STRING)
    private MembershipStatus status;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public MembershipStatus getStatus() { return status; }
    public void setStatus(MembershipStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}
