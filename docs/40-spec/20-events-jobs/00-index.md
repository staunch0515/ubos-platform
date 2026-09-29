---
id: SPEC-20
title: "Events, Triggers and Jobs"
status: complete
phase: P4
depends_on: [SPEC-11, SPEC-15, SPEC-16, SPEC-17, SPEC-19, SPEC-22, SPEC-24, ADR-004]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Events, Triggers and Jobs

## 0. Chapter header

- **Scope:** This chapter covers:
  - the domain event catalogue and event shape;
  - the dispatcher, which relays the transactional outbox;
  - Hooks (event-triggered work), Crons (time-triggered work) and Webhooks (outbound HTTP);
  - Jobs (durable asynchronous work): lifecycle, claiming, retries, dead letters, cancellation, progress;
  - the worker pool with tenant fairness;
  - the scheduler and the sweeper;
  - notifications and mail;
  - the fan-out of events to protocol subscriptions.
- **MOD:** `EVT`
- **depends_on:** SPEC-11, SPEC-15, SPEC-16, SPEC-17, SPEC-19, SPEC-22, SPEC-24, ADR-004.
- **Terms used:** TERM-Outbox, TERM-Event, TERM-Hook, TERM-Cron, TERM-Job, TERM-Worker, TERM-Webhook, TERM-DeadLetter, TERM-Subscription, TERM-SystemIdentity, TERM-Emit.
- **Origin summary:**
  - Verdicts: VRD-11-01…05, VRD-06-05, VRD-15-04, ADR-004.
  - UP: event bus and scheduler as data (CON-UP-025, CON-UP-026).
  - UC: Cron and Hook entities, jobs, the idempotent worker, the producer/consumer split, child jobs (CON-UC-027, 013, 032, 003, 026).
  - LC: scheduled processes under a system role and the email outbox (CON-LC-027).
  - FU: signed webhooks (CON-FU-024).
  - US: lifecycle-phase event bus design (CON-US-007).
  - LS: reactor and client push (CON-LS-028, 005).

## Parts of this chapter

This chapter is split into files (WRITING-RULES R2.2). Read them in order.

| File | Sections |
|---|---|
| `00-index.md` (this file) | §0, §1, §6, §7, §8 |
| `01-model.md` | Model — §2 |
| `02-behavior.md` | Behavior — §3 |
| `03-interfaces-nfr.md` | Interfaces and non-functional — §4, §5 |

## 1. Concepts

```
 process flush ──(same DB transaction)──► rt_outbox rows: EVENT | EMIT | JOB_SIGNAL
                                              │
                                   dispatcher (leader per partition, claims with lease)
            ┌───────────────┬─────────────────┼────────────────────┬──────────────────────┐
            ▼               ▼                 ▼                    ▼                      ▼
     Hook matching    Webhook matching   event stream          emits (SPEC-19)       job signals
     → Jobs / DIRECT   → delivery Jobs    evt:{tenant}          ACTION/SIGNAL/NOTIFY  → ready queue
       processes                          → gateways → sessions  UI/MAIL
                                                                                         │
 scheduler (leader): Cron fires → Jobs; delayed jobs → ready queue; sweeper              ▼
                                                                          worker pool: claim (CAS) → attempt process
```

- **At-least-once.** Every outbox row is delivered at least once. Consumers are idempotent through event IDs and deduplication keys.
- **Ordering.** Events of the same entity are dispatched in commit order within a dispatcher partition. There is no global order.
- **Jobs are entities (ADR-004).** Queues and leases are only hints and locks in the runtime store. The truth is the Job's head.

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-EVT-001…003 | Events from every flush; dispatcher lag recovery; deduplication on re-delivery |
| REQ-EVT-010, 011 | Hook matching and firing in both modes; run_as governance |
| REQ-EVT-020…026 | Delays; exactly-once claiming; retry/backoff/DEAD; completion signals; tenant fairness; cancel/retry/progress; sweeper recovery |
| REQ-EVT-027 | Cron DST and failover |
| REQ-EVT-030, 031 | Signed delivery with retries; auto-deactivation |
| REQ-EVT-040…042 | Event stream fan-out; notifications; mail |

## 7. Implementation notes

- **Reference code:**
  - UC scheduler and event bus [UC:src/services/scheduler.rs#L12-L62], [UC:src/services/event_bus.rs#L12-L57], worker [UC:src/services/worker.rs#L9-L44], child jobs [UC:src/kernel/syscalls/task.rs#L15-L54].
  - UP event and scheduler services [UP:ubos-server/src/main/java/org/logrum/ubos/engine/service/LcmEventService.java], [UP:ubos-server/src/main/java/org/logrum/ubos/engine/service/LcmSchedulerService.java].
  - FU webhooks [FU:backend/src/main/java/org/logrum/ubos/kernel/service/WebhookService.java].
  - LC schedulers [LC:src/main/java/com/logicorum/scheduler/].
- **Divergences resolved:**
  - UC pushed events only from `sys_db_commit` and polled Crons every second with the user `scheduler`. Here events come from every flush, and fires run as the tenant system account.
  - FU webhooks were never dispatched. Here the dispatcher delivers them.
- **Crates:** `cron` (plus `chrono-tz` for time zones), Redis Streams (`XADD`/`XREAD`) for event streams, sorted sets for delayed jobs, `reqwest` with rustls for webhooks, `lettre` for SMTP.

## 8. Open questions

None. Deferred to P2: mobile push providers (SPEC-26), and webhook health auto-deactivation (REQ-EVT-031).
