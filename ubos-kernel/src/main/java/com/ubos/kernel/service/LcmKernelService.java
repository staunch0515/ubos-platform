package com.ubos.kernel.service;

import com.ubos.kernel.model.LcmEntityInstance;
import com.ubos.kernel.model.LcmEntityVersionChain;
import com.ubos.kernel.repository.LcmEntityRepository;
import com.ubos.kernel.repository.LcmVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LcmKernelService {

    private final LcmEntityRepository entityRepo;
    private final LcmVersionRepository versionRepo;
    private final DatabaseClient dbClient; // 用于执行原生 SQL 更新 Head 指针

    /**
     * 【读】获取资源的最新快照
     * 策略：先找当前分支，没有则返回 Empty (由上层 Engine 处理降级逻辑)
     */
    public Mono<String> getResourceSnapshot(String type, String slug, String branch) {
        return entityRepo.findByEntityTypeAndSlug(type, slug)
            .flatMap(entity -> versionRepo.findHeadSnapshot(entity.getId(), branch))
            .map(LcmEntityVersionChain::getSnapshotData);
    }

    /**
     * 【写】提交变更 (Commit)
     */
    @Transactional
    public Mono<Long> commit(String type, String slug, String branch, String jsonContent, String author, String msg) {
        // 1. 获取或创建实体
        return entityRepo.findByEntityTypeAndSlug(type, slug)
            .switchIfEmpty(createEntity(type, slug))
            .flatMap(entity -> {
                // 2. 获取该分支当前的 Head Commit ID
                return versionRepo.findHeadSnapshot(entity.getId(), branch)
                    .map(LcmEntityVersionChain::getCommitId)
                    .defaultIfEmpty(0L) // 如果没有历史，默认用 0L 占位
                    // ❌ 删除原来的 .map(parentId -> parentId == 0L ? null : parentId)

                    // 3. 在 flatMap 内部处理 null 逻辑
                    .flatMap(parentId -> {
                        // 这里是普通 Java 代码块，可以使用 null
                        Long actualParentId = (parentId == 0L) ? null : parentId;

                        LcmEntityVersionChain newCommit = LcmEntityVersionChain.builder()
                            .entityId(entity.getId())
                            .branchName(branch)
                            .parentCommitId(actualParentId) // 使用处理后的 ID
                            .snapshotData(jsonContent)
                            .authorId(author)
                            .message(msg)
                            .committedAt(LocalDateTime.now())
                            .build();

                        return versionRepo.save(newCommit);
                    })
                    .flatMap(savedCommit ->
                        // 4. 更新 Branch Head 指针
                        updateBranchHead(entity.getId(), branch, savedCommit.getCommitId())
                            .thenReturn(savedCommit.getCommitId())
                    );
            });
    }

    private Mono<LcmEntityInstance> createEntity(String type, String slug) {
        LcmEntityInstance entity = new LcmEntityInstance();
        entity.setId(UUID.randomUUID().toString());
        entity.setEntityType(type);
        entity.setSlug(slug);
        entity.setCreatedAt(LocalDateTime.now());

        // 【新增这一行】告诉 R2DBC 这是一个新对象，请 Insert！
        entity.setNewEntity(true);

        return entityRepo.save(entity);
    }

    private Mono<Void> updateBranchHead(String entityId, String branch, Long newCommitId) {
        // Postgres Upsert 语法
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