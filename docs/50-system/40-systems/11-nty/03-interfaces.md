---
id: UBS-SYS-NTY-03
title: Notary — Interfaces
status: draft
phase: PH-3
depends_on: [UBS-SYS-NTY-02]
---

# Notary — Interfaces (IF-NTY-*)

### IF-NTY-001 — Evidence request
- **Kind:** UBTP message
- **Caller → Callee:** WSP (auditor) → NOD → NTY
- **Request:** `{ scope: query + selector, recipient: internal | external, redaction_profile, include_journals }`
- **Response:** job handle, then archive link and manifest
- **Errors:** `IAM.DENIED`
- **Idempotency:** request hash.
- **Authorization:** `evidence.export`.
- **Satisfies:** FR-PROOF-052

### IF-NTY-002 — Verifier CLI and library
- **Kind:** CLI command
- **Caller → Callee:** any party → verifier
- **Request:** `ubos-verify pack <file> | seal <pdf> | archive <file> | proof <json> [--trust <keys>] [--json]`
- **Response:** verification report
- **Errors:** exit code 1 on any failed check
- **Idempotency:** pure.
- **Authorization:** —
- **Satisfies:** FR-PROOF-061, FR-PROOF-062, CTR-032
