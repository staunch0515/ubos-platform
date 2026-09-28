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
| Q-001 | META-CHARTER | Target technology stack (Java / Rust / hybrid)? | research | open | User delegated the recommendation; decided in ADR-001 (P3). |
| Q-002 | META-CHARTER | Is there an old/new relationship among the 8 repos? | user | answered | No. All repos are peers. |
| Q-003 | META-CHARTER | Documentation language? | user | answered | English only. |
| Q-004 | META-CHARTER | Keep analysis notes? | user | answered | Yes, write everything; exhaustive research. |
| Q-005 | CMP-01 | Idempotency and job records: entities or technical store? | research | open | Candidate: jobs as entities, idempotency keys technical with TTL. |
| Q-006 | CMP-02 | Namespaced property keys for tenant extensions? | research | open | Candidate: dotted namespaces for non-root extensions. |
| Q-007 | CMP-09 | Restricted expression language for invariants/guards (Rhai subset, CEL, JSONLogic)? | research | open | To be decided with ADR-001. |
| Q-008 | CMP-19 | Confirm the "five clients" (web, desktop, CLI, IDE, SDK/agent; or mobile)? | user | open | Candidate list in VRD-19-04. |
