---
id: CMP-07
title: "Comparison: Context Model"
status: complete
phase: P2
depends_on: [CMP-03, CMP-04]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Comparison: Context Model

## 1. Question

What context accompanies every operation — who, where (tenant, branch, environment), with which code and configuration versions, with which session memory — and how is it represented, persisted and traced?

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | `AuthContext{user, role, tenantId=branch}` in reactive context | CON-UP-018, 030 | Identity → branch |
| FU | Environments pointing to branch head or pinned commit; client tenant context | CON-FU-031, 056, HL-FU-006 | Release-oriented context |
| LC | Process context as blackboard (state map, change sets, status) | CON-LC-011 | Per-request execution state |
| UB | Context entity (slug → tenant + branch); ProcessContext execution tree; typed context keys | CON-UB-017, 020, 004, HL-UB-001, 005 | Environment as data + reified execution stack |
| UC | Workspace (sovereign tenant, branch, memory, env vars); ProcessContext (identity, workspace, request, provenance, trace); thread-local ExecutionContext; Ambassador (host vs sovereign) | CON-UC-015, 022, 023, 024, HL-UC-004 | Environment vs execution separation |
| US | UC + session in execution context | CON-US-020 | Transactional context |
| LS | ProcessContext{workspace, user, process_id, app_id}; per-connection session | CON-LS-003 | Protocol session |
| UW | Project context with tenant, branch, sprint | CON-UW-011 | UX context |

## 3. Dimension matrix

| Dimension | UP | FU | LC | UB | UC | LS |
|---|---|---|---|---|---|---|
| Environment as entity | policy | env table | – | ● Context entity | ● Workspace entity | Workspace |
| Pinned versions (commit) | – | ● env pin | – | – | ● anchors | – |
| Execution trace tree | – | – | messages | ● node tree | flat trace | – |
| Session memory across executions | – | – | – | – | ● memory (Redis) | – |
| Host vs sovereign tenant | – | – | – | – | ● | – |
| Typed context keys | – | – | state keys | ● `ContextKey<T>` | – | – |
| Persisted with audit | – | – | JSON files | `toFullJson` | design snapshot | – |

## 4. Analysis

- Two different objects are needed, and UC names the distinction ("Logrum is environment, Process is execution", CON-UC-024):
  - an **environment**: stable, versioned, selected by the user or client;
  - an **execution**: transient, created per request or job.
- **Environment.**
  - UB makes the environment an entity addressable in URIs (Context slug → tenant + branch).
  - UC adds memory, env vars and host/sovereign tenancy.
  - FU adds pinning to a commit, which gives releases.
- **Execution.**
  - UB's ProcessContext is the richest: a tree of handler frames with inputs, outputs, logs and failures.
  - UC adds provenance (anchors) and request identity.
  - LC's blackboard adds step-to-step state and pending change sets.
- **Memory.** UC's Ambassador memory is held only in Redis with a TTL and is not versioned. *Interpretation:* acceptable as a cache; durable memory should be a Workspace commit.

## 5. Verdicts

### VRD-07-01 — Context entity (environment) (fusion UB Context + UC Workspace + FU environments)
- **Decision:** fusion.
- **Consequences:** A `Context` entity has:
  - `slug`;
  - `sovereign_tenant` and optional `host_tenant`;
  - `branch` or `pinned_commit` (release mode);
  - `env_vars` and `memory` (committed on demand);
  - policy bindings.
- Context slugs are the URI authority (VRD-03-03).
- DEV/UAT/PROD are Context entities.

### VRD-07-02 — Execution context (process) (fusion UB ProcessContext + UC ProcessContext + LC blackboard)
- **Decision:** fusion.
- **Consequences:** Per process:
  - `trace_id`, identity (user, roles, app), context reference with the resolved tenant, branch and commit;
  - request, blackboard state, session (unit of work), provenance;
  - execution node tree (handler frames with timing, inputs, outputs, logs, status), result or error.
- The node tree is serialised into the audit record, truncated or offloaded by size (CMP-21).

### VRD-07-03 — Identity and tenant are implicit in execution, never passed by scripts (best-of UC/UP)
- **Decision:** best-of UC and UP.
- **Consequences:**
  - The syscall layer reads identity and tenant from the execution context (thread-local or task-local).
  - Background work runs as an explicit system identity (UP `SYSTEM`, LC `SYSTEM_INTERNAL_ROLE`).

### VRD-07-04 — Session memory: cached, optionally durable (fusion UC + NEW)
- **Decision:** fusion of UC with a NEW durability rule.
- **Consequences:**
  - Ambassador and agent memory lives in the runtime store for speed.
  - It is flushed to the Context entity (new version) when the session is marked durable or on explicit `memory.persist`.

### VRD-07-05 — Host vs sovereign tenant for foreign logic (best-of UC)
- **Decision:** best-of UC (CON-UC-024).
- **Consequences:**
  - Pure foreign logic runs under the host's data.
  - Effectful foreign logic writes under the sovereign tenant in the host.
  - Cross-tenant reads require an explicit grant (CMP-10, CMP-18).
