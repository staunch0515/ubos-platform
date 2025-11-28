package com.ubos.kernel.repository;

import com.ubos.kernel.model.LcmEntityVersionChain;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface LcmVersionRepository extends R2dbcRepository<LcmEntityVersionChain, Long> {

    // 【核心查询】 给定实体ID和分支名，通过 Head 指针找到最新的快照
    @Query("""
        SELECT v.* FROM lcm_entity_version_chain v
        JOIN lcm_entity_branch_head h ON v.commit_id = h.head_commit_id
        WHERE h.entity_id = :entityId AND h.branch_name = :branchName
    """)
    Mono<LcmEntityVersionChain> findHeadSnapshot(String entityId, String branchName);
}