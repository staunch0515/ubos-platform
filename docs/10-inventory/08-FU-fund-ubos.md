---
id: INV-FU
title: "Inventory: fund-ubos"
status: complete
phase: P0
depends_on: [META-TEMPLATES]
sources: [FU]
---

# Inventory: fund-ubos (`FU`)

## 1. Identity

| Field | Value |
|---|---|
| Key | `FU` |
| Repository | staunch-studio/fund-ubos |
| Languages | Java 17 (backend), Groovy (scripts), TypeScript/React (UBOS Studio frontend) |
| Build | Gradle (`backend` subproject), npm/Vite |
| Size (source lines) | Java 9,878 · TS/TSX 13,128 · SQL 100 |
| Commits | 28 |
| Commit span | 2025-12-05 → 2025-12-12 (+ 2026-09-29 housekeeping) |
| Root Java package | `org.logrum.ubos` (same as `UP`) |
| README tagline | "Universal Business OS – A Git-like Logic Engine" |
| Shared files | 44 byte-identical files with `UP`, 16 with `UB` (see INV-SUMMARY §3) |

## 2. Top-level layout

```
fund-ubos/
  backend/src/main/java/org/logrum/ubos/
    kernel/     versioned store + governance services (approval, authz, merge conflict, stash, history, environment, metrics, webhook, api key, users)
    engine/     Groovy executor, events, scheduler (same lineage as UP)
    server/     controllers + web console command parser
    web/console LcmConsoleController, History, MergeConflict, Stash controllers + DTOs
    web/security ApiKeySecurityFilter, LocalhostSecurityFilter
  backend/src/main/resources/db/migration/  V1..V6 (V6: environment)
  frontend/     "UBOS Studio" – IDE-style web console (React, antd, shadcn, Monaco, TanStack Table, RTK Query)
```

## 3. Modules / packages

| Name | Path | Role |
|---|---|---|
| `kernel.service` | `kernel/service/` | `LcmKernelService`, `LcmApprovalService`, `LcmAuthzService`, `LcmEnvironmentService`, `LcmHistoryService`, `LcmMergeConflictService`, `LcmStashService`, `LcmMetricsService`, `LcmUserService`, `ApiKeyService`, `WebhookService`, `UbosResourceResolverService`, `LcmRelationService`, `LcmAuditService` |
| `kernel.util` | `kernel/util/` | `JsonSchemaValidator`, `UbosUriUtil`, `ReactiveRetry` |
| `engine` | `engine/` | `LcmLogicExecutor`, `ScriptSecurityConfig`, `LcmEventService`, `LcmSchedulerService`, `UbosRuntimeService` |
| `server.controller.console` | | `CommandParser` – text command console |
| `web.console` | | REST for console: batch commit, branch create/merge, diff, conflict detect, revert, rename/copy, URI-based commit/revert, approvals, policies, groups, users, webhooks, api keys, system health |
| `frontend` | `frontend/src/components/` | ~35 Studio components: `NamespaceTree`, `EntityGrid`, `SnapshotEditor`, `DiffViewer`, `MergeConflictResolver`, `BranchManagerModal`, `BranchStatusViewer`, `TimeTravelSlider`, `HistoryViewer`, `DraftManager`, `ApprovalManager`, `SchemaManager`, `EnvironmentManager`, `IdentityManager`, `WebhookManager`, `APIKeyManager`, `CacheManager`, `OperationalDashboard`, `ProcessLogViewer` |

## 4. Tech stack

| Layer | Technology |
|---|---|
| Runtime | Java 17, Spring Boot WebFlux, actuator |
| Persistence | PostgreSQL via R2DBC, Flyway |
| Validation | JSON Schema (networknt 1.0.87) |
| Scripting | Groovy 4.0.15 |
| Frontend | React 18, Vite, Tailwind, antd + shadcn/ui, Monaco, TanStack Table, react-diff-viewer, Redux Toolkit/RTK Query, zustand, react-router |

## 5. Design documents

| Path | Lang | Topic |
|---|---|---|
| `frontend/UBOS_STUDIO.md` | ZH | UBOS Studio IDE-style console: layout, entity grid, snapshot editor, history |
| `frontend/RTK_QUERY_SETUP.md` | EN/ZH | RTK Query integration |
| `frontend/README.md` | ZH | Frontend project overview |

## 6. Entry points

| Entry | Path |
|---|---|
| Server main | `backend/src/main/java/org/logrum/ubos/server/UbosApplication.java` |
| Studio | `frontend/src/main.tsx` → `UBOSStudio.tsx` |
| Text console | `frontend/public/console.html` + `WebConsoleController` / `CommandParser` |

## 7. Data assets

| Asset | Path | Notes |
|---|---|---|
| Schema | `db/migration/V1..V6` | UP schema lineage + `V6__add_environment.sql` (environment config) |
| Config | `application_config.yml` | |

## 8. First impression

- Shares its kernel lineage with `UP` but adds a full **governance layer**: approvals, authorization policies, groups, API keys, webhooks, environments, metrics.
- Most complete implementation of **Git semantics for data**: branch create/merge, **merge-conflict detection and resolution**, **stash**, revert, history, rename/copy, time-travel slider.
- Introduces **namespace tree** navigation and **`ubos://` URI-addressed** commit/revert.
- The most developed **operator/developer console** (UBOS Studio) — an IDE for business data.

## 9. Analysis reading plan (P1)

1. `db/migration/V1..V6`.
2. `kernel/service/LcmKernelService.java` (diff vs UP), then every other `kernel/service/*`.
3. `kernel/util/UbosUriUtil.java`, `UbosResourceResolverService`.
4. `web/console/**` controllers + DTOs (API surface), `server/controller/console/CommandParser.java`.
5. `web/security/**`.
6. `frontend/UBOS_STUDIO.md`, `frontend/src/components/**`, `store/ubosApi.ts`, `utils/ubosUri.ts`, `namespaceTree.ts`.
