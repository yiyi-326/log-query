package com.example.logquery.controller;

import com.example.logquery.dto.ApiResponse;
import com.example.logquery.entity.Application;
import com.example.logquery.service.ApplicationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/apps")
public class ApplicationApiController {

    private final ApplicationService appService;

    public ApplicationApiController(ApplicationService appService) {
        this.appService = appService;
    }

    @GetMapping
    public ApiResponse<List<Application>> list() {
        return ApiResponse.success(appService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Application> get(@PathVariable Long id) {
        return appService.getById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("应用不存在"));
    }

    @PostMapping
    public ApiResponse<Application> create(@RequestBody Application app) {
        Application created = appService.create(app);
        return ApiResponse.success("应用创建成功", created);
    }

    @PutMapping("/{id}")
    public ApiResponse<Application> update(@PathVariable Long id, @RequestBody Application app) {
        Application updated = appService.update(id, app);
        return ApiResponse.success("应用更新成功", updated);
    }

    @PutMapping("/{id}/regenerate-key")
    public ApiResponse<Application> regenerateKey(@PathVariable Long id) {
        Application updated = appService.regenerateApiKey(id);
        return ApiResponse.success("API Key 已重新生成", updated);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        appService.delete(id);
        return ApiResponse.success("应用已删除", null);
    }
}
