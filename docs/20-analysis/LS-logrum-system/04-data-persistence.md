---
id: ANA-LS-04
title: "LS Data & Persistence"
status: complete
phase: P1
depends_on: [ANA-LS-02]
sources: [LS]
---

# LS — Data & Persistence

### CON-LS-040 — The six-table schema in an SQLite dialect
- **What:** `docs/db/sqlite/V1_init.sql` — schema v2.1 (same tables as UB/UC/US) adapted: JSONB → TEXT, vector → BLOB, no partitions, tag arrays → JSON text, `INTEGER PRIMARY KEY AUTOINCREMENT` commit ids; `clear.sql` resets.
- **Why (intent):** The same logical model on a local file database for personal/edge editions.
- **Sources:** [LS:docs/db/sqlite/V1_init.sql].
- **Tags:** persistence, schema, deployment

### CON-LS-041 — Atomic multi-entity commit pipeline
- **What:** `commit_changes(ctx, changes)` in one transaction: (1) process log (`INSERT OR IGNORE`, name `ATOMIC_SAVE`, operator = user, app id) → per change: (2) instance upsert by locator (reuse id or new UUID; `owner_id` = user) → (3) version insert (branch from change or workspace, `author_id`, message) → (4) branch head upsert with `seq_num+1` → (5) search index from payload → (6) lineage `OUTPUT` → commit [LS:crates/ubos_store/src/engine/writer.rs#L19-L77], [LS:crates/ubos_store/src/backend/sqlite/].
- **Why (intent):** The single write path ("only commit_changes"): every change audited, versioned, indexed and linked to its process atomically.
- **Tags:** persistence, versioning, observability

### CON-LS-042 — Search index populated on every commit
- **What:** Top-level scalar properties of the payload become rows `(commit_id, prop_name, val_text | val_num)` — with relation-URI keys, the index is keyed by relation type [LS:crates/ubos_store/src/backend/sqlite/index.rs].
- **Why (intent):** Implements the typed search index designed in UB (CON-UB-042).
- **Tags:** search, query

### CON-LS-043 — One generic reader by criteria
- **What:** `EntityReaderOps::find_entities(pool, EntityCriteria{tenant_id?, entity_types?, slug_pattern?, branch})` → `EntitySnapshot{tenant, entity_type, slug, head_commit_id, snapshot}` — "no load_types, load_perms… only find_entities"; `load_entity_head(tenant, type, slug)` = locator → id → head commit → snapshot → `Entity{…, version}` [LS:crates/ubos_store/src/ops/reader.rs], [LS:crates/ubos_store/src/engine/reader.rs].
- **Tags:** query, persistence

### CON-LS-044 — Immutable versions enable permanent caches
- **What:** See CON-LS-007; commit ids are the cache key and the URI query parameter `commit`.
- **Tags:** cache, versioning
