---
id: META-QUESTIONS
title: Open Questions
status: draft
phase: P0
depends_on: [META-RULES]
---

# Open Questions

Format: one row per question. `Owner` = `user` (needs human answer) or `research`
(resolvable by further analysis). Answered questions stay in the table.

| ID | Raised in | Question | Owner | Status | Answer / resolution |
|---|---|---|---|---|---|
| Q-001 | META-CHARTER | Target technology stack (Java / Rust / hybrid)? | research | answered | User delegated the recommendation; ADR-001 accepted: Rust kernel + Rhai + PostgreSQL/SQLite + React/TS (VRD-20-01…05). |
| Q-002 | META-CHARTER | Is there an old/new relationship among the 8 repos? | user | answered | No. All repos are peers. |
| Q-003 | META-CHARTER | Documentation language? | user | answered | English only. |
| Q-004 | META-CHARTER | Keep analysis notes? | user | answered | Yes, write everything; exhaustive research. |
| Q-005 | CMP-01 | Idempotency and job records: entities or technical store? | user | answered | 2026-09-28: Jobs are entities; idempotency keys live in the technical (runtime) store with TTL. See ADR-004. |
| Q-006 | CMP-02 | Namespaced property keys for tenant extensions? | user | answered | 2026-09-28: Yes, dotted namespaces for non-root extension keys (e.g. `acme.email`). See SPEC-02. |
| Q-007 | CMP-09 | Restricted expression language for invariants/guards (Rhai subset, CEL, JSONLogic)? | user | answered | 2026-09-28: Rhai subset. See ADR-002. |
| Q-008 | CMP-19 | Confirm the "five clients" (web, desktop, CLI, IDE, SDK/agent; or mobile)? | user | answered | 2026-09-28: Mobile replaces IDE → web, desktop, mobile, CLI, SDK/agent. IDE integration remains developer tooling, not a client. See ADR-003. |
