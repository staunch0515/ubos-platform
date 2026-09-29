---
id: SPEC-20-1
title: "Events, Triggers and Jobs — Model"
status: complete
phase: P4
depends_on: [SPEC-20, SPEC-11, SPEC-15, SPEC-16, SPEC-17, SPEC-19, SPEC-22, SPEC-24, ADR-004]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Events, Triggers and Jobs — Model

Part 1 of SPEC-20 (`20-events-jobs/`). Header, concepts and file list: `00-index.md`.

---

## 2. Model

```yaml
ENT-Event:
  purpose: A domain event (TERM-Event), carried in outbox rows and on subscriptions.
  origin: [VRD-11-01, CON-UC-027, CON-UP-025]
  fields:
    - {name: id, type: uuid, required: true, description: "Event id (UUID v7); deduplication key for consumers"}
    - {name: type, type: string, required: true, description: "Event type (§2.1)"}
    - {name: tenant, type: tenant_code, required: true, description: "Tenant"}
    - {name: subject, type: uri?, required: false, description: "Entity/process/job concerned (canonical, with commit when applicable)"}
    - {name: entity_type, type: type_code?, required: false, description: "Type of the subject entity"}
    - {name: action, type: "enum{CREATE|UPDATE|DELETE|RENAME|COPY|MERGE|REVERT|UNSET}?", required: false, description: "entity.committed only"}
    - {name: commit_id, type: commit_id?, required: false, description: "Commit"}
    - {name: branch, type: branch_name?, required: false, description: "Branch"}
    - {name: process_id, type: uuid, required: true, description: "Producing process"}
    - {name: principal, type: uri, required: true, description: "Acting principal"}
    - {name: occurred_at, type: timestamp, required: true, description: "Flush time"}
    - {name: causation, type: "list<uuid>", required: true, description: "Process chain (REQ-FLOW-041)"}
    - {name: changed_paths, type: "list<string>?", required: false, description: "entity.committed UPDATE - top-level paths changed"}
    - {name: data, type: json?, required: false, description: "Custom event payload; never raw snapshots"}
```

### 2.1 Event catalogue

| Type | Emitted when | Subject |
|---|---|---|
| `entity.committed` | every commit (one event per commit), `action` set | entity URI with commit |
| `process.completed` / `process.failed` / `process.rejected` | process end (failed/rejected via the failure-record transaction) | process |
| `job.queued` / `job.started` / `job.progress` / `job.succeeded` / `job.failed` / `job.dead` / `job.cancelled` | job transitions (`progress` is not durable, runtime only) | job URI |
| `approval.requested` / `approval.decided` | SPEC-22 approvals | ApprovalRequest |
| `branch.created` / `branch.merged` / `branch.deleted` | versioning | Branch |
| `package.installed` / `package.upgraded` | SPEC-28 | Package |
| `notification.created` | NOTIFY emits | Notification |
| `<namespace>.<name>` (e.g. `acme.invoice.posted`) | custom `emit.event` | as given |

Custom event types MUST be in a namespace the emitting package or tenant owns (REQ-TEN-010).

