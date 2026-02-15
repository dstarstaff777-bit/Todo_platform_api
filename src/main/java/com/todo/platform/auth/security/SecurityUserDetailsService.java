package com.todo.platform.auth.security;

import com.todo.platform.auth.model.AuthUser;
import com.todo.platform.user.model.User;
import com.todo.platform.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecurityUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public SecurityUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AuthUser loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();

        return new AuthUser(
                user.getId(),
                user.getEmail(),
                "hashed-password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}