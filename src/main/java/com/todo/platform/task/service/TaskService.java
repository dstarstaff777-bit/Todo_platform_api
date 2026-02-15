package com.todo.platform.task.service;


import com.todo.platform.task.dto.TaskCreateRequest;
import com.todo.platform.task.dto.TaskUpdateRequest;
import com.todo.platform.task.model.Task;
import com.todo.platform.task.model.TaskStatus;
import com.todo.platform.task.repository.TaskRepository;
import com.todo.platform.user.model.User;
import com.todo.platform.user.service.UserService;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Getter
@Slf4j
@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository repository;
    private final UserService userService;

    public TaskService(TaskRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    @Transactional
    public Task create(TaskCreateRequest request) {
        User user = userService.getCurrentUser();
        Task task = new Task(
                request.title(),
                request.description(),
                request.dueDate(),
                user
        );
        return repository.save(task);
    }

    public Page<Task> getMyTasks(TaskStatus status, String query, Pageable pageable) {
        User user = userService.getCurrentUser();

        if (status != null && query != null && !query.isBlank()) {
            return repository.searchByOwnerAndStatus(user, status, query, pageable);
        }
        if (query != null && !query.isBlank()) {
            return repository.searchByOwner(user, query, pageable);
        }
        if (status != null) {
            return repository.findByOwnerAndStatus(user, status, pageable);
        }
        return repository.findByOwner(user, pageable);
    }

    // Просроченные задачи
    public Page<Task> getOverdueTasks(Pageable pageable) {
        User user = userService.getCurrentUser();
        return repository.findOverdue(user, LocalDateTime.now(), pageable);
    }

    // Задачи с дедлайном в ближайшие N часов (по умолчанию 24)
    public Page<Task> getDueSoonTasks(int hours, Pageable pageable) {
        User user = userService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusHours(hours);
        return repository.findDueSoon(user, now, deadline, pageable);
    }

    @Transactional
    public Task update(Long taskId, TaskUpdateRequest request) {
        User user = userService.getCurrentUser();
        Task task = repository.findByIdAndOwner(taskId, user)
                .orElseThrow(() -> new IllegalStateException("Task not found"));
        task.update(request.title(), request.description(), request.dueDate());
        return repository.save(task);
    }

    @Transactional
    public Task updateStatus(Long taskId, TaskStatus newStatus) {
        User user = userService.getCurrentUser();
        Task task = repository.findByIdAndOwner(taskId, user)
                .orElseThrow(() -> new IllegalStateException("Task not found"));
        task.setStatus(newStatus);
        return repository.save(task);
    }

    @Transactional
    public void delete(Long taskId) {
        User user = userService.getCurrentUser();
        Task task = repository.findByIdAndOwner(taskId, user)
                .orElseThrow(() -> new IllegalStateException("Task not found"));
        repository.delete(task);
    }
}