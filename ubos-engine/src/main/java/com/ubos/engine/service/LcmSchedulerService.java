package com.ubos.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubos.kernel.service.LcmKernelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient; // 引入 DatabaseClient
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
public class LcmSchedulerService {

    private final TaskScheduler taskScheduler;
    private final UbosRuntimeService runtimeService;
    private final ObjectMapper objectMapper;
    private final DatabaseClient dbClient; // 【新增】直接使用 DatabaseClient

    private final Map<String, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();

    // 解决循环依赖：注入时 @Lazy
    public LcmSchedulerService(TaskScheduler taskScheduler, UbosRuntimeService runtimeService,
        ObjectMapper objectMapper, DatabaseClient dbClient) {
        this.taskScheduler = taskScheduler;
        this.runtimeService = runtimeService;
        this.objectMapper = objectMapper;
        this.dbClient = dbClient;
    }

    @PostConstruct
    public void init() {
        log.info("⏰ [Bio-Clock] Initializing scheduler...");
        refreshAllTasks();
    }

    public void refreshAllTasks() {
        activeTasks.forEach((slug, future) -> future.cancel(false));
        activeTasks.clear();

        // 【核心修复】直接使用原生 SQL 查询 CRON 实体，避免复杂的 JOIN 和 Null 传播
        String sql = """
            SELECT v.snapshot_data 
            FROM lcm_entity_instance i
            JOIN lcm_entity_branch_head h ON i.id = h.entity_id
            JOIN lcm_entity_version_chain v ON h.head_commit_id = v.commit_id
            WHERE i.entity_type = 'CRON' AND h.branch_name = 'master'
        """;

        dbClient.sql(sql)
            .fetch().all() // 获取所有 CRON 任务的 JSON 快照
            .doOnNext(this::scheduleTask)
            .subscribe(
                null, // onNext is handled by doOnNext
                error -> log.error("❌ Scheduler initialization failed due to DB error.", error) // 捕获 DB 启动错误
            );
    }

    @SuppressWarnings("unchecked")
    private void scheduleTask(Map<String, Object> snapshot) {
        try {
            // 【修正】JSON 解包逻辑保持不变
            String contentJson = (String) snapshot.get("snapshot_data"); // 从 row.get("snapshot_data") 中取出
            if (contentJson == null || contentJson.isBlank()) {
                // 如果查询结果是空的，跳过
                return;
            }

            // 二次解析：将 content 字符串转为 Map
            Map<String, Object> config = objectMapper.readValue(contentJson, Map.class);

            String cronExpression = (String) config.get("cron");
            String targetLogic = (String) config.get("target");
            Map<String, Object> params = (Map<String, Object>) config.getOrDefault("params", Collections.emptyMap());

            String taskId = targetLogic + "::" + cronExpression;

            if (cronExpression != null && targetLogic != null) {
                ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> executeJob(targetLogic, params),
                    new CronTrigger(cronExpression)
                );

                activeTasks.put(taskId, future);
                log.info("⏰ [Bio-Clock] Task scheduled: [{}] -> [{}]", targetLogic, cronExpression);
            }
        } catch (Exception e) {
            log.error("Failed to schedule task: " + snapshot, e);
        }
    }

    private void executeJob(String logicSlug, Map<String, Object> params) {
        log.info("⏰ [Bio-Clock] Triggering job: {}", logicSlug);
        runtimeService.runLogic(logicSlug, "master", null, params)
            .subscribe(
                result -> log.info("✅ Job [{}] success: {}", logicSlug, result),
                error -> log.error("❌ Job [{}] failed", logicSlug, error)
            );
    }
}