---
id: ANA-UP-04
title: "UP Data & Persistence"
status: complete
phase: P1
depends_on: [ANA-UP-02]
sources: [UP]
---

# UP — Data & Persistence

## Physical schema (Flyway V1–V5)

| Migration | Table | Purpose (from SQL comments) |
|---|---|---|
| V1 | `lcm_entity_instance` | Entity registry: existence only |
| V1 | `lcm_entity_version_chain` | Git commit nodes (full JSON snapshot) |
| V1 | `lcm_entity_branch_head` | Git refs |
| V2 | `lcm_entity_search_index` | Property index per commit, "gives the system search capability" |
| V3 | `sys_branch_config` | Branch inheritance ("family tree"); seeds `master` (root) and `beijing → master` |
| V4 | `lcm_process_commit_log`, `lcm_process_entity_map` | Process log and process↔commit map |
| V5 | `lcm_entity_relation` | Typed edges between entities |

### CON-UP-040 — Full-snapshot storage (no deltas)
- **What:** Every commit stores the complete entity JSON.
- **How:** `snapshot_data TEXT NOT NULL -- JSON full data`. Reads never replay history.
- **Why (intent):** Simplicity; O(1) historical reads; supports time travel directly.
- **Sources:** [UP:ubos-server/src/main/resources/db/migration/V1__init_ubos_schema.sql].
- **Tags:** versioning, persistence

### CON-UP-041 — Branch hierarchy with recursive fallback resolution
- **What:** Branches form a tree; a read on a child branch that finds no head falls back to the parent branch, recursively up to `master`.
- **How:**
  - `sys_branch_config(branch_name PK, parent_branch, description)`; `master` has null parent; example `beijing → master` ("Beijing Branch inherits from Master").
  - `findInBranchRecursive(type, slug, branch)`: head on `branch` → else `parent_branch` (or `master` if none configured and branch ≠ master) → recurse. Logged as "Resource missing in [child], fallback to parent [parent]".
  - Commit `38cb9a7`: "1. Database: define the family tree (Branch Hierarchy) 2. Code: recursive lookup in the kernel layer".
- **Why (intent):** A branch is an **overlay**: a region/tenant/environment only stores what it overrides and inherits everything else (copy-on-write customization of logic, views and data).
- **Sources:** [UP:ubos-server/src/main/resources/db/migration/V3__add_branch_config.sql], [UP:ubos-server/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L54-L89].
- **Tags:** versioning, multi-tenancy

### CON-UP-042 — Commit-scoped property index (EAV search)
- **What:** A side table indexes selected properties of each commit to allow attribute search.
- **How:** `lcm_entity_search_index(commit_id, prop_name, val_text, val_num)`, index `(prop_name, val_text)`. `search(type, branch, filters)`: without filters returns all head snapshots of a type on a branch; with one filter joins the index on `(prop_name, val_text)`. Index population is designed but the write path is a stub.
- **Why (intent):** Query JSON snapshots by attribute without a per-type table.
- **Sources:** [UP:ubos-server/src/main/resources/db/migration/V2__add_search_index.sql], [UP:ubos-server/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L91-L128].
- **Tags:** search, query

### CON-UP-043 — One concept per migration
- **What:** Schema evolves through small Flyway migrations, each introducing exactly one concept (search, branch config, process log, relations).
- **Sources:** [UP:ubos-server/src/main/resources/db/migration/].
- **Tags:** schema

### CON-UP-044 — Commit write sequence
- **What:** The ordered steps of a commit.
- **How:** 1) find-or-create instance; 2) read head on branch → parent; 3) insert version; 4) index; 5) upsert head; 6) link process. Marked `@Transactional` (reactive), each step with transient retry.
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L138-L172].
- **Tags:** versioning, persistence

### CON-UP-045 — Import/export as logic + action entities (cross-lineage artifact)
- **What:** Data import/export are implemented as committed logic scripts plus action entities that reference them by URI.
- **How:** `db/sqls/improt_export.sql` inserts into `bpu_*` tables (the `UB`/Rust schema family, with `tenant_id`, `display_name`, `status`, `seq_num`): `sys.logic.ops.export` (Groovy: load all heads of a branch), `sys.logic.ops.import` (Groovy: build `EntityChange` list and call `kernel.commitTransaction(tenant, branch, commitCtx, changes)`), and `sys.action.ops.export/import` with `handler: "ubos://sys_boot/sys.logic.ops.export"` and typed `params`.
- **Why (intent):** Shows the Action → Logic indirection and `ubos://` handler references; the file belongs to the `bpu` schema generation (see ANA-UB).
- **Sources:** [UP:ubos-server/src/main/resources/db/sqls/improt_export.sql].
- **Tags:** integration, scripting, protocol

## Multi-tenancy

No `tenant_id` column. Tenancy/regionalization is expressed as **branches** (`beijing`) plus
the identity→branch policy (CON-UP-052). The `AuthContext.tenantId` holds a branch name.

## Caching

Only the compiled-script cache (CON-UP-023). No data cache.
