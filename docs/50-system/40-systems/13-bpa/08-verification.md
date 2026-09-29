---
id: UBS-SYS-BPA-08
title: BPA Standard Governance — Verification Plan
status: draft
phase: PH-0
depends_on: [UBS-SYS-BPA-02]
---

# BPA Standard Governance — Verification Plan

| Suite | Content | Method |
|---|---|---|
| SUITE-BPA-COVERAGE | every MUST clause covered by vectors per profile | INSP |
| SUITE-BPA-MODELS | formal models checked; model traces pass on the reference DVM | FORM, CONF |
| SUITE-BPA-COMPAT | vectors of previous minor releases pass on each new release | CONF |
| SUITE-BPA-CERT | dry-run certification of the reference DVM and one independent implementation (PH-4) | INSP, CONF |

| Phase | BPA evidence |
|---|---|
| PH-0 | 0.x release with Core, Bitemporal and Ledger vectors; three formal models checked |
| PH-1 | 1.0-rc with full coverage for the PH-1 profiles |
| PH-2 | 1.0 final; errata process exercised at least once |
| PH-4 | certification programme live; reference DVM certified |
