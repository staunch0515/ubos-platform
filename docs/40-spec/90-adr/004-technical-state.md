---
id: ADR-004
title: "ADR-004: Technical state — jobs as entities, idempotency keys in the runtime store"
status: complete
phase: P3
depends_on: [CMP-01, CMP-04, CMP-11, ADR-001]
sources: [UC, LC, UB, US]
---

# ADR-004: Technical state — jobs as entities, idempotency keys in the runtime store

- **Status:** accepted (2026-09-28). The user chose this split (Q-005).

## Context

- UC's rule "six tables only, never add tables" (CON-UC-040) keeps all state in one versioned model.
- LC keeps idempotency keys, request logs and an outbox in extra operational tables (CON-LC-043, CON-LC-026).
- UC keeps job queues and memory in Redis (CON-UC-043), while job records are entities (CON-UC-013, CON-UC-032).
- VRD-01-06 requires every piece of technical state outside the six tables to be declared, and asked (Q-005) where job records and idempotency keys belong.

## Options considered

### Option A — everything as entities
- **Pros:** one model; everything is versioned and auditable.
- **Cons:**
  - Idempotency keys are high-volume, short-lived and read on every retry.
  - Storing them as versions bloats history and the index.

### Option B — jobs as entities; idempotency keys in the runtime store with TTL
- **Pros:**
  - Jobs are business-visible (status, retries, results, audit), so they belong in the model.
  - Idempotency keys are pure technical deduplication with a natural expiry.
- **Cons:** a declared second store is needed, and its loss must be tolerated.

### Option C — technical tables for both
- **Pros:** simple SQL.
- **Cons:** breaks the six-table rule for jobs, which carry business meaning.

## Decision

Option B.

1. **Jobs are entities** of type `Job` (TERM-Job):
   - The state, attempts, schedule origin, input, result reference and error are stored as versions.
   - The runtime store holds only a queue signal (job ID and priority). Workers load the Job entity.
   - A lost queue signal is recovered by a sweeper that re-enqueues QUEUED jobs older than a threshold.
2. **Idempotency keys** live in the runtime store:
   - Key: `(tenant, principal, idempotency_key)`.
   - Value: request digest, status (`IN_PROGRESS`, `DONE`), response digest and response (or a reference to the process), `process_id`.
   - TTL: 24 hours by default, configurable per tenant (max 7 days).
   - A repeat with the same digest replays the response. A repeat with a different digest fails with `TX.IDEMPOTENCY_MISMATCH`.
   - After TTL expiry, the process log remains the durable evidence.
3. **Other runtime-store state** (all non-authoritative):
   - event delivery cursors, cache entries, rate-limit counters, protocol session data;
   - the session memory cache (durable memory is written as entity versions, VRD-07-04).
4. **Store selection:**
   - Enterprise edition: Redis 7.
   - Personal edition: in-process structures persisted in a separate SQLite file (`rt_*` tables, SPEC-02 REQ-CONV-062).
   - Both behind one `RuntimeStore` trait (SPEC-11).

## Consequences

- SPEC-11 defines the `RuntimeStore` trait and its key spaces.
- SPEC-15 defines the idempotency protocol.
- SPEC-20 defines the Job entity, the queue signal and the sweeper.
- Schedules, hooks, webhooks, subscriptions stored server-side, approvals and process instances are entities (SPEC-02 REQ-CONV-063).
- Loss of the runtime store may:
  - let a retried request run twice when its idempotency record is lost (for updates, CAS on the expected head rejects the second write);
  - delay jobs until the sweeper runs.
  It loses no committed business fact.

## Origin

Q-005, VRD-01-06, VRD-04-04, VRD-11-03, VRD-07-04, CON-UC-040, CON-UC-043, CON-LC-026, CON-LC-043, HL-UC-001.
