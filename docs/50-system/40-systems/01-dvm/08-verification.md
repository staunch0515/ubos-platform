---
id: UBS-SYS-DVM-08
title: DVM — Verification Plan
status: draft
phase: PH-1
depends_on: [UBS-SYS-DVM-02, UBS-STD-18]
---

# DVM — Verification Plan

This plan states how every DSN-DVM item is verified, which suites exist, which environments
run them, and what "done" means per phase. Suite and verification IDs are defined in the
verification volume (UBS-VER-00), except the DVM suites, which are defined in §1 below.

## 1. Suites

| Suite | Content | Method | Runs |
|---|---|---|---|
| SUITE-DVM-CONF | standard conformance vectors (VER-CONF-*), all profiles the phase claims | CONF | every commit to the kernel repository |
| SUITE-DVM-STORE | storage adapter suite on PostgreSQL; identical results on SQLite from PH-4 (DEC-024) | CONF | every commit |
| SUITE-DVM-PROP | property tests: timelines, merge, diff, tree history independence, balances, cursors, row filters | PROP | every commit (short), nightly (long: 1 h per property) |
| SUITE-DVM-SIM | deterministic simulation of concurrency, crashes and clock faults | SIM | nightly: 10,000 seeds; weekly: 1,000,000 seeds |
| SUITE-DVM-FAULT | fault injection on real PostgreSQL, SQLite and object store: kill -9, disk full, network partitions, corrupt chunks | FAULT | nightly |
| SUITE-DVM-FORM | TLA+ models: commit and CAS, merge, timelines, sagas, approvals | FORM | on model change; weekly full state space |
| SUITE-DVM-BENCH | NR-PERF, NR-SCAL and NR-COST workloads on ENV-REF-SERVER, ENV-REF-SMALL and ENV-REF-BOX | BENCH | nightly; release gate |
| SUITE-DVM-SEC | sandbox escape suite (L2, L3), permission matrix, isolation suite, masking leak tests | SEC | every commit (matrix), release (escape and pen test) |
| SUITE-DVM-REPLAY | replay corpus across OS, CPU and browser build | CONF | nightly |
| SUITE-DVM-XIMPL | cross-implementation equivalence: export from one kernel build, import into another, compare roots and query results | CONF | release gate |

## 2. Method coverage rule

Each DSN-DVM item lists its methods in **Verification**. The traceability generator
(B27) MUST confirm that every DSN-DVM item is covered by at least one test in the suites
of its methods, and that every **Acceptance** criterion maps to a named test. A DSN item
without mapped tests blocks the phase exit for its phase.

## 3. Key properties (property and simulation suites)

| Property | Items | Suite |
|---|---|---|
| roots are history-independent | DSN-DVM-007, DSN-DVM-930 | PROP |
| indexes equal rebuild from commits | DSN-DVM-005, DSN-DVM-092 | PROP, SIM |
| snapshot reads never see later commits | DSN-DVM-008 | SIM |
| no lost update; linear history; dense `cseq` | DSN-DVM-208, DSN-DVM-510, DSN-DVM-006 | SIM, FORM |
| serializable processes have no anomalies (Elle-style checker) | DSN-DVM-511 | SIM |
| timeline operations never overlap | DSN-DVM-303 | PROP, FORM |
| past `(cseq, asof)` answers never change | DSN-DVM-305, DSN-DVM-326 | PROP, FORM |
| balances equal sums of entries | DSN-DVM-420, DSN-DVM-421 | PROP |
| gap-free sequences under faults | DSN-DVM-402 | FAULT |
| merge determinism and ledger completeness | DSN-DVM-240, DSN-DVM-244, DSN-DVM-249 | PROP, FORM |
| diff cost proportional to change | DSN-DVM-230 | BENCH |
| row filter equals per-object check | DSN-DVM-820 | PROP |
| cursor pages are complete and unique | DSN-DVM-908, DSN-DVM-262 | PROP |
| replay reproduces outcome hashes | DSN-DVM-631, DSN-DVM-632 | CONF |
| caches never change results | DSN-DVM-009 | CONF (cache on vs off) |

