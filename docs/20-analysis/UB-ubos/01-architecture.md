---
id: ANA-UB-01
title: "UB Architecture"
status: complete
phase: P1
depends_on: [INV-UB, ANA-UB-07]
sources: [UB]
---

# UB — Architecture

Source path prefix: `backend/src/main/java/com/ubos/` (written in full in references).

## Layer map (code)

| Package | Layer (design name) | Content |
|---|---|---|
| `core.api` | SPI | `EntityAssembler`, `EntityCacheManager`, `EntityEventPublisher`, `SchemaComposer`, `ReferenceResolver`, `SchemaValidator`, `UbosProtocolResolver`, `NativeActionHandler`, `BlobStorageProvider`, `UbosUri` |
| `core.context` | runtime context | `UbosContext` (environment), `ProcessContext` (execution), `ExecutionNode`, `ContextKey<T>`, `LogEntry`, `ProcessResult` |
| `core.domain/dto/model` | model | `EntityInstance`, `EntityVersionChain`, `BranchHead`, `ProcessCommitLog`, `ProcessEntityMap`, `EntityChange`, `EntityIdentity`, `MergeStrategy`, `UbosEntity`, `UbosProperties` |
| `bpu` | **BPU** — L1 core atomic + L2 git transaction | `EntityDataOperation` enum + handlers: Commit, CreateBranch, DeleteBranch, MergeBranch, Revert, Diff, GetCommit, GetHead(+All), GetHistory, ListBranch, Audit; `MicroCommitService` |
| `ese` | **ESE** — L3 evaluation + entity state | `EntityStateOperation` enum + handlers: Initialize, SaveDrafts, DiscardDrafts, Publish, Archive, GetState (slug/id/batch/commit), EvaluateEntity, UbosAction, EntitySearch; lifecycle `UbosSchemaComposer`, `UbosReferenceResolver`, `ContextEnvironmentProvider`; `RecursiveEntityAssembler`; `CaffeineEntityCacheManager`; `LogicExecutor` + `UbosScriptContext` |
| `protocol` | L4 protocol | `DefaultProtocolResolver` (`ubos://` → evaluated entity) |
| `boot` | kernel management | `KernelManagerService` (export/import of heads as archives), `EntityArchive` |
| `web` | access | `UbosController`, `DataController`, `DebugController`, `PageController` |

### CON-UB-001 — Intent / Rules / Facts layering (UBOS – ESE – BPU)
- **What:** Three layers with strict roles: UBOS captures intent and dispatches events, ESE judges (state machine, guards, side effects) and emits commands, BPU executes atomically and keeps the immutable ledger.
- **How:** Design rules (D-UB-03): UBOS never modifies data and holds no status conditions; ESE does no I/O (data injected via context) and is pure/testable; BPU knows no business vocabulary; dependency direction `ubos-apps → ubos-kernel → ese-engine → bpu-core`. In code: `web` → `ese` → `bpu`, with `core` shared contracts. `design.md` refines the stack into four layers: L1 Core Atomic, L2 Git Transaction, L3 Evaluation, L4 Protocol (D-UB-04).
- **Why (intent):** "Define – Orchestrate – Commit" replaces MVC; business complexity is concentrated in one rule layer; the ledger is stable and business-agnostic.
- **Sources:** [UB:docs/UBOS Three Layer System.md], [UB:docs/design.md], [UB:backend/src/main/java/com/ubos/].
- **Tags:** process, state-machine, versioning

### CON-UB-002 — SPI-first kernel contracts
- **What:** Every pluggable kernel concern is an interface in `core.api`, implemented in `ese`/`protocol`.
- **How:** `SchemaComposer.compose(tenant, raw, user)`, `ReferenceResolver.resolve(ctx, root)`, `EntityAssembler.assemble(raw, AssemblyContext{fetchReference})`, `EntityCacheManager.get/put(key, json, isSystemData)/evict`, `EntityEventPublisher.publishCommittedEvent(CommitResult)`, `SchemaValidator.validate(tenant, type, data)`, `NativeActionHandler.execute(target, params)`, `BlobStorageProvider.readBlob(hash)`, `UbosProtocolResolver.handle(ctx)`.
- **Why (intent):** Replaceable implementations (cache, blob store, validators) and clean layering.
- **Sources:** [UB:backend/src/main/java/com/ubos/core/api/].
- **Tags:** plugin, api

