package com.example.workflow.comment;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class Comment {

  @Id
  private UUID id = UUID.randomUUID();

  private UUID taskId;
  private UUID authorId;
  private String body;
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

  public UUID getTaskId() {
    return taskId;
  }

  public void setTaskId(UUID taskId) {
    this.taskId = taskId;
  }

  public UUID getAuthorId() {
    return authorId;
  }

  public void setAuthorId(UUID authorId) {
    this.authorId = authorId;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
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
