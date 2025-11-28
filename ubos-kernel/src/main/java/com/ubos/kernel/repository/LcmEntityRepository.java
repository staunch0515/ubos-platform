package com.ubos.kernel.repository;

import com.ubos.kernel.model.LcmEntityInstance;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface LcmEntityRepository extends R2dbcRepository<LcmEntityInstance, String> {
    // 根据业务Slug查找实体
    Mono<LcmEntityInstance> findByEntityTypeAndSlug(String entityType, String slug);
}