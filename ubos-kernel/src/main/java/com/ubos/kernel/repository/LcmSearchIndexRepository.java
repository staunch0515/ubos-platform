package com.ubos.kernel.repository;

import com.ubos.kernel.model.LcmEntitySearchIndex;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LcmSearchIndexRepository extends R2dbcRepository<LcmEntitySearchIndex, Long> {
    // R2DBC 会自动实现基本的 CRUD
}