### CON-UB-003 — Operation enums as an instruction set ("opcodes")
- **What:** Each layer exposes its capabilities as an enum whose constants implement `execute(ProcessContext)`; higher-layer operations are programs composed of lower-layer opcodes.
- **How:**
  - BPU `EntityDataOperation`: `COMMIT_TRANSACTION`, `MERGE_BRANCH`, `CREATE_BRANCH`, `DELETE_BRANCH`, `REVERT_COMMIT`, `LIST_BRANCHES`, `GET_COMMIT`, `GET_HISTORY`, `DIFF_COMMITS`, `GET_HEAD`, `GET_ALL_HEADS`; typed context keys declared on the enum (`KEY_IDENTITY`, `KEY_SOURCE_BRANCH`, `KEY_TARGET_BRANCH`, `KEY_COMMIT_ID`, `KEY_COMMIT_ID_A/B`, `KEY_LIMIT`, `KEY_BEFORE_COMMIT_ID`); each constant validates required keys (`ctx.require`) then delegates to its handler.
  - ESE `EntityStateOperation`: `INITIALIZE`, `SAVE_DRAFTS`, `DISCARD_DRAFTS`, `PUBLISH`, `ARCHIVE`, `GET_STATE_BY_SLUG/ID/BATCH/COMMIT`, `EVALUATE_ENTITY`, `LOGIC_ACTION`, `ENTITY_SEARCH`.
  - Example composition: `SAVE_DRAFTS` = for each request `GET_HEAD(draft)` → if absent `CREATE_BRANCH(main→draft)` → `COMMIT_TRANSACTION`; `PUBLISH` = `MERGE_BRANCH(draft→main, FORCE_SOURCE)` → `DELETE_BRANCH(draft)`.
  - Design analogy (D-UB-07): enum = opcodes, context node = stack frame.
- **Why (intent):** A closed, auditable set of kernel instructions; everything else (handlers, scripts, APIs) is a program over them.
- **Sources:** [UB:backend/src/main/java/com/ubos/bpu/EntityDataOperation.java], [UB:backend/src/main/java/com/ubos/ese/EntityStateOperation.java].
- **Tags:** api, process, versioning

### CON-UB-004 — Context-in / Context-out handler convention
- **What:** Every handler has the signature `Mono<ProcessContext> handle(ProcessContext)`; inputs come from the payload or typed keys, outputs go to `ctx.success(result)`; failures are recorded with `ctx.failNode(e)` instead of thrown.
- **How:** Uniform pattern: `requirePayload()` or `require(KEY)` → `enter(name, input)` → work → `success/exit` or `failNode`; callers check `getGlobalResult().getStatus()`.
- **Why (intent):** D-UB-07 — one context per request, never `new`; complete traceability.
- **Sources:** [UB:backend/src/main/java/com/ubos/bpu/handler/CommitHandler.java], [UB:backend/src/main/java/com/ubos/core/context/ProcessContext.java].
- **Tags:** context, process, observability

### CON-UB-005 — Context-addressed HTTP API
- **What:** The first path segment is a **context slug** resolved to (tenant, branch).
- **How:**

| Endpoint | Function |
|---|---|
| `GET /api/{contextSlug}/{type}/{slug}` | evaluate entity (composed + resolved) |
| `POST /api/{contextSlug}/drafts` | save drafts (batch) |
| `POST /api/{contextSlug}/publish` | publish drafts |
| `POST /api/{contextSlug}/discard` | discard drafts |
| `POST /api/{contextSlug}/execute` | run an action URI |
| `POST /api/{contextSlug}/search` | search identities |
| `GET /api/data/export/{contextSlug}`, `POST /api/data/import/{contextSlug}` | archive export/import |
| `GET /api/debug/raw|effective/{contextSlug}`, `POST /api/debug/execute/{contextSlug}` | inspect raw vs effective, trigger action |
| `GET /{contextSlug}/{type}/{slug}.view` | server-rendered page chosen by entity metadata |

- **Sources:** [UB:backend/src/main/java/com/ubos/web/controller/].
- **Tags:** api, multi-tenancy

### CON-UB-006 — OS-ification blueprint (design)
- **What:** Proposals to make UBOS an operating system for business: VFS paths (`/sys`, `/bin`, `/home/<tenant>`, `/dev`, `/mnt`), process management with PID/PCB/`ps`/`kill`/`top`, drivers and mounts for external systems, package manager (`upm install`), pipes of actions, user vs kernel space with quotas and a kernel-panic breaker.
- **Sources:** [UB:docs/UBOS Three Layer System.md] (D-UB-03 part A).
- **Tags:** plugin, integration, scripting
