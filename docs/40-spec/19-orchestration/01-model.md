---
id: SPEC-19-1
title: "Orchestration — Model"
status: complete
phase: P4
depends_on: [SPEC-19, SPEC-13, SPEC-15, SPEC-16, SPEC-17, SPEC-18]
sources: [UP, LC, UB, UC, US, LS, UW]
---

# Orchestration — Model

Part 1 of SPEC-19 (`19-orchestration/`). Header, concepts and file list: `00-index.md`.

---

## 2. Model

### 2.1 Pipeline

```yaml
ENT-Pipeline:
  purpose: Versioned step list for one command or query (TERM-Pipeline); a named operation.
  origin: [VRD-06-02, CON-LC-010, CON-LC-001, CON-LC-002, CON-LC-021]
  fields:
    - {name: title, type: string, required: true, description: "Name"}
    - {name: description, type: text?, required: false, description: "Purpose (for people and AI)"}
    - {name: mode, type: "enum{COMMAND|QUERY}", required: true, description: "QUERY runs READ_ONLY without flush (SPEC-21)"}
    - {name: input_schema, type: json_schema?, required: false, description: "Arguments; defaults applied; violations fail FLOW.INPUT_INVALID"}
    - {name: output, type: string?, required: false, description: "vars key whose value is the result (default: last step output)"}
    - {name: permissions, type: "list<string>?", required: false, description: "Permissions required in addition to operation.execute (SPEC-22)"}
    - {name: steps, type: "list<Step>", required: true, description: "Ordered steps"}
    - {name: execution, type: "enum{SYNC|ASYNC|AUTO}?", required: false, description: "SYNC returns Result; ASYNC returns Ack + Job; AUTO = ASYNC when estimated long (flag long_running) (VRD-15-04). Default SYNC"}
    - {name: long_running, type: bool?, required: false, description: "Hint for AUTO"}
    - {name: retry_on_conflict, type: int32?, required: false, description: "0..3 (REQ-TX-013)"}
    - {name: audit_always, type: bool?, required: false, description: "Write a process row even without commits (REQ-TX-012)"}
    - {name: max_step_executions, type: int32?, required: false, description: "Loop guard; default 1000"}
  invariants:
    - Step names are unique; every jump target exists.
    - QUERY pipelines contain only steps whose implementations have effect ≤ READ.
```

```yaml
ENT-Step:
  purpose: One element of a pipeline (TERM-Step).
  origin: [VRD-06-02, CON-LC-005, CON-LC-010, CON-LC-021]
  fields:
    - {name: name, type: string(64), required: true, description: "Step name"}
    - {name: kind, type: "enum{CALL|DECISION|FOREACH|EMIT|FAIL}", required: true, description: "CALL an implementation; DECISION branch; FOREACH map a sub-pipeline over a list; EMIT an emit; FAIL with an error"}
    - {name: impl, type: uri?, required: false, description: "CALL/FOREACH - Logic, Native or Pipeline URI (with @vN)"}
    - {name: config, type: json?, required: false, description: "Static configuration passed as params.config"}
    - {name: input, type: "map<string, string>?", required: false, description: "params key -> expression (rule subset) over vars/args, e.g. {user: 'vars.user_id'}"}
    - {name: output, type: string?, required: false, description: "vars key receiving the result"}
    - {name: when, type: rhai?, required: false, description: "Rule-subset condition; false = SKIPPED"}
    - {name: condition, type: rhai?, required: false, description: "DECISION - rule-subset expression"}
    - {name: then, type: string?, required: false, description: "DECISION - step if true"}
    - {name: else, type: string?, required: false, description: "DECISION - step if false"}
    - {name: next, type: string?, required: false, description: "Jump after this step (default: following step; '$end' ends)"}
    - {name: on_error, type: "enum{FAIL|CONTINUE|GOTO}?", required: false, description: "Default FAIL"}
    - {name: error_step, type: string?, required: false, description: "GOTO target on error; the error is in vars.$error"}
    - {name: items, type: string?, required: false, description: "FOREACH - vars key of the list (≤ 10 000 items)"}
    - {name: emit, type: Emit?, required: false, description: "EMIT - emit template (§2.5)"}
    - {name: error, type: "{code, message}?", required: false, description: "FAIL - error to raise"}
```

### 2.2 Action

