---
id: ANA-UC-01
title: "UC Architecture"
status: complete
phase: P1
depends_on: [INV-UC, ANA-UC-07]
sources: [UC]
---

# UC — Architecture

## Module map (code)

| Module | Role | Key files |
|---|---|---|
| `domain` | Pure types: `Entity`, `EntityType`, `Job`/`JobStatus`/`SystemEvent`, `Logic`, `Workspace`, `UbosUri`, `EntityResolver` trait, `SysTypeView` | `src/domain/*.rs` |
| `infra::store` | Postgres (sqlx): `reader/` (fetch, mapper, search, scanner, scan_by_type, timeline), `writer/` (atomic, manual, runtime) | `src/infra/store/**` |
| `kernel` | `Kernel` struct + `factory` (boot/engine setup), `manager` (deploy, enqueue_invoke), `executor/` (ephemeral, worker: loader/runner/committer), `syscalls/` (common, ui, memory, task, db, fs, ai), `context` (thread-local), `process_context`, `workspace_manager` (Redis), `security` (policy), `resolver` (Rhai module resolver), `resolution` (inheritance), `bootstrap/` | `src/kernel/**` |
| `services` | Long-running loops: `worker` (task queue), `scheduler` (Cron), `event_bus` (Hook) | `src/services/*.rs` |
| `api` | axum routes and handlers | `src/api/**` |
| `ui` | React "Cockpit" client | `ui/**` |

