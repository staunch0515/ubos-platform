---
id: UBS-SYS-DVM-02-03
title: DVM — Time, Ledger and Transaction Design
status: draft
phase: PH-1
depends_on: [UBS-SYS-DVM-02-02, UBS-STD-04, UBS-STD-06, UBS-STD-07]
---

# DVM — Time, Ledger and Transaction Design (DSN-DVM-3xx, DSN-DVM-4xx, DSN-DVM-5xx)

This file designs the crates `ubos-time`, `ubos-ledger` and `ubos-txn`. Reading order:
1. transaction time and timelines (301–312);
2. retroactive changes and impact (320–326);
3. rule bindings and future effect (330–334);
4. ledger storage and appends (401–409);
5. projections and balances (420–426);
6. periods, double entry and matching (440–446);
7. processes and the flush (501–508);
8. concurrency control (510–516);
9. signing (520–522);
10. idempotency, lineage and outbox (530–538);
11. deferred commits, bulk processes and sagas (550–566).

## Transaction time and timelines

### DSN-DVM-301 — Transaction time assignment
- **Statement:** Transaction time MUST be assigned by the sequencer, under the branch lock, as `max(host clock now, parent commit tx_time + 1 µs)` (STD-TIME-001). The host clock MUST come from CTR-002. If the host clock is behind the parent by more than the configured skew limit (default 500 ms), the flush MUST fail with `NOD.CLOCK_SKEW` instead of silently advancing time.
- **Rationale:** Monotonic transaction time per branch (FR-TIME-012) without trusting clients.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, NOD
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given a host clock that jumps back 100 ms, when committing, then transaction time still increases.
  2. Given a clock 2 s behind, when committing, then `NOD.CLOCK_SKEW` is returned, and an operator alert is raised.
- **Verification:** SIM, FAULT
- **Origin:** FR-TIME-011, FR-TIME-012, FR-TIME-013, STD-TIME-001

### DSN-DVM-302 — Timeline representation
- **Statement:** The timeline of a Definition object MUST be stored as the sequence of its versions, each with a valid period `[valid_from, valid_to)`. `temporal_index` MUST hold one row per `(object, valid segment, cseq interval)`. A GiST (PostgreSQL) or R*-tree (SQLite) index over `(valid_from, valid_to)` MUST support point and range lookups.
- **Rationale:** Bitemporal reads at index cost (NR-PERF-007).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given 1,000,000 objects with 10 segments each, when reading at random `(asof, cseq)` points, then the overhead versus a current read meets NR-PERF-007.
- **Verification:** BENCH, PROP
- **Origin:** FR-TIME-021, FR-TIME-025, STD-TIME-010, STD-TIME-011, NR-PERF-007

### DSN-DVM-303 — Timeline edit algebra
- **Statement:** `ubos-time` MUST implement the four timeline operations (`supersede_from`, `set_period`, `replace_future`, `end`) as pure functions `(timeline, op) → (new timeline, segment changes)`. Operations MUST NOT create overlapping segments (`TIME.OVERLAPPING_VALIDITY`). Business code MUST NOT write the reserved temporal fields directly (`TIME.RESERVED_FIELD`).
- **Rationale:** Correct, reviewable timeline edits (FR-TIME-022).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given random sequences of the four operations, when applied, then segments never overlap, and the timeline covers exactly the union of intended periods (property test).
  2. Given the timeline vectors, when applied, then results equal the vectors.
- **Verification:** PROP, CONF
- **Origin:** FR-TIME-022, STD-TIME-010, STD-TIME-011

### DSN-DVM-304 — Granularity and business time zone
- **Statement:** Valid-time values MUST be stored as UTC instants. Each class MAY declare a valid-time granularity (instant or date). For date granularity, dates MUST be converted to instants at midnight in the VAE's business time zone, using the IANA time-zone database version recorded in the commit.
- **Rationale:** Date-based business rules without ambiguity (FR-TIME-023).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a VAE in `America/New_York` and a date across a DST change, when stored and read, then the business date is preserved.
- **Verification:** CONF
- **Origin:** FR-TIME-023, STD-FND-024, NR-LOC-003

### DSN-DVM-305 — As-of resolution
- **Statement:** A read with `(cseq, asof)` MUST select the `temporal_index` row where `cseq_from ≤ cseq < cseq_to` and `valid_from ≤ asof < valid_to` (STD-TIME-012). If none exists, the result MUST be "not found at time" (`QRY.NOT_FOUND_AT_TIME`) rather than a generic not found.
- **Rationale:** Precise answers for historical questions.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given the as-of vectors, when resolved, then results equal the vectors, including not-found cases.
- **Verification:** CONF
- **Origin:** FR-TIME-031, STD-TIME-012

### DSN-DVM-306 — Consistent temporal context for references
- **Statement:** The read context MUST carry `(branch snapshot, asof)` into reference resolution, traversals and expressions, so that referenced objects are read at the same coordinates (STD-TIME-013). A reference MAY override `asof` only through an explicit selector.
- **Rationale:** FR-TIME-032.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a fund whose manager changed on 1 March, when read as of 15 February with the manager expanded, then the February manager is returned.
- **Verification:** CONF
- **Origin:** FR-TIME-032, STD-TIME-013

