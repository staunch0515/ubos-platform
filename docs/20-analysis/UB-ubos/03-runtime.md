---
id: ANA-UB-03
title: "UB Runtime"
status: complete
phase: P1
depends_on: [ANA-UB-02]
sources: [UB]
---

# UB — Runtime Mechanisms

### CON-UB-020 — ProcessContext: the reified execution stack
- **What:** One context object per request that records the full execution tree and results.
- **How:** Fields: `environment: UbosContext`, `userId`, `clientIp`, `traceId`, `originalPayload`, `rootNode`, transient `currentNode`, `globalResult`. Methods: `create(env, user, ip, payload)`, `enter(handlerName, input)` (push `ExecutionNode{handlerName, start/end, status PENDING/SUCCESS/FAILURE, exception, input, output, attributes, logs, children}`), `exit(output)`, `failNode(e)`, `putVar/getVar` (node scope), `log(level, msg)` (attached to the current node), `success(data)`, `fail(msg)`, `getResultData()`, `requirePayload()`, typed `put/get/require(ContextKey<T>)`, `toFullJson()` (persistable "holographic" record), `toSimpleString()`, `getExecutionPath()`.
- **Why (intent):** D-UB-07 — "black box" of every business transaction: who called whom, with what inputs, logs and outputs; no scattered logs.
- **Sources:** [UB:backend/src/main/java/com/ubos/core/context/ProcessContext.java], [UB:backend/src/main/java/com/ubos/core/context/ExecutionNode.java], [UB:docs/ProcessContext.md].
- **Tags:** context, observability, process

### CON-UB-021 — Batch commit transaction with optimistic head CAS
- **What:** `COMMIT_TRANSACTION` writes a list of `EntityChange`s atomically.
- **How:** New transaction id → `bpu_process_commit_log` row ("Batch Commit (n)", operator) → for each change (sequential `concatMap`): idempotent instance registration (tenant, type, slug, `displayName`, `owner_id` from snapshot or user) → version row with **explicit `parentCommitId` from the change** → typed index rows extracted from top-level fields → process–entity map → head: no parent ⇒ create head; parent ⇒ **compare-and-swap** (`UPDATE … WHERE head = expectedParent`); 0 rows ⇒ `OptimisticLockingFailureException` (whole transaction rolls back) → `CommitResult{transactionId, committedAt, newHeadCommits: entityId → commitId}`.
- **Why (intent):** Multi-entity atomicity plus lost-update prevention via expected parents.
- **Sources:** [UB:backend/src/main/java/com/ubos/bpu/handler/CommitHandler.java], [UB:backend/src/main/java/com/ubos/bpu/repository/EntityBranchHeadRepository.java].
- **Tags:** versioning, persistence, process

### CON-UB-022 — Draft → publish lifecycle on per-user branches
- **What:** Users edit on `draft/{userId}`; publishing merges drafts into `main` and deletes the draft branches.
- **How:** `SAVE_DRAFTS`: for each `DraftSaveRequest{identity, data, expectedParentCommitId}` ensure the entity's draft head exists (auto-`CREATE_BRANCH` from `main`), then one `COMMIT_TRANSACTION` on the draft branch (CAS). `PUBLISH`: `MERGE_BRANCH(draft/{user} → main, FORCE_SOURCE)` then `DELETE_BRANCH` per entity. `DISCARD_DRAFTS` drops drafts. `INITIALIZE` creates new identities (UUIDs) with first commits (parent null). Evaluation prefers the user's draft when present.
- **Why (intent):** Safe editing with isolation; publish as an atomic promotion; editors see live vs draft (CON-UB-016).
- **Sources:** [UB:backend/src/main/java/com/ubos/ese/handler/SaveDraftsHandler.java], [UB:backend/src/main/java/com/ubos/ese/handler/PublishEntitiesHandler.java], [UB:backend/src/main/java/com/ubos/ese/handler/InitializeEntitiesHandler.java].
- **Tags:** versioning, workflow

