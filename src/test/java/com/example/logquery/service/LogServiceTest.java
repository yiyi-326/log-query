package com.example.logquery.service;

import com.example.logquery.dto.LogQueryRequest;
import com.example.logquery.entity.LogEntry;
import com.example.logquery.repository.LogEntryRepository;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @Mock
    private LogEntryRepository repository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private LogService logService;

    @BeforeEach
    void setUp() {
        logService = new LogService(repository, jdbcTemplate);
    }

    @Nested
    class Save {

        @Test
        void shouldSetTimestampWhenNull() {
            LogEntry entry = new LogEntry(null, "INFO", "test", "msg");
            when(repository.save(any())).thenReturn(entry);

            logService.save(entry);

            assertNotNull(entry.getTimestamp());
        }

        @Test
        void shouldSetDefaultLevelWhenNull() {
            LogEntry entry = new LogEntry(LocalDateTime.now(), null, "test", "msg");
            when(repository.save(any())).thenReturn(entry);

            logService.save(entry);

            assertEquals("INFO", entry.getLevel());
        }

        @Test
        void shouldNotOverwriteExistingValues() {
            LocalDateTime now = LocalDateTime.now();
            LogEntry entry = new LogEntry(now, "ERROR", "app", "crash");
            when(repository.save(any())).thenReturn(entry);

            logService.save(entry);

            assertEquals(now, entry.getTimestamp());
            assertEquals("ERROR", entry.getLevel());
        }
    }

    @Nested
    class BatchSave {

        @Test
        void shouldReturnEmptyForEmptyList() {
            int[][] result = logService.saveBatch(List.of(), 1L);
            assertEquals(0, result.length);
        }

        @Test
        void shouldUseJdbcBatchUpdate() {
            List<LogEntry> entries = List.of(
                    new LogEntry(LocalDateTime.now(), "ERROR", "s1", "m1"),
                    new LogEntry(LocalDateTime.now(), "WARN", "s2", "m2")
            );
            when(jdbcTemplate.batchUpdate(anyString(), eq(entries), eq(500), any()))
                    .thenReturn(new int[][]{{1}, {1}});

            int[][] result = logService.saveBatch(entries, 1L);

            assertEquals(2, result.length);
            verify(jdbcTemplate).batchUpdate(
                    contains("INSERT INTO log_entries"),
                    eq(entries), eq(500), any());
        }

        @Test
        void shouldSetNullAppId() {
            List<LogEntry> entries = List.of(
                    new LogEntry(LocalDateTime.now(), "INFO", "src", "msg")
            );
            when(jdbcTemplate.batchUpdate(anyString(), any(), anyInt(), any()))
                    .thenReturn(new int[][]{{1}});

            int[][] result = logService.saveBatch(entries, null);

            assertEquals(1, result.length);
        }
    }

    @Nested
    class Query {

        @Test
        void shouldFilterByLevel() {
            LogQueryRequest req = new LogQueryRequest();
            req.setLevel("ERROR");

            Page<LogEntry> page = new PageImpl<>(List.of());
            when(repository.findAll(ArgumentMatchers.<Specification<LogEntry>>any(), any(PageRequest.class)))
                    .thenReturn(page);

            Page<LogEntry> result = logService.query(req);

            assertNotNull(result);
            verify(repository).findAll(ArgumentMatchers.<Specification<LogEntry>>any(), any(PageRequest.class));
        }

        @Test
        void shouldFilterByAppId() {
            LogQueryRequest req = new LogQueryRequest();
            req.setAppId(5L);

            when(repository.findAll(ArgumentMatchers.<Specification<LogEntry>>any(), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            Page<LogEntry> result = logService.query(req);

            assertNotNull(result);
        }

        @Test
        void shouldFilterByTimeRange() {
            LogQueryRequest req = new LogQueryRequest();
            req.setStartTime(LocalDateTime.now().minusHours(1));
            req.setEndTime(LocalDateTime.now());

            when(repository.findAll(ArgumentMatchers.<Specification<LogEntry>>any(), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            Page<LogEntry> result = logService.query(req);

            assertNotNull(result);
        }

        @Test
        void shouldApplyKeywordFilter() {
            LogQueryRequest req = new LogQueryRequest();
            req.setKeyword("timeout");

            when(repository.findAll(ArgumentMatchers.<Specification<LogEntry>>any(), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            Page<LogEntry> result = logService.query(req);

            assertNotNull(result);
        }

        @Test
        void shouldApplyMultiKeywordAndLogic() {
            LogQueryRequest req = new LogQueryRequest();
            req.setKeywords(List.of("error", "timeout"));
            req.setKeywordLogic("AND");

            when(repository.findAll(ArgumentMatchers.<Specification<LogEntry>>any(), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            Page<LogEntry> result = logService.query(req);

            assertNotNull(result);
        }

        @Test
        void shouldApplyMultiKeywordOrLogic() {
            LogQueryRequest req = new LogQueryRequest();
            req.setKeywords(List.of("error", "timeout"));
            req.setKeywordLogic("OR");

            when(repository.findAll(ArgumentMatchers.<Specification<LogEntry>>any(), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            Page<LogEntry> result = logService.query(req);

            assertNotNull(result);
        }

        @Test
        void shouldApplyRegexFilter() {
            LogQueryRequest req = new LogQueryRequest();
            req.setRegex("error|timeout");

            when(repository.findAll(ArgumentMatchers.<Specification<LogEntry>>any(), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            Page<LogEntry> result = logService.query(req);

            assertNotNull(result);
        }

        @Test
        void queryAllShouldReturnList() {
            LogQueryRequest req = new LogQueryRequest();
            req.setLevel("INFO");

            when(repository.findAll(ArgumentMatchers.<Specification<LogEntry>>any(), any(org.springframework.data.domain.Sort.class)))
                    .thenReturn(List.of());

            List<LogEntry> result = logService.queryAll(req);

            assertNotNull(result);
        }
    }

    @Nested
    class Stats {

        @Test
        void shouldReturnStatsForAllApps() {
            when(repository.count()).thenReturn(100L);
            when(repository.countByTimestampAfter(any())).thenReturn(10L);
            List<Object[]> levelCounts = Arrays.asList(new Object[][]{
                    {"ERROR", 5L},
                    {"WARN", 10L},
                    {"INFO", 80L},
                    {"DEBUG", 5L}
            });
            when(repository.countByLevel(null)).thenReturn(levelCounts);
            List<Object[]> sourceCounts = Arrays.asList(new Object[][]{
                    {"app1", 50L}
            });
            when(repository.countBySource(null)).thenReturn(sourceCounts);
            List<Object[]> recentCounts = Arrays.asList(new Object[][]{
                    {"ERROR", 2L}
            });
            when(repository.countByLevelSince(any(), isNull())).thenReturn(recentCounts);
            when(repository.findByTimestampAfter(any())).thenReturn(List.of());

            var stats = logService.getStats();

            assertEquals(100L, stats.totalCount());
            assertEquals(10L, stats.todayCount());
            assertEquals(5L, stats.levelDistribution().get("ERROR"));
            assertEquals(5.0, stats.errorRate(), 0.01);
        }

        @Test
        void shouldReturnStatsForSpecificApp() {
            when(repository.count(ArgumentMatchers.<Specification<LogEntry>>any())).thenReturn(50L, 5L);
            List<Object[]> levelCounts = Arrays.asList(new Object[][]{
                    {"ERROR", 3L},
                    {"WARN", 7L},
                    {"INFO", 35L},
                    {"DEBUG", 5L}
            });
            when(repository.countByLevel(1L)).thenReturn(levelCounts);
            when(repository.countBySource(1L)).thenReturn(List.of());
            List<Object[]> recentCounts = Arrays.asList(new Object[][]{
                    {"ERROR", 1L}
            });
            when(repository.countByLevelSince(any(), eq(1L))).thenReturn(recentCounts);
            when(repository.findByTimestampAfterAndAppId(any(), eq(1L))).thenReturn(List.of());

            var stats = logService.getStats(1L);

            assertEquals(50L, stats.totalCount());
            assertEquals(5L, stats.todayCount());
            assertEquals(3L, stats.levelDistribution().get("ERROR"));
            assertEquals(6.0, stats.errorRate(), 0.01);
        }

        @Test
        void shouldCalculateZeroErrorRateWhenNoLogs() {
            when(repository.count()).thenReturn(0L);
            when(repository.countByTimestampAfter(any())).thenReturn(0L);
            when(repository.countByLevel(null)).thenReturn(List.of());
            when(repository.countBySource(null)).thenReturn(List.of());
            when(repository.countByLevelSince(any(), isNull())).thenReturn(List.of());
            when(repository.findByTimestampAfter(any())).thenReturn(List.of());

            var stats = logService.getStats();

            assertEquals(0.0, stats.errorRate(), 0.01);
            assertEquals(0.0, stats.todayErrorRate(), 0.01);
        }
    }

    @Nested
    class CsvExport {

        @Test
        void shouldExportHeaderOnlyWhenEmpty() {
            String csv = logService.exportCsv(List.of());
            assertEquals("时间,级别,来源,内容\r\n", csv);
        }

        @Test
        void shouldExportLogEntries() {
            LogEntry e = new LogEntry(
                    LocalDateTime.of(2026, 6, 22, 10, 30, 0),
                    "ERROR", "my-app", "connection timeout"
            );
            String csv = logService.exportCsv(List.of(e));

            assertTrue(csv.contains("connection timeout"));
            assertTrue(csv.contains("ERROR"));
            assertTrue(csv.contains("my-app"));
            assertTrue(csv.contains("2026-06-22T10:30"));
        }

        @Test
        void shouldEscapeCsvSpecialChars() {
            LogEntry e = new LogEntry(
                    LocalDateTime.now(), "INFO", "test",
                    "value with, comma and \"quotes\""
            );
            String csv = logService.exportCsv(List.of(e));

            assertTrue(csv.contains("\"value with, comma and \"\"quotes\"\"\""));
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteAndReturnCount() {
            when(repository.count()).thenReturn(100L, 20L);

            long deleted = logService.deleteBefore(LocalDateTime.now().minusDays(7));

            assertEquals(80L, deleted);
            verify(repository).deleteByTimestampBefore(any());
        }
    }

    @Nested
    class RecentErrors {

        @Test
        void shouldReturnErrorsForAllApps() {
            List<LogEntry> errors = List.of(
                    new LogEntry(LocalDateTime.now(), "ERROR", "app", "crash")
            );
            when(repository.findByLevelOrderByTimestampDesc(eq("ERROR"), any()))
                    .thenReturn(errors);

            List<LogEntry> result = logService.getRecentErrors(20);

            assertEquals(1, result.size());
        }

        @Test
        void shouldReturnErrorsForSpecificApp() {
            List<LogEntry> errors = List.of(
                    new LogEntry(LocalDateTime.now(), "ERROR", "app", "crash")
            );
            when(repository.findRecentErrors(eq("ERROR"), eq(1L), any()))
                    .thenReturn(errors);

            List<LogEntry> result = logService.getRecentErrors(30, 1L);

            assertEquals(1, result.size());
        }
    }
}