```yaml
ENT-Hook:
  purpose: Event subscription that triggers work (TERM-Hook).
  origin: [VRD-11-02, CON-UC-027, CON-UP-025]
  fields:
    - {name: title, type: string, required: true, description: "Name"}
    - {name: events, type: "list<string>", required: true, description: "Event type patterns (e.g. entity.committed, acme.invoice.*)"}
    - {name: filter, type: HookFilter?, required: false, description: "{types?: [uri] (subtypes incl.), actions?: [enum], branches?: [glob], changed_any?: [path], expression?: rhai (rule subset over event)}"}
    - {name: target, type: uri, required: true, description: "Action (EXECUTE on the subject), Pipeline, Logic, Workflow (start) or Workflow + correlation (signal)"}
    - {name: args, type: "map<string, string>?", required: false, description: "Arg name -> rule-subset expression over event"}
    - {name: correlation, type: string?, required: false, description: "Expression producing a correlation key (Workflow signal targets)"}
    - {name: signal, type: string?, required: false, description: "Signal name for Workflow targets"}
    - {name: mode, type: "enum{JOB|DIRECT}", required: true, description: "JOB = create a Job per firing (durable, retried); DIRECT = run a process directly after dispatch (lightweight, at-least-once)"}
    - {name: run_as, type: uri?, required: false, description: "Principal; default tenant ServiceAccount/system (REQ-CTX-007)"}
    - {name: context, type: string?, required: false, description: "Authority; default the event's context/branch"}
    - {name: retry, type: RetryPolicy?, required: false, description: "For JOB mode"}
    - {name: enabled, type: bool, required: true, description: "Disabled hooks never fire"}
```

```yaml
ENT-Cron:
  purpose: Time-triggered work (TERM-Cron).
  origin: [VRD-11-02, CON-UC-027, CON-UP-026, CON-LC-027]
  fields:
    - {name: title, type: string, required: true, description: "Name"}
    - {name: schedule, type: string, required: true, description: "Cron expression (5 fields, optional seconds field) or ISO 8601 repeating interval (R/PT15M)"}
    - {name: timezone, type: string, required: true, description: "IANA zone for evaluation (DST-aware)"}
    - {name: target, type: uri, required: true, description: "Pipeline, Action (with target entity in args), Logic or Workflow"}
    - {name: args, type: json?, required: false, description: "Static args; `${fire_time}` placeholder available"}
    - {name: run_as, type: uri?, required: false, description: "Default tenant system principal"}
    - {name: context, type: string?, required: false, description: "Authority; default tenant default Context"}
    - {name: overlap, type: "enum{SKIP|QUEUE|ALLOW}", required: true, description: "When the previous run is still queued/running"}
    - {name: catch_up, type: "enum{NONE|ONE|ALL}", required: true, description: "Missed fires after downtime (ALL capped at 100)"}
    - {name: jitter, type: duration?, required: false, description: "Random delay up to this value"}
    - {name: retry, type: RetryPolicy?, required: false, description: "For created jobs"}
    - {name: enabled, type: bool, required: true, description: "Enabled"}
```

```yaml
ENT-Job:
  purpose: Durable asynchronous work item (TERM-Job, ADR-004).
  origin: [VRD-11-03, CON-UC-013, CON-UC-032, CON-UC-026]
  fields:
    - {name: operation, type: uri, required: true, description: "Operation with @vN (Action, Pipeline, Logic, Native, Workflow signal)"}
    - {name: target, type: uri?, required: false, description: "Action target entity / instance"}
    - {name: args, type: json, required: true, description: "Arguments (≤ 256 KiB)"}
    - {name: anchors, type: Anchors, required: true, description: "Pinned operation/context commits (REQ-TX-021)"}
    - {name: context, type: string, required: true, description: "Authority"}
    - {name: run_as, type: uri, required: true, description: "Principal of attempts"}
    - {name: on_behalf_of, type: uri?, required: false, description: "Originating principal"}
    - {name: state, type: "enum{QUEUED|RUNNING|SUCCEEDED|FAILED|DEAD|CANCELLED}", required: true, description: "§2.2"}
    - {name: priority, type: int32, required: true, description: "0 (lowest) … 9; default 5"}
    - {name: not_before, type: timestamp?, required: false, description: "Delay"}
    - {name: timeout, type: duration, required: true, description: "Per-attempt wall time (default PT5M, max PT1H)"}
    - {name: retry, type: RetryPolicy, required: true, description: "{max_attempts (default 5), backoff: EXPONENTIAL|FIXED, base (PT10S), max (PT1H)}"}
    - {name: attempts, type: int32, required: true, description: "Attempts started"}
    - {name: lease_until, type: timestamp?, required: false, description: "RUNNING lease expiry"}
    - {name: worker, type: string?, required: false, description: "Claiming worker id"}
    - {name: last_error, type: error?, required: false, description: "Error of last attempt"}
    - {name: result, type: json?, required: false, description: "Result (≤ 256 KiB) or {ref: blob uri}"}
    - {name: last_process, type: uuid?, required: false, description: "Process of the last attempt"}
    - {name: parent_job, type: uri?, required: false, description: "Parent job (child jobs, CON-UC-026)"}
    - {name: reply_to, type: uri?, required: false, description: "ProcessInstance to signal on completion (REQ-FLOW-033)"}
    - {name: source, type: "{kind: EMIT|HOOK|CRON|SUBMIT|WEBHOOK|SYSTEM, ref?: uri, event?: uuid, fire_time?: timestamp}", required: true, description: "Origin"}
    - {name: dedupe_key, type: string?, required: false, description: "Unique among non-terminal jobs of the tenant (searchable)"}
    - {name: ephemeral, type: bool?, required: false, description: "High-volume jobs: RUNNING not committed (runtime lease only) (REQ-EVT-022)"}
```

