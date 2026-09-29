package com.example.workflow.label;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, UUID> {
  List<Label> findByProjectIdOrderByName(UUID projectId);
}
