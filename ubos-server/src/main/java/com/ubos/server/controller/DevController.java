package com.ubos.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubos.kernel.service.LcmKernelService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevController {

    private final LcmKernelService kernelService;
    private final ObjectMapper objectMapper;

    /**
     * 【上帝之手】提交逻辑/数据
     */
    @PostMapping("/commit")
    public Mono<Map<String, Object>> commit(@RequestBody CommitRequest req) {
        // 1. 将代码封装成 UBOS 标准快照格式 (JSON)
        Map<String, Object> snapshotMap = Map.of(
            "meta", Map.of("type", "groovy"),
            "content", req.getCode()
        );

        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshotMap);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }

        // 2. 调用内核提交 (现在传入 7 个参数)
        return kernelService.commit(
            req.getType(),
            req.getSlug(),
            req.getBranch(),
            snapshotJson,
            "admin", // 作者
            req.getMessage(),
            req.getProcessId() // 【新增】传入流程ID (可为 null)
        ).map(commitId -> Map.of(
            "status", "success",
            "commitId", commitId,
            "slug", req.getSlug()
        ));
    }

    @Data
    static class CommitRequest {
        private String type;
        private String slug;
        private String branch;
        private String code;
        private String message;
        private String processId; // 【新增】请求体增加 processId 字段
    }
}