### DSN-DVM-307 — Range queries
- **Statement:** Temporal range queries (STD-TIME-014) MUST return, for each matching object, the list of segments intersecting the range, clipped to the range, in valid-time order.
- **Rationale:** FR-TIME-033.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-DataAnalyst
- **Acceptance:**
  1. Given a range covering three segments, when queried, then three clipped segments are returned in order.
- **Verification:** CONF
- **Origin:** FR-TIME-033, STD-TIME-014

### DSN-DVM-308 — Evaluation evidence
- **Statement:** Every read result MUST be able to produce evaluation evidence (STD-TIME-015): the commit ID, `asof`, the version IDs used, and the model hash. Reproducing a result from its evidence MUST give identical bytes.
- **Rationale:** Reproducible answers (FR-TIME-034).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-RegulatorExaminer
- **Acceptance:**
  1. Given evidence recorded one year earlier, when the read is replayed, then the bytes are identical.
- **Verification:** CONF, PROP
- **Origin:** FR-TIME-034, STD-TIME-015

### DSN-DVM-309 — Valid time on ledger entries
- **Statement:** Ledger entries MUST carry a valid time (the business date) and inherit the commit's transaction time. Bitemporal inclusion (STD-TIME-020) MUST be decided by `(entry.valid_at ≤ asof) ∧ (entry.cseq ≤ cseq)`.
- **Rationale:** FR-TIME-024.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a backdated posting committed today, when reading the balance as of last month at yesterday's commit, then the posting is excluded; at today's commit it is included.
- **Verification:** CONF, PROP
- **Origin:** FR-TIME-024, STD-TIME-020

## Retroactive changes and impact

### DSN-DVM-320 — Retroactivity detection
- **Statement:** The flush MUST mark every change whose valid time starts before the commit's transaction time (minus a configurable grace of 0 by default) as retroactive and record the earliest affected valid instant per object. Retroactive changes to classes whose policy requires review MUST require a change set (STD-TIME-030).
- **Rationale:** Retroactive changes are recognised and governed (FR-TIME-041).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given a backdated price change on `main` for a review-required class, when flushed directly, then it is rejected with `VER.CHANGE_SET_REQUIRED`.
- **Verification:** CONF
- **Origin:** FR-TIME-041, STD-TIME-030

### DSN-DVM-321 — Impact set computation
- **Statement:** The impact set of a retroactive change (STD-TIME-031) MUST be computed by a breadth-first walk of `derivation_edge` from the changed versions, restricted to derived results whose evaluation valid time is at or after the earliest affected instant. The walk MUST be bounded by a configurable depth and size and MUST report truncation.
- **Rationale:** FR-TIME-042.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a price change affecting 3 NAVs and 12 investor allocations, when the impact set is computed, then all 15 are listed with paths.
- **Verification:** CONF, PROP
- **Origin:** FR-TIME-042, STD-TIME-031

### DSN-DVM-322 — Restatement on a correction branch
- **Statement:** The DVM MUST provide `restate(impact set)` that creates a correction branch, re-runs the producing processes of the impacted results with their recorded inputs replaced by the corrected ones, and records the new results with their original valid times (FR-TIME-044).
- **Rationale:** FR-TIME-043.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given the impact set above, when restated, then the correction branch contains 15 new versions with unchanged valid times, and the diff to main shows only these.
- **Verification:** SCN, CONF
- **Origin:** FR-TIME-043, FR-TIME-044

### DSN-DVM-323 — Compensation proposals
- **Statement:** When restatement changes values that were posted to ledgers, the DVM MUST compute compensation proposals (adjustment entries per ledger key, STD-KIND-023) as the difference between original and restated postings, dated per policy (original date or current open period).
- **Rationale:** FR-TIME-045.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a restated fee that changes by +120, when proposals are computed, then one adjustment of +120 on the right ledger key is proposed.
- **Verification:** CONF
- **Origin:** FR-TIME-045, STD-KIND-023

### DSN-DVM-324 — Upcoming changes
- **Statement:** The DVM MUST answer "what becomes effective between t1 and t2" from `temporal_index` by a range scan on `valid_from`.
- **Rationale:** FR-TIME-053.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundOperationsManager
- **Acceptance:**
  1. Given 10 future-dated changes, when listing the next 30 days, then those in range are returned in order.
- **Verification:** CONF
- **Origin:** FR-TIME-053

### DSN-DVM-325 — Became-effective emission
- **Statement:** The DVM MUST expose `due_effective(t_from, t_to)` returning segments whose `valid_from` falls in the interval. The host (NOD) MUST call it on a schedule and emit became-effective events through the outbox (STD-TIME-050). No job is needed for the state itself to take effect (FR-TIME-051).
- **Rationale:** Future changes take effect by reading, events are best-effort notifications with a declared latency.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, NOD
- **Personas:** PER-FundOperationsManager
- **Acceptance:**
  1. Given a future change due at 09:00, when no scheduler runs, then reads after 09:00 still return the new value.
  2. Given the scheduler running, when 09:00 passes, then the event is emitted within NR-PERF-012.
- **Verification:** CONF, BENCH
- **Origin:** FR-TIME-051, FR-TIME-052, STD-TIME-050, NR-PERF-012

### DSN-DVM-326 — Retroactivity formal model
- **Statement:** The timeline algebra, bitemporal inclusion and impact set MUST be modelled formally with the properties: no overlapping segments; past `(cseq, asof)` answers never change; impact set is a superset of results whose inputs changed.
- **Rationale:** Bitemporal correctness is subtle.
- **Priority:** Must · **Phase:** PH-0 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given the model with 3 objects and 5 operations, when checked, then no property is violated.
- **Verification:** FORM
- **Origin:** STD-TIME-011, STD-TIME-031, NR-DET-001

