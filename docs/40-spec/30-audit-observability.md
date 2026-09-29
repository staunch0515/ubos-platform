---
id: SPEC-30
title: "Audit and Observability"
status: complete
phase: P4
depends_on: [SPEC-11, SPEC-15, SPEC-16, SPEC-21, SPEC-22]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Audit and Observability

## 0. Chapter header

- **Scope:** This chapter covers:
  - the audit record of a process and the decision summary it carries;
  - persistence of the execution trace (inline and offloaded) and redaction;
  - typed lineage, including TRIGGERED_BY;
  - read auditing;
  - audit export for archives and SIEM;
  - tamper evidence and privacy redaction (later version);
  - history analytics;
  - operational telemetry: spans, structured logs, the request log, metrics.

  Business history itself is the version store (SPEC-11/14). This chapter covers the evidence *about* changes and the operational signals.
- **MOD:** `OBS`
- **depends_on:** SPEC-11, SPEC-15, SPEC-16, SPEC-21, SPEC-22.
- **Terms used:** TERM-AuditRecord, TERM-TraceTree, TERM-Provenance, TERM-Process, TERM-Blame.
- **Origin summary:**
  - Verdicts: VRD-21-01…05, VRD-04-01, VRD-04-05, VRD-07-02, VRD-16-06.
  - UP: the process log (CON-UP-017).
  - UB: ProcessContext trees and billing linkage (CON-UB-020, CON-UB-043).
  - UC: ProcessContext, provenance and log capture (CON-UC-021, 022, 030, 042), robustness notes (CON-UC-035).
  - LC: dual audit, the request log and handler versions (CON-LC-003, 041, 044).
  - FU: blame, audit queries and metrics (CON-FU-041, 043, 044).
  - LS: the instrumented kernel (CON-LS-064).
  - US: causal traces as the basis of explanations (CON-US-029, 061).

---

## 1. Concepts

| Layer | Question answered | Storage | Written | Chapter |
|---|---|---|---|---|
| Facts | What is and was the state? | `entity_version`, `branch_head` | in the flush | SPEC-11 |
| Audit record | Who did what, when, where, with which result? | `process_log` row | in the flush (or right after a failure) | here |
| Lineage | Which versions did a change produce, depend on, or get triggered by? | `process_commit_map` | in the flush | here, SPEC-15 |
| Trace | How did the execution unfold, and why was something rejected? | `trace_summary` inline, full trace in the blob store | in the flush | here, SPEC-16 |
| Telemetry | How is the system behaving? (latency, errors, load) | OTel spans, logs, metrics (external) | continuously | here |
| Analytics | Who changes what, and how often? | queries over the above | on demand | here, SPEC-21 |

The evidence of a process is atomic with the process: the audit record, the lineage and the trace summary are written in the same transaction as its commits (VRD-04-01).

---

## 2. Model

```yaml
ENT-AuditRecord:
  purpose: Logical view of one process's evidence (TERM-AuditRecord) = process_log row + lineage + trace.
  origin: [VRD-21-01, CON-UP-017, CON-UB-043, CON-LC-041]
  fields:
    - {name: process, type: ENT-ProcessLogRow, required: true, description: "SPEC-11 §2.5"}
    - {name: outputs, type: "list<{uri, commit_id, action}>", required: true, description: "OUTPUT lineage"}
    - {name: dependencies, type: "list<{uri, commit_id, relation}>", required: true, description: "DEPENDS_ON_* and TRIGGERED_BY lineage"}
    - {name: decisions, type: "list<DecisionSummary>", required: true, description: "From trace_summary.decisions (REQ-OBS-002)"}
    - {name: trace, type: "TraceTree | {ref: uri}", required: true, description: "Inline summary or offloaded reference"}
    - {name: children, type: "list<uuid>", required: true, description: "Processes with parent_process_id = this (jobs, approvals, retries)"}
```

