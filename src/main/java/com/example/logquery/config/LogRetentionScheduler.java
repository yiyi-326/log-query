package com.example.logquery.config;

import com.example.logquery.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LogRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(LogRetentionScheduler.class);

    private final LogService logService;

    @Value("${log.retention.days:30}")
    private int retentionDays;

    public LogRetentionScheduler(LogService logService) {
        this.logService = logService;
    }

    @Scheduled(cron = "${log.retention.cron:0 0 3 * * *}")
    public void cleanExpiredLogs() {
        LocalDateTime before = LocalDateTime.now().minusDays(retentionDays);
        long deleted = logService.deleteBefore(before);
        if (deleted > 0) {
            log.info("日志保留清理完成: 删除 {} 条超过 {} 天的日志 (截止 {})", deleted, retentionDays, before);
        }
    }
}
