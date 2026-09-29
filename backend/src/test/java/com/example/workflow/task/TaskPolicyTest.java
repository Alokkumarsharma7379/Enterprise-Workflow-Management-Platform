package com.example.workflow.task;
import com.example.workflow.common.ApiException;
import com.example.workflow.organization.Role;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class TaskPolicyTest {
    private final UUID actor=UUID.randomUUID();
    @Test void assignedMemberCanChangeStatus() { assertDoesNotThrow(() -> TaskPolicy.edit(Role.MEMBER,actor,actor,Set.of("status","version"))); }
    @Test void memberCannotReassignOwnTask() { assertThrows(ApiException.class,() -> TaskPolicy.edit(Role.MEMBER,actor,actor,Set.of("assigneeId","version"))); }
    @Test void memberCannotEditAnotherPersonsTask() { assertThrows(ApiException.class,() -> TaskPolicy.edit(Role.MEMBER,actor,UUID.randomUUID(),Set.of("title","version"))); }
    @Test void memberCannotEditUnassignedTask() { assertThrows(ApiException.class,() -> TaskPolicy.edit(Role.MEMBER,actor,null,Set.of("status","version"))); }
    @Test void managerCanAssign() { assertDoesNotThrow(() -> TaskPolicy.edit(Role.MANAGER,actor,null,Set.of("assigneeId","version"))); }
}
