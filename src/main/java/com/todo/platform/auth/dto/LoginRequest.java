package com.todo.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public record LoginRequest(String email, String password) {}