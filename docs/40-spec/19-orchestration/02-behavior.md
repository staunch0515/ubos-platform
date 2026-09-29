---
id: SPEC-19-2
title: "Orchestration — Behavior"
status: complete
phase: P4
depends_on: [SPEC-19, SPEC-13, SPEC-15, SPEC-16, SPEC-17, SPEC-18]
sources: [UP, LC, UB, UC, US, LS, UW]
---

# Orchestration — Behavior

Part 2 of SPEC-19 (`19-orchestration/`). Header, concepts and file list: `00-index.md`.

---

## 3. Behavior

### 3.1 Pipelines

### REQ-FLOW-001 — Pipeline execution
- **Statement:** Emitting to a Pipeline URI MUST start a process (kind EMIT) and then:
  1) apply the `input_schema` defaults and validate the arguments;
  2) authorise `operation.execute` plus the `permissions` (SPEC-22);
  3) put the arguments into `vars.args`;
  4) run the steps from the first. Each step:
     - evaluates `when` (SKIPPED if false);
     - computes its params from `input` expressions (rule subset) over `vars`;
     - calls `impl` (SYNC mode, REQ-RT-001, profile FULL for COMMAND, READ_ONLY for QUERY);
     - stores the result in `vars[output]`;
     - then continues at `next` / `then` / `else`, or at the following step;
  5) end at `$end` or after the last step;
  6) for COMMAND: pre-flush and flush (REQ-TX-010/011). For QUERY: no flush;
  7) return `vars[output]`.

  Each step is a STEP trace node. Step-local vars (REQ-CTX-008) are cleared after the step. More than `max_step_executions` executions fail with `FLOW.LOOP_LIMIT`.
- **Rationale:** LC's engine, made script- and native-agnostic and placed inside the process envelope.
- **Origin:** VRD-06-02, CON-LC-010, CON-LC-020, CON-LC-021, CON-LC-002
- **Acceptance:**
  1) `auth.register@v1` with the steps validate-user → create-user → grant-role → emit-welcome commits 2 entities and 1 outbox emit in one process.
  2) A DECISION step routes by `vars.args.kind`.
- **Priority:** P0

### REQ-FLOW-002 — Step errors
- **Statement:** A step error MUST be handled by its `on_error`:
  - FAIL (default) ends the process with the error;
  - CONTINUE records the error in `vars.$errors` and goes on;
  - GOTO jumps to `error_step`, with `vars.$error` set.

  Errors from limits, timeouts and security (`RT.LIMIT`, `TX.TIMEOUT`, `SEC.*`) always FAIL.
- **Origin:** CON-LC-021 (INTERRUPTED), NEW: explicit error routing
- **Acceptance:** A CONTINUE step failure shows in the result's `vars.$errors` and the pipeline completes.
- **Priority:** P1

### REQ-FLOW-003 — FOREACH and sub-pipelines
- **Statement:** A FOREACH step MUST run `impl` (a Pipeline or Logic) once per item of `vars[items]`, in order, within the same process and session. The item is `params.item` and the index `params.index`. Results are collected in a list at `output`. Calling a Pipeline from a CALL step runs it as a sub-pipeline in the same process. It has its own `vars` scope, and its args are the step params.
- **Origin:** NEW: required for batch business operations (CON-UP-028 multi-entity operations)
- **Acceptance:** A FOREACH over 3 order lines stages 3 stock reservations, flushed together.
- **Priority:** P1

### 3.2 Actions

### REQ-FLOW-010 — Action addressing and discovery
- **Statement:** Actions are named operations (`ubos://{tenant}/Action/{slug}@v{N}`). For a target entity or type, the kernel MUST list the **available actions**: Actions whose `target_type` is in the target's type chain, that are visible in the read tenants, pass `available_when`, and that the principal may execute. The list is ordered by `ui.order`. Actions the principal may not execute are omitted, never disabled (UB: "unauthorized actions omitted").
- **Origin:** VRD-13-05, CON-UB-051, CON-US-017
- **Acceptance:** A Customer shows `edit`, `delete` and `acme.customer.merge`. A reader without `action.acme.customer.merge` does not see the last one.
- **Priority:** P0

### REQ-FLOW-011 — Stage DESCRIBE
- **Statement:** `Emit{action, target?, stage: DESCRIBE}` MUST return the action without running user logic, in a READ_ONLY process with no audit row:
  - its params form: `params_schema` merged with the schemas of the parameter properties' `schema` slots, rendered by SPEC-25;
  - the dialogue's initial state and available events;
  - the guard checklist for the target (REQ-RULE-040);
  - the `confirm` text.
