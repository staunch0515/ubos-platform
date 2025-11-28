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

import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LcmLogicExecutor {

    private final ObjectMapper objectMapper;
    private final CompilerConfiguration compilerConfig;

    // 【新增】免疫系统的独立线程池 (隔离业务逻辑，防止阻塞主线程)
    private final ExecutorService sandboxPool = Executors.newCachedThreadPool();

    // 编译缓存
    private final Map<String, Class<?>> scriptCache = new ConcurrentHashMap<>();

    // 【新增】最大执行时间 (毫秒) -> 超过这个时间直接杀掉
    private static final long MAX_EXECUTION_TIME_MS = 3000;

    public Mono<Object> execute(String snapshotJson, Map<String, Object> contextParams) {
        return Mono.create(sink -> {
            try {
                // 1. 解析与编译 (这一步很快，在主线程做)
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

                // 2. 准备环境
                Binding binding = new Binding();
                binding.setVariable("ctx", contextParams);
                binding.setVariable("log", log);

                Script script = (Script) scriptClass.getDeclaredConstructor().newInstance();
                script.setBinding(binding);

                // 3. 【核心升级】在沙箱线程池中执行，并设置超时熔断
                CompletableFuture.supplyAsync(() -> {
                        try {
                            return script.run();
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }, sandboxPool)
                    .orTimeout(MAX_EXECUTION_TIME_MS, TimeUnit.MILLISECONDS) // JDK 9+ 特性：超时机制
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            // 4. 处理异常
                            if (ex instanceof TimeoutException) {
                                log.error("⚔️ [Immune System] Logic execution timed out! Killing process...");
                                sink.error(new RuntimeException("Security Alert: Logic execution time exceeded limit (3000ms)."));
                            } else {
                                sink.error(ex.getCause() != null ? ex.getCause() : ex);
                            }
                        } else {
                            // 5. 执行成功
                            sink.success(result);
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