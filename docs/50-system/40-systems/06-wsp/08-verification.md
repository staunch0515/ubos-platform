---
id: UBS-SYS-WSP-08
title: Workspace — Verification Plan
status: draft
phase: PH-2
depends_on: [UBS-SYS-WSP-02]
---

# Workspace — Verification Plan

| Suite | Content | Method |
|---|---|---|
| SUITE-WSP-E2E | Playwright end-to-end suites for the reference scenarios (Basic Finance SCN-501…512) on the Web; desktop and mobile from PH-4 | SCN |
| SUITE-WSP-A11Y | automated axe checks per widget and screen; manual audit per release | INSP, USE |
| SUITE-WSP-RENDER | deterministic rendering (byte-equal PDFs), PDF/A and PDF/UA validation | CONF |
| SUITE-WSP-PERF | lab and field performance budgets | BENCH |
| SUITE-WSP-USE | usability studies with business users, approvers and auditors (NR-USE-001…003, NR-USE-007) | USE |
| SUITE-WSP-SEC | portal scoping, widget sandbox, XSS and CSP tests | SEC |

| Phase | WSP evidence |
|---|---|
| PH-1b | engineering UI supports acceptance scenarios of PH-1b |
| PH-2 | finance E2E suite green on the Web; professional workbench items DSN-WSP-116…120 accepted; WCAG 2.2 AA audit passed; NR-USE-001 and NR-USE-002 met in studies with accountants; Smart Grid with 100,000 rows |
| PH-3 | portal, War Room, Action Messages and Copilot panel suites; NR-USE-003 |
| PH-4 | desktop, mobile and offline suites (DEC-024, DEC-025); Books reader sandbox tests |
