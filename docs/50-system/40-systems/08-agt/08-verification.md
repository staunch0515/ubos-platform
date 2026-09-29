---
id: UBS-SYS-AGT-08
title: Agent Hub — Verification Plan
status: draft
phase: PH-3
depends_on: [UBS-SYS-AGT-02]
---

# Agent Hub — Verification Plan

| Suite | Content | Method |
|---|---|---|
| SUITE-AGT-GOV | classification blocking and redaction, region pinning, budget enforcement, no-merge rule, scope denial | SEC |
| SUITE-AGT-KILL | kill switch latency and completeness under load | BENCH, SEC |
| SUITE-AGT-INJECT | prompt-injection corpus against agents with side-effect tools | SEC |
| SUITE-AGT-MCP | MCP conformance and delegation expiry | CONF |
| SUITE-AGT-EVAL | evaluation suites for the reference agents (reconciliation assistant, contract clause reviewer) | CONF |
| SUITE-AGT-SCN | agent scenarios SCN-3xx end to end | SCN |

| Phase | AGT evidence |
|---|---|
| PH-3 | all suites green; reference agents reach their evaluation thresholds; zero successful injections with side effects; kill switch within NR-PERF-019 |
| PH-4 | cost controls with CTL budgets verified; self-hosted inference route tested |
