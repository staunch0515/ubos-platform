---
id: UBS-SYS-FED-03
title: Federation — Interfaces
status: draft
phase: PH-5
depends_on: [UBS-SYS-FED-02]
---

# Federation — Interfaces (IF-FED-*)

### IF-FED-001 — Registry API
- **Kind:** HTTP endpoint
- **Caller → Callee:** NOD → FED registry
- **Request:** `PUT /v1/vae/{did}` (signed registration), `GET /v1/resolve/{did}`, `POST /v1/trust` (proposal), `POST /v1/trust/{id}:accept|end`
- **Response:** signed records
- **Errors:** `PROOF.SIGNATURE_INVALID`, `404`
- **Idempotency:** registrations versioned by sequence number.
- **Authorization:** signatures of the controlling keys.
- **Satisfies:** CTR-061, FR-SYNC-051, FR-SYNC-053

### IF-FED-002 — Shared-object exchange
- **Kind:** UBTP message
- **Caller → Callee:** NOD ↔ NOD
- **Request:** `Sync` phases for shared scopes; `proposal.submit`, `proposal.sign`, `proposal.reject`
- **Response:** acknowledgements, signatures, integration results
- **Errors:** `SYNC.INTEGRITY_FAILURE`, `IAM.DENIED`
- **Idempotency:** content-addressed.
- **Authorization:** trust relationship and party keys.
- **Satisfies:** CTR-062, FR-SYNC-072
