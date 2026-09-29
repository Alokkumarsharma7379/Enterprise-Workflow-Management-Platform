package com.example.workflow.label;
import java.io.Serializable;
import java.util.*;
public class TaskLabelId implements Serializable {
    private UUID taskId;
    private UUID labelId;
    public TaskLabelId() {}
    public TaskLabelId(UUID taskId,UUID labelId) { this.taskId=taskId; this.labelId=labelId; }
    @Override public boolean equals(Object value) { return value instanceof TaskLabelId other && Objects.equals(taskId,other.taskId) && Objects.equals(labelId,other.labelId); }
    @Override public int hashCode() { return Objects.hash(taskId,labelId); }
}
