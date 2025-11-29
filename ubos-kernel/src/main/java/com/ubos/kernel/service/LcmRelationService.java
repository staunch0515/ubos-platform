package com.ubos.kernel.service;

import com.ubos.kernel.repository.LcmEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class LcmRelationService {

    private final DatabaseClient dbClient;
    private final LcmEntityRepository entityRepository; // 保持依赖，但不再用于查询ID

    /**
     * 【核心写入】创建实体间关系 (Link)
     */
    public Mono<Void> createRelation(String sourceSlug, String targetSlug, String relationType) {

        // 1. 【修复】分别查找并抛出明确异常，便于调试
        // 使用 row.get(..., String.class) 避免 UUID 强转 String 的潜在 ClassCastException
        Mono<String> sourceIdMono = dbClient.sql("SELECT id FROM lcm_entity_instance WHERE slug = :slug")
            .bind("slug", sourceSlug)
            .map((row, meta) -> row.get("id", String.class))
            .one()
            .switchIfEmpty(Mono.error(new RuntimeException("Source entity not found: " + sourceSlug)));

        Mono<String> targetIdMono = dbClient.sql("SELECT id FROM lcm_entity_instance WHERE slug = :slug")
            .bind("slug", targetSlug)
            .map((row, meta) -> row.get("id", String.class))
            .one()
            .switchIfEmpty(Mono.error(new RuntimeException("Target entity not found: " + targetSlug)));

        // 2. 合并查找结果
        return Mono.zip(sourceIdMono, targetIdMono)
            .flatMap(tuple -> {
                String sql = """
                        INSERT INTO lcm_entity_relation (source_entity_id, target_entity_id, relation_type)
                        VALUES (:srcId, :tgtId, :relType)
                    """;
                return dbClient.sql(sql)
                    .bind("srcId", tuple.getT1())
                    .bind("tgtId", tuple.getT2())
                    .bind("relType", relationType)
                    .then();
            });
    }

    /**
     * 【核心查询】获取某个实体下的所有关联实体 (Graph Traversal)
     */
    public Flux<String> getRelatedEntities(String sourceSlug, String relationType) {

        String sql = """
            SELECT T.slug 
            FROM lcm_entity_relation R
            JOIN lcm_entity_instance S ON R.source_entity_id = S.id
            JOIN lcm_entity_instance T ON R.target_entity_id = T.id
            WHERE S.slug = :srcSlug AND R.relation_type = :relType
        """;

        return dbClient.sql(sql)
            .bind("srcSlug", sourceSlug)
            .bind("relType", relationType)
            .map((row, meta) -> row.get("slug", String.class))
            .all()
            .filter(Objects::nonNull)
            .distinct()
            .onErrorResume(e -> {
                log.error("Failed to query relations.", e);
                return Flux.empty();
            });
    }
}