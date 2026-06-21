package com.example.logquery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LogQueryApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogQueryApplication.class, args);
    }
} 