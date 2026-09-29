package com.example.workflow.label;
import jakarta.persistence.*;
import java.util.UUID;
@Entity
@Table(name="task_labels")
@IdClass(TaskLabelId.class)
public class TaskLabel {
    @Id private UUID taskId;
    @Id private UUID labelId;
    private UUID projectId;
    protected TaskLabel() {}
    public TaskLabel(UUID taskId,UUID labelId,UUID projectId) { this.taskId=taskId; this.labelId=labelId; this.projectId=projectId; }
    public UUID getTaskId() { return taskId; }
    public UUID getLabelId() { return labelId; }
}
