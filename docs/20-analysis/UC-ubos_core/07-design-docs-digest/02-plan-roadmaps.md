---
id: ANA-UC-07-2
title: "UC Digest Part 2 — Plans and Roadmaps"
status: complete
phase: P1
depends_on: [ANA-UC-07]
sources: [UC]
---

# UC digest — `docs/plan`

The plan files are AI-assistant planning answers: roadmaps split into stages with a
copy-paste prompt per stage, plus critiques of delivered code.

### D-UC-30 — Plan_01: "Five steps to build the UBOS core" + distributed-architecture critique
- Stages: foundation (Entity + `EntityStore` trait + `MemoryStore`), skeleton (`SysFunction`, `Logrum` stack-frame entity), core (Kernel boot + invoke with wiring check, PENDING), power (executor; code reads only the Logrum; execution is commit), completion (salary→adder demo printing history like `git log`).
- Critique of the Redis version: `Mutex<Kernel>` serializes requests (pools are already thread-safe — drop the mutex); dual write Postgres→Redis can leave zombie `PENDING` jobs (roll back, or **transactional outbox** with a poller); worker executes one job at a time (spawn tasks). Praises: 202 async decoupling, `BLPOP` instead of polling, audit-by-default (job exists before the queue).

### D-UC-31 — Plan_02: Core 2.0 — "empower the script"
- Priorities: BPU stdlib (`sys_db_get`, `sys_db_update` where every change is a commit, `sys_http_post`), tenant context (`X-Tenant-ID`, token → user, `ctx.tenant/operator`, prefix isolation), orchestration (`sys_invoke_async`, parent ids → call tree). Metaphor: "a genius locked in a dark room" gets eyes, voice and identity.

### D-UC-32 — Plan_03: Core 3.0 — query, time, events
- Stage 5 JSONB search, stage 6 `SysCron` + built-in scheduler, stage 7 data-change hooks ("reactive architecture").

### D-UC-33 — Plan_04: Core 3.0 second half — usability & engineering
- Pain points: logs visible only on server; unvalidated parameters; copy-paste code. Stages: execution logs into Logrum, JSON-Schema contracts, module import.

### D-UC-34 — Plan_05: Core 4.0 — intelligence & perception
- AI (`sys_ai_chat`), VFS (reports, images, Excel — "enterprise must-have"), policy as code (script decides permission; test intern vs admin). Linked to the Executable Book vision.

### D-UC-35 — Plan_06: 5.0 productization
- Summary of kernel capabilities (multi-tenant, distributed, AI, schema + policy, cron/hooks/files/imports) → next: Web IDE (Linear style), standard library `sys.std.*`, killer demo (investment weekly report), options IDE/CLI/demo.

### D-UC-36 — Plan_07: Generative application shell
- User NOTE: data must be produced only by scripts; a console to run scripts; a raw JSON save; golden-ratio vertical split, left = previous page (active), right = passive area where the backend pushes pages.
- Answer: SDUI + command pattern; Logrum = session state container revived in Redis/memory; phases: dynamic form engine (`GET /function/{slug}` → JSON-Schema form → invoke with `parent_logrum_id` to continue in context), split screen with streaming (`sys_push_card`, structured logs, SSE/WebSocket), console + direct entity ops.

### D-UC-37 — Plan_08: Architecture upgrade (Java ProcessContext → Rust)
- User NOTE: one commit may create many entities (do not call it "batch"); Logrum is an entity with its own cache; page submits carry the Logrum name → cache → + session → big ProcessContext → background execution.
- Plan: entity as change set (`EntityChange`, `CommitRequest{tenant, operator, process_name, changes}`); Logrum context manager (Redis, TTL, load from Postgres); dual persistence (business data in `bpu_entity_*`, execution scene in process log).

### D-UC-38 — Plan_09: Highest-standard audit of the port
- Four defects: broken atomicity (two transactions), missing optimistic locking (lost update on long-lived Logrum; check version, raise "data expired"), missing Redis write-back of session changes, big-object archive risk (TOAST bloat; truncate / async to S3/MinIO / split trace). Fix = Phase 4 (`commit_execution_result`, write-back, worker wiring).

