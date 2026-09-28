---
id: ANA-LS-01
title: "LS Architecture"
status: complete
phase: P1
depends_on: [INV-LS, ANA-LS-07]
sources: [LS]
---

# LS — Architecture

| Crate / app | Role | Key files |
|---|---|---|
| `ubos_proto` | Wire and domain types: `UbosOp`/`UbosResp`, `EntityMutation`, `ChangeSet`/`CommitAction`, `Entity`, `EntityType`, `ProcessContext`, `Workspace`, `Logic`, `EntityResolver` | `crates/ubos_proto/src/*` |
| `ubos_store` | `PersistenceEngine` (reader/writer) over op traits (`InstanceOps`, `VersionChainOps`, `BranchHeadOps`, `SearchIndexOps`, `ProcessMapOps`, `ProcessLogOps`, `EntityReaderOps`) with `sqlite` and `postgres` backends | `crates/ubos_store/src/**` |
| `ubos_kernel` | `semantic/` (type + relation layers, URI, loader, merge, behavior dispatch), `engine/` (bootloader, orchestrator, process_runner), `script/` (Rhai adapter), `model/` (genesis, process state), `mechanism/` + `pipeline/` + `kernel.rs` (design sketches) | `crates/ubos_kernel/src/**` |
| `ubos_server` | TCP server + codec; test client binaries | `apps/ubos_server/src/**` |

### CON-LS-001 — Protocol-first workspace without HTTP
- **What:** The only external interface is a TCP server (`PORT`, default 9876); no web server, no Redis usage (axum, redis, reqwest, cron are declared workspace dependencies only).
- **How:** `main`: SQLite pool → `PersistenceEngine` → optional genesis (`RUN_GENESIS=true`) → initialize `SemanticLayer` and `RelationSemanticLayer` → `ProcessOrchestrator` → `UbosServer::run` (one task per connection) [LS:apps/ubos_server/src/main.rs#L23-L86].
- **Why (intent):** "Own binary protocol": one small instruction set for all five client kinds.
- **Tags:** protocol, deployment, api

### CON-LS-002 — UBTP: length-prefixed frames and five operations
- **What:** Frame = 4-byte big-endian length + JSON body; request `UbosOp` (serde tag `op`, content `data`): `Handshake{client_id, workspace_id, token}`, `Mutate{tx_id, changes: [EntityMutation{uri, op: create|update|delete, payload}]}`, `Emit{target_uri, signal, args}`, `Query{criteria, projection}`, `Subscribe{filter}`; response `UbosResp` = `Ack{request_id}` | `Op(UbosOp)` | `Error{message}`.
- **How:** `UbosCodec` decoder/encoder [LS:apps/ubos_server/src/codec.rs#L20-L63]; ops defined as "5 core instructions (not modifiable)" [LS:crates/ubos_proto/src/op.rs#L8-L57]; a second, earlier variant with `Emit{target_uri, behavior_uri, args}` and `Mutate{uri, payload}` [LS:crates/ubos_proto/src/proto/mod.rs].
- **Why (intent):** Closed, transport-level instruction set (the US whitepaper's UBTP, CON-US-006) implemented; JSON body now, binary encoding later ("payload currently serde").
- **Tags:** protocol, api

### CON-LS-003 — Per-connection session context and response streaming
- **What:** Each connection holds a `ProcessContext` (workspace, user, process id, app id), replaced by `Handshake` (workspace in tenant `logrums`, branch `main`, fresh process UUID); each op yields `Ack` and then every side-effect op as a separate `Op` message, or `Error` [LS:apps/ubos_server/src/server.rs#L43-L121].
- **Why (intent):** Stateful, bidirectional session: the server can push instructions to the client after the acknowledgement.
- **Tags:** context, protocol

### CON-LS-004 — Orchestrator routing by operation and target kind
- **What:** `ProcessOrchestrator {store, script_engine: RhaiAdapter, semantic, relation_semantic}`: `handle_mutate` (URI → tenant/type/slug → ChangeSet → one atomic commit), `handle_emit` (load target head; process entity → `drive_process_step`; otherwise → `dispatch_behavior(signal, args)`), `handle_query` (URI → entity head → `Emit{client://current, QueryResult, {uri, payload, version}}`) [LS:crates/ubos_kernel/src/engine/orchestrator/mod.rs#L18-L119].
- **Tags:** process, api, events

### CON-LS-005 — The client is an addressable target
- **What:** The server sends instructions to `client://current` with signals `RenderForm` (title + field schema), `Notify`, `QueryResult`.
- **How:** genesis `label` SchemaProvide script and test processes emit `{op: Emit, data: {target_uri: client://current, signal: RenderForm, args}}` [LS:genesis_data/02_relationType.json], [LS:apps/ubos_server/src/bin/test_notify.rs].
- **Why (intent):** UI is driven by the same Emit primitive as business signals — the client is just another entity in the signal network.
- **Tags:** ui-rendering, protocol, events

### CON-LS-006 — Pluggable storage editions behind op traits
- **What:** Store operations are traits; backends `sqlite` (feature `personal`, default) and `postgres` (feature `enterprise`) export the same aliases (`DbPool`, `DbTx`, `InstanceImpl`, `VersionImpl`, …) [LS:crates/ubos_store/src/backend/mod.rs], [LS:crates/ubos_store/Cargo.toml].
- **Why (intent):** One kernel for local/edge/desktop (SQLite file) and server (PostgreSQL); "one core, five clients".
- **Tags:** persistence, deployment, plugin

### CON-LS-007 — Commit-keyed semantic caches
- **What:** `SemanticLayer` / `RelationSemanticLayer` each hold three concurrent maps: URI → head commit id, commit id → raw snapshot, commit id → assembled node (lazy); loaded at startup from `MetaType`+`MetaRelationType` (types) or `MetaRelationType` (relations) on branch `main` [LS:crates/ubos_kernel/src/semantic/mod.rs#L18-L63], [LS:crates/ubos_kernel/src/semantic/relation/mod.rs].
- **Why (intent):** Because versions are immutable, assembled nodes keyed by commit id never need invalidation; only the URI→head map changes.
- **Tags:** cache, meta-model, versioning

### CON-LS-008 — Kernel mechanism pipeline (design sketch)
- **What:** Eight "physical laws" applied to every entity operation through one interface `KernelMechanism { name(), apply(entity, ctx, dry_run) → Result<Vec<UbosOp>> }`: **Resolution** (load by URI, fail if absent), **Extends** (merge fully-resolved parents' payload and relations; child wins; C3-style resolution), **Policy** (PDP decides subject/object/action; deny aborts), **Prerequisite** (`sys:rel/prerequisite` logic blocks with reason), **Schema** (`sys:rel/schema` validation), **Transformation** (`sys:rel/transform` pure field transforms, in-memory), **Implement** (`sys:rel/implements` bindings execute and produce the main ops), **Reactor** (topology of subscribers → `Notify` ops; skipped in dry run). A `PipelineContext {user, request_uri, entity, pending_ops, is_dry_run}` flows through with `PipelineFlow::{Continue, Break, Reject, SystemError}`.
- **Why (intent):** Mechanisms know nothing of business types, only their own closed logic; the whole operation is one explainable pipeline whose output is a list of ops.
- **Status:** design (types referenced are not defined).
- **Sources:** [LS:crates/ubos_kernel/src/mechanism/], [LS:crates/ubos_kernel/src/kernel.rs], [LS:crates/ubos_kernel/src/pipeline/context.rs].
- **Tags:** process, rules, plugin, permission
