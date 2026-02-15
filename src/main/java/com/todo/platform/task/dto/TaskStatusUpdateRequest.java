package com.todo.platform.task.dto;

import com.todo.platform.task.model.TaskStatus;

public record TaskStatusUpdateRequest(TaskStatus status) {
}
