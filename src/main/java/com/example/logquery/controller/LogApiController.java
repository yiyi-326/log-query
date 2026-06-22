package com.example.logquery.controller;

import com.example.logquery.dto.ApiResponse;
import com.example.logquery.dto.LogEntryDTO;
import com.example.logquery.dto.LogQueryRequest;
import com.example.logquery.dto.LogStatsResponse;
import com.example.logquery.exception.LogImportException;
import com.example.logquery.service.LogImportService;
import com.example.logquery.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
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

    private static final String APP_ID_ATTR = "appId";

    private final LogService logService;
    private final LogImportService importService;

    public LogApiController(LogService logService, LogImportService importService) {
        this.logService = logService;
        this.importService = importService;
    }

    @PostMapping
    public ApiResponse<Integer> ingest(@RequestBody List<LogEntryDTO> dtos,
                                       HttpServletRequest request) {
        if (dtos.isEmpty()) {
            return ApiResponse.error("日志列表不能为空");
        }
        Long appId = (Long) request.getAttribute(APP_ID_ATTR);
        var entries = dtos.stream().map(LogEntryDTO::toEntity).toList();
        logService.saveBatch(entries, appId);
        return ApiResponse.success("成功接收 " + entries.size() + " 条日志", entries.size());
    }

    @PostMapping("/single")
    public ApiResponse<LogEntryDTO> ingestSingle(@RequestBody LogEntryDTO dto,
                                                  HttpServletRequest request) {
        Long appId = (Long) request.getAttribute(APP_ID_ATTR);
        var entry = dto.toEntity();
        entry.setAppId(appId);
        var saved = logService.save(entry);
        return ApiResponse.success(LogEntryDTO.from(saved));
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> query(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> keywords,
            @RequestParam(required = false, defaultValue = "OR") String keywordLogic,
            @RequestParam(required = false) String regex,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Long appId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        LogQueryRequest req = new LogQueryRequest();
        applyTextFilters(req, keyword, keywords, keywordLogic, regex);
        applyScopeFilters(req, level, source, appId, startTime, endTime);
        req.setPage(page);
        req.setSize(Math.min(size, 100));

        Page<LogEntryDTO> result = logService.query(req).map(LogEntryDTO::from);
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
    public ResponseEntity<Object> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> keywords,
            @RequestParam(required = false, defaultValue = "OR") String keywordLogic,
            @RequestParam(required = false) String regex,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Long appId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "csv") String format) {

        LogQueryRequest req = new LogQueryRequest();
        applyTextFilters(req, keyword, keywords, keywordLogic, regex);
        applyScopeFilters(req, level, source, appId, startTime, endTime);
        req.setPage(0);
        req.setSize(Integer.MAX_VALUE);

        var logs = logService.queryAll(req);

        if ("json".equalsIgnoreCase(format)) {
            var dtos = logs.stream().map(LogEntryDTO::from).toList();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=logs-export.json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.success(dtos));
        } else {
            String csv = logService.exportCsv(logs);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=logs-export.csv")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csv);
        }
    }

    private void applyTextFilters(LogQueryRequest req, String keyword,
            List<String> keywords, String keywordLogic, String regex) {
        req.setKeyword(keyword);
        req.setKeywords(keywords);
        req.setKeywordLogic(keywordLogic);
        req.setRegex(regex);
    }

    private void applyScopeFilters(LogQueryRequest req, String level, String source,
            Long appId, LocalDateTime startTime, LocalDateTime endTime) {
        req.setLevel(level);
        req.setSource(source);
        req.setAppId(appId);
        req.setStartTime(startTime);
        req.setEndTime(endTime);
    }

    @GetMapping("/errors/recent")
    public ApiResponse<List<LogEntryDTO>> recentErrors(
            @RequestParam(defaultValue = "20") int count,
            @RequestParam(required = false) Long appId) {
        var dtos = logService.getRecentErrors(Math.min(count, 200), appId)
                .stream().map(LogEntryDTO::from).toList();
        return ApiResponse.success(dtos);
    }

    @GetMapping("/stats")
    public ApiResponse<LogStatsResponse> stats(
            @RequestParam(required = false) Long appId) {
        return ApiResponse.success(logService.getStats(appId));
    }

    @PostMapping("/import")
    public ApiResponse<Map<String, Object>> importFile(@RequestParam("file") MultipartFile file,
                                                       @RequestParam(required = false) Long appId,
                                                       HttpServletRequest request) {
        try {
            var entries = importService.parse(file);
            if (entries.isEmpty()) {
                return ApiResponse.error("未能从文件中解析出有效日志");
            }
            Long effectiveAppId = appId != null ? appId : (Long) request.getAttribute(APP_ID_ATTR);
            logService.saveBatch(entries, effectiveAppId);
            Map<String, Object> data = Map.of(
                    "parsed", entries.size(),
                    "imported", entries.size()
            );
            return ApiResponse.success("成功导入 " + entries.size() + " 条日志", data);
        } catch (LogImportException e) {
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
