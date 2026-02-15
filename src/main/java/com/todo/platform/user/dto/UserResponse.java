package com.todo.platform.user.dto;

import com.todo.platform.user.model.User;

public record UserResponse(
        Long id,
        String email,
        String username
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername()
        );
    }
}