```yaml
ENT-TraceSummary:
  purpose: Content of process_log.trace_summary (≤ 64 KiB).
  origin: [VRD-21-01, VRD-21-03, CON-UB-020, CON-UC-022]
  fields:
    - {name: level, type: "enum{SUMMARY|DETAILED}", required: true, description: "Tracing level used"}
    - {name: nodes, type: "list<TraceNode>", required: true, description: "SPEC-16 ENT-TraceNode, pruned to fit (REQ-OBS-003)"}
    - {name: truncated, type: bool, required: true, description: "True when nodes were pruned"}
    - {name: decisions, type: "list<DecisionSummary>", required: true, description: "{source, code, allow, severity, path?, target?, overridden?} for every deny, warning, veto, override and authorisation denial"}
    - {name: emits, type: "list<{kind, name, target}>", required: true, description: "Emits written to the outbox"}
    - {name: errors, type: "list<error>", required: true, description: "Top-level errors (masked)"}
```

```yaml
ENT-TelemetryConventions:
  purpose: Names and attributes shared by spans, logs and metrics (REQ-OBS-020…023).
  origin: [VRD-21-05, CON-LS-064, CON-LC-003]
  fields:
    - {name: span_names, type: "list<string>", required: true, description: "ubtp.request, process, process.stage, flow.step, logic.call, syscall, rule.evaluate, store.flush, store.query, job.attempt, outbox.dispatch, ai.call"}
    - {name: attributes, type: "list<string>", required: true, description: "ubos.tenant, ubos.process_id, ubos.operation, ubos.principal (id only), ubos.context, ubos.branch, ubos.commit_id, ubos.job_id, ubos.binding, ubos.client_kind, error.code"}
```

---

## 3. Behavior

### 3.1 Audit records

### REQ-OBS-001 — Completeness
- **Statement:** Every process that commits, fails or is rejected MUST have exactly one audit record (REQ-STO-009). It contains:
  - the operation URI with version and its definition commit (DEPENDS_ON_FUNC);
  - the principal and on-behalf-of, the app, tenant, context and branch;
  - the request ID and idempotency key;
  - the status, timing, error (masked) and metrics;
  - the decision summary and the trace (REQ-OBS-002/003);
  - the lineage (REQ-OBS-005).

  Read-only processes produce records only under read auditing (REQ-OBS-006), and empty processes only when `audit_always` is set (REQ-TX-012).
- **Origin:** VRD-21-01, VRD-04-01, CON-UP-017, CON-LC-041
- **Acceptance:** For each process kind (MUTATE, EMIT, JOB, MERGE, ADMIN, BOOT, SYSTEM), a scenario checks that the record has all fields.
- **Priority:** P0

### REQ-OBS-002 — Decision summary
- **Statement:** The trace summary MUST list every Decision that affected the outcome:
  - validation denials and warnings (SPEC-18);
  - guard results;
  - authorisation denials and approvals required (SPEC-22);
  - vetoes and applied overrides (REQ-RULE-031);
  - each with source, code, path and target.

  A REJECTED process's record MUST allow the reason to be reconstructed without re-running the process.
- **Origin:** VRD-21-01 ("a summary of rule decisions"), VRD-09-05, CON-US-029
- **Acceptance:** A rejected invoice approval's audit shows the unmet guard `has_lines` and the invariant `no_overdraft` with paths.
- **Priority:** P0

### REQ-OBS-003 — Trace persistence and limits
- **Statement:** At flush, or when a failure is recorded:
  1) If the serialised trace is ≤ 64 KiB, it is stored inline in `trace_summary`.
  2) Otherwise, the full trace (≤ 16 MiB, compressed with zstd) is written to the blob store before the transaction, and referenced in `trace_ref` as `blob://sha256/<hex>`. The inline summary then keeps the root, the stage and step nodes, all error and veto nodes with their ancestors, and the decision list, with `truncated: true`.
  3) Traces above 16 MiB keep the first 16 MiB of nodes in breadth-first order, marked as truncated.

  Offloaded trace blobs are subject to the tenant's retention (REQ-OBS-011). Losing a blob degrades to the inline summary and never invalidates the record.
- **Rationale:** UC's own design proposed storing the full ProcessContext with truncation or object storage for heavy use (CON-UC-022). The blob store keeps the six-table rule.
- **Origin:** VRD-21-03, VRD-07-02, CON-UC-022, CON-UC-030
- **Acceptance:** A pipeline with 5 000 syscalls at DETAILED level stores an offloaded trace, and its inline summary still shows the failing step.
- **Priority:** P0 (inline), P1 (offload)

