---
id: ANA-US-04
title: "US Data & Persistence"
status: complete
phase: P1
depends_on: [ANA-UC-04]
sources: [US]
---

# US — Data & Persistence

### CON-US-040 — Same six-table schema and store code as UC
- **What:** `docs/db/V1_init.sql` is byte-identical to UC's (schema v2.1, same as UB); `ubos_store` = UC `infra/store` (CON-UC-040…046) with one change: search matches `snapshot_data->>'name'` **or** `slug` (ILIKE) [US:crates/ubos_store/src/reader/search.rs].
- **Tags:** persistence, schema

### CON-US-041 — Genesis data deltas
- **What:** Files 01–15 as UC plus `ui_modes` on every item (CON-US-015); `13_kernel_instances` still duplicates 01 (differs only by `ui_modes` on `integer`); new 16 (Action, StatefulAction, TwoStepAction, act_universal_editor + 2 logics) and 17 (act_logic_maintain + 2 logics); `init_data/01_meta_type.json` (root with read-only `id`, generic `Entity` view type). Scripts in 16/17 use the 3-argument `sys_db_commit` and `ubos://` reads.
- **Sources:** [US:docs/db/genesis_data/], [US:docs/db/init_data/01_meta_type.json].
- **Tags:** boot, metadata, domain-pack

### CON-US-042 — Development vs production database (design)
- **What:** Whitepaper: SQLite (`mode=rwc`, local file) for development, PostgreSQL for enterprise; sqlx raw SQL for both. `console.md` shows the actual setup: `pgvector/pgvector:pg15` + `redis:alpine` in Docker.
- **Sources:** [US:docs/front/UBOS.md], [US:console.md].
- **Tags:** persistence, deployment

### CON-US-043 — Append-only verification as a scenario
- **What:** CLI scripts force payload changes and ask the operator to observe new rows in `bpu_entity_version_chain` (`test_append_only.rhai`, `step1_v2_force_update.rhai`) — immutability as a checked property.
- **Tags:** versioning, observability
