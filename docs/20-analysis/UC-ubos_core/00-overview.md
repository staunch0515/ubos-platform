---
id: ANA-UC-00
title: "UC Overview"
status: complete
phase: P1
depends_on: [INV-UC]
sources: [UC]
---

# UC (ubos_core) — Overview

## Vision as stated by the repository

- `docs/prompt.md` (the house rules every build prompt carries): **"Everything is an entity."** · "The database has only 6 tables; never create new tables." · "The default tenant is `logrums`." · "Entities must use purely explicit inheritance (`_extends`)." · "One kernel, five clients, cross-platform, own binary protocol, zero-copy, high-performance vector processing" (stated target; not implemented).
- `docs/master/*`: a stage-by-stage construction log of a **Rust micro-kernel** ("UBOS Core 1.0 → 5.0") in which *logic is data* (Rhai scripts stored as entities), *execution is commit* (every run is a persisted, versioned capsule), and scripts reach the world only through **syscalls**.
- `docs/plan/Plan_10.md`, `Plan_14.md`: positioning as an **"AI Agent operating system"** — a stateful serverless runtime whose environments ("Logrum" → later "Workspace", "the Embassy") keep memory between executions.
- `docs/book.md`: the product target is the **Executable Book** — documents that carry state, logic, events and UI ("the knowledge base is the brain, the executable book is the hand").

## Problem it addresses

A minimal, fast, multi-tenant runtime in which **business logic, configuration and data are
all versioned entities** in one 6-table Git-for-data store; logic is deployed without
redeployment, executed asynchronously as durable jobs with full lineage (which code version,
which environment version, which outputs), and exposed to people and AI through a menu-less,
command-driven cockpit whose screens are derived from entity types.

## Design in one paragraph

A single-crate Rust (Tokio/axum) binary holds a `Kernel` = {Postgres store, one shared Rhai
`Engine` with registered **syscalls**, Redis client, HTTP client, Workspace manager}. HTTP
`/deploy` stores a **Logic** entity; `/invoke` loads the **Workspace** (environment: sovereign
tenant, default branch, memory, env vars) and the Logic, **anchors their head commit ids**
into a new **Job** entity (`__anchors`), saves it and pushes its id to a Redis queue (202).
A worker pops the id, **loads** the anchored versions (time travel), builds a
**ProcessContext** (identity + environment + request + provenance), **runs** the script with
a thread-local `ExecutionContext` (logs, memory, tenant) behind the syscalls, then
**commits** in one Postgres transaction: the Job's new version (optimistic lock on its head),
a process log row and process→commit lineage rows (`OUTPUT`, `DEPENDS_ON_CTX`,
`DEPENDS_ON_FUNC`), and writes the Workspace memory back to Redis. Cron and Hook entities
drive a scheduler and an event bus (`sys_db_commit` emits `entity_committed`). Entities are
located by `(tenant, type, slug)` behind UUIDs, addressed as `ubos://tenant/type/slug[?commit=]`,
and resolve missing properties through an explicit `_extends` chain. Genesis loads 15 JSON
files (a UODS-standard semantic type ontology) through an idempotent loader. A React
"Cockpit" provides Workspace selection, an omnibar, an Active zone (forms, REPL, resource
browser) and a Passive feed.

## File map

| File | Content | Key IDs |
|---|---|---|
| `01-architecture.md` | Micro-kernel, stage-built architecture, syscall ABI, async split, pipeline, smart entity | CON-UC-001…007 |
| `02-core-model.md` | Entity, open EntityType, explicit inheritance, Logic/Job, Workspace, URI, UODS, ontology, sovereign tenant | CON-UC-010…019 |
| `03-runtime.md` | Wiring/validation, execution-is-commit, anchoring, ProcessContext, Ambassador, env/exec separation, eval, orchestration, cron/hooks, policy, import, guards, genesis, robustness | CON-UC-020…035 |
| `04-data-persistence.md` | 6-table schema usage, locator upsert, lineage, Redis roles, VFS, branches, search | CON-UC-040…046 |
| `05-interface-ui.md` | HTTP API, hydration, bicameral cockpit, schema-to-UI, lens system, cards, resource browser, copilot prompt | CON-UC-050…059 |
| `06-extensibility-ai.md` | Extension points, LLM syscall, Executable Book, agent runtime, stdlib, AI discovery, prompt-driven development, URI as universal argument | CON-UC-060…067 |
| `07-design-docs-digest/` | Index + 3 parts covering all 56 design documents (D-UC-01…58) | – |
| `08-highlights.md` | 14 highlights | HL-UC-001…014 |
| `09-concept-index.md` | Flat index | – |

## Top highlights

1. HL-UC-001 Execution is commit — durable job capsules with atomic result + lineage.
2. HL-UC-002 Version anchoring: jobs pin the exact environment and code commits; provenance map.
3. HL-UC-003 Syscall ABI between sandboxed scripts and the kernel (thread-local execution context).
4. HL-UC-004 Environment (Workspace) vs execution (Process) separation; Ambassador sessions with memory.
5. HL-UC-006 Lens system: component = f(type chain, mode) with semantic fallback.
6. HL-UC-008 UODS: a strict meta-schema for metadata definitions.

## Notes for later phases

- **Shared corpus.** The docs corpus is shared (82 identical files) with US. This digest (`07-design-docs-digest/`) is the canonical digest of `docs/master`, `docs/plan`, `docs/front/front_01…04`, `docs/init_data.md`, `docs/book.md`, `docs/prompt.md`, `docs/db/*`. ANA-US-07 must reference it and cover only US-only documents and content that differs (INV-UC §5 lists them).
- **Vocabulary drift inside the repo:** `SysFunction` → `Logic`; `Logrum` (execution record) → `Logrum` (environment/session) → `Workspace`; execution records → `Job`; `sys` tenant → `logrums`; `SysCron/SysHook/SysPolicy` → `Cron/Hook/Policy`; ids `tenant:type:slug` → UUID + locator. P2 must normalize terms (glossary in P3).
- **Design vs implementation.** Several stages described in `docs/master` are absent from the final code: JSON-Schema validation (19), `sys_db_query` (15), `sys_http_*` (12), ProcessContext snapshot archive (29/30 — computed but not stored), extraterritorial read via `HOST_TENANT_ID` (35 — env vars are not passed to the runner). Some code paths still build legacy `tenant:type:slug` ids against UUID lookups (module resolver, policy, scheduler/event-bus default context, history syscall). These are noted as "(design)" or "divergence", not evaluated as defects.
- Schema `docs/db/V1_init.sql` is identical to UB's schema v2.1 (whitespace only) and byte-identical to US; UC uses only a subset of its columns (CON-UC-040).
- `genesis_data/13_kernel_instances.json` duplicates `01_meta_kernel.json`; `init_data.md` intended it to hold tenant/user/group instances.
