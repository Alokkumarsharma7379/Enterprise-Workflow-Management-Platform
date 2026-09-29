package com.example.workflow.comment;

import com.example.workflow.common.Pages;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class CommentController {

  private final CommentService service;

  public CommentController(CommentService service) {
    this.service = service;
  }

  @GetMapping("/api/tasks/{id}/comments")
  Pages.Result<CommentService.View> list(
    @PathVariable UUID id,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return service.list(id, page, size);
  }

  @PostMapping("/api/tasks/{id}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  CommentService.View create(
    @PathVariable UUID id,
    @Valid @RequestBody CommentService.Create request
  ) {
    return service.create(id, request);
  }

  @PatchMapping("/api/comments/{id}")
  CommentService.View update(
    @PathVariable UUID id,
    @RequestBody JsonNode patch
  ) {
    return service.update(id, patch);
  }

  @DeleteMapping("/api/comments/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
