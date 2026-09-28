---
id: ANA-US-02
title: "US Core Model"
status: complete
phase: P1
depends_on: [ANA-US-01, ANA-UC-02]
sources: [US]
---

# US — Core Model

Entity, EntityType, URI, Workspace, Job, Logic and the genesis ontology are as in UC
(CON-UC-010…019). US-specific concepts follow.

### CON-US-010 — Composition model: Behavior → Relation Type → Entity Type (design)
- **What:** Replace "field types" with composition: **Behavior** (atomic capability: `store_text`, `valid_regex`, `ui_input`), **Relation Type** (a set of behaviours = a property with characteristics, e.g. `prop_email` = store text + regex + email input; also `sys_descriptor`, `rel_composition`), **Entity Type** (the relations an object must/may have, e.g. `User` must have `prop_email`). Genesis loads behaviors → relations → types.
- **Why (intent):** "Composition over inheritance"; N×M feature combinations become N+M building blocks ("full-slice development").
- **Sources:** [US:docs/front/UBOS.md], [US:docs/front/FullSlice.md].
- **Tags:** meta-model, type-system, plugin

### CON-US-011 — Core meta-relations `sys_primitive` and `sys_descriptor` (design)
- **What:** Every entity is driven by `sys_primitive` (proof of existence: id, tenant, slug, type — for the kernel: addressing and permission) and `sys_descriptor` (readable face: title/name, description, icon — for humans: uniform header card and full-text search).
- **Why (intent):** Separates machine identity from human presentation for every object uniformly.
- **Sources:** [US:docs/front/UBOS.md].
- **Tags:** identity, ui-rendering, search