```yaml
ENT-Action:
  purpose: Public operation on entities (TERM-Action); base of the action type hierarchy.
  origin: [VRD-06-03, VRD-05-04, CON-US-017, CON-LS-018, CON-UB-029]
  fields:
    - {name: title, type: string, required: true, description: "Label for buttons and menus"}
    - {name: icon, type: string?, required: false, description: "Icon"}
    - {name: description, type: text?, required: false, description: "Help, used by AI copilot"}
    - {name: target_type, type: "uri<Type>?", required: false, description: "Entity type it applies to (and subtypes); absent = type-less action (e.g. report)"}
    - {name: target_mode, type: "enum{INSTANCE|TYPE|NONE}", required: true, description: "INSTANCE = acts on an existing entity; TYPE = creates/acts on the type (e.g. create); NONE"}
    - {name: params_schema, type: json_schema?, required: false, description: "Parameters; merged with slot-provided schemas"}
    - {name: default_values, type: json?, required: false, description: "Hidden defaults, including logic references (US)"}
    - {name: permission, type: string?, required: false, description: "Permission name checked on the target (default action.<slug>)"}
    - {name: effect, type: "enum{NONE|READ|STAGE|EXTERNAL}", required: true, description: "Maximum effect of the execute stage; NONE/READ actions are allowed in read-only contexts"}
    - {name: guards, type: "list<GuardSpec>?", required: false, description: "Preconditions (SPEC-18 REQ-RULE-040)"}
    - {name: available_when, type: rhai?, required: false, description: "Rule-subset visibility condition over the target (hides the action)"}
    - {name: lifecycle_event, type: string?, required: false, description: "Lifecycle event fired on successful execute (§2.3)"}
    - {name: prepare, type: uri?, required: false, description: "Generator - Logic/Pipeline returning {data, ui?} for the prepare stage"}
    - {name: execute, type: uri?, required: false, description: "Submitter - Logic/Pipeline run in the execute stage"}
    - {name: execute_ref, type: string?, required: false, description: "Name of a property (resolved through inheritance) that holds the execute URI (US logic_ref)"}
    - {name: dialogue, type: Dialogue?, required: false, description: "Multi-step interaction state machine (§2.4)"}
    - {name: execution, type: "enum{SYNC|ASYNC|AUTO}?", required: false, description: "As for pipelines (VRD-15-04)"}
    - {name: confirm, type: string?, required: false, description: "Confirmation text shown before execute"}
    - {name: retry_on_conflict, type: int32?, required: false, description: "0..3"}
    - {name: audit_always, type: bool?, required: false, description: "See REQ-TX-012"}
    - {name: ui, type: "{placement?: list<enum{TOOLBAR|ROW|MENU|DOCK|OMNIBAR}>, order?: int32, style?: enum{PRIMARY|DEFAULT|DANGER}}?", required: false, description: "Presentation hints (SPEC-25)"}
  invariants:
    - At most one of execute and execute_ref.
    - lifecycle_event is an event of the target type's lifecycle.
    - Slug ends with .v<N> (REQ-URI-010).
```

Action type hierarchy (root package), following US:

| Type | Extends | Adds |
|---|---|---|
| `Action` | `Entity` | the fields above |
| `StatefulAction` | `Action` | `dialogue` required |
| `TwoStepAction` | `StatefulAction` | `dialogue` defaults to INIT → REVIEW → DONE, with events `next`, `back`, `submit`; `prepare` = generator, `execute` = submitter (US "three-piece set") |
| `TransitionAction` | `Action` | `lifecycle_event` required; `target_mode = INSTANCE` |
| `QueryAction` | `Action` | `effect = READ`; the execute stage returns data and never flushes |

### 2.3 Lifecycle

```yaml
ENT-Lifecycle:
  purpose: State machine of an entity type, referenced by Type.lifecycle (UB entity state machine).
  origin: [VRD-06-03, CON-UB-032, CON-US-022, CON-UW-022]
  fields:
    - {name: initial, type: string, required: true, description: "State given to new entities (_state)"}
    - name: states
      type: "map<string, LifecycleState>"
      required: true
      description: "code -> {title, editable - bool (default true), terminal - bool, ui_policy - enum{EDIT|READ_ONLY}, color?}"
    - name: transitions
      type: "list<LifecycleTransition>"
      required: true
      description: "{event, from - list<state> ('*' = any), to, guards?, title?} ; (from, event) unique"
  invariants:
    - initial and every to/from name a state; terminal states have no outgoing transitions.
    - _extends to another Lifecycle inherits states and transitions (child may add states/transitions and override guards/titles).
```

### 2.4 Dialogue

```yaml
ENT-Dialogue:
  purpose: Interaction state machine of an action (US Action FSM).
  origin: [VRD-06-03, CON-US-017, CON-US-022, CON-US-023]
  fields:
    - {name: initial, type: string, required: true, description: "Start state"}
    - name: states
      type: "map<string, {title, ui_policy - enum{EDIT|READ_ONLY}, final - bool}>"
      required: true
      description: "Dialogue states"
    - name: transitions
      type: "map<string, {from - list<string>, to, stage - enum{PREPARE|EXECUTE}, logic?, logic_ref?, guards?}>"
      required: true
      description: "event -> rule; PREPARE transitions are read-only, EXECUTE transitions write and flush"
  invariants:
    - Inherited along the action type chain; concrete actions usually override only params_schema and default_values (US).
```

