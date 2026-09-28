---
id: ANA-UC-03
title: "UC Runtime"
status: complete
phase: P1
depends_on: [ANA-UC-01, ANA-UC-02, ANA-UC-07]
sources: [UC]
---

# UC — Runtime Mechanisms

## End-to-end invoke flow (code)

```
POST /invoke {context_job_id=<workspace uuid>, func_slug, args, user_id}
  Kernel::enqueue_invoke
    load Workspace (latest) + its head commit id
    load Logic logrums/Logic/<slug> + its head commit id
    new Job entity (tenant = workspace sovereign tenant, _extends Type/Job)
      payload = Job{…PENDING, context_data=args, parent_job_id=<workspace id>}
      payload.__anchors = {context_commit_id, func_commit_id, branch}
    store.save(job) ; RPUSH ubos_task_queue <job uuid>   → 202 {job_id}
worker: BLPOP → worker_execute_logrum(job)
  loader  : load Job; anchors → load_by_commit(ctx), load_by_commit(func); provenance; ProcessContext
  runner  : scope{ctx=ProcessContext, each arg as variable}; CURRENT_CONTEXT{logs, memory}
            eval → result JSON | error; capture logs (≤2000), memory
  committer: Redis SET ubos:workspace:<id> (memory write-back, TTL 1h)
             Postgres TX: head CAS on Job → new version → process log → map OUTPUT + DEPENDS_ON_*
```
[UC:src/kernel/manager.rs#L39-L104], [UC:src/kernel/executor/worker/].

### CON-UC-020 — Execution is commit
- **What:** Every execution ends with a versioned commit of its capsule (the Job) together with an audit process and lineage rows, in one transaction.
- **How:** `commit_execution(job_entity, context_snapshot, provenance)`: (1) optimistic lock — reject when the Job's branch head differs from the loaded `head_commit_id` ("Optimistic Lock Error"); (2) `commit_transaction_atomically` (instance upsert, version insert, head upsert with `seq_num+1`); (3) `bpu_process_commit_log` row (`process_name` = function, `operator_id` = user, `app_id` = `UBOS_KERNEL`); (4) `bpu_process_entity_map` rows [UC:src/infra/store/writer/runtime.rs#L11-L98].
- **Why (intent):** D-UC-01 "execution is commit" + D-UC-38 audit: separate transactions for data and log are "catastrophic in finance/audit"; lost updates on long-lived records must be detected.
- **Sources:** [UC:docs/master/27_commit.md], [UC:docs/master/30_strong.md], [UC:docs/plan/Plan_09.md].
- **Tags:** versioning, process, observability

### CON-UC-021 — Version anchoring and provenance ("context anchoring")
- **What:** A Job pins the exact commit of its environment and its code at creation time; the worker executes those versions, not the latest; the commit records inputs and outputs.
- **How:** `__anchors {context_commit_id, func_commit_id, branch}` injected at enqueue; loader uses `load_by_commit` ("strictly no load_latest"), with a fallback to latest for jobs without anchors; `ProcessContext.provenance` → map rows `DEPENDS_ON_CTX`, `DEPENDS_ON_FUNC`, `OUTPUT` [UC:src/kernel/executor/worker/loader.rs#L27-L87]. Design adds `/audit/provenance/{process_id}` and recursive lineage ("which process produced this environment version?").
- **Why (intent):** D-UC-44/45 — deterministic replay and "error scene reconstruction": even if code or environment change later, you know exactly what ran; a complete data-lineage graph.
- **Sources:** [UC:docs/plan/Plan_15.md], [UC:docs/plan/Plan_16.md].
- **Tags:** versioning, observability, process

### CON-UC-022 — ProcessContext: the holographic execution record
- **What:** One serializable object per execution: `{id (trace), identity{tenant_id,user_id}, workspace, request{func_slug,args}, provenance, execution_trace[], result, error_message, start/end time, status PENDING|SUCCESS|ERROR}`.
- **How:** Built by the loader, pushed into the script scope as `ctx`, completed/failed by the runner [UC:src/kernel/process_context.rs#L9-L87]. Design (D-UC-24/25): archive the full ProcessContext JSON in `bpu_process_commit_log.snapshot` (JSONB) and truncate the trace (>2000 lines) or move it to object storage for heavy use. Code: the snapshot is serialized and passed to the store, which only extracts `func_slug` and `user_id`; the schema has no `snapshot` column.
- **Why (intent):** Port of the Java `ProcessContext` idea ("identity, environment, trace, result — all recorded"), making every run explainable.
- **Status:** implemented (in-memory, scope); design (persistent snapshot).
- **Sources:** [UC:docs/master/29_logrum.md], [UC:docs/plan/Plan_08.md].
- **Tags:** context, observability, process

### CON-UC-023 — Ambassador mode: stateful sessions with memory
- **What:** A long-lived environment ("embassy") that a client connects to by alias and in which successive executions share memory.
- **How:** `POST /ambassador/connect {host_tenant_id, sovereign_tenant_id, default_branch, user_id, alias}` → Workspace located by slug `genesis` or `<user>-<alias>` in the sovereign tenant; env vars `HOST_TENANT_ID`, `IS_AMBASSADOR=true` injected; cached in Redis [UC:src/kernel/workspace_manager.rs#L64-L90]. Scripts use `sys_memory_set/get` on `ExecutionContext.memory`; the runner seeds memory from the Workspace and the committer writes it back to Redis [UC:src/kernel/syscalls/memory.rs], [UC:src/kernel/executor/worker/committer.rs#L15-L19].
- **Why (intent):** D-UC-39 — turn a "serverless function platform" into an "AI-agent operating system": an agent logs in once, keeps a token in memory, and continues multi-turn work without passing state around; all operations still audited.
- **Sources:** [UC:docs/master/31_5_1_memory.md]…[UC:docs/master/34_5_4_.md], [UC:docs/plan/Plan_10.md].
- **Tags:** context, ai, multi-tenancy

### CON-UC-024 — Separation of environment and execution; host vs sovereign tenant
- **What:** The environment (Workspace: sovereign tenant, memory, env vars) is stable; the execution (Process: actor user, request, trace) is transient and assembled per run.
- **How:** Design Phase 5.5 steps: purify `LogrumContext` (remove user/process), manager injects `HOST_TENANT_ID` on handshake, loader fuses actor (from the Job) with environment (from Redis), syscalls allow "extraterritorial" reads of host data when `env.HOST_TENANT_ID` authorizes it. Rule for foreign logic (35 NOTE): **pure logic** of a foreign tenant may run on the local tenant's environment and write under the local tenant; **non-pure logic** runs in the embassy environment, stores data locally but **under the foreign tenant**, invisible to the local tenant. Code: steps 1–3 done; step 4 not wired (runner passes empty `env_vars`; `sys_db_get` ignores them).
- **Why (intent):** "Logrum is environment, Process is execution, User is in Process" — a precise permission model for code that crosses tenant borders (apps from a vendor running inside a customer).
- **Sources:** [UC:docs/master/35_logrum.md], [UC:docs/plan/Plan_11.md].
- **Tags:** context, multi-tenancy, permission
- **Status:** partly design.

### CON-UC-025 — Ephemeral evaluation (console / REPL)
- **What:** Run an ad-hoc script synchronously within a Workspace, still leaving an audit trace.
- **How:** `/eval {script, user_id, context_job_id}` → `exec_ephemeral`: loads the Workspace, builds a transient Logic and a `Job` (`RUNNING`), sets `ctx.identity`, evaluates, formats `Dynamic` results as JSON/text, saves the Job with result and logs [UC:src/kernel/executor/ephemeral.rs#L14-L74]. The UI resource browser uses `/eval` with `sys_db_get`/`sys_db_history` as its query language.
- **Why (intent):** D-UC-21/D-UC-36 — "script as app": the database is meant to be written only by scripts, so the console is a primary tool ("god mode"); scripts double as a query language.
- **Sources:** [UC:docs/master/26_eval.md], [UC:docs/plan/Plan_07.md].
- **Tags:** scripting, api, observability

### CON-UC-026 — Orchestration by child jobs
- **What:** A script dispatches another Logic asynchronously; the child Job records its parent.
- **How:** `sys_invoke_async(func_slug, args)` creates a Job with `parent_job_id` = current execution and enqueues it; returns the child id [UC:src/kernel/syscalls/task.rs#L15-L54]. Stage-5 demo: salary → adder, parent `WAITING_SYSCALL` until child `DONE` (D-UC-01).
- **Why (intent):** D-UC-10 — "call teammates": order → stock → shipping chains; the audit shows a full call tree.
- **Tags:** workflow, process, scheduling

### CON-UC-027 — Time and data triggers as entities (Cron, Hook)
- **What:** Schedules and reactions are entities; kernel services interpret them.
- **How:** Scheduler polls every 1 s, scans `Cron` entities (`schedule` cron expression, `target_func`, optional `context_id`) and enqueues due ones with user `scheduler` [UC:src/services/scheduler.rs#L12-L62]. `sys_db_commit` pushes `SystemEvent{event_type: entity_committed, entity_id, entity_type, tenant_id, timestamp}` to Redis `ubos_event_bus`; the event bus scans `Hook` entities (`on_event`, `filter_type`, `target_func`, optional `context_id`) and enqueues matches with args `trigger_entity_id`, `event_type` [UC:src/services/event_bus.rs#L12-L57].
- **Why (intent):** D-UC-12/13 — "the system gets a sense of time" and "pull one hair and the whole body moves": reactive architecture without code changes; Plan_15 requires triggered jobs to get a default genesis context so they also benefit from anchoring.
- **Sources:** [UC:docs/master/16_corn.md], [UC:docs/master/17_event.md].
- **Tags:** events, scheduling, workflow

### CON-UC-028 — Policy as code
- **What:** Authorization decided by a tenant-owned script: `Policy:main` receives `ctx{tenant_id,user_id}` and `target{id, action READ|WRITE}` and returns bool.
- **How:** `Kernel::check_policy` loads the policy, runs it in a fresh engine, errors ⇒ deny, **no policy ⇒ allow** ("fail-open for development; fail-closed for production") [UC:src/kernel/security.rs#L5-L33]. Genesis `fn_sys_policy`: root allowed, own-tenant prefix allowed, `logrums` readable. Design stage 13 intercepts `sys_db_get/commit`; in the final code `check_policy` exists but is not called by syscalls; syscalls apply a hard-coded tenant rule instead (own tenant or `logrums`).
- **Why (intent):** D-UC-19 — finer than RBAC ("interns read, managers write"), changeable as data.
- **Sources:** [UC:docs/master/23_policy.md], [UC:docs/db/genesis_data/14_kernel_scripts.json].
- **Tags:** permission, scripting, governance

### CON-UC-029 — Code reuse by importing entities as modules
- **What:** `import "x" as m;` in Rhai resolves to a stored Logic entity compiled into a module.
- **How:** `UbosModuleResolver` loads the Logic, compiles `script_content`, evaluates it as a module [UC:src/kernel/resolver.rs#L16-L58]. Design extensions: standard library `sys.std.http/time/ai`, import by `ubos://` URI (D-UC-35, D-UC-47). Divergence: resolver builds `sys:Logic:<path>` (legacy id) while storage now uses UUIDs.
- **Why (intent):** D-UC-16 — "package management embryo"; UBOS becomes a code platform.
- **Tags:** scripting, plugin

### CON-UC-030 — Runtime guards and observability capture
- **What:** Scripts are bounded and their output captured.
- **How:** `set_max_operations(5000)` with an `on_progress` hook that logs the violation into the execution context; `on_print` / `on_debug` go both to tracing and to the context log; trace truncated to 2000 lines; logs saved on the Job (`logs`) and ProcessContext (`execution_trace`) [UC:src/kernel/factory.rs#L20-L71], [UC:src/kernel/executor/worker/runner.rs#L66-L79]. `sys_push_card` writes structured UI messages into the same log stream (CON-UC-056).
- **Why (intent):** D-UC-14, Plan_14 task 3 — prevent infinite loops; make the console the user's window into execution.
- **Tags:** observability, scripting

### CON-UC-031 — Direct entity commit syscall with upsert-by-locator
- **What:** `sys_db_commit(slug, type, data)` saves an entity in the caller's tenant: update if the locator exists (UUID kept), else create; emits `entity_committed`.
- **How:** [UC:src/kernel/syscalls/db/writer.rs#L15-L61]; the genesis script `save_entity_raw` wraps it for the UI JSON editor, returning `{status, final_id, slug}` [UC:src/kernel/bootstrap/genesis.rs#L6-L37]. Reads: `sys_db_get(uuid | ubos://)` with tenant check; `sys_db_history(id)` returns up to 50 versions `{version, timestamp, data}` [UC:src/kernel/syscalls/db/reader/].
- **Why (intent):** D-UC-41 — the "universal creator": one raw editor function from which everything else is created ("use code to create the editor, then the editor to create everything").
- **Tags:** persistence, scripting, api

### CON-UC-032 — Idempotent, retry-safe worker
- **What:** Re-delivered jobs do not re-execute.
- **How:** Worker returns the stored result when the Job is already `DONE`; commit uses the optimistic lock (CON-UC-020) [UC:src/kernel/executor/worker/mod.rs#L19-L22].
- **Tags:** process, idempotency

### CON-UC-033 — Genesis bootstrap: first function, then data, then environment
- **What:** Idempotent self-initialization at every server start.
- **How:** `ensure_genesis_data`: (1) ensure kernel function `logrums/Logic/save_entity_raw` (built-in script); (2) load all `docs/db/genesis_data/*.json` sorted by name, accepting UODS `{data:[…]}` or plain arrays; each item → slug (`code`/`id`/`target_id`), type (`entity_type`/`type`/`target_type`, `sys.logic`→Logic), `content`→`script_content`, auto-inject `_extends: ubos://logrums/Type/<type>`, save only if the locator is absent; (3) ensure the genesis Workspace and sync it to Redis [UC:src/kernel/bootstrap/mod.rs#L8-L25], [UC:src/kernel/bootstrap/loader/].
- **Why (intent):** D-UC-41/43 — "minimum viable system": one god user, one god environment, one universal editor; genesis moved from Rust constants to JSON data so the bootstrap itself is data. Contrast: UB loads genesis through kernel operations with topological ordering (CON-UB-031).
- **Tags:** boot, metadata

### CON-UC-034 — Constitutional functions (design)
- **What:** Critical functions guard themselves by tenant identity at the top of the script (see CON-UC-019).
- **Sources:** [UC:docs/plan/Plan_13.md].
- **Tags:** governance, permission

### CON-UC-035 — Recorded robustness review
- **What:** A self-audit of the ported design listing four defects and fixes: broken atomicity (data and log in separate transactions → one transaction), missing optimistic lock (lost update on long-lived Logrum → version check), missing Redis write-back of session state, and oversized archives (truncate, compress, or store in object storage with a key). Plus the Plan_01 critique (mutex, dual write/outbox, worker concurrency) and Plan_14 roadmap (status API, WebSocket push, unified error format, quotas, cross-tenant audit, semantic function discovery, vector memory).
- **Why (intent):** Explicit non-functional requirements for the platform spec (NFR candidates for P3/P4).
- **Sources:** [UC:docs/plan/Plan_09.md], [UC:docs/plan/Plan_01.md], [UC:docs/plan/Plan_14.md].
- **Tags:** process, persistence, observability
