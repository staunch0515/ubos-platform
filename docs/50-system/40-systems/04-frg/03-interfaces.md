---
id: UBS-SYS-FRG-03
title: Logic Forge — Interfaces
status: draft
phase: PH-1
depends_on: [UBS-SYS-FRG-02]
---

# Logic Forge — Interfaces (IF-FRG-*)

### IF-FRG-001 — CLI command set
- **Kind:** CLI command
- **Caller → Callee:** developer, CI → FRG
- **Request:** `forge <command> [options] [--json]` for the commands of DSN-FRG-001
- **Response:** human output or JSON per command schema; exit codes 0 success, 1 findings, 2 usage error, 3 environment error
- **Errors:** as exit codes plus JSON error objects
- **Idempotency:** all commands except `publish` and `install` are side-effect free outside the project's `.forge/` cache.
- **Authorization:** local; `install`, `publish`, `simulate` use UBTP or EXC credentials.
- **Satisfies:** FR-DEV-011, FR-DEV-012

### IF-FRG-002 — Library API (Forge service)
- **Kind:** library API
- **Caller → Callee:** NOD → FRG
- **Request:** `check(source | branch)`, `types(branch, lang)`, `verify_lens(lens)`, `simulate(spec, budget)`, `verify_buk(archive)`, `docs(branch)`
- **Response:** findings, generated sources, simulation results, verification records
- **Errors:** budget exhaustion returns partial results with a flag
- **Idempotency:** pure for identical inputs.
- **Authorization:** caller's context (enforced by NOD and DVM).
- **Satisfies:** CTR-012

### IF-FRG-003 — Conformance adapter protocol
- **Kind:** library API
- **Caller → Callee:** FRG → implementation under test
- **Request:** JSON lines `{ op: setup | execute | observe | teardown, vector_id, payload }`
- **Response:** `{ ok, result | error }`
- **Errors:** `not_applicable` for vectors outside claimed profiles
- **Idempotency:** per vector.
- **Authorization:** —
- **Satisfies:** FR-STD-021, CTR-014

### IF-FRG-004 — Language server
- **Kind:** library API
- **Caller → Callee:** editor → FRG
- **Request:** LSP 3.17 requests
- **Response:** LSP responses
- **Errors:** LSP errors
- **Idempotency:** —
- **Authorization:** —
- **Satisfies:** FR-DEV-071

### IF-FRG-005 — Debug adapter
- **Kind:** library API
- **Caller → Callee:** editor → FRG
- **Request:** DAP requests over a journal replay session
- **Response:** DAP events (stopped, variables, stack)
- **Errors:** `LOGIC.REPLAY_INPUT_MISSING`
- **Idempotency:** —
- **Authorization:** replay rights on the source VAE when pulling journals.
- **Satisfies:** FR-DEV-092
