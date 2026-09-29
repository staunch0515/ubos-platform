---
id: SPEC-20-2
title: "Events, Triggers and Jobs — Behavior"
status: complete
phase: P4
depends_on: [SPEC-20, SPEC-11, SPEC-15, SPEC-16, SPEC-17, SPEC-19, SPEC-22, SPEC-24, ADR-004]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Events, Triggers and Jobs — Behavior

Part 2 of SPEC-20 (`20-events-jobs/`). Header, concepts and file list: `00-index.md`.

---

## 3. Behavior

### 3.1 Events and dispatch

### REQ-EVT-001 — Events from the commit path
- **Statement:** The flush (REQ-TX-011 step 11) MUST write the following to the outbox:
  - one `entity.committed` event per commit, with `changed_paths` for UPDATE;
  - one `process.completed` event;
  - the custom events staged by `emit.event`;
  - the deferred emits;
  - job signals for Jobs created in the process.

  The failure-record transaction (REQ-STO-009) writes `process.failed` or `process.rejected`. No other code path may publish events. Every writer goes through the flush, so no event can be lost (the gap of US/FU, CMP-11).
- **Origin:** VRD-11-01, CON-LC-027, CON-UC-003 (dual-write critique)
- **Acceptance:** Killing the server right after a commit still delivers its events after restart.
- **Priority:** P0

### REQ-EVT-002 — Dispatcher
- **Statement:** The dispatcher (runtime role DISPATCHER, REQ-ARCH-007) MUST:
  1) claim outbox rows in `seq` order, per partition (`hash(tenant, subject entity) mod P`, default P = 16), with a lease of 30 s;
  2) for each row, perform the matching deliveries:
     - EVENT rows: hooks (REQ-EVT-010), webhooks (REQ-EVT-030), the event stream (REQ-EVT-040) and workflow correlation;
     - EMIT rows: SPEC-19 REQ-FLOW-040;
     - JOB_SIGNAL rows: push the job to its ready queue (REQ-EVT-020);
  3) delete the row only after every delivery for it has been handed off durably (a Job committed, a process committed, or an event appended to the stream).

  If handing off fails, the lease expires and the row is re-delivered. The deduplication rules of REQ-EVT-003 then prevent double effects.
- **Origin:** VRD-11-01, REQ-STO-011
- **Acceptance:** The outbox lag metric (REQ-OBS-023) returns to 0 after a burst of 10 000 commits, and each hook fires once per event.
- **Priority:** P0

### REQ-EVT-003 — Idempotent consumption
- **Statement:** Every consumer of a delivery MUST deduplicate:
  - **Jobs created from events** carry `dedupe_key = "<hook-or-webhook URI>:<event id>"`, which is unique among non-terminal and recently terminal jobs (7 days).
  - **DIRECT hook processes** use the idempotency protocol (REQ-TX-015) with key `hook:<hook>:<event>`.
  - **Event stream appends** use the event ID as the message ID.
  - **Custom consumers** (logic reacting to events) receive `event.id` and SHOULD use it as their idempotency key.
- **Origin:** VRD-11-01 (at-least-once, idempotent consumers), CON-UC-032
- **Acceptance:** Forcing a re-delivery of an outbox row creates no duplicate jobs or commits.
- **Priority:** P0

### 3.2 Hooks

### REQ-EVT-010 — Hook matching and firing
- **Statement:** The dispatcher MUST keep, per tenant, an index of the enabled Hook heads on the tenant's `main`. It is rebuilt when Hook commit events arrive (UP refresh). For each EVENT, the hooks match when:
  - the event type matches a pattern;
  - the filter holds: types (subtypes included), actions, branch globs, changed paths, and the rule-subset expression over the event.

  For each match:
  1) compute the args from the `args` expressions;
  2) then, by mode:
     - **JOB:** create a Job with `source {kind: HOOK, ref: hook, event}`, anchors from the Hook's `target` version and the context, `run_as` and `on_behalf_of` = the event principal;
     - **DIRECT:** start a process at once (kind SYSTEM, operation = target) with the same identity, which records TRIGGERED_BY = the event's commit.

  Hooks do not see events of processes they started themselves, unless `allow_self_trigger: true`. Loop protection is REQ-FLOW-041.