### REQ-OBS-004 — Redaction in evidence
- **Statement:**
  - Inputs, outputs, logs and errors in traces and audit records MUST be masked with the process principal's view (REQ-SEC-021).
  - Secrets and SECRET-sensitivity values MUST NOT appear in any form.
  - Readers of an audit record see it masked again with *their* view.
  - `input_digest` hashes the unmasked canonical arguments, so a later verification is possible without storing them.
- **Origin:** VRD-10-03, REQ-CTX-003, CON-UC-035 (robustness notes on logs)
- **Acceptance:** A login process trace contains no password. A support user viewing an audit record sees masked customer data.
- **Priority:** P0

### REQ-OBS-005 — Lineage completeness
- **Statement:** The lineage of each process MUST contain:
  - OUTPUT rows for its commits;
  - DEPENDS_ON_* rows per REQ-TX-020;
  - a TRIGGERED_BY row naming the commit whose event started it, for processes started by hooks, correlation signals or deferred emits.

  Processes started by a parent process also carry `parent_process_id`. Together these allow the full causal chain to be walked: request → process → commit → event → triggered process → …
- **Origin:** VRD-21-02, VRD-04-05, CON-UC-042
- **Acceptance:** From a shipment created by a hook, `lineage.of_commit PRODUCED_BY depth 3` reaches the payment commit and the user's payment request.
- **Priority:** P0

### REQ-OBS-006 — Read auditing
- **Statement:**
  - When a tenant enables `read_audit`, or a Query entity declares `audit: true`, each read process MUST write an audit record of kind QUERY with the returned entities as DEPENDS_ON_DATA (up to 1 000 per process, then summarised).
  - Reads of properties with sensitivity PERSONAL or SECRET by principals holding unmask permissions MUST always be audited, whatever the setting.
- **Origin:** VRD-21-01, CON-LC-002 (QUERY log entries), NFR-PRIV
- **Acceptance:** Viewing an unmasked IBAN produces an audit record naming the viewer and the entity.
- **Priority:** P1

### REQ-OBS-007 — Audit immutability
- **Statement:** Audit records and lineage rows MUST be append-only (REQ-STO-009/010). Only three exceptions exist:
  - setting `billing_commit_id` once;
  - tenant purge (REQ-TEN-004);
  - privacy redaction (REQ-OBS-013).

  All three are themselves audited in `logrums`.
- **Origin:** VRD-21-01, REQ-STO-003
- **Acceptance:** A database trigger (PG) or a store check (SQLite) rejects updates to process rows other than the billing column.
- **Priority:** P0

### 3.2 Retention, export, integrity

### REQ-OBS-010 — Audit export
- **Statement:** `audit.export@v1 {from, to, filters, format: NDJSON}` MUST stream audit records, with lineage and decision summaries, as NDJSON to a blob, for archiving or a SIEM. It requires `tenant.admin`. It replaces LC's per-process JSON files (CON-LC-044) with an on-demand, consistent export. Exports are masked as for the requester, unless the requester holds `entity.unmask.*`.
- **Origin:** CON-LC-044, VRD-21-01
- **Acceptance:** Exporting a day of audit records yields one NDJSON line per process, which a SIEM parser can ingest.
- **Priority:** P1

### REQ-OBS-011 — Retention
- **Statement:** A SYSTEM job MUST enforce the tenant settings `retention.process_log` and `retention.versions_online` (SPEC-23 §2.1):
  - it detaches or archives old partitions (REQ-STO-015, enterprise);
  - it deletes offloaded trace blobs older than the trace retention (default 180 days);
  - it never deletes audit rows within the process-log retention.
- **Origin:** REQ-STO-015, CON-UB-040
- **Acceptance:** Trace blobs older than the retention are deleted, and their records keep the inline summary with `trace_ref_expired: true`.
- **Priority:** P2

