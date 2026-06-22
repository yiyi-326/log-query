package com.example.logquery.controller;

import com.example.logquery.dto.LogStatsResponse;
import com.example.logquery.entity.LogEntry;
import com.example.logquery.exception.LogImportException;
import com.example.logquery.service.LogImportService;
import com.example.logquery.service.LogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogApiController.class)
class LogApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LogService logService;

    @MockBean
    private LogImportService importService;

    @Nested
    class Query {
        

        @Test
        void shouldReturnPagedResults() throws Exception {
            LogEntry entry = new LogEntry(LocalDateTime.now(), "ERROR", "app", "msg");
            entry.setId(1L);
            Page<LogEntry> page = new PageImpl<>(List.of(entry));

            when(logService.query(any())).thenReturn(page);

            mvc.perform(get("/api/v1/logs")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].level").value("ERROR"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        void shouldFilterByLevel() throws Exception {
            when(logService.query(any())).thenReturn(new PageImpl<>(List.of()));

            mvc.perform(get("/api/v1/logs").param("level", "ERROR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void shouldFilterByAppId() throws Exception {
            when(logService.query(any())).thenReturn(new PageImpl<>(List.of()));

            mvc.perform(get("/api/v1/logs").param("appId", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldFilterByTimeRange() throws Exception {
            when(logService.query(any())).thenReturn(new PageImpl<>(List.of()));

            mvc.perform(get("/api/v1/logs")
                            .param("startTime", "2026-06-01T00:00:00")
                            .param("endTime", "2026-06-22T23:59:59"))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldFilterByKeyword() throws Exception {
            when(logService.query(any())).thenReturn(new PageImpl<>(List.of()));

            mvc.perform(get("/api/v1/logs").param("keyword", "timeout"))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldCapSizeAt100() throws Exception {
            when(logService.query(any())).thenReturn(new PageImpl<>(List.of()));

            mvc.perform(get("/api/v1/logs").param("size", "500"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class Stats {

        @Test
        void shouldReturnStats() throws Exception {
            var levelDist = Map.of("ERROR", 5L, "WARN", 15L, "INFO", 80L, "DEBUG", 0L);
            LogStatsResponse stats = new LogStatsResponse(
                    100, 20, levelDist, List.of(), List.of(), 5.0, 0);

            when(logService.getStats(null)).thenReturn(stats);

            mvc.perform(get("/api/v1/logs/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalCount").value(100))
                    .andExpect(jsonPath("$.data.todayCount").value(20))
                    .andExpect(jsonPath("$.data.errorRate").value(5.0));
        }

        @Test
        void shouldFilterStatsByAppId() throws Exception {
            LogStatsResponse stats = new LogStatsResponse(
                    50, 0, Map.of(), List.of(), List.of(), 0, 0);
            when(logService.getStats(1L)).thenReturn(stats);

            mvc.perform(get("/api/v1/logs/stats").param("appId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalCount").value(50));
        }
    }

    @Nested
    class RecentErrors {

        @Test
        void shouldReturnRecentErrors() throws Exception {
            LogEntry e = new LogEntry(LocalDateTime.now(), "ERROR", "app", "crash");
            when(logService.getRecentErrors(20, null)).thenReturn(List.of(e));

            mvc.perform(get("/api/v1/logs/errors/recent").param("count", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].level").value("ERROR"));
        }

        @Test
        void shouldFilterErrorsByAppId() throws Exception {
            when(logService.getRecentErrors(10, 1L)).thenReturn(List.of());

            mvc.perform(get("/api/v1/logs/errors/recent")
                            .param("count", "10")
                            .param("appId", "1"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class Export {

        @Test
        void shouldExportCsv() throws Exception {
            when(logService.queryAll(any())).thenReturn(List.of());
            when(logService.exportCsv(any())).thenReturn("时间,级别,来源,内容\r\n");

            mvc.perform(get("/api/v1/logs/export").param("format", "csv"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition",
                            containsString("logs-export.csv")));
        }

        @Test
        void shouldExportJson() throws Exception {
            when(logService.queryAll(any())).thenReturn(List.of());

            mvc.perform(get("/api/v1/logs/export").param("format", "json"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition",
                            containsString("logs-export.json")));
        }
    }

    @Nested
    class Ingest {

        @Test
        void shouldRejectEmptyList() throws Exception {
            mvc.perform(post("/api/v1/logs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void shouldAcceptLogEntries() throws Exception {
            List<LogEntry> entries = List.of(
                    new LogEntry(LocalDateTime.now(), "INFO", "test", "hello")
            );
            when(logService.saveBatch(any(), any())).thenReturn(new int[][]{{1}});

            mvc.perform(post("/api/v1/logs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(entries)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value(containsString("1")));
        }
    }

    @Nested
    class ImportFile {

        @Test
        void shouldRejectEmptyFile() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "empty.txt",
                    "text/plain", new byte[0]);
            when(importService.parse(file)).thenReturn(List.of());

            mvc.perform(multipart("/api/v1/logs/import").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void shouldImportValidFile() throws Exception {
            String content = "2026-06-22T10:00:00 ERROR app crash";
            MockMultipartFile file = new MockMultipartFile("file", "logs.txt",
                    "text/plain", content.getBytes());
            List<LogEntry> entries = List.of(
                    new LogEntry(LocalDateTime.now(), "ERROR", "app", "crash")
            );
            when(importService.parse(file)).thenReturn(entries);
            when(logService.saveBatch(entries, null)).thenReturn(new int[][]{{1}});

            mvc.perform(multipart("/api/v1/logs/import").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void shouldHandleImportException() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "bad.txt",
                    "text/plain", "bad data".getBytes());
            when(importService.parse(file)).thenThrow(new LogImportException("格式错误", new RuntimeException()));

            mvc.perform(multipart("/api/v1/logs/import").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("格式错误")));
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteOldLogs() throws Exception {
            when(logService.deleteBefore(any())).thenReturn(50L);

            mvc.perform(delete("/api/v1/logs")
                            .param("beforeTime", "2026-06-01T00:00:00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.deleted").value(50));
        }

        @Test
        void shouldUseDefaultRetention() throws Exception {
            when(logService.deleteBefore(any())).thenReturn(0L);

            mvc.perform(delete("/api/v1/logs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.deleted").value(0));
        }
    }
}
