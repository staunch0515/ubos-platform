---
id: ANA-LS-03
title: "LS Runtime"
status: complete
phase: P1
depends_on: [ANA-LS-01, ANA-LS-02]
sources: [LS]
---

# LS — Runtime Mechanisms

### CON-LS-020 — Process step: state in, script, state + emits out
- **What:** `Emit` on a process entity runs one transition.
- **How:** `drive_process_step`: deserialize `ProcessState` from the entity payload → Rhai scope `{current_step, signal, variables, args, ctx}` → script returns a map with `next_step`, `variables`, `emits` → update state → commit the new process version as a ChangeSet → deserialize `emits` into `Vec<UbosOp>` and return them (the server streams them to the client) [LS:crates/ubos_kernel/src/engine/orchestrator/process.rs#L12-L86], [LS:crates/ubos_kernel/src/script/adapter.rs#L51-L80]. Genesis scripts use a second convention: variables `CURSOR`, `SIGNAL`, `MEMORY`, `PAYLOAD`; result `{next, instructions: [{op: Mutate, payload}], ui_schema, memory, ui_error}`.
- **Why (intent):** State (instance) and logic (definition) separated; each step is a commit; side effects are data.
- **Tags:** workflow, state-machine, versioning

### CON-LS-021 — "Scripts only give orders, the engine does the running"
- **What:** Scripts never traverse metadata; they call `ctx.emit(<BehaviorType URI>, args)` and receive a **list of results**, one per relation of the target entity that fills that slot; they only prepare parameters, emit, and aggregate.
- **How:** Design: engine loads entity → entity type → relation types → configured behavior for the slot → runs it (with refine chain). Code: `ctx.emit` is registered on the Rhai `ProcessContext` type and calls `behavior::dispatch_behavior(slot, args)`, currently a static mock returning `[{provider: Kernel_Static_Mock, schema: [fields]}]` for `SchemaProvideSlot` [LS:crates/ubos_kernel/src/script/adapter.rs#L26-L48], [LS:crates/ubos_kernel/src/semantic/behavior.rs].
- **Why (intent):** D-LS-01 — generic processes (e.g. a universal editor) work for every entity type; business variation lives only in relation-type configuration.
- **Tags:** scripting, plugin, events

### CON-LS-022 — Three-stage action protocol (Init → Interact → Exec)
- **What:** Stage 1 **Init**: emit `SchemaProvideSlot` on the *action entity* → parameter form (e.g. EntityPicker from `param_target_ref`). Stage 2 **Interact** (on the target entity): `PermissionSlot` (any `allowed=false` → error), `ComputeFieldSlot` (merge computed values), `FieldMaskSlot` (masked values), `SchemaProvideSlot` (field UI) → return `{ui_schema, data, meta{target_id, original_data}}`. Stage 3 **Exec**: `DefaultValueSlot` (fill missing), `TransformInputSlot` (clean), `ValidationSlot` (collect `valid=false` messages → errors), `BeforeUpdateSlot` (old/new data) → return `Mutate` instructions per relation.
- **Sources:** [LS:claude.md], [LS:genesis_data/07_action.json].
- **Why (intent):** A standard, complete interaction pipeline (security, computation, privacy, UI, defaults, cleansing, validation, hooks) assembled from slots — no per-screen code.
- **Tags:** workflow, ui-rendering, permission, validation

### CON-LS-023 — Type and relation assembly by inheritance (deep merge)
- **What:** Types: effective payload = deep-merge of parents' effective payloads (in `_extends` order) then self (objects merged recursively, other values replaced). Relation types: effective behaviors = parents' behaviors then self's (child overrides; later parents override earlier). Both: cycle detection with a visited commit list (cloned per branch for multiple inheritance), memoized by commit id.
- **How:** [LS:crates/ubos_kernel/src/semantic/assembler.rs#L8-L102], [LS:crates/ubos_kernel/src/semantic/relation/assembler.rs#L11-L116], [LS:crates/ubos_kernel/src/semantic/merge.rs].
- **Divergence:** parents are read from `_extends`, while genesis uses the `…/MetaRelationType/extends` relation.
- **Tags:** meta-model, inheritance, cache

### CON-LS-024 — Genesis bootloader: ordered, idempotent, one process per file
- **What:** Skip when `logrums/MetaType/MetaType` exists; else load `01_meta_type` … `06_action` in dependency order; each file → one `ProcessContext` (`genesis-load-<file>`, user `system-bootloader`, app `UBOS_KERNEL`) → all its entities as ChangeSets in one atomic commit; missing files are warned and skipped [LS:crates/ubos_kernel/src/engine/bootloader.rs#L15-L131].
- **Why (intent):** Genesis goes through the same commit path as business writes (audit + lineage per file).
- **Tags:** boot, metadata, versioning

### CON-LS-025 — Emits as intents; recursive dispatch (design)
- **What:** Scripts return `emits` instead of calling the kernel; the kernel dispatches them (design: `kernel_bus.dispatch(cmd)` recursively — "the process conductor tells the kernel: send a signal to Order, send a mail to User"); in code the ops are returned to the client.
- **Sources:** [LS:crates/ubos_kernel/src/engine/process_runner.rs].
- **Tags:** events, workflow, scripting

### CON-LS-026 — Dry-run as a first-class pipeline mode (design)
- **What:** Every mechanism receives `dry_run`; reactor emits nothing in dry run; used "for UI generation or checks without side effects"; `PipelineFlow::Break` ends normally (e.g. after schema generation).
- **Sources:** [LS:crates/ubos_kernel/src/mechanism/mod.rs], [LS:crates/ubos_kernel/src/pipeline/context.rs].
- **Tags:** process, ui-rendering

### CON-LS-027 — Blocking mechanisms: policy, prerequisite, schema (design)
- **What:** Policy (subject, object, action, relations → PDP), prerequisite logic via `sys:rel/prerequisite` (allowed + reason), schema validation via `sys:rel/schema`; any `Err` stops the pipeline before `Implement` runs.
- **Sources:** [LS:crates/ubos_kernel/src/mechanism/policy.rs], [LS:crates/ubos_kernel/src/mechanism/prerequisite.rs], [LS:crates/ubos_kernel/src/mechanism/schema.rs].
- **Tags:** permission, rules, validation

### CON-LS-028 — Reactor: subscription topology produces notifications (design)
- **What:** After a change, find observers of the entity URI in a topology graph and emit `Notify{target, event: EntityChanged, payload}` ops (queued or recursive in the same transaction).
- **Sources:** [LS:crates/ubos_kernel/src/mechanism/reactor.rs].
- **Tags:** events

### CON-LS-029 — Query answered as an emitted result
- **What:** `Query{criteria=URI}` → Ack then `Emit{client://current, QueryResult, {uri, payload, version}}` (or `{error}`) [LS:crates/ubos_kernel/src/engine/orchestrator/mod.rs#L83-L118].
- **Why (intent):** One uniform downstream channel (Emit) for all server-to-client data.
- **Tags:** query, protocol
