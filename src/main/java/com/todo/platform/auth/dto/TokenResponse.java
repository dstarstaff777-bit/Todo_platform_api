package com.todo.platform.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn  // в секундах
) {
    public String token() {
        return accessToken;
    }
}