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
     * 修改后的运行逻辑：
     * 1. 如果传了 commitId，直接通过 ID 找（回到过去）。
     * 2. 如果没传，才去通过 branch 找最新版（活在当下）。
     */
    public Mono<Object> runLogic(String logicSlug, String branch, Long commitId, Map<String, Object> context) {
        Mono<String> sourceMono;

        if (commitId != null) {
            // 🕰️ 时光穿梭模式
            sourceMono = kernelService.getSnapshotByCommit(commitId)
                .switchIfEmpty(Mono.error(new RuntimeException("Commit not found: " + commitId)));
        } else {
            // ⚡️ 正常模式
            sourceMono = kernelService.getResourceSnapshot("LOGIC", logicSlug, branch)
                .switchIfEmpty(Mono.error(new RuntimeException("Logic not found: " + logicSlug + " @ " + branch)));
        }

        return sourceMono
            .flatMap(jsonSnapshot -> logicExecutor.execute(jsonSnapshot, context));
    }
}