Runtime topology: one process in `server` mode boots the kernel, runs genesis, wraps the
kernel in `Arc<tokio::Mutex<Kernel>>`, spawns scheduler / event bus / worker tasks and serves
HTTP on `:8100` [UC:src/main.rs#L44-L77]. Compose adds `pgvector/pgvector:pg16` and
`redis:7-alpine` and mounts `./docs` (genesis files) into the container
[UC:docker-compose.yml].

### CON-UC-001 — Micro-kernel: a kernel object plus user-space scripts
- **What:** The kernel is a small value `Kernel { store, engine: Arc<rhai::Engine>, redis, http_client, workspace_manager }`; all business behaviour lives in Rhai scripts stored as `Logic` entities.
- **How:** `Kernel::boot` creates the Rhai engine, sets operation limits and print/debug capture, installs the DB-backed module resolver and registers all syscalls, then opens Redis and builds the `WorkspaceManager` [UC:src/kernel/factory.rs#L11-L97]. `Kernel` also implements `EntityResolver` (CON-UC-007).
- **Why (intent):** D-UC-01/D-UC-30 — "UBOS as an operating system": the kernel wires data into logic, runs it and records it; the business is data.
- **Sources:** [UC:src/kernel/mod.rs#L24-L31], [UC:src/kernel/factory.rs], [UC:docs/plan/Plan_01.md].
- **Tags:** plugin, scripting, process

### CON-UC-002 — Architecture grown in documented stages
- **What:** The architecture is the sum of explicit, prompt-driven stages ("UBOS Core 1.0 → 5.0"), each a design note with a goal, pain point, code and verification step.
- **How:** Stages 1–5 entity/store/logic/Logrum/kernel wiring (in-memory) → Postgres → Rhai → Web → Redis queue → file split → DB syscalls → HTTP syscalls → tenant context → orchestration → query → cron → events → logs → schema validation → import → AI → VFS → policy → function metadata API → eval → atomic commit → Logrum cache manager → holographic ProcessContext → robustness → Ambassador mode → environment/execution separation → version anchoring → dynamic EntityType → `ubos://` → resource browser (D-UC-01…48). Git history maps one commit per stage (62 commits).
- **Why (intent):** "Like building a skyscraper: foundation → skeleton → walls → finishing" (Plan_01); each step compiles and runs before the next.
- **Sources:** [UC:docs/master/], [UC:docs/plan/].
- **Tags:** process, metadata

### CON-UC-003 — Asynchronous producer/consumer split
- **What:** API calls create durable work and return immediately; workers execute.
- **How:** `/invoke` saves a `Job` entity (status `PENDING`) and `RPUSH`es its id to `ubos_task_queue`, answering `202 {status: queued, job_id}` [UC:src/api/handlers/invoke.rs#L18-L39], [UC:src/kernel/manager.rs#L39-L104]; `services::worker` `BLPOP`s and calls `worker_execute_logrum` [UC:src/services/worker.rs#L9-L44]. The client polls `/workspace/{id}/status` (D-UC-46).
- **Why (intent):** D-UC-05 — long-running tasks, unreliable integrations with retry, traffic shaping, CPU-heavy AI work; "Audit by default": the Job exists in Postgres before the queue is touched, so a lost queue message leaves a visible `PENDING` record.
- **Design notes:** Plan_01 critique lists: `Mutex<Kernel>` serializes the API; dual write Postgres+Redis (suggests transactional outbox); single-threaded worker (suggests `tokio::spawn`). The final code keeps the serialized design (comment in worker.rs).
- **Sources:** [UC:docs/master/09_redis.md], [UC:docs/plan/Plan_01.md].
- **Tags:** process, integration, scheduling

### CON-UC-004 — Syscall layer as the kernel ABI
- **What:** Scripts cannot touch I/O directly; they call named kernel functions (`sys_*`) registered on the engine.
- **How:** Registered set: `sys_log`, `parse_int`, `sys_push_card`, `sys_memory_set/get`, `sys_invoke_async`, `sys_db_get`, `sys_db_history`, `sys_resolve_prop`, `sys_db_commit`, `sys_file_save/read`, `sys_ai_chat` [UC:src/kernel/syscalls/mod.rs#L12-L32]. Design stages also defined `sys_http_get/post` (D-UC-08), `sys_db_query` (D-UC-11) — not in the final code.
- **Why (intent):** D-UC-31 — "empowering the script": a standard library / syscall layer turns a calculator into a business system while the kernel keeps control (tenant checks, events, audit).
- **Sources:** [UC:src/kernel/syscalls/], [UC:docs/plan/Plan_02.md].
- **Tags:** scripting, api, permission

### CON-UC-005 — Sync-script / async-kernel bridge with a thread-local execution context
- **What:** Rhai is synchronous; storage and network are async. Syscalls bridge with `tokio::task::block_in_place` + `Handle::block_on`; per-execution identity and buffers live in a thread-local `CURRENT_CONTEXT`.
- **How:** `ExecutionContext { tenant_id, user_id, job_id, env_vars, logs: Arc<Mutex<Vec>>, memory: Arc<Mutex<HashMap>> }` set before evaluation and cleared after [UC:src/kernel/context.rs#L6-L27], [UC:src/kernel/executor/worker/runner.rs#L27-L39]. Lesson recorded: std `Mutex`, not tokio `Mutex`, inside sync callbacks (D-UC-14); rhai `features=["sync"]` so the engine is `Send+Sync` (D-UC-04).
- **Why (intent):** Keep scripts simple (no async) while the kernel stays async; the syscall knows *who* is calling without the script passing identity.
- **Sources:** [UC:src/kernel/syscalls/db/writer.rs#L15-L61], [UC:docs/master/11_db_strong.md], [UC:docs/master/13_context.md].
- **Tags:** scripting, context, multi-tenancy

### CON-UC-006 — Loader → Runner → Committer execution pipeline
- **What:** Job execution is split into three stages with single responsibilities.
- **How:** `loader::load_execution_context` (load Job, anchored Workspace and Logic, build ProcessContext) → `runner::run_script` (scope, thread context, eval, capture result/logs/memory) → `committer::commit_results` (Redis write-back, atomic Postgres commit) [UC:src/kernel/executor/worker/mod.rs#L10-L46]. Idempotency: a Job already `DONE` returns its stored result.
- **Why (intent):** D-UC-26/D-UC-43 ("Microkernel: Loader, Runner, Committer"); each stage testable and replaceable.
- **Sources:** [UC:src/kernel/executor/worker/].
- **Tags:** process, observability

### CON-UC-007 — Smart entity: the kernel injected as resolver
- **What:** A loaded entity can carry a reference to the kernel (`_resolver: Arc<dyn EntityResolver>`), so `entity.get(key)` falls back to inherited values.
- **How:** `EntityResolver { resolve(uri), resolve_property_recursive(entity, key), resolve_property(type, key) }`; `Kernel::resolve` parses a `ubos://` URI, loads head or a specific commit, then `attach_resolver(self)` ("inject the soul") [UC:src/kernel/mod.rs#L33-L71], [UC:src/domain/resolver.rs#L11-L26], [UC:src/domain/entity.rs#L86-L99].
- **Why (intent):** Entities "gain self-loading and self-governing capability" — prototype-chain property lookup without the caller knowing the type system.
- **Sources:** as above.
- **Tags:** entity, meta-model, rehydration