- **Origin:** CON-LS-022 (Init stage), CON-US-022
- **Acceptance:** DESCRIBE of `invoice.approve@v1` on an invoice missing a required approver returns a checklist with an unmet guard.
- **Priority:** P0

### REQ-FLOW-012 — Stage PREPARE
- **Statement:** `Emit{…, stage: PREPARE, args}` MUST run a READ_ONLY process that:
  1) resolves the target (effective data);
  2) checks the `access` slots and permission (a denial ends with `SEC.FORBIDDEN`);
  3) applies READ-mode `compute` slots;
  4) masks the data for the principal;
  5) runs the `prepare` generator, if any, with `{target, params, dialogue_state}`. The generator may replace the data or add a UI (`{data, ui}`, US generator);
  6) builds the render payload for the target in the dialogue state's `ui_policy` mode (SPEC-25);
  7) returns a TransitionResponse (REQ-FLOW-015), with `base_commit` = the target's version, which is used as the expected head at EXECUTE.
- **Origin:** CON-LS-022 (Interact stage: permission → compute → mask → schema), CON-US-023 (generator)
- **Acceptance:** PREPARE of the universal editor returns masked data, an edit form, and `base_commit`.
- **Priority:** P0

### REQ-FLOW-013 — Stage EXECUTE
- **Statement:** `Emit{…, stage: EXECUTE, args, base_commit?}` MUST start a process of kind EMIT, with operation = the action URI, and then:
  1) validate the args against `params_schema` (with defaults);
  2) authorise;
  3) evaluate the guards. A DENY ends with `RULE.GUARD_FAILED` and the checklist;
  4) if `lifecycle_event` is set: check that the event is allowed from the target's `_state` (`FLOW.TRANSITION_NOT_ALLOWED` otherwise) and evaluate the lifecycle transition's guards;
  5) load the target with `expected = base_commit` when given (REQ-TX-005);
  6) run `execute`, or the logic resolved from `execute_ref`, with `{target, params, dialogue_state}`. It stages changes and emits. Without an execute logic, the args are applied as a `set_effective` of the target (a generic edit);
  7) if `lifecycle_event` is set: set `_state := to` on the target, through `fsm.transition@v1`;
  8) run pre-flush and flush;
  9) return a TransitionResponse with the process result.

  If `execution` resolves to ASYNC, steps 1–4 run synchronously. The rest runs as a Job, and the response is `Ack{job}` (VRD-15-04).
- **Rationale:** LS's Exec stage (default → transform → validate → before hooks → mutations) is realised by the pre-flush pipeline, so every action gets it without writing it again.
- **Origin:** VRD-06-03, CON-LS-022, CON-US-022, CON-UB-029, CON-UB-032
- **Acceptance:**
  1) `invoice.approve@v1` moves `_state` from SUBMITTED to APPROVED, runs its logic, and commits atomically.
  2) Executing it on a DRAFT invoice fails with `FLOW.TRANSITION_NOT_ALLOWED`.
- **Priority:** P0

### REQ-FLOW-014 — Dialogue transitions
- **Statement:** For actions with a `dialogue`, `Emit{…, event, dialogue_state, dialogue_token, args}` MUST:
  1) verify `dialogue_token`: an HMAC, under a kernel key, over (action commit, target, base_commit, dialogue_state). A missing or invalid token for a non-initial state fails with `FLOW.DIALOGUE_INVALID`;
  2) find the transition for `event` whose `from` contains `dialogue_state` (`FLOW.TRANSITION_NOT_ALLOWED` otherwise);
  3) run its logic as PREPARE (read-only, no process row) or as EXECUTE (per REQ-FLOW-013, including the flush), according to `stage`;
  4) return a TransitionResponse for the `to` state, with a new token.

  Dialogue state is held by the client (US). The token prevents skipping states, and EXECUTE still re-checks every guard.
- **Origin:** VRD-06-03, CON-US-022, CON-US-055, NEW: signed dialogue state
- **Acceptance:** A TwoStepAction goes INIT (next) → REVIEW (submit) → DONE. Sending `submit` with a forged INIT state token fails.
- **Priority:** P1

### REQ-FLOW-015 — Transition response
- **Statement:** Every PREPARE, EXECUTE and dialogue call MUST return:
```yaml
TransitionResponse:
  action: uri                 # canonical with commit
  target: uri?                # canonical with commit after execute
  state: string?              # dialogue state (or lifecycle state for transition actions)
  ui_policy: EDIT | READ_ONLY
  ui: RenderPayload?          # SPEC-25
  data: json?                 # form data / result data
  base_commit: string?        # expected head for the next EXECUTE
  available_events: [ {event, title, style} ]
  checklist: [ ChecklistItem ]?
  dialogue_token: string?
  result: ProcessResult?      # EXECUTE only (REQ-TX-031)
  emits_to_client: [ PushedUi ]?   # UI emits addressed to client://current (SPEC-25)
```
- **Origin:** VRD-06-03, CON-US-022 (response shape), CON-LS-022
- **Acceptance:** The web action automator renders any action using only this response (SPEC-26).
- **Priority:** P0

