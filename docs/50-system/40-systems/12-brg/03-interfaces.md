---
id: UBS-SYS-BRG-03
title: Bridge — Interfaces
status: draft
phase: PH-2
depends_on: [UBS-SYS-BRG-02]
---

# Bridge — Interfaces (IF-BRG-*)

### IF-BRG-001 — Import run
- **Kind:** UBTP message
- **Caller → Callee:** STU → NOD → BRG
- **Request:** `{ source: file ref | adapter, mapping version, target branch, mode: dry_run | apply }`
- **Response:** run handle, then row results and reconciliation report
- **Errors:** mapping and validation errors per row
- **Idempotency:** run key from source hash and mapping version.
- **Authorization:** import rights on the target branch.
- **Satisfies:** FR-MIG-012, FR-MIG-013

### IF-BRG-002 — Export sink management
- **Kind:** HTTP endpoint
- **Caller → Callee:** tenant administrator → BRG (via NOD)
- **Request:** create or update sink `{ kind: parquet | iceberg | kafka | kinesis | pubsub, target, classes, clearance }`
- **Response:** sink status and watermark
- **Errors:** residency and clearance violations
- **Idempotency:** by sink ID.
- **Authorization:** `analytics.admin`.
- **Satisfies:** FR-ANL-051, FR-ANL-054