- **Origin:** VRD-11-02, VRD-11-05, CON-UC-027, CON-UP-025
- **Acceptance:** A hook on `entity.committed` of `acme.Invoice` with `changed_any: ["/_state"]` and expression `event.data == () && true` fires once per state change and creates one job each time.
- **Priority:** P0 (JOB), P1 (DIRECT)

### REQ-EVT-011 — Hook governance
- **Statement:** Committing a Hook, Cron or Webhook MUST require `definition.author`. If `run_as` names a principal other than the tenant's system account, the committer must be allowed to act as it: `security.admin`, or a DELEGATES_TO relationship from that principal. Hooks are checked again at firing. If `run_as` is no longer ACTIVE, the firing becomes a DEAD job with `SEC.PRINCIPAL_INACTIVE`.
- **Origin:** VRD-11-05, VRD-10-06
- **Acceptance:** A user cannot create a hook that runs as the tenant admin.
- **Priority:** P0

### 3.3 Jobs

### REQ-EVT-020 — Job creation and queues
- **Statement:** A Job is created inside a process: through `job.submit`, an ASYNC emit, a Hook in JOB mode, a Cron fire, webhook delivery or `delay` emits. When that process flushes, a JOB_SIGNAL goes to the outbox. The dispatcher then pushes the job ID to the runtime queue `queue:{tenant}:{priority}`, or, when `not_before` is in the future, to the delayed set `delayed:{tenant}` scored by `not_before`. The scheduler moves due delayed jobs into the ready queues every second.
- **Origin:** VRD-11-03, ADR-004, CON-UC-003
- **Acceptance:** A job submitted with a 10-minute delay starts no earlier than 10 minutes after submission.
- **Priority:** P0

### REQ-EVT-021 — Claiming
- **Statement:** A worker MUST obtain jobs by the following loop:
  1) pop a job ID with fair selection (REQ-EVT-024);
  2) load the Job's head;
  3) skip it if its state is not QUEUED or its `not_before` is in the future;
  4) commit `state: RUNNING, attempts: +1, lease_until: now + timeout + 30 s, worker: id`, with CAS on the head (a small process of kind SYSTEM). A CAS failure means another worker won, so skip.

  Only the claim commit decides ownership. The queue is only a hint.
- **Origin:** VRD-11-03, CON-UC-032, CON-UC-020
- **Acceptance:** 10 workers competing for 1 000 jobs execute each job exactly once, with no duplicates in outputs.
- **Priority:** P0

### REQ-EVT-022 — Attempt execution
- **Statement:** An attempt MUST run the operation in a process of kind JOB with:
  - `job_id` and `parent_process_id` = the claim process;
  - principal `run_as`, `on_behalf_of`, and the anchored versions (REQ-TX-021);
  - a deadline = the job timeout.

  On success, the same flush commits the business changes **and** the Job update `state: SUCCEEDED, result, last_process`. The Job update uses CAS on the head the worker wrote at claim time, so a stale attempt (whose lease expired and was reclaimed) cannot commit (UC "execution is commit").

  On failure or timeout, a new process updates the Job:
  - `last_error` is set;
  - the state becomes QUEUED, with `not_before` = the backoff, when `attempts < max_attempts` and the error is retryable (category UNAVAILABLE, CONFLICT, LIMIT (rate) or timeout);
  - otherwise the state becomes DEAD (validation and forbidden errors are not retried).

  **Ephemeral jobs** skip the RUNNING commit. They use a runtime lease and CAS on the QUEUED head at the terminal commit.
- **Origin:** VRD-11-03, CON-UC-020, CON-UC-006
- **Acceptance:**
  1) A job whose logic throws a transient `RT.EXTERNAL_FAILED` is retried with exponential backoff and succeeds on attempt 3.
  2) A validation failure goes to DEAD after one attempt.
- **Priority:** P0

