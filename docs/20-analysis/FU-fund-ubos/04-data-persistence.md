---
id: ANA-FU-04
title: "FU Data & Persistence"
status: complete
phase: P1
depends_on: [ANA-FU-02, ANA-UP-04]
sources: [FU]
---

# FU — Data & Persistence

## Schema delta vs UP

| Migration | Change |
|---|---|
| V1 | `lcm_entity_instance.tenant_id VARCHAR(64) NOT NULL` added (tenant-aware code also upserts heads on `(tenant_id, entity_id, branch_name)`) |
| V2–V5 | identical to UP |
| V6 | new `sys_environment_config` (CON-FU-031) seeded with DEV/UAT/STAGING/PROD → `master` |

All governance state lives in the existing version tables (CON-FU-001).

### CON-FU-040 — Branch diff by comparing head pointers
- **What:** The difference between two branches is computed from heads only, without reading snapshots.
- **How:** CTEs `cur` and `base` over `lcm_entity_branch_head`, `FULL OUTER JOIN` on entity: base missing → `NEW`, current missing → `DELETED`, head ids differ → `MODIFIED`. Endpoint `/api/console/status/diff`; UI `BranchStatusViewer`.
- **Why (intent):** Commit ids act as content identity; comparing pointers is O(#heads).
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L1019-L1062].
- **Tags:** versioning, query

### CON-FU-041 — Field-level blame in SQL
- **What:** For each commit of an entity, list which top-level JSON keys changed (added, modified, removed).
- **How:** Window `LAG(snapshot_data::jsonb) OVER (PARTITION BY entity_id ORDER BY commit_id)`; keys where `prev IS NULL`, key absent in prev, or value `IS DISTINCT FROM`; plus keys removed; streamed as NDJSON from `/api/v1/history/{entityId}/blame`. Feeds the time-travel slider's blame mode.
- **Why (intent):** "Who changed this field, when" — audit at field granularity derived from full snapshots.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmHistoryService.java], [FU:backend/src/main/java/org/logrum/ubos/web/console/LcmHistoryController.java].
- **Tags:** versioning, observability, query

### CON-FU-042 — Global search (index text + slug)
- **What:** Search across all heads by indexed text value or slug pattern.
- **How:** `searchFullText(q, branch?, type?, limit)`: `lcm_entity_search_index.val_text ILIKE %q%` joined to heads, returns matched field/value + snapshot; `searchBySlug(pattern, branch?, limit)`. (Index population still a stub as in UP.) Endpoint `/api/console/search`.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L321-L437].
- **Tags:** search, query

### CON-FU-043 — Process audit queries
- **What:** Process log queries extended: process info, commit count per process, processes by operator, per-type and per-branch entity history.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmAuditService.java].
- **Tags:** process, observability

### CON-FU-044 — Operational metrics derived from the version store
- **What:** Health and activity metrics are SQL aggregates over kernel tables and cache counters.
- **How:** DB up (`SELECT 1`), entity count, commit count; commits last 24 h; commits per entity type; average seconds between commits (`LAG` over `committed_at`); cache hit ratio from kernel counters; workflow queue status (pending approvals). UI `OperationalDashboard` (cards: System Status, Workflow Health, Cache Performance, Commits, Commits by Entity Type).
- **Why (intent):** The commit log is also the telemetry source.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmMetricsService.java], [FU:frontend/src/components/OperationalDashboard.tsx].
- **Tags:** observability

## Caches (summary)

| Cache | Key | TTL / invalidation |
|---|---|---|
| Snapshot cache | `tenant::uri` | none (manual evict) |
| Schema cache | entity type | 5 min; evicted on SCHEMA commit; manual clear endpoint |
| API key cache | key hash | 5 min; evicted on revoke |
| Script class cache | content hash | (UP) |
