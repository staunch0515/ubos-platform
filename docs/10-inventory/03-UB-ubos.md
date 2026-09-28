---
id: INV-UB
title: "Inventory: ubos"
status: complete
phase: P0
depends_on: [META-TEMPLATES]
sources: [UB]
---

# Inventory: ubos (`UB`)

## 1. Identity

| Field | Value |
|---|---|
| Key | `UB` |
| Repository | staunch0515/ubos |
| Languages | Java 17 (backend), Groovy (logic as data), SQL (schema + large seed data), TS (placeholder frontend) |
| Build | Gradle (`backend` subproject) |
| Size (source lines) | Java 8,733 · SQL 2,802 · JSON 2,515 · **Markdown 16,740** |
| Commits | 3 (code imported as a snapshot on 2025-12-25) |
| Root Java package | `com.ubos` (sub-packages `bpu`, `ese`, `core`, `protocol`, `web`, `boot`) |
| README tagline | "Universal Business OS – A Git-like Logic Engine" |

## 2. Top-level layout

```
ubos/
  backend/src/main/java/com/ubos/
    core/          API contracts (SPI interfaces), context model, domain, DTOs
    bpu/           Business Processing Unit: versioned storage handlers (commit, branch, merge, diff, revert, history)
    ese/           Entity State Engine: draft/publish lifecycle, assembly, schema composition, reference resolution, cache, script execution
    protocol/      ubos:// protocol resolver
    boot/          KernelManagerService, EntityArchive (boot import)
    web/           Controllers (Ubos, Data, Page, Debug), exception handler
  backend/src/main/resources/db/migration/   V1 schema + V2..V20 genesis/meta seed data
  data/boot/init.json      126 boot items (meta types, logic, actions)
  docs/                    23 design documents (mostly Chinese), the largest design corpus of all repos
  frontend/                Vite/React placeholder
  docker-compose.yml       pgvector Postgres
```

## 3. Modules / packages

| Name | Path | Role |
|---|---|---|
| `core.api` | `core/api/` | SPI: `BlobStorageProvider`, `EntityAssembler`, `EntityCacheManager`, `EntityEventPublisher`, `NativeActionHandler`, `ReferenceResolver`, `SchemaComposer`, `SchemaValidator`, `UbosProtocolResolver`, `UbosUri` |
| `core.context` | `core/context/` | `UbosContext`, `ProcessContext`, `ExecutionNode`, `LogEntry`, `ContextKey`, `ContextFileArchiver`, `ProcessResult/Status` |
| `core.domain`/`dto`/`model` | | `EntityInstance`, `EntityVersionChain`, `BranchHead`, `ProcessCommitLog`, `EntityChange`, `EntityIdentity`, `MergeStrategy`, `MicroCommitRequest`, `UbosEntity`, `UbosProperties` |
| `bpu` | `bpu/` | Git-like operations as handlers: Commit, CreateBranch, DeleteBranch, Diff, GetCommit, GetHead, GetHistory, ListBranch, MergeBranch, Revert, Audit; `MicroCommitService` |
| `ese` | `ese/` | Entity lifecycle handlers: Initialize, SaveDrafts, BatchSaveDrafts, DiscardDrafts, Publish, Archive, Evaluate, Search, GetState (by id/slug/commit), UbosAction; `RecursiveEntityAssembler`, `UbosSchemaComposer`, `UbosReferenceResolver`, `ContextEnvironmentProvider`, `CaffeineEntityCacheManager`, `LogicExecutor` + `UbosScriptContext` |
| `protocol` | `protocol/service/DefaultProtocolResolver.java` | Resolves `ubos://` URIs |
| `boot` | `boot/` | `KernelManagerService`, `EntityArchive` |
| `web` | `web/controller/` | `UbosController`, `DataController`, `PageController`, `DebugController` |

## 4. Tech stack

| Layer | Technology |
|---|---|
| Runtime | Java 17, Spring Boot WebFlux (reactive) |
| Persistence | PostgreSQL with **pgvector** (embedding column, ivfflat index), JSONB + GIN, **range partitioning by year**, multi-tenant (`tenant_id`) |
| Cache | Caffeine |
| Scripting | Groovy |
| Frontend | placeholder only |

## 5. Design documents (all under `docs/`)

