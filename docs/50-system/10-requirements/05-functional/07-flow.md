---
id: UBS-REQ-05-07
title: Functional Requirements — FLOW (Lifecycles, Actions, Approvals, Workflows)
status: draft
phase: PH-1
depends_on: [UBS-REQ-05, UBS-REQ-05-06]
---

# Functional Requirements — FLOW

## CAP-FLOW-01 — Lifecycles

### FR-FLOW-011 — Lifecycle declaration
- **Statement:** A class MAY declare a lifecycle. The lifecycle MUST declare:
  - its states, one initial state and its final states;
  - its transitions (from, to, triggering action, guards, effects);
  - per-state field rules (read-only, required and hidden fields).
- **Rationale:** The BPU "clock": controlled state progression (EXT-BPU).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given the `Invoice` lifecycle, when a transition from `paid` to `draft` is requested but not declared, then it fails with `FLOW.TRANSITION_NOT_ALLOWED`.
- **Verification:** CONF
- **Origin:** L40:SPEC-19

### FR-FLOW-012 — State changes only through transitions
- **Statement:** The lifecycle state field MUST be changed only by executing a declared transition. Direct writes to it MUST be rejected with `FLOW.STATE_DIRECT_WRITE`.
- **Rationale:** State integrity.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given an update that sets `state: approved`, when committed, then it fails.
- **Verification:** CONF
- **Origin:** L40:SPEC-19

### FR-FLOW-013 — Entry and exit effects
- **Statement:** States MAY declare entry and exit effects (L1 assignments or L2 logic). They MUST run in the same process as the transition, and their failure MUST abort the transition.
- **Rationale:** Consistent side conditions (for example stamping `approved_at`).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given an entry effect that fails, when the transition runs, then the state is unchanged, and nothing is committed.
- **Verification:** CONF
- **Origin:** L40:SPEC-19

### FR-FLOW-014 — Lifecycle evolution
- **Statement:** When a lifecycle changes, existing objects MUST keep their current state. Removing or renaming a state that is in use MUST require a state mapping in the lens (FR-MODEL-082).
- **Rationale:** Process changes must not orphan objects.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given 40 objects in `review` and a lifecycle that removes `review`, when committed without a mapping, then it fails with `MODEL.LENS_REQUIRED`.
- **Verification:** CONF
- **Origin:** IMP-04

### FR-FLOW-015 — Lifecycle inheritance
- **Statement:** A subclass MAY add states, transitions, guards and effects to an inherited lifecycle. It MUST NOT remove inherited states or transitions.
- **Rationale:** Narrowing-only applies to behaviour too.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given a subclass that removes the transition `approve`, when committed, then it fails with `MODEL.NARROWING_VIOLATION`.
- **Verification:** CONF
- **Origin:** L40:SPEC-13

## CAP-FLOW-02 — Actions (ports)

### FR-FLOW-021 — Port declaration
- **Statement:** A port MUST declare:
  - its name and description;
  - its parameters with types and constraints;
  - its result type and the error codes it may return;
  - its kind (`query` without effects, or `command`);
  - the lifecycle states in which it is available;
  - the permission or policy action it requires and its idempotency behaviour;
  - UI hints (label, icon, confirmation text).
- **Rationale:** Typed input pins of the BPU (EXT-BPU).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect, PER-AiAgent
- **Acceptance:**
  1. Given a port declaration missing its kind, when committed, then it fails with `MODEL.DECLARATION_INCOMPLETE`.
- **Verification:** CONF
- **Origin:** L40:SPEC-19

### FR-FLOW-022 — Uniform invocation
- **Statement:** A port MUST behave identically, including validation, decisions, guards and audit, whether it is invoked from Workspace, the SDK, UBTP, REST, an agent tool, a schedule or another port.
- **Rationale:** No side doors.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given the same invocation through 4 channels, when compared, then the effects and errors are identical except for the channel attribution.
- **Verification:** SCN, CONF
- **Origin:** L40:SPEC-24

