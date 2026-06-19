package com.example.logquery.controller;

import com.example.logquery.dto.ApiResponse;
import com.example.logquery.dto.LogQueryRequest;
import com.example.logquery.dto.LogStatsResponse;
import com.example.logquery.entity.LogEntry;
import com.example.logquery.service.LogImportService;
import com.example.logquery.service.LogService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/logs")
public class LogApiController {

    private final LogService logService;
    private final LogImportService importService;

    public LogApiController(LogService logService, LogImportService importService) {
        this.logService = logService;
        this.importService = importService;
    }

    @PostMapping
    public ApiResponse<List<LogEntry>> ingest(@RequestBody List<LogEntry> entries) {
        if (entries.isEmpty()) {
            return ApiResponse.error("日志列表不能为空");
        }
        List<LogEntry> saved = logService.saveBatch(entries);
        return ApiResponse.success("成功接收 " + saved.size() + " 条日志", saved);
    }

    @PostMapping("/single")
    public ApiResponse<LogEntry> ingestSingle(@RequestBody LogEntry entry) {
        LogEntry saved = logService.save(entry);
        return ApiResponse.success(saved);
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> query(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> keywords,
            @RequestParam(required = false, defaultValue = "OR") String keywordLogic,
            @RequestParam(required = false) String regex,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        LogQueryRequest req = new LogQueryRequest();
        req.setKeyword(keyword);
        req.setKeywords(keywords);
        req.setKeywordLogic(keywordLogic);
        req.setRegex(regex);
        req.setLevel(level);
        req.setSource(source);
        req.setStartTime(startTime);
        req.setEndTime(endTime);
        req.setPage(page);
        req.setSize(Math.min(size, 100));

        Page<LogEntry> result = logService.query(req);
        Map<String, Object> data = Map.of(
                "content", result.getContent(),
                "totalPages", result.getTotalPages(),
                "totalElements", result.getTotalElements(),
                "currentPage", result.getNumber(),
                "size", result.getSize()
        );
        return ApiResponse.success(data);
    }

    @GetMapping("/export")
    public ResponseEntity<?> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> keywords,
            @RequestParam(required = false, defaultValue = "OR") String keywordLogic,
            @RequestParam(required = false) String regex,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "csv") String format) {

        LogQueryRequest req = new LogQueryRequest();
        req.setKeyword(keyword);
        req.setKeywords(keywords);
        req.setKeywordLogic(keywordLogic);
        req.setRegex(regex);
        req.setLevel(level);
        req.setSource(source);
        req.setStartTime(startTime);
        req.setEndTime(endTime);
        req.setPage(0);
        req.setSize(Integer.MAX_VALUE);

        List<LogEntry> logs = logService.queryAll(req);

        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=logs-export.json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.success(logs));
        } else {
            String csv = logService.exportCsv(logs);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=logs-export.csv")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csv);
        }
    }

    @GetMapping("/errors/recent")
    public ApiResponse<List<LogEntry>> recentErrors(
            @RequestParam(defaultValue = "20") int count) {
        return ApiResponse.success(logService.getRecentErrors(Math.min(count, 200)));
    }

    @GetMapping("/stats")
    public ApiResponse<LogStatsResponse> stats() {
        return ApiResponse.success(logService.getStats());
    }

    @PostMapping("/import")
    public ApiResponse<Map<String, Object>> importFile(@RequestParam("file") MultipartFile file) {
        try {
            List<LogEntry> entries = importService.parse(file);
            if (entries.isEmpty()) {
                return ApiResponse.error("未能从文件中解析出有效日志");
            }
            List<LogEntry> saved = logService.saveBatch(entries);
            Map<String, Object> data = Map.of(
                    "parsed", entries.size(),
                    "imported", saved.size()
            );
            return ApiResponse.success("成功导入 " + saved.size() + " 条日志", data);
        } catch (Exception e) {
            return ApiResponse.error("文件解析失败: " + e.getMessage());
        }
    }

    @DeleteMapping
    public ApiResponse<Map<String, Object>> delete(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beforeTime) {
        if (beforeTime == null) {
            beforeTime = LocalDateTime.now().minusDays(7);
        }
        long deleted = logService.deleteBefore(beforeTime);
        return ApiResponse.success("删除了 " + deleted + " 条日志", Map.of("deleted", deleted));
    }
}
