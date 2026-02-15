package com.todo.platform.auth.service;




import com.todo.platform.auth.dto.LoginRequest;
import com.todo.platform.auth.dto.RegisterRequest;
import com.todo.platform.auth.dto.TokenResponse;
import com.todo.platform.auth.model.AuthUser;
import com.todo.platform.auth.security.SecurityUserDetailsService;
import com.todo.platform.user.model.User;
import com.todo.platform.user.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class AuthService {
    private final SecurityUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public AuthService(SecurityUserDetailsService userDetailsService,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       UserRepository userRepository) {
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public TokenResponse login(LoginRequest request) {
        AuthUser user = userDetailsService.loadUserByUsername(request.email());

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return new TokenResponse(jwtService.generateToken(user));
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


        return new TokenResponse(jwtService.generateToken(authUser));
    }
}
