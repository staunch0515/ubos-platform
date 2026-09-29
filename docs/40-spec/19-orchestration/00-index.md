---
id: SPEC-19
title: "Orchestration"
status: complete
phase: P4
depends_on: [SPEC-13, SPEC-15, SPEC-16, SPEC-17, SPEC-18]
sources: [UP, LC, UB, UC, US, LS, UW]
---

# Orchestration

## 0. Chapter header

- **Scope:** This chapter defines the three orchestration levels (VRD-06-01):
  - (a) **Pipelines**: step lists for one command or query.
  - (b) **Actions**: public operations on entities. They have three stages (describe, prepare, execute), an optional dialogue state machine, and they drive the entity **Lifecycle** (the `_state` state machine of a type).
  - (c) **Process instances**: durable workflows that react to signals and timers.

  It also defines emits and their dispatch, and client **intents** with their mapping to operations. Generic built-in actions (create, view, edit, delete) are part of this chapter.
- **MOD:** `FLOW`
- **depends_on:** SPEC-13, SPEC-15, SPEC-16, SPEC-17, SPEC-18.
- **Terms used:** TERM-Pipeline, TERM-Step, TERM-Action, TERM-ActionType, TERM-Transition, TERM-Stage, TERM-ProcessInstance, TERM-Signal, TERM-Intent, TERM-Emit, TERM-Guard, TERM-NamedOperation.
- **Origin summary:**
  - Verdicts: VRD-06-01…06, VRD-15-03, VRD-15-04, VRD-09-06.
  - LC: named, versioned processes and the command/query split (CON-LC-001, 002, 010, 013), jumps and decisions (CON-LC-021).
  - US: the action type hierarchy, the FSM engine, generator/submitter and the action automator (CON-US-017, 022, 023, 055).
  - LS: actions with three stages, process instances, emit-only scripts, emits as intents, the reactor (CON-LS-017, 018, 020, 022, 025, 028).
  - UB: action → logic indirection, the entity state machine contract, action chains (CON-UB-029, 032, 051).
  - UC: child jobs (CON-UC-026).
  - UP: a multi-entity operation in one script (CON-UP-028).
  - UW: typed intents and command routing (CON-UW-012, 020, 003).

## Parts of this chapter

This chapter is split into files (WRITING-RULES R2.2). Read them in order.

| File | Sections |
|---|---|
| `00-index.md` (this file) | §0, §1, §6, §7, §8 |
| `01-model.md` | Model — §2 |
| `02-behavior.md` | Behavior — §3 |
| `03-interfaces-nfr.md` | Interfaces and non-functional — §4, §5 |

## 1. Concepts

### 1.1 Three levels

| Level | Unit | Lifetime | State kept in | Writes | Example |
|---|---|---|---|---|---|
| (a) Pipeline | one request | one process | `vars` (blackboard) | one flush | `auth.register@v1`: validate → create user → grant role → send welcome emit |
| (b) Action | one call per stage/event | one process per call | entity `_state` (lifecycle) + client-held dialogue state | execute stage flushes | `invoice.approve@v1` on `Invoice/inv-1`; the universal editor |
| (c) Process instance | many signals over time | durable entity | `ProcessInstance` entity versions | one flush per signal | order fulfilment waiting for payment and shipment |

All three are entities, address their implementations by URI, and share the logic contracts of SPEC-17.

### 1.2 Actions and lifecycles

- An **Action** is a public, permissioned operation that is bindable to buttons, intents and APIs.
- A **Lifecycle** is the state machine of an entity type (UB's entity state machine). Its transitions are fired by actions.
- An action may also have a **dialogue**: a small state machine for multi-step interaction (US wizard: INIT → REVIEW → DONE).
- The dialogue state is not entity state.

```
Intent (client) ──► Emit{target, action@vN, event, args}
                        │
             ┌──────────┼───────────────────────────┐
             ▼          ▼                           ▼
        DESCRIBE     PREPARE (read-only)        EXECUTE (write)
        params form  target data + render       logic/pipeline stages changes,
        guards       checklist                  lifecycle transition, emits → flush
```

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-FLOW-001…003 | Pipeline scenarios: register, decision, error routing, foreach |
| REQ-FLOW-010…017 | Action discovery; describe/prepare/execute; dialogue with token; response shape; lifecycle enforcement; generic actions on a new type |
| REQ-FLOW-030…034 | Instance start; signal atomicity; correlation; timers; durable dialogue |
| REQ-FLOW-040, 041 | Emit dispatch per kind; loop protection |
| REQ-FLOW-050 | Intent mapping |

## 7. Implementation notes

- **Reference code:**
  - US action engine [US:crates/ubos_kernel/src/action_engine.rs#L32-L179], genesis actions [US:docs/db/genesis_data/16_entity_actions.json], action automator [US:apps/ubos_web/src/components/Action/ActionAutomator.tsx].
  - LS process step [LS:crates/ubos_kernel/src/engine/orchestrator/process.rs#L12-L86], action genesis [LS:genesis_data/07_action.json].
  - LC engine [LC:src/main/java/com/logicorum/core/LogicExecutionEngine.java], decision handler [LC:src/main/java/com/logicorum/handler/DecisionHandlerV1.java].
  - UB action handler [UB:backend/src/main/java/com/ubos/ese/handler/UbosActionHandler.java].
- **Unifications:**
  - The corpus's two script conventions for process steps (LS `current_step/variables/emits` vs. genesis `CURSOR/SIGNAL/MEMORY/PAYLOAD`) become one params shape: `{cursor, signal, args, memory}` → `{next, memory}` + emits.
  - LC's SpEL conditions become the Rhai rule subset (ADR-002).
- **Dialogue token:** HMAC-SHA256 with a rotating kernel key stored as a root Secret (SPEC-22). Token lifetime: 24 h.

## 8. Open questions

None. Deferred to P2: saga compensation steps (`compensate` logic per step), and visual workflow designer metadata (layout coordinates, SPEC-26).
