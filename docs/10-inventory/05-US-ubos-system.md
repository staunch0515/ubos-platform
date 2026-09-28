---
id: INV-US
title: "Inventory: ubos-system"
status: complete
phase: P0
depends_on: [META-TEMPLATES]
sources: [US]
---

# Inventory: ubos-system (`US`)

## 1. Identity

| Field | Value |
|---|---|
| Key | `US` |
| Repository | staunch0515/ubos-system |
| Languages | Rust (kernel/server/CLI), Rhai (scripts, CLI test scenarios), TypeScript/React (web client), JSON (genesis metadata) |
| Build | Cargo workspace (5 members; desktop Tauri app planned), npm/Vite |
| Size (source lines) | Rust 5,347 · TS/TSX 7,427 · JSON 3,229 · Rhai 260 · **Markdown 24,094** |
| Commits | 10 |
| Commit span | 2026-01-10 → 2026-01-18 (+ 2026-09-29 housekeeping) |
| README tagline | same as `LS`: "one kernel, five clients; cross-platform; custom binary protocol; zero-copy; high-performance vector processing" |
| Shared files | 82 byte-identical files with `UC` (mostly `docs/`) |

## 2. Top-level layout

```
ubos-system/
  crates/
    ubos_proto/        domain types: entity, entity_type, sys_type, logic, job, uri, workspace, resolver
    ubos_store/        Postgres store: reader (fetch, scan_by_type, scanner, search, timeline, mapper), writer (atomic, manual, runtime)
    ubos_kernel/       kernel: action engine + transitions, bootstrap (genesis, loaders, env, kernel_func), executor (sync, ephemeral, worker), graph (assembler, composite), session, syscalls (ai, db, fs, memory, task, ui, host_functions), schema engine, security, resolver/resolution, workspace manager
    ubos_client_sdk/   (stub)
  apps/
    ubos_server/       axum HTTP server: api handlers/routes, services (event bus, scheduler, worker)
    ubos_cli/          clap CLI running Rhai scenario scripts (tests/*.rhai)
    ubos_web/          React client: kernel/ (ClientKernel, protocol, registry, rpc), engine/ (ViewEngine, UniversalRenderer, SchemaRenderer, WidgetFactory, WidgetRegistry), Action/, ActiveZone/, Passive/, CommandPalette/, Console/
  docs/
    master/  01..35, 51  — staged build log of the Rust kernel (Foundation → Skeleton → Core → Power → … → Ambassador Mode)
    plan/    Plan_01..19 — roadmap/decision documents
    front/   front_01..05, EOP, FullSlice, UBOS (architecture whitepaper v2.0), VsCode, 三件套, 渲染
    db/      V1_init.sql, genesis_data/01..17, init_data/
    init_data.md (2,648 lines: metadata definition standard), book.md, prompt.md (core design rules)
```

## 3. Modules / crates

| Name | Path | Role |
|---|---|---|
| `ubos_proto` | `crates/ubos_proto/src/` | Core types incl. `uri.rs` (`ubos://`), `sys_type.rs`, `job.rs` |
| `ubos_store` | `crates/ubos_store/src/` | Read models (timeline, search, scan by type) and write modes (atomic / manual / runtime) |
| `ubos_kernel::action` | `action/`, `action_engine.rs` | Action execution with **transitions** (multi-step actions) |
| `ubos_kernel::bootstrap` | `bootstrap/` | Genesis loading from JSON, environment, kernel functions |
| `ubos_kernel::executor` | `executor/` | Sync, ephemeral (eval), and **worker** (async job) executors |
| `ubos_kernel::graph` | `graph/` | Entity graph assembly, composite entities |
| `ubos_kernel::session` | `session/` | Session manager/state |
| `ubos_kernel::syscalls` | `syscalls/` | **Syscall surface for scripts**: ai, db (6 files), fs (virtual FS), memory, task, ui, host functions |
| `ubos_server` | `apps/ubos_server/` | HTTP API + event bus + cron scheduler + worker |
| `ubos_cli` | `apps/ubos_cli/` | Scenario runner (`step1_init_types.rhai`, `test_inheritance.rhai`, `test_append_only.rhai`, `demo_full_flow.rhai`) |
| `ubos_web` | `apps/ubos_web/src/` | "Bicameral" UI: ActiveZone (desktop/stage) + Passive (feed stream), command palette, widget registry, universal renderer, client kernel with RPC |

## 4. Tech stack

