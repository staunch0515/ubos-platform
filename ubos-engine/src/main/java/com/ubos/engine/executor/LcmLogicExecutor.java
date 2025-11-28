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
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class LcmLogicExecutor {

    private final ObjectMapper objectMapper;
    private final CompilerConfiguration compilerConfig; // 注入上面的安全配置

    // 【记忆皮层】 编译缓存：避免每次都重新编译，Key 是脚本内容的 Hash 或 MD5
    private final Map<String, Class<?>> scriptCache = new ConcurrentHashMap<>();

    /**
     * 执行逻辑的核心入口
     * @param snapshotJson 数据库里存的那段 JSON 字符串
     * @param contextParams 传给脚本的变量 (ctx)
     */
    public Mono<Object> execute(String snapshotJson, Map<String, Object> contextParams) {
        return Mono.fromCallable(() -> {
            // 1. 解析 JSON 包 (解开 DNA)
            JsonNode root = objectMapper.readTree(snapshotJson);

            // 假设 JSON 结构是: { "meta": {...}, "content": "groovy code..." }
            String scriptContent = root.path("content").asText();

            if (scriptContent == null || scriptContent.isBlank()) {
                throw new IllegalArgumentException("Logic snapshot content is empty");
            }

            // 2. 编译或获取缓存 (神经连接)
            // 这里的 Cache Key 可以用 snapshotJson 的哈希值，这里简化用 content 的哈希
            String cacheKey = String.valueOf(scriptContent.hashCode());

            Class<?> scriptClass = scriptCache.computeIfAbsent(cacheKey, k -> {
                log.info("Compiling new script with hash: {}", k);
                GroovyShell shell = new GroovyShell(compilerConfig);
                return shell.getClassLoader().parseClass(scriptContent);
            });

            // 3. 准备运行环境 (注入上下文)
            Binding binding = new Binding();

            // 注入 'ctx' 变量，这是脚本与外界交互的唯一窗口
            binding.setVariable("ctx", contextParams);

            // TODO: 在这里还可以注入 'db', 'http', 'log' 等工具类
            binding.setVariable("log", log);

            // 4. 实例化并运行
            Script script = (Script) scriptClass.getDeclaredConstructor().newInstance();
            script.setBinding(binding);

            log.debug("Executing script...");
            return script.run();
        });
    }

    /**
     * 清除缓存 (当发生“神经脉冲”/回滚时调用)
     */
    public void invalidateCache() {
        scriptCache.clear();
        log.info("Script cache cleared.");
    }
}