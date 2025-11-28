package com.ubos.server.config;

import com.ubos.common.model.AuthContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import com.ubos.common.util.UbosConstants;
import java.util.Optional;

@Component
public class AuthWebFilter implements WebFilter {


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 1. 模拟从 Header 中解析 Token (实际生产环境需要 JWT 解析)
        Optional<String> authHeader = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("X-Auth-Token"));

        // 2. 构造身份上下文 (这里硬编码一个默认的 Admin)
        AuthContext authContext = authHeader.map(token -> {
            // In a real app, logic would decode JWT based on token value
            if (token.equalsIgnoreCase("VALID_TOKEN_STAFF")) {
                return AuthContext.builder().userId("U001").username("StaffUser").role("STAFF").tenantId("MASTER").build();
            } else {
                return AuthContext.builder().userId("GUEST").username("GuestUser").role("GUEST").tenantId("MASTER").build();
            }
        }).orElseGet(() ->
            // 默认用户 (方便测试)
            AuthContext.builder().userId("A001").username("Architect").role("ADMIN").tenantId("MASTER").build()
        );

        // 3. 将身份上下文注入到 Reactor Context 中，向下游传递
        return chain.filter(exchange)
            .contextWrite(ctx -> ctx.put(UbosConstants.AUTH_CONTEXT_KEY, authContext));
    }
}