package com.example.logquery.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "alert_records")
public class AlertRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ruleId;

    @Column(nullable = false, length = 100)
    private String ruleName;

    @Column(nullable = false, length = 10)
    private String level;

    @Column(nullable = false)
    private long matchCount;

    @Column(nullable = false)
    private int threshold;

    @Column(nullable = false)
    private int windowMinutes;

    @Column(nullable = false)
    private LocalDateTime triggeredAt;

    @Column(length = 20)
    private String notificationStatus;

    @Column(length = 1000)
    private String notificationDetail;

    @PrePersist
    protected void onCreate() {
        if (triggeredAt == null) {
            triggeredAt = LocalDateTime.now();
        }
    }

    public AlertRecord() {
        // JPA no-arg constructor
    }

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
    public Long getRuleId() { return ruleId; }
    public String getRuleName() { return ruleName; }
    public String getLevel() { return level; }
    public long getMatchCount() { return matchCount; }
    public int getThreshold() { return threshold; }
    public int getWindowMinutes() { return windowMinutes; }
    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public String getNotificationStatus() { return notificationStatus; }
    public String getNotificationDetail() { return notificationDetail; }

    public void setNotificationStatus(String notificationStatus) {
        this.notificationStatus = notificationStatus;
    }

    public void setNotificationDetail(String notificationDetail) {
        this.notificationDetail = notificationDetail;
    }
}