### FR-FLOW-023 — Batch invocation
- **Statement:** A command port MUST be invocable on a set of objects. The caller chooses `atomic` (all or nothing) or `independent` mode, and the platform returns a per-item result.
- **Rationale:** Bulk approvals and updates.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-BusinessUser, PER-Approver
- **Acceptance:**
  1. Given 20 objects of which 2 fail guards, when invoked `independent`, then 18 succeed and 2 report errors. When invoked `atomic`, none succeed.
- **Verification:** CONF
- **Origin:** NEW

### FR-FLOW-024 — Available actions with reasons
- **Statement:** For an object and a principal, the platform MUST list the ports that are available now, and for unavailable ports the reason (state, guard, permission, decision).
- **Rationale:** UIs show only valid actions, and agents plan with them.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessUser, PER-AiAgent
- **Acceptance:**
  1. Given a `submitted` invoice viewed by its maker, when actions are listed, then `approve` is unavailable with reason "maker–checker".
- **Verification:** CONF
- **Origin:** L40:SPEC-25

## CAP-FLOW-03 — Guards

### FR-FLOW-031 — Guards with explanations
- **Statement:** Guards MUST be L1 expressions or L2 functions that return pass or fail with an explanation. They are attached to ports and transitions.
- **Rationale:** Business preconditions (SCN-003).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessAnalyst
- **Acceptance:**
  1. Given the notice-period guard of SCN-003, when it fails, then the explanation names the guard, the notice end date and today.
- **Verification:** CONF
- **Origin:** L40:SPEC-19

### FR-FLOW-032 — Guards are recorded
- **Statement:** Guard evaluations (results and versions) for executed actions MUST be recorded in the rule binding of the resulting commit.
- **Rationale:** Evidence that checks were applied.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given an executed transition, when inspected, then each evaluated guard and its version is listed.
- **Verification:** CONF
- **Origin:** IMP-02

### FR-FLOW-033 — No partial effects on guard failure
- **Statement:** When any guard fails, the action MUST have no effect other than an optional audit record of the attempt.
- **Rationale:** Atomic semantics.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given a guard that fails after an effect was computed, when the action ends, then no change is committed.
- **Verification:** CONF
- **Origin:** L40:SPEC-19

## CAP-FLOW-04 — Approvals and segregation of duties

### FR-FLOW-041 — Approval requirements
- **Statement:** Approval requirements MUST be declarable through sheets and decisions: the number of approvals, eligible approvers (roles, relationships, named principals), sequential or parallel order, conditions and expiry.
- **Rationale:** Rule-driven approvals (SCN-012, SCN-202).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer, PER-Approver
- **Acceptance:**
  1. Given SCN-012, when the invoice is submitted, then two approval tasks are created for eligible approvers.
- **Verification:** SCN, CONF
- **Origin:** L40:SPEC-22

### FR-FLOW-042 — Deferred commits
- **Statement:** An action that requires approval MUST be stored as a pending change (a deferred commit). It is applied atomically when the final approval is given and discarded on rejection or expiry. Its effects MUST NOT be visible on the target before application.
- **Rationale:** Approval as deferred commit (L40:SPEC-22).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-Approver
- **Acceptance:**
  1. Given a pending transition, when other users read the invoice, then it is still `submitted`. After the final approval, it is `approved` in one commit.
- **Verification:** CONF, SCN
- **Origin:** L40:SPEC-22

### FR-FLOW-043 — Maker–checker and SoD matrix
- **Statement:** The platform MUST enforce maker–checker (the initiator cannot approve) and a configurable segregation-of-duties matrix of incompatible duties, across the whole approval chain and across delegations.
- **Rationale:** Core financial control (SOX, SCN-012).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given a user who submitted, when the same user approves directly or through a delegation, then it is denied with an SoD explanation.
- **Verification:** CONF, SEC
- **Origin:** NEW — SOX ITGC.

