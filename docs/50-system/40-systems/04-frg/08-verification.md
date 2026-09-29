---
id: UBS-SYS-FRG-08
title: Logic Forge — Verification Plan
status: draft
phase: PH-1
depends_on: [UBS-SYS-FRG-02]
---

# Logic Forge — Verification Plan

| Suite | Content | Method |
|---|---|---|
| SUITE-FRG-CLI | every command, JSON schemas, exit codes | CONF |
| SUITE-FRG-CHECK | a corpus of faulty projects with expected findings per rule ID | CONF |
| SUITE-FRG-REPRO | pack and verify reproducibility across OS and CPU | CONF |
| SUITE-FRG-RESOLVE | dependency solver properties and conflict explanations | PROP |
| SUITE-FRG-CONFORM | runner against the reference DVM and a sample external adapter | CONF |
| SUITE-FRG-DX | developer loop benchmarks (NR-PERF-016) and usability sessions with developers (NR-USE-004, NR-USE-005) | BENCH, USE |

## Phase exit contribution

| Phase | FRG evidence |
|---|---|
| PH-0 | runner executes the first vector set through the adapter protocol |
| PH-1a | check, types, test on the reference DVM; L2 verification records accepted by the DVM |
| PH-1b | `forge dev` loop within NR-PERF-016; Governance vectors |
| PH-2 | pack, sign, verify reproducibly; simulator and debugger; LSP and VS Code extension; both vertical Buks built and verified with Forge |
| PH-3 | L3 toolchains for Rust, TypeScript and Go; AI iteration loop |
| PH-4 | publish to EXC with reproducible verification |
