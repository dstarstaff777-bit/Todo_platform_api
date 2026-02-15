package com.todo.platform.task.dto;

import com.todo.platform.task.model.Task;
import com.todo.platform.task.model.TaskStatus;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        LocalDateTime dueDate,
        boolean overdue,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate(),
                task.isOverdue(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}