### 2.5 Process instance

```yaml
ENT-Workflow:
  purpose: Blueprint of a durable process (LS MetaProcessType).
  origin: [VRD-06-04, CON-LS-017, CON-LS-020]
  fields:
    - {name: title, type: string, required: true, description: "Name"}
    - {name: input_schema, type: json_schema?, required: false, description: "Start arguments"}
    - {name: initial, type: string, required: true, description: "Start state"}
    - name: states
      type: "map<string, WorkflowState>"
      required: true
      description: "code -> {entry? - uri (logic run on entering), on - map<signal, {logic?, next?, guard?}>, timeout? - {after - duration, signal}, final? - bool}"
    - {name: correlation, type: "list<string>?", required: false, description: "Memory keys used to route external signals (REQ-FLOW-032)"}
```

```yaml
ENT-ProcessInstance:
  purpose: A running durable workflow (TERM-ProcessInstance); every signal commits a new version.
  origin: [VRD-06-04, CON-LS-017, CON-LS-020, CON-UC-026]
  fields:
    - {name: workflow, type: uri, required: true, description: "Workflow URI with ?commit (pinned blueprint)"}
    - {name: cursor, type: string, required: true, description: "Current state"}
    - {name: status, type: "enum{RUNNING|WAITING|COMPLETED|FAILED|CANCELLED}", required: true, description: "Status"}
    - {name: memory, type: json, required: true, description: "Instance variables"}
    - {name: correlation_keys, type: "list<string>", required: true, description: "Searchable keys, e.g. 'order:ubos://acme/Order/o-1'"}
    - {name: waiting_for, type: "list<{signal, deadline?}>", required: true, description: "Expected signals and timer deadlines"}
    - {name: context, type: uri?, required: false, description: "Context the instance runs in"}
    - {name: anchors, type: Anchors, required: true, description: "Pinned context/logic commits (REQ-TX-021)"}
    - {name: parent, type: uri?, required: false, description: "Parent instance"}
    - {name: last_error, type: error?, required: false, description: "Error of the last failed signal"}
```

### 2.6 Emit and intent

```yaml
ENT-Emit:
  purpose: A side-effect request returned or staged by logic (TERM-Emit); stored in the outbox at flush.
  origin: [VRD-06-05, CON-LS-020, CON-LS-025, CON-US-008]
  fields:
    - {name: kind, type: "enum{EVENT|ACTION|SIGNAL|JOB|NOTIFY|UI|MAIL|WEBHOOK}", required: true, description: "Dispatch kind"}
    - {name: target, type: uri?, required: false, description: "Entity, instance, principal, client (client://…) or webhook"}
    - {name: name, type: string, required: true, description: "Event type, action URI, signal name, job operation, template"}
    - {name: args, type: json?, required: false, description: "Payload"}
    - {name: delivery, type: "enum{AFTER_COMMIT|JOB}?", required: false, description: "AFTER_COMMIT = dispatched right after flush; JOB = as a durable job (default per kind, REQ-FLOW-040)"}
    - {name: delay, type: duration?, required: false, description: "Deliver no earlier than now + delay (uses a job)"}
    - {name: dedupe_key, type: string?, required: false, description: "Idempotency of delivery"}
```

```yaml
ENT-Intent:
  purpose: Client-side typed request in business words (TERM-Intent), mapped to UBTP operations.
  origin: [VRD-06-06, CON-UW-012, CON-UW-020, CON-UW-003]
  fields:
    - {name: kind, type: "enum{OPEN|VIEW|CREATE|EDIT|RUN_ACTION|TRANSITION|SEARCH|ASK}", required: true, description: "Intent kind"}
    - {name: target, type: uri?, required: false, description: "Entity or type"}
    - {name: action, type: uri?, required: false, description: "Action URI (RUN_ACTION/TRANSITION)"}
    - {name: event, type: string?, required: false, description: "Lifecycle or dialogue event"}
    - {name: args, type: json?, required: false, description: "Arguments"}
    - {name: view, type: uri?, required: false, description: "View to open (OPEN/VIEW)"}
    - {name: text, type: string?, required: false, description: "SEARCH/ASK text"}
    - {name: placement, type: "enum{WORKSPACE|DOCK}?", required: false, description: "Where the client shows it (UW view vs task routing)"}
```