## Rule bindings and re-evaluation

### DSN-DVM-330 — Rule binding capture
- **Statement:** During a process, the runtime MUST record every sheet, decision, logic asset and model version used, as a `RuleBinding` (STD-TIME-040). The flush MUST store the binding set once per commit (deduplicated by hash) and reference it from each derived version.
- **Rationale:** FR-TIME-061, FR-TIME-062.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given a derived fee, when inspected, then the sheet, decision and logic versions used are listed.
- **Verification:** CONF
- **Origin:** FR-TIME-061, FR-TIME-062, STD-TIME-040

### DSN-DVM-331 — Re-evaluation modes
- **Statement:** Re-evaluation of a derived result MUST support the modes of STD-TIME-041: `as-recorded` (default; the original bindings), `as-of-valid-time` (bindings effective at the result's valid time) and `current` (current bindings). The mode used MUST be recorded on the new version.
- **Rationale:** Old data follows old logic by default (FR-TIME-064).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a fee rule changed in March, when a January fee is re-evaluated in the default mode, then the January rule is used.
- **Verification:** CONF
- **Origin:** FR-TIME-063, FR-TIME-064, STD-TIME-041

## Ledger storage and appends

### DSN-DVM-401 — Ledger entry table
- **Statement:** Ledger entries MUST be stored in `ledger_entry`, partitioned by tenant and keyed by `(branch, ledger class, ledger key, seq)`. Rows MUST be insert-only: the adapter MUST revoke UPDATE and DELETE on the table for the application role, and a trigger MUST reject them (`LEDG.IMMUTABLE_ENTRY`).
- **Rationale:** Append-only enforced in depth (STD-KIND-021).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given the application role, when an UPDATE on `ledger_entry` is attempted, then the database rejects it.
- **Verification:** SEC, CONF
- **Origin:** FR-LEDG-011, FR-LEDG-012, STD-KIND-020, STD-KIND-021

### DSN-DVM-402 — Gap-free sequence allocation
- **Statement:** Sequence numbers per ledger key MUST be allocated under the branch sequencer lock from a `ledger_sequence` counter row updated in the flush transaction. Because the flush is atomic, an aborted flush MUST leave no gap. Sequences on child branches MUST be local to the branch and reassigned on merge (DSN-DVM-244).
- **Rationale:** FR-LEDG-041, STD-KIND-022.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given 10,000 appends with 5% injected flush failures, when sequences are verified, then they are gap-free.
- **Verification:** FAULT, PROP
- **Origin:** FR-LEDG-041, FR-LEDG-042, STD-KIND-022

### DSN-DVM-403 — Append cost independent of history
- **Statement:** An append MUST touch only the counter row, the entry row, the projection delta rows and the tree path of the entry key. It MUST NOT read prior entries.
- **Rationale:** FR-LEDG-014.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given ledger keys with 10 and with 10,000,000 entries, when appending, then latencies differ by less than 10%.
- **Verification:** BENCH
- **Origin:** FR-LEDG-014, NR-SCAL-007

### DSN-DVM-404 — Ledger appends and concurrency
- **Statement:** Appends MUST NOT take part in object-level compare-and-set: two processes appending to the same key MUST both succeed, ordered by the sequencer (STD-TXN-024). Only balance invariants (DSN-DVM-424) can cause a conflict, and they MUST be re-checked in the flush against the latest balance.
- **Rationale:** High-contention accounts without false conflicts.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given 200 concurrent appends to one cash account, when flushed, then all succeed with consecutive sequences.
- **Verification:** SIM, BENCH
- **Origin:** STD-TXN-024, FR-LEDG-042

### DSN-DVM-405 — Reversal and adjustment
- **Statement:** `commit.reverse` MUST append an entry with negated amounts that references the original, and MUST record the reversal in a `ledger_reversal` unique index so that a second reversal fails with `LEDG.ALREADY_REVERSED`. `commit.adjust` MUST append a correcting entry referencing its target; a missing target MUST fail with `LEDG.CORRECTION_TARGET_MISSING`. Non-reversible entry types MUST fail with `LEDG.REVERSAL_NOT_REVERSIBLE`.
- **Rationale:** FR-LEDG-021, FR-LEDG-022, FR-LEDG-023.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given two concurrent reversals of the same entry, when flushed, then exactly one succeeds.
- **Verification:** CONF, SIM
- **Origin:** FR-LEDG-021, FR-LEDG-022, FR-LEDG-023, STD-KIND-023, STD-ISA-015

### DSN-DVM-406 — Correction chains
- **Statement:** The DVM MUST provide `correction_chain(entry)` returning the original entry and all reversals and adjustments that reference it, transitively, in sequence order.
- **Rationale:** FR-LEDG-024.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given an entry adjusted twice and then reversed, when the chain is read, then four entries are returned in order.
- **Verification:** CONF
- **Origin:** FR-LEDG-024

### DSN-DVM-407 — Entry correlation
- **Statement:** Entries MUST carry a correlation ID and the source process ID. The DVM MUST index entries by correlation ID for retrieval across ledgers.
- **Rationale:** FR-LEDG-013.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a subscription posting to 4 ledgers, when queried by correlation ID, then the 4 entries are returned.
- **Verification:** CONF
- **Origin:** FR-LEDG-013

### DSN-DVM-408 — Sequence verification
- **Statement:** The DVM MUST provide `verify_sequences(ledger, key range)` that checks gap-freeness, total order and the hash chain of entries per key, and reports violations.
- **Rationale:** FR-LEDG-043.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given an injected gap, when verified, then the key and missing number are reported.
- **Verification:** FAULT
- **Origin:** FR-LEDG-043

### DSN-DVM-409 — Multi-currency postings
- **Statement:** Entries MUST carry money values with currency (STD-FND-023). Balances MUST be kept per currency. Arithmetic across currencies MUST fail with `MODEL.CURRENCY_MISMATCH` unless an explicit conversion with a recorded rate reference is used.
- **Rationale:** FR-LEDG-053.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given USD and EUR postings on one key, when the balance is read, then two currency balances are returned.
- **Verification:** CONF
- **Origin:** FR-LEDG-053, STD-FND-023

## Projections and balances

### DSN-DVM-420 — Balance projection as deltas
- **Statement:** Declared balance projections (FR-LEDG-031) MUST be maintained in the flush as rows `(projection, key, valid_at, cseq, delta)` in `balance_delta`. Checkpoint rows `(projection, key, valid_at, cseq, balance)` MUST be written by a background task every N deltas per key (default 1,000). A balance read MUST sum deltas after the nearest checkpoint satisfying the bitemporal inclusion rule.
- **Rationale:** Bitemporal balances with bounded read cost (FR-LEDG-032).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given 10,000,000 entries on one key, when reading balances at random `(asof, cseq)` points, then p95 latency meets NR-PERF-001 plus 5 ms.
  2. Given random workloads, when balances are recomputed from entries, then they equal the projected balances.
- **Verification:** BENCH, PROP
- **Origin:** FR-LEDG-031, FR-LEDG-032, FR-LEDG-033, AR-016

### DSN-DVM-421 — Backdated entries and checkpoints
- **Statement:** A backdated delta (valid_at before a checkpoint's valid_at) MUST NOT modify existing checkpoints. Reads MUST include such deltas by their `(valid_at, cseq)` coordinates. The checkpoint task MUST create new checkpoints at later `cseq` that include them.
- **Rationale:** Checkpoints are immutable, derived data (AR-015).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given a backdated posting before a checkpoint, when reading as of a date after the posting at the latest commit, then it is included; at an earlier commit, it is not.
- **Verification:** PROP
- **Origin:** FR-LEDG-032, AR-015

### DSN-DVM-422 — Generic projections
- **Statement:** Besides balances, classes MAY declare projections as a fold of `(group key, aggregate: sum | count | min | max | last)` over entries or over Definition versions. The flush MUST maintain `sum` and `count` incrementally; `min`, `max` and `last` MUST be maintained incrementally for insert-only sources and recomputed per group otherwise.
- **Rationale:** Common aggregates stay cheap without an external engine.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given a count-by-status projection, when objects change status, then counts stay equal to a full recount.
- **Verification:** PROP
- **Origin:** FR-LEDG-031, AR-016

### DSN-DVM-423 — Projection rebuild
- **Statement:** Every projection MUST be rebuildable from its sources (`rebuild_projection(name, branch)`), writing into a shadow table and swapping atomically. The verifier (DSN-DVM-092) MUST cover projections.
- **Rationale:** Derived data is rebuildable (AR-015).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a corrupted projection, when rebuilt, then it equals the verifier's expected values, and reads were never served from the partial shadow table.
- **Verification:** FAULT
- **Origin:** AR-015, NR-DUR-005

### DSN-DVM-424 — Invariants over balances
- **Statement:** Invariants over balances (for example "cash ≥ 0", FR-LEDG-034) MUST be evaluated in the flush on the balances after the batch's deltas are applied. Violations MUST abort only the offending process (`RULE.INVARIANT_VIOLATED`).
- **Rationale:** Exact enforcement under concurrency.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a balance of 100 and two concurrent withdrawals of 60, when flushed, then exactly one succeeds.
- **Verification:** SIM, CONF
- **Origin:** FR-LEDG-034, STD-LGS-030

## Periods, double entry and matching

### DSN-DVM-440 — Balanced posting groups
- **Statement:** Entries appended in one process with the same posting-group ID MUST be checked in the flush for balance per currency (debits = credits, STD-KIND-024). An unbalanced group MUST fail with `LEDG.UNBALANCED`.
- **Rationale:** FR-LEDG-051.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a group off by 0.01, when flushed, then `LEDG.UNBALANCED` reports the difference.
- **Verification:** CONF
- **Origin:** FR-LEDG-051, STD-KIND-024

### DSN-DVM-441 — Chart of accounts
- **Statement:** Accounts MUST be Definition objects of a kernel base class with a hierarchy (parent account). Postings MUST reference leaf accounts. Roll-up balances MUST be computed by summing leaf projections over the account subtree (using a pre-order interval like DSN-DVM-107).
- **Rationale:** FR-LEDG-052.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a 4-level chart, when reading a top-level balance, then it equals the sum of its leaves.
- **Verification:** CONF, PROP
- **Origin:** FR-LEDG-052

### DSN-DVM-442 — Periods and locks
- **Statement:** Periods (STD-KIND-025) MUST be Definition objects with states open, soft-closed and closed. The flush MUST reject entries whose valid time falls in a closed period (`LEDG.PERIOD_CLOSED`) and entries in a soft-closed period without the override capability.
- **Rationale:** FR-LEDG-061.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a closed March, when posting a March entry, then it fails; an April entry succeeds.
- **Verification:** CONF
- **Origin:** FR-LEDG-061, STD-KIND-025

### DSN-DVM-443 — Close artefacts
- **Statement:** Closing a period MUST produce a close artefact: the balances per key at close, the commit ID, and the state root, sealed (STD-PROOF-055) as one object.
- **Rationale:** FR-LEDG-062.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given a closed period, when the artefact is verified, then its balances equal balances read at the close commit.
- **Verification:** CONF
- **Origin:** FR-LEDG-062, STD-PROOF-055

### DSN-DVM-444 — Governed reopen
- **Statement:** Reopening a closed period MUST require a change set approved per the reopen decision, and MUST record the reason and the superseded close artefact.
- **Rationale:** FR-LEDG-063.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given a reopen without approval, when flushed, then it fails with `VER.CHANGE_SET_REQUIRED`.
- **Verification:** CONF
- **Origin:** FR-LEDG-063

### DSN-DVM-445 — Matching engine
- **Statement:** The DVM MUST provide a deterministic matching operator: given two entry sets and a matching rule (keys, tolerances, date windows, many-to-one allowed), it produces match records and breaks. Match records MUST be immutable objects (FR-LEDG-073); un-matching MUST create a new record that supersedes the old one.
- **Rationale:** FR-LEDG-071, FR-LEDG-072, FR-LEDG-073.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given the matching vectors, when matched twice, then results are identical, and breaks equal the vectors.
- **Verification:** CONF, PROP
- **Origin:** FR-LEDG-071, FR-LEDG-072, FR-LEDG-073

### DSN-DVM-446 — Idempotent statement ingestion
- **Statement:** Statement lines MUST be ingested with an idempotency key derived from `(source, account, statement ID, line number, content hash)`, so a re-delivered statement creates no duplicates.
- **Rationale:** FR-LEDG-074.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, BRG
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given the same bank statement ingested three times, when counted, then each line exists once.
- **Verification:** CONF
- **Origin:** FR-LEDG-074, STD-TXN-040

## Processes and the flush

### DSN-DVM-501 — Process object and lifecycle
- **Statement:** A process MUST be created by the host API with a context (STD-CTX-001) and MUST pass through states: created, running, staged, flushing, committed, aborted, or deferred (DSN-DVM-550). The in-memory process holds a snapshot map, a staging area, a read set, a journal and a metering record.
- **Rationale:** STD-TXN-001.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given every state transition, when exercised, then only the transitions of the state machine in UBS-SYS-DVM-05 are possible.
- **Verification:** CONF, PROP
- **Origin:** FR-TXN-011, STD-TXN-001

### DSN-DVM-502 — Staging area
- **Statement:** The staging area MUST hold, per object, the base version and the pending new state; per ledger, the pending entries in append order; and the pending events and effects. Reads within the process MUST see its own staged writes (read-your-writes) layered over the snapshot.
- **Rationale:** Unit of work (FR-TXN-012, FR-TXN-014).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given a port that updates an object and calls another port that reads it, when run, then the inner port sees the update.
- **Verification:** CONF
- **Origin:** FR-TXN-012, FR-TXN-014, STD-TXN-002

### DSN-DVM-503 — Flush pipeline
- **Statement:** The flush MUST execute these steps:
  1. pre-lock: validation plans, sheet invariants that do not depend on concurrent state, chunk upload;
  2. enqueue to the branch sequencer;
  3. under the lock: head CAS, base and read-set validation, unique keys, balance invariants, ledger sequences, tree update, commit construction and signature;
  4. one storage transaction writing chunks' descriptors, versions, entries, indexes, projections, commit, outbox rows and idempotency records;
  5. release the lock and publish the new head to caches.

  Only step 4 writes authoritative state (AR-004).
- **Rationale:** Minimal time under lock, one atomic write.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given a crash injected at every step boundary, when recovered, then either the complete commit or nothing is visible (FR-TXN-013).
- **Verification:** FAULT, SIM
- **Origin:** FR-TXN-012, FR-TXN-013, STD-TXN-003, STD-TXN-004, AR-004

### DSN-DVM-504 — Commit latency budget
- **Statement:** For a single-object process on ENV-REF-SERVER, the flush budget MUST be allocated as: pre-lock ≤ 2 ms, lock wait ≤ 2 ms (group commit window), under-lock work ≤ 1 ms, storage commit ≤ 5 ms, giving NR-PERF-002. Each step MUST emit a span with its duration.
- **Rationale:** A measurable budget makes regressions visible.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given the benchmark, when run, then per-step p95 values meet the budget.
- **Verification:** BENCH
- **Origin:** NR-PERF-002, NR-OBS-001

### DSN-DVM-505 — Empty and read-only processes
- **Statement:** A process with no staged writes MUST complete without a commit. Read-only processes MUST NOT take the sequencer lock.
- **Rationale:** Avoid empty commits and lock pressure.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given 1,000 read-only processes, when run, then no commit and no lock acquisition occurs.
- **Verification:** CONF
- **Origin:** FR-VER-013

### DSN-DVM-506 — Multi-branch processes are forbidden
- **Statement:** A process MUST write to exactly one branch. Writes to another branch MUST be done by a separate process linked by a saga (DSN-DVM-564) or by merge.
- **Rationale:** Atomicity stays per branch.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given a process that writes to two branches, when flushed, then it fails before any write.
- **Verification:** CONF
- **Origin:** STD-TXN-003

### DSN-DVM-507 — Process limits
- **Statement:** The DVM MUST enforce per-profile limits on staged objects (default 10,000), staged bytes (default 64 MB) and ledger entries (default 50,000) per process. Larger work MUST use bulk processes (DSN-DVM-560).
- **Rationale:** Predictable memory and lock duration.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given a process staging 10,001 objects under default limits, when staging, then `LOGIC.LIMIT_EXCEEDED` names the limit.
- **Verification:** CONF
- **Origin:** STD-CTX-004, STD-TXN-070

### DSN-DVM-508 — Crash recovery
- **Statement:** On start, the DVM MUST require no recovery step beyond the storage engine's own: a commit is visible if and only if its storage transaction committed. In-flight processes are lost and MUST be retried by their callers or idempotently re-delivered.
- **Rationale:** Simple, provable recovery (NR-DUR-001).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a process kill (SIGKILL) during 1,000 random flushes, when restarted, then every acknowledged commit is present, and no partial commit exists.
- **Verification:** FAULT
- **Origin:** NR-DUR-001, FR-TXN-013

## Concurrency control

### DSN-DVM-510 — Base validation
- **Statement:** Every staged update MUST name its base version (STD-TXN-020). Under the lock, the flush MUST compare each base with the head version of the object on the branch. Mismatches MUST abort the process with `TXN.HEAD_MOVED` listing all mismatching objects (FR-TXN-024).
- **Rationale:** Object-level compare-and-set (STD-TXN-021).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given two processes updating the same object from the same base, when flushed, then one succeeds, and the other fails naming the object.
- **Verification:** SIM, CONF
- **Origin:** FR-TXN-021, FR-TXN-022, FR-TXN-024, STD-TXN-020, STD-TXN-021

### DSN-DVM-511 — Serializable read-set validation
- **Statement:** Processes running in the serializable mode (STD-TXN-022) MUST record their read set: point reads as `(object, version)` and query reads as `(predicate hash, index ranges, result versions)`. Under the lock, the flush MUST detect writes committed after the snapshot that intersect the read set (by object or by index range) and abort with `TXN.READ_SET_CHANGED`.
- **Rationale:** Serializable business logic where needed (FR-TXN-023).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given the write-skew scenario of two on-call doctors, when run serializably, then one process aborts.
  2. Given 10,000 random concurrent histories, when checked by an Elle-style checker, then no serializability anomaly is found.
- **Verification:** SIM, PROP
- **Origin:** FR-TXN-023, STD-TXN-022

### DSN-DVM-512 — Committed read and write summaries
- **Statement:** Each commit MUST store a compact write summary (object IDs and touched index ranges) in `commit_write_summary`, retained for the maximum process age (default 10 minutes), to validate read sets without scanning versions. Read sets of committed processes MUST be stored for lineage when the class requests it (FR-TXN-042).
- **Rationale:** Efficient validation and lineage.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given 1,000 commits since a snapshot, when validating a read set of 100 items, then validation takes under 1 ms.
- **Verification:** BENCH
- **Origin:** FR-TXN-042, FR-TXN-043, STD-TXN-022

### DSN-DVM-513 — Automatic retry
- **Statement:** Processes declared re-executable (STD-TXN-023) MUST be retried automatically after `TXN.HEAD_MOVED` or `TXN.READ_SET_CHANGED` with a new snapshot, up to 5 attempts with jittered back-off (5, 10, 20, 40 ms). The journal of each attempt MUST be kept until the process ends. Pre-commit external calls MUST prevent automatic retry unless idempotent.
- **Rationale:** FR-TXN-025.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given 50 concurrent increments of a counter by re-executable processes, when run, then the final value is 50.
- **Verification:** SIM
- **Origin:** FR-TXN-025, STD-TXN-023

### DSN-DVM-514 — Hot-object mitigation
- **Statement:** The DVM MUST expose per-object conflict counters. Objects whose conflict rate exceeds a threshold MUST be reported with a recommendation (use a ledger, a sum-of-deltas field, or a serial queue).
- **Rationale:** Visible contention guides modelling.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** DVM, NOD
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given a counter object updated by 100 concurrent processes, when observed, then it appears in the hot-object report.
- **Verification:** BENCH
- **Origin:** NR-OBS-002

### DSN-DVM-515 — Concurrency formal model
- **Statement:** Base validation, read-set validation, head CAS and group commit MUST be modelled formally with the properties: linear history per branch; no lost update; serializability for serializable processes; dense `cseq`.
- **Rationale:** Concurrency is verified before implementation (AR-033).
- **Priority:** Must · **Phase:** PH-0 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given the model with 3 processes, 2 nodes and 3 objects, when checked, then no property is violated.
- **Verification:** FORM
- **Origin:** STD-TXN-021, STD-TXN-022, AR-033

### DSN-DVM-516 — Deterministic simulation harness
- **Statement:** `ubos-txn` MUST be testable under a deterministic simulator in which the clock, the scheduler, storage latency and crashes are controlled by a seed. Every failing seed MUST be reproducible.
- **Rationale:** Concurrency bugs are found and replayed deterministically.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, FRG
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given a seed that exposed a bug, when re-run, then the same interleaving occurs.
- **Verification:** SIM
- **Origin:** NR-MAINT-002, AR-032

## Signing

### DSN-DVM-520 — Commit signing
- **Statement:** Every commit MUST be signed with Ed25519 (STD-PROOF-010) by the author key through the host signing service (CTR-002). For custodial keys, the host MAY cache the unwrapped key in node memory so signing takes under 100 µs; for HSM keys, the sequencer MUST sign outside the lock with a pre-computed commit body and verify the head again before writing.
- **Rationale:** Signed commits without breaking the latency budget.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, NOD
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given every commit in a test run, when verified with the key history, then all signatures are valid.
- **Verification:** CONF, BENCH
- **Origin:** STD-PROOF-010, NR-SEC-006

### DSN-DVM-521 — Key history verification
- **Statement:** Signature verification MUST use the key valid at the commit's transaction time, from the principal's key history (STD-PROOF-011). A signature by a key revoked before the commit's transaction time MUST fail with `PROOF.SIGNATURE_INVALID`.
- **Rationale:** Signatures stay verifiable after rotation.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given a key rotated in March, when verifying a February commit, then the old key is used, and verification succeeds.
- **Verification:** CONF
- **Origin:** STD-PROOF-011

### DSN-DVM-522 — Co-signatures
- **Statement:** A commit MAY carry additional signatures (approvers of a change set, parties of a shared BPU). They MUST be over the same commit ID and stored in a separate `commit_signature` table so they can be added after the commit without changing its ID.
- **Rationale:** Multi-party attestations.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-Approver
- **Acceptance:**
  1. Given a merge commit, when two approvers co-sign, then both signatures verify, and the commit ID is unchanged.
- **Verification:** CONF
- **Origin:** STD-PROOF-010, STD-SYNC-011

## Idempotency, lineage and outbox

### DSN-DVM-530 — Idempotency records
- **Statement:** A process started with an idempotency key MUST record `(tenant, scope, key) → (request hash, outcome, commit ID)` in the flush. A second request with the same key and same hash MUST return the recorded outcome without executing; with a different hash it MUST fail with `TXN.IDEMPOTENCY_MISMATCH` (STD-TXN-040).
- **Rationale:** FR-TXN-031.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-IsvDeveloper
- **Acceptance:**
  1. Given 10 concurrent requests with one key, when processed, then exactly one executes, and all return the same outcome.
- **Verification:** SIM, CONF
- **Origin:** FR-TXN-031, STD-TXN-040, AR-010

### DSN-DVM-531 — Idempotency retention
- **Statement:** Idempotency records MUST be kept for the configured retention (default 7 days, FR-TXN-032) and then removed by a sweep. Requests after expiry MUST be treated as new.
- **Rationale:** Bounded storage.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a record older than retention, when swept, then it is removed.
- **Verification:** CONF
- **Origin:** FR-TXN-032

### DSN-DVM-532 — Trigger lineage
- **Statement:** Every process MUST record its trigger (user request, event, job, flow step, agent task, parent process) and its causality chain ID (STD-TXN-031). The DVM MUST answer "why did this change happen" by walking triggers and derivations (FR-TXN-043).
- **Rationale:** FR-TXN-041, FR-TXN-043.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given a posting created by a flow started by an event from a user commit, when asked why, then the chain back to the user commit is returned.
- **Verification:** CONF
- **Origin:** FR-TXN-041, FR-TXN-043, STD-TXN-031

### DSN-DVM-533 — Derivation records
- **Statement:** Derived writes MUST record their input versions (STD-TXN-030) as rows in `derivation_edge` `(output version, input version, rule binding)`. Inputs MUST be taken from the read set filtered to the reads that the deriving port declared as inputs, or all reads when undeclared.
- **Rationale:** FR-TXN-042, impact analysis (DSN-DVM-321).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a NAV derived from 20 prices, when inspected, then 20 input edges exist.
- **Verification:** CONF
- **Origin:** FR-TXN-042, STD-TXN-030

### DSN-DVM-534 — Outbox
- **Statement:** Events and external effects (notifications, connector calls declared after-commit) MUST be written as rows in `outbox` in the flush transaction (STD-TXN-050). The DVM MUST expose `outbox_claim(n, lease)` and `outbox_ack(ids)` for the host, which releases them at least once (FR-TXN-052).
- **Rationale:** Effects happen if and only if the commit happens.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, NOD
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given an aborted process that emitted events, when inspected, then no outbox row exists.
  2. Given a host crash after claiming, when the lease expires, then the rows are claimed again.
- **Verification:** FAULT, CONF
- **Origin:** FR-TXN-051, FR-TXN-052, STD-TXN-050, NR-DUR-006

### DSN-DVM-535 — Outbox ordering
- **Statement:** Outbox rows MUST carry `(branch, cseq, index within commit)`, and the host MUST release events of one aggregate key in that order.
- **Rationale:** Consumers see events in commit order per key.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, NOD
- **Personas:** PER-IsvDeveloper
- **Acceptance:**
  1. Given 100 commits on one object, when consumed, then events arrive in commit order.
- **Verification:** SIM
- **Origin:** FR-TXN-052

### DSN-DVM-536 — Pre-commit external calls
- **Statement:** External calls made before commit (`call.connector` in pre-commit mode, STD-TXN-051) MUST be journaled with request and response. If the process aborts, a declared compensation MUST be enqueued through a separate always-committed compensation record.
- **Rationale:** FR-TXN-053.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, NOD
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given a pre-commit reservation followed by an abort, when inspected, then a compensation record exists and is released.
- **Verification:** FAULT
- **Origin:** FR-TXN-053, STD-TXN-051

### DSN-DVM-537 — Channel and client attribution
- **Statement:** Every process MUST record the channel (UI, API, SDK, agent, job, sync) and client identity from the context, stored in the commit metadata.
- **Rationale:** FR-TXN-044.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given commits from UI and API, when listed, then each shows its channel and client.
- **Verification:** CONF
- **Origin:** FR-TXN-044

### DSN-DVM-538 — Idempotent handlers
- **Statement:** Event handlers MUST be invoked with an idempotency key derived from `(handler, event ID)`, so re-delivered events do not create duplicate effects (FR-TXN-033).
- **Rationale:** At-least-once delivery with exactly-once effect.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, NOD
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given one event delivered three times, when handled, then one commit results.
- **Verification:** SIM
- **Origin:** FR-TXN-033

## Deferred commits, bulk processes and sagas

### DSN-DVM-550 — Deferred commits
- **Statement:** When a decision returns "requires approval" (STD-TXN-060), the flush MUST, instead of committing to the target, persist the staged changes as a `PendingChange` object on the target branch (in a System-kind area), with the snapshot, the base versions and a preview hash. The process ends in state deferred.
- **Rationale:** Approvals without long-running locks.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-Approver
- **Acceptance:**
  1. Given a payment above the approval threshold, when submitted, then a pending change exists, and the target state is unchanged.
- **Verification:** CONF
- **Origin:** STD-TXN-060, FR-VER-031

### DSN-DVM-551 — Applying a pending change
- **Statement:** Approval of a pending change MUST start a process that re-validates bases against the current head. If bases are unchanged, it commits the staged changes with the approval recorded. If bases changed, it MUST re-run the original process (when re-executable) and require re-approval when the preview hash differs.
- **Rationale:** What was approved is what is committed.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-Approver
- **Acceptance:**
  1. Given a pending change whose base object changed, when approved, then re-approval is requested with the new preview.
- **Verification:** CONF, SIM
- **Origin:** STD-TXN-060, STD-VER-021

### DSN-DVM-552 — Pending-change expiry
- **Statement:** Pending changes MUST expire after a configured time (default 30 days) and move to state expired, emitting an event.
- **Rationale:** No indefinitely open work.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-Approver
- **Acceptance:**
  1. Given a pending change older than its expiry, when the sweep runs, then it is expired, and an event is emitted.
- **Verification:** CONF
- **Origin:** STD-TXN-060

### DSN-DVM-560 — Bulk processes
- **Statement:** A bulk process (STD-TXN-070) MUST iterate a query result in batches (default 1,000 objects), commit one commit per batch with a common bulk ID, and persist a checkpoint (last key processed) in each commit. On restart, it MUST resume from the checkpoint. It MUST be cancellable between batches.
- **Rationale:** FR-TXN-015.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundOperationsManager
- **Acceptance:**
  1. Given 1,000,000 objects and a crash after 400 batches, when resumed, then processing continues at batch 401, and no object is processed twice.
- **Verification:** FAULT
- **Origin:** FR-TXN-015, STD-TXN-070

### DSN-DVM-561 — Bulk throughput and fairness
- **Statement:** Bulk processes MUST run in the batch profile with a lower sequencer priority than interactive processes, so interactive p95 commit latency on the same branch rises by no more than 50%.
- **Rationale:** Batch work does not starve users.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, NOD
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a bulk process and interactive load on one branch, when benchmarked, then interactive latency meets the bound.
- **Verification:** BENCH
- **Origin:** NR-PERF-022, STD-CTX-004

### DSN-DVM-564 — Sagas
- **Statement:** A saga (STD-TXN-080) MUST be a System-kind object with steps, each a process on a possibly different branch or VAE, with a compensation per step. The saga coordinator MUST run in the host, persisting step state in the saga object through processes, so it survives restarts.
- **Rationale:** FR-TXN-061, FR-TXN-062.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, NOD
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given a 3-step saga that fails at step 3, when run, then compensations for steps 2 and 1 run in reverse order.
- **Verification:** FAULT, CONF
- **Origin:** FR-TXN-061, FR-TXN-062, STD-TXN-080

### DSN-DVM-565 — Saga timeouts and monitoring
- **Statement:** Each saga step MUST have a timeout; on timeout the coordinator MUST compensate or escalate per declaration. Sagas MUST be queryable by state, age and failing step (FR-TXN-064).
- **Rationale:** FR-TXN-063, FR-TXN-064.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, NOD
- **Personas:** PER-FundOperationsManager
- **Acceptance:**
  1. Given a step that never answers, when its timeout passes, then the declared action runs, and the saga appears in the "stuck" list until resolved.
- **Verification:** FAULT
- **Origin:** FR-TXN-063, FR-TXN-064

### DSN-DVM-566 — Saga formal model
- **Statement:** The saga protocol MUST be modelled formally with properties: every started saga ends completed or compensated; compensations run at most once per step.
- **Rationale:** Distributed failure handling is verified before implementation.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given the model with 3 steps and crash injection, when checked, then no property is violated.
- **Verification:** FORM
- **Origin:** STD-TXN-080
