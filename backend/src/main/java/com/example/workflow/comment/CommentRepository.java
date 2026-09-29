package com.example.workflow.comment;

import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
  Page<Comment> findByTaskId(UUID taskId, Pageable pageable);
}
