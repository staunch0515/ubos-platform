package com.ubos.engine.service;

import com.ubos.engine.executor.LcmLogicExecutor;
import com.ubos.kernel.service.LcmKernelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UbosRuntimeService {

    private final LcmKernelService kernelService;
    private final LcmLogicExecutor logicExecutor;

    /**
     * 终极调用：给定逻辑名，直接运行结果
     */
    public Mono<Object> runLogic(String logicSlug, String branch, Map<String, Object> context) {
        // 1. 去内核层找代码 (DNA)
        return kernelService.getResourceSnapshot("LOGIC", logicSlug, branch)
            .switchIfEmpty(Mono.error(new RuntimeException("Logic not found: " + logicSlug + " @ " + branch)))
            // 2. 去引擎层跑代码 (Brain)
            .flatMap(jsonSnapshot -> logicExecutor.execute(jsonSnapshot, context));
    }
}