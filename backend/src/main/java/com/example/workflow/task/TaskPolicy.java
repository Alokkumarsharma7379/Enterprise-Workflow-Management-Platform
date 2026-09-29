package com.example.workflow.task;
import com.example.workflow.common.ApiException;
import com.example.workflow.organization.Role;
import java.util.*;
public final class TaskPolicy {
    private TaskPolicy() {}
    public static void edit(Role role,UUID actor,UUID assignee,Set<String> fields) {
        if (role!=Role.MEMBER) return;
        if (!actor.equals(assignee) || !Set.of("title","description","status","version").containsAll(fields)) throw ApiException.forbidden();
    }
}
