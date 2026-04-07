package com.todo.platform.auth.service;

import com.todo.platform.auth.dto.LoginRequest;
import com.todo.platform.auth.dto.RegisterRequest;
import com.todo.platform.auth.dto.TokenResponse;
import com.todo.platform.auth.model.AuthUser;
import com.todo.platform.auth.model.RefreshToken;
import com.todo.platform.auth.security.SecurityUserDetailsService;
import com.todo.platform.user.model.User;
import com.todo.platform.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class AuthService {

    @Value("${jwt.expiration:1800000}")
    private long jwtExpirationMs;

    private final SecurityUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public AuthService(SecurityUserDetailsService userDetailsService,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       UserRepository userRepository,
                       RefreshTokenService refreshTokenService) {
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
    }

    public TokenResponse login(LoginRequest request) {
        AuthUser authUser = userDetailsService.loadUserByUsername(request.email());

        if (!passwordEncoder.matches(request.password(), authUser.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        return buildTokenResponse(authUser, user);
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        User user = new User(
                request.email(),
                request.username(),
                passwordEncoder.encode(request.password())
        );
        User savedUser = userRepository.save(user);

        AuthUser authUser = new AuthUser(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        return buildTokenResponse(authUser, savedUser);
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenValue);
        User user = refreshToken.getUser();

        AuthUser authUser = new AuthUser(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        String newAccessToken = jwtService.generateToken(authUser);

        return new TokenResponse(
                newAccessToken,
                refreshToken.getToken(),
                jwtExpirationMs / 1000
        );
    }

    @Transactional
    public void logout(String email) {
        userRepository.findByEmail(email)
                .ifPresent(refreshTokenService::deleteByUser);
    }

    private TokenResponse buildTokenResponse(AuthUser authUser, User user) {
        String accessToken = jwtService.generateToken(authUser);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new TokenResponse(
                accessToken,
                refreshToken.getToken(),
                jwtExpirationMs / 1000
        );
    }
}