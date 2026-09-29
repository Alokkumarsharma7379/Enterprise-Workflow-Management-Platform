package com.example.workflow.label;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class LabelController {

  private final LabelService service;

  public LabelController(LabelService service) {
    this.service = service;
  }

  @GetMapping("/api/projects/{id}/labels")
  List<LabelService.View> list(@PathVariable UUID id) {
    return service.list(id);
  }

  @PostMapping("/api/projects/{id}/labels")
  @ResponseStatus(HttpStatus.CREATED)
  LabelService.View create(
    @PathVariable UUID id,
    @Valid @RequestBody LabelService.Create request
  ) {
    return service.create(id, request);
  }

  @PatchMapping("/api/labels/{id}")
  LabelService.View update(@PathVariable UUID id, @RequestBody JsonNode patch) {
    return service.update(id, patch);
  }

  @DeleteMapping("/api/labels/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable UUID id) {
    service.delete(id);
  }

  @PutMapping("/api/tasks/{taskId}/labels/{labelId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void attach(@PathVariable UUID taskId, @PathVariable UUID labelId) {
    service.attach(taskId, labelId, true);
  }

  @DeleteMapping("/api/tasks/{taskId}/labels/{labelId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void detach(@PathVariable UUID taskId, @PathVariable UUID labelId) {
    service.attach(taskId, labelId, false);
  }
}
