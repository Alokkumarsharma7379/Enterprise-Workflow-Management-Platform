package com.example.workflow.label;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface TaskLabelRepository extends JpaRepository<TaskLabel,TaskLabelId> {
    interface LabeledTask { UUID getTaskId(); UUID getId(); String getName(); String getColor(); }
    @Query(value="select tl.task_id as taskId, l.id, l.name, l.color from task_labels tl join labels l on l.id=tl.label_id where tl.task_id in (:ids) order by l.name",nativeQuery=true)
    List<LabeledTask> labelsForTasks(Collection<UUID> ids);
}