### REQ-EVT-023 — Completion signals and parents
- **Statement:** A job reaching a terminal state MUST trigger the following:
  - the event `job.succeeded`, `job.failed` (non-final attempt), `job.dead` or `job.cancelled`;
  - if `reply_to` is set, the signal `job.completed` or `job.failed` to that ProcessInstance (REQ-FLOW-033);
  - if `parent_job` is set, the event is also published under the parent's subject, so that parents waiting through workflows can react.
- **Origin:** VRD-11-03, CON-UC-026, VRD-06-04
- **Acceptance:** A workflow waiting on a submitted job moves on when the job succeeds.
- **Priority:** P1

### REQ-EVT-024 — Worker pool and fairness
- **Statement:** A worker process MUST run up to `workers.concurrency` attempts in parallel (default 4 × CPU cores). This fixes UC's serial worker. It selects the next job as follows:
  - rotate round-robin over tenants with ready jobs;
  - within a tenant, take the highest priority first;
  - skip tenants at their `max_concurrent_processes` or job quota (REQ-TEN-031).

  Script execution uses the blocking pool (REQ-CONV-084).
- **Origin:** VRD-11-03, VRD-19-03, CON-UC-003 (serial worker critique)
- **Acceptance:** One tenant flooding 100 000 jobs does not delay another tenant's single job by more than one attempt slot.
- **Priority:** P1

### REQ-EVT-025 — Cancellation, retry and progress
- **Statement:**
  - **Cancel:** `job.cancel@v1 {job}` changes a QUEUED job to CANCELLED by CAS. For a RUNNING job, it sets `cancel:{job}` in the runtime store. The worker checks the flag at each syscall and progress callback (REQ-CTX-013), aborts without flushing, and commits CANCELLED.
  - **Retry:** `job.retry@v1 {job}` (admins) moves a DEAD or FAILED job back to QUEUED with `attempts` reset.
  - **Progress:** `job.progress(percent, message)` (syscall) writes to the runtime store and publishes `job.progress` events to JOB subscriptions. Progress is not versioned.
- **Origin:** CON-UC-059 (live console, status), VRD-15-04
- **Acceptance:** A cockpit following a long import shows progress and can cancel it, and the job ends as CANCELLED with no business commits.
- **Priority:** P1

### REQ-EVT-026 — Sweeper
- **Statement:** The scheduler's sweeper MUST run every 30 s and:
  - re-enqueue QUEUED jobs with no queue entry for more than 60 s (after a runtime-store loss, ADR-004);
  - reclaim RUNNING jobs whose `lease_until` has passed, treating them as a failed attempt with `TX.TIMEOUT` and applying the retry rules;
  - recompute the delayed set from the QUEUED jobs with `not_before` after a runtime-store loss.

  The sweeper queries by indexed Job properties (`state`, `not_before`, `lease_until` are searchable).
- **Origin:** ADR-004, CON-UC-003 ("a lost queue message leaves a visible PENDING record")
- **Acceptance:** Flushing Redis during load loses no job; every job finishes after the sweeper runs.
- **Priority:** P0

### 3.4 Scheduler

### REQ-EVT-027 — Cron firing
- **Statement:** The scheduler leader MUST keep the next fire time of every enabled Cron head per tenant. The set is rebuilt on Cron commit events. At each fire time, the scheduler creates a Job with `source {kind: CRON, ref, fire_time}` and `dedupe_key = "<cron URI>:<fire_time>"`. Then:
  - **Overlap:** SKIP drops the fire while a previous job is non-terminal; QUEUE creates it anyway; ALLOW creates it and allows parallel running.
  - **After downtime:** `catch_up` NONE skips missed fires, ONE fires once, and ALL fires every missed time, up to 100.
  - **Jitter:** adds a random delay.
  - **Time zones:** evaluation uses the Cron's time zone, with DST gaps and overlaps handled so that each wall-clock time fires at most once.
- **Origin:** VRD-11-02, CON-UP-026, CON-UC-027, CON-LC-027
- **Acceptance:**
  1) A Cron `0 2 * * *` in `Europe/Berlin` fires once on DST-change days.
  2) A scheduler failover does not duplicate a fire (the dedupe key).
