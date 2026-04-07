package com.todo.platform.task.repository;

import com.todo.platform.task.model.Task;
import com.todo.platform.task.model.TaskStatus;
import com.todo.platform.user.model.User;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDateTime;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByOwner(User owner, Pageable pageable);

    Page<Task> findByOwnerAndStatus(User owner, TaskStatus status, Pageable pageable);

    @Query("""
        SELECT t FROM Task t
        WHERE t.owner = :owner
        AND t.dueDate IS NOT NULL
        AND t.dueDate < :now
        AND t.status != 'DONE'
        """)
    Page<Task> findOverdue(
            @Param("owner") User owner,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("""
        SELECT t FROM Task t
        WHERE t.owner = :owner
        AND t.dueDate IS NOT NULL
        AND t.dueDate BETWEEN :now AND :deadline
        AND t.status != 'DONE'
        """)
    Page<Task> findDueSoon(
            @Param("owner") User owner,
            @Param("now") LocalDateTime now,
            @Param("deadline") LocalDateTime deadline,
            Pageable pageable
    );

    @Query("""
        SELECT t FROM Task t
        WHERE t.owner = :owner
        AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')))
        """)
    Page<Task> searchByOwner(
            @Param("owner") User owner,
            @Param("query") String query,
            Pageable pageable
    );

    @Query("""
        SELECT t FROM Task t
        WHERE t.owner = :owner
        AND t.status = :status
        AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')))
        """)
    Page<Task> searchByOwnerAndStatus(
            @Param("owner") User owner,
            @Param("status") TaskStatus status,
            @Param("query") String query,
            Pageable pageable
    );

    Optional<Task> findByIdAndOwner(Long id, User owner);
}