### CON-US-012 — CompositeEntity: head + ancestor chain, merged view
- **What:** An in-memory object: `head` entity + `ancestors` (immediate parent → root) assembled by following `_extends` URIs with **circular-inheritance detection**.
- **How:** `get_property(key)`: head payload → head schema default (`properties[].default`) → each ancestor payload → ancestor default. `render_merged_payload()`: iterate root → head, apply schema defaults then explicit payload, skipping `_`-prefixed keys, so children override parents and explicit values override defaults [US:crates/ubos_kernel/src/graph/composite.rs#L17-L111], [US:crates/ubos_kernel/src/graph/assembler.rs#L18-L109]. Supports `?commit=` for the head.
- **Why (intent):** Scripts see the effective object (e.g. `Dog/buddy.has_legs` inherited from `Type/Animal`, `category` overridden by `Type/Dog`).
- **Sources:** as above; [US:apps/ubos_cli/tests/demo_full_flow.rhai].
- **Tags:** meta-model, inheritance, rehydration

### CON-US-013 — Entity-Oriented Programming calculus (design)
- **What:** Axioms: (1) entity as a spacetime stream `E = (ID, Σ, S0, {Ti})`, `S_t = fold(S0, T0…Tt)`; (2) behaviour as pure, deterministic function `S(n+1) = Action(S(n), Context, Input)` (enables revert, cherry-pick, AI sandbox prediction); (3) relations as a first-class graph layer, not hidden foreign keys.
- **How:** JSON is the AST of an Entity Definition Language: `EntityDefinition {metadata {namespace, symbol, abstract, final}, topology (algebraic types: Struct, Scalar::Decimal(precision, scale), StateVector{states, initial}, Vector<Struct> with cardinality), invariants [{name, expression}] ("violation = panic"), derivations {name: {return_type, pure_logic}}}`; `ActionDefinition {target_symbol, symbol, guards {state_check, auth_check}, mutation {engine: rhai, script → Transaction.transition().push().emit()}, effects [declared webhooks]}`; Rust `EntityFrame {id, schema_hash, …}` generic container; features: orthogonal persistence, structural polymorphism (schema changes with state), time-travel debugging.
- **Why (intent):** "DDD + Event Sourcing + FRP": JSON = serialized type system, Rhai = state-transition operator, Rust = VM executing the rules.
- **Sources:** [US:docs/front/EOP.md].
- **Tags:** dsl, state-machine, type-system, rules

### CON-US-014 — Metadata specification v1.0 (URN categories, reserved keys)
- **What:** URN `ubos://{tenant}/{Category}/{Code}` with categories **Type**, **Action**, **Logic**, **Widget**; mandatory meta fields `urn`, `code`, `name`, `entity_type` (+ `description`, `_extends`); entity schema (`properties` map with `type` URI, `title`, UI protocol); action schema (`target_type`, `input_schema`, hidden `default_values`, `transitions`); UI protocol (`ui_widget`, `ui_options`, `readOnly`, `title`; standard widgets TextInput, TextArea, JsonEditor, MonacoEditor with options); Logic protocol (injected `payload`, `ctx.identity`, `sys_db_get(uri)`, `sys_db_commit(id, type, data)`; standard returns: saver `{status, message, data}`, generator `{data, ui_schema}`); reserved keys `_extends`, `ui_widget`, `target_type`, `generator_logic`/`submitter_logic`, `transitions`.
- **Why (intent):** "The constitution of storage and communication" for all metadata.
- **Sources:** [US:docs/front/front_05.md].
- **Tags:** meta-model, protocol, ui-rendering

### CON-US-015 — `ui_modes` on every type
- **What:** Each type declares per-mode rendering: `{edit|view|list|execute: {widget, config}}` — e.g. `name`: edit `TextInput`, view `TextLabel{weight: bold}`, list `TextCell{copyable}`; `person`: edit `Form`, view `ProfileCard{imageField}`; Logic: edit `ScriptEditor{language: rhai}`, view `CodeBlock`; View entities: edit `DashboardBuilder`/`ListBuilder`/`FormBuilder`, view renderers.
- **How:** Added to all genesis types 01–17; `meta` type declares `ui_modes` as a map property [US:docs/db/genesis_data/].
- **Why (intent):** Implements UC's lens idea (CON-UC-055) as data on the type itself.
- **Tags:** ui-rendering, type-system, metadata

### CON-US-016 — Root "Entity" generic view type
- **What:** `Type/Entity` (extends root) with properties `target_id` (read-only TextInput), `target_type` (read-only TextInput), `entity_content` (JsonEditor) — the raw view of any entity.
- **Why (intent):** D-US-07: viewing an object as the base type yields the universal JSON editor; viewing it as its concrete type yields a proper form — "UI is the skin grown on the type's properties".
- **Sources:** [US:docs/db/init_data/01_meta_type.json], [US:docs/front/渲染.md].
- **Tags:** ui-rendering, meta-model

### CON-US-017 — Action type hierarchy: Action → StatefulAction → TwoStepAction
- **What:** `Action` (name, icon, description, required_permission default `read`); `StatefulAction` (initial_state, states, transitions; UI WorkflowDesigner/StateDiagram); `TwoStepAction` (generator_logic, submitter_logic relations; default FSM `INIT → NEXT → REVIEW → SUBMIT → DONE`, `BACK` REVIEW→INIT; per-state `ui_policy {default: edit|read_only}`). Concrete: `act_universal_editor` (params target_id, target_type ∈ {Action, Logic, Type, Widget}; generator `logic_universal_load`, submitter `logic_universal_save`), `act_logic_maintain` (load/save Logic script with Monaco).
- **How:** [US:docs/db/genesis_data/16_entity_actions.json], [US:docs/db/genesis_data/17_logic_actions.json]; concrete actions override only `params_schema` and `default_values`.
- **Why (intent):** 三件套 — stop repeating transitions in each action; new interactions = one JSON + two scripts.
- **Tags:** workflow, state-machine, metadata

### CON-US-018 — Scripts storable as line arrays
- **What:** `script_content` may be a string or an array of lines (joined with `\n`); the renderer converts back and forth for script editors [US:crates/ubos_proto/src/logic.rs#L17-L56], [US:apps/ubos_web/src/engine/UniversalRenderer.tsx].
- **Why (intent):** Readable, diff-friendly scripts inside JSON genesis files.
- **Tags:** scripting, metadata