| Layer | Technology |
|---|---|
| Runtime | Rust, Tokio, axum 0.7 |
| Persistence | PostgreSQL (sqlx), 6-table `bpu_*` schema with yearly partitions; Redis declared |
| Scripting | Rhai 1.23 (sync) |
| Frontend | React, Vite, antd, Monaco, cmdk, framer-motion, zustand, react-markdown |
| CLI | clap 4 |

## 5. Design documents (highlights; full digest in P1)

| Path | Lines | Topic |
|---|---|---|
| `docs/prompt.md` | 16 | **Core design rules**: "Everything is an entity"; "the database has only 6 tables, never add tables"; default tenant `logrums`; explicit inheritance only (`_extends`); files ≤100 lines; no Chinese in code |
| `docs/front/UBOS.md` | 158 | **Architecture whitepaper v2.0 (Compositional Era)**: Git-for-Data + Pure Composition; Behavior (atom) → Relation Type (molecule) → Entity Type (organism); core meta-relations `sys_primitive`, `sys_descriptor` |
| `docs/init_data.md` | 2,648 | UBOS metadata definition standard |
| `docs/master/01..35,51` | ~15,000 | Build log: foundation, skeleton, core, power, completion; DB persistence, Rhai, web, Redis, file split, DB hardening, BPU stdlib (call API), context, orchestration, advanced query, cron, event bus, logs, schema (data contract), import system, AI (LLM), virtual file system, policy-as-code, dynamic form function, eval, commit, Logrum cache manager, "holographic" ProcessContext, robustness, **Ambassador Mode** (memory, handshake API) |
| `docs/plan/Plan_01..19` | ~3,900 | Roadmap and architectural decisions (e.g. Plan_11 separation of environment and execution; Plan_12 "bicameral" front end; Plan_13 re-rooting the system; Plan_16 EntityType from hard-coded enum to data; plan_18 the `ubos://` protocol) |
| `docs/front/*` | ~3,800 | Front-end design: Linear-style prompts, polymorphic rendering, EOP, FullSlice, VS Code-like shell, 三件套 (three-piece set), 渲染 (recursive rendering) |
| `docs/book.md` | 124 | Next-generation software philosophy |

## 6. Entry points

| Entry | Path |
|---|---|
| Server | `apps/ubos_server/src/main.rs` → `api/routes.rs` |
| CLI | `apps/ubos_cli/src/main.rs` |
| Kernel | `crates/ubos_kernel/src/lib.rs`, `manager.rs`, `factory.rs` |
| Web | `apps/ubos_web/src/index.tsx` → `App.tsx` → `MainLayout.tsx` |

## 7. Data assets

| Asset | Path | Notes |
|---|---|---|
| Schema | `docs/db/V1_init.sql` | The canonical **6 tables** (`bpu_entity_instance`, `_version_chain`, `_branch_head`, `_search_index`, `bpu_process_commit_log`, `bpu_process_entity_map`) with yearly partitions |
| Genesis | `docs/db/genesis_data/01..17` | meta kernel (root, meta…), business types, tech formats, contact/geo, party/roles, doc content, system security, IT infra, kernel logic (functions), reference data, measure types, PKI, kernel instances, kernel scripts, **UI config (views/screens)**, entity actions, logic actions — each type has `ui_modes` (view/edit widget) |
| Scenarios | `apps/ubos_cli/tests/*.rhai` | executable specifications |

## 8. First impression

- The **most complete Rust kernel**: actions with transitions, sync/ephemeral/worker executors, syscall layer for scripts (db, fs, ai, memory, task, ui), event bus, cron, sessions.
- Fixes the **physical schema at 6 tables forever**; everything else — types, logic, views, actions, security — is **data in the genesis graph**.
- Metadata types carry **UI rendering modes** (`ui_modes`) and actions carry **param schemas with UI widgets** → metadata-driven UI end to end.
- A new **"bicameral" UI concept** (active work zone + passive feed stream) and a command-palette-first client with a client-side kernel.
- The richest **design journal** (master/plan/front), documenting the evolution of every subsystem.

## 9. Analysis reading plan (P1)

1. `docs/prompt.md`, `docs/front/UBOS.md`, `docs/book.md`.
2. `docs/init_data.md` + `docs/db/genesis_data/*` (meta-model), `docs/db/V1_init.sql`.
3. `crates/ubos_proto/**`, `crates/ubos_store/**`.
4. `crates/ubos_kernel/**` (bootstrap → action engine → executors → syscalls → graph → session → security).
5. `apps/ubos_server/**`, `apps/ubos_cli/**` incl. `.rhai`.
6. `apps/ubos_web/src/**` (kernel/, engine/, stores, zones).
7. `docs/plan/*` (decisions), `docs/master/*` (mechanisms; skim where identical to `UC`), `docs/front/*`.