### FR-FLOW-044 — Approval evidence
- **Statement:** Each approval MUST record the approver, time, decision, comment, the hash of the exact pending change or diff approved, and the authentication strength. Strong authentication MAY be required by policy.
- **Rationale:** Non-repudiation of approvals.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given an approval, when inspected, then the diff hash matches the applied change.
- **Verification:** CONF
- **Origin:** NEW

### FR-FLOW-045 — Invalidation on change
- **Statement:** If the subject of a pending approval changes, all collected approvals MUST be invalidated, and approvers MUST see the difference since their decision.
- **Rationale:** SCN-012 A2.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-Approver
- **Acceptance:**
  1. Given SCN-012 A2, when the invoice is edited, then the approvals are voided.
- **Verification:** CONF, SCN
- **Origin:** NEW

### FR-FLOW-046 — One approval mechanism
- **Statement:** Change-set approvals, action approvals and agent change-set reviews MUST use the same approval mechanism and evidence model.
- **Rationale:** Consistency and less to learn.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-Approver, PER-AgentSupervisor
- **Acceptance:**
  1. Given approvals of the three kinds, when their records are compared, then they share one schema.
- **Verification:** INSP
- **Origin:** NEW

## CAP-FLOW-05 — Tasks and work queues

### FR-FLOW-051 — Task objects
- **Statement:** Human work items MUST be task objects with type, subject object, assignee (a principal, role or queue), due time, priority, state (`open`, `claimed`, `completed`, `cancelled`) and outcome.
- **Rationale:** Inbox and queue management.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given an approval requirement, when triggered, then a task exists with the subject and due time.
- **Verification:** CONF
- **Origin:** L40:SPEC-19

### FR-FLOW-052 — Assignment rules
- **Statement:** Queues MUST support assignment rules: manual claim, round robin, least loaded and attribute or skill match.
- **Rationale:** Operations teams balance work.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-FundOperationsManager
- **Acceptance:**
  1. Given round robin over 3 users, when 9 tasks arrive, then each receives 3.
- **Verification:** CONF
- **Origin:** NEW

### FR-FLOW-053 — Claim, release, reassign
- **Statement:** Users MUST be able to claim, release and (with permission) reassign tasks. Every change MUST be recorded.
- **Rationale:** Operational flexibility with an audit trail.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given a claimed task, when released, then it returns to the queue, and history shows both events.
- **Verification:** CONF
- **Origin:** NEW

### FR-FLOW-054 — Inbox
- **Statement:** Each user MUST have an inbox combining their assigned tasks, their queue tasks and their approvals, sortable by due time and priority, with counts.
- **Rationale:** A single place for work.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser, PER-Approver
- **Acceptance:**
  1. Given 5 own tasks and 3 queue tasks, when the inbox is opened, then 8 items are listed with sources.
- **Verification:** SCN, USE
- **Origin:** NEW

## CAP-FLOW-06 — Durable workflows

### FR-FLOW-061 — Workflow definitions
- **Statement:** The platform MUST support versioned workflow definitions with the step types `action`, `wait_event`, `timer`, `human_task`, `decision`, `parallel` (with join), `sub_workflow` and `end`.
- **Rationale:** Multi-step processes across objects and time.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect, PER-LogicDeveloper
- **Acceptance:**
  1. Given a workflow with a parallel branch and join, when both branches complete, then the join fires once.
- **Verification:** CONF
- **Origin:** L40:SPEC-19

### FR-FLOW-062 — Durable instances
- **Statement:** Workflow instances MUST persist their state after each step and resume after crashes. Running instances MUST continue on the definition version they started with unless explicitly migrated.
- **Rationale:** Reliability and predictable change.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, NOD
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a crash during a timer wait, when the node restarts, then the timer fires at its original due time.
- **Verification:** FAULT, SIM
- **Origin:** L40:SPEC-19

### FR-FLOW-063 — Business-calendar timers
- **Statement:** Timers MUST support absolute times, durations and business-calendar expressions (for example "3 business days, cut-off 12:00 New York").
- **Rationale:** Deadlines in finance follow business calendars.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-FundOperationsManager
- **Acceptance:**
  1. Given "2 business days" from Friday before a Monday holiday, when computed, then the due date is Wednesday.