### CON-UB-023 — Merge strategies including true three-way merge
- **What:** Branch merge over all heads of the source branch with a selectable strategy.
- **How:** `MergeStrategy {FAIL_ON_CONFLICT, FORCE_SOURCE, FORCE_TARGET, THREE_WAY_MERGE}`; identical snapshots are skipped; target without head ⇒ merge commit from source; `THREE_WAY_MERGE` finds the **common ancestor by walking parent chains (max depth 10,000)** and merges per top-level field (changed on one side ⇒ take it; changed differently on both ⇒ conflict error). Audit log "Merge src -> tgt (strategy)" with trace id as process id; tenant-scoped commit lookups.
- **Sources:** [UB:backend/src/main/java/com/ubos/bpu/handler/MergeBranchHandler.java], [UB:backend/src/main/java/com/ubos/core/dto/MergeStrategy.java].
- **Tags:** versioning

### CON-UB-024 — Revert as a new commit (Git revert semantics)
- **What:** Reverting writes a new commit whose snapshot equals the target commit, with the current head as parent ("Revert to commit N").
- **Sources:** [UB:backend/src/main/java/com/ubos/bpu/handler/RevertHandler.java].
- **Tags:** versioning

### CON-UB-025 — Diff as RFC 6902 JSON Patch
- **What:** `DIFF_COMMITS(a, b)` returns `JsonDiff.asJson(a, b)` (JSON Patch operations), with tenant-checked commit access.
- **Sources:** [UB:backend/src/main/java/com/ubos/bpu/handler/DiffHandler.java].
- **Tags:** versioning

### CON-UB-026 — Micro-commit service
- **What:** Single-entity fine-grained commit: audit action first, then immutable version, then CAS on the branch head; the author is taken from the trusted context, not the request.
- **Why (intent):** Supports autosave/realtime editing (D-UB-13) and "Git-like content-addressable micro commits" (D-UB-20).
- **Sources:** [UB:backend/src/main/java/com/ubos/bpu/service/MicroCommitService.java].
- **Tags:** versioning, persistence

### CON-UB-027 — Three-pass object lifecycle: compose → resolve → execute
- **What:** Effective objects are produced by (1) inheritance composition, (2) reference resolution, (3) deferred execution of actions.
- **How:**
  - Pass 1 `UbosSchemaComposer`: follow `_extends` (URI) through the protocol resolver (parent evaluated recursively), deep merge (child wins), cycle detection by visited URIs, **max depth 10**, remove `_extends` from the result (normalization; D-UB-11 adds expanding short `type`/`target` references to canonical URIs).
  - Pass 2 `UbosReferenceResolver`: walk objects/arrays/text; `{{ ubos://… }}` in text is replaced by the evaluated target (text value or JSON); **cross-context references** resolve the target context via `ContextEnvironmentProvider`; failures degrade to empty string. Design: type-aware resolution (`ENTITY_REFERENCE` fields replaced by target JSON; `TEXT` interpolated) (D-UB-20).
  - Pass 3: actions are only descriptors until invoked (CON-UB-029).
  - `inheritanceChain` records `[self, parent, …, root]` for provenance, loop detection, UI, `instanceOf` (D-UB-04).
- **Why (intent):** D-UB-08 — separate compiler/linker/runtime concerns; interpolation never runs before composition.
- **Sources:** [UB:backend/src/main/java/com/ubos/ese/lifecycle/UbosSchemaComposer.java], [UB:backend/src/main/java/com/ubos/ese/lifecycle/UbosReferenceResolver.java], [UB:docs/UBOS协议的综合处理.md].
- **Tags:** meta-model, rehydration, protocol

