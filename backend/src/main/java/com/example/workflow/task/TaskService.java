package com.example.workflow.task;

import static com.example.workflow.task.TaskDtos.*;

import com.example.workflow.activity.ActivityService;
import com.example.workflow.auth.Actor;
import com.example.workflow.common.*;
import com.example.workflow.label.TaskLabelRepository;
import com.example.workflow.organization.*;
import com.example.workflow.project.*;
import com.example.workflow.user.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

  private final TaskRepository tasks;
  private final OrganizationAccess access;
  private final Actor actor;
  private final ActivityService activity;
  private final UserRepository users;
  private final TaskLabelRepository labels;
  private final EntityManager em;

  public TaskService(
    TaskRepository tasks,
    OrganizationAccess access,
    Actor actor,
    ActivityService activity,
    UserRepository users,
    TaskLabelRepository labels,
    EntityManager em
  ) {
    this.tasks = tasks;
    this.access = access;
    this.actor = actor;
    this.activity = activity;
    this.users = users;
    this.labels = labels;
    this.em = em;
  }

  @Transactional
  public View create(UUID projectId, Create request) {
    var project = access.project(projectId, true);
    manage(project);
    access.writable(project);
    access.activeAssignee(project.getOrganizationId(), request.assigneeId());
    var task = new Task();
    task.setProjectId(projectId);
    task.setTaskNumber(project.getNextTaskNumber());
    project.setNextTaskNumber(project.getNextTaskNumber() + 1);
    task.setTitle(request.title().trim());
    task.setDescription(request.description());
    task.setPriority(request.priority());
    task.setStatus(TaskStatus.TODO);
    task.setReporterId(actor.id());
    task.setAssigneeId(request.assigneeId());
    task.setDueDate(request.dueDate());
    tasks.saveAndFlush(task);
    event(project, task, "TASK_CREATED", null, task.getTitle());
    if (task.getAssigneeId() != null) event(
      project,
      task,
      "ASSIGNEE_CHANGED",
      null,
      task.getAssigneeId()
    );
    return views(List.of(task), project).getFirst();
  }

  @Transactional(readOnly = true)
  public View get(UUID id) {
    var task = find(id);
    var project = access.project(task.getProjectId(), false);
    return views(List.of(task), project).getFirst();
  }

  @Transactional(readOnly = true)
  public Pages.Result<View> list(
    UUID projectId,
    String search,
    TaskStatus status,
    TaskPriority priority,
    UUID assignee,
    UUID label,
    int page,
    int size,
    String sort
  ) {
    var project = access.project(projectId, false);
    Pages.request(page, size);
    var result = tasks.findAll(
      TaskSpecifications.filter(
        projectId,
        search,
        status,
        priority,
        assignee,
        label,
        sort
      ),
      PageRequest.of(page, size)
    );
    return new Pages.Result<>(
      views(result.getContent(), project),
      page,
      size,
      result.getTotalElements(),
      result.getTotalPages()
    );
  }

  @Transactional
  public View update(UUID id, JsonNode json) {
    var task = find(id);
    var project = access.project(task.getProjectId(), true);
    em.refresh(task);
    access.writable(project);
    var patch = new Patch(
      json,
      "title",
      "description",
      "status",
      "priority",
      "assigneeId",
      "dueDate",
      "version"
    );
    Set<String> fields = new HashSet<>();
    json.fieldNames().forEachRemaining(fields::add);
    TaskPolicy.edit(
      access.role(project.getOrganizationId()),
      actor.id(),
      task.getAssigneeId(),
      fields
    );
    patch.version(task.getVersion());
    if (patch.has("title")) {
      var value = patch.text("title", 200, false);
      if (!Objects.equals(value, task.getTitle())) event(
        project,
        task,
        "TITLE_CHANGED",
        task.getTitle(),
        value
      );
      task.setTitle(value);
    }
    if (patch.has("description")) {
      var value = patch.text("description", 10000, true);
      if (!Objects.equals(value, task.getDescription())) event(
        project,
        task,
        "DESCRIPTION_CHANGED",
        null,
        null
      );
      task.setDescription(value);
    }
    if (patch.has("status")) {
      var value = patch.enumeration("status", TaskStatus.class);
      TaskWorkflow.validate(task.getStatus(), value);
      if (value != task.getStatus()) event(
        project,
        task,
        "STATUS_CHANGED",
        task.getStatus(),
        value
      );
      task.setStatus(value);
    }
    if (patch.has("priority")) {
      var value = patch.enumeration("priority", TaskPriority.class);
      if (value != task.getPriority()) event(
        project,
        task,
        "PRIORITY_CHANGED",
        task.getPriority(),
        value
      );
      task.setPriority(value);
    }
    if (patch.has("assigneeId")) {
      var value = patch.uuid("assigneeId");
      access.activeAssignee(project.getOrganizationId(), value);
      if (!Objects.equals(value, task.getAssigneeId())) event(
        project,
        task,
        "ASSIGNEE_CHANGED",
        task.getAssigneeId(),
        value
      );
      task.setAssigneeId(value);
    }
    if (patch.has("dueDate")) {
      var value = patch.date("dueDate");
      if (!Objects.equals(value, task.getDueDate())) event(
        project,
        task,
        "DUE_DATE_CHANGED",
        task.getDueDate(),
        value
      );
      task.setDueDate(value);
    }
    tasks.flush();
    return views(List.of(task), project).getFirst();
  }

  @Transactional
  public void delete(UUID id) {
    var task = find(id);
    var project = access.project(task.getProjectId(), true);
    access.writable(project);
    manage(project);
    event(project, task, "TASK_DELETED", task.getTitle(), null);
    tasks.delete(task);
  }

  @Transactional(readOnly = true)
  public Statistics statistics(UUID projectId) {
    access.project(projectId, false);
    Map<TaskStatus, Long> counts = new EnumMap<>(TaskStatus.class);
    for (var status : TaskStatus.values()) counts.put(status, 0L);
    tasks
      .counts(projectId)
      .forEach(c -> counts.put(c.getStatus(), c.getCount()));
    return new Statistics(
      counts.values().stream().mapToLong(Long::longValue).sum(),
      counts
    );
  }

  private Task find(UUID id) {
    return tasks.findById(id).orElseThrow(() -> ApiException.missing("Task"));
  }

  private void manage(Project p) {
    access.require(p.getOrganizationId(), Role.OWNER, Role.ADMIN, Role.MANAGER);
  }

  private void event(
    Project p,
    Task t,
    String action,
    Object oldValue,
    Object newValue
  ) {
    activity.record(
      p.getOrganizationId(),
      p.getId(),
      "TASK",
      t.getId(),
      action,
      oldValue,
      newValue
    );
  }

  private List<View> views(List<Task> list, Project project) {
    if (list.isEmpty()) return List.of();
    Set<UUID> ids = new HashSet<>();
    list.forEach(t -> {
      ids.add(t.getReporterId());
      if (t.getAssigneeId() != null) ids.add(t.getAssigneeId());
    });
    var people = users
      .findAllById(ids)
      .stream()
      .collect(Collectors.toMap(AppUser::getId, Function.identity()));
    var tags = labels
      .labelsForTasks(list.stream().map(Task::getId).toList())
      .stream()
      .collect(
        Collectors.groupingBy(TaskLabelRepository.LabeledTask::getTaskId)
      );
    return list
      .stream()
      .map(t ->
        new View(
          t.getId(),
          project.getOrganizationId(),
          project.getId(),
          project.getProjectKey() + "-" + t.getTaskNumber(),
          t.getTitle(),
          t.getDescription(),
          t.getStatus(),
          t.getPriority(),
          UserView.of(people.get(t.getReporterId())),
          t.getAssigneeId() == null
            ? null
            : UserView.of(people.get(t.getAssigneeId())),
          t.getDueDate(),
          tags
            .getOrDefault(t.getId(), List.of())
            .stream()
            .map(l -> new LabelView(l.getId(), l.getName(), l.getColor()))
            .toList(),
          t.getVersion(),
          t.getCreatedAt(),
          t.getUpdatedAt()
        )
      )
      .toList();
  }
}
