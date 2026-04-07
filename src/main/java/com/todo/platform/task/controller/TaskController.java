package com.todo.platform.task.controller;

import com.todo.platform.task.dto.TaskCreateRequest;
import com.todo.platform.task.dto.TaskResponse;
import com.todo.platform.task.dto.TaskStatusUpdateRequest;
import com.todo.platform.task.dto.TaskUpdateRequest;
import com.todo.platform.task.model.TaskStatus;
import com.todo.platform.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@RequestBody @Valid TaskCreateRequest request) {
        return TaskResponse.from(service.create(request));
    }

    @GetMapping
    public Page<TaskResponse> myTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return service.getMyTasks(status, query, pageable).map(TaskResponse::from);
    }

    @GetMapping("/overdue")
    public Page<TaskResponse> overdue(
            @PageableDefault(size = 20, sort = "dueDate") Pageable pageable
    ) {
        return service.getOverdueTasks(pageable).map(TaskResponse::from);
    }

    @GetMapping("/due-soon")
    public Page<TaskResponse> dueSoon(
            @RequestParam(defaultValue = "24") int hours,
            @PageableDefault(size = 20, sort = "dueDate") Pageable pageable
    ) {
        return service.getDueSoonTasks(hours, pageable).map(TaskResponse::from);
    }

    @PutMapping("/{id}")
    public TaskResponse update(
            @PathVariable Long id,
            @RequestBody @Valid TaskUpdateRequest request
    ) {
        return TaskResponse.from(service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(
            @PathVariable Long id,
            @RequestBody TaskStatusUpdateRequest request
    ) {
        return TaskResponse.from(service.updateStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}