package com.example.workflow.project;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
  List<Project> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
