package com.example.workflow.task;

import com.example.workflow.common.Pages;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class TaskController {

  private final TaskService service;

  public TaskController(TaskService service) {
    this.service = service;
  }

  @PostMapping("/api/projects/{id}/tasks")
  @ResponseStatus(HttpStatus.CREATED)
  TaskDtos.View create(
    @PathVariable UUID id,
    @Valid @RequestBody TaskDtos.Create request
  ) {
    return service.create(id, request);
  }

  @GetMapping("/api/projects/{id}/tasks")
  Pages.Result<TaskDtos.View> list(
    @PathVariable UUID id,
    @RequestParam(required = false) String search,
    @RequestParam(required = false) TaskStatus status,
    @RequestParam(required = false) TaskPriority priority,
    @RequestParam(required = false) UUID assigneeId,
    @RequestParam(required = false) UUID labelId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(defaultValue = "createdAt,desc") String sort
  ) {
    return service.list(
      id,
      search,
      status,
      priority,
      assigneeId,
      labelId,
      page,
      size,
      sort
    );
  }

  @GetMapping("/api/tasks/{id}")
  TaskDtos.View get(@PathVariable UUID id) {
    return service.get(id);
  }

  @PatchMapping("/api/tasks/{id}")
  TaskDtos.View update(@PathVariable UUID id, @RequestBody JsonNode patch) {
    return service.update(id, patch);
  }

  @DeleteMapping("/api/tasks/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable UUID id) {
    service.delete(id);
  }

  @GetMapping("/api/projects/{id}/statistics")
  TaskDtos.Statistics statistics(@PathVariable UUID id) {
    return service.statistics(id);
  }
}
