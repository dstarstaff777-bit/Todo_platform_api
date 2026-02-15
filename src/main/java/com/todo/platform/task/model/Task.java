package com.todo.platform.task.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.todo.platform.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Table(name = "tasks")
@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Setter
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Task() {}

    public Task(String title, String description, LocalDateTime dueDate, User owner) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.owner = owner;
        this.status = TaskStatus.CREATED;
    }

    public void update(String title, String description, LocalDateTime dueDate) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
    }

    public boolean isOverdue() {
        return dueDate != null
                && LocalDateTime.now().isAfter(dueDate)
                && status != TaskStatus.DONE;
    }

    public void markDone() {
        this.status = TaskStatus.DONE;
    }
}