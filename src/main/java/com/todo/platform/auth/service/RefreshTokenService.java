package com.todo.platform.auth.service;

import com.todo.platform.auth.model.RefreshToken;
import com.todo.platform.auth.repository.RefreshTokenRepository;
import com.todo.platform.user.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // Создать новый refresh token
    public RefreshToken createRefreshToken(User user) {
        // Удаляем старые токены пользователя
        refreshTokenRepository.deleteAllByUser(user);

        RefreshToken refreshToken = new RefreshToken(
                user,
                UUID.randomUUID().toString(),
                LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000L)
        );

        return refreshTokenRepository.save(refreshToken);
    }

    // Найти и проверить refresh token
    @Transactional(readOnly = true)
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("Refresh token expired. Please login again");
        }

        return refreshToken;
    }

    // Удалить токен (логаут)
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteAllByUser(user);
    }

    // Очищаем просроченные токены каждую ночь в 3:00
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}