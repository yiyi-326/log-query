package com.example.logquery.service;

import com.example.logquery.entity.AlertRecord;
import com.example.logquery.entity.AlertRule;
import com.example.logquery.entity.LogEntry;
import com.example.logquery.repository.AlertRecordRepository;
import com.example.logquery.repository.AlertRuleRepository;
import com.example.logquery.repository.LogEntryRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRuleRepository ruleRepository;
    @Mock
    private AlertRecordRepository recordRepository;
    @Mock
    private LogEntryRepository logEntryRepository;
    @Mock
    private NotificationService notificationService;

    private AlertService initService() {
        return new AlertService(ruleRepository, recordRepository, logEntryRepository, notificationService);
    }

    @Nested
    class RuleCrud {

        @Test
        void shouldListAllRules() {
            when(ruleRepository.findAll()).thenReturn(List.of(new AlertRule(), new AlertRule()));

            List<AlertRule> rules = initService().listRules();

            assertEquals(2, rules.size());
        }

        @Test
        void shouldGetRuleById() {
            AlertRule rule = new AlertRule();
            rule.setId(1L);
            rule.setName("test-rule");
            when(ruleRepository.findById(1L)).thenReturn(Optional.of(rule));

            AlertRule result = initService().getRule(1L);

            assertEquals("test-rule", result.getName());
        }

        @Test
        void shouldThrowWhenRuleNotFound() {
            when(ruleRepository.findById(999L)).thenReturn(Optional.empty());
            AlertService service = initService();

            assertThrows(IllegalArgumentException.class, () -> service.getRule(999L));
        }

        @Test
        void shouldCreateRule() {
            AlertRule rule = new AlertRule();
            rule.setName("high-error");
            rule.setLevel("ERROR");
            rule.setThreshold(10);
            rule.setWindowMinutes(5);
            when(ruleRepository.save(rule)).thenReturn(rule);

            AlertRule created = initService().createRule(rule);

            assertNotNull(created);
            verify(ruleRepository).save(rule);
        }

        @Test
        void shouldUpdateRule() {
            AlertRule existing = new AlertRule();
            existing.setId(1L);
            existing.setName("old");

            when(ruleRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(ruleRepository.save(existing)).thenReturn(existing);

            AlertRule update = new AlertRule();
            update.setName("new-name");
            update.setLevel("ERROR");
            update.setThreshold(5);
            update.setWindowMinutes(10);
            update.setWebhookUrl("https://hook.example.com");
            update.setEmailRecipients("a@b.com");
            update.setEnabled(false);

            AlertRule result = initService().updateRule(1L, update);

            assertEquals("new-name", result.getName());
            assertEquals("ERROR", result.getLevel());
            assertEquals(5, result.getThreshold());
            assertEquals(10, result.getWindowMinutes());
            assertEquals("https://hook.example.com", result.getWebhookUrl());
            assertEquals("a@b.com", result.getEmailRecipients());
            assertFalse(result.isEnabled());
        }

        @Test
        void shouldDeleteRule() {
            when(ruleRepository.existsById(1L)).thenReturn(true);

            initService().deleteRule(1L);

            verify(ruleRepository).deleteById(1L);
        }

        @Test
        void shouldThrowWhenDeletingNonExistentRule() {
            when(ruleRepository.existsById(999L)).thenReturn(false);
            AlertService service = initService();

            assertThrows(IllegalArgumentException.class, () -> service.deleteRule(999L));
        }
    }

    @Nested
    class History {

        @Test
        void shouldListRecordsPaginated() {
            Page<AlertRecord> page = new PageImpl<>(List.of(new AlertRecord(), new AlertRecord()));
            when(recordRepository.findAllByOrderByTriggeredAtDesc(any(PageRequest.class)))
                    .thenReturn(page);

            Page<AlertRecord> result = initService().listRecords(0, 20);

            assertEquals(2, result.getContent().size());
        }

        @Test
        void shouldCount24hAlerts() {
            when(recordRepository.countByTriggeredAtAfter(any(LocalDateTime.class)))
                    .thenReturn(5L);

            long count = initService().count24h();

            assertEquals(5L, count);
        }
    }

    @Nested
    class Evaluate {

        @Test
        void shouldTriggerAlertWhenThresholdExceeded() {
            AlertRule rule = new AlertRule();
            rule.setId(1L);
            rule.setName("error-spike");
            rule.setLevel("ERROR");
            rule.setThreshold(5);
            rule.setWindowMinutes(10);
            rule.setWebhookUrl("https://hook.example.com");
            rule.setEmailRecipients("admin@example.com");

            when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
            when(recordRepository.existsByRuleIdAndTriggeredAtAfter(eq(1L), any()))
                    .thenReturn(false); // no prior alert in window
            when(logEntryRepository.count(ArgumentMatchers.<Specification<LogEntry>>any())).thenReturn(8L);
            when(notificationService.sendWebhook(anyString(), anyString(), anyString()))
                    .thenReturn("webhook_sent");
            when(notificationService.sendEmail(anyString(), anyString(), anyString()))
                    .thenReturn("email_sent");

            initService().evaluateRules();

            verify(recordRepository).save(any(AlertRecord.class));
        }

        @Test
        void shouldNotTriggerWhenBelowThreshold() {
            AlertRule rule = new AlertRule();
            rule.setId(1L);
            rule.setName("low-threshold");
            rule.setLevel("WARN");
            rule.setThreshold(100);
            rule.setWindowMinutes(10);

            when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
            when(recordRepository.existsByRuleIdAndTriggeredAtAfter(eq(1L), any()))
                    .thenReturn(false);
            when(logEntryRepository.count(ArgumentMatchers.<Specification<LogEntry>>any())).thenReturn(3L);

            initService().evaluateRules();

            verify(recordRepository, never()).save(any());
        }

        @Test
        void shouldNotTriggerDuplicateAlertInSameWindow() {
            AlertRule rule = new AlertRule();
            rule.setId(1L);
            rule.setName("dedup-test");
            rule.setLevel("ERROR");
            rule.setThreshold(1);
            rule.setWindowMinutes(10);

            when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
            when(recordRepository.existsByRuleIdAndTriggeredAtAfter(eq(1L), any()))
                    .thenReturn(true); // already alerted

            initService().evaluateRules();

            verify(logEntryRepository, never()).count(ArgumentMatchers.<Specification<LogEntry>>any());
        }

        @Test
        void shouldSkipDisabledRules() {
            when(ruleRepository.findByEnabledTrue()).thenReturn(List.of());

            initService().evaluateRules();

            verify(logEntryRepository, never()).count(ArgumentMatchers.<Specification<LogEntry>>any());
        }

        @Test
        void shouldContinueEvaluationOnError() {
            AlertRule goodRule = new AlertRule();
            goodRule.setId(1L);
            goodRule.setName("good");
            goodRule.setLevel("ERROR");
            goodRule.setThreshold(5);
            goodRule.setWindowMinutes(10);

            AlertRule badRule = new AlertRule();
            badRule.setId(2L);
            badRule.setName("bad");
            badRule.setLevel("ERROR");
            badRule.setThreshold(5);
            badRule.setWindowMinutes(10);

            when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(goodRule, badRule));
            when(recordRepository.existsByRuleIdAndTriggeredAtAfter(eq(1L), any())).thenReturn(true);
            when(recordRepository.existsByRuleIdAndTriggeredAtAfter(eq(2L), any()))
                    .thenThrow(new IllegalStateException("DB error"));

            assertDoesNotThrow(() -> initService().evaluateRules());
        }
    }
}
