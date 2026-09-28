---
id: ANA-US-09
title: "US Concept Index"
status: complete
phase: P1
depends_on: [ANA-US-01, ANA-US-02, ANA-US-03, ANA-US-04, ANA-US-05, ANA-US-06]
sources: [US]
---

# US — Concept Index

Shared UC concepts (CON-UC-001…067) also apply to US unless restated; see ANA-UC-09.

| ID | Name | File | Tags | Highlight |
|---|---|---|---|---|
| CON-US-001 | Cargo workspace (proto/store/kernel/SDK/apps) | 01 | deployment, plugin, api | HL-US-009 |
| CON-US-002 | Layered kernel: graph → session → syscalls → executors → FSM | 01 | meta-model, persistence, process | HL-US-009 |
| CON-US-003 | Three execution modes | 01 | process, scripting | HL-US-009 |
| CON-US-004 | CLI scenario runner | 01 | scripting, boot, observability | HL-US-010 |
| CON-US-005 | Compile-only syntax check | 01 | scripting, api, frontend | HL-US-010 |
| CON-US-006 | UBTP + five instructions (design) | 01 | protocol, api, deployment | HL-US-007 |
| CON-US-007 | Event bus + plugin scheduler phases (design) | 01 | events, plugin, rules | HL-US-006 |
| CON-US-008 | Emit flow, effects as returned intents (design) | 01 | workflow, events, scripting | HL-US-007 |
| CON-US-010 | Behavior → Relation Type → Entity Type (design) | 02 | meta-model, type-system, plugin | HL-US-003 |
| CON-US-011 | `sys_primitive` / `sys_descriptor` (design) | 02 | identity, ui-rendering, search | HL-US-003 |
| CON-US-012 | CompositeEntity merged view | 02 | meta-model, inheritance, rehydration | HL-US-002 |
| CON-US-013 | EOP calculus (design) | 02 | dsl, state-machine, rules | HL-US-003 |
| CON-US-014 | Metadata specification v1.0 | 02 | meta-model, protocol, ui-rendering | HL-US-008 |
| CON-US-015 | `ui_modes` on types | 02 | ui-rendering, type-system, metadata | HL-US-004 |
| CON-US-016 | Generic `Entity` view type | 02 | ui-rendering, meta-model | HL-US-005 |
| CON-US-017 | Action type hierarchy | 02 | workflow, state-machine, metadata | HL-US-001 |
| CON-US-018 | Scripts as line arrays | 02 | scripting, metadata | HL-US-012 |
| CON-US-020 | Unit-of-work session | 03 | persistence, versioning, process | HL-US-002 |
| CON-US-021 | Session-backed syscalls | 03 | scripting, persistence, inheritance | HL-US-002 |
| CON-US-022 | Action FSM engine | 03 | workflow, state-machine, ui-rendering | HL-US-001 |
| CON-US-023 | Generator / submitter pattern | 03 | workflow, ui-rendering, scripting | HL-US-001 |
| CON-US-024 | Sync execution, flush on success | 03 | process, persistence | HL-US-002 |
| CON-US-025 | Worker two-step commit | 03 | process, versioning | – |
| CON-US-026 | Slice projection / merge | 03 | ui-rendering, meta-model, inheritance | HL-US-005 |
| CON-US-027 | Slice voting (design) | 03 | rules, permission, plugin | HL-US-006 |
| CON-US-028 | Context blackboard (design) | 03 | context, plugin | HL-US-006 |
| CON-US-029 | Causal slice trace (design) | 03 | observability, rules | HL-US-006 |
| CON-US-030 | Logic in relation types (design) | 03 | scripting, versioning, plugin | HL-US-006 |
| CON-US-031 | `sys_invoke` cross-tenant call (stub) | 03 | integration, multi-tenancy | – |
| CON-US-040 | Same schema/store as UC | 04 | persistence, schema | – |
| CON-US-041 | Genesis deltas | 04 | boot, metadata, domain-pack | HL-US-004 |
| CON-US-042 | SQLite dev / Postgres prod (design) | 04 | persistence, deployment | – |
| CON-US-043 | Append-only verification scenarios | 04 | versioning, observability | HL-US-010 |
| CON-US-050 | Client-side kernel layer | 05 | frontend, protocol, context | HL-US-004 |
| CON-US-051 | Widgets as `ubos://` resources | 05 | ui-rendering, protocol, plugin | HL-US-004 |
| CON-US-052 | Universal renderer | 05 | ui-rendering, type-system | HL-US-004 |
| CON-US-053 | Recursive schema renderer | 05 | ui-rendering, form | HL-US-004 |
| CON-US-054 | View engine | 05 | ui-rendering, metadata | – |
| CON-US-055 | Action automator UI | 05 | frontend, workflow, ui-rendering | HL-US-001 |
| CON-US-056 | Rhai authoring support | 05 | frontend, scripting | HL-US-010 |
| CON-US-057 | VS Code `ubos://` FS (design) | 05 | frontend, integration, protocol | HL-US-010 |
| CON-US-058 | Widget lookup order | 05 | ui-rendering, type-system | HL-US-004 |
| CON-US-060 | Neuro-symbolic integration (design) | 06 | ai, rules, codegen | HL-US-011 |
| CON-US-061 | Explainable decisions from traces | 06 | ai, observability | HL-US-011 |
| CON-US-062 | Hot-swappable versioned rules (design) | 06 | versioning, rules, plugin | HL-US-011 |
| CON-US-063 | Three-piece delivery protocol | 06 | ai, codegen, process | HL-US-012 |
| CON-US-064 | One core, many clients | 06 | deployment, protocol | HL-US-009 |
| CON-US-065 | Development tooling | 06 | ai, codegen, observability | HL-US-010 |
