---
id: SPEC-01-2
title: "Glossary part 2 — Logic, rules, orchestration, events, query"
status: complete
phase: P3
depends_on: [SPEC-01]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Glossary part 2 — Logic, rules, orchestration, events, query

Part of SPEC-01. Entry format and rules: `00-index.md` §0.

---

## 6. Logic runtime

### TERM-Logic
- **Definition:** An entity holding executable code (`language`, `source`, `entry`, input/output schema, required capabilities). Language `rhai` in version 1.
- **Aliases:** SysFunction (UC, early); script (UP, UB); `sys_function` (UC genesis); mould (UC metaphor).
- **Not:** a Job (a requested execution) or a Process (an execution record).
- **Chapter:** SPEC-17
- **Origin:** CON-UP-020, CON-UC-013, CON-US-018, VRD-05-01

### TERM-Syscall
- **Definition:** A kernel function registered into the script engine under a namespace (`entity.*`, `query.*`, `ctx.*`, `ai.*`, …). Syscalls are the only way logic reaches data or the outside world.
- **Aliases:** kernel API (UP); capability injection (UP); script context as SDK (UB).
- **Chapter:** SPEC-17
- **Origin:** CON-UC-004, CON-UP-021, CON-UP-029, CON-UB-030, VRD-05-02

### TERM-SyscallAbi
- **Definition:** The versioned set of syscall signatures (`v1`, `v2`, …). Logic declares the ABI version it targets.
- **Chapter:** SPEC-17
- **Origin:** CON-UP-029, CON-UC-004, VRD-05-02

### TERM-Sandbox
- **Definition:** The layered containment of logic: no host access except syscalls, operation/depth/size/time limits, policy checks per syscall, output size caps.
- **Aliases:** immune system (UP); runtime guards (UC).
- **Chapter:** SPEC-17
- **Origin:** CON-UP-022, CON-UC-030, VRD-05-03, VRD-10-06

### TERM-NativeHandler
- **Definition:** A kernel function addressed by URI (`ubos://system/Native/<name>@vN`) that implements a mechanic (persist, validate, authorize, index) and can be used wherever logic can.
- **Aliases:** handler bean (LC); `native::` host function (LS).
- **Chapter:** SPEC-17
- **Origin:** CON-LC-005, CON-LC-025, CON-LS-061, VRD-05-05

### TERM-Emit
- **Definition:** A side-effect request returned by logic instead of performed directly (write, notify, schedule, push UI, call). The kernel validates, authorises and dispatches emits.
- **Aliases:** emits as intents (LS); effects as returned intents (US).
- **Not:** the UBTP `Emit` operation (TERM-UbtpOperation), which invokes a named operation.
- **Chapter:** SPEC-17, SPEC-19
- **Origin:** CON-LS-021, CON-LS-025, CON-US-008, VRD-06-05

