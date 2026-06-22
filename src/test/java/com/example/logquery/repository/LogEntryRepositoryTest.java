package com.example.logquery.repository;

import com.example.logquery.entity.LogEntry;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class LogEntryRepositoryTest {

    @Autowired
    private LogEntryRepository repository;

    @Nested
    class Count {

        @Test
        void shouldCountByTimestampAfter() {
            repository.save(new LogEntry(LocalDateTime.now(), "INFO", "s", "m"));
            repository.save(new LogEntry(LocalDateTime.now().minusDays(10), "INFO", "s", "m"));

            long count = repository.countByTimestampAfter(LocalDateTime.now().minusDays(1));

            assertEquals(1, count);
        }

        @Test
        void shouldCountByTimestampAfterAndAppId() {
            LogEntry e1 = new LogEntry(LocalDateTime.now(), "INFO", "s", "m");
            e1.setAppId(1L);
            LogEntry e2 = new LogEntry(LocalDateTime.now(), "INFO", "s", "m");
            e2.setAppId(2L);
            repository.saveAll(List.of(e1, e2));

            long count = repository.countByTimestampAfterAndAppId(
                    LocalDateTime.now().minusDays(1), 1L);

            assertEquals(1, count);
        }

        @Test
        void shouldCountByLevel() {
            repository.save(new LogEntry(LocalDateTime.now(), "ERROR", "s", "err"));
            repository.save(new LogEntry(LocalDateTime.now(), "INFO", "s", "info"));
            repository.save(new LogEntry(LocalDateTime.now(), "WARN", "s", "warn"));

            List<Object[]> rows = repository.countByLevel(null);

            assertEquals(3, rows.size());
        }

        @Test
        void shouldCountByLevelForApp() {
            LogEntry e1 = new LogEntry(LocalDateTime.now(), "ERROR", "s", "m");
            e1.setAppId(1L);
            LogEntry e2 = new LogEntry(LocalDateTime.now(), "ERROR", "s", "m");
            e2.setAppId(2L);
            repository.saveAll(List.of(e1, e2));

            List<Object[]> rows = repository.countByLevel(1L);

            assertEquals(1, rows.size());
            assertEquals("ERROR", rows.get(0)[0]);
            assertEquals(1L, rows.get(0)[1]);
        }
    }

    @Nested
    class Find {

        @Test
        void shouldFindByLevelOrderByTimestampDesc() {
            LogEntry older = new LogEntry(LocalDateTime.now().minusHours(1), "ERROR", "s", "older");
            LogEntry newer = new LogEntry(LocalDateTime.now(), "ERROR", "s", "newer");
            repository.saveAll(List.of(older, newer));

            List<LogEntry> result = repository.findByLevelOrderByTimestampDesc(
                    "ERROR", PageRequest.of(0, 10));

            assertEquals(2, result.size());
            assertTrue(result.get(0).getTimestamp().isAfter(result.get(1).getTimestamp()));
        }

        @Test
        void shouldFindRecentErrors() {
            LogEntry e = new LogEntry(LocalDateTime.now(), "ERROR", "app", "msg");
            e.setAppId(1L);
            repository.save(e);

            List<LogEntry> result = repository.findRecentErrors(
                    "ERROR", 1L, PageRequest.of(0, 10));

            assertEquals(1, result.size());
            assertEquals("ERROR", result.get(0).getLevel());
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteByTimestampBefore() {
            repository.save(new LogEntry(LocalDateTime.now().minusDays(10), "INFO", "s", "old"));
            repository.save(new LogEntry(LocalDateTime.now(), "INFO", "s", "new"));

            repository.deleteByTimestampBefore(LocalDateTime.now().minusDays(5));

            assertEquals(1, repository.count());
        }
    }
}
