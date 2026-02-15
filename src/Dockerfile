# ====== Stage 1: Build ======

FROM oracle-openjdk:21-jdk AS builder

WORKDIR /app
# Копируем gradle файлы для кэширования зависимостей
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Даём права на выполнение
RUN chmod +x gradlew

# Скачиваем зависимости (кэшируется если build.gradle не менялся)
RUN ./gradlew dependencies --no-daemon

# Копируем исходный код
COPY src src

# Собираем приложение (без тестов)
RUN ./gradlew bootJar --no-daemon -x test

# ====== Stage 2: Run ======
FROM oracle-openjdk:21-jre

WORKDIR /app

# Создаём непривилегированного пользователя
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Копируем JAR из builder стадии
COPY --from=builder /app/build/libs/*.jar app.jar

# Меняем владельца файлов
RUN chown -R appuser:appgroup /app

# Запускаем от непривилегированного пользователя
USER appuser

# Порт приложения
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Запуск с оптимизированными JVM параметрами
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]