package com.ubos.server.controller;

import com.ubos.engine.service.UbosRuntimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/run")
@RequiredArgsConstructor
public class RunController {

    private final UbosRuntimeService runtimeService;

    /**
     * URL: POST /api/run/{slug}?branch=master&commitId=1
     */
    @PostMapping("/{slug}")
    public Mono<Object> execute(
        @PathVariable String slug,
        @RequestParam(defaultValue = "master") String branch,
        @RequestParam(required = false) Long commitId, // 【新增】可选参数
        @RequestBody Map<String, Object> context) {

        // 传入 commitId
        return runtimeService.runLogic(slug, branch, commitId, context);
    }
}