### REQ-OBS-012 — Tamper evidence
- **Statement:** A SYSTEM job SHOULD compute, per tenant and day, a Merkle root over the canonical process rows and OUTPUT lineage of that day, and commit it as an `AuditSeal` entity in `logrums`. Each seal includes the previous seal's hash. `audit.verify@v1 {tenant, day}` recomputes the root and compares it with the seal.
- **Origin:** NEW: tamper evidence for audit-by-default (strengthens VRD-21-01)
- **Acceptance:** Changing a process row directly in the database makes `audit.verify` for that day fail.
- **Priority:** P2

### REQ-OBS-013 — Privacy redaction
- **Statement:** `privacy.redact@v1 {uri, paths, reason}` (a tenant admin action that requires approval) MUST:
  - write a new version without the listed personal values;
  - scrub the same paths in the entity's **older versions** and in their search index rows, replacing the values with a redaction marker;
  - record the redaction as an ADMIN process with DEPENDS_ON_DATA on every scrubbed version.

  This is the only in-tenant exception to version immutability (REQ-STO-003). Snapshot hashes of scrubbed versions are kept as they were, together with a `redacted: true` flag in `details`.
- **Origin:** NEW: legal erasure of personal data; complements REQ-TEN-004
- **Acceptance:** After redaction, no version of the entity returns the erased email, and the audit shows who redacted it and why.
- **Priority:** P2

### 3.3 Analytics and explanations

### REQ-OBS-015 — History analytics
- **Statement:** The kernel MUST provide these NATIVE queries (authorised as `tenant.admin`, or as reads of the scoped entities), computed from the version store and the process log:

| Query | Result |
|---|---|
| `audit.metrics@v1 {from, to, group_by: day\|type\|principal\|operation}` | commits, processes by status, failures, conflicts, median and p95 durations |
| `audit.activity@v1 {principal?, type?, from, to}` | activity feed (processes with their outputs) |
| `audit.hotspots@v1 {from, to}` | entities with the most commits or conflicts (contention analysis) |

  Blame, timelines and branch status are in SPEC-21 and SPEC-14.
- **Origin:** VRD-21-04, CON-FU-044, CON-FU-043
- **Acceptance:** The operational dashboard (SPEC-26) shows commits per type per day from `audit.metrics`.
- **Priority:** P1

### REQ-OBS-016 — Trace retrieval and explanation input
- **Statement:** `audit.trace@v1 {process_id, node?, depth?}` MUST return the (masked) trace, fetching offloaded blobs when present. `audit.explain_input@v1 {process_id}` MUST return a compact, structured explanation input for the AI explainer (SPEC-27):
  - the decisions;
  - the failing nodes with their inputs;
  - the changed paths with before/after values (masked);
  - the definitions involved (URIs and commits).
- **Origin:** VRD-16-06, VRD-21-03, CON-US-061
- **Acceptance:** For a rejected salary change, the explanation input lists the invariant, the path and the values, which the explainer turns into one sentence.
- **Priority:** P1

### 3.4 Telemetry

### REQ-OBS-020 — Tracing spans
- **Statement:** The kernel and the hosts MUST emit OpenTelemetry-compatible spans (`tracing` + OTLP exporter) with the names and attributes of ENT-TelemetryConventions:
  - The W3C `traceparent` from the envelope (REQ-PROTO-001) is continued. Otherwise a new trace is started.
  - The trace ID is recorded in the audit record's metrics, so telemetry and audit are linked.
  - Span attributes MUST NOT contain snapshot values or secrets.
  - Sampling is configurable, and errors are always sampled.
- **Origin:** VRD-21-05, CON-LS-064
- **Acceptance:** One request produces a span tree ubtp.request → process → stage → logic.call → syscall → store.flush in the configured collector.
- **Priority:** P1

### REQ-OBS-021 — Structured logs
- **Statement:** Hosts MUST write logs as JSON lines with these fields:
  - `ts`, `level`, `target`, `msg`;
  - the ENT-TelemetryConventions attributes, when a process is active;
  - `error.code`.

  Script logs (`log.*`) go to the trace, not to host logs, except at WARN or ERROR when the tenant enables forwarding. The default level is INFO, and it can be changed per module at run time through the admin endpoint.
- **Origin:** VRD-21-05, REQ-CONV-083, CON-UC-030
- **Acceptance:** Filtering host logs by `ubos.process_id` shows every kernel log line of that process.
- **Priority:** P0

