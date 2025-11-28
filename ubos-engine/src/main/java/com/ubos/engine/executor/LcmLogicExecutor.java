package com.ubos.engine.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import com.ubos.kernel.service.LcmKernelService;

import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LcmLogicExecutor {

    private final ObjectMapper objectMapper;
    private final CompilerConfiguration compilerConfig;
    // 【新增】注入内核服务
    private final LcmKernelService kernelService;

    // 使用 CachedThreadPool，但建议生产环境限制最大线程数，防止线程爆炸
    private final ExecutorService sandboxPool = Executors.newCachedThreadPool();

    private final Map<String, Class<?>> scriptCache = new ConcurrentHashMap<>();

    // 3秒熔断
    private static final long MAX_EXECUTION_TIME_MS = 3000;

    public Mono<Object> execute(String snapshotJson, Map<String, Object> contextParams) {
        return Mono.create(sink -> {
            try {
                JsonNode root = objectMapper.readTree(snapshotJson);
                String scriptContent = root.path("content").asText();
                if (scriptContent == null || scriptContent.isBlank()) {
                    sink.error(new IllegalArgumentException("Logic content is empty"));
                    return;
                }

                String cacheKey = String.valueOf(scriptContent.hashCode());
                Class<?> scriptClass = scriptCache.computeIfAbsent(cacheKey, k -> {
                    GroovyShell shell = new GroovyShell(compilerConfig);
                    return shell.getClassLoader().parseClass(scriptContent);
                });

                Binding binding = new Binding();
                binding.setVariable("ctx", contextParams);
                binding.setVariable("log", log);

                // 【核心修改】将内核服务注入给脚本，变量名叫 "kernel"
                binding.setVariable("kernel", kernelService);
                // 注入一个简单的 JSON 工具，方便脚本序列化数据
                binding.setVariable("jsonUtils", objectMapper);

                Script script = (Script) scriptClass.getDeclaredConstructor().newInstance();
                script.setBinding(binding);

                // 【核心修复：使用 Future 并在超时时强制中断】
                Future<Object> future = sandboxPool.submit(() -> {
                    try {
                        return script.run();
                    } catch (Exception e) {
                        // 脚本内部抛出的异常（包括被中断后的异常）
                        throw new CompletionException(e);
                    }
                });

                // 启动一个监视器来处理超时 (异步非阻塞)
                CompletableFuture.runAsync(() -> {
                    try {
                        // 等待结果
                        Object result = future.get(MAX_EXECUTION_TIME_MS, TimeUnit.MILLISECONDS);
                        sink.success(result);
                    } catch (TimeoutException e) {
                        // 1. 超时发生！
                        log.error("⚔️ [Immune System] Logic timeout! Sending INTERRUPT signal...");

                        // 2. 关键动作：发送中断信号！
                        // 配合 ScriptSecurityConfig 里的 ThreadInterrupt，这会让 while(true) 瞬间崩溃
                        future.cancel(true);

                        sink.error(new RuntimeException("Security Alert: Logic killed by Immune System (Timeout)."));
                    } catch (Exception e) {
                        // 其他执行错误
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        sink.error(cause);
                    }
                });

            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    public void invalidateCache() {
        scriptCache.clear();
        log.info("Script cache cleared.");
    }
}