### 2.2 Job states

```
           claim (CAS)                 success (in attempt flush)
 QUEUED ─────────────► RUNNING ───────────────────────────────────► SUCCEEDED
   ▲  │                   │  failure / timeout / lease expiry
   │  │ cancel            ├──► FAILED ──(attempts < max, backoff)──► QUEUED
   │  ▼                   │                └──(attempts = max)──────► DEAD
   │ CANCELLED ◄──────────┘ cancel observed
   └───────────── job.retry (admin) from DEAD / FAILED
```

```yaml
ENT-Webhook:
  purpose: Outbound HTTP notification subscription (TERM-Webhook).
  origin: [VRD-10-05, VRD-11-02, CON-FU-024]
  fields:
    - {name: title, type: string, required: true, description: "Name"}
    - {name: events, type: "list<string>", required: true, description: "Event type patterns"}
    - {name: filter, type: HookFilter?, required: false, description: "As for hooks"}
    - {name: url, type: string, required: true, description: "HTTPS URL (HTTP only for loopback in development)"}
    - {name: secret, type: "uri<Secret>", required: true, description: "HMAC secret"}
    - {name: headers, type: "map<string, string>?", required: false, description: "Extra static headers (no secrets)"}
    - {name: include_data, type: bool?, required: false, description: "Include masked effective data of the subject (read as owner)"}
    - {name: timeout_ms, type: int32, required: true, description: "≤ 10 000"}
    - {name: retry, type: RetryPolicy, required: true, description: "Retries on 5xx/429/network errors"}
    - {name: owner, type: uri, required: true, description: "Principal whose read permissions apply to included data"}
    - {name: active, type: bool, required: true, description: "Auto-set false after 50 consecutive failures (REQ-EVT-031)"}
```

```yaml
ENT-Notification:
  purpose: A user-facing notification (inbox item), created by NOTIFY emits.
  origin: [VRD-11-04, CON-UC-056, CON-UW-013]
  fields:
    - {name: recipient, type: uri, required: true, description: "Principal"}
    - {name: title, type: string, required: true, description: "Short text (i18n rendered at creation)"}
    - {name: body, type: text?, required: false, description: "Details"}
    - {name: link, type: "uri | Intent?", required: false, description: "Target to open (SPEC-19 Intent)"}
    - {name: level, type: "enum{INFO|SUCCESS|WARNING|ERROR|TASK}", required: true, description: "TASK items appear in the dock (SPEC-26)"}
    - {name: read_at, type: timestamp?, required: false, description: "Set by the recipient"}
    - {name: expires_at, type: timestamp?, required: false, description: "Hidden after"}
  invariants:
    - Visibility PRIVATE; owner = recipient.
```