## 4. Performance gates

| Workload | NR | Environment | Gate |
|---|---|---|---|
| point reads, 10 M objects, 4-deep branch chain | NR-PERF-001 | ENV-REF-SERVER | p95 target met |
| single-object commits | NR-PERF-002 | ENV-REF-SERVER | p95 target met; per-step budget (DSN-DVM-504) |
| 500 concurrent processes on `main` | NR-PERF-003 | ENV-REF-SERVER | throughput target met |
| branch creation on 10 M objects | NR-PERF-004 | ENV-REF-SERVER | target met |
| diff and merge preview of 1,000 changes in 10 M | NR-PERF-005, NR-PERF-006 | ENV-REF-SERVER | targets met |
| bitemporal reads | NR-PERF-007 | ENV-REF-SERVER | overhead target met |
| indexed and polymorphic queries with row filters | NR-PERF-008 | ENV-REF-SERVER | target met |
| 5,000-rule sheet evaluation | NR-PERF-009 | ENV-REF-SERVER | target met |
| L2 script overhead | NR-PERF-010 | ENV-REF-SERVER | target met |
| Box start-up, browser local reads | NR-PERF-023, NR-PERF-024 | ENV-REF-BOX, ENV-REF-BROWSER | targets met (PH-3, PH-5) |

## 5. Phase exit contributions

| Phase | DVM evidence required |
|---|---|
| PH-0 | prototype benchmarks validating DEC-020 and ASM-003; FORM models for commit/CAS, merge and timelines checked; storage and tree prototype passing SUITE-DVM-PROP core properties |
| PH-1a | SUITE-DVM-CONF 100% for Core, Bitemporal, Ledger and Proof (roots, signatures, inclusion); SUITE-DVM-STORE 100% on PostgreSQL; SIM 10,000 seeds with no violation; FAULT matrix passed; BENCH gates NR-PERF-001…008; export and import round trip equal |
| PH-1b | CONF for Governance profile (sheets, decisions, lifecycles); SEC permission matrix 100%; L2 escape suite passed; NR-PERF-009, NR-PERF-010, NR-PERF-014 |
| PH-2 | lenses, decision tables, approvals, deferred commits, sagas, ReBAC, masking: CONF and PROP; FORM for approvals and sagas; replay corpus 100%; Basic Finance scenario suite (SUITE-PH2-FINANCE) passes on the DVM |
| PH-3 | L3 escape suite and cross-architecture determinism; crypto-shredding with proofs; holds and retention |
| PH-4 | SQLite adapter passes SUITE-DVM-STORE and SUITE-DVM-CONF; sync structures under SIM and NR-PERF-018 (DEC-024); tiering reads (NR-PERF-021); Cell-scale BENCH (NR-SCAL-001, NR-SCAL-002); SUITE-DVM-XIMPL with the third-party embedding build |
| PH-5 | browser build passes SUITE-DVM-CONF; shared-BPU structures under SIM and FORM |

## 6. Environments

| Use | Environment |
|---|---|
| primary benchmarks and fault injection with PostgreSQL | ENV-REF-SERVER |
| small-tenant footprint (NR-COST-003) | ENV-REF-SMALL |
| SQLite edition benchmarks | ENV-REF-BOX |
| device and browser builds | ENV-REF-MOBILE, ENV-REF-BROWSER |
| Cell-scale runs (PH-4) | ENV-REF-CELL |

## 7. Defect policy

- A conformance, property, simulation or formal-model failure is a release blocker, whatever
  its apparent severity.
- Every simulation failure is stored with its seed and becomes a permanent regression case.
- A benchmark regression over 10% against the previous release blocks the release unless
  a decision record accepts it.
