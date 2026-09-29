package com.example.workflow.activity;

import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<ActivityLog, UUID> {
  Page<ActivityLog> findByOrganizationId(UUID id, Pageable pageable);
  Page<ActivityLog> findByProjectId(UUID id, Pageable pageable);
  Page<ActivityLog> findByEntityTypeAndEntityId(
    String type,
    UUID id,
    Pageable pageable
  );
}
