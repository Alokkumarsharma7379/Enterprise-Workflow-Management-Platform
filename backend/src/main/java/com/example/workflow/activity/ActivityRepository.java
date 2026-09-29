package com.example.workflow.activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.UUID;
public interface ActivityRepository extends JpaRepository<ActivityLog, UUID> {
    Page<ActivityLog> findByOrganizationId(UUID id, Pageable pageable);
    Page<ActivityLog> findByProjectId(UUID id, Pageable pageable);
    Page<ActivityLog> findByEntityTypeAndEntityId(String type, UUID id, Pageable pageable);
}
