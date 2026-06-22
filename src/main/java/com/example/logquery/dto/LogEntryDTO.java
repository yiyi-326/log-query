package com.example.logquery.dto;

import com.example.logquery.entity.LogEntry;

import java.time.LocalDateTime;

public record LogEntryDTO(
        Long id,
        LocalDateTime timestamp,
        String level,
        String source,
        String message,
        Long appId,
        LocalDateTime createdAt) {

    public static LogEntryDTO from(LogEntry e) {
        return new LogEntryDTO(
                e.getId(),
                e.getTimestamp(),
                e.getLevel(),
                e.getSource(),
                e.getMessage(),
                e.getAppId(),
                e.getCreatedAt()
        );
    }

    public LogEntry toEntity() {
        LogEntry e = new LogEntry(timestamp, level, source, message);
        e.setId(id);
        e.setAppId(appId);
        e.setCreatedAt(createdAt);
        return e;
    }
}
