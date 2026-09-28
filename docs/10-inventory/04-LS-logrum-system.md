---
id: INV-LS
title: "Inventory: logrum-system"
status: complete
phase: P0
depends_on: [META-TEMPLATES]
sources: [LS]
---

# Inventory: logrum-system (`LS`)

## 1. Identity

| Field | Value |
|---|---|
| Key | `LS` |
| Repository | staunch0515/logrum-system |
| Languages | Rust (edition 2021), Rhai (scripting), JSON (genesis data) |
| Build | Cargo workspace (4 members) |
| Size (source lines) | Rust 3,793 · JSON 1,442 · SQL 192 |
| Commits | 11 |
| Commit span | 2026-01-18 → 2026-01-27 (+ 2026-09-29 housekeeping) |
| README tagline | "一核五端、跨平台、自研二进制协议、零拷贝、高性能向量处理" — *one kernel, five clients; cross-platform; custom binary protocol; zero-copy; high-performance vector processing* |

## 2. Top-level layout

```
logrum-system/
  crates/
    ubos_proto/      protocol + domain types (UbosOp, change set, entity, entity type, context, workspace, resolver)
    ubos_store/      storage engine with pluggable backends (postgres/, sqlite/), reader/writer, ops
    ubos_kernel/     semantic layer, mechanisms, orchestrator, process runner, script adapter
  apps/ubos_server/  TCP server with length-prefixed binary codec + test client binaries (test_emit, test_mutate, test_query, test_process, test_approve, test_notify, test_universal)
  genesis_data/      01..07 JSON: meta types, relation types, base behaviors, entity types, processes, actions
  docs/db/sqlite/    V1_init.sql
  claude.md          design brief of the "relational architecture refactoring"
  *.db               SQLite databases (runtime data)
```

## 3. Modules / crates

| Name | Path | Role |
|---|---|---|
| `ubos_proto` | `crates/ubos_proto/` | Wire/domain types: `op.rs` (UbosOp/UbosResp), `change_set.rs`, `entity.rs`, `entity_type.rs`, `logic.rs`, `context.rs`, `workspace.rs`, `resolver.rs` |
| `ubos_store` | `crates/ubos_store/` | Storage abstraction: `ops/*` traits (branch, index, instance, lineage, process_log, reader, version) with **postgres** and **sqlite** backend implementations; `engine/reader.rs`, `engine/writer.rs` |
| `ubos_kernel::semantic` | `crates/ubos_kernel/src/semantic/` | Loader, assembler, merge, tree, types, `uri.rs`, `relation/*` (relation-type assembler), `behavior.rs` |
| `ubos_kernel::mechanism` | `.../mechanism/` | Named semantic mechanisms: `extend`, `implement`, `policy`, `prerequisite`, `reactor`, `resolution`, `schema`, `transformation` |
| `ubos_kernel::engine` | `.../engine/` | `bootloader`, `orchestrator/{behavior,process}`, `process_runner` |
| `ubos_kernel::process` | `.../process/` | Process definition/instance/state |
| `ubos_kernel::script` | `.../script/` | Rhai adapter |
| `ubos_server` | `apps/ubos_server/` | Tokio TCP server, `UbosCodec` (4-byte big-endian length prefix framing) |

## 4. Tech stack

| Layer | Technology |
|---|---|
| Runtime | Rust, Tokio, tokio-util codec |
| Protocol | Custom length-prefixed binary framing over TCP (payload currently serde) |
| Persistence | SQLite (active) and PostgreSQL (backend implemented) via sqlx |
| Scripting | Rhai 1.16 |
| Other declared | axum, redis, reqwest, cron (workspace deps) |

## 5. Design documents

| Path | Lang | Topic |
|---|---|---|
| `claude.md` | EN/ZH | **Relational architecture refactoring**: "Entity is just a UUID; everything else is a Relation"; BehaviorType (slot) / Behavior (impl) / Refine (inheritance chain) / RelationType (configurator); three-stage action design (Init → Interact → Exec) with signals like `SchemaProvideSlot`, `PermissionSlot`, `ComputeFieldSlot`, `FieldMaskSlot`, `DefaultValueSlot`, `TransformInputSlot`, `ValidationSlot`, `BeforeUpdateSlot`; principle "scripts only emit, the engine does the work" |
| `console.md` | EN | Docker/DB commands |
| `README.md` | ZH | Tagline (see §1) |

## 6. Entry points

| Entry | Path |
|---|---|
| Server main | `apps/ubos_server/src/main.rs` → `server.rs` / `tcp_server.rs` |
| Kernel | `crates/ubos_kernel/src/kernel.rs`, `engine/bootloader.rs` |
| Test clients | `apps/ubos_server/src/bin/test_*.rs` |

## 7. Data assets

| Asset | Path | Notes |
|---|---|---|
| Schema | `docs/db/sqlite/V1_init.sql` | same `bpu_*` table family as `UB` (instance, version chain, branch head, search index, process commit log, process entity map) |
| Genesis | `genesis_data/01..07*.json` | Entities are maps keyed by **relation-type URIs** (`ubos://logrums/MetaRelationType/identity`, `/label`, `/relationTypes`, `/behaviorConfig`, `/schema`); BehaviorTypes such as `StorageSlot`; processes with state cursor; actions with 3 stage processes |

## 8. First impression

- The most **radical meta-model**: an entity has no fields, only **relations**; every attribute is an instance of a **RelationType**, and RelationTypes carry **behavior slot configurations**.
- **Behavior-slot dispatch** (BehaviorType = interface, Behavior = implementation with a `refine` inheritance chain, RelationType = slot configurator) replaces hand-written business logic: scripts **emit** slot signals, the engine fans out across relations.
- A standard **three-stage Action protocol** (Init/Interact/Exec) that yields UI schemas, permissions, computed fields, masking, defaults, transforms, validations, hooks.
- **Everything is addressed by `ubos://tenant/Type/slug` URIs**, including keys inside entity documents.
- Systems concerns: pluggable storage backends (SQLite for local/edge, Postgres for server), custom binary TCP protocol, "one kernel, five clients".

## 9. Analysis reading plan (P1)

1. `claude.md` (full), `genesis_data/01..07` (full).
2. `ubos_proto/src/*` (all).
3. `ubos_kernel/src/semantic/**`, `mechanism/**` (each mechanism).
4. `ubos_kernel/src/engine/**`, `process/**`, `script/**`, `kernel.rs`.
5. `ubos_store/src/ops/*`, `engine/*`, one backend (sqlite) in full, postgres diff.
6. `apps/ubos_server/src/*` incl. `bin/test_*.rs` (usage scenarios).
7. Commit messages.
