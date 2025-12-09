package com.ubos.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubos.kernel.service.LcmKernelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BootController {

    private final LcmKernelService kernelService;
    private final ObjectMapper objectMapper;

    @GetMapping("/boot")
    public Mono<Map<String, Object>> boot() {
        // 这里的 ID 必须和 Genesis.java 里的一致
        String targetSlug = "schema.desktop.default";

        return kernelService.getResourceSnapshot("VIEW", targetSlug, "master")
            .map(jsonString -> {
                // --- 分支 1：找到数据了 ---
                try {
                    Object schemaObj = objectMapper.readValue(jsonString, Object.class);
                    log.info("✅ [Boot] Loaded schema from DB");
                    Map<String, Object> result = new HashMap<>();
                    result.put("mode", "OWNER");
                    result.put("desktop_schema", schemaObj); // 确保 Key 是 desktop_schema
                    return result;
                } catch (Exception e) {
                    throw new RuntimeException("JSON Parse Error");
                }
            })
            // --- 分支 2：没找到数据 (数据库空的) ---
            // ⚠️⚠️ 之前的错误通常在这里！
            .defaultIfEmpty(Map.of(
                "mode", "GUEST",
                // ⚠️⚠️ 必须改这里！把 "schema" 改为 "desktop_schema"
                "desktop_schema", Map.of(
                    "wallpaper", "https://images.unsplash.com/photo-1620641788421-7a1c342ea42e",
                    "icons", java.util.List.of(), // 空图标
                    "error_hint", "Genesis Data Not Found" // 加个标记，方便调试
                )
            ));
    }
}