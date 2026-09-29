package com.example.workflow.comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.UUID;
public interface CommentRepository extends JpaRepository<Comment,UUID> {
    Page<Comment> findByTaskId(UUID taskId,Pageable pageable);
}
