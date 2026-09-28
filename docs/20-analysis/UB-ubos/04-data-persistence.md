---
id: ANA-UB-04
title: "UB Data & Persistence"
status: complete
phase: P1
depends_on: [ANA-UB-02]
sources: [UB]
---

# UB — Data & Persistence

## Schema v2.1 ("BPU Core Schema — Unified with Tenant Support")

Header principles: "Everything is an Entity" · "Git for Data" · "Audit by Default" · "Billing
Ready"; high-volume tables partitioned by time range.

| Table | Key columns | Notes |
|---|---|---|
| `bpu_entity_instance` | `id` PK, `tenant_id`, `entity_type`, `slug`, `status`, `terminator_commit_id`, `display_name`, `tags TEXT[]`, `owner_id`, `visibility`, timestamps; UNIQUE(tenant, type, slug) | partial index on ACTIVE; list index by last modified; GIN on tags; owner index |
| `bpu_entity_version_chain` | `commit_id` identity, `entity_id`, `branch_name`, `parent_commit_id`, `snapshot_data JSONB`, `embedding vector(1536)`, `author_id`, `message`, `committed_at`; PK(commit_id, committed_at) | **partitioned by year**; GIN on JSONB; **ivfflat cosine index** on embedding |
| `bpu_entity_branch_head` | PK(`tenant_id`, `entity_id`, `branch_name`), `head_commit_id`, **`seq_num`**, `updated_at` | branch height / optimistic version |
| `bpu_entity_search_index` | `commit_id`, `prop_name`, `val_text`, `val_num`, `val_date` | partitioned; (prop, text) index |
| `bpu_process_commit_log` | `process_id`, `process_name`, `operator_id`, **`app_id`**, **`billing_commit_id`**, `started_at` | partitioned |
| `bpu_process_entity_map` | `process_id`, `commit_id`, `created_at` | partitioned |

### CON-UB-040 — Six-table, partitioned, AI-ready physical model
- **What:** A fixed set of six tables holds all tenants, types and history; JSONB snapshots with GIN; vector embeddings per version; yearly range partitions for version, index, process and map tables; `CREATE EXTENSION vector`.
- **Why (intent):** D-UB-12 — full snapshots for O(1) historical reads; "the store is natively a vector database for RAG"; partitions for archiving; billing linkage at the process level.
- **Sources:** [UB:backend/src/main/resources/db/migration/V1__init_schema.sql], [UB:docker-compose.yml] (pgvector image).
- **Tags:** persistence, schema, ai, multi-tenancy

### CON-UB-041 — Branch height (`seq_num`) and head CAS
- **What:** Optimistic concurrency is per (entity, branch): `seq_num` is the branch height; CAS updates compare the expected parent/sequence.
- **Why (intent):** D-UB-12 — version numbers belong to branches ("block height per parallel universe"); locks never block other branches.
- **Sources:** [UB:backend/src/main/resources/db/migration/V1__init_schema.sql], [UB:backend/src/main/java/com/ubos/bpu/repository/EntityBranchHeadRepository.java].
- **Tags:** versioning, persistence

### CON-UB-042 — Automatic typed search index at commit
- **What:** Top-level snapshot fields are flattened into `bpu_entity_search_index` (text / number / date) during every commit; `SearchCriteria` (typed filters EQ/GT/LT/BETWEEN/LIKE/IN over metadata and indexed properties, sort, paging) queries identities.
- **How:** `CommitHandler.extractIndexRows`; `EntitySearchHandler`; design adds an EAV **inheritance index** (`sys_code`, `_extends`) with recursive CTE to find all descendants of a type (D-UB-09 tail).
- **Sources:** [UB:backend/src/main/java/com/ubos/bpu/handler/CommitHandler.java], [UB:backend/src/main/java/com/ubos/ese/handler/EntitySearchHandler.java], [UB:backend/src/main/java/com/ubos/core/dto/SearchCriteria.java].
- **Tags:** search, query

### CON-UB-043 — Process log with app and billing linkage
- **What:** Each business process records the initiating application (`app_id`) and a pointer to a billing batch entity commit (`billing_commit_id`).
- **Why (intent):** "Billing Ready" — usage metering per app/tenant derived from the audit log.
- **Sources:** [UB:backend/src/main/resources/db/migration/V1__init_schema.sql].
- **Tags:** process, observability, finance

### CON-UB-044 — Tombstones, lifecycle status, owner vs author
- **What:** Delete = tombstone commit + status change with `terminator_commit_id`; status ACTIVE/DELETED/ARCHIVED/LOCKED governs visibility/writability; `owner_id` (custody, transferable) is distinct from commit `author_id` (history).
- **Sources:** [UB:docs/database_strong.md] (D-UB-12), schema V1.
- **Tags:** versioning, permission, entity

### CON-UB-045 — Cache raw fragments, assemble just in time (design)
- **What:** Cache each entity's own JSON (immutable `JsonNode`), not assembled objects; assemble per request; a parent change invalidates only the parent.
- **Sources:** [UB:docs/design.md] (D-UB-04).
- **Tags:** cache, rehydration

### CON-UB-046 — Context export/import archives
- **What:** All heads of a context can be exported as per-entity archive files (identity + snapshot) and imported into another context in one transaction with ordering by dependency priority; HTTP export/import per context slug.
- **Why (intent):** D-UB-15 — "data is application": exporting entities migrates an application between environments.
- **Sources:** [UB:backend/src/main/java/com/ubos/boot/KernelManagerService.java], [UB:backend/src/main/java/com/ubos/web/controller/DataController.java].
- **Tags:** integration, deployment, versioning

### CON-UB-047 — Snapshot + delta hybrid and micro-commit squash (design)
- **What:** Realtime edits send path/value deltas; backend applies them to the head (with optimistic check) and stores the full snapshot *and* the delta; micro-commits may be squashed later.
- **Sources:** [UB:docs/动态视图渲染器.md] (D-UB-13).
- **Tags:** versioning, persistence

## Scalability notes recorded in the corpus (D-UB-12)

Hot `branch_head` rows (optimistic retry, advisory locks, queue-based async commits);
schema validation to prevent JSONB drift; HNSW preferred to ivfflat, async embeddings in a
separate table; automated partition management; monthly partitions and cold archive for the
version chain (~365 M rows/year at 1 M users); PostgreSQL row-level security per tenant;
dedicated partitions/shards for large tenants.
