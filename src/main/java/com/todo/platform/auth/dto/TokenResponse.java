package com.todo.platform.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn  // в секундах
) {
    // Для обратной совместимости если где-то используется старый вариант
    public String token() {
        return accessToken;
    }
}