---
id: UBS-SYS-FED-08
title: Federation — Verification Plan
status: draft
phase: PH-5
depends_on: [UBS-SYS-FED-02]
---

# Federation — Verification Plan

| Suite | Content | Method |
|---|---|---|
| SUITE-FED-SHARED | shared-BPU protocol with 2–5 parties, partitions and crashes; equal roots after convergence | SIM, FORM |
| SUITE-FED-TRUST | trust enforcement, DID-bound mTLS, revoked keys | SEC |
| SUITE-FED-FALLBACK | remote parent outages and reconciliation | SIM |
| SUITE-FED-PILOT | a two-organisation pilot (fund administrator and asset manager sharing investor registers) | PILOT |

| Phase | FED evidence |
|---|---|
| PH-5 | all suites green; formal model STD-SYNC-012 checked; pilot accepted by both organisations |