### TERM-ExecutionMode
- **Definition:** How logic runs: `sync` (inside the caller's process), `job` (asynchronously as a Job), `dry_run`.
- **Aliases:** three execution modes (US); ephemeral eval (UC).
- **Chapter:** SPEC-17
- **Origin:** CON-US-003, CON-UC-025

---

---

## 7. Rules and validation

### TERM-DerivedSchema
- **Definition:** The JSON Schema of instances of a type, compiled from its properties and their semantic types at each type commit.
- **Aliases:** schema-on-commit (FU); schema from SchemaProvide slots (LS).
- **Chapter:** SPEC-18
- **Origin:** CON-FU-020, CON-LS-052, VRD-09-01

### TERM-ValidationLevel
- **Definition:** The strictness at which rules run: `DRAFT` (structural only, partial data allowed) and `PUBLISH` (all rules).
- **Aliases:** DRAFT_SAVE / FINAL_PUBLISH (LC).
- **Chapter:** SPEC-18
- **Origin:** CON-LC-023, VRD-09-02

### TERM-Constraint
- **Definition:** A pluggable business rule entity that checks data across entities (uniqueness, cardinality, referential rules) over staged and persisted state.
- **Chapter:** SPEC-18
- **Origin:** CON-LC-024, VRD-09-03

### TERM-Invariant
- **Definition:** A rule over the transition from `PREV` to `NEW` state, written in the restricted Rhai subset (ADR-002), with severity `error` (reject the commit) or `warn` (tag the commit metadata).
- **Aliases:** BEL INVARIANT/WARN (UB); EOP (US).
- **Chapter:** SPEC-18
- **Origin:** CON-UB-061, CON-US-013, VRD-09-04

### TERM-Decision
- **Definition:** The outcome of a rule: `allow` (bool), reason, priority, veto flag. Decisions from many rules combine by veto first, then priority.
- **Aliases:** slice voting (US).
- **Chapter:** SPEC-18
- **Origin:** CON-US-027, VRD-09-05

### TERM-Guard
- **Definition:** A rule evaluated before a transition fires. Guards reuse constraints, invariants and decisions.
- **Aliases:** guardrail checklist (UW).
- **Chapter:** SPEC-18, SPEC-19
- **Origin:** CON-UW-022, VRD-09-06

---

---

## 8. Orchestration

### TERM-Pipeline
- **Definition:** A versioned definition of ordered steps (logic or native handlers) with named jumps and decisions, invoked as a named operation.
- **Aliases:** process definition (LC); process blueprint (LS); workflow (UC genesis).
- **Not:** TERM-Process (an execution record).
- **Chapter:** SPEC-19
- **Origin:** CON-LC-001, CON-LC-010, CON-LC-021, CON-LS-017, VRD-06-02

### TERM-Step
- **Definition:** One element of a pipeline: a reference to logic or a native handler plus input mapping, output mapping and flow control.
- **Chapter:** SPEC-19
- **Origin:** CON-LC-005, CON-LS-020, VRD-06-02

### TERM-Action
- **Definition:** A public, named operation on an entity type (create, edit, approve, submit…) with a state-machine transition and stage steps. Actions bind to logic indirectly.
- **Aliases:** `sys_action` (UC genesis); action chain (UB); Action type (US).
- **Chapter:** SPEC-19
- **Origin:** CON-UB-029, CON-UB-051, CON-US-017, CON-LS-018, VRD-06-03

### TERM-ActionType
- **Definition:** The type hierarchy of actions (base action, entity action, transition action, query action). Transitions are inherited along it.
- **Chapter:** SPEC-19
- **Origin:** CON-US-017, VRD-06-03

### TERM-Transition
- **Definition:** A change of an entity's lifecycle state caused by an action, with guard, stage steps and a transition response.
- **Aliases:** Action FSM (US); entity state machine (UB).
- **Chapter:** SPEC-19
- **Origin:** CON-US-022, CON-UB-032, VRD-06-03

### TERM-Stage
- **Definition:** One of the three phases of an action: `prepare` (build UI/data), `validate`, `execute`.
- **Aliases:** three-stage action protocol (LS).
- **Chapter:** SPEC-19
- **Origin:** CON-LS-018, CON-LS-022, VRD-06-03

### TERM-ProcessInstance
- **Definition:** A durable, long-running orchestration stored as an entity: current state, waiting signals, history.
- **Aliases:** process instance (LS); orchestration by child jobs (UC).
- **Chapter:** SPEC-19
- **Origin:** CON-LS-017, CON-UC-026, VRD-06-04

### TERM-Signal
- **Definition:** A message delivered to a waiting process instance (approval, timer, external event).
- **Chapter:** SPEC-19
- **Origin:** VRD-06-04

### TERM-Intent
- **Definition:** A client-side, typed request expressed in business words (open view, run action, start task). Intents map to UBTP operations.
- **Aliases:** typed intent (UW); command grammar (UW).
- **Chapter:** SPEC-19, SPEC-26
- **Origin:** CON-UW-012, CON-UW-020, VRD-06-06

---

---

## 9. Events and asynchronous work

### TERM-Outbox
- **Definition:** Rows of domain events written in the same flush as the commits and later delivered to subscribers.
- **Aliases:** email outbox (LC).
- **Chapter:** SPEC-20
- **Origin:** CON-LC-027, VRD-11-01

### TERM-Event
- **Definition:** A fact emitted by the commit path (e.g. `entity.committed`, `process.completed`) with type, subject URI, commit and payload.
- **Chapter:** SPEC-20
- **Origin:** CON-UP-025, VRD-11-01

### TERM-Hook
- **Definition:** An entity that subscribes logic or a pipeline to events matching a filter.
- **Aliases:** SysHook (UC, early); event bus declared as data (UP).
- **Chapter:** SPEC-20
- **Origin:** CON-UP-025, CON-UC-027, VRD-11-02

### TERM-Cron
- **Definition:** An entity that schedules logic or a pipeline by a cron expression.
- **Aliases:** SysCron (UC, early); scheduler declared as data (UP).
- **Chapter:** SPEC-20
- **Origin:** CON-UP-026, CON-UC-027, VRD-11-02

### TERM-Job
- **Definition:** An entity that requests asynchronous execution of logic or a pipeline, with state (QUEUED, RUNNING, SUCCEEDED, FAILED, DEAD), attempts and result reference. Workers pick jobs; each attempt runs as a process.
- **Aliases:** Job as execution record / cast (UC); Logrum (UC, early).
- **Chapter:** SPEC-20 (ADR-004)
- **Origin:** CON-UC-013, CON-UC-032, VRD-11-03

### TERM-Worker
- **Definition:** A runtime role that takes jobs from the queue and runs them.
- **Aliases:** consumer (UC); worker two-step commit (US).
- **Chapter:** SPEC-20
- **Origin:** CON-UC-003, CON-US-025

### TERM-Webhook
- **Definition:** A versioned entity describing an outbound HTTP notification, signed with HMAC.
- **Chapter:** SPEC-20, SPEC-22
- **Origin:** CON-FU-024, VRD-10-05

### TERM-DeadLetter
- **Definition:** The terminal state of a job or event delivery that exhausted its retries.
- **Chapter:** SPEC-20
- **Origin:** VRD-11-03

### TERM-Subscription
- **Definition:** A protocol-level registration by a client to receive events or changes matching a filter.
- **Chapter:** SPEC-20, SPEC-24
- **Origin:** CON-LS-002, VRD-11-04

---

---

## 10. Query and search

### TERM-SearchIndex
- **Definition:** The derived, typed attribute index (`search_index`) written at commit for searchable properties: `(commit_id, prop_path, val_text | val_num | val_date | val_bool)`.
- **Aliases:** commit-scoped property index (UP); automatic typed index (UB).
- **Chapter:** SPEC-11, SPEC-21
- **Origin:** CON-UP-042, CON-UB-042, CON-LS-042, VRD-12-01

### TERM-CriteriaQuery
- **Definition:** A declarative query over a type: filters on property paths, relationship joins, sorting, paging, projection, `as_of`.
- **Aliases:** generic criteria reader (LS).
- **Chapter:** SPEC-21
- **Origin:** CON-LS-043, VRD-12-02

### TERM-AsOf
- **Definition:** A time or commit bound on a query that returns data as it was then.
- **Chapter:** SPEC-21
- **Origin:** CON-UP-024, VRD-12-03

### TERM-QueryPipeline
- **Definition:** A read operation declared as a pipeline with only read steps.
- **Aliases:** query process (LC).
- **Chapter:** SPEC-21
- **Origin:** CON-LC-002, VRD-12-05

### TERM-Embedding
- **Definition:** A vector computed per version of an entity, asynchronously, for similarity search and retrieval.
- **Chapter:** SPEC-21, SPEC-27
- **Origin:** CON-UB-065, VRD-12-04
