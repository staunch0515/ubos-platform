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
     * 对应 Git Commit。
     * * 示例 Request Body:
     * {
     * "type": "LOGIC",
     * "slug": "logic.hello.world",
     * "branch": "master",
     * "code": "return 'Hello ' + ctx.name",
     * "message": "First commit"
     * }
     */
    @PostMapping("/commit")
    public Mono<Map<String, Object>> commit(@RequestBody CommitRequest req) {
        // 1. 将代码封装成 UBOS 标准快照格式 (JSON)
        Map<String, Object> snapshotMap = Map.of(
            "meta", Map.of("type", "groovy"),
            "content", req.getCode() // 真正的逻辑代码
        );

        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshotMap);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }

        // 2. 调用内核提交
        return kernelService.commit(
            req.getType(),
            req.getSlug(),
            req.getBranch(),
            snapshotJson,
            "admin", // 暂时写死作者，实际从 Token 取
            req.getMessage()
        ).map(commitId -> Map.of(
            "status", "success",
            "commitId", commitId,
            "slug", req.getSlug()
        ));
    }

    @Data
    static class CommitRequest {
        private String type;   // LOGIC, VIEW, DATA
        private String slug;   // 唯一标识
        private String branch; // 分支
        private String code;   // Groovy 代码 或 UI JSON
        private String message;// 提交信息
    }
}