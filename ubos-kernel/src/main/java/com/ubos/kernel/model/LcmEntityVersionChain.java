package com.ubos.kernel.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Data
@Builder
@Table("lcm_entity_version_chain")
public class LcmEntityVersionChain {
    @Id
    private Long commitId;
    private String entityId;
    private String branchName;
    private Long parentCommitId;
    private String snapshotData; // 存 JSON 字符串
    private String authorId;
    private String message;
    private LocalDateTime committedAt;
}