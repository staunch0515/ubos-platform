---
id: INV-UP
title: "Inventory: ubos-platform"
status: complete
phase: P0
depends_on: [META-TEMPLATES]
sources: [UP]
---

# Inventory: ubos-platform (`UP`)

## 1. Identity

| Field | Value |
|---|---|
| Key | `UP` |
| Repository | staunch0515/ubos-platform |
| Languages | Java 17 (server), Groovy (scripts stored as data), TypeScript/React (2 frontends), JavaScript (1 frontend), Python (AI copilot) |
| Build | Gradle multi-project (`ubos-server` only included), npm/Vite for frontends |
| Size (source lines) | Java 1,677 · TS/TSX 797 · JS/JSX 206 · Python 271 · SQL 221 |
| Commits | 28 (excluding docs work) |
| Commit span | 2025-11-29 → 2025-12-10 (+ 2026-09-29 housekeeping) |
| Root Java package | `org.logrum.ubos` |
| First commit message | "Initial commit: UBOS Genesis v0.1 - The Digital Lifeform Engine" |

## 2. Top-level layout

```
ubos-platform/
  ubos-server/        Java Spring Boot WebFlux server (kernel + engine + server layers)
  ubos-shell/         React/TS "desktop OS" shell (windows, desktop, schema renderer)
  ubos-web/           React/TS minimal client (logic commit form)
  ubos_client/        React/JS minimal client (earlier variant of ubos-web)
  ubos-copilot/       Python Streamlit + OpenAI copilot console
  commit_*.json       Sample logic commits (Groovy code wrapped in JSON)
  Dockerfile*         Server / copilot images
  docker-compose.yml  postgres + server + copilot
  docs/               THIS documentation set (added by the research project)
```

## 3. Modules / packages

| Name | Path | Role |
|---|---|---|
| `kernel` | `ubos-server/src/main/java/org/logrum/ubos/kernel/` | Versioned entity store: instances, version chain, branch heads, search index, relations, audit |
| `engine` | `.../ubos/engine/` | Groovy logic executor with security sandbox, event service, scheduler, runtime service |
| `server` | `.../ubos/server/` | WebFlux controllers (boot, dev, run, view, audit, auth, admin ops), bootstrap (`Genesis`, `SystemBootstrapper`), auth filter |
| `common` | `.../ubos/common/` | `AuthContext`, constants |
| `ubos-shell` | `ubos-shell/src/` | Browser "OS" metaphor: `Desktop`, `WindowFrame`, `SchemaRenderer`, zustand stores `kernelStore`/`osStore` |
| `ubos-web` / `ubos_client` | | Forms to commit logic entities and call the API |
| `ubos-copilot` | `ubos-copilot/app.py` | Streamlit console: run logic, audit browser, server-driven UI renderer, LLM assistant |

## 4. Tech stack

| Layer | Technology |
|---|---|
| Runtime | Java 17, Spring Boot 3.4 (WebFlux, reactive) |
| Persistence | PostgreSQL 15 via R2DBC; Flyway migrations (V1–V5) |
| Scripting | Apache Groovy 4.0.15 (logic stored as data, executed at runtime) |
| Frontend | React + Vite + TypeScript, zustand, react-draggable, sass |
| AI | Python Streamlit, OpenAI SDK (gpt-4o) |
| Deploy | Docker, docker-compose |

## 5. Design documents

| Path | Language | Topic |
|---|---|---|
| (none besides boilerplate READMEs) | – | Design intent must be read from code, SQL comments, commit messages and sample JSON. Commit messages are unusually descriptive (e.g. "Time Travel", "Branch Hierarchy", "Identity governance", "API version coexistence kernel.v1/v2"). |

## 6. Entry points

| Entry | Path |
|---|---|
| Server main | `ubos-server/src/main/java/org/logrum/ubos/server/UbosApplication.java` |
| Genesis bootstrap | `.../server/bootstrap/Genesis.java`, `SystemBootstrapper.java` |
| Logic execution | `RunController` → `UbosRuntimeService` → `LcmLogicExecutor` (Groovy) |
| Shell UI | `ubos-shell/src/main.tsx` |
| Copilot | `ubos-copilot/app.py` (Streamlit, port 8501) |

## 7. Data assets

| Asset | Path | Notes |
|---|---|---|
| Core schema | `ubos-server/src/main/resources/db/migration/V1..V5*.sql` | `lcm_entity_instance`, `lcm_entity_version_chain`, `lcm_entity_branch_head`, `lcm_entity_search_index`, `sys_branch_config` (branch inheritance), `lcm_process_commit_log` + `lcm_process_entity_map`, `lcm_entity_relation` |
| Import/export SQL | `.../db/sqls/improt_export.sql` | |
| Sample logic | `commit_onboard.json`, `commit_search_logic.json` | Groovy business logic committed as versioned entities |

## 8. First impression

- A **"Git for business data and logic"** kernel: every entity (LOGIC, VIEW, DATA) is a slug with an append-only commit chain, branch heads and parent commits.
- **Branch inheritance** (`beijing` inherits from `master`) is used as a tenancy/localization mechanism — lookups fall back up the branch family tree.
- **Logic is data**: Groovy scripts are committed like any other entity and executed with a kernel handle; a **Process** groups multiple commits into one auditable business operation.
- **Time travel**: execution can be pinned to a specific commit.
- An early **AI copilot + server-driven UI** concept, and an "OS desktop" shell UI metaphor.
- Name "Digital Lifeform Engine" / "Genesis" / "immune system" (script sandbox) shows a biological metaphor in naming.

## 9. Analysis reading plan (P1)

1. `db/migration/V1..V5` (data model).
2. `kernel/service/LcmKernelService.java`, `LcmRelationService.java`, `LcmAuditService.java`.
3. `engine/executor/LcmLogicExecutor.java`, `engine/config/ScriptSecurityConfig.java`, `LcmEventService`, `LcmSchedulerService`.
4. `server/bootstrap/Genesis.java`, `SystemBootstrapper.java`, all controllers.
5. `commit_*.json` samples.
6. `ubos-shell/src/**` (engine/SchemaRenderer, stores, Desktop).
7. `ubos-copilot/app.py`.
8. Full commit log messages (design intent).