### REQ-OBS-022 — Request log
- **Statement:** The gateway MUST emit one request-log event per UBTP request, in the log stream under target `ubos::request`. It contains:
  - binding, client kind and version, principal ID, tenant, op, target URI;
  - request ID, process ID, terminal status and error code;
  - duration, request and response bytes;
  - remote address (a hash of it when privacy mode is on).

  The request log is telemetry. It is not stored in the six tables. The process log is the durable business audit.
- **Origin:** VRD-21-05, CON-LC-003 (lcm_request_log)
- **Acceptance:** Every request, including authentication failures, has exactly one request-log line.
- **Priority:** P1

### REQ-OBS-023 — Metrics
- **Statement:** Hosts MUST expose Prometheus metrics at `GET /metrics` on the admin listener (REQ-SEC-070). At least these series:

| Metric | Type | Labels |
|---|---|---|
| `ubos_requests_total` | counter | binding, op, status |
| `ubos_request_duration_seconds` | histogram | binding, op |
| `ubos_processes_total` | counter | kind, status |
| `ubos_flush_duration_seconds` | histogram | edition |
| `ubos_commits_total` | counter | action |
| `ubos_head_conflicts_total` | counter | – |
| `ubos_validation_rejections_total` | counter | code |
| `ubos_jobs_queued` | gauge | priority |
| `ubos_job_latency_seconds` | histogram | operation kind |
| `ubos_outbox_lag_seconds` | gauge | – |
| `ubos_cache_hit_ratio` | gauge | cache (snapshot, effective_type, head, compile, context, principal_set) |
| `ubos_rhai_operations_total` | counter | – |
| `ubos_ai_tokens_total` | counter | provider, direction |
| `ubos_sessions_active` | gauge | binding |
| `ubos_store_pool_in_use` | gauge | – |

  Tenant codes are not used as labels, to keep cardinality bounded. Per-tenant figures come from `audit.metrics`.
- **Origin:** VRD-21-05, CON-FU-044, CON-LC-046
- **Acceptance:** Scraping `/metrics` during load shows the series, and the outbox lag rises when the dispatcher is stopped.
- **Priority:** P1

---

## 4. Interfaces

### API-OBS-001 — AuditService
- **Kind:** rust trait (`ubos_kernel::obs`)
- **Signature / shape:**
```rust
#[async_trait]
pub trait AuditService: Send + Sync {
    fn finalize(&self, p: &Process, x: &ExecutionContext) -> Result<AuditArtifacts, UbosError>; // row + lineage + trace (for the flush)
    async fn record_failure(&self, p: &Process, x: &ExecutionContext, err: &UbosError) -> Result<(), UbosError>;
    async fn record(&self, x: &ExecutionContext, process_id: Uuid) -> Result<AuditRecord, UbosError>;
    async fn trace(&self, x: &ExecutionContext, process_id: Uuid, sel: TraceSelector) -> Result<TraceTree, UbosError>;
    async fn export(&self, x: &ExecutionContext, req: AuditExport) -> Result<BlobRef, UbosError>;
}
pub struct AuditArtifacts { pub row: ProcessLogRow, pub lineage: Vec<LineageRow>, pub trace_blob: Option<(BlobRef, Bytes)> }
```
- **Origin:** CON-UB-020 (toFullJson), CON-UC-022

### API-OBS-002 — Named queries
- **Kind:** UBTP Query targets
- **Signature / shape:**

| URI | Args | Result |
|---|---|---|
| `ubos://system/Query/audit.record@v1` | `{process_id}` | AuditRecord |
| `ubos://system/Query/audit.trace@v1` | `{process_id, node?, depth?}` | TraceTree |
| `ubos://system/Query/audit.explain_input@v1` | `{process_id}` | explanation input |
| `ubos://system/Query/audit.processes@v1` | SPEC-21 REQ-QRY-021 | process rows |
| `ubos://system/Query/audit.metrics@v1` / `audit.activity@v1` / `audit.hotspots@v1` | REQ-OBS-015 | analytics |
| `ubos://system/Action/audit.export@v1` | `{from, to, filters}` | blob sha256 |
| `ubos://system/Query/audit.verify@v1` | `{tenant, day}` | `{ok, expected, actual}` |
| `ubos://system/Action/privacy.redact@v1` | `{uri, paths, reason}` | ApprovalRequest / result |
- **Origin:** CON-FU-043, CON-FU-044

