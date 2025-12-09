package com.ubos.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubos.kernel.service.LcmKernelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * 认证服务
 * 职责：连接 Controller 和 Kernel，负责校验密码和发放 Token
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final LcmKernelService kernelService;
    private final ObjectMapper objectMapper;

    /**
     * 登录逻辑
     * @param email 前端传来的邮箱
     * @param password 前端传来的密码
     * @return 登录成功后的用户信息和 Token
     */
    public Mono<Map<String, Object>> login(String email, String password) {
        // --- MVP 简化策略 ---
        // 在 v0.1 中，我们暂时还没做全文索引。
        // 我们知道 Genesis 里创建的管理员 ID 是 "user_admin"。
        // 所以我们直接加载这个 ID，然后比对邮箱和密码。
        // (在 v1.0 中，这里应该调用 kernelService.search("email", email))

        String targetSlug = "user_admin";

        return kernelService.getResourceSnapshot("USER", targetSlug, "master")
            .flatMap(json -> {
                try {
                    // 1. 将 JSON 字符串转为 Map
                    Map<String, Object> userData = objectMapper.readValue(json, new TypeReference<>() {});

                    // 2. 获取数据库里的账号密码
                    String storedEmail = (String) userData.get("email");
                    String storedPass = (String) userData.get("password");

                    // 3. 校验
                    if (email.equals(storedEmail) && password.equals(storedPass)) {
                        // 4. 生成 Token (MVP 用 UUID 代替 JWT)
                        String token = UUID.randomUUID().toString();

                        log.info("✅ Login success: {}", email);

                        // 5. 返回给前端的数据
                        return Mono.just(Map.of(
                            "token", token,
                            "user", Map.of(
                                "id", targetSlug,
                                "name", userData.get("name"),
                                "role", userData.get("role"),
                                "avatar", userData.get("avatar")
                            )
                        ));
                    } else {
                        log.warn("❌ Login failed (password mismatch): {}", email);
                        return Mono.error(new RuntimeException("Invalid Credentials"));
                    }
                } catch (Exception e) {
                    log.error("❌ Login error", e);
                    return Mono.error(new RuntimeException("Data Error"));
                }
            })
            // 如果数据库里连 user_admin 都没有 (Genesis 没跑成功)
            .switchIfEmpty(Mono.error(new RuntimeException("Admin User Not Found")));
    }
}