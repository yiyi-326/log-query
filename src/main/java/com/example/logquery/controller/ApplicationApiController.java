package com.example.logquery.controller;

import com.example.logquery.dto.ApiResponse;
import com.example.logquery.dto.ApplicationDTO;
import com.example.logquery.service.ApplicationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/apps")
public class ApplicationApiController {

    private final ApplicationService appService;

    public ApplicationApiController(ApplicationService appService) {
        this.appService = appService;
    }

    @GetMapping
    public ApiResponse<List<ApplicationDTO>> list() {
        List<ApplicationDTO> apps = appService.listAll().stream()
                .map(ApplicationDTO::from)
                .toList();
        return ApiResponse.success(apps);
    }

    @GetMapping("/{id}")
    public ApiResponse<ApplicationDTO> get(@PathVariable Long id) {
        return appService.getById(id)
                .map(ApplicationDTO::from)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("应用不存在"));
    }

    @PostMapping
    public ApiResponse<ApplicationDTO> create(@RequestBody ApplicationDTO dto) {
        var created = appService.create(dto.toEntity());
        return ApiResponse.success("应用创建成功", ApplicationDTO.from(created));
    }

    @PutMapping("/{id}")
    public ApiResponse<ApplicationDTO> update(@PathVariable Long id, @RequestBody ApplicationDTO dto) {
        var updated = appService.update(id, dto.toEntity());
        return ApiResponse.success("应用更新成功", ApplicationDTO.from(updated));
    }

    @PutMapping("/{id}/regenerate-key")
    public ApiResponse<ApplicationDTO> regenerateKey(@PathVariable Long id) {
        var updated = appService.regenerateApiKey(id);
        return ApiResponse.success("API Key 已重新生成", ApplicationDTO.from(updated));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        appService.delete(id);
        return ApiResponse.success("应用已删除", null);
    }
}
