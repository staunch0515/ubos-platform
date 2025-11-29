package com.ubos.kernel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubos.kernel.model.LcmEntityInstance;
import com.ubos.kernel.model.LcmEntitySearchIndex;
import com.ubos.kernel.model.LcmEntityVersionChain;
import com.ubos.kernel.repository.LcmEntityRepository;
import com.ubos.kernel.repository.LcmSearchIndexRepository;
import com.ubos.kernel.repository.LcmVersionRepository;
import com.ubos.kernel.util.ReactiveRetry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LcmKernelService {

    private final LcmEntityRepository entityRepo;
    private final LcmVersionRepository versionRepo;
    private final LcmSearchIndexRepository indexRepo;
    private final DatabaseClient dbClient;
    private final ObjectMapper objectMapper;

    // ====================================================================
    // I. READ OPERATIONS (READ / TIME-TRAVEL / INHERITANCE)
    // ====================================================================

    /**
     * 【主入口】获取资源的最新快照 (支持继承)
     */
    public Mono<String> getResourceSnapshot(String type, String slug, String branch) {
        return findInBranchRecursive(type, slug, branch);
    }

    /**
     * 时光机：根据 CommitID 获取历史快照
     */
    public Mono<String> getSnapshotByCommit(Long commitId) {
        return versionRepo.findById(commitId)
            .map(LcmEntityVersionChain::getSnapshotData);
    }

    // 私有递归查找方法 (实现了继承逻辑)
    private Mono<String> findInBranchRecursive(String type, String slug, String currentBranch) {
        // 1. 尝试在当前分支查找
        return entityRepo.findByEntityTypeAndSlug(type, slug)
            .flatMap(entity -> versionRepo.findHeadSnapshot(entity.getId(), currentBranch))
            .map(LcmEntityVersionChain::getSnapshotData)

            // 2. 如果当前分支找不到
            .switchIfEmpty(Mono.defer(() -> {
                // FIX: 确保 Mono.defer 内部的整个链条被明确返回
                return getParentBranchName(currentBranch)
                    .defaultIfEmpty(!"master".equalsIgnoreCase(currentBranch) ? "master" : "")
                    .filter(p -> !p.isBlank())
                    .flatMap(parentBranch -> {
                        log.info("🔍 Resource [{}::{}] missing in [{}], fallback to parent [{}]",
                            type, slug, currentBranch, parentBranch);
                        // 3. 递归调用 (去父分支找)
                        return findInBranchRecursive(type, slug, parentBranch);
                    });
            }))
            .retryWhen(ReactiveRetry.databaseTransientErrors());
    }

    // 辅助方法：查询 sys_branch_config 表获取父分支名
    private Mono<String> getParentBranchName(String branch) {
        String sql = "SELECT parent_branch FROM sys_branch_config WHERE branch_name = :branch";
        return dbClient.sql(sql)
            .bind("branch", branch)
            .map((row, meta) -> row.get("parent_branch", String.class))
            .one()
            // ⚠️ FIX 3: 对配置查找应用重试策略
            .retryWhen(ReactiveRetry.databaseTransientErrors());
    }

    /**
     * 【搜索接口】支持无条件查询 (获取所有) 和有条件过滤 (查索引)
     */
    public Flux<Map<String, Object>> search(String type, String branch, Map<String, Object> filters) {
        String sql;

        if (filters.isEmpty()) {
            // 模式 A: 查所有 CRON 任务 (用于调度器加载)
            sql = """
                SELECT COALESCE(v.snapshot_data, '{}') as snapshot_data_safe 
                FROM lcm_entity_instance i
                JOIN lcm_entity_branch_head h ON i.id = h.entity_id
                JOIN lcm_entity_version_chain v ON h.head_commit_id = v.commit_id
                WHERE i.entity_type = :type
                  AND h.branch_name = :branch
            """;

            return dbClient.sql(sql)
                .bind("type", type)
                .bind("branch", branch)
                .map((row, meta) -> {
                    // 【终极防御】读取安全别名，防止 R2DBC 内部 Null 传播
                    String json = row.get("snapshot_data_safe", String.class);
                    if (json == null || json.isBlank()) return Collections.<String, Object>emptyMap();
                    return parseJsonToMap(json);
                })
                .all();
        } else {
            // 模式 B: 按条件查 (用于业务搜索)
            String propName = filters.keySet().iterator().next();
            Object propValue = filters.get(propName);

            sql = """
                SELECT COALESCE(v.snapshot_data, '{}') as snapshot_data_safe
                FROM lcm_entity_instance i
                JOIN lcm_entity_branch_head h ON i.id = h.entity_id
                JOIN lcm_entity_version_chain v ON h.head_commit_id = v.commit_id
                JOIN lcm_entity_search_index idx ON v.commit_id = idx.commit_id
                WHERE i.entity_type = :type
                  AND h.branch_name = :branch
                  AND idx.prop_name = :propName
                  AND idx.val_text = :valText
            """;

            return dbClient.sql(sql)
                .bind("type", type)
                .bind("branch", branch)
                .bind("propName", propName)
                .bind("valText", propValue.toString())
                .map((row, meta) -> {
                    // 【终极防御】读取安全别名
                    String json = row.get("snapshot_data_safe", String.class);
                    if (json == null || json.isBlank()) return Collections.<String, Object>emptyMap();
                    return parseJsonToMap(json);
                })
                .all();
        }
    }

    // ====================================================================
    // II. WRITE OPERATIONS (COMMIT / PROCESS / INDEXING)
    // ====================================================================

    // 兼容旧脚本的重载方法 (6 参数)
    public Mono<Long> commit(String type, String slug, String branch, String jsonContent, String author, String msg) {
        return commit(type, slug, branch, jsonContent, author, msg, null);
    }

    /**
     * 【主写入方法】提交变更 (Commit)
     */
    @Transactional
    public Mono<Long> commit(String type, String slug, String branch, String jsonContent, String author, String msg, String processId) {
        return entityRepo.findByEntityTypeAndSlug(type, slug)
            .switchIfEmpty(createEntity(type, slug))
            .flatMap(entity -> {
                // 1. 获取该分支当前的 Head Commit ID
                Mono<Long> parentIdMono = versionRepo.findHeadSnapshot(entity.getId(), branch)
                    .map(LcmEntityVersionChain::getCommitId)
                    .defaultIfEmpty(0L);
                return parentIdMono
                    .flatMap(parentId -> {
                        Long actualParentId = (parentId == 0L) ? null : parentId;
                        LcmEntityVersionChain newCommit = LcmEntityVersionChain.builder()
                            .entityId(entity.getId())
                            .branchName(branch)
                            .parentCommitId(actualParentId)
                            .snapshotData(jsonContent)
                            .authorId(author)
                            .message(msg)
                            .committedAt(LocalDateTime.now())
                            .build();
                        return versionRepo.save(newCommit);
                    })
                    .retryWhen(ReactiveRetry.databaseTransientErrors())
                    .flatMap(savedCommit ->
                        // 1. 建立索引
                        createIndex(savedCommit.getCommitId(), jsonContent)
                            // 2. 移动分支指针
                            .then(updateBranchHead(entity.getId(), branch, savedCommit.getCommitId()))
                            // 3. 关联 Process Log
                            .then(linkProcess(processId, savedCommit.getCommitId()))
                            .thenReturn(savedCommit.getCommitId())
                    );
            });
    }

    // 开启一个业务过程 (Process Context)
    public Mono<String> startProcess(String processName, String operator) {
        String processId = UUID.randomUUID().toString();
        String sql = "INSERT INTO lcm_process_commit_log (process_id, process_name, operator_id, started_at) VALUES (:pid, :name, :op, NOW())";
        return dbClient.sql(sql)
            .bind("pid", processId)
            .bind("name", processName)
            .bind("op", operator)
            .then()
            .thenReturn(processId);
    }

    // 关联 Process 和 Commit
    private Mono<Void> linkProcess(String processId, Long commitId) {
        if (processId == null || processId.isBlank()) return Mono.empty();

        String sql = "INSERT INTO lcm_process_entity_map (process_id, commit_id) VALUES (:pid, :cid)";
        return dbClient.sql(sql)
            .bind("pid", processId)
            .bind("cid", commitId)
            .then();
    }

    // 解析 JSON 并保存索引
    private Mono<Void> createIndex(Long commitId, String jsonContent) {
        return Mono.fromCallable(() -> {
            JsonNode root = objectMapper.readTree(jsonContent);
            List<LcmEntitySearchIndex> indices = new ArrayList<>();

            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                JsonNode val = field.getValue();

                if (val.isTextual()) {
                    indices.add(LcmEntitySearchIndex.builder()
                        .commitId(commitId).propName(key).valText(val.asText()).build());
                } else if (val.isNumber()) {
                    indices.add(LcmEntitySearchIndex.builder()
                        .commitId(commitId).propName(key).valNum(new BigDecimal(val.asText())).build());
                }
            }
            return indices;
        }).flatMapMany(indexRepo::saveAll).then();
    }

    // 创建实体实例 (UUID)
    private Mono<LcmEntityInstance> createEntity(String type, String slug) {
        LcmEntityInstance entity = new LcmEntityInstance();
        entity.setId(UUID.randomUUID().toString());
        entity.setEntityType(type);
        entity.setSlug(slug);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setNewEntity(true);
        return entityRepo.save(entity);
    }

    // 移动 Branch Head 指针 (Upsert)
    private Mono<Void> updateBranchHead(String entityId, String branch, Long newCommitId) {
        String sql = """
            INSERT INTO lcm_entity_branch_head (entity_id, branch_name, head_commit_id, updated_at)
            VALUES (:eid, :branch, :cid, NOW())
            ON CONFLICT (entity_id, branch_name) 
            DO UPDATE SET head_commit_id = :cid, updated_at = NOW()
        """;
        return dbClient.sql(sql)
            .bind("eid", entityId)
            .bind("branch", branch)
            .bind("cid", newCommitId)
            .then();
    }

    // 辅助方法：解析 JSON (供 search 调用)
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isBlank()) {
            log.warn("Attempted to parse NULL or Blank JSON snapshot.");
            return Collections.emptyMap();
        }
        try {
            return (Map<String, Object>) objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse snapshot JSON: " + json, e);
            return Collections.emptyMap();
        }
    }
}