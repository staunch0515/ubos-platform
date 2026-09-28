---
id: ANA-LC-04
title: "LC Data & Persistence"
status: complete
phase: P1
depends_on: [ANA-LC-02]
sources: [LC]
---

# LC — Data & Persistence

## Schema (`data/database/init_db/000–011`)

| # | Table | Purpose (from SQL comments) |
|---|---|---|
| 000 | – | drop all `lcm_%` tables (reset script) |
| 001 | `lcm_entity_instance(id, app_id, entity_type, slug, is_deleted, created_at, updated_at, UNIQUE(app_id, entity_type, slug))` | "unique identity and metadata of entities" |
| 002 | `lcm_process_commit_log(process_log_id, app_id, process_name, process_version, operator_id, handler_versions JSONB, committed_at)` | "event log of each business operation, decoupled from entity versions" |
| 003 | `lcm_entity_version_chain(commit_id, entity_id FK, branch_name, parent_commit_id, commit_type, committed_at, process_log_id, operator_id)` | "version history chain" |
| 004 | `lcm_process_entity_map(process_log_id, commit_id)` | "links one business operation to all affected entity version snapshots" |
| 005 | `lcm_entity_attribute_value(commit_id, prop_name, val_text, val_num, val_bool, val_date, val_json JSONB, PK(commit_id, prop_name))` | "vertical attribute storage of version snapshots for efficient querying" |
| 006 | `lcm_entity_branch_head(entity_id, branch_name, head_commit_id)` | latest version pointer per branch |
| 007 | `lcm_process_idempotency` | CON-LC-026 |
| 008 | `lcm_verification_token(token_hash PK, entity_id, app_id, token_type, expires_at)` | hashed one-time tokens |
| 009 | `lcm_login_session(session_id, user_id, app_id, start_time, end_time, client_ip, user_agent, status ACTIVE/TERMINATED/EXPIRED)` | sessions |
| 010 | `lcm_request_log(...)` | CON-LC-003 |
| 011 | `lcm_email_dispatch_queue(...)` | CON-LC-027 |

### CON-LC-040 — Typed EAV snapshot per commit
- **What:** A version's content is stored as one row per property with typed value columns, not as a JSON blob.
- **How:** PK `(commit_id, prop_name)` ⇒ each commit holds the full property set; value routed by Java type (String → text, Number → num, Boolean → bool, other → JSONB; `val_date` for dates); indexes `(prop_name, val_text)` and `(prop_name, val_num)`. Named SQL queries use self-joins on the EAV table, e.g. `COUNT_ACTIVE_RELATIONSHIPS` joins `source_entity_id`, `type` and optional `end_date` rows with temporal validity `end_date IS NULL OR end_date > :now`; `CHECK_ATTRIBUTE` for uniqueness.
- **Why (intent):** SQL comment: "vertical storage for indexing and querying (e.g. find all entities with status='DRAFT')"; `docs/README.md`: "`commit_id` is the only connection point, no redundant `entity_id`."
- **Sources:** [LC:data/database/init_db/005_lcm_entity_attribute_value.sql], [LC:data/database/sql/COUNT_ACTIVE_RELATIONSHIPS.sql], [LC:src/main/java/com/logicorum/service/impl/DefaultAttributeValueMapper.java].
- **Tags:** persistence, schema, query

### CON-LC-041 — Process log records handler versions
- **What:** The business-operation log is separate from entity versions and records process version and (designed) the versions of the handlers that ran.
- **How:** `process_version`, `handler_versions JSONB`; version chain rows reference `process_log_id`; queries are logged too (`QUERY_` prefix).
- **Why (intent):** Reproduce *which logic* produced a change, not only *which data* changed.
- **Sources:** [LC:data/database/init_db/002_process_commit_log.sql], [LC:src/main/java/com/logicorum/service/impl/R2dbcQueryCommitService.java].
- **Tags:** process, versioning, observability

### CON-LC-042 — Commit types and soft delete
- **What:** Every version records `commit_type` (CREATE/UPDATE/DELETE); deletions set `is_deleted` on the identity and are rejected twice.
- **Sources:** [LC:src/main/java/com/logicorum/handler/PersistenceSaveHandlerV1.java].
- **Tags:** versioning, entity

### CON-LC-043 — Operational tables beside the version store
- **What:** High-churn operational data (idempotency, tokens, sessions, request log, email outbox) uses dedicated tables rather than versioned entities.
- **Why (intent):** Interpretation: these records are technical, high-volume and not business history; the design separates them from the versioned business graph.
- **Sources:** [LC:data/database/init_db/007..011].
- **Tags:** persistence, auth, observability

### CON-LC-044 — Dual audit: DB process log + JSON transaction files
- **What:** After persistence, a pretty-printed JSON file per process execution is written under `<log.storage.path>/<yyyyMMdd>/<processLogId>-<timestamp>.json` with process name, status, state and all change sets.
- **Sources:** [LC:src/main/java/com/logicorum/service/TransactionLogService.java].
- **Tags:** observability, versioning

### CON-LC-045 — Branch used as staging area per request
- **What:** The request chooses `targetBranchName` (engine default `draft`, query default `main`); change sets are written to that branch.
- **Why (intent):** Draft vs main data separation driven by the caller; no merge/inheritance logic in LC.
- **Sources:** [LC:src/main/java/com/logicorum/core/LogicExecutionEngine.java], [LC:data/processes/auth/auth_register_v1.process.json] (`targetBranchName` default `draft`).
- **Tags:** versioning

### CON-LC-046 — Caching strategy proposal (Redis)
- **What:** `docs/Redis.md` proposes five Redis uses for high concurrency: idempotency (`SET NX EX`), session validation cache (cache-aside), entity head snapshot cache `entity:{app}:{type}:{id}:head` invalidated on commit, distributed locks for wallets (Redisson), rate limiting (`INCR`+TTL); rollout order idempotency → sessions → locks.
- **Sources:** [LC:docs/Redis.md].
- **Tags:** cache, finance
