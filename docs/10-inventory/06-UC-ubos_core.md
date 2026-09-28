---
id: INV-UC
title: "Inventory: ubos_core"
status: complete
phase: P0
depends_on: [META-TEMPLATES]
sources: [UC]
---

# Inventory: ubos_core (`UC`)

## 1. Identity

| Field | Value |
|---|---|
| Key | `UC` |
| Repository | staunch0515/ubos_core |
| Languages | Rust (single crate), Rhai (scripts as data), TypeScript/React (UI), JSON (genesis) |
| Build | Cargo single crate; npm/Vite for `ui/` |
| Size (source lines) | Rust 4,060 · TS/TSX 5,067 · JSON 2,066 · **Markdown 21,524** |
| Commits | 62 (the most granular history; one commit per build stage) |
| Commit span | 2025-12-30 → 2026-01-06 (+ 2026-09-29 housekeeping) |
| Workflow | Issue-linked branches and PRs (`UBOS#7`, `UBOS#10`, `UBOS#12` …) |
| Shared files | 82 byte-identical files with `US` (mostly `docs/`) |

## 2. Top-level layout

```
ubos_core/
  src/
    domain/          entity, entity_type, sys_type, logic, job, uri, workspace, resolver
    infra/store/     core + reader/ (7 files) + writer/ (4 files)   — Postgres
    kernel/          bootstrap (env, genesis, kernel_func, loader/*), context, process_context, executor (ephemeral, worker/*), factory, manager, resolution, resolver, security, syscalls (ai, common, db/*, fs, memory, task, ui), workspace_manager
    services/        event_bus, scheduler, worker
    api/             axum routes + handlers (deploy, invoke, eval, workspace, entity, query, logic, ambassador)
  tests/             api_test, core_test, job_test, logic_test
  ui/                React client (ActiveZone desktop/stage, Passive feed, CommandBar, Repl console, Workspace/Logrum drawers, EntityEditor, UbosResourceBrowser)
  docs/              master/, plan/, front/ (01–04), db/, init_data.md, book.md, prompt.md
  Dockerfile, docker-compose.yml   ubos_core + pgvector/pg16 + redis:7
```

## 3. Modules

| Name | Path | Role |
|---|---|---|
| `domain` | `src/domain/` | Pure types; `uri.rs` defines `ubos://` addressing; `workspace.rs` (renamed from "Logrum" in commit "Logrum -> Workspace") |
| `infra::store` | `src/infra/store/` | Postgres access; readers/writers |
| `kernel::bootstrap` | `src/kernel/bootstrap/` | Genesis load, kernel functions registration |
| `kernel::executor` | `src/kernel/executor/` | Ephemeral (eval) and worker (async jobs, atomic commit) execution |
| `kernel::syscalls` | `src/kernel/syscalls/` | Script syscall surface: ai, db, fs (VFS), memory, task, ui |
| `kernel::workspace_manager` | | **Workspace ("Logrum")**: an execution environment instance with its own cache/status |
| `services` | `src/services/` | Event bus, cron scheduler, background worker |
| `api` | `src/api/` | `/deploy`, `/invoke` (async), `/eval`, `/workspace/{id}`, `/workspace/{id}/status`, `/workspace/list`, `/entity/{id}`, `/entity/search`, `/logic/{slug}`, `/ambassador/connect` |
| `ui` | `ui/` | Same UI concept family as `US` web client (earlier variant) |

## 4. Tech stack

| Layer | Technology |
|---|---|
| Runtime | Rust, Tokio, axum 0.7 |
| Persistence | PostgreSQL 16 + **pgvector** (sqlx), 6-table schema |
| Cache | Redis 7 (Logrum/Workspace cache manager) |
| Scripting | Rhai 1.16 (sync) |
| Frontend | React, Vite, Monaco, cmdk, framer-motion, zustand |
| Deploy | Dockerfile + compose (app, postgres, redis) |

## 5. Design documents

Same corpus as `US` (see INV-US §5) with these differences:
- `UC` lacks `docs/front/EOP.md`, `FullSlice.md`, `UBOS.md` (whitepaper v2.0), `VsCode.md`, `front_05.md`, `三件套.md`, `渲染.md`, and genesis files 16/17 (actions).
- Several shared files differ in content (`book.md`, `prompt.md`, `front_04.md`, `master/01..07`, genesis 01–15). P1 must diff them.
- `docs/prompt.md` states the same core rules as `US`.

## 6. Entry points

| Entry | Path |
|---|---|
| Server main | `src/main.rs` → `src/api/routes.rs` |
| Kernel | `src/kernel/mod.rs`, `manager.rs`, `factory.rs` |
| UI | `ui/index.tsx` → `App.tsx` |

## 7. Data assets

| Asset | Path | Notes |
|---|---|---|
| Schema | `docs/db/V1_init.sql` | 6 `bpu_*` tables |
| Genesis | `docs/db/genesis_data/01..15` | meta kernel → UI config (earlier variant of `US` genesis) |

## 8. First impression

- The **reference single-crate Rust kernel** built stage by stage (the `docs/master` build log maps 1:1 to commits: connect DB → Rhai → web → Redis → split files → DB hardening → call API → context → orchestration → advanced query → cron → event → UI → logs → schema → import → AI → VFS → policy → function/slug → send card → eval → commit → logrum manager → memory → ExecutionContext → Ambassador).
- Introduces the **Workspace (formerly "Logrum")** as a runtime tenant/environment unit with status and cache.
- **Ambassador Mode**: a handshake API (`/ambassador/connect`) for external agents/clients to join a workspace.
- **Deploy / invoke / eval** trio: deploy logic as data, invoke asynchronously as jobs, evaluate ad-hoc scripts.
- **Policy-as-code**, **virtual file system**, **LLM syscalls** as kernel services available to scripts.

## 9. Analysis reading plan (P1)

1. `docs/prompt.md`, `docs/book.md`, `docs/plan/*` (only where different from `US`; else reference `ANA-US-07`).
2. `docs/master/*` in order — correlate each with the commit that implemented it.
3. `src/domain/**`, `src/infra/store/**`.
4. `src/kernel/**` (bootstrap, workspace_manager, executor, syscalls, security, resolution).
5. `src/services/**`, `src/api/**`.
6. `tests/*.rs` (usage scenarios).
7. `ui/**` (diff vs `US` web client).
