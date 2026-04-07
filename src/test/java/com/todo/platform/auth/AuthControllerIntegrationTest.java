package com.todo.platform.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.BaseIntegrationTest;
import com.todo.platform.auth.dto.LoginRequest;
import com.todo.platform.auth.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Auth API Integration Tests")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @DisplayName("POST /api/auth/register - успешная регистрация")
    void register_ShouldReturn201_WhenValidRequest() {

        RegisterRequest request = new RegisterRequest(
                "newuser@test.com", "newuser", "password123"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("token");
    }

    @Test
    @DisplayName("POST /api/auth/register - дубликат email")
    void register_ShouldReturn400_WhenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "duplicate@test.com", "user1", "password123"
        );
        restTemplate.postForEntity("/api/auth/register", request, String.class);

        RegisterRequest duplicateRequest = new RegisterRequest(
                "duplicate@test.com", "user2", "password456"
        );
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register",
                duplicateRequest,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/auth/register - невалидный email")
    void register_ShouldReturn400_WhenInvalidEmail() {
        RegisterRequest request = new RegisterRequest(
                "not-an-email", "user", "password123"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/auth/register - короткий пароль")
    void register_ShouldReturn400_WhenPasswordTooShort() {
        RegisterRequest request = new RegisterRequest(
                "user@test.com", "user", "short"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/auth/login - успешный логин")
    void login_ShouldReturn200_WhenValidCredentials() {
        RegisterRequest register = new RegisterRequest(
                "loginuser@test.com", "loginuser", "password123"
        );
        restTemplate.postForEntity("/api/auth/register", register, String.class);

        LoginRequest login = new LoginRequest("loginuser@test.com", "password123");
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login",
                login,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("token");
    }

    @Test
    @DisplayName("POST /api/auth/login - неверный пароль")
    void login_ShouldReturn401_WhenWrongPassword() {
        RegisterRequest register = new RegisterRequest(
                "wrongpass@test.com", "user", "correctpassword"
        );
        restTemplate.postForEntity("/api/auth/register", register, String.class);

        LoginRequest login = new LoginRequest("wrongpass@test.com", "wrongpassword");
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login",
                login,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
