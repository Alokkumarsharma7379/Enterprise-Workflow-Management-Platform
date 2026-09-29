package com.example.workflow.task;

import com.example.workflow.user.UserView;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public final class TaskDtos {

  private TaskDtos() {}

  public record Create(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 10000) String description,
    @NotNull TaskPriority priority,
    UUID assigneeId,
    LocalDate dueDate
  ) {}

  public record LabelView(UUID id, String name, String color) {}

  public record View(
    UUID id,
    UUID organizationId,
    UUID projectId,
    String identifier,
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    UserView reporter,
    UserView assignee,
    LocalDate dueDate,
    List<LabelView> labels,
    long version,
    Instant createdAt,
    Instant updatedAt
  ) {}

  public record Statistics(long totalTasks, Map<TaskStatus, Long> byStatus) {}
}
