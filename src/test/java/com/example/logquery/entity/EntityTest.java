package com.example.logquery.entity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EntityTest {

    @Nested
    class LogEntryTests {

        @Test
        void shouldSetCreatedAtOnPersist() {
            LogEntry e = new LogEntry();
            e.onCreate();
            assertNotNull(e.getCreatedAt());
        }

        @Test
        void shouldNotOverwriteExistingCreatedAt() {
            LocalDateTime fixed = LocalDateTime.of(2026, 1, 1, 0, 0);
            LogEntry e = new LogEntry();
            e.setCreatedAt(fixed);
            e.onCreate();
            assertEquals(fixed, e.getCreatedAt());
        }

        @Test
        void shouldConstructWithFields() {
            LocalDateTime now = LocalDateTime.now();
            LogEntry e = new LogEntry(now, "ERROR", "app", "boom");

            assertEquals(now, e.getTimestamp());
            assertEquals("ERROR", e.getLevel());
            assertEquals("app", e.getSource());
            assertEquals("boom", e.getMessage());
        }
    }

    @Nested
    class ApplicationTests {

        @Test
        void shouldSetCreatedAtOnPersist() {
            Application app = new Application();
            app.onCreate();
            assertNotNull(app.getCreatedAt());
        }

        @Test
        void shouldConstructWithNameAndDescription() {
            Application app = new Application("test", "a test app");

            assertEquals("test", app.getName());
            assertEquals("a test app", app.getDescription());
        }

        @Test
        void shouldDefaultToEnabled() {
            Application app = new Application();
            assertTrue(app.isEnabled());
        }
    }

    @Nested
    class AlertRuleTests {

        @Test
        void shouldSetCreatedAtOnPersist() {
            AlertRule rule = new AlertRule();
            rule.onCreate();
            assertNotNull(rule.getCreatedAt());
        }

        @Test
        void shouldDefaultToEnabled() {
            AlertRule rule = new AlertRule();
            assertTrue(rule.isEnabled());
        }
    }

    @Nested
    class AlertRecordTests {

        @Test
        void shouldSetTriggeredAtOnPersist() {
            AlertRecord alertRec = new AlertRecord();
            alertRec.onCreate();
            assertNotNull(alertRec.getTriggeredAt());
        }

        @Test
        void shouldConstructWithAlertDetails() {
            AlertRecord alertRec = new AlertRecord(1L, "Error Spike", "ERROR",
                    100L, 50, 10);

            assertEquals(1L, alertRec.getRuleId());
            assertEquals("Error Spike", alertRec.getRuleName());
            assertEquals("ERROR", alertRec.getLevel());
            assertEquals(100L, alertRec.getMatchCount());
            assertEquals(50, alertRec.getThreshold());
            assertEquals(10, alertRec.getWindowMinutes());
            assertEquals("pending", alertRec.getNotificationStatus());
            assertNotNull(alertRec.getTriggeredAt());
        }
    }
}