### REQ-FLOW-016 — Lifecycle enforcement
- **Statement:** For types with a `lifecycle`, the following rules apply:
  - New entities MUST receive `_state = initial` at CREATE.
  - `_state` changes only through lifecycle transitions (REQ-FLOW-013 step 7). Any other attempt fails with `FLOW.STATE_READ_ONLY`.
  - Entities in a state with `editable: false` accept no UPDATE or PATCH except through transition actions and ADMIN processes (`FLOW.NOT_EDITABLE`).
  - Available lifecycle events for the target are part of the available actions (REQ-FLOW-010).
- **Origin:** CON-UB-032, VRD-06-03
- **Acceptance:** Mutating a POSTED invoice (editable false) fails. The `invoice.reverse` transition action still works.
- **Priority:** P0

### REQ-FLOW-017 — Built-in generic actions
- **Statement:** Genesis MUST provide these actions in `logrums`, with `target_type = Entity`, so that every entity type has them:

| Action | Kind | Behaviour |
|---|---|---|
| `entity.view@v1` | QueryAction | PREPARE renders the view mode |
| `entity.create@v1` | TwoStepAction (target_mode TYPE) | INIT renders the create form from the derived schema; submit creates |
| `entity.edit@v1` | TwoStepAction | the universal editor: prepare loads the effective data; submit applies `set_effective` with `base_commit` (US `act_universal_editor`, LS UniversalEditAction) |
| `entity.edit_raw@v1` | TwoStepAction | JSON editor over own data; permission `ubos.entity.edit_raw` (UC raw editor) |
| `entity.delete@v1` | Action | confirm, then DELETE |
| `entity.history@v1` | QueryAction | history and diff (SPEC-14) |
| `entity.project@v1` | TwoStepAction | view or edit an entity as one of its ancestor types (projection, SPEC-25) |

  Type-specific actions override these by `target_type` specificity: the most specific action with the same `ui.placement` slot and base slug suffix wins.
- **Origin:** CON-US-017, CON-US-023, CON-LS-018, CON-UC-031, VRD-13-04
- **Acceptance:** A newly created type immediately offers view/create/edit/delete/history in the cockpit with no extra definitions.
- **Priority:** P0

### 3.3 Process instances

### REQ-FLOW-030 — Starting an instance
- **Statement:** `Emit{target: Workflow URI, signal: "start", args}` MUST start a process that:
  - validates the args;
  - creates a `ProcessInstance` with `workflow` pinned to the Workflow's commit, `cursor = initial`, `memory = {args}`, the correlation keys computed from `correlation`, and the anchors;
  - runs the initial state's `entry` logic;
  - flushes, and returns the instance URI.
- **Origin:** VRD-06-04, CON-LS-017
- **Acceptance:** Starting `order.fulfilment@v1` creates an instance in state `await_payment` with `waiting_for [payment.received]`.
- **Priority:** P1

### REQ-FLOW-031 — Signals
- **Statement:** `Emit{target: instance URI, signal, args}` MUST run **one** transition in a process:
  1) load the instance, with the expected head;
  2) find `states[cursor].on[signal]` (`FLOW.SIGNAL_NOT_EXPECTED` otherwise) and evaluate its guard;
  3) run its logic (the pinned version) with `{cursor, signal, args, memory}`. The logic may stage business changes and emits, and returns `{next?, memory?}`;
  4) update the cursor, memory, `waiting_for` and status;
  5) run the new state's `entry`;
  6) flush the instance version **together** with the business changes.

  A failed transition leaves the instance unchanged. A second process records `last_error` and status FAILED only when the failure is not retryable.
- **Rationale:** LS's "Emit on a process entity runs one transition", made atomic with its effects.
- **Origin:** VRD-06-04, CON-LS-020
- **Acceptance:** A `payment.received` signal moves the instance and creates a Shipment in one commit set. A duplicate signal (same dedupe key) is ignored.
- **Priority:** P1

### REQ-FLOW-032 — Correlation
- **Statement:** A signal MAY target instances by correlation instead of URI: `Emit{target: Workflow URI, signal, correlation: "order:ubos://acme/Order/o-1"}`. The kernel finds WAITING instances of that workflow whose `correlation_keys` contain the key (the property is searchable) and that wait for the signal, and delivers the signal to each of them in its own process. Hooks (SPEC-20) use this to route domain events to workflows.
- **Origin:** NEW: required to connect events to durable processes; CON-LS-028 (reactor)
- **Acceptance:** A `Payment` commit event routed by a hook with correlation `order:<uri>` reaches the waiting instance.
- **Priority:** P2

