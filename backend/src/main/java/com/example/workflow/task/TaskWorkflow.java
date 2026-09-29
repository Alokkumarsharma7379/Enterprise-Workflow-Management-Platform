package com.example.workflow.task;

import static com.example.workflow.task.TaskStatus.*;

import com.example.workflow.common.ApiException;
import java.util.*;

public final class TaskWorkflow {

  private TaskWorkflow() {}

  private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS = Map.of(
    TODO,
    Set.of(IN_PROGRESS, CANCELLED),
    IN_PROGRESS,
    Set.of(TODO, IN_REVIEW, CANCELLED),
    IN_REVIEW,
    Set.of(IN_PROGRESS, DONE, CANCELLED),
    DONE,
    Set.of(),
    CANCELLED,
    Set.of()
  );

  public static void validate(TaskStatus from, TaskStatus to) {
    if (
      from != to && !TRANSITIONS.get(from).contains(to)
    ) throw ApiException.conflict(
      "Cannot transition from " + from + " to " + to
    );
  }
}
