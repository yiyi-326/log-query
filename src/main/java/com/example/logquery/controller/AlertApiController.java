package com.example.logquery.controller;

import com.example.logquery.dto.AlertRecordDTO;
import com.example.logquery.dto.AlertRuleDTO;
import com.example.logquery.dto.ApiResponse;
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
    public ApiResponse<List<AlertRuleDTO>> listRules() {
        List<AlertRuleDTO> rules = alertService.listRules().stream()
                .map(AlertRuleDTO::from)
                .toList();
        return ApiResponse.success(rules);
    }

    @GetMapping("/rules/{id}")
    public ApiResponse<AlertRuleDTO> getRule(@PathVariable Long id) {
        return ApiResponse.success(AlertRuleDTO.from(alertService.getRule(id)));
    }

    @PostMapping("/rules")
    public ApiResponse<AlertRuleDTO> createRule(@RequestBody AlertRuleDTO dto) {
        var created = alertService.createRule(dto.toEntity());
        return ApiResponse.success("告警规则创建成功", AlertRuleDTO.from(created));
    }

    @PutMapping("/rules/{id}")
    public ApiResponse<AlertRuleDTO> updateRule(@PathVariable Long id, @RequestBody AlertRuleDTO dto) {
        var updated = alertService.updateRule(id, dto.toEntity());
        return ApiResponse.success("告警规则更新成功", AlertRuleDTO.from(updated));
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
        Page<AlertRecordDTO> result = alertService.listRecords(page, Math.min(size, 100))
                .map(AlertRecordDTO::from);
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
