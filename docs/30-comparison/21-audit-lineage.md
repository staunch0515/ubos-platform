---
id: CMP-21
title: "Comparison: Audit, Lineage and Observability"
status: complete
phase: P2
depends_on: [CMP-04, CMP-07]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Comparison: Audit, Lineage and Observability

## 1. Question

How can every change and every execution be explained after the fact: who did what, with which code and inputs, what was produced, and why it was allowed or rejected? How is the system observed at runtime?

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | Process log + process-entity map; audit API/console replays a process as a timeline | CON-UP-017, HL-UP-003 | Audit replay |
| FU | Process audit queries; field-level blame (JSONB window functions); metrics from the version store | CON-FU-041, 043, 044, HL-FU-009, 014 | History analytics |
| LC | Process log records process and handler versions; request log (device, latency, status); JSON transaction files | CON-LC-041, 044, HL-LC-006, 007 | Production audit |
| UB | ProcessContext execution node tree (`toFullJson`); process log with `app_id` and `billing_commit_id` | CON-UB-020, 043, HL-UB-005, 008 | Holographic trace + billing |
| UC | Typed lineage `OUTPUT / DEPENDS_ON_CTX / DEPENDS_ON_FUNC`; anchors; execution logs on job; trace truncation; resource browser timeline | CON-UC-021, 022, 030, 042, HL-UC-001, 002 | Provenance |
| US | Execution logs; append-only verification scenarios | CON-US-043 | – |
| LS | Lineage per commit in the atomic write; tracing instrumentation everywhere | CON-LS-041, 064 | Instrumented kernel |

## 3. Dimension matrix

| Dimension | UP | FU | LC | UB | UC | LS |
|---|---|---|---|---|---|---|
| Process → commits map | ● | ● | ● | ● | ● typed | ● |
| Input provenance (code/context versions) | – | – | handler versions | – | ● | – |
| Execution trace tree | – | – | – | ● | flat logs | tracing |
| Field-level blame | – | ● | – | – | – | – |
| Request/latency log | – | – | ● | – | – | – |
| Billing / app attribution | – | – | app | ● | – | app |
| Replay / time travel UI | ● | ● | – | – | ● | – |
| Size control of traces | – | – | files | – | truncation | – |

## 4. Analysis

- **Complementary layers.** The corpus has the full stack of audit mechanisms, spread across repositories:
  - facts: commits and the process map (all repositories);
  - provenance (UC);
  - execution trace (UB);
  - operational request log (LC);
  - analytics, i.e. blame and metrics (FU);
  - billing attribution (UB).
- **Size of traces.** Two concerns come from UC's own audit (CON-UC-035) and LC's files. The first is size: truncate, or offload large traces to object storage. The second is atomicity: the audit row is written in the same transaction as the data (VRD-04-01).

## 5. Verdicts

### VRD-21-01 — Audit record per process (fusion UP + UB + LC)
- **Decision:** fusion.
- **Consequences:** The process log carries:
  - id, name (URI + version), operator (user/agent/system), app, tenant, context;
  - status, timing;
  - request id / idempotency key;
  - billing reference;
  - a summary of rule decisions;
  - trace location (inline JSON up to a limit, else an object-store key).

### VRD-21-02 — Typed lineage (best-of UC, extended)
- **Decision:** best-of UC, extended.
- **Consequences:** The process map relation types are OUTPUT, DEPENDS_ON_LOGIC, DEPENDS_ON_CONTEXT, DEPENDS_ON_TYPE, READ (optional sampling) and TRIGGERED_BY (parent process or event). Recursive lineage queries are available (VRD-12-03).

### VRD-21-03 — Execution trace tree (best-of UB)
- **Decision:** best-of UB.
- **Consequences:**
  - Nodes: handler/step/logic frames with inputs, outputs, logs, decisions, duration and status.
  - Stored with the process (VRD-07-02); capped by size and depth.
  - Used by the UI (time travel), by support staff and by AI explanations (VRD-16-06).

### VRD-21-04 — History analytics (best-of FU)
- **Decision:** best-of FU.
- **Consequences:** Blame per field path, branch status/diff, and activity metrics computed from the version store. These are exposed as queries.

### VRD-21-05 — Operational telemetry (fusion LC + LS)
- **Decision:** fusion.
- **Consequences:**
  - Structured tracing spans (OpenTelemetry-compatible) with trace id = process id.
  - A request log (client, latency, status).
  - Metrics: queue depth, job latency, cache hit rate, commit rate.
