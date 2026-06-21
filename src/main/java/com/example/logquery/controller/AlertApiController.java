package com.example.logquery.controller;

import com.example.logquery.dto.ApiResponse;
import com.example.logquery.entity.AlertRecord;
import com.example.logquery.entity.AlertRule;
import com.example.logquery.service.AlertService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertApiController {

    private final AlertService alertService;

    public AlertApiController(AlertService alertService) {
        this.alertService = alertService;
    }

    // ==================== 告警规则 CRUD ====================

    @GetMapping("/rules")
    public ApiResponse<List<AlertRule>> listRules() {
        return ApiResponse.success(alertService.listRules());
    }

    @GetMapping("/rules/{id}")
    public ApiResponse<AlertRule> getRule(@PathVariable Long id) {
        return ApiResponse.success(alertService.getRule(id));
    }

    @PostMapping("/rules")
    public ApiResponse<AlertRule> createRule(@RequestBody AlertRule rule) {
        AlertRule created = alertService.createRule(rule);
        return ApiResponse.success("告警规则创建成功", created);
    }

    @PutMapping("/rules/{id}")
    public ApiResponse<AlertRule> updateRule(@PathVariable Long id, @RequestBody AlertRule rule) {
        AlertRule updated = alertService.updateRule(id, rule);
        return ApiResponse.success("告警规则更新成功", updated);
    }

    @DeleteMapping("/rules/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        alertService.deleteRule(id);
        return ApiResponse.success("告警规则已删除", null);
    }

    // ==================== 告警历史 ====================

    @GetMapping("/records")
    public ApiResponse<Map<String, Object>> listRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AlertRecord> result = alertService.listRecords(page, Math.min(size, 100));
        Map<String, Object> data = Map.of(
                "content", result.getContent(),
                "totalPages", result.getTotalPages(),
                "totalElements", result.getTotalElements(),
                "currentPage", result.getNumber()
        );
        return ApiResponse.success(data);
    }

    // ==================== 24h 统计 ====================

    @GetMapping("/count")
    public ApiResponse<Long> count24h() {
        long count = alertService.count24h();
        return ApiResponse.success(count);
    }

    // ==================== 手动触发检查 ====================

    @PostMapping("/evaluate")
    public ApiResponse<String> manualEvaluate() {
        alertService.evaluateRules();
        return ApiResponse.success("告警检查已执行");
    }
}
