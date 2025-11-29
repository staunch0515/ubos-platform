package com.ubos.engine.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubos.common.model.AuthContext;
import com.ubos.engine.service.LcmEventService;
import com.ubos.kernel.service.LcmKernelService;
import com.ubos.kernel.service.LcmRelationService;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import com.ubos.common.util.UbosConstants;

import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
public class LcmLogicExecutor {

    private final ObjectMapper objectMapper;
    private final CompilerConfiguration compilerConfig;
    private final LcmKernelService kernelService;
    private final LcmEventService eventService;

    private final ExecutorService sandboxPool = Executors.newCachedThreadPool();
    private final Map<String, Class<?>> scriptCache = new ConcurrentHashMap<>();

    private static final long MAX_EXECUTION_TIME_MS = 3000;

    private final LcmRelationService relationService;

    // 构造函数：注入所有依赖 (注意：LcmEventService 需要 @Lazy 配合，以解决循环依赖)
    public LcmLogicExecutor(ObjectMapper objectMapper, CompilerConfiguration compilerConfig,
        LcmKernelService kernelService, @Lazy LcmEventService eventService, LcmRelationService relationService) {
        this.objectMapper = objectMapper;
        this.compilerConfig = compilerConfig;
        this.kernelService = kernelService;
        this.eventService = eventService;
        this.relationService = relationService;
    }

    /**
     * 核心执行方法：从 Reactor Context 中获取 AuthContext，并运行脚本。
     * @param snapshotJson 逻辑快照 JSON
     * @param contextParams 初始业务参数
     */
    public Mono<Object> execute(String snapshotJson, Map<String, Object> contextParams) {

        // 【核心结构调整】使用 Mono.deferContextual 获取 AuthContext
        return Mono.deferContextual(contextView -> {

            // 1. 从 Reactor Context 中取出 AuthContext
            AuthContext authContext = contextView.getOrDefault(UbosConstants.AUTH_CONTEXT_KEY,
                AuthContext.builder().userId("SYSTEM").username("SYSTEM_USER").role("SYSTEM").tenantId("MASTER").build());

            // 2. 将身份信息合并到 Groovy 脚本的 contextParams 中
            // 脚本里可以用 ctx.currentUser.role 来获取权限信息
            contextParams.put("currentUser", authContext);

            // 3. 脚本编译和运行逻辑 (封装为 Mono.create/fromCallable)
            return Mono.create(sink -> {
                try {
                    // --- 编译和缓存逻辑 ---
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

                    // --- 运行环境 (Binding) ---
                    Binding binding = new Binding();
                    binding.setVariable("ctx", contextParams);
                    binding.setVariable("log", log);
                    binding.setVariable("kernel", kernelService);
                    binding.setVariable("jsonUtils", objectMapper);
                    binding.setVariable("dispatcher", eventService);
                    binding.setVariable("relations", relationService);

                    Script script = (Script) scriptClass.getDeclaredConstructor().newInstance();
                    script.setBinding(binding);

                    // --- 沙箱执行与超时熔断 ---
                    Future<Object> future = sandboxPool.submit(() -> {
                        return script.run();
                    });

                    // 监视器 (异步非阻塞地等待结果)
                    CompletableFuture.runAsync(() -> {
                        try {
                            // 关键：等待结果并触发 TimeoutException
                            Object result = future.get(MAX_EXECUTION_TIME_MS, TimeUnit.MILLISECONDS);
                            sink.success(result);
                        } catch (TimeoutException e) {
                            log.error("⚔️ [Immune System] Logic timeout! Sending INTERRUPT signal...");
                            future.cancel(true); // 强制中断
                            sink.error(new RuntimeException("Security Alert: Logic killed by Immune System (Timeout)."));
                        } catch (Exception e) {
                            // 智能解包异常链，提取最根本的业务异常
                            Throwable cause = e;
                            while ((cause instanceof ExecutionException || cause instanceof CompletionException) && cause.getCause() != null) {
                                cause = cause.getCause();
                            }
                            sink.error(cause);
                        }
                    });

                } catch (Exception e) {
                    sink.error(e);
                }
            });
        });
    }

    public void invalidateCache() {
        scriptCache.clear();
        log.info("Script cache cleared.");
    }
}