package com.ubos.kernel.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable; // 引入这个接口
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Data
@Table("lcm_entity_instance")
// 1. 实现 Persistable 接口
public class LcmEntityInstance implements Persistable<String> {
    @Id
    private String id;
    private String entityType;
    private String slug;
    private LocalDateTime createdAt;

    // 2. 增加一个临时标记，不存入数据库
    @Transient
    private boolean isNewEntity = false;

    // 3. 重写判断逻辑：如果是新实体标记为 true，或者 ID 为空，都算新数据
    @Override
    @Transient
    public boolean isNew() {
        return isNewEntity || id == null;
    }
}