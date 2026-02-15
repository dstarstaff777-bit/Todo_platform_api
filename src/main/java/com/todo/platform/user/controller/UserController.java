package com.todo.platform.user.controller;

import com.todo.platform.user.dto.UpdateProfileRequest;
import com.todo.platform.user.dto.UserResponse;
import com.todo.platform.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Получить профиль текущего пользователя
    @GetMapping("/me")
    public UserResponse getProfile() {
        return UserResponse.from(userService.getCurrentUser());
    }

    // Обновить профиль
    @PatchMapping("/me")
    public UserResponse updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        return UserResponse.from(userService.updateProfile(request));
    }
}