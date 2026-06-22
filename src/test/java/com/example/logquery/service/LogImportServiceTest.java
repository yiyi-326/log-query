package com.example.logquery.service;

import com.example.logquery.entity.LogEntry;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogImportServiceTest {

    private final LogImportService service = new LogImportService();

    @Nested
    class JsonLines {

        @Test
        void shouldParseSingleJsonLine() {
            String json = "{\"timestamp\":\"2026-06-22T10:30:00\",\"level\":\"ERROR\",\"source\":\"order-service\",\"message\":\"connection timeout\"}";
            MockMultipartFile file = new MockMultipartFile("file", "test.jsonl",
                    "text/plain", json.getBytes(StandardCharsets.UTF_8));

            List<LogEntry> entries = service.parse(file);

            assertEquals(1, entries.size());
            LogEntry e = entries.get(0);
            assertEquals("ERROR", e.getLevel());
            assertEquals("order-service", e.getSource());
            assertEquals("connection timeout", e.getMessage());
        }

        @Test
        void shouldParseMultipleJsonLines() {
            String content = """
                    {"timestamp":"2026-06-22T10:30:00","level":"ERROR","source":"app1","message":"err1"}
                    {"timestamp":"2026-06-22T10:31:00","level":"WARN","source":"app2","message":"warn1"}
                    {"timestamp":"2026-06-22T10:32:00","level":"INFO","source":"app3","message":"info1"}
                    """;

            MockMultipartFile file = new MockMultipartFile("file", "test.jsonl",
                    "text/plain", content.getBytes(StandardCharsets.UTF_8));

            List<LogEntry> entries = service.parse(file);

            assertEquals(3, entries.size());
        }

        @Test
        void shouldSkipEmptyLines() {
            String json = "{\"timestamp\":\"2026-06-22T10:30:00\",\"level\":\"ERROR\",\"source\":\"app\",\"message\":\"err\"}\n\n\n";
            MockMultipartFile file = new MockMultipartFile("file", "test.jsonl",
                    "text/plain", json.getBytes(StandardCharsets.UTF_8));

            List<LogEntry> entries = service.parse(file);

            assertEquals(1, entries.size());
        }

        @Test
        void shouldNormalizeLevelToUppercase() {
            String json = "{\"timestamp\":\"2026-06-22T10:30:00\",\"level\":\"error\",\"source\":\"app\",\"message\":\"err\"}";
            MockMultipartFile file = new MockMultipartFile("file", "test.jsonl",
                    "text/plain", json.getBytes(StandardCharsets.UTF_8));

            List<LogEntry> entries = service.parse(file);

            assertEquals("ERROR", entries.get(0).getLevel());
        }

        @Test
        void shouldSetDefaultTimestampWhenMissing() {
            String json = "{\"level\":\"INFO\",\"source\":\"app\",\"message\":\"msg\"}";
            MockMultipartFile file = new MockMultipartFile("file", "test.jsonl",
                    "text/plain", json.getBytes(StandardCharsets.UTF_8));

            List<LogEntry> entries = service.parse(file);

            assertNotNull(entries.get(0).getTimestamp());
        }
    }

    @Nested
    class CommonLogFormat {

        @Test
        void shouldParseStandardLogFormat() {
            String line = "2026-06-17 10:30:00 [ERROR] order-service - connection timeout";
            MockMultipartFile file = new MockMultipartFile("file", "test.log",
                    "text/plain", line.getBytes(StandardCharsets.UTF_8));

            List<LogEntry> entries = service.parse(file);

            assertEquals(1, entries.size());
            LogEntry e = entries.get(0);
            assertEquals("ERROR", e.getLevel());
            assertEquals("order-service", e.getSource());
            assertEquals("connection timeout", e.getMessage());
        }

        @Test
        void shouldParseWithoutSource() {
            String line = "2026-06-17 10:30:00 [WARN] - something happened";
            MockMultipartFile file = new MockMultipartFile("file", "test.log",
                    "text/plain", line.getBytes(StandardCharsets.UTF_8));

            List<LogEntry> entries = service.parse(file);

            assertEquals(1, entries.size());
            assertEquals("WARN", entries.get(0).getLevel());
            assertNotNull(entries.get(0).getTimestamp());
        }

        @Test
        void shouldNormalizeWarningToWarn() {
            String line = "2026-06-17 10:30:00 [WARNING] app - warning message";
            MockMultipartFile file = new MockMultipartFile("file", "test.log",
                    "text/plain", line.getBytes(StandardCharsets.UTF_8));

            List<LogEntry> entries = service.parse(file);

            assertEquals("WARN", entries.get(0).getLevel());
        }

        @Test
        void shouldParseDebugAndTraceLevels() {
            String line = "2026-06-17 10:30:00 [DEBUG] app - debug info";
            MockMultipartFile file = new MockMultipartFile("file", "test.log",
                    "text/plain", line.getBytes(StandardCharsets.UTF_8));

            List<LogEntry> entries = service.parse(file);

            assertEquals("DEBUG", entries.get(0).getLevel());
        }

        @Test
        void shouldParseWithTDateSeparator() {
            String line = "2026-06-17T10:30:00 [ERROR] svc - failed";
            MockMultipartFile file = new MockMultipartFile("file", "test.log",
                    "text/plain", line.getBytes(StandardCharsets.UTF_8));

            List<LogEntry> entries = service.parse(file);

            assertEquals(1, entries.size());
            assertEquals("ERROR", entries.get(0).getLevel());
        }

        @Test
        void shouldHandleUnparseableLines() {
            String line = "this is not a log line at all";
            MockMultipartFile file = new MockMultipartFile("file", "test.log",
                    "text/plain", line.getBytes(StandardCharsets.UTF_8));

            List<LogEntry> entries = service.parse(file);

            assertEquals(0, entries.size());
        }
    }

    @Nested
    class MixedFormat {

        @Test
        void shouldParseMixedJsonAndCommonFormat() {
            String content = """
                    2026-06-17 10:30:00 [ERROR] app1 - error from common format
                    {"timestamp":"2026-06-17T10:31:00","level":"WARN","source":"app2","message":"warn from json"}
                    """;

            MockMultipartFile file = new MockMultipartFile("file", "test.log",
                    "text/plain", content.getBytes(StandardCharsets.UTF_8));

            List<LogEntry> entries = service.parse(file);

            assertEquals(2, entries.size());
            assertEquals("ERROR", entries.get(0).getLevel());
            assertEquals("WARN", entries.get(1).getLevel());
        }
    }

    @Test
    void shouldParseEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.log",
                "text/plain", "".getBytes(StandardCharsets.UTF_8));

        List<LogEntry> entries = service.parse(file);

        assertTrue(entries.isEmpty());
    }
}
