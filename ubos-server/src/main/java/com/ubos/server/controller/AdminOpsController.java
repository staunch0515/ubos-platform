package com.ubos.server.controller;

import com.ubos.server.bootstrap.SystemBootstrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/ops")
@RequiredArgsConstructor
public class AdminOpsController {

    private final SystemBootstrapper bootstrapper;

    @PostMapping("/reset-world")
    public Mono<String> resetWorld(@RequestHeader("X-Admin-Key") String key) {
        // 1. 简单的安全检查
        if (!"MY_SECRET_KEY".equals(key)) {
            return Mono.error(new RuntimeException("Forbidden"));
        }

        // 2. 强制执行 RESET 逻辑
        // 注意：这里需要把 Bootstrapper 里的 wipe 和 genesis 逻辑提取出来调用
        return bootstrapper.forceReset()
            .thenReturn("World has been reset.");
    }
}