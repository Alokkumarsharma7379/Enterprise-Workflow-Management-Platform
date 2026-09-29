package com.example.workflow.project;

import com.example.workflow.activity.ActivityService;
import com.example.workflow.auth.Actor;
import com.example.workflow.common.Patch;
import com.example.workflow.organization.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

  private final ProjectRepository projects;
  private final OrganizationAccess access;
  private final Actor actor;
  private final ActivityService activity;

  public ProjectService(
    ProjectRepository projects,
    OrganizationAccess access,
    Actor actor,
    ActivityService activity
  ) {
    this.projects = projects;
    this.access = access;
    this.actor = actor;
    this.activity = activity;
  }

  public record Create(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9]{1,9}") String key,
    @Size(max = 10000) String description
  ) {}

  public record View(
    UUID id,
    UUID organizationId,
    String name,
    String key,
    String description,
    ProjectStatus status,
    Role role,
    long version
  ) {}

  @Transactional
  public View create(UUID org, Create request) {
    access.organization(org, true);
    access.require(org, Role.OWNER, Role.ADMIN);
    var p = new Project();
    p.setOrganizationId(org);
    p.setName(request.name().trim());
    p.setProjectKey(request.key().toUpperCase(Locale.ROOT));
    p.setDescription(request.description());
    p.setStatus(ProjectStatus.ACTIVE);
    p.setCreatedBy(actor.id());
    p.setNextTaskNumber(1);
    projects.saveAndFlush(p);
    activity.record(
      org,
      p.getId(),
      "PROJECT",
      p.getId(),
      "PROJECT_CREATED",
      null,
      p.getName()
    );
    return view(p);
  }

  @Transactional(readOnly = true)
  public List<View> list(UUID org) {
    access.organization(org, false);
    Role role = access.role(org);
    return projects
      .findByOrganizationIdOrderByCreatedAtDesc(org)
      .stream()
      .map(p -> view(p, role))
      .toList();
  }

  @Transactional(readOnly = true)
  public View get(UUID id) {
    return view(access.project(id, false));
  }

  @Transactional
  public View update(UUID id, JsonNode json) {
    var p = access.project(id, true);
    access.require(p.getOrganizationId(), Role.OWNER, Role.ADMIN);
    var patch = new Patch(json, "name", "description", "status", "version");
    patch.version(p.getVersion());
    if (patch.has("name")) p.setName(patch.text("name", 120, false));
    if (patch.has("description")) p.setDescription(
      patch.text("description", 10000, true)
    );
    if (patch.has("status")) p.setStatus(
      patch.enumeration("status", ProjectStatus.class)
    );
    activity.record(
      p.getOrganizationId(),
      id,
      "PROJECT",
      id,
      "PROJECT_UPDATED",
      null,
      p.getName()
    );
    projects.flush();
    return view(p);
  }

  @Transactional
  public void delete(UUID id) {
    var p = access.project(id, true);
    access.require(p.getOrganizationId(), Role.OWNER, Role.ADMIN);
    activity.record(
      p.getOrganizationId(),
      null,
      "PROJECT",
      id,
      "PROJECT_DELETED",
      p.getName(),
      null
    );
    projects.delete(p);
  }

  private View view(Project p) {
    return view(p, access.role(p.getOrganizationId()));
  }

  private View view(Project p, Role role) {
    return new View(
      p.getId(),
      p.getOrganizationId(),
      p.getName(),
      p.getProjectKey(),
      p.getDescription(),
      p.getStatus(),
      role,
      p.getVersion()
    );
  }
}