### Example audit record (abridged)

```json
{ "process": { "process_id": "0192…f3", "tenant_id": "acme", "operation": "ubos://acme/Action/invoice.approve@v1",
    "kind": "EMIT", "status": "REJECTED", "principal_id": "0191…a1", "context_uri": "ubos://acme/Context/prod",
    "branch_name": "main", "request_id": "e7", "started_at": "2026-09-29T08:00:00.120Z", "finished_at": "2026-09-29T08:00:00.161Z",
    "error": { "code": "RULE.GUARD_FAILED", "category": "INVALID", "message": "Transition guards not met", "retryable": false },
    "metrics": { "rhai_ops": 1840, "syscalls": 6, "db_ms": 4, "trace_id": "4bf92f3577b34da6a3ce929d0e0e4736" } },
  "outputs": [],
  "dependencies": [ { "uri": "ubos://acme/Action/invoice.approve@v1?commit=880", "relation": "DEPENDS_ON_FUNC" },
                    { "uri": "ubos://acme/Context/prod?commit=12", "relation": "DEPENDS_ON_CTX" },
                    { "uri": "ubos://acme/acme.Invoice/inv-1?commit=901", "relation": "DEPENDS_ON_DATA" } ],
  "decisions": [ { "source": "ubos://acme/Lifecycle/acme.invoice#has_lines", "code": "RULE.GUARD_FAILED",
                   "allow": false, "severity": "ERROR", "path": "/lines", "target": "ubos://acme/acme.Invoice/inv-1" } ],
  "trace": { "level": "SUMMARY", "truncated": false, "nodes": [ "…" ] } }
```

### Error codes (OBS)

| Code | Category | Meaning |
|---|---|---|
| `OBS.TRACE_EXPIRED` | NOT_FOUND | offloaded trace deleted by retention |
| `OBS.SEAL_MISMATCH` | CONFLICT | audit seal verification failed |
| `OBS.EXPORT_TOO_LARGE` | LIMIT | export range exceeds the limit (split it) |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-150 | Audit overhead in the flush (row + lineage + inline trace) | ≤ 15 % of flush time |
| NFR-OPS-150 | Telemetry overhead at default sampling | ≤ 3 % CPU |
| NFR-DUR-150 | Audit record for every committed process | 100 % (no commit without a record) |
| NFR-PRIV-150 | Personal data in telemetry | none (IDs and codes only) |

---

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-OBS-001…007 | Records per process kind; decision summary; trace offload and pruning; masking in evidence; lineage chains incl. TRIGGERED_BY; read auditing; immutability |
| REQ-OBS-010…013 | Export; retention; seal verification; redaction |
| REQ-OBS-015, 016 | Analytics queries; explanation input |
| REQ-OBS-020…023 | Span tree; structured logs; request log; metrics |

---

## 7. Implementation notes

- **Reference code:**
  - UB ProcessContext/ExecutionNode [UB:backend/src/main/java/com/ubos/core/context/ProcessContext.java].
  - UC ProcessContext and runner log capture [UC:src/kernel/process_context.rs#L9-L87], [UC:src/kernel/executor/worker/runner.rs#L66-L79].
  - LC transaction log files [LC:src/main/java/com/logicorum/service/TransactionLogService.java] and request audit [LC:src/main/java/com/logicorum/service/impl/R2dbcRequestAuditService.java].
  - FU metrics [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmMetricsService.java].
- **Divergences resolved:**
  - LC wrote audit JSON files next to the DB log. Here there is one audit store plus on-demand export.
  - UC's schema had no snapshot column for the ProcessContext. It gets `trace_summary`/`trace_ref`.
- **Crates:** `tracing`, `tracing-subscriber` (JSON), `opentelemetry-otlp`, `metrics` + `metrics-exporter-prometheus`, `zstd` for trace blobs.

## 8. Open questions

None. Deferred to P2: tamper-evidence seals (REQ-OBS-012), privacy redaction (REQ-OBS-013), and retention automation (REQ-OBS-011).
