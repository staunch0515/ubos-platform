---
id: UBS-SYS-CTL-03
title: Control Plane — Interfaces
status: draft
phase: PH-4
depends_on: [UBS-SYS-CTL-02]
---

# Control Plane — Interfaces (IF-CTL-*)

CTL calls the node admin interfaces IF-NOD-060…064. This document lists the interfaces CTL
itself provides.

### IF-CTL-001 — Node enrolment
- **Kind:** HTTP endpoint
- **Caller → Callee:** NOD → CTL
- **Request:** `POST /v1/nodes/enrol { token, node_id, edition, version, csr }`
- **Response:** `{ certificate, ca_chain, config_version }`
- **Errors:** `401` invalid token
- **Idempotency:** by `node_id` and token (single use).
- **Authorization:** registration token.
- **Satisfies:** FR-OPS-081

### IF-CTL-002 — Usage and heartbeat intake
- **Kind:** HTTP endpoint
- **Caller → Callee:** NOD → CTL
- **Request:** `POST /v1/usage` (batch of DAT-UsageRecord), `POST /v1/heartbeat`
- **Response:** `{ last_accepted_seq }`
- **Errors:** `409` chain mismatch
- **Idempotency:** record IDs.
- **Authorization:** node mTLS.
- **Satisfies:** FR-BILL-011, CTR-051

### IF-CTL-003 — Operator API
- **Kind:** HTTP endpoint
- **Caller → Callee:** console, automation → CTL
- **Request:** `/v1/tenants`, `/v1/tenants/{id}:{suspend|resume|archive|delete|migrate}`, `/v1/cells`, `/v1/waves`, `/v1/configs`, `/v1/plans`, `/v1/approvals`
- **Response:** resources and operation handles
- **Errors:** RFC 9457 problem details
- **Idempotency:** `Idempotency-Key` on all mutations.
- **Authorization:** operator roles; destructive operations need two approvals.
- **Satisfies:** FR-OPS-081, FR-OPS-082, FR-TEN-072

### IF-CTL-004 — Tenant portal API
- **Kind:** HTTP endpoint
- **Caller → Callee:** tenant administrator → CTL
- **Request:** `/v1/me/tenant`, `/usage`, `/budgets`, `/invoices`, `/operator-actions`
- **Response:** tenant-scoped resources
- **Errors:** problem details
- **Idempotency:** `Idempotency-Key` for budget changes.
- **Authorization:** tenant administrator token issued by the tenant's node (federated sign-in).
- **Satisfies:** FR-BILL-051, FR-BILL-052

### IF-CTL-005 — Entitlement sync with EXC
- **Kind:** HTTP endpoint
- **Caller → Callee:** CTL ↔ EXC
- **Request:** licence status changes, invoice line batches
- **Response:** acknowledgements
- **Errors:** retry
- **Idempotency:** event IDs.
- **Authorization:** service mTLS.
- **Satisfies:** CTR-053, FR-BILL-022
