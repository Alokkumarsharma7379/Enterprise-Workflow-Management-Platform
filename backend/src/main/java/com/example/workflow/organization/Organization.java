package com.example.workflow.organization;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class Organization {
    @Id
    private UUID id = UUID.randomUUID();
    private String name;
    private UUID createdBy;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    @Version
    private long version;

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    public long getVersion() { return version; }
}
