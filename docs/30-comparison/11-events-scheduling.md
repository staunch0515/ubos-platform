---
id: CMP-11
title: "Comparison: Events, Scheduling, Async Jobs, Webhooks"
status: complete
phase: P2
depends_on: [CMP-04, CMP-05, CMP-06]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Comparison: Events, Scheduling, Async Jobs, Webhooks

## 1. Question

How does the system react to data changes and time, run work asynchronously, and notify external systems and users, with the wiring expressed as data?

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | LISTENER entities `{trigger}`; CRON entities `{cron, target, params}` rebuilt from master | CON-UP-025, 026, HL-UP-005 | Events and time as data |
| FU | WEBHOOK entities (event + type filter, HMAC, retry) — dispatch not wired | CON-FU-024, HL-FU-011 | Outbound integration |
| LC | Schedulers invoke processes under system role; email outbox | CON-LC-027 | Outbox pattern |
| UB | Committed-event publisher SPI; async embedding on commit (design) | CON-UB-002, 065 | – |
| UC | Job entity + Redis queue + worker (202); Cron and Hook entities; `entity_committed` events on Redis; child jobs; status polling | CON-UC-003, 013, 026, 027, 032, HL-UC-001, 005 | Durable async execution |
| US | UC; `sys_db_commit` no longer emits events (session) | CON-US-021 | Regression noted |
| LS | Reactor mechanism (design); `Subscribe` op; server push to `client://current`; emits | CON-LS-002, 005, 028 | Push to clients |
| UW | Inbox of issues/notifications/approvals | CON-UW-013 | UX consumer |

## 3. Dimension matrix

| Dimension | UP | FU | LC | UC | LS |
|---|---|---|---|---|---|
| Event subscriptions as entities | ● | ● (webhooks) | – | ● Hook | design |
| Schedules as entities | ● | – | code | ● Cron | – |
| Durable async jobs with status | – | – | – | ● | – |
| Transactional outbox | – | – | ● (email) | – (dual write noted) | – |
| Retries / backoff | – | ● | status | – | – |
| Push to clients | – | – | – | cards via logs | ● |
| External webhooks signed | – | ● | – | – | – |
| Provenance of triggered work | – | – | system role | ● anchors + default context | – |

## 4. Analysis

- **Convergent design:** subscriptions and schedules are entities (UP, UC), triggered work runs as ordinary logic or processes (LC, UC), and triggered work has a system identity.
- **Durability.** UC's job capsule (PENDING → RUNNING → DONE/ERROR, idempotent, anchored) is the only durable async model. Its queue is written after the database commit (dual write); UC's own audit recommends a transactional outbox, which LC already uses for email.
- **Emission gap.** Events must be emitted from the single commit path. US lost this when writes moved into the session, and FU's webhooks are never dispatched.
- **Client push.** LS's streamed ops to `client://current` and UC's cards (via logs and polling) are two versions of the same need. The protocol (CMP-15) should provide subscriptions.

## 5. Verdicts

### VRD-11-01 — Domain events emitted by the commit path via a transactional outbox (fusion LC + UC + NEW)
- **Decision:** fusion of LC and UC, plus NEW wiring.
- **Consequences:**
  - Every committed process writes event records (`entity.created | updated | deleted | renamed`, `process.completed`, custom emits) into an outbox in the same transaction.
  - A dispatcher delivers them to subscribers, with at-least-once delivery and idempotent consumers.

### VRD-11-02 — Subscriptions and schedules as entities (fusion UP + UC + FU)
- **Decision:** fusion.
- **Consequences:**
  - `Hook` = `{on_event, filter (type, branch, JSONPath), target (logic/action/pipeline URI), context?}`.
  - `Cron` = `{schedule, target, params, context?}`.
  - `Webhook` = `{event filter, url, secret, timeout, retry}`, HMAC-signed.
  - All are versioned. The scheduler and dispatcher rebuild from heads on change.

### VRD-11-03 — Durable jobs for asynchronous work (best-of UC)
- **Decision:** best-of UC.
- **Consequences:**
  - A job entity carries status, args, result, logs, parent job and anchors.
  - Invoke returns 202 with the job URI.
  - Workers run concurrently with bounded parallelism (fixing UC's serial worker).
  - Retry policy and dead-letter status are per job type.

### VRD-11-04 — Client notifications through protocol subscriptions (fusion LS + UC)
- **Decision:** fusion.
- **Consequences:** Clients subscribe to event filters and to their own jobs. The server pushes events, cards and render instructions over the session channel (CMP-15). Polling remains as a fallback.

### VRD-11-05 — Triggered work runs with a system identity and a default context (best-of LC/UC)
- **Decision:** best-of LC and UC.
