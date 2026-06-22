package com.example.logquery.dto;

import com.example.logquery.entity.AlertRule;

import java.time.LocalDateTime;

public record AlertRuleDTO(
        Long id,
        String name,
        String level,
        int threshold,
        int windowMinutes,
        String webhookUrl,
        String emailRecipients,
        boolean enabled,
        LocalDateTime createdAt) {

    public static AlertRuleDTO from(AlertRule rule) {
        return new AlertRuleDTO(
                rule.getId(),
                rule.getName(),
                rule.getLevel(),
                rule.getThreshold(),
                rule.getWindowMinutes(),
                rule.getWebhookUrl(),
                rule.getEmailRecipients(),
                rule.isEnabled(),
                rule.getCreatedAt()
        );
    }

    public AlertRule toEntity() {
        AlertRule rule = new AlertRule();
        rule.setId(id);
        rule.setName(name);
        rule.setLevel(level);
        rule.setThreshold(threshold);
        rule.setWindowMinutes(windowMinutes);
        rule.setWebhookUrl(webhookUrl);
        rule.setEmailRecipients(emailRecipients);
        rule.setEnabled(enabled);
        rule.setCreatedAt(createdAt);
        return rule;
    }
}
