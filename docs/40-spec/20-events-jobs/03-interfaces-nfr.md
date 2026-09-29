---
id: SPEC-20-3
title: "Events, Triggers and Jobs — Interfaces and non-functional"
status: complete
phase: P4
depends_on: [SPEC-20, SPEC-11, SPEC-15, SPEC-16, SPEC-17, SPEC-19, SPEC-22, SPEC-24, ADR-004]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Events, Triggers and Jobs — Interfaces and non-functional

Part 3 of SPEC-20 (`20-events-jobs/`). Header, concepts and file list: `00-index.md`.

---

## 4. Interfaces

### API-EVT-001 — Services
- **Kind:** rust traits (`ubos_kernel::events`)
- **Signature / shape:**
```rust
#[async_trait] pub trait Dispatcher: Send + Sync { async fn run(&self, partition: u16, shutdown: ShutdownSignal); }
#[async_trait] pub trait Scheduler:  Send + Sync { async fn run(&self, shutdown: ShutdownSignal); async fn next_fires(&self, tenant: &TenantCode) -> Vec<(String, Timestamp)>; }
#[async_trait] pub trait WorkerPool: Send + Sync { async fn run(&self, concurrency: usize, shutdown: ShutdownSignal); }
#[async_trait]
pub trait JobService: Send + Sync {
    async fn submit(&self, s: &mut Session, spec: JobSpec) -> Result<String, UbosError>;   // staged; signal via outbox
    async fn cancel(&self, x: &ExecutionContext, job: &str) -> Result<(), UbosError>;
    async fn retry(&self, x: &ExecutionContext, job: &str) -> Result<(), UbosError>;
    async fn progress(&self, x: &ExecutionContext, percent: u8, message: &str) -> Result<(), UbosError>;
}
```
- **Origin:** CON-UC-003, CON-UP-026

### API-EVT-002 — Named operations
- **Kind:** UBTP targets
- **Signature / shape:**

| URI | Args | Result |
|---|---|---|
| `ubos://system/Action/job.cancel@v1` | `{job}` | state |
| `ubos://system/Action/job.retry@v1` | `{job}` | state |
| `ubos://system/Query/job.status@v1` | `{job}` | `{state, attempts, progress, result?, last_error?}` |
| `ubos://system/Query/job.list@v1` | `{state?, operation?, source?, page}` | jobs |
| `ubos://system/Action/webhook.test@v1` | `{webhook}` | delivery result |
| `ubos://system/Query/webhook.deliveries@v1` | `{webhook, page}` | delivery jobs |
| `ubos://system/Action/notification.read@v1` | `{notifications: [uri] \| all: true}` | – |
| `ubos://system/Query/cron.next@v1` | `{cron}` | next 10 fire times |
- **Origin:** CON-UC-059, CON-FU-024

### Example hook, cron and webhook

```json
{ "type": "Hook", "slug": "acme.invoice.on_approved", "data": {
    "title": "Post approved invoices", "events": ["entity.committed"],
    "filter": { "types": ["ubos://acme/Type/acme.Invoice"], "changed_any": ["/_state"],
                "expression": "event.data.new_state == \"APPROVED\"" },
    "target": "ubos://acme/Action/acme.invoice.post@v1", "mode": "JOB", "enabled": true } }

{ "type": "Cron", "slug": "acme.reports.nightly", "data": {
    "title": "Nightly sales report", "schedule": "0 2 * * *", "timezone": "Asia/Shanghai",
    "target": "ubos://acme/Pipeline/acme.reports.sales@v1", "args": { "day": "${fire_time}" },
    "overlap": "SKIP", "catch_up": "ONE", "enabled": true } }

{ "type": "Webhook", "slug": "acme.erp.sync", "data": {
    "title": "ERP sync", "events": ["entity.committed"], "filter": { "types": ["ubos://acme/Type/acme.Invoice"] },
    "url": "https://erp.acme.example/hooks/ubos", "secret": "ubos://acme/Secret/erp-hook",
    "include_data": true, "owner": "ubos://acme/ServiceAccount/erp-sync", "timeout_ms": 5000,
    "retry": { "max_attempts": 8, "backoff": "EXPONENTIAL", "base": "PT10S", "max": "PT1H" }, "active": true } }
```

For `changed_any: ["/_state"]` events, `event.data` carries `{old_state, new_state}`. The kernel adds these for lifecycle types (REQ-FLOW-016).

### Error codes (EVT)

| Code | Category | Meaning |
|---|---|---|
| `EVT.JOB_NOT_CANCELLABLE` | CONFLICT | job already terminal |
| `EVT.JOB_LEASE_LOST` | CONFLICT | attempt lost its lease (stale attempt) |
| `EVT.HOOK_TARGET_INVALID` | INVALID | hook target not an operation |
| `EVT.CRON_INVALID` | INVALID | unparsable schedule or time zone |
| `EVT.WEBHOOK_URL_INVALID` | INVALID | URL not HTTPS or not allowed |
| `EVT.MAIL_NOT_CONFIGURED` | UNAVAILABLE | no MailTransport |
| `EVT.EVENT_TYPE_NOT_OWNED` | FORBIDDEN | custom event outside owned namespaces |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-160 | Commit → event on subscriber session | p95 ≤ 200 ms |
| NFR-PERF-161 | Commit → hook job started (idle workers) | p95 ≤ 1 s |
| NFR-SCAL-160 | Dispatched events per second per enterprise installation | ≥ 5 000 (16 partitions) |
| NFR-DUR-160 | Lost events or jobs after crash or runtime-store loss | 0 |
| NFR-AVAIL-160 | Scheduler failover | new leader within 15 s, no duplicate fires |