### D-UC-39 — Plan_10: Ambassador mode
- Definition: Logrum = an **embassy** resident in the target tenant; persistent "diplomatic immunity" (identity, token, connections); multi-turn commands with continuous context. Missing pieces: handshake by alias, memory shared between scripts. Steps: memory field, memory syscalls (seed/write-back through thread-local), `/ambassador/connect`, persistence of state.
- Positioning section: stateless (Lambda-like, "utility") vs stateful ("infrastructure/platform", like Durable Objects / Assistants API memory); valuation claims (not requirements); the demo that proves it: login once, refresh, reuse token.

### D-UC-40 — Plan_11: Phase 5.5 — separation of environment and execution
- Same four steps as D-UC-27; guiding sentence: "Logrum is the environment, Process is the execution, the User is in the Process, everything is an entity."

### D-UC-41 — Plan_12: Bicameral UI and the minimum viable system
- Active zone = intent (where am I — Logrum; what — search); Passive zone = event-driven push (approval page, form). Bootstrap: god user, god environment, universal creator function. Genesis: root tenant/user, default Logrum `sys:Logrum:sys:root:main`, first function `save_entity_raw` with `ui_schema {widget: JsonEditor, fields}`; APIs `POST /entity/search`; bootstrap in Rust on start ("self-bootstrapping wherever deployed"). "Use code to create the editor; use the editor to create everything."

### D-UC-42 — Plan_13: Re-rooting to `logrums` ("collective intelligence")
- Genesis reset (sole sovereign tenant `logrums`), constitution scripts `create_user`, `create_community` with tenant guard, front-end default connection alignment.

### D-UC-43 — Plan_14: Project handoff + next roadmap
- Handoff: entity everything; `SysLogrum` = task/environment, API returns its id (a "work order"); addressing `{tenant}:{type}:{slug}`; microkernel Loader/Runner/Committer; runner uses `eval::<Dynamic>` + `from_dynamic` (objects returnable); `on_print` → tracing + context logs; genesis from `docs/db/genesis_data/*.json`; `save_entity_raw` auto-prefixes tenant and type; save flow end to end.
- Roadmap: observability loop (status API, WebSocket, unified errors), governance (default policy scripts, quotas via instruction counter, audited cross-tenant access), AI-native (`sys_discovery`, AI deploy pipeline with syntax check, vector long-term memory). Task prompts 1–4 (status API, policy script, `set_max_operations(5000)`, SysHook for `sync_to_search`).

### D-UC-44 — Plan_15: Version anchoring (four prompts)
- Store: `get_head_commit_id(entity)`, `load_by_commit(commit)`; manager: `__anchors {context_commit_id, func_commit_id}` in the Task payload; loader: `load_by_commit` ("strictly no load_latest"), fallback for old tasks, `ProcessContext.provenance`; committer: map rows `DEPENDS_ON_CTX`, `DEPENDS_ON_FUNC`, `OUTPUT` in the same transaction; scheduler/event bus: default genesis context (`{tenant}:SysLogrum:genesis`), hook context priority (hook config → trigger entity's tenant genesis).

### D-UC-45 — Plan_16: Dynamic EntityType + provenance API
- `EntityType` as a string wrapper (sqlx transparent); logical tables via the `(tenant, type, slug)` unique index; types defined by meta entities; consistency check between id and type. Audit API `/audit/provenance/{process_id}` via `timeline.rs`; suggestion: recursive lineage graph.

### D-UC-46 — Plan_17: Showing async work in the UI
- Submit (202) → poll `/logrum/{id}/status` → final; UI variants (status tag, live console with incremental logs, notification center with provenance link); show current branch; confirm "save to `{branch}`"; history via `sys_db_history` with time travel.

### D-UC-47 — plan_18_ubos: the `ubos://` protocol
- Intent addressing: scheme / namespace (tenant or LogrumContext) / logical table (dynamic type) / slug; snapshot awareness (same URI resolves to anchored commit inside a ProcessContext). Uses: field references, embassy sovereignty declaration, full-address targets for Cron/Hook (cross-tenant Open API), front-end resource params, `import` by URI. Values: unified API semantics, semantic audit paths, shadow environments. Implementation plan: `UbosUri` parser, then `sys_db_get` accepts URIs ("no hard introduction; extend naturally").

### D-UC-48 — plan_19: Universal Resource Browser
- Command bar (only `ubos://`, future wildcards), state editor (60 %, Monaco/CodeMirror, HEAD editable, history read-only), timeline (40 %, `sys_db_history`, commit short id, relative time, operator, click = time travel). Three front-end prompts.
