---
id: UBS-SYS-BPA-03
title: BPA Standard Governance — Interfaces
status: draft
phase: PH-0
depends_on: [UBS-SYS-BPA-02]
---

# BPA Standard Governance — Interfaces (IF-BPA-*)

### IF-BPA-001 — Conformance package
- **Kind:** HTTP endpoint
- **Caller → Callee:** FRG, implementers → BPA release site
- **Request:** `GET /releases/{version}/conformance.tar.zst` and `manifest.json` with signature
- **Response:** package with vectors, schemas, profiles, adapter protocol description
- **Errors:** `404`
- **Idempotency:** immutable per version.
- **Authorization:** public.
- **Satisfies:** CTR-014, FR-STD-021

### IF-BPA-002 — Certificate registry
- **Kind:** HTTP endpoint
- **Caller → Callee:** anyone → BPA registry
- **Request:** `GET /certificates/{id}`, `GET /certificates?implementation=`
- **Response:** DAT-Certificate with status
- **Errors:** `404`
- **Idempotency:** pure.
- **Authorization:** public.
- **Satisfies:** FR-STD-052
