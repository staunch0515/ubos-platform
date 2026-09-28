---
id: ANA-US-03
title: "US Runtime"
status: complete
phase: P1
depends_on: [ANA-US-01, ANA-US-02, ANA-UC-03]
sources: [US]
---

# US — Runtime Mechanisms

UC mechanisms retained unchanged: execution is commit (CON-UC-020, with the difference in
CON-US-025), version anchoring (CON-UC-021), ProcessContext (CON-UC-022), Ambassador/memory
(CON-UC-023/024), eval (CON-UC-025), child jobs (CON-UC-026), Cron/Hook (CON-UC-027), policy
(CON-UC-028), import (CON-UC-029), guards (CON-UC-030), genesis bootstrap (CON-UC-033).

### CON-US-020 — Unit-of-work session per execution (identity map)
- **What:** `EntitySession { identity_map: URI → EntityEntry{composite, state Clean|Dirty|New|Deleted, loaded_commit_id}, assembler, store, tenant }`.
- **How:** `load(uri)` / `load_by_id(uuid)` cache composites; `update(uri, payload)` marks Dirty; `attach_new(entity)` assembles ancestors immediately (so reads before flush already see inherited values) and marks New; `flush(provenance)` commits every Dirty/New entry via `store.commit_execution` and marks it Clean [US:crates/ubos_kernel/src/session/manager.rs#L9-L141], [US:crates/ubos_kernel/src/session/state.rs]. A session lives in `ExecutionContext.session` [US:crates/ubos_kernel/src/context.rs#L11-L22].
- **Why (intent):** Read-your-writes inside a script, one identity per object, batching of writes until the script succeeds (script failure ⇒ nothing flushed in sync mode), foundation for optimistic locking (`loaded_commit_id`, check not yet implemented).
- **Tags:** persistence, versioning, process

### CON-US-021 — Syscalls through the session: merged reads, staged writes
- **What:** `sys_db_get(uri|uuid)` returns `composite.render_merged_payload()` (inherited, defaults applied); `sys_db_commit(slug, type, map)` loads `ubos://tenant/type/slug` into the session and updates it, or attaches a new entity; returns the UUID [US:crates/ubos_kernel/src/syscalls/db/reader/get.rs#L10-L49], [US:crates/ubos_kernel/src/syscalls/db/writer.rs#L14-L68].
- **Differences vs UC:** no tenant check in `sys_db_get`; `sys_db_commit` no longer pushes `entity_committed` events (so Hooks fire only from other paths); writes are deferred.
- **Tags:** scripting, persistence, inheritance

### CON-US-022 — Action FSM engine: metadata-defined transitions
- **What:** `exec_action_transition(action_uri, current_state, event, payload)`.
- **How:** (1) resolve the Action; (2) find `transitions` and `states` walking the `_extends` chain (own payload, then `default_values`, depth ≤ 10); (3) rule = `transitions[event]`, must have `from == current_state`; (4) logic = rule.`logic` (URI) or rule.`logic_ref` (name of a property holding a Logic URI, resolved through inheritance — e.g. `generator_logic`); (5) run it with `exec_logic_sync` and scope `{payload, current_state, next_state, event}`; (6) return `{status, next_state, ui_policy: states[next].ui_policy, data: logic result, available_events: events whose from == next_state}` [US:crates/ubos_kernel/src/action_engine.rs#L32-L179]. Exposed as `POST /action/transition` [US:apps/ubos_server/src/api/handlers/action.rs#L99-L125].
- **Why (intent):** Server-driven wizards: the server owns the state machine, the client only renders the returned schema and offers the returned events.
- **Tags:** workflow, state-machine, ui-rendering

### CON-US-023 — Generator / Submitter two-step pattern ("three-piece set")
- **What:** An interaction = **Action JSON** (params schema + default values incl. logic references) + **Generator Logic** (loads data and *builds the next form*: returns `{data, ui_schema}`) + **Submitter Logic** (validates and persists; returns `{status, msg}`).
- **How:** `logic_universal_load` resolves `ubos://{tenant}/{type}/{id}` and returns `{target_id, target_type, entity_content}` with a schema (ID editable "to clone", JSON content editor); `logic_universal_save` commits it; `logic_load_logic`/`logic_save_logic` edit a Logic entity's name, description and script (Monaco) [US:docs/db/genesis_data/16_entity_actions.json], [US:docs/db/genesis_data/17_logic_actions.json]. Earlier API variant: `/action/{slug}/init`, `/action/prepare` (scope `params`), `/action/submit` (scope `payload`, `context_params`).
- **Why (intent):** D-US-06 — deliver features as a standard package instead of scattered patches; generators make UIs dynamic without front-end code.
- **Tags:** workflow, ui-rendering, scripting

### CON-US-024 — Synchronous logic execution with flush-on-success
- **What:** See CON-US-003; the sync executor creates a fresh session and flushes it only if the script succeeded ("database write failure must be returned, not reported as success").
- **Sources:** [US:crates/ubos_kernel/src/executor/sync.rs#L68-L93].
- **Tags:** process, persistence

### CON-US-025 — Worker commit in two steps
- **What:** Runner keeps the thread context alive; committer (1) writes Workspace memory to Redis, (2) flushes the session (business entities, each via `commit_execution` with the job's provenance), (3) commits the Job record with ProcessContext, (4) clears the context [US:crates/ubos_kernel/src/executor/worker/committer.rs#L8-L71].
- **Note:** flush errors are logged and the job record is still saved; business entities and job record are separate transactions (UC design goal: one transaction, CON-UC-020).
- **Tags:** process, versioning

### CON-US-026 — Slice projection and merge ("view as ancestor type") (design + partial code)
- **What:** Physical storage uses the **concrete type** + id ("super entity", full payload); a **slice type** (must be the concrete type or an ancestor — Is-A) is a mould: **read** = super entity × slice type → slice data + UI schema; **write** = slice data + super entity + slice type → merged super entity.
- **How:** Design functions `sys_slice_project`, `sys_slice_merge`; action `act_universal_editor` with params concrete id/type and slice type; earlier variants: `sys_render_schema(type, {include, exclude, overrides})` to hide system fields, vary by scenario (create vs edit password), restrict to a few fields; recursive schema generation only for **inline** nested properties — cross-entity type references are passed as URIs and loaded lazily by the client (avoid cycles and blow-up). Code: `sys_load_and_project(entity, view_type)` with Is-A check and generic-`Entity` special case [US:crates/ubos_kernel/src/engine/schema.rs] (not compiled in), [US:crates/ubos_kernel/src/schema_engine.rs] (not registered).
- **Why (intent):** D-US-07 — Type stays the single source of truth; an Action is a projection (view) of it; the kernel does the heavy slicing so scripts stay minimal; one entity can be edited through many lenses without data duplication.
- **Sources:** [US:docs/front/渲染.md].
- **Tags:** ui-rendering, meta-model, inheritance

### CON-US-027 — Slice voting protocol (design)
- **What:** Behaviours return `Decision {allow, reason, priority (100 normal … 999 constitutional), is_veto}`; any veto with `allow=false` aborts; otherwise weighted by priority.
- **Why (intent):** Resolve conflicts between slices ("Rel_Admin allows delete, Rel_Lock forbids"); infrastructure slices (security) always dominate application slices.
- **Sources:** [US:docs/front/FullSlice.md].
- **Tags:** rules, permission, plugin

### CON-US-028 — Context blackboard between slices (design)
- **What:** A mutable per-operation map shared by all slices (`ctx.set("keywords")` by a parse slice, read by tag and notify slices).
- **Why (intent):** Loose coupling: slices cooperate without knowing each other.
- **Sources:** [US:docs/front/FullSlice.md].
- **Tags:** context, plugin

### CON-US-029 — Causal traceability of slice execution (design)
- **What:** Each complex operation records a slice execution log (step, slice, result, cost, field mutations "price 100 → 80 by Rel_Discount"), visualized as Gantt/flow.
- **Why (intent):** Flexibility without opacity: "why did this value change / why was it rejected?"; also the substrate for LLM explanations (CON-US-061).
- **Sources:** [US:docs/front/FullSlice.md].
- **Tags:** observability, rules

### CON-US-030 — Logic stored in relation types (design)
- **What:** Relation-type entities carry behaviour scripts (Rhai or WASM), e.g. `Rel_Discount.script_calculate`; hot update by committing a new version; rollback of business rules via the version chain; per-tenant polymorphism under the same relation URI.
- **Sources:** [US:docs/front/FullSlice.md].
- **Tags:** scripting, versioning, plugin

### CON-US-031 — Cross-tenant synchronous call syscall (stub)
- **What:** `sys_invoke("ubos://…", args)` intended to route a synchronous call (via Redis `SystemEvent::SyncCall` to the scheduler) to another tenant's function; currently only simulates `std_math` addition [US:crates/ubos_kernel/src/syscalls/host_functions.rs#L7-L32].
- **Tags:** integration, multi-tenancy, scripting
