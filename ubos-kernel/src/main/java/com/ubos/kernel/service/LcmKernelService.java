package com.ubos.kernel.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubos.kernel.model.LcmEntityInstance;
import com.ubos.kernel.model.LcmEntitySearchIndex;
import com.ubos.kernel.model.LcmEntityVersionChain;
import com.ubos.kernel.repository.LcmEntityRepository;
import com.ubos.kernel.repository.LcmSearchIndexRepository;
import com.ubos.kernel.repository.LcmVersionRepository;
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
    // 【修复1】 补全缺失的 Repository 字段
    private final LcmSearchIndexRepository indexRepo;
    private final DatabaseClient dbClient;
    private final ObjectMapper objectMapper;

    /**
     * 【读】获取资源的最新快照
     */
    public Mono<String> getResourceSnapshot(String type, String slug, String branch) {
        return entityRepo.findByEntityTypeAndSlug(type, slug)
            .flatMap(entity -> versionRepo.findHeadSnapshot(entity.getId(), branch))
            .map(LcmEntityVersionChain::getSnapshotData);
    }

    /**
     * 【新增】时光机：根据 CommitID 获取历史快照
     */
    public Mono<String> getSnapshotByCommit(Long commitId) {
        return versionRepo.findById(commitId)
            .map(LcmEntityVersionChain::getSnapshotData);
    }

    /**
     * 【新增】通用搜索接口
     */
    public Flux<Map<String, Object>> search(String type, String branch, Map<String, Object> filters) {
        if (filters.isEmpty()) {
            return Flux.empty();
        }

        String propName = filters.keySet().iterator().next();
        Object propValue = filters.get(propName);

        String sql = """
            SELECT v.snapshot_data
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
                String json = row.get("snapshot_data", String.class);
                try {
                    // 【修复2】 使用 TypeReference 确保泛型匹配 Map<String, Object>
                    // 或者简单地进行强转
                    return (Map<String, Object>) objectMapper.readValue(json, Map.class);
                } catch (Exception e) {
                    return Map.<String, Object>of();
                }
            })
            .all();
    }

    /**
     * 【写】提交变更 (Commit)
     */
    @Transactional
    public Mono<Long> commit(String type, String slug, String branch, String jsonContent, String author, String msg) {
        return entityRepo.findByEntityTypeAndSlug(type, slug)
            .switchIfEmpty(createEntity(type, slug))
            .flatMap(entity -> {
                return versionRepo.findHeadSnapshot(entity.getId(), branch)
                    .map(LcmEntityVersionChain::getCommitId)
                    .defaultIfEmpty(0L)
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
                    .flatMap(savedCommit ->
                        // 保存成功后，立即建立索引
                        createIndex(savedCommit.getCommitId(), jsonContent)
                            .then(updateBranchHead(entity.getId(), branch, savedCommit.getCommitId()))
                            .thenReturn(savedCommit.getCommitId())
                    );
            });
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

    private Mono<LcmEntityInstance> createEntity(String type, String slug) {
        LcmEntityInstance entity = new LcmEntityInstance();
        entity.setId(UUID.randomUUID().toString());
        entity.setEntityType(type);
        entity.setSlug(slug);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setNewEntity(true);
        return entityRepo.save(entity);
    }

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
}