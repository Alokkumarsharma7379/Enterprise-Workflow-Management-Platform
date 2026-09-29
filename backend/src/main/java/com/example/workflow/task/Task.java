package com.example.workflow.task;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task {

  @Id
  private UUID id = UUID.randomUUID();

  private UUID projectId;
  private long taskNumber;
  private String title;
  private String description;

  @Enumerated(EnumType.STRING)
  private TaskStatus status;

  @Enumerated(EnumType.STRING)
  private TaskPriority priority;

  private UUID reporterId;
  private UUID assigneeId;
  private LocalDate dueDate;
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

  public UUID getProjectId() {
    return projectId;
  }

  public void setProjectId(UUID projectId) {
    this.projectId = projectId;
  }

  public long getTaskNumber() {
    return taskNumber;
  }

  public void setTaskNumber(long taskNumber) {
    this.taskNumber = taskNumber;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  public TaskPriority getPriority() {
    return priority;
  }

  public void setPriority(TaskPriority priority) {
    this.priority = priority;
  }

  public UUID getReporterId() {
    return reporterId;
  }

  public void setReporterId(UUID reporterId) {
    this.reporterId = reporterId;
  }

  public UUID getAssigneeId() {
    return assigneeId;
  }

  public void setAssigneeId(UUID assigneeId) {
    this.assigneeId = assigneeId;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
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
