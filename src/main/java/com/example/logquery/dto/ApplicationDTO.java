package com.example.logquery.dto;

import com.example.logquery.entity.Application;

import java.time.LocalDateTime;

public record ApplicationDTO(
        Long id,
        String name,
        String description,
        String apiKey,
        boolean enabled,
        LocalDateTime createdAt) {

    public static ApplicationDTO from(Application app) {
        return new ApplicationDTO(
                app.getId(),
                app.getName(),
                app.getDescription(),
                app.getApiKey(),
                app.isEnabled(),
                app.getCreatedAt()
        );
    }

    public Application toEntity() {
        Application app = new Application(name, description);
        app.setId(id);
        app.setApiKey(apiKey);
        app.setEnabled(enabled);
        app.setCreatedAt(createdAt);
        return app;
    }
}
