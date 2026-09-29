package com.example.workflow.activity;

import com.example.workflow.auth.Actor;
import com.example.workflow.common.*;
import com.example.workflow.organization.OrganizationAccess;
import com.example.workflow.task.TaskRepository;
import com.example.workflow.user.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ActivityService {
    private final ActivityRepository logs;
    private final Actor actor;
    private final OrganizationAccess access;
    private final TaskRepository tasks;
    private final UserRepository users;
    public ActivityService(ActivityRepository logs, Actor actor, OrganizationAccess access, TaskRepository tasks, UserRepository users) {
        this.logs=logs; this.actor=actor; this.access=access; this.tasks=tasks; this.users=users;
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void record(UUID org, UUID project, String type, UUID entity, String action, Object oldValue, Object newValue) {
        var log = new ActivityLog();
        log.setOrganizationId(org); log.setProjectId(project); log.setActorId(actor.id()); log.setEntityType(type);
        log.setEntityId(entity); log.setActionType(action);
        log.setOldValue(oldValue == null ? null : oldValue.toString()); log.setNewValue(newValue == null ? null : newValue.toString());
        logs.save(log);
    }
    public record View(UUID id, UserView actor, String actionType, String entityType, UUID entityId,
        String oldValue, String newValue, Instant createdAt) {}
    @Transactional(readOnly=true)
    public Pages.Result<View> list(String scope, UUID id, int page, int size) {
        var pageable = Pages.request(page, size);
        var result = switch (scope) {
            case "organization" -> { access.organization(id, false); yield logs.findByOrganizationId(id, pageable); }
            case "project" -> { access.project(id, false); yield logs.findByProjectId(id, pageable); }
            case "task" -> {
                var task = tasks.findById(id).orElseThrow(() -> ApiException.missing("Task"));
                access.project(task.getProjectId(), false);
                yield logs.findByEntityTypeAndEntityId("TASK", id, pageable);
            }
            default -> throw ApiException.invalid("Invalid activity scope");
        };
        var actors = users.findAllById(result.stream().map(ActivityLog::getActorId).collect(Collectors.toSet()))
            .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        return Pages.Result.of(result.map(log -> new View(log.getId(), UserView.of(actors.get(log.getActorId())),
            log.getActionType(), log.getEntityType(), log.getEntityId(), log.getOldValue(), log.getNewValue(), log.getCreatedAt())));
    }
}
