package com.ubos.server.bootstrap;

import com.ubos.kernel.service.LcmKernelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SystemBootstrapper implements CommandLineRunner {

    private final Genesis genesis; // 注入 Genesis 逻辑类
    private final LcmKernelService kernelService;
    private final R2dbcEntityTemplate dbTemplate;

    @Value("${ubos.genesis.strategy:SAFE}")
    private String strategy;

    @Override
    public void run(String... args) {
        log.info("🏁 [Boot] Genesis Strategy: {}", strategy);

        switch (strategy.toUpperCase()) {
            case "RESET":
                // 开发模式：强制重置
                forceReset().subscribe();
                break;

            case "SAFE":
            default:
                // 生产模式：先查后建
                kernelService.exists("type.meta")
                    .flatMap(exists -> {
                        if (exists) {
                            log.info("🛡️ [Safe] System already initialized. Skipping Genesis.");
                            return Mono.empty();
                        } else {
                            // 调用 Genesis 类里的 performGenesis 方法（注意：您需要在 Genesis.java 里把 performGenesis 设为 public）
                            // 或者在这里直接调用 genesis.run() 如果 Genesis 也是 CommandLineRunner
                            // 建议：让 Genesis 提供一个 public Mono<Void> execute() 方法
                            return genesis.execute()
                                .doOnSuccess(s -> log.info("✅ [Init] First-time Genesis Completed!"));
                        }
                    })
                    .subscribe();
                break;
        }
    }

    /**
     * 【公开接口】强制重置系统 (供 AdminOpsController 调用)
     * 1. 清空数据库
     * 2. 重新执行创世
     */
    public Mono<Void> forceReset() {
        return wipeDatabase()
            .then(genesis.execute()) // 重新运行创世逻辑
            .doOnSuccess(v -> log.info("♻️ [Force Reset] System has been reset successfully!"))
            .doOnError(e -> log.error("❌ [Force Reset] Failed", e));
    }

    /**
     * 清空核心表
     */
    private Mono<Void> wipeDatabase() {
        log.warn("⚠️ [WIPE] Truncating all core tables...");

        // 这里的表名必须和您数据库实际表名一致
        String sql = """
            TRUNCATE TABLE lcm_entity_version_chain, 
                           lcm_entity_branch_head, 
                           lcm_entity_instance 
            RESTART IDENTITY CASCADE;
        """;

        return dbTemplate.getDatabaseClient().sql(sql).then();
    }
}