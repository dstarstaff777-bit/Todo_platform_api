package com.todo.platform.task.service;

import com.todo.platform.task.dto.TaskCreateRequest;
import com.todo.platform.task.dto.TaskUpdateRequest;
import com.todo.platform.task.model.Task;
import com.todo.platform.task.model.TaskStatus;
import com.todo.platform.task.repository.TaskRepository;
import com.todo.platform.user.model.User;
import com.todo.platform.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Unit Tests")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TaskService taskService;

    private User testUser;
    private Task testTask;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "testuser", "hashedpassword");
        testTask = new Task("Test task", "Description", null, testUser);
    }

    // ==================== CREATE ====================

    @Test
    @DisplayName("Создание задачи - успешно")
    void create_ShouldReturnTask_WhenValidRequest() {
        // given
        TaskCreateRequest request = new TaskCreateRequest(
                "Test task", "Description", null
        );
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // when
        Task result = taskService.create(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test task");
        assertThat(result.getStatus()).isEqualTo(TaskStatus.CREATED);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("Создание задачи с дедлайном - успешно")
    void create_ShouldSetDueDate_WhenProvided() {
        // given
        LocalDateTime dueDate = LocalDateTime.now().plusDays(3);
        TaskCreateRequest request = new TaskCreateRequest(
                "Task with deadline", "Description", dueDate
        );
        Task taskWithDeadline = new Task("Task with deadline", "Description", dueDate, testUser);

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(taskRepository.save(any(Task.class))).thenReturn(taskWithDeadline);

        // when
        Task result = taskService.create(request);

        // then
        assertThat(result.getDueDate()).isEqualTo(dueDate);
    }

    // ==================== GET TASKS ====================

    @Test
    @DisplayName("Получение задач без фильтров - успешно")
    void getMyTasks_ShouldReturnAllTasks_WhenNoFilters() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Task> expectedPage = new PageImpl<>(List.of(testTask));

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByOwner(testUser, pageable)).thenReturn(expectedPage);

        // when
        Page<Task> result = taskService.getMyTasks(null, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(taskRepository).findByOwner(testUser, pageable);
        verify(taskRepository, never()).findByOwnerAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("Фильтрация задач по статусу - успешно")
    void getMyTasks_ShouldFilterByStatus_WhenStatusProvided() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Task> expectedPage = new PageImpl<>(List.of(testTask));

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByOwnerAndStatus(testUser, TaskStatus.CREATED, pageable))
                .thenReturn(expectedPage);

        // when
        Page<Task> result = taskService.getMyTasks(TaskStatus.CREATED, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(taskRepository).findByOwnerAndStatus(testUser, TaskStatus.CREATED, pageable);
        verify(taskRepository, never()).findByOwner(any(), any());
    }

    @Test
    @DisplayName("Поиск задач по тексту - успешно")
    void getMyTasks_ShouldSearchByQuery_WhenQueryProvided() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Task> expectedPage = new PageImpl<>(List.of(testTask));

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(taskRepository.searchByOwner(testUser, "test", pageable))
                .thenReturn(expectedPage);

        // when
        Page<Task> result = taskService.getMyTasks(null, "test", pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(taskRepository).searchByOwner(testUser, "test", pageable);
    }

    // ==================== UPDATE STATUS ====================

    @Test
    @DisplayName("Обновление статуса задачи - успешно")
    void updateStatus_ShouldChangeStatus_WhenTaskExists() {
        // given
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByIdAndOwner(1L, testUser)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(testTask)).thenReturn(testTask);

        // when
        Task result = taskService.updateStatus(1L, TaskStatus.DONE);

        // then
        assertThat(result.getStatus()).isEqualTo(TaskStatus.DONE);
        verify(taskRepository).save(testTask);
    }

    @Test
    @DisplayName("Обновление статуса - задача не найдена")
    void updateStatus_ShouldThrow_WhenTaskNotFound() {
        // given
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByIdAndOwner(999L, testUser)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> taskService.updateStatus(999L, TaskStatus.DONE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Task not found");

        verify(taskRepository, never()).save(any());
    }

    // ==================== UPDATE ====================

    @Test
    @DisplayName("Редактирование задачи - успешно")
    void update_ShouldUpdateTask_WhenValidRequest() {
        // given
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Updated title", "Updated description", null
        );
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByIdAndOwner(1L, testUser)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(testTask)).thenReturn(testTask);

        // when
        Task result = taskService.update(1L, request);

        // then
        assertThat(result.getTitle()).isEqualTo("Updated title");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        verify(taskRepository).save(testTask);
    }

    @Test
    @DisplayName("Редактирование задачи - не найдена")
    void update_ShouldThrow_WhenTaskNotFound() {
        // given
        TaskUpdateRequest request = new TaskUpdateRequest("Title", "Desc", null);
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByIdAndOwner(999L, testUser)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> taskService.update(999L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Task not found");
    }

    // ==================== DELETE ====================

    @Test
    @DisplayName("Удаление задачи - успешно")
    void delete_ShouldDeleteTask_WhenTaskExists() {
        // given
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByIdAndOwner(1L, testUser)).thenReturn(Optional.of(testTask));

        // when
        taskService.delete(1L);

        // then
        verify(taskRepository).delete(testTask);
    }

    @Test
    @DisplayName("Удаление задачи - не найдена")
    void delete_ShouldThrow_WhenTaskNotFound() {
        // given
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByIdAndOwner(999L, testUser)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> taskService.delete(999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Task not found");

        verify(taskRepository, never()).delete(any());
    }

    // ==================== OVERDUE ====================

    @Test
    @DisplayName("Просроченные задачи - успешно")
    void getOverdueTasks_ShouldReturnOverdueTasks() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Task overdueTask = new Task(
                "Overdue task", "Desc",
                LocalDateTime.now().minusDays(1),  // дедлайн вчера
                testUser
        );
        Page<Task> expectedPage = new PageImpl<>(List.of(overdueTask));

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(taskRepository.findOverdue(eq(testUser), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(expectedPage);

        // when
        Page<Task> result = taskService.getOverdueTasks(pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).isOverdue()).isTrue();
    }

    // ==================== TASK MODEL ====================

    @Test
    @DisplayName("isOverdue - задача просрочена")
    void isOverdue_ShouldReturnTrue_WhenDeadlinePassed() {
        // given
        Task task = new Task(
                "Task", "Desc",
                LocalDateTime.now().minusHours(1),
                testUser
        );

        // when & then
        assertThat(task.isOverdue()).isTrue();
    }

    @Test
    @DisplayName("isOverdue - задача не просрочена (дедлайн в будущем)")
    void isOverdue_ShouldReturnFalse_WhenDeadlineInFuture() {
        // given
        Task task = new Task(
                "Task", "Desc",
                LocalDateTime.now().plusDays(1),
                testUser
        );

        // when & then
        assertThat(task.isOverdue()).isFalse();
    }

    @Test
    @DisplayName("isOverdue - задача выполнена, не просрочена")
    void isOverdue_ShouldReturnFalse_WhenTaskIsDone() {
        // given
        Task task = new Task(
                "Task", "Desc",
                LocalDateTime.now().minusHours(1),  // дедлайн прошёл
                testUser
        );
        task.setStatus(TaskStatus.DONE);  // но задача выполнена

        // when & then
        assertThat(task.isOverdue()).isFalse();
    }

    @Test
    @DisplayName("isOverdue - задача без дедлайна, не просрочена")
    void isOverdue_ShouldReturnFalse_WhenNoDueDate() {
        // given
        Task task = new Task("Task", "Desc", null, testUser);

        // when & then
        assertThat(task.isOverdue()).isFalse();
    }
}