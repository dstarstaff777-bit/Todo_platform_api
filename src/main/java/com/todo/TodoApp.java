package com.todo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TodoApp {
    public static void main(String[] args) {
        System.out.println("========== STARTING APPLICATION ==========");
        SpringApplication.run(TodoApp.class, args);

    }
}