---
id: UBS-SYS-EXC-03
title: Buk Exchange — Interfaces
status: draft
phase: PH-4
depends_on: [UBS-SYS-EXC-02]
---

# Buk Exchange — Interfaces (IF-EXC-*)

### IF-EXC-001 — Publish API
- **Kind:** HTTP endpoint
- **Caller → Callee:** FRG → EXC
- **Request:** `POST /v1/archives` (upload), `POST /v1/archives/{root}/verify`, `POST /v1/packages/{name}/versions/{v}:publish`, `:deprecate`
- **Response:** verification status and record; listing URL
- **Errors:** `PKG.SIGNATURE_INVALID`, `PKG.MANIFEST_INVALID`, `PKG.NAMESPACE_VIOLATION`, findings list
- **Idempotency:** by archive root.
- **Authorization:** publisher key and account token.
- **Satisfies:** CTR-015

### IF-EXC-002 — Retrieval API
- **Kind:** HTTP endpoint
- **Caller → Callee:** NOD → EXC
- **Request:** `GET /v1/search`, `GET /v1/archives/{root}`, `GET /v1/archives/{root}/verification`, `GET /v1/licences/{id}`, `GET /v1/advisories?since=`
- **Response:** archives, records, licences, advisories
- **Errors:** `404`, `403` for unlicensed commercial archives
- **Idempotency:** pure.
- **Authorization:** node or tenant token for licensed content.
- **Satisfies:** CTR-054

### IF-EXC-003 — Billing integration
- **Kind:** HTTP endpoint
- **Caller → Callee:** CTL ↔ EXC
- **Request:** invoice line batches; licence status events
- **Response:** acknowledgements
- **Errors:** retry
- **Idempotency:** event IDs.
- **Authorization:** service mTLS.
- **Satisfies:** CTR-053
