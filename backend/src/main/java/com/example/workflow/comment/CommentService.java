package com.example.workflow.comment;

import com.example.workflow.activity.ActivityService;
import com.example.workflow.auth.Actor;
import com.example.workflow.common.*;
import com.example.workflow.organization.*;
import com.example.workflow.project.Project;
import com.example.workflow.task.*;
import com.example.workflow.user.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

  private final CommentRepository comments;
  private final TaskRepository tasks;
  private final OrganizationAccess access;
  private final Actor actor;
  private final UserRepository users;
  private final ActivityService activity;
  private final EntityManager em;

  public CommentService(
    CommentRepository comments,
    TaskRepository tasks,
    OrganizationAccess access,
    Actor actor,
    UserRepository users,
    ActivityService activity,
    EntityManager em
  ) {
    this.comments = comments;
    this.tasks = tasks;
    this.access = access;
    this.actor = actor;
    this.users = users;
    this.activity = activity;
    this.em = em;
  }

  public record Create(@NotBlank @Size(max = 10000) String body) {}

  public record View(
    UUID id,
    UUID taskId,
    UserView author,
    String body,
    long version,
    Instant createdAt,
    Instant updatedAt
  ) {}

  @Transactional(readOnly = true)
  public Pages.Result<View> list(UUID taskId, int page, int size) {
    var task = task(taskId);
    access.project(task.getProjectId(), false);
    var result = comments.findByTaskId(taskId, Pages.request(page, size));
    var authors = users
      .findAllById(
        result.stream().map(Comment::getAuthorId).collect(Collectors.toSet())
      )
      .stream()
      .collect(Collectors.toMap(AppUser::getId, Function.identity()));
    return Pages.Result.of(
      result.map(c -> view(c, authors.get(c.getAuthorId())))
    );
  }

  @Transactional
  public View create(UUID taskId, Create request) {
    var task = task(taskId);
    var project = access.project(task.getProjectId(), true);
    access.writable(project);
    var comment = new Comment();
    comment.setTaskId(taskId);
    comment.setAuthorId(actor.id());
    comment.setBody(request.body().trim());
    comments.saveAndFlush(comment);
    event(project, taskId, "COMMENT_ADDED", comment.getId());
    return view(comment, users.findById(actor.id()).orElseThrow());
  }

  @Transactional
  public View update(UUID id, JsonNode json) {
    var comment = find(id);
    var task = task(comment.getTaskId());
    var project = access.project(task.getProjectId(), true);
    em.refresh(comment);
    access.writable(project);
    if (
      !comment.getAuthorId().equals(actor.id())
    ) throw ApiException.forbidden();
    var patch = new Patch(json, "body", "version");
    patch.version(comment.getVersion());
    comment.setBody(patch.text("body", 10000, false));
    comments.flush();
    event(project, task.getId(), "COMMENT_EDITED", id);
    return view(comment, users.findById(actor.id()).orElseThrow());
  }

  @Transactional
  public void delete(UUID id) {
    var comment = find(id);
    var task = task(comment.getTaskId());
    var project = access.project(task.getProjectId(), true);
    access.writable(project);
    if (!comment.getAuthorId().equals(actor.id())) access.require(
      project.getOrganizationId(),
      Role.OWNER,
      Role.ADMIN
    );
    comments.delete(comment);
    event(project, task.getId(), "COMMENT_DELETED", id);
  }

  private Comment find(UUID id) {
    return comments
      .findById(id)
      .orElseThrow(() -> ApiException.missing("Comment"));
  }

  private Task task(UUID id) {
    return tasks.findById(id).orElseThrow(() -> ApiException.missing("Task"));
  }

  private View view(Comment c, AppUser user) {
    return new View(
      c.getId(),
      c.getTaskId(),
      UserView.of(user),
      c.getBody(),
      c.getVersion(),
      c.getCreatedAt(),
      c.getUpdatedAt()
    );
  }

  private void event(Project p, UUID task, String action, UUID comment) {
    activity.record(
      p.getOrganizationId(),
      p.getId(),
      "TASK",
      task,
      action,
      null,
      comment
    );
  }
}
