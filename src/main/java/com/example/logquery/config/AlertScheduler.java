package com.example.logquery.config;

import com.example.logquery.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertScheduler.class);

    private final AlertService alertService;

    public AlertScheduler(AlertService alertService) {
        this.alertService = alertService;
    }

    @Scheduled(fixedDelayString = "${alert.check-interval-seconds:60}000")
    public void checkAlerts() {
        log.debug("开始告警检查...");
        alertService.evaluateRules();
    }
}
