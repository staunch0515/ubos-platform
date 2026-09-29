---
id: UBS-SYS-STU-03
title: Studio — Interfaces
status: draft
phase: PH-2
depends_on: [UBS-SYS-STU-02]
---

# Studio — Interfaces (IF-STU-*)

### IF-STU-001 — Builder operations
- **Kind:** UBTP message
- **Caller → Callee:** STU → NOD
- **Request:** reflection queries, mutations on builder branches, change-set operations, `merge_preview`, `simulate`, `impact`, `tag`
- **Response:** as the DVM host API
- **Errors:** standard errors; long operations return job handles
- **Idempotency:** SDK keys.
- **Authorization:** builder roles on branches and model scopes.
- **Satisfies:** CTR-011

### IF-STU-002 — Forge service calls
- **Kind:** UBTP message
- **Caller → Callee:** STU → NOD (Forge service)
- **Request:** `forge.check(branch)`, `forge.types(branch)`, `forge.verify_lens`, `forge.verify_buk`, `forge.docs`
- **Response:** findings and artefacts
- **Errors:** partial results on budget exhaustion
- **Idempotency:** pure.
- **Authorization:** read on the branch.
- **Satisfies:** CTR-012
