package com.todo.platform.auth.service;

import com.todo.platform.auth.dto.LoginRequest;
import com.todo.platform.auth.dto.RegisterRequest;
import com.todo.platform.auth.dto.TokenResponse;
import com.todo.platform.auth.model.AuthUser;
import com.todo.platform.auth.security.SecurityUserDetailsService;
import com.todo.platform.user.model.User;
import com.todo.platform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private SecurityUserDetailsService userDetailsService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private AuthUser testAuthUser;
    private User testUser;

    @BeforeEach
    void setUp() {
        testAuthUser = new AuthUser(
                1L,
                "test@example.com",
                "$2a$10$hashedpassword",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        testUser = new User("test@example.com", "testuser", "$2a$10$hashedpassword");
    }


    @Test
    @DisplayName("Логин - успешно")
    void login_ShouldReturnToken_WhenValidCredentials() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        when(userDetailsService.loadUserByUsername("test@example.com"))
                .thenReturn(testAuthUser);
        when(passwordEncoder.matches("password123", testAuthUser.getPassword()))
                .thenReturn(true);
        when(jwtService.generateToken(testAuthUser)).thenReturn("jwt.token.here");


        TokenResponse result = authService.login(request);

        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("jwt.token.here");
    }

    @Test
    @DisplayName("Логин - неверный пароль")
    void login_ShouldThrow_WhenWrongPassword() {

        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
        when(userDetailsService.loadUserByUsername("test@example.com"))
                .thenReturn(testAuthUser);
        when(passwordEncoder.matches("wrongpassword", testAuthUser.getPassword()))
                .thenReturn(false);


        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }


    @Test
    @DisplayName("Регистрация - успешно")
    void register_ShouldReturnToken_WhenNewUser() {

        RegisterRequest request = new RegisterRequest(
                "newuser@example.com", "newuser", "password123"
        );
        when(userRepository.findByEmail("newuser@example.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123"))
                .thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(AuthUser.class))).thenReturn("jwt.token.here");

        TokenResponse result = authService.register(request);

        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("jwt.token.here");
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("Регистрация - email уже занят")
    void register_ShouldThrow_WhenEmailAlreadyExists() {

        RegisterRequest request = new RegisterRequest(
                "test@example.com", "testuser", "password123"
        );
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Регистрация - пароль хешируется")
    void register_ShouldEncodePassword_BeforeSaving() {

        RegisterRequest request = new RegisterRequest(
                "new@example.com", "newuser", "plainpassword"
        );
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plainpassword")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any())).thenReturn("token");

        authService.register(request);

        verify(passwordEncoder).encode("plainpassword");
        verify(userRepository).save(argThat(user ->
                user.getPassword().equals("$2a$10$encoded")
        ));
    }
}
