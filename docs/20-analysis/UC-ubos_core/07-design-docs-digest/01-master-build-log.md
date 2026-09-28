---
id: ANA-UC-07-1
title: "UC Digest Part 1 — Master Build Log"
status: complete
phase: P1
depends_on: [ANA-UC-07]
sources: [UC]
---

# UC digest — `docs/master` (build log)

Each file is one stage: goal → code (full files) → verification command (`cargo run -- test_x`)
→ expected output. Only design content is digested; code is summarized.

### D-UC-01 — Stages 1–5: the in-memory prototype (`01_first step` … `05_fifth`)
- `Entity {id, entity_type, version_hash, parent_hash, payload}`; `EntityStore` trait (`save/load/history`) with `MemoryStore` — Git-for-data abstraction.
- `EntityType` enum: `SysFunction` (code) vs `SysLogrum` (runtime) vs `SysData`.
- `SysFunction {input_schema/required_inputs, body_ref}` = mould; `Logrum {function_id, context_data, status, parent_logrum_id, result}` = cast; Logrum persisted because it **freezes time**, allows **long-running async** work and keeps compute **stateless**.
- Kernel `invoke`: load function, **strict wiring** (missing inputs ⇒ no Logrum), status `PENDING`, save. `execute_logrum`: code reads **only** its Logrum; result + status saved = "execution is commit".
- Demo: `app.salary.calc` calls `sys.math.adder`: parent `PENDING → WAITING_SYSCALL`, child `PENDING → DONE` with parent link, parent resumes `DONE`; printing the store is the "black box" / git log of the computation.

### D-UC-02 — Stage 6: Postgres (`06_connect db`)
- sqlx; ids `tenant:type:slug` split onto `bpu_entity_instance` / `version_chain` / `branch_head`; `load_latest` joins head → version.

### D-UC-03 — Stage 7: Rhai (`07_add rhai`)
- Rhai chosen: Deno "too heavy", Wasmtime "too low-level"; sandboxed; `sys_log` injected; the script body is the Function payload.

### D-UC-04 — Stage 8: Web (`08_web`)
- Axum `POST /deploy`, `POST /invoke`, `GET /logrum/:id`; rhai `sync` feature; kernel shared as `Arc<Mutex<Kernel>>`.

### D-UC-05 — Stage 9: Redis (`09_redis`)
- API `LPUSH`/`RPUSH` + 202; worker `BLPOP` + execute. Reasons: long tasks, flaky integrations with retries, peak shaving, CPU-heavy AI.

### D-UC-06 — Stage 10: file split (`10_slipt_file`) — model / store / kernel / api modules.

### D-UC-07 — Stage 11: DB syscalls (`11_db_strong`)
- `sys_db_get(id)`, `sys_db_commit(id, map)` via `block_in_place`; every script write is a commit.

### D-UC-08 — Stage 12: HTTP syscalls (`12_call_api`) — `sys_http_get/post` with a shared reqwest client and timeouts. *(absent in final code)*

### D-UC-09 — Stage 13: context (`13_context`)
- `tenant_id`, `user_id` on the Logrum; `ctx` map in scope; thread-local `CURRENT_CONTEXT`; DB syscalls enforce id prefix `${tenant}:` (hard tenant isolation).

### D-UC-10 — Stage 14: orchestration (`14_Orchestration`)
- `sys_invoke_async(slug, args)` creates child Logrum with `parent_logrum_id`; thread-local `ExecutionContext{logrum_id, tenant, user}`.

### D-UC-11 — Stage 15: advanced query (`15_advanced_query`) — `sys_db_query(type, filter)` with JSONB `@>`; returns arrays. *(absent in final code)*

### D-UC-12 — Stage 16: cron (`16_corn`) — `SysCron {schedule, target_func}` scanned system-wide; scheduler loop enqueues invokes.

### D-UC-13 — Stage 17: events (`17_event`) — `SysHook {on_event, filter_type, target_func}`; `sys_db_commit` publishes `SystemEvent` to Redis `ubos_event_bus`; consumer triggers hooks.

### D-UC-14 — Stage 18: logs (`18_logs`) — `Logrum.logs` (serde default); `sys_log` writes into the context; fix: std `Mutex` in sync callbacks; logs viewable via API/SQL.

### D-UC-15 — Stage 19: schema (`19_schema`) — `input_schema` as JSON Schema (`jsonschema` crate) validated before execution; smart coercion string→number/bool. *(absent in final code)*

### D-UC-16 — Stage 20: import (`20_import`) — Rhai `ModuleResolver` loads `sys:SysFunction:<path>` from Postgres; `import "sys.utils" as u`.

### D-UC-17 — Stage 21 (Core 4.0 §11): AI (`21_ai`) — `sys_ai_chat(model, prompt)`; key + base URL at boot; OpenAI-compatible (DeepSeek).

### D-UC-18 — Stage 22 (§12): VFS (`22_virtual_file_system`) — `SysFile` entity, content Base64 in JSONB; `sys_file_save/read`.

### D-UC-19 — Stage 23 (§13): policy (`23_policy`)
- `[tenant]:SysPolicy:main` script with `ctx` and `target{id, action READ|WRITE}` evaluated inside `sys_db_get/commit`; no policy ⇒ allow (fail-open; fail-closed advised for production); test: intern write denied, admin allowed.

### D-UC-20 — `24_function{slug}`, `25_send_card` (empty) — `GET /function/{slug}` returns `{name, input_schema, script_content}` to drive a dynamic form engine.

### D-UC-21 — `26_eval` — kernel `check_policy` (reuse) + `exec_ephemeral(script)`; `POST /entity` (direct save) and `POST /eval`.

### D-UC-22 — `27_commit` — "Phase 1 atomic commit", ported from Java: `EntityChange`, `CommitRequest{tenant, operator, process_name, changes[]}`; one transaction: process log → per entity instance/version/head → process-entity map; SQL patch for `bpu_process_commit_log`, `bpu_process_entity_map`.

### D-UC-23 — `28_redis` — "Phase 2 Logrum cache manager": `LogrumContext{id, tenant_id, user_id, process_id (trace), session_data, env_vars}` in Redis with TTL refresh; load from Postgres if missing.

### D-UC-24 — `29_logrum` — "Phase 3 holographic ProcessContext": `{id, identity, logrum, request, execution_trace, result, error_message, start/end, status}`; archive JSON snapshot into `bpu_process_commit_log.snapshot` (added column); mirrors Java `PersistenceSaveHandlerV1`.

### D-UC-25 — `30_strong` — "Phase 4 robustness": store accepts the snapshot as JSON (avoids infra→kernel dependency cycle); compare original vs new session data; truncate logs > 2000 lines; one `commit_execution` transaction.

### D-UC-26 — `31_5_1_memory` … `34_5_4_` — "Phase 5 Ambassador": `memory: HashMap<String, Value>` in `LogrumContext`; `ExecutionContext.memory: Arc<Mutex<…>>`; `sys_memory_set/get`; `POST /ambassador/connect {tenant_id, user_id, alias}` → deterministic id `sys:Logrum:<tenant>:<user>:<alias>`; worker seeds memory from Redis and writes it back ("long-term memory across scripts").

### D-UC-27 — `35_logrum` — "Phase 5.5": four prompts (purify domain, manager handshake with `HOST_TENANT_ID`, loader fuses actor + environment, extraterritorial `sys_db_get`); NOTE on pure vs non-pure foreign logic and data ownership (CON-UC-024).

### D-UC-28 — `51_` — `GET /logrum/{id}/status` → `{status PENDING|RUNNING|DONE|ERROR, result, logs}` from the committed Job.
