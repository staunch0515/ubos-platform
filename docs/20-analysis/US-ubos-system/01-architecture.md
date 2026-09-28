---
id: ANA-US-01
title: "US Architecture"
status: complete
phase: P1
depends_on: [INV-US, ANA-UC-01, ANA-US-07]
sources: [US, UC]
---

# US — Architecture

## Mapping to UC (code)

| US path | UC path | Difference |
|---|---|---|
| `crates/ubos_proto/src/*` | `src/domain/*` | same except manual `Debug` for `Entity`, `Logic::from_entity` accepts `script_content` as string **or array of lines** |
| `crates/ubos_store/src/**` | `src/infra/store/**` | import paths; search matches `name` **or slug** |
| `crates/ubos_kernel/src/{factory,context,…}` | `src/kernel/*` | `ExecutionContext` gains `session: Arc<Mutex<EntitySession>>`; buffers use tokio `Mutex` |
| `crates/ubos_kernel/src/syscalls/db/*` | `src/kernel/syscalls/db/*` | `sys_db_get` / `sys_db_commit` go through the session (CON-US-020/021) |
| `crates/ubos_kernel/src/executor/worker/*` | `src/kernel/executor/worker/*` | runner creates a session; committer flushes it, then commits the job |
| `apps/ubos_server/src/**` | `src/main.rs`, `src/api/**`, `src/services/**` | + action & syntax handlers; worker backs off 3 s on Redis errors |
| `apps/ubos_web/src/**` | `ui/**` | + `kernel/`, `engine/`, `Action/`, `widgets/`, Monaco Rhai |
| – | – | **new**: `graph/`, `session/`, `action_engine.rs`, `schema_engine.rs`, `executor/sync.rs`, `syscalls/host_functions.rs`, `apps/ubos_cli` |

### CON-US-001 — Cargo workspace: protocol, store, kernel, SDK, apps
- **What:** Members `ubos_proto` (pure types), `ubos_store` (persistence), `ubos_kernel` (runtime), `ubos_client_sdk` (stub), `apps/ubos_server`, `apps/ubos_cli`; `apps/ubos_web` (npm); `apps/ubos_desktop` (Tauri) commented as future.
- **How:** [US:Cargo.toml]; shared dependency versions in `[workspace.dependencies]`.
- **Why (intent):** "One core, five clients": one kernel crate reused by several front ends (server, CLI, desktop, web, SDK).
- **Sources:** [US:Cargo.toml], [US:README.md].
- **Tags:** deployment, plugin, api

### CON-US-002 — Layered kernel: store → graph → session → syscalls → executors → action FSM
- **What:** Reads go through an object graph (composite entities), writes are staged in a session, syscalls see only the session, executors own session lifecycle, the action FSM composes executors.
- **How:** `Kernel::resolve` now assembles a `CompositeEntity` via `ObjectAssembler`; `resolve_property_recursive` reads `composite.get_property` [US:crates/ubos_kernel/src/lib.rs#L43-L66].
- **Why (intent):** Inheritance and transactions become kernel services instead of per-syscall code.
- **Tags:** meta-model, persistence, process

### CON-US-003 — Three execution modes
- **What:** async **worker** (Job + queue, CON-UC-003/006), **ephemeral** eval (console, CLI), **sync** logic (`exec_logic_sync(logic_uri, scope_vars)`: HTTP thread, no Job, every key of `scope_vars` becomes a script variable, session flushed on success) [US:crates/ubos_kernel/src/executor/sync.rs#L19-L94].
- **Why (intent):** Interactive action steps need immediate answers (the next form), while long work stays asynchronous.
- **Divergence:** sync identity fixed to tenant `logrums`, user `dev`; no Job/provenance record.
- **Tags:** process, scripting

### CON-US-004 — CLI as scenario runner and executable specification
- **What:** `ubos boot` (run genesis) and `ubos eval <file.rhai> [--user] [--context genesis|<uuid>]` (slug `genesis` resolved to the Workspace UUID) [US:apps/ubos_cli/src/main.rs#L19-L100].
- **How:** Scenario scripts: define `Type/Animal`, `Type/Dog extends Animal` (default override), instance `Dog/buddy`, verify inherited values, mutate and recommit (session dirty check), append-only version check, diagnostics [US:apps/ubos_cli/tests/].
- **Why (intent):** Behaviour specified as runnable Rhai against the real kernel; the same language users write.
- **Tags:** scripting, boot, observability

### CON-US-005 — Server-side script compilation check
- **What:** `POST /check_syntax {script}` compiles without running and returns `{line, column, message}`; the web Rhai editor turns it into Monaco markers [US:apps/ubos_server/src/api/handlers/syntax.rs], [US:apps/ubos_web/src/components/Editor/RhaiEditor.tsx#L37-L57].
- **Why (intent):** Safe, fast feedback for logic-as-data authoring (also the dry-run step for AI-generated scripts).
- **Tags:** scripting, api, frontend

### CON-US-006 — Target protocol architecture: UBTP and five atomic instructions (design)
- **What:** Client ⇄ Server over TCP with **UBTP** (length-prefixed JSON); long-lived session holding ProcessContext (user, trace, workspace snapshot); only five instructions: `Handshake` (establish context), `Mutate` (ChangeSet), `Emit` (signal/process), `Query`, `Subscribe` (change feed). Kernel parts: ProcessOrchestrator, RhaiAdapter (`sync`), SemanticLayer (type/relation inheritance → final shape), PersistenceEngine. Stack rules: Rust 2021, tokio, rhai `sync`+`serde`, SQLite (dev) / PostgreSQL (prod), sqlx raw SQL, serde, anyhow.
- **Why (intent):** A closed, minimal instruction set independent of HTTP routes; any client speaks the same five verbs.
- **Status:** design (US uses HTTP routes).
- **Sources:** [US:docs/front/UBOS.md].
- **Tags:** protocol, api, deployment

### CON-US-007 — Kernel as event bus + plugin scheduler (design)
- **What:** Every commit request is broadcast through fixed lifecycle phases: `PHASE_1_INTENT` (defaults, formatting) → `PHASE_2_VALIDATE` (rules; veto allowed) → `PHASE_3_AUTHORIZE` (RBAC, ownership) → `PHASE_4_PERSIST` (the only hard-wired kernel behaviour) → `PHASE_5_NOTIFY` (mail, cache, downstream). The scheduler discovers "plugins" = relation types found in the entity's `_extends` and relations, orders by priority, runs their scripts and collects `Decision`s.
- **Why (intent):** "Everything is an event, all logic is a plugin"; the kernel stays business-free; hot-plug by editing `_extends`; sandboxed; recordable for replay.
- **Sources:** [US:docs/front/FullSlice.md].
- **Tags:** events, plugin, rules, process

### CON-US-008 — Emit flow with side effects as returned intentions (design)
- **What:** `Emit {target, signal}`: load process state from head commit → inject signal/args/user → run the type's Rhai → script returns new variables, `next_step` and an `emits` list → commit new state version → kernel dispatches the emits recursively. Scripts never call the kernel directly ("a wish list").
- **Why (intent):** Scripts stay pure and testable; orchestration is recursive signal propagation; state and logic separated (instance struct vs definition script).
- **Sources:** [US:docs/front/UBOS.md], [US:docs/front/FullSlice.md] ("Process handling" prototype).
- **Tags:** workflow, events, scripting
