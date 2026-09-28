---
id: CMP-06
title: "Comparison: Process Orchestration and Actions"
status: complete
phase: P2
depends_on: [CMP-04, CMP-05]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Comparison: Process Orchestration and Actions

## 1. Question

How are business operations composed from steps, and how are multi-step user interactions (wizards, approvals, workflows) and long-running work defined, as data, with clear state and UI?

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | Script orchestrates multiple commits | CON-UP-028 | Imperative orchestration inside one logic |
| LC | Declarative process pipeline: steps = configured handler beans, named jumps, decision step (SpEL), command/query split | CON-LC-001, 010, 020, 021, 002, HL-LC-001, 008, 009 | Mature data-defined pipeline |
| UB | Action → Logic; entity state machine contract `from –event[guard]→ to` (design) | CON-UB-029, 032, HL-UB-013, 016 | Intent / rules / facts layering |
| UC | Jobs (durable capsules), child jobs, cron/hook triggers | CON-UC-013, 026, 027, HL-UC-005 | Asynchronous orchestration |
| US | Action FSM: Action → StatefulAction → TwoStepAction; `transitions {event: {from, to, logic | logic_ref}}`, per-state `ui_policy`, `available_events` | CON-US-017, 022, 023, HL-US-001 | Server-driven wizard state machines |
| LS | Process entities (blueprint vs instance, cursor, memory); three-stage action Init/Interact/Exec emitting slot signals; emits as intents | CON-LS-017, 018, 020, 022, 025, HL-LS-003, 006 | Generic interaction pipeline over slots |
| UW | Typed intents from a command bar | CON-UW-012, 020 | UX layer |

## 3. Dimension matrix

| Dimension | LC | UB | UC | US | LS |
|---|---|---|---|---|---|
| Composition defined as data | ● pipeline | design | job + triggers | ● FSM | ● process + stages |
| Branching / decisions | ● jumps + expressions | guards (design) | script | event table | script |
| Long-running / durable state | – (per request) | – | ● job entity | client-held state | ● process instance entity |
| User interaction model | – | actions | forms | ● states + UI policy + events | ● 3 stages + slot signals |
| Inheritance of workflow definitions | – | – | – | ● via `_extends` | ● ActionType extends |
| Reusable steps library | ● handlers | – | syscalls | generator/submitter logic | slot behaviours |
| Command vs query separation | ● | – | invoke vs eval | – | Emit vs Query |
| AI-friendliness (small declarative units) | ● | ◐ | ◐ | ● | ● |

## 4. Analysis

- Three levels of orchestration appear in the corpus:
  - (a) **step pipelines**, which compose one transactional operation (LC);
  - (b) **interaction state machines**, which run a multi-request dialogue with the user (US FSM, LS three stages, UB state machine design);
  - (c) **durable/asynchronous processes**, which span time and triggers (UC jobs, LS process instances, UP/UC cron and hooks).
- No repository covers all three levels; each covers one of them well.
- **US and LS complement each other.**
  - US defines *who moves the dialogue* (events, states and UI policy, inherited from base action types).
  - LS defines *what each step does generically*: emit standard slot signals such as permission, compute, mask, schema, default, transform, validate and hooks.
- **Durable instances.**
  - US keeps FSM state in the client (`current_state` is sent back with each request).
  - LS persists process instances as versioned entities, which gives resumability and audit.
- **LC's pipeline** is the most robust per-request engine: it validates input schema and roles, then runs steps with jumps and decisions under an idempotency and audit envelope.

## 5. Verdicts

### VRD-06-01 — Three orchestration levels in the spec (NEW structure, parts best-of)
- **Decision:** NEW structure; each part is a best-of.
- **Consequences:** The spec defines:
  - (a) **Pipeline** — a step list for one command or query (from LC);
  - (b) **Action state machine** — an interaction dialogue (fusion US + LS);
  - (c) **Process instance** — a durable, possibly asynchronous workflow (fusion LS + UC).
- All three are entities and share the logic and syscall contracts (CMP-05).

### VRD-06-02 — Pipeline definition (best-of LC)
- **Decision:** best-of LC.
- **Consequences:**
  - A pipeline has: `name` + version; `input_schema` (defaults applied); `required_permissions`; steps `[{name, handler: native or logic URI, config, next?}]`; decision steps with a sandboxed expression; `mode: command | query`.
  - It runs inside the process envelope: idempotency → authorize → steps → atomic flush → audit (VRD-04).

### VRD-06-03 — Action state machine with inherited transitions and generic stage steps (fusion US + LS + UB)
- **Decision:** fusion.
- **Consequences:**
  - An Action type hierarchy mirrors US's: Action → StatefulAction → TwoStepAction (and further templates).
  - `states {code: {title, ui_policy}}` and `transitions {event: {from, to, guard?, logic | logic_ref}}` are resolved through inheritance.
  - Every transition returns `{next_state, ui_policy, ui_schema, data, available_events}`.
  - The standard steps of the universal editor follow LS's sequence:
    - Init: parameter schema.
    - Interact: permission → compute → mask → schema.
    - Exec: default → transform → validate → before-hooks → staged mutations.
  - Guards use UB's `[guard]` notation (CON-UB-032).

### VRD-06-04 — Durable process instances as entities (fusion LS + UC)
- **Decision:** fusion.
- **Consequences:**
  - A process instance entity holds `definition` (URI + pinned commit), `cursor` (state), `memory` (variables), `status` and `history`.
  - Each signal (Emit) runs one transition and commits a new instance version.
  - Asynchronous steps are Jobs pinned to their logic and context commits (UC anchors).
  - Action dialogues may opt into durability by creating an instance (resume on another device).

### VRD-06-05 — Side effects returned as emits, dispatched by the kernel (best-of LS/US design)
- **Decision:** best-of LS, with the US design.
- **Consequences:** Transition logic returns `emits: [ops]`, e.g. signals to other entities, notifications to clients and job invocations. The kernel dispatches them after the commit (outbox semantics, see CMP-11).

### VRD-06-06 — Intents as the client-facing vocabulary (best-of UW, mapped)
- **Decision:** best-of UW, mapped onto platform actions.
- **Consequences:** Client intents (create, transition, view, board, stand-up…) map to Action URIs plus parameters; the command bar produces intents (CMP-14).
