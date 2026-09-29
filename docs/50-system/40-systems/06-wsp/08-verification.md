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
| SUITE-WSP-E2E | Playwright end-to-end suites for the reference scenarios (fund back office, contracts) on web, desktop and mobile | SCN |
| SUITE-WSP-A11Y | automated axe checks per widget and screen; manual audit per release | INSP, USE |
| SUITE-WSP-RENDER | deterministic rendering (byte-equal PDFs), PDF/A and PDF/UA validation | CONF |
| SUITE-WSP-PERF | lab and field performance budgets | BENCH |
| SUITE-WSP-USE | usability studies with business users, approvers and auditors (NR-USE-001…003, NR-USE-007) | USE |
| SUITE-WSP-SEC | portal scoping, widget sandbox, XSS and CSP tests | SEC |

| Phase | WSP evidence |
|---|---|
| PH-1b | engineering UI supports acceptance scenarios of PH-1b |
| PH-2 | E2E suites for both verticals green; WCAG 2.2 AA audit passed; NR-USE-001 and NR-USE-003 met in studies; signing ceremonies pass legal review for E-SIGN and UETA |
| PH-3 | desktop, mobile, offline and portal suites; Smart Grid performance with 100,000 rows |
| PH-4 | Books reader sandbox tests |