- **Priority:** P0

### 3.5 Webhooks

### REQ-EVT-030 — Webhook delivery
- **Statement:** For each matching active Webhook, the dispatcher MUST create a delivery Job (operation `notify.webhook@v1`, dedupe by webhook and event). The job POSTs:
  - **Body:** `{id, type, occurred_at, tenant, subject, entity_type, action, commit_id, branch, principal, data?}`. `data` holds the subject's effective data read as the webhook `owner` (masked) when `include_data` is set.
  - **Headers:** `Content-Type: application/json`, `X-UBOS-Event`, `X-UBOS-Delivery` (the event ID), `X-UBOS-Timestamp`, and `X-UBOS-Signature: sha256=<hex(HMAC-SHA256(secret, timestamp + "." + body))>`.
  - **Transport rules:** `timeout_ms` applies, redirects are not followed, and the response body is limited to 64 KiB (recorded, truncated).

  A 2xx response is success. 5xx, 429 and network errors are retryable. Other 4xx responses are final (DEAD).
- **Origin:** VRD-10-05, VRD-11-02, CON-FU-024
- **Acceptance:** A receiver that verifies the signature with the shared secret accepts deliveries. A replay with a stale timestamp (more than 5 minutes) is detectable by the receiver.
- **Priority:** P1

### REQ-EVT-031 — Webhook health
- **Statement:** After 50 consecutive DEAD deliveries, the kernel MUST set `active: false` through a SYSTEM process and notify the webhook owner. `webhook.test@v1 {webhook}` sends a `ping` event on demand. `webhook.deliveries@v1 {webhook}` lists recent delivery jobs with status codes.
- **Origin:** CON-FU-024 (retry with backoff), NEW: health management
- **Acceptance:** An endpoint returning 500 repeatedly is deactivated after 50 dead deliveries, and the owner gets a notification.
- **Priority:** P2

### 3.6 Event stream and notifications

### REQ-EVT-040 — Event stream for subscriptions
- **Statement:** The dispatcher MUST append every EVENT to the per-tenant stream `evt:{tenant}` in the runtime store (Redis Streams, or an in-process ring buffer in the personal edition), retaining at least 10 minutes or 10 000 events. Gateways read the streams of the tenants of their sessions and evaluate each session's filters and permissions (REQ-PROTO-011). They mask event data per recipient and deliver `Event` messages with resumable cursors = stream IDs.
- **Origin:** VRD-11-04, CON-LS-028, CON-UC-043 (event bus)
- **Acceptance:** Two gateways each deliver a committed invoice event to their own subscribed sessions within 200 ms (p95).
- **Priority:** P0

### REQ-EVT-041 — Notifications
- **Statement:** A NOTIFY emit MUST create a `Notification` entity for the recipient in the dispatch process. The entity is PRIVATE and owned by the recipient. The resulting `notification.created` event reaches the recipient's sessions subscribed to NOTIFICATIONS. Recipients mark notifications as read with `notification.read@v1`, and archive them after `expires_at`. TASK-level notifications appear in the cockpit dock (SPEC-26). Mobile push through platform providers is P2 (SPEC-26).
- **Origin:** VRD-11-04, CON-UW-013 (dock items), CON-UC-056 (cards)
- **Acceptance:** An approval request creates a TASK notification for each approver, and it appears on web and mobile.
- **Priority:** P1

### REQ-EVT-042 — Mail
- **Statement:** A MAIL emit MUST become a delivery Job (`notify.email@v1`):
  - The body is rendered from a `MessageTemplate` entity (ontology, SPEC-29) in the recipient's locale, with the emit args.
  - It is sent through the tenant's `MailTransport` entity (SMTP host, port, TLS, and credentials as a Secret URI).
  - Retries follow the job policy.
  - Missing configuration gives DEAD with `EVT.MAIL_NOT_CONFIGURED`.
- **Origin:** CON-LC-027 (email outbox), VRD-11-02
- **Acceptance:** The `welcome` template is sent once per registration and retried on SMTP 4xx.
- **Priority:** P1
