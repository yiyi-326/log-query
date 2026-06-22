package com.example.logquery.service;

import com.example.logquery.entity.AlertRecord;
import com.example.logquery.entity.AlertRule;
import com.example.logquery.entity.LogEntry;
import com.example.logquery.repository.AlertRecordRepository;
import com.example.logquery.repository.AlertRuleRepository;
import com.example.logquery.repository.LogEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRuleRepository ruleRepository;
    private final AlertRecordRepository recordRepository;
    private final LogEntryRepository logEntryRepository;
    private final NotificationService notificationService;

    public AlertService(AlertRuleRepository ruleRepository,
                        AlertRecordRepository recordRepository,
                        LogEntryRepository logEntryRepository,
                        NotificationService notificationService) {
        this.ruleRepository = ruleRepository;
        this.recordRepository = recordRepository;
        this.logEntryRepository = logEntryRepository;
        this.notificationService = notificationService;
    }

    // ==================== 规则 CRUD ====================

    public List<AlertRule> listRules() {
        return ruleRepository.findAll();
    }

    public AlertRule getRule(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("告警规则不存在: " + id));
    }

    @Transactional
    public AlertRule createRule(AlertRule rule) {
        return ruleRepository.save(rule);
    }

    @Transactional
    public AlertRule updateRule(Long id, AlertRule update) {
        AlertRule existing = getRule(id);
        existing.setName(update.getName());
        existing.setLevel(update.getLevel());
        existing.setThreshold(update.getThreshold());
        existing.setWindowMinutes(update.getWindowMinutes());
        existing.setWebhookUrl(update.getWebhookUrl());
        existing.setEmailRecipients(update.getEmailRecipients());
        existing.setEnabled(update.isEnabled());
        return ruleRepository.save(existing);
    }

    @Transactional
    public void deleteRule(Long id) {
        if (!ruleRepository.existsById(id)) {
            throw new IllegalArgumentException("告警规则不存在: " + id);
        }
        ruleRepository.deleteById(id);
    }

    // ==================== 告警历史 ====================

    public Page<AlertRecord> listRecords(int page, int size) {
        return recordRepository.findAllByOrderByTriggeredAtDesc(
                PageRequest.of(page, size));
    }

    // ==================== 统计 ====================

    public long count24h() {
        return recordRepository.countByTriggeredAtAfter(LocalDateTime.now().minusHours(24));
    }

    // ==================== 告警评估 ====================

    @Transactional
    public void evaluateRules() {
        List<AlertRule> rules = ruleRepository.findByEnabledTrue();
        for (AlertRule rule : rules) {
            try {
                evaluateRule(rule);
            } catch (RuntimeException e) {
                log.error("评估告警规则 [{}] 失败: {}", rule.getName(), e.getMessage());
            }
        }
    }

    private void evaluateRule(AlertRule rule) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(rule.getWindowMinutes());

        // 检查去重：同一规则在窗口期内已经触发过，不再重复告警
        if (recordRepository.existsByRuleIdAndTriggeredAtAfter(rule.getId(), windowStart)) {
            return;
        }

        // 使用 Specification 查询窗口内指定级别的日志数量
        long count = logEntryRepository.count((root, query, cb) ->
                cb.and(
                        cb.equal(root.get("level"), rule.getLevel()),
                        cb.greaterThanOrEqualTo(root.get("timestamp"), windowStart)
                )
        );

        if (count >= rule.getThreshold()) {
            AlertRecord record = new AlertRecord(
                    rule.getId(), rule.getName(), rule.getLevel(),
                    count, rule.getThreshold(), rule.getWindowMinutes()
            );

            // 发送通知
            String title = "日志告警: " + rule.getName();
            String content = buildAlertMessage(rule, count);
            StringBuilder status = new StringBuilder();

            String whResult = notificationService.sendWebhook(rule.getWebhookUrl(), title, content);
            status.append("webhook: ").append(whResult).append("; ");

            String emailResult = notificationService.sendEmail(rule.getEmailRecipients(), title, content);
            status.append("email: ").append(emailResult);

            record.setNotificationStatus(determineStatus(whResult, emailResult));
            record.setNotificationDetail(status.toString());
            recordRepository.save(record);

            log.info("告警触发: {}, level={}, count={}, threshold={}",
                    rule.getName(), rule.getLevel(), count, rule.getThreshold());
        }
    }

    private String buildAlertMessage(AlertRule rule, long count) {
        return String.format(
                "## 日志告警\n\n" +
                "**规则**: %s\n" +
                "**级别**: %s\n" +
                "**触发条件**: %d 分钟内出现 %d 条 %s 日志\n" +
                "**实际匹配**: %d 条\n" +
                "**时间**: %s\n",
                rule.getName(), rule.getLevel(),
                rule.getWindowMinutes(), rule.getThreshold(), rule.getLevel(),
                count,
                LocalDateTime.now().toString().replace("T", " ")
        );
    }

    private String determineStatus(String webhookResult, String emailResult) {
        boolean whOk = webhookResult != null && (webhookResult.contains("sent") || webhookResult.contains("跳过"));
        boolean emOk = emailResult != null && (emailResult.contains("sent") || emailResult.contains("跳过"));
        if (whOk && emOk) return "both_sent";
        if (whOk) return "webhook_sent";
        if (emOk) return "email_sent";
        return "failed";
    }
}
