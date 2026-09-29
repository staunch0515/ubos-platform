---
id: UBS-SYS-STU-08
title: Studio — Verification Plan
status: draft
phase: PH-2
depends_on: [UBS-SYS-STU-02]
---

# Studio — Verification Plan

| Suite | Content | Method |
|---|---|---|
| SUITE-STU-E2E | end-to-end builder scenarios: new class with lens, sheet change with simulation, decision table with gaps, release | SCN |
| SUITE-STU-USE | builder productivity and learnability studies (NR-USE-004, NR-USE-005) | USE |
| SUITE-STU-PATH | inspection that no Studio operation bypasses the commit path | INSP |

| Phase | STU evidence |
|---|---|
| PH-2 | the Basic Finance Buk can be extended by a trained analyst in Studio within the NR-USE-004 targets; all edits appear as ordinary commits |
| PH-3 | AI drafting produces checked branches; Buk packaging from Studio |
