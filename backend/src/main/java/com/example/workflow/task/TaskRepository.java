package com.example.workflow.task;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface TaskRepository extends JpaRepository<Task,UUID>, JpaSpecificationExecutor<Task> {
    List<Task> findByProjectIdInAndAssigneeId(List<UUID> projectIds,UUID assigneeId,Pageable pageable);
    interface CountByStatus { TaskStatus getStatus(); long getCount(); }
    @Query("select t.status as status, count(t) as count from Task t where t.projectId = :project group by t.status")
    List<CountByStatus> counts(UUID project);
}
