package com.ubos.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper; // 引入 Jackson Mapper
import com.ubos.common.model.AuthContext;
import com.ubos.kernel.service.LcmKernelService;
import com.ubos.server.config.AuthWebFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.ubos.common.util.UbosConstants;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/view")
@RequiredArgsConstructor
public class ViewController {

    private final LcmKernelService kernelService;
    private final ObjectMapper objectMapper;

    /**
     * 【主视图接口】根据用户身份和分支，获取 UI 结构 JSON
     * GET /api/view/view.country.form
     */
    @GetMapping("/{slug}")
    public Mono<Map<String, Object>> getViewDefinition(
        @PathVariable String slug,
        ServerWebExchange exchange) {

        // 1. 从 Reactor Context 中获取 AuthContext
        return Mono.deferContextual(contextView -> {
            AuthContext authContext = contextView.get(UbosConstants.AUTH_CONTEXT_KEY);
            // 1. 确定目标分支 (Admin 默认看 dev)
            final String targetBranch = authContext.getRole().equals("ADMIN") ? "dev" : authContext.getTenantId().toLowerCase();

            // 2. 注入响应头
            // ⚠️ FIX: 必须在 Mono 流中添加 Header，以确保在响应发出前被设置
            exchange.getResponse().getHeaders().add("X-UBOS-BRANCH", targetBranch);

            // 3. 调用 KernelService 查找 VIEW 实体
            return kernelService.getResourceSnapshot("VIEW", slug, targetBranch)
                .map(jsonString -> {
                    try {
                        return (Map<String, Object>) objectMapper.readValue(jsonString, Map.class);
                    } catch (Exception e) {
                        throw new RuntimeException("View JSON parsing failed: " + e.getMessage(), e);
                    }
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "View definition not found for slug: " + slug)));
        });
    }
}