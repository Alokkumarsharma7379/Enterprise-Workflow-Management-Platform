package com.example.workflow.task;
import com.example.workflow.common.ApiException;
import com.example.workflow.label.TaskLabel;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;
import java.util.*;

public final class TaskSpecifications {
    private TaskSpecifications() {}
    public static Specification<Task> filter(UUID project,String search,TaskStatus status,TaskPriority priority,UUID assignee,UUID label,String sort) {
        String[] parts=sort.split(",",-1);
        if (parts.length!=2 || !Set.of("createdAt","updatedAt","priority","dueDate").contains(parts[0]) || !Set.of("asc","desc").contains(parts[1])) throw ApiException.invalid("Invalid sort; use field,asc or field,desc");
        if (search!=null && search.length()>200) throw ApiException.invalid("Search must be at most 200 characters");
        return (root,query,cb) -> {
            List<Predicate> where=new ArrayList<>(); where.add(cb.equal(root.get("projectId"),project));
            if (status!=null) where.add(cb.equal(root.get("status"),status));
            if (priority!=null) where.add(cb.equal(root.get("priority"),priority));
            if (assignee!=null) where.add(cb.equal(root.get("assigneeId"),assignee));
            if (search!=null && !search.isBlank()) {
                String pattern="%"+search.trim().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
                where.add(cb.or(cb.like(cb.lower(root.get("title")),pattern,'\\'),cb.like(cb.lower(root.get("description")),pattern,'\\')));
            }
            if (label!=null) {
                var sub=query.subquery(UUID.class); var link=sub.from(TaskLabel.class);
                sub.select(link.get("taskId")).where(cb.equal(link.get("labelId"),label)); where.add(root.get("id").in(sub));
            }
            if (query.getResultType()!=Long.class && query.getResultType()!=long.class) {
                Expression<?> order=parts[0].equals("priority") ? cb.selectCase(root.get("priority"))
                    .when(TaskPriority.LOW,0).when(TaskPriority.MEDIUM,1).when(TaskPriority.HIGH,2).when(TaskPriority.CRITICAL,3).otherwise(0) : root.get(parts[0]);
                List<Order> ordering=new ArrayList<>();
                if (parts[0].equals("dueDate")) ordering.add(cb.asc(cb.selectCase().when(cb.isNull(order),1).otherwise(0)));
                ordering.add(parts[1].equals("asc")?cb.asc(order):cb.desc(order)); ordering.add(cb.asc(root.get("id"))); query.orderBy(ordering);
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }
}
