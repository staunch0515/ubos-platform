---
id: ANA-UC-04
title: "UC Data & Persistence"
status: complete
phase: P1
depends_on: [ANA-UC-02, ANA-UC-03]
sources: [UC]
---

# UC — Data & Persistence

## Schema

`docs/db/V1_init.sql` is **the same schema v2.1 as UB** ("BPU Core Schema — Unified with Tenant
Support"; only whitespace differs) and byte-identical to US: `bpu_entity_instance`,
`bpu_entity_version_chain` (partitioned, JSONB + `vector(1536)`), `bpu_entity_branch_head`
(`seq_num`), `bpu_entity_search_index`, `bpu_process_commit_log` (app/billing),
`bpu_process_entity_map` (`relation_type`). The full description is CON-UB-040 … CON-UB-044;
this file records **how UC uses it**.

| Column / table | Used by UC code |
|---|---|
| instance `id, tenant_id, entity_type, slug, status, created_at, last_modified_at` | yes |
| instance `terminator_commit_id, display_name, tags, owner_id, visibility` | no |
| version `commit_id, entity_id, branch_name, snapshot_data` | yes |
| version `parent_commit_id, embedding, author_id, message` | no |
| head `tenant_id, entity_id, branch_name, head_commit_id, seq_num, updated_at` | yes |
| search index | no |
| process log `process_id, process_name, operator_id, app_id, started_at` | yes (`billing_commit_id` no) |
| process map `process_id, commit_id, relation_type, created_at` | yes (OUTPUT, DEPENDS_ON_CTX, DEPENDS_ON_FUNC) |

### CON-UC-040 — "Six tables only" as a hard design rule
- **What:** No feature may add tables; every new concept is an entity type.
- **How:** Rule in `prompt.md`; all features (jobs, workspaces, crons, hooks, files, policies, views) are rows in the same tables.
- **Why (intent):** "Everything is an entity" enforced physically; one audit, versioning and search mechanism for all.
- **Sources:** [UC:docs/prompt.md], [UC:docs/db/V1_init.sql].
- **Tags:** persistence, schema, meta-model

### CON-UC-041 — Atomic write unit: instance upsert → version insert → head upsert
- **What:** One entity write = three statements inside a caller's transaction.
- **How:** `commit_transaction_atomically(tx, entity, default_branch)`: branch = `payload.branch` or default; upsert instance `ON CONFLICT uq_instance_locator` (reactivate, touch); insert version returning `commit_id`; upsert head, `seq_num+1` [UC:src/infra/store/writer/atomic.rs#L8-L76]. `save()` first looks up the locator and reuses the stored UUID to prevent "ID split", then writes a `manual_save` process + `OUTPUT` map in the same transaction [UC:src/infra/store/writer/manual.rs#L9-L50].
- **Why (intent):** Every write is versioned and audited even outside the job pipeline; locator uniqueness is the business key.
- **Tags:** persistence, versioning

### CON-UC-042 — Process log + typed process→commit map as the lineage graph
- **What:** Processes (who/what/when) link to commits with a relation type.
- **How:** `OUTPUT` for produced versions, `DEPENDS_ON_CTX` / `DEPENDS_ON_FUNC` for consumed versions (CON-UC-021); `sys_db_history` joins versions with map timestamps [UC:src/kernel/syscalls/db/reader/history.rs#L34-L44].
- **Why (intent):** Queryable data lineage ("previous life and present life of a process") (D-UC-45).
- **Tags:** observability, versioning, process

### CON-UC-043 — Redis roles: queue, event bus, environment cache
- **What:** Redis holds only transient/derived state; Postgres is the source of truth.
- **How:** List `ubos_task_queue` (job ids), list `ubos_event_bus` (JSON events), key `ubos:workspace:<id>` (Workspace JSON, `SETEX` 3600 s) [UC:src/kernel/workspace_manager.rs#L19-L61].
- **Why (intent):** D-UC-05/23 — decoupling, and "Logrum as hot data in Redis, not just a row in the DB" (session state with TTL).
- **Design note:** memory lives only in Redis between runs (not versioned); a durable memory would need a Workspace commit.
- **Tags:** cache, events, scheduling

### CON-UC-044 — Virtual file system as entities
- **What:** Files are `File` entities: `{filename, content, size, mime_type}`; content in JSONB (Base64 for binary in design).
- **How:** `sys_file_save(name, content)` → UUID; `sys_file_read(uuid)` [UC:src/kernel/syscalls/fs.rs#L9-L60]. Design: move to S3/MinIO with a key in the entity for large or enterprise use.
- **Why (intent):** D-UC-18 — scripts handle unstructured data (reports, images, PDFs) with the same versioning and audit.
- **Tags:** persistence, document

### CON-UC-045 — Branch support is structural but main-only in practice
- **What:** Schema and `__anchors.branch` carry branches; readers query `branch_name = 'main'`; a write goes to `payload.branch` if present.
- **How:** [UC:src/infra/store/reader/fetch.rs#L10-L55], [UC:src/infra/store/writer/atomic.rs#L14-L16]. Workspace has `default_branch`; UI injects `X-Branch`; Plan_17 asks the UI to show the current branch and warn "this change will be saved to `{branch}`".
- **Why (intent):** Branch = environment dimension chosen with the Workspace; merge semantics are not designed in UC (compare UB CON-UB-023).
- **Tags:** versioning, context

### CON-UC-046 — Search: name matching today, JSONB containment in design
- **What:** `store.search(tenant, type, query)` → `snapshot_data->>'name' ILIKE %q%`, newest first, limit 50; `scan_by_type` for system scans [UC:src/infra/store/reader/search.rs#L10-L41]. Design stage 15: `sys_db_query(type, filter_map)` using JSONB `@>` containment (e.g. "all users with balance > 100" discussed).
- **Tags:** search, query
