package com.ubos.server.config;

import com.fasterxml.jackson.databind.ObjectMapper; // 需要引入 ObjectMapper
import com.ubos.common.model.AuthContext;
import com.ubos.kernel.service.LcmKernelService; // 引入内核服务
import com.ubos.common.util.UbosConstants;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@Component
public class AuthWebFilter implements WebFilter {

    public static final Class<AuthContext> CONTEXT_KEY = UbosConstants.AUTH_CONTEXT_KEY;

    // 【新增】注入内核服务和 ObjectMapper
    private final LcmKernelService kernelService;
    private final ObjectMapper objectMapper;

    public AuthWebFilter(LcmKernelService kernelService, ObjectMapper objectMapper) {
        this.kernelService = kernelService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 1. 模拟身份验证 (确定 userId 和默认角色)
        AuthContext defaultContext = AuthContext.builder().userId("A001").username("Architect").role("ADMIN").tenantId("MASTER").build();

        // 假设我们从 Header/Session 中获取了 User ID
        String userId = defaultContext.getUserId(); // 示例：使用 Admin ID 作为查找目标

        // 2. 定义查询用户策略的逻辑
        // 我们查找一个 DATA_USER_POLICY 实体，该实体的 slug 可能是 policy.A001
        Mono<String> policyBranchMono = kernelService.getResourceSnapshot(
                "CONFIG",
                "policy.user." + userId,
                "master"
            )
            .onErrorResume(e -> Mono.empty()); // 如果找不到配置，不报错

        // 3. 异步获取策略，并构建最终的 AuthContext
        Mono<AuthContext> finalContextMono = policyBranchMono
            .flatMap(json -> {
                // 3a. 解析策略 JSON，获取 policyBranchName
                try {
                    Map<String, String> policy = objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                    String policyBranch = policy.getOrDefault("policyBranch", "master");

                    // 3b. 构建最终上下文，覆盖默认 TenantID
                    return Mono.just(AuthContext.builder()
                        .userId(userId)
                        .username(defaultContext.getUsername())
                        .role(defaultContext.getRole())
                        .tenantId(policyBranch) // ⚠️ 【核心 FIX】将 Branch 赋给 TenantID
                        .build());

                } catch (Exception e) {
                    // 解析失败，回退到默认上下文
                    return Mono.just(defaultContext);
                }
            })
            .defaultIfEmpty(defaultContext); // 如果 policyBranchMono 是空的，则使用默认上下文

        // 4. 将最终上下文注入到 Reactor Context 中，并继续链路
        return finalContextMono.flatMap(finalContext -> {
            return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(CONTEXT_KEY, finalContext));
        });
    }
}