| File | Lines | Topic |
|---|---|---|
| `sys_boot.md` | 5,166 | Step-by-step system bootstrap design (largest single doc) |
| `动态视图渲染器.md` (Dynamic View Renderer) | 2,018 | Metadata-driven view rendering |
| `database_strong.md` | 1,367 | Enterprise-grade database hardening |
| `UBOS协议的综合处理.md` (Comprehensive handling of the UBOS protocol) | 1,066 | `ubos://` protocol |
| `ProcessContext.md` | 1,049 | Process execution context |
| `design.md` | 850 | Overall design summary |
| `元数据加载器.md` (Meta-Loader) | 683 | Metadata loader per BPU architecture guide |
| `UBOS Three Layer System.md` | 670 | Three-layer system |
| `ai-dev.md` | 601 | AI-driven development |
| `boot.md` | 552 | Boot hardening |
| `BEL Pro.md` | 465 | BEL (business expression language?) 1.0 |
| `UbosContext.md` | 354 | Script engine context design |
| `Rehydration.md` | 340 | Rehydration (restoring entities/state) |
| `IDE.md` | 324 | IDE / entity manager |
| `forntend.md` | 291 | Frontend overall design |
| `UBOS Smart Document.md` | 262 | Smart Document concept |
| `高级提示词.md` (Advanced prompts) | 238 | Prompts for IDE AI to implement the system |
| `I18n.md` | 185 | AI-based dynamic multi-language support |
| `The essence of entities.md` | 98 | Philosophy of entities |
| `三块拼图.md` (Three puzzle pieces) | 86 | The system as three cooperating parts |
| `EnvContext.md` | 0 | (empty) |

## 6. Entry points

| Entry | Path |
|---|---|
| Server main | `backend/src/main/java/com/ubos/UbosApplication.java` |
| Boot | `boot/KernelManagerService.java` + `data/boot/init.json` + Flyway V2..V19 genesis data |
| API | `web/controller/UbosController.java` (action dispatch), `DataController`, `PageController` |

## 7. Data assets

| Asset | Path | Notes |
|---|---|---|
| Schema | `db/migration/V1__init_schema.sql` | `bpu_entity_instance` (tenant, tags, owner, active), `bpu_entity_version_chain` (JSONB snapshot, **vector embedding**, partitioned), `bpu_entity_branch_head` (tenant+branch), `bpu_entity_search_index`, `bpu_process_commit_log` (app_id, **billing_commit_id**), `bpu_process_entity_map` |
| Genesis seed | `V2..V19` | "BPU Genesis Protocol": root object `sys.entity.root`, meta type `sys.meta.type`, primitive types `sys.type.*`, every change tied to a genesis process |
| Boot JSON | `data/boot/init.json` | 112 type definitions with `_extends` (inheritance) + `properties` + `is_abstract`; units with scale; logic (Groovy) and actions |

## 8. First impression

- The most **philosophically elaborated** repo: large design corpus about entities, context, rehydration, protocol, three-layer system, smart documents, i18n, AI-driven development.
- Architecture splits the kernel into **BPU** (versioned storage, Git operations incl. merge/diff/revert) and **ESE** (entity state engine with **draft → publish** lifecycle).
- Strong **SPI-first** design (`core/api/*` interfaces) and a **`ubos://` URI protocol** for addressing any resource.
- A **self-describing meta-model** bootstrapped from a single root object with inheritance (`_extends`), abstract types, primitive types.
- Enterprise storage ideas: tenant partitioning, yearly partitions, JSONB GIN, **vector embeddings for AI search**, billing linkage on processes.

## 9. Analysis reading plan (P1)

1. `docs/design.md`, `三块拼图.md`, `UBOS Three Layer System.md`, `The essence of entities.md` (vision).
2. `docs/sys_boot.md`, `boot.md`, `元数据加载器.md` + `V1..V19` + `data/boot/init.json` (meta-model and boot).
3. `core/api/*`, `core/context/*`, `docs/UbosContext.md`, `ProcessContext.md`, `Rehydration.md`.
4. `bpu/**` (Git operations), `ese/**` (entity lifecycle), `protocol/**` + `UBOS协议的综合处理.md`.
5. `docs/动态视图渲染器.md`, `forntend.md`, `IDE.md`, `UBOS Smart Document.md`, `I18n.md`.
6. `docs/BEL Pro.md`, `database_strong.md`, `ai-dev.md`, `高级提示词.md`.
7. `web/**`.
