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
     * 【凡人之躯】执行逻辑
     * * URL: POST /api/run/logic.hello.world?branch=master
     * Body: { "name": "Gemini" }
     */
    @PostMapping("/{slug}")
    public Mono<Object> execute(
        @PathVariable String slug,
        @RequestParam(defaultValue = "master") String branch,
        @RequestBody Map<String, Object> context) {

        return runtimeService.runLogic(slug, branch, context);
    }
}