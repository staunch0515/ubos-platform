---
id: CMP-04
title: "Comparison: Business Change Unit"
status: complete
phase: P2
depends_on: [CMP-01, CMP-03]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Comparison: Business Change Unit

## 1. Question

What is the unit in which business changes are made, validated, committed and audited? How are atomicity, concurrency, idempotency and provenance guaranteed?

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | Process groups commits | CON-UP-017, 028, 044, HL-UP-003 | Named process; script orchestrates several commits |
| FU | Process + approval as deferred commit | CON-FU-021, HL-FU-003 | Commit executed later by the approver |
| LC | Change sets staged by steps, one persistence step; idempotency keys | CON-LC-012, 022, 026, HL-LC-002, 006 | Unit of work with derived CREATE/UPDATE/DELETE, response replay |
| UB | Batch commit transaction with head CAS; drafts → publish | CON-UB-021, 022, 026, HL-UB-006 | All changes succeed or fail together; expected parent per entity |
| UC | Execution is commit; anchors + typed lineage | CON-UC-020, 021, 042, HL-UC-001, 002 | Job record, process log and DEPENDS_ON/OUTPUT map in one transaction |
| US | Unit-of-work session per execution | CON-US-020, 021, 025, HL-US-002 | Identity map, read-your-writes, flush on success |
| LS | ChangeSet + `commit_changes` only | CON-LS-019, 041 | Single write path; action Create/Update/Delete/Upsert |

## 3. Dimension matrix

| Dimension | UP | FU | LC | UB | UC | US | LS |
|---|---|---|---|---|---|---|---|
| Multi-entity atomic commit | ● | ● | ● | ● | ● (job) | ◐ (per entity) | ● |
| Staging before commit (UoW) | – | – | ● | draft branch | – | ● | – |
| Read-your-writes in logic | – | – | ● state | – | – | ● | – |
| Optimistic concurrency | – | – | ● handler | ● CAS | ● head check | loaded commit (unused) | – |
| Idempotency | – | – | ● keys + replay | – | job DONE check | – | – |
| Input provenance (code/env versions) | – | – | handler versions | – | ● anchors | ◐ | – |
| Deferred / approved commit | – | ● | – | publish | – | – | – |
| Explicit action type | – | RENAME | derived | – | – | – | ● |

## 4. Analysis

- **Consensus.** A named **process** is the audit unit: UP introduced it, and FU, LC, UB, UC and LS all have a process log plus a process → commit map. A **change set list** is the write unit (LC, LS, UB `EntityChange`).
- **Best partial solutions.**
  - LC and US give logic a staging area with read-your-writes and one final commit.
  - UB gives per-entity CAS on the expected parent.
  - LC gives idempotency with response replay.
  - UC gives provenance: the exact code and environment commits used, recorded as typed lineage.
  - FU turns approval into a deferred commit.
  - No single repository combines all of these.
- **Gaps.** US flushes entity by entity, each its own transaction, and its optimistic check is not implemented. UC commits the job and the business writes separately in US-style flows.

## 5. Verdicts

### VRD-04-01 — Process = unit of business change and audit (consensus, best-of UP generalised)
- **Decision:** consensus.
- **Consequences:**
  - Every write happens inside a Process with id, name (action or logic URI), operator, app, tenant, workspace/context, start/end and status.
  - The process log row and all its commits are written in one transaction.

### VRD-04-02 — Unit-of-work session with staged change sets, single atomic flush (fusion US + LC + LS)
- **Decision:** fusion.
- **Consequences:**
  - Logic reads through a session (identity map, merged inherited view) and stages changes.
  - At the end, all staged ChangeSets are committed in one transaction through `commit_changes`, the only write path.
  - Script failure means nothing is written.
  - ChangeSet = `{locator, payload (own data), action: CREATE | UPDATE | DELETE | UPSERT | RENAME | COPY, branch?, expected_head?}`.

### VRD-04-03 — Optimistic concurrency per entity and branch (best-of UB)
- **Decision:** best-of UB.
- **Consequences:**
  - Each staged change carries the head or height it was based on (captured at load).
  - A mismatch aborts the whole process with a CONFLICT status that lists the entities concerned.

### VRD-04-04 — Idempotency keys with response replay (best-of LC)
- **Decision:** best-of LC.
- **Consequences:**
  - Commands accept an idempotency key.
  - The first result is stored; retries return it.
  - Stale in-progress keys are recoverable.
  - Storage location follows VRD-01-06.

### VRD-04-05 — Provenance: processes record the versions they depend on (best-of UC)
- **Decision:** best-of UC.
- **Consequences:**
  - The process map has `relation_type` ∈ {OUTPUT, DEPENDS_ON_LOGIC, DEPENDS_ON_CONTEXT, DEPENDS_ON_TYPE, READ}.
  - Asynchronous work pins its logic and context commits at enqueue time.
  - Approvals (FU) are processes whose execution commit links back to the request entity (see CMP-10).