### CON-UB-028 — Evaluation with read-through cache
- **What:** `EVALUATE_ENTITY` returns the effective `UbosEntity`.
- **How:** Cache key per context/identity/user → hit returns cached effective JSON; miss → fetch state (by id or type+slug) → choose user draft if present else live → compose → resolve → cache (**business cache**: 10,000 entries, 5 min; **system cache**: 1,000 entries, 24 h) → result. Separate caches: snapshot by commit id (1 h), branch head (2 s).
- **Why (intent):** Assembly is compute-heavy; D-UB-04 recommends caching **raw per-entity JSON** and assembling just-in-time to avoid cascade invalidation (the implemented cache stores effective JSON; recorded as a design/implementation divergence).
- **Sources:** [UB:backend/src/main/java/com/ubos/ese/handler/EntityEvaluationHandler.java], [UB:backend/src/main/java/com/ubos/ese/service/CaffeineEntityCacheManager.java], [UB:backend/src/main/java/com/ubos/ese/config/CacheConfig.java].
- **Tags:** cache, rehydration

### CON-UB-029 — Action → Logic indirection
- **What:** An **Action** entity declares `handler: ubos://…` (and `params` types); the handler is a **Logic** entity with `lang` and `content`; executing an action URI evaluates both and runs the script.
- **How:** `UbosActionHandler`: evaluate `Action/<slug>` → read `handler` → evaluate `Logic/<slug>` → pass `content` to `LogicExecutor`. Seed examples: `sys.action.ops.export/import` → `sys.logic.ops.export/import` (params `data: array`, `branch: string`).
- **Why (intent):** Separates the public, typed operation (action, bindable to buttons, permissions) from its implementation (logic, versioned independently).
- **Sources:** [UB:backend/src/main/java/com/ubos/ese/handler/UbosActionHandler.java], [UB:data/boot/init.json].
- **Tags:** scripting, api, workflow

### CON-UB-030 — Script context as the SDK
- **What:** Groovy scripts get one facade `ctx` (`UbosScriptContext`) plus `log`.
- **How:** `param(key, default)`, `getAt(key)` (`ctx['x']`), `json(str)`, `getTenant()`, `getProcessContext()`, `getBean(Class)`; design adds `findOne`, `loadAllHeads`, `newId`, `commit`, `ai()`, HTTP (D-UB-05, D-UB-19). Execution on a sandbox pool with **5 s timeout**; compiled classes cached by content hash.
- **Why (intent):** "Context is the SDK": hide reactive types, auto-tenant, anti-corruption layer so stored scripts survive kernel changes.
- **Sources:** [UB:backend/src/main/java/com/ubos/ese/engine/LogicExecutor.java], [UB:backend/src/main/java/com/ubos/ese/engine/script/UbosScriptContext.java], [UB:docs/UbosContext.md].
- **Tags:** scripting, api

### CON-UB-031 — Genesis ingestion through the kernel
- **What:** Seed data is loaded via kernel operations, not raw SQL (design), with ordering and change detection.
- **How:** Implemented: Flyway `V2..V19` "BPU Genesis Protocol" seeds (each tied to a genesis process), `KernelManagerService.importData` sorts archives by priority (`sys.entity.root` first, `sys.biz.text*` next) and commits them in one transaction; `exportData` exports all heads of a context. Designed (D-UB-10): Meta-Loader with topological batches over `_extends`, SHA-256 dirty check, dedicated `draft/sys_loader` branch, publish per batch, remote genesis from the UBOS Store; plus a runtime registry of live metadata reloaded on `MetadataUpdatedEvent`.
- **Sources:** [UB:backend/src/main/java/com/ubos/boot/KernelManagerService.java], [UB:docs/元数据加载器.md].
- **Tags:** boot, metadata

### CON-UB-032 — Entity state machine contract (design)
- **What:** ESE's intended programming model: state definitions with transitions `from –event[guard]→ to` plus actions; engine loop load → match → guards → actions → one BPU command batch; UI derives allowed actions from transitions.
- **Sources:** [UB:docs/UBOS Three Layer System.md] (D-UB-03 part D).
- **Tags:** state-machine, rules, workflow

### CON-UB-033 — Boot designs: instance entity and network boot permit
- **What:** A running-instance entity supplies defaults; nodes can boot by presenting a hardware fingerprint to a central server and receiving a signed boot permit (tenant, role, config).
- **Sources:** [UB:docs/boot.md] (D-UB-11).
- **Tags:** boot, deployment, auth
