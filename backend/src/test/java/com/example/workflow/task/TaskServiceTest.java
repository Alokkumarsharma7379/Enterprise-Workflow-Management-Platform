package com.example.workflow.task;
import com.example.workflow.activity.ActivityService;
import com.example.workflow.auth.Actor;
import com.example.workflow.common.ApiException;
import com.example.workflow.label.TaskLabelRepository;
import com.example.workflow.organization.OrganizationAccess;
import com.example.workflow.project.Project;
import com.example.workflow.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class TaskServiceTest {
    @Test void invalidAssigneeDoesNotAllocateNumberOrSaveTask() {
        var tasks=mock(TaskRepository.class); var access=mock(OrganizationAccess.class); var actor=mock(Actor.class);
        var activity=mock(ActivityService.class); var users=mock(UserRepository.class); var labels=mock(TaskLabelRepository.class); var em=mock(EntityManager.class);
        var project=new Project(); UUID org=UUID.randomUUID(),assignee=UUID.randomUUID(); project.setOrganizationId(org); project.setNextTaskNumber(9);
        when(access.project(project.getId(),true)).thenReturn(project);
        doThrow(ApiException.invalid("Invalid assignee")).when(access).activeAssignee(org,assignee);
        var service=new TaskService(tasks,access,actor,activity,users,labels,em);
        assertThrows(ApiException.class,() -> service.create(project.getId(),new TaskDtos.Create("Task",null,TaskPriority.HIGH,assignee,null)));
        assertEquals(9,project.getNextTaskNumber()); verifyNoInteractions(tasks,activity);
    }
}
