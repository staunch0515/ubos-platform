---
id: UBS-SYS-CTL-04
title: Control Plane — Data
status: draft
phase: PH-4
depends_on: [UBS-SYS-CTL-02]
---

# Control Plane — Data

CTL uses its own PostgreSQL database. It stores no business data.

| Table | Purpose |
|---|---|
| `cell` | Cells: region, capacity, state, version |
| `node` | registered nodes, certificates, versions, last heartbeat |
| `tenant` | tenant ID, placement, plan, residency, state, technical sizes |
| `placement_decision` | inputs and outcome of each placement |
| `operation` | long-running operations (lifecycle, migration, wave) with step state |
| `approval` | two-person approvals with signatures |
| `wave`, `wave_target` | upgrade and configuration waves |
| `config_version` | signed node configuration documents |
| `usage_record` | verified usage records per node |
| `usage_daily` | aggregates per tenant and meter |
| `plan`, `plan_version` | plan catalogue |
| `entitlement` | derived entitlements per tenant |
| `budget` | budgets and caps |
| `operator_audit` | hash-chained operator actions |

### DAT-CtlOperation
- **Purpose:** A resumable orchestrated operation.
- **Kind:** System
- **Stored in:** `operation`
- **Schema:**
```yaml
DAT-CtlOperation:
  operation_id: { type: uuid, required: true, description: identity and idempotency key }
  kind:         { type: enum, required: true, description: tenant_create | tenant_suspend | tenant_delete | migrate | wave | evacuate }
  target:       { type: string, required: true, description: tenant, cell or wave }
  steps:        { type: list<object>, required: true, description: ordered steps with state, attempts, last error }
  state:        { type: enum, required: true, description: pending | running | waiting_approval | succeeded | failed | aborted }
  approvals:    { type: list<object>, required: false, description: operator approvals }
```
- **Invariants:**
  1. Each step is executed at most once successfully; retries are idempotent on the node.
- **Satisfies:** FR-TEN-072, FR-OPS-082
