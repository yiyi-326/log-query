package com.example.logquery.dto;

import com.example.logquery.entity.AlertRecord;

import java.time.LocalDateTime;

public record AlertRecordDTO(
        Long id,
        Long ruleId,
        String ruleName,
        String level,
        long matchCount,
        int threshold,
        int windowMinutes,
        LocalDateTime triggeredAt,
        String notificationStatus,
        String notificationDetail) {

    public static AlertRecordDTO from(AlertRecord entity) {
        return new AlertRecordDTO(
                entity.getId(),
                entity.getRuleId(),
                entity.getRuleName(),
                entity.getLevel(),
                entity.getMatchCount(),
                entity.getThreshold(),
                entity.getWindowMinutes(),
                entity.getTriggeredAt(),
                entity.getNotificationStatus(),
                entity.getNotificationDetail()
        );
    }
}
