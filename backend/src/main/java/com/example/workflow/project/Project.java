package com.example.workflow.project;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project {

  @Id
  private UUID id = UUID.randomUUID();

  private UUID organizationId;
  private String name;
  private String projectKey;
  private String description;

  @Enumerated(EnumType.STRING)
  private ProjectStatus status;

  private UUID createdBy;
  private long nextTaskNumber;
  private Instant createdAt = Instant.now();
  private Instant updatedAt = Instant.now();

  @Version
  private long version;

  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(UUID organizationId) {
    this.organizationId = organizationId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public void setProjectKey(String projectKey) {
    this.projectKey = projectKey;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ProjectStatus getStatus() {
    return status;
  }

  public void setStatus(ProjectStatus status) {
    this.status = status;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(UUID createdBy) {
    this.createdBy = createdBy;
  }

  public long getNextTaskNumber() {
    return nextTaskNumber;
  }

  public void setNextTaskNumber(long nextTaskNumber) {
    this.nextTaskNumber = nextTaskNumber;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }
}
