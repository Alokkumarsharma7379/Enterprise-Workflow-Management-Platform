package com.example.workflow.label;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface LabelRepository extends JpaRepository<Label,UUID> {
    List<Label> findByProjectIdOrderByName(UUID projectId);
}
