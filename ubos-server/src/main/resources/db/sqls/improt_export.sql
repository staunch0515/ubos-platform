-- ==================================================================================
-- 1. 注册逻辑实体 (Logic): 数据导出
-- [Fix] 移除了 version_seq 字段
-- ==================================================================================
INSERT INTO bpu_entity_instance (id, tenant_id, entity_type, slug, display_name, status)
VALUES ('logic_export_001', 'sys_boot', 'sys.logic', 'sys.logic.ops.export', 'Export Data Logic', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO bpu_entity_version_chain (entity_id, branch_name, snapshot_data, author_id, message)
VALUES ('logic_export_001', 'main',
jsonb_build_object(
  'type', 'sys.logic',
  'lang', 'groovy',
  'content', '
    def branch = ctx.param(''branch'', ''main'')
    def chains = ctx.loadAllHeads(branch)

    def result = chains.collect { chain ->
       def snap = ctx.json(chain.getSnapshotData().asString())
       if (!snap.has(''id'')) {
           // snap.put(''id'', chain.getEntityId()) // Optional
       }
       return snap
    }

    return [
      count: result.size(),
      branch: branch,
      data: result
    ]
  '
), 'system', 'Init Export Logic');

INSERT INTO bpu_entity_branch_head (tenant_id, entity_id, branch_name, head_commit_id, seq_num)
SELECT 'sys_boot', 'logic_export_001', 'main', MAX(commit_id), 1 FROM bpu_entity_version_chain WHERE entity_id = 'logic_export_001'
ON CONFLICT (tenant_id, entity_id, branch_name) DO UPDATE SET head_commit_id = EXCLUDED.head_commit_id;


-- ==================================================================================
-- 2. 注册动作实体 (Action): 数据导出
-- [Fix] 移除了 version_seq 字段
-- ==================================================================================
INSERT INTO bpu_entity_instance (id, tenant_id, entity_type, slug, display_name, status)
VALUES ('action_export_001', 'sys_boot', 'sys.action', 'sys.action.ops.export', 'Export Action', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO bpu_entity_version_chain (entity_id, branch_name, snapshot_data, author_id, message)
VALUES ('action_export_001', 'main',
'{
  "type": "sys.action",
  "handler": "ubos://sys_boot/sys.logic.ops.export",
  "params": {
    "branch": "string"
  }
}'::jsonb, 'system', 'Init Export Action');

INSERT INTO bpu_entity_branch_head (tenant_id, entity_id, branch_name, head_commit_id, seq_num)
SELECT 'sys_boot', 'action_export_001', 'main', MAX(commit_id), 1 FROM bpu_entity_version_chain WHERE entity_id = 'action_export_001'
ON CONFLICT (tenant_id, entity_id, branch_name) DO UPDATE SET head_commit_id = EXCLUDED.head_commit_id;


-- ==================================================================================
-- 3. 注册逻辑实体 (Logic): 数据导入
-- [Fix] 移除了 version_seq 字段
-- ==================================================================================
INSERT INTO bpu_entity_instance (id, tenant_id, entity_type, slug, display_name, status)
VALUES ('logic_import_001', 'sys_boot', 'sys.logic', 'sys.logic.ops.import', 'Import Data Logic', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO bpu_entity_version_chain (entity_id, branch_name, snapshot_data, author_id, message)
VALUES ('logic_import_001', 'main',
jsonb_build_object(
  'type', 'sys.logic',
  'lang', 'groovy',
  'content', '
    import com.ubos.bpu.core.dto.*

    def dataset = ctx.param(''data'')
    def targetBranch = ctx.param(''branch'', ''main'')
    def tenant = ctx.getTenant()

    if (dataset == null || dataset.isEmpty()) {
        return "No data to import"
    }

    List<EntityChange> changes = []

    dataset.each { item ->
        def type = item.get(''type'') ?: ''sys.dynamic''
        def slug = item.get(''slug'') ?: java.util.UUID.randomUUID().toString()

        def identity = ctx.newId(slug)
        identity.setEntityType(type)
        if (item.get(''id'')) identity.setEntityId(item.get(''id''))

        def change = new EntityChange()
        change.setIdentity(identity)
        change.setCommitMessage(''Imported via UBOS Ops'')
        change.setSnapshotData(ctx.json(jsonUtils.writeValueAsString(item)))

        changes.add(change)
    }

    def commitCtx = CommitContext.builder()
        .authorId(ctx.getUserId())
        .appId(''SYS_OPS_IMPORT'')
        .processName(''Batch Import Process'')
        .build()

    def result = kernel.commitTransaction(tenant, targetBranch, commitCtx, changes).block()

    return [
       status: ''success'',
       txId: result.getTransactionId(),
       count: result.getNewHeadCommits().size()
    ]
  '
), 'system', 'Init Import Logic');

INSERT INTO bpu_entity_branch_head (tenant_id, entity_id, branch_name, head_commit_id, seq_num)
SELECT 'sys_boot', 'logic_import_001', 'main', MAX(commit_id), 1 FROM bpu_entity_version_chain WHERE entity_id = 'logic_import_001'
ON CONFLICT (tenant_id, entity_id, branch_name) DO UPDATE SET head_commit_id = EXCLUDED.head_commit_id;


-- ==================================================================================
-- 4. 注册动作实体 (Action): 数据导入
-- [Fix] 移除了 version_seq 字段
-- ==================================================================================
INSERT INTO bpu_entity_instance (id, tenant_id, entity_type, slug, display_name, status)
VALUES ('action_import_001', 'sys_boot', 'sys.action', 'sys.action.ops.import', 'Import Action', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO bpu_entity_version_chain (entity_id, branch_name, snapshot_data, author_id, message)
VALUES ('action_import_001', 'main',
'{
  "type": "sys.action",
  "handler": "ubos://sys_boot/sys.logic.ops.import",
  "params": {
    "data": "array",
    "branch": "string"
  }
}'::jsonb, 'system', 'Init Import Action');

INSERT INTO bpu_entity_branch_head (tenant_id, entity_id, branch_name, head_commit_id, seq_num)
SELECT 'sys_boot', 'action_import_001', 'main', MAX(commit_id), 1 FROM bpu_entity_version_chain WHERE entity_id = 'action_import_001'
ON CONFLICT (tenant_id, entity_id, branch_name) DO UPDATE SET head_commit_id = EXCLUDED.head_commit_id;