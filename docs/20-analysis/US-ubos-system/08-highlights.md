---
id: ANA-US-08
title: "US Highlights"
status: complete
phase: P1
depends_on: [ANA-US-01, ANA-US-02, ANA-US-03, ANA-US-04, ANA-US-05, ANA-US-06, ANA-US-07]
sources: [US]
---

# US — Highlights

(US also carries all UC highlights HL-UC-001…014 through shared code and documents; listed here are US-specific ones.)

### HL-US-001 — Metadata-defined action state machines with inherited transitions
- **Summary:** Action → StatefulAction → TwoStepAction; `states` with UI policies, `transitions` with `from/to` and `logic` or `logic_ref`; one endpoint computes the next state, runs bound logic, returns UI schema, data and available events.
- **Why it stands out:** Workflows and wizards become data; the client is a generic renderer of server state; new interactions need one JSON and two scripts.
- **Concepts:** CON-US-017, CON-US-022, CON-US-023, CON-US-055
- **Reuse recommendation:** adopt (merge with UB entity state machines CON-UB-032 and LC pipelines in P2 topic 5/9)
- **Sources:** [US:crates/ubos_kernel/src/action_engine.rs], [US:docs/db/genesis_data/16_entity_actions.json]
- **Tags:** workflow, state-machine, ui-rendering

### HL-US-002 — Unit-of-work session over inheritance-assembled composites
- **Summary:** Identity map per execution; reads return the merged inherited view; writes are staged and flushed on success; cycle-safe ancestor assembly.
- **Why it stands out:** Gives scripts ORM-like semantics (read-your-writes, batching, one identity) on a Git-for-data store.
- **Concepts:** CON-US-012, CON-US-020, CON-US-021, CON-US-024
- **Reuse recommendation:** adopt (add optimistic lock on `loaded_commit_id` and single-transaction flush)
- **Sources:** [US:crates/ubos_kernel/src/session/], [US:crates/ubos_kernel/src/graph/]
- **Tags:** persistence, inheritance, process

### HL-US-003 — Composition model and entity-oriented programming axioms
- **Summary:** Behaviours → relation types → entity types; entity as state stream, actions as pure transitions returning changesets/effects, relations first-class; invariants and derivations in the type.
- **Why it stands out:** The most formal statement of the platform's semantics in the corpus; a basis for the spec's meta-model and rules chapters.
- **Concepts:** CON-US-010, CON-US-011, CON-US-013
- **Reuse recommendation:** adapt (reconcile with UB type chain + BEL invariants and LC relationship-as-entity in P2 topics 2/9)
- **Sources:** [US:docs/front/UBOS.md], [US:docs/front/EOP.md]
- **Tags:** meta-model, dsl, rules

### HL-US-004 — Mode-aware universal rendering with widgets as resources
- **Summary:** Types declare `ui_modes`; the renderer picks widget per mode with fallbacks; widgets registered by `ubos://…/Widget/<name>`.
- **Concepts:** CON-US-015, CON-US-051, CON-US-052, CON-US-053, CON-US-058
- **Reuse recommendation:** adopt
- **Sources:** [US:apps/ubos_web/src/engine/UniversalRenderer.tsx]
- **Tags:** ui-rendering, type-system

### HL-US-005 — Slice projection and merge: view/edit any entity as any ancestor type
- **Summary:** Store the full concrete entity; project a slice for a chosen ancestor type (data + schema); merge edits back; generic `Entity` type gives the raw JSON view.
- **Why it stands out:** One data object, many safe editing lenses without duplicate forms or data.
- **Concepts:** CON-US-016, CON-US-026
- **Reuse recommendation:** adopt (design; partial code)
- **Sources:** [US:docs/front/渲染.md]
- **Tags:** ui-rendering, inheritance

### HL-US-006 — Lifecycle-phase event bus with voting slices
- **Summary:** Intent → validate (veto) → authorize → persist → notify; plugins discovered from type/relations; decisions with priority and veto; shared blackboard; causal trace.
- **Concepts:** CON-US-007, CON-US-027, CON-US-028, CON-US-029, CON-US-030
- **Reuse recommendation:** adapt (as the commit pipeline in the spec; compare LC declarative pipelines and FU governance)
- **Sources:** [US:docs/front/FullSlice.md]
- **Tags:** events, rules, plugin

### HL-US-007 — Five-instruction protocol target
- **Summary:** Handshake / Mutate / Emit / Query / Subscribe over a length-prefixed protocol; side effects returned as emits.
- **Concepts:** CON-US-006, CON-US-008
- **Reuse recommendation:** adapt (compare LS protocol in P2 topic 15)
- **Sources:** [US:docs/front/UBOS.md]
- **Tags:** protocol, api

### HL-US-008 — Metadata specification v1.0
- **Summary:** URN categories (Type, Action, Logic, Widget), mandatory meta fields, standard UI and logic return protocols, reserved keywords.
- **Concepts:** CON-US-014
- **Reuse recommendation:** adopt (merge with UC UODS, CON-UC-017)
- **Sources:** [US:docs/front/front_05.md]
- **Tags:** meta-model, protocol

### HL-US-009 — Modular kernel with multiple execution modes and clients
- **Summary:** Workspace crates (proto/store/kernel) reused by server and CLI; async worker, ephemeral eval, sync logic.
- **Concepts:** CON-US-001, CON-US-002, CON-US-003, CON-US-064
- **Reuse recommendation:** adopt
- **Sources:** [US:Cargo.toml]
- **Tags:** deployment, process

### HL-US-010 — Executable scenarios and authoring tooling
- **Summary:** CLI `.rhai` scenarios as specifications; compile-only syntax endpoint feeding editor markers; Rhai language support; VS Code virtual file system design.
- **Concepts:** CON-US-004, CON-US-005, CON-US-056, CON-US-057
- **Reuse recommendation:** adopt
- **Sources:** [US:apps/ubos_cli/], [US:docs/front/VsCode.md]
- **Tags:** scripting, frontend

### HL-US-011 — Neuro-symbolic positioning
- **Summary:** LLM as compiler, UBOS as hallucination firewall, LLM-generated logic as versioned data, traces as explanation context.
- **Concepts:** CON-US-060, CON-US-061, CON-US-062
- **Reuse recommendation:** adapt (AI chapter)
- **Sources:** [US:docs/front/FullSlice.md]
- **Tags:** ai, rules

### HL-US-012 — Feature packaging convention ("three-piece set")
- **Summary:** Every feature = Action JSON + generator + submitter (+ widget); scripts as line arrays in JSON.
- **Concepts:** CON-US-018, CON-US-023, CON-US-063
- **Reuse recommendation:** adopt
- **Sources:** [US:docs/front/三件套.md]
- **Tags:** codegen, workflow