### REQ-FLOW-033 — Timers and async steps
- **Statement:** A state `timeout {after, signal}` MUST create a delayed Job (SPEC-20) that delivers `signal` when the instance is still in that state at the deadline. Leaving the state cancels the job. Logic that submits Jobs (`job.submit`) receives a job URI. When the job completes, the kernel delivers the signal `job.completed` (or `job.failed`) with `{job, result}` to the instance recorded in the job's `reply_to`.
- **Origin:** VRD-06-04, CON-UC-026, VRD-11-03
- **Acceptance:** An instance with `timeout {after: PT48H, signal: payment.timeout}` receives the signal after 48 h (tested with an injectable clock).
- **Priority:** P2

### REQ-FLOW-034 — Durable dialogues
- **Statement:** A client MAY ask for a durable dialogue (`durable: true`). The kernel then stores the dialogue as a ProcessInstance of the built-in workflow `flow.dialogue@v1`, keyed to the principal, and the instance URI replaces the dialogue token, so the dialogue can be resumed on another device.
- **Origin:** VRD-06-04 ("action dialogues may opt into durability")
- **Acceptance:** A wizard started on the web resumes on mobile at the same state.
- **Priority:** P2

### 3.4 Emits

### REQ-FLOW-040 — Emit dispatch
- **Statement:** Emits collected by a process (REQ-RT-014) MUST be written to the outbox in its flush and dispatched afterwards (SPEC-20). Default deliveries:

| Kind | Default delivery | Dispatched as |
|---|---|---|
| EVENT | AFTER_COMMIT | domain event to hooks and subscriptions |
| ACTION | JOB | a new process executing the action EXECUTE stage on the target (principal = the emitting process's principal, unless `run_as` is configured) |
| SIGNAL | AFTER_COMMIT | a new process delivering the signal (REQ-FLOW-031) |
| JOB | JOB | the Job entity is already created in the flush |
| NOTIFY | AFTER_COMMIT | a notification to a principal (SPEC-20) |
| UI | AFTER_COMMIT | a pushed UI instruction; `client://current` goes back to the requesting protocol session in the response stream (SPEC-24/25) |
| MAIL / WEBHOOK | JOB | delivery with retries |

  Emits with `delay` always use a Job. Emits of a failed or dry-run process are never dispatched. The dry run returns them.
- **Origin:** VRD-06-05, VRD-11-01, CON-LS-025, CON-US-008
- **Acceptance:** An action that emits `ACTION invoice.notify@v1` and `UI toast` produces a toast in the caller's response stream and one job for the notify action.
- **Priority:** P0 (EVENT, UI, JOB), P1 (others)

### REQ-FLOW-041 — Emit chains and loop protection
- **Statement:** Every dispatched emit MUST carry a `causation` chain: the process IDs from the original request. A process started by an emit whose chain length is 16 or more MUST fail with `FLOW.EMIT_DEPTH`. An emit with a `dedupe_key` that was already dispatched for the same target is dropped.
- **Origin:** NEW: protects recursive dispatch (CON-LS-025 recursive design)
- **Acceptance:** Two hooks that trigger each other stop after 16 hops, with a visible error in the audit.
- **Priority:** P0

### 3.5 Intents

### REQ-FLOW-050 — Intent resolution
- **Statement:** Clients express user requests as Intents (§2.6). They are resolved to UBTP operations as follows:

| Intent | Operation |
|---|---|
| OPEN / VIEW | `Query` of the target with a view (SPEC-25), or `entity.view@v1` PREPARE |
| CREATE | `entity.create@v1` (or the type's create action) DESCRIBE/PREPARE |
| EDIT | `entity.edit@v1` PREPARE |
| RUN_ACTION | action DESCRIBE → PREPARE → EXECUTE |
| TRANSITION | the TransitionAction whose `lifecycle_event` = event, on the target |
| SEARCH | `Query` search (SPEC-21) |
| ASK | copilot (SPEC-27), which returns proposed intents for the user to confirm |

  `ubos://system/Query/flow.intents@v1 {target}` returns the intents available for a target (from REQ-FLOW-010). Intents are never executed without passing through the listed operations, so every security and validation check applies.
- **Origin:** VRD-06-06, CON-UW-012, CON-UW-003, VRD-16-04
- **Acceptance:** The cockpit's `close 42` command (SPEC-26) produces `TRANSITION{target: Issue/42, event: close}` and runs `issue.close@v1`.
- **Priority:** P1
