package com.todo.platform.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.BaseIntegrationTest;
import com.todo.platform.auth.dto.RegisterRequest;
import com.todo.platform.auth.dto.TokenResponse;
import com.todo.platform.task.dto.TaskCreateRequest;
import com.todo.platform.task.dto.TaskStatusUpdateRequest;
import com.todo.platform.task.dto.TaskUpdateRequest;
import com.todo.platform.task.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Task API Integration Tests")
class TaskControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;
    private HttpHeaders authHeaders;

    @BeforeEach
    void setUp() {
        // Регистрируем нового пользователя перед каждым тестом
        String uniqueEmail = "taskuser" + System.currentTimeMillis() + "@test.com";
        RegisterRequest register = new RegisterRequest(
                uniqueEmail, "taskuser", "password123"
        );
        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                "/api/auth/register", register, TokenResponse.class
        );

        authToken = response.getBody().token();
        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(authToken);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
    }

    // ==================== CREATE TASK ====================

    @Test
    @DisplayName("POST /api/tasks - успешное создание")
    void createTask_ShouldReturn201_WhenValidRequest() {
        // given
        TaskCreateRequest request = new TaskCreateRequest(
                "Test task", "Description", null
        );

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("Test task");
        assertThat(response.getBody()).contains("CREATED");
    }

    @Test
    @DisplayName("POST /api/tasks - без авторизации возвращает 401/403")
    void createTask_ShouldReturn401_WhenNoToken() {
        // given
        TaskCreateRequest request = new TaskCreateRequest("Task", "Desc", null);
        HttpHeaders noAuthHeaders = new HttpHeaders();
        noAuthHeaders.setContentType(MediaType.APPLICATION_JSON);

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks",
                HttpMethod.POST,
                new HttpEntity<>(request, noAuthHeaders),
                String.class
        );

        // then
        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    @DisplayName("POST /api/tasks - с дедлайном")
    void createTask_ShouldSetDueDate_WhenProvided() {
        // given
        LocalDateTime dueDate = LocalDateTime.now().plusDays(7);
        TaskCreateRequest request = new TaskCreateRequest(
                "Task with deadline", "Desc", dueDate
        );

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("dueDate");
    }

    // ==================== GET TASKS ====================

    @Test
    @DisplayName("GET /api/tasks - список задач")
    void getTasks_ShouldReturnTasks_WhenAuthenticated() {
        // given - создаём задачи
        createTask("Task 1", "Desc 1");
        createTask("Task 2", "Desc 2");

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Task 1");
        assertThat(response.getBody()).contains("Task 2");
    }

    @Test
    @DisplayName("GET /api/tasks?status=CREATED - фильтрация по статусу")
    void getTasks_ShouldFilterByStatus() {
        // given
        createTask("Created task", "Desc");

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks?status=CREATED",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Created task");
    }

    @Test
    @DisplayName("GET /api/tasks?query=milk - поиск по тексту")
    void getTasks_ShouldSearchByQuery() {
        // given
        createTask("Buy milk", "In the shop");
        createTask("Walk the dog", "In the park");

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks?query=milk",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Buy milk");
        assertThat(response.getBody()).doesNotContain("Walk the dog");
    }

    // ==================== UPDATE STATUS ====================

    @Test
    @DisplayName("PATCH /api/tasks/{id}/status - обновление статуса")
    void updateStatus_ShouldChangeStatus_WhenTaskExists() {
        // given
        Long taskId = createTaskAndGetId("Status test task", "Desc");

        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest(TaskStatus.DONE);

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks/" + taskId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(request, authHeaders),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("DONE");
    }

    // ==================== UPDATE TASK ====================

    @Test
    @DisplayName("PUT /api/tasks/{id} - редактирование задачи")
    void updateTask_ShouldUpdateFields_WhenTaskExists() {
        // given
        Long taskId = createTaskAndGetId("Original title", "Original desc");
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Updated title", "Updated desc", null
        );

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks/" + taskId,
                HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Updated title");
        assertThat(response.getBody()).contains("Updated desc");
    }

    // ==================== DELETE ====================

    @Test
    @DisplayName("DELETE /api/tasks/{id} - удаление задачи")
    void deleteTask_ShouldReturn204_WhenTaskExists() {
        // given
        Long taskId = createTaskAndGetId("Task to delete", "Desc");

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks/" + taskId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("DELETE /api/tasks/{id} - задача не найдена")
    void deleteTask_ShouldReturn404_WhenTaskNotFound() {
        // when
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks/999999",
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("DELETE - нельзя удалить чужую задачу")
    void deleteTask_ShouldReturn404_WhenTaskBelongsToOtherUser() throws Exception {
        // given - создаём задачу от первого пользователя
        Long taskId = createTaskAndGetId("Owner task", "Desc");

        // Создаём второго пользователя
        String otherEmail = "other" + System.currentTimeMillis() + "@test.com";
        RegisterRequest otherRegister = new RegisterRequest(
                otherEmail, "otheruser", "password123"
        );
        ResponseEntity<TokenResponse> otherResponse = restTemplate.postForEntity(
                "/api/auth/register", otherRegister, TokenResponse.class
        );
        HttpHeaders otherHeaders = new HttpHeaders();
        otherHeaders.setBearerAuth(otherResponse.getBody().token());
        otherHeaders.setContentType(MediaType.APPLICATION_JSON);

        // when - второй пользователь пытается удалить задачу первого
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks/" + taskId,
                HttpMethod.DELETE,
                new HttpEntity<>(otherHeaders),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ==================== OVERDUE ====================

    @Test
    @DisplayName("GET /api/tasks/overdue - пустой список если нет просроченных")
    void getOverdue_ShouldReturnEmpty_WhenNoOverdueTasks() {
        // when
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks/overdue",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"totalElements\":0");
    }

    // ==================== HELPERS ====================

    private void createTask(String title, String description) {
        TaskCreateRequest request = new TaskCreateRequest(title, description, null);
        restTemplate.exchange(
                "/api/tasks",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders),
                String.class
        );
    }

    private Long createTaskAndGetId(String title, String description) {
        TaskCreateRequest request = new TaskCreateRequest(title, description, null);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders),
                String.class
        );

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            return mapper.readTree(response.getBody()).get("id").asLong();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse task id", e);
        }
    }
}