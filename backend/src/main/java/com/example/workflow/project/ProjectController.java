package com.example.workflow.project;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public class ProjectController {
    private final ProjectService service;
    public ProjectController(ProjectService service) { this.service=service; }
    @PostMapping("/api/organizations/{org}/projects") @ResponseStatus(HttpStatus.CREATED) ProjectService.View create(@PathVariable UUID org,@Valid @RequestBody ProjectService.Create request) { return service.create(org,request); }
    @GetMapping("/api/organizations/{org}/projects") List<ProjectService.View> list(@PathVariable UUID org) { return service.list(org); }
    @GetMapping("/api/projects/{id}") ProjectService.View get(@PathVariable UUID id) { return service.get(id); }
    @PatchMapping("/api/projects/{id}") ProjectService.View update(@PathVariable UUID id,@RequestBody JsonNode patch) { return service.update(id,patch); }
    @DeleteMapping("/api/projects/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable UUID id) { service.delete(id); }
}
