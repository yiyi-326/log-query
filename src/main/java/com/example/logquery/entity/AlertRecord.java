package com.example.logquery.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_records")
public class AlertRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(nullable = false, length = 10)
    private String level;

    @Column(name = "match_count", nullable = false)
    private long matchCount;

    @Column(nullable = false)
    private int threshold;

    @Column(name = "window_minutes", nullable = false)
    private int windowMinutes;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "notification_status", length = 20)
    private String notificationStatus; // pending, webhook_sent, email_sent, both_sent, failed

    @Column(name = "notification_detail", length = 1000)
    private String notificationDetail;

    @PrePersist
    protected void onCreate() {
        if (triggeredAt == null) {
            triggeredAt = LocalDateTime.now();
        }
    }

    public AlertRecord() {}

    public AlertRecord(Long ruleId, String ruleName, String level, long matchCount,
                       int threshold, int windowMinutes) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.level = level;
        this.matchCount = matchCount;
        this.threshold = threshold;
        this.windowMinutes = windowMinutes;
        this.triggeredAt = LocalDateTime.now();
        this.notificationStatus = "pending";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public long getMatchCount() { return matchCount; }
    public void setMatchCount(long matchCount) { this.matchCount = matchCount; }

    public int getThreshold() { return threshold; }
    public void setThreshold(int threshold) { this.threshold = threshold; }

    public int getWindowMinutes() { return windowMinutes; }
    public void setWindowMinutes(int windowMinutes) { this.windowMinutes = windowMinutes; }

    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }

    public String getNotificationStatus() { return notificationStatus; }
    public void setNotificationStatus(String notificationStatus) { this.notificationStatus = notificationStatus; }

    public String getNotificationDetail() { return notificationDetail; }
    public void setNotificationDetail(String notificationDetail) { this.notificationDetail = notificationDetail; }
}