- **Verification:** CONF
- **Origin:** NEW

### FR-FLOW-064 — Event correlation
- **Statement:** `wait_event` steps MUST correlate events to instances by declared correlation keys, and events that arrive before the wait MUST be buffered for a declared window.
- **Rationale:** Asynchronous confirmations (for example settlement messages).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given a settlement event that arrives 1 second before the wait starts, when the wait starts, then the buffered event completes it.
- **Verification:** CONF, SIM
- **Origin:** NEW

## CAP-FLOW-07 — Dialogues

### FR-FLOW-071 — Actions that ask for input
- **Statement:** An action MAY return `needs_input` with a typed request (a schema and a message) instead of completing. The caller supplies the input to continue the same action instance within a timeout. No state MUST be committed before completion.
- **Rationale:** Interactive decisions such as "choose the account to credit" without splitting the action.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** DVM, WSP, SDK
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given an action that needs an account, when the user supplies it, then the action completes in one commit. On timeout nothing is committed.
- **Verification:** CONF
- **Origin:** L40:SPEC-19

### FR-FLOW-072 — Dialogues for agents
- **Statement:** Dialogue requests MUST be machine-readable so that agents can answer them or escalate them to humans.
- **Rationale:** Agents use the same interaction model.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** DVM, AGT
- **Personas:** PER-AiAgent
- **Acceptance:**
  1. Given an agent that receives `needs_input`, when it answers with a valid value, then the action completes.
- **Verification:** CONF
- **Origin:** IMP-10

## CAP-FLOW-08 — SLAs and escalation

### FR-FLOW-081 — Due-date rules
- **Statement:** Tasks, obligations and workflow steps MUST support due-date rules expressed as L1 expressions over business calendars.
- **Rationale:** SCN-206.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-FundOperationsManager
- **Acceptance:**
  1. Given a rule "90 days before the renewal date, previous business day", when computed, then the date matches the calendar.
- **Verification:** CONF
- **Origin:** NEW

### FR-FLOW-082 — Escalation chains
- **Statement:** Overdue items MUST trigger configurable escalation steps (notify, reassign, raise priority), in order, with delays.
- **Rationale:** No silent misses.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-FundOperationsManager
- **Acceptance:**
  1. Given an overdue obligation, when 1 day overdue, then the manager is notified. After 3 days it is reassigned.
- **Verification:** CONF
- **Origin:** NEW

### FR-FLOW-083 — SLA reporting
- **Statement:** The platform MUST report SLA attainment per task type, queue and period.
- **Rationale:** Operations management.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-FundOperationsManager
- **Acceptance:**
  1. Given a month of tasks, when reported, then on-time percentages per queue are shown.
- **Verification:** SCN
- **Origin:** NEW

## CAP-FLOW-09 — Delegation

### FR-FLOW-091 — Delegation objects
- **Statement:** A principal MUST be able to delegate tasks and approval authority to another principal for a period and a scope (task types, classes, amount limits). The delegate acts "on behalf of" the delegator.
- **Rationale:** Absence cover.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-Approver
- **Acceptance:**
  1. Given a delegation for approvals up to 100,000, when the delegate approves 80,000, then it succeeds with on-behalf-of attribution. When the delegate approves 120,000, it is denied.
- **Verification:** CONF
- **Origin:** NEW

### FR-FLOW-092 — Delegation within policy
- **Statement:** Delegations MUST NOT grant more authority than the delegator holds and MUST respect SoD rules for the delegate.
- **Rationale:** Delegation is not a privilege escalation path.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a delegate who initiated the item, when the delegate approves it through the delegation, then it is denied.
- **Verification:** CONF, SEC
- **Origin:** NEW

### FR-FLOW-093 — Delegation audit
- **Statement:** Every delegated action MUST record both principals, and delegations MUST be listed in access reviews.
- **Rationale:** Traceability.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given a delegated approval, when audited, then the delegator and the delegate are shown.
- **Verification:** CONF
- **Origin:** NEW
