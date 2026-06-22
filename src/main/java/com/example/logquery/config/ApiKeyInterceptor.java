package com.example.logquery.config;

import com.example.logquery.entity.Application;
import com.example.logquery.service.ApplicationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyInterceptor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ApplicationService appService;

    public ApiKeyInterceptor(ApplicationService appService) {
        this.appService = appService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @Nullable Object handler) {
        // 只拦截 POST / PUT / DELETE，GET 请求直接放行
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        if ("GET".equals(method)) {
            return true;
        }

        // OPTIONS 预检请求放行
        if ("OPTIONS".equals(method)) {
            return true;
        }

        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || apiKey.isBlank()) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "缺少 API Key，请在 X-API-Key 请求头中提供");
            return false;
        }

        Optional<Application> appOpt = appService.getByApiKey(apiKey.trim());
        if (appOpt.isEmpty()) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "无效的 API Key");
            return false;
        }

        Application app = appOpt.get();
        if (!app.isEnabled()) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "该应用已被禁用");
            return false;
        }

        request.setAttribute("appId", app.getId());
        request.setAttribute("appName", app.getName());
        return true;
    }

    private void writeError(HttpServletResponse response, int status, String message) {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        try {
            String json = objectMapper.writeValueAsString(
                    Map.of("success", false, "message", message)
            );
            response.getWriter().write(json);
        } catch (JsonProcessingException e) {
            log.error("序列化错误响应失败", e);
        } catch (IOException e) {
            log.error("写入错误响应失败", e);
        }
    }
}
