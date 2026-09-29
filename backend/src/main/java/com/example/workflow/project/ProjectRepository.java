package com.example.workflow.project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
