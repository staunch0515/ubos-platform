package com.ubos.kernel.service;

import com.ubos.kernel.model.LcmEntityInstance;
import com.ubos.kernel.repository.LcmEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class LcmRelationService {

    private final DatabaseClient dbClient;
    private final LcmEntityRepository entityRepository;

    /**
     * 【核心写入】创建实体间关系 (Link)
     * @param sourceSlug 源实体唯一标识 (如 user.007)
     * @param targetSlug 目标实体唯一标识 (如 order.xyz)
     * @param relationType 关系类型 (如 OWNS)
     */
    public Mono<Void> createRelation(String sourceSlug, String targetSlug, String relationType) {

        Mono<LcmEntityInstance> sourceMono = entityRepository.findByEntityTypeAndSlug("USER", sourceSlug); // 假设 USER 实体
        Mono<LcmEntityInstance> targetMono = entityRepository.findByEntityTypeAndSlug("ORDER", targetSlug); // 假设 ORDER 实体

        return Mono.zip(sourceMono, targetMono)
            .flatMap(tuple -> {
                String sql = """
                        INSERT INTO lcm_entity_relation (source_entity_id, target_entity_id, relation_type)
                        VALUES (:srcId, :tgtId, :relType)
                    """;
                return dbClient.sql(sql)
                    .bind("srcId", tuple.getT1().getId())
                    .bind("tgtId", tuple.getT2().getId())
                    .bind("relType", relationType)
                    .then();
            })
            .switchIfEmpty(Mono.error(new RuntimeException("Source or Target entity not found for relation.")));
    }

    /**
     * 【核心查询】获取某个实体下的所有关联实体 (Graph Traversal)
     * @param sourceSlug 源实体 Slug
     * @param relationType 关系类型
     * @return 关联实体的 Slug 列表
     */
    public Flux<String> getRelatedEntities(String sourceSlug, String relationType) {

        String sql = """
            SELECT T.slug 
            FROM lcm_entity_relation R
            -- ... (rest of SQL query) ...
            WHERE S.slug = :srcSlug AND R.relation_type = :relType
        """;

        return dbClient.sql(sql)
            .bind("srcSlug", sourceSlug)
            .bind("relType", relationType)
            .map((row, meta) -> row.get("slug", String.class))
            // 1. 关键修复：调用 .all() 将 RowsFetchSpec 转换为 Flux<String>
            .all()
            // 2. 现在 filter、distinct 都是 Flux 的方法，可以正确使用
            .filter(Objects::nonNull)
            .distinct()
            .onErrorResume(e -> {
                log.error("Failed to query relations.", e);
                return Flux.empty();
            });
    }
}