package com.example.workflow.activity;

import com.example.workflow.common.Pages;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
public class ActivityController {

  private final ActivityService service;

  public ActivityController(ActivityService service) {
    this.service = service;
  }

  @GetMapping("/api/organizations/{id}/activity")
  Pages.Result<ActivityService.View> organization(
    @PathVariable UUID id,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return service.list("organization", id, page, size);
  }

  @GetMapping("/api/projects/{id}/activity")
  Pages.Result<ActivityService.View> project(
    @PathVariable UUID id,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return service.list("project", id, page, size);
  }

  @GetMapping("/api/tasks/{id}/activity")
  Pages.Result<ActivityService.View> task(
    @PathVariable UUID id,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return service.list("task", id, page, size);
  }
}
