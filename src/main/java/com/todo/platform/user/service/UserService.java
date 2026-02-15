package com.todo.platform.user.service;



import com.todo.platform.auth.model.AuthUser;
import com.todo.platform.user.dto.UpdateProfileRequest;
import com.todo.platform.user.model.User;
import com.todo.platform.user.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User getCurrentUser() {
        AuthUser authUser =
                (AuthUser) SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal();

        return repository.findById(authUser.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    @Transactional
    public User updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();
        user.setUsername(request.username());
        return repository.save(user);
    }
}