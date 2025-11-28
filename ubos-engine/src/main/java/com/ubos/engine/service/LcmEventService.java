package com.ubos.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubos.engine.executor.LcmLogicExecutor;
import com.ubos.kernel.service.LcmKernelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Slf4j
@Service
public class LcmEventService {

    private final LcmKernelService kernelService;
    private final LcmLogicExecutor logicExecutor;
    private final ObjectMapper objectMapper;

    // 使用 @Lazy 解决循环依赖：Executor 需要 EventService (注入给脚本)，EventService 需要 Executor (执行脚本)
    public LcmEventService(LcmKernelService kernelService,
        @Lazy LcmLogicExecutor logicExecutor,
        ObjectMapper objectMapper) {
        this.kernelService = kernelService;
        this.logicExecutor = logicExecutor;
        this.objectMapper = objectMapper;
    }

    /**
     * 发射神经信号 (Fire and Forget)
     * @param eventName 事件名称，如 "user.created"
     * @param payload 携带的数据
     */
    public void publish(String eventName, Map<String, Object> payload) {
        log.info(" [Nervous System] Event Fired: [{}]", eventName);

        // 1. 搜索所有订阅了该事件的监听器 (LISTENER)
        // 我们利用之前实现的 search 接口，查找 type=LISTENER 且 trigger=eventName 的实体
        Flux<Map<String, Object>> listeners = kernelService.search(
            "LISTENER",
            "master", // 暂时默认只在 master 分支触发，未来可传递 branch
            Map.of("trigger", eventName)
        );

        // 2. 异步并行执行
        listeners.publishOn(Schedulers.boundedElastic()) // 切换到后台线程，不阻塞当前业务
            .flatMap(listenerSnapshot -> {
                try {
                    // 将 Map 还原为 JSON 字符串以便 Executor 解析
                    String snapshotJson = objectMapper.writeValueAsString(listenerSnapshot);

                    log.info("⚡ [Synapse] Triggering listener...");
                    // 执行监听器逻辑
                    return logicExecutor.execute(snapshotJson, payload)
                        .onErrorResume(e -> {
                            log.error("Listener execution failed", e);
                            return reactor.core.publisher.Mono.empty(); // 失败不影响其他监听器
                        });
                } catch (Exception e) {
                    return reactor.core.publisher.Mono.error(e);
                }
            })
            .subscribe(); // 立即订阅触发
    }
}