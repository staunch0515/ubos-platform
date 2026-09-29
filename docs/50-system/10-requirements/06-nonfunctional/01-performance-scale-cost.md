---
id: UBS-REQ-06-01
title: Non-Functional Requirements — PERF, SCAL, COST
status: review
phase: PH-1
depends_on: [UBS-REQ-06]
---

# Non-Functional Requirements — Performance, Scalability, Cost

## PERF — Performance

### NR-PERF-001 — Point read latency
- **Statement:** Reading one object version by URI at the branch head MUST be fast enough for interactive use.
- **Target:** p95 ≤ 5 ms and p99 ≤ 20 ms at the server boundary; WL-GENERIC-10M under WL-MIXED-OLTP at 70% of maximum sustainable throughput; ENV-REF-SERVER.
- **Rationale:** Interactive UI and logic reads.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, NOD
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given the workload, when benchmarked for 30 minutes, then the percentiles meet the target.
- **Verification:** BENCH
- **Origin:** NEW

### NR-PERF-002 — Simple commit latency
- **Statement:** A commit of 1–10 Definition objects with validation, rule binding and synchronous projections MUST complete quickly.
- **Target:** p50 ≤ 15 ms and p99 ≤ 60 ms at 500 commits/s; WL-GENERIC-10M; ENV-REF-SERVER.
- **Rationale:** Responsive saves.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given the load, when benchmarked, then the percentiles meet the target.
- **Verification:** BENCH
- **Origin:** NEW

### NR-PERF-003 — Commit throughput
- **Statement:** The Server cluster MUST sustain high commit and append throughput on disjoint objects.
- **Target:** At least 2,000 simple commits/s and at least 5,000 ledger entries/s (batched per process), sustained 30 minutes with p99 commit latency ≤ 150 ms; ENV-REF-SERVER.
- **Rationale:** Dealing peaks and batch postings.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, NOD
- **Personas:** PER-FundOperationsManager
- **Acceptance:**
  1. Given the load generator, when run, then the throughput and latency targets hold.
- **Verification:** BENCH
- **Origin:** NEW

### NR-PERF-004 — Branch creation time
- **Statement:** Branch creation MUST be constant-time.
- **Target:** p99 ≤ 10 ms for sources of 1,000 objects and of 10,000,000 objects; ENV-REF-SERVER.
- **Rationale:** FR-VER-021.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-AiAgent
- **Acceptance:**
  1. Given both sizes, when 1,000 branches are created, then p99 ≤ 10 ms for each.
- **Verification:** BENCH
- **Origin:** IMP-03

### NR-PERF-005 — Diff performance
- **Statement:** Diffs MUST scale with change size.
- **Target:** Diff of 10 changes in a 10,000,000-object state ≤ 50 ms p95; summary of a 1,000,000-change diff ≤ 5 s; ENV-REF-SERVER.
- **Rationale:** FR-VER-043, FR-VER-045.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-Approver
- **Acceptance:**
  1. Given both cases, when benchmarked, then the targets hold.
- **Verification:** BENCH
- **Origin:** IMP-03

### NR-PERF-006 — Merge preview time
- **Statement:** Merge previews MUST be interactive for typical change sets.
- **Target:** Preview of a 1,000-object change set, with validation, ≤ 2 s p95; of 100,000 objects ≤ 60 s; ENV-REF-SERVER.
- **Rationale:** Review workflows.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-Approver
- **Acceptance:**
  1. Given both sizes, when previewed, then the targets hold.
- **Verification:** BENCH
- **Origin:** NEW

### NR-PERF-007 — Bitemporal read overhead
- **Statement:** Reads with `asof` and `time` MUST cost not much more than current reads.
- **Target:** p95 latency of bitemporal point reads ≤ 2× that of NR-PERF-001 on the same workload with 24 months of history.
- **Rationale:** Bitemporality is a daily feature, not a batch feature.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given WL-GENERIC-10M with history, when bitemporal reads are benchmarked, then the ratio ≤ 2.
- **Verification:** BENCH
- **Origin:** IMP-02

### NR-PERF-008 — Indexed query latency
- **Statement:** Indexed criteria queries MUST be fast.
- **Target:** Returning the first 100 rows of an indexed filter over 10,000,000 objects: p95 ≤ 100 ms; ENV-REF-SERVER.
- **Rationale:** Lists and searches.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given the query suite, when benchmarked, then p95 ≤ 100 ms.
- **Verification:** BENCH
- **Origin:** NEW

### NR-PERF-009 — Governance-sheet evaluation
- **Statement:** Effective-declaration evaluation MUST add little to commit latency.
- **Target:** ≤ 1 ms p95 per object for 20 candidate blocks within 10,000 total blocks.
- **Rationale:** FR-RULE-024.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given the synthetic sheet set, when evaluated, then the target holds.
- **Verification:** BENCH
- **Origin:** NEW

### NR-PERF-010 — Logic execution overhead
- **Statement:** Logic execution MUST have low fixed overhead.
- **Target:** L1 expression evaluation ≤ 10 µs median for typical expressions; warm L2 script invocation overhead ≤ 1 ms p95 excluding instruction work; ENV-REF-SERVER.
- **Rationale:** Rules run on every commit.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given micro-benchmarks, when run, then the targets hold.
- **Verification:** BENCH
- **Origin:** NEW

### NR-PERF-011 — Event delivery latency
- **Statement:** Events MUST reach subscribers promptly.
- **Target:** Commit → handler start p95 ≤ 500 ms and p99 ≤ 2 s under WL-MIXED-OLTP; ENV-REF-SERVER.
- **Rationale:** Near-real-time reactions.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** NOD
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given timestamped events, when measured, then the percentiles hold.
- **Verification:** BENCH
- **Origin:** NEW

### NR-PERF-012 — Became-effective timing
- **Statement:** Became-effective events MUST fire close to the valid time.
- **Target:** ≤ 5 s after the valid-from instant, p99.
- **Rationale:** FR-TIME-052.
- **Priority:** Should · **Phase:** PH-1 · **Systems:** NOD
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given 1,000 future-dated versions, when their time comes, then 99% fire within 5 s.
- **Verification:** BENCH
- **Origin:** NEW

### NR-PERF-013 — Search index freshness
- **Statement:** Full-text search MUST reflect commits quickly.
- **Target:** p95 ≤ 5 s from commit to searchable; ENV-REF-SERVER.
- **Rationale:** FR-QRY-023.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** DVM, NOD
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given commits under load, when polled, then 95% are searchable within 5 s.
- **Verification:** BENCH
- **Origin:** NEW

### NR-PERF-014 — View payload latency
- **Statement:** Server-driven UI payloads MUST be delivered quickly.
- **Target:** p95 ≤ 150 ms server time for detail views with up to 5 related lists; client-perceived p95 ≤ 1 s on ENV-REF-WAN.
- **Rationale:** Usable UI.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given WL-FUND-M, when detail views are loaded, then the targets hold.
- **Verification:** BENCH
- **Origin:** NEW

### NR-PERF-015 — Subscription update latency
- **Statement:** Live subscriptions MUST push changes promptly.
- **Target:** Commit → client update p95 ≤ 1 s on ENV-REF-WAN.
- **Rationale:** Live dashboards and collaboration.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** NOD, SDK
- **Personas:** PER-FundOperationsManager
- **Acceptance:**
  1. Given subscribed clients, when changes commit, then 95% arrive within 1 s.
- **Verification:** BENCH
- **Origin:** NEW

### NR-PERF-016 — Developer loop time
- **Statement:** The Forge edit–check–test loop MUST be fast.
- **Target:** ≤ 10 s for a reference Buk of 100 classes, 200 rules and 50 tests on ENV-REF-BOX.
- **Rationale:** FR-DEV-013.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** FRG
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given the reference Buk, when the loop is timed, then ≤ 10 s.
- **Verification:** BENCH
- **Origin:** NEW

### NR-PERF-017 — Simulation throughput
- **Statement:** Historical simulation MUST process realistic windows in reasonable time.
- **Target:** At least 1,000 replayed processes per minute per application node; a one-month NAV simulation on WL-FUND-M completes within 30 minutes on ENV-REF-SERVER.
- **Rationale:** SCN-106.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** FRG, DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given the simulation job, when run, then the targets hold.
- **Verification:** BENCH
- **Origin:** NEW

### NR-PERF-018 — Sync efficiency
- **Statement:** Sync MUST transfer data in proportion to the difference.
- **Target:** Synchronising 100 changed objects (average 4 KB) between replicas of 1,000,000 objects completes in ≤ 10 s on ENV-REF-WAN, with a transfer volume ≤ 2× the changed payload plus 200 KB.
- **Rationale:** FR-SYNC-011.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** NOD
- **Personas:** PER-FieldWorker
- **Acceptance:**
  1. Given the scenario, when measured, then the targets hold.
- **Verification:** BENCH
- **Origin:** IMP-07

### NR-PERF-019 — Agent kill-switch latency
- **Statement:** Stopping agents MUST be immediate.
- **Target:** ≤ 2 s from command to cancellation of all in-flight tool calls, p99.
- **Rationale:** FR-AI-022.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** AGT
- **Personas:** PER-AgentSupervisor
- **Acceptance:**
  1. Given 20 concurrent runs, when stopped, then all cancel within 2 s.
- **Verification:** BENCH, FAULT
- **Origin:** NEW

### NR-PERF-020 — Analytics performance
- **Statement:** The embedded analytics engine MUST handle medium datasets interactively.
- **Target:** Monthly aggregation over 50,000,000 ledger entries ≤ 30 s; typical dashboard queries ≤ 3 s p95; ENV-REF-SERVER.
- **Rationale:** FR-ANL-041.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** BRG
- **Personas:** PER-DataAnalyst
- **Acceptance:**
  1. Given WL-FUND-L entries, when aggregated, then the targets hold.
- **Verification:** BENCH
- **Origin:** IMP-09

### NR-PERF-021 — Tiered history reads
- **Statement:** Reads of tiered versions MUST remain usable.
- **Target:** p95 ≤ 2 s for single-version reads from the cold tier; ENV-REF-CELL.
- **Rationale:** FR-VER-092.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given tiered versions, when read, then p95 ≤ 2 s.
- **Verification:** BENCH
- **Origin:** IMP-08

### NR-PERF-022 — Fairness under noisy neighbours
- **Statement:** Small tenants MUST keep good latency while a large tenant is under heavy load.
- **Target:** Under WL-TENANTS-1K with one large tenant at 10× its normal load, small-tenant p95 read latency increases by ≤ 30%, and job start latency p95 ≤ 5 s; ENV-REF-CELL.
- **Rationale:** FR-TEN-024, FR-EVT-034.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** NOD, CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given the scenario, when measured, then the targets hold.
- **Verification:** BENCH
- **Origin:** NEW

### NR-PERF-023 — Box start-up
- **Statement:** The Box MUST start quickly.
- **Target:** Cold start to usable UI ≤ 3 s with 100,000 objects; first-time bootstrap ≤ 2 min on ENV-REF-BOX.
- **Rationale:** PER-PersonalUser success signal.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** NOD, WSP
- **Personas:** PER-PersonalUser
- **Acceptance:**
  1. Given the Box, when started 10 times, then all starts are ≤ 3 s.
- **Verification:** BENCH
- **Origin:** NEW

### NR-PERF-024 — Browser kernel local reads
- **Statement:** The browser kernel MUST serve local reads fast.
- **Target:** p95 ≤ 10 ms for point reads with a replicated scope of 200,000 objects on ENV-REF-BROWSER.
- **Rationale:** Local-first responsiveness.
- **Priority:** Should · **Phase:** PH-5 · **Systems:** DVM, SDK
- **Personas:** PER-PersonalUser
- **Acceptance:**
  1. Given the scope, when benchmarked, then p95 ≤ 10 ms.
- **Verification:** BENCH
- **Origin:** IMP-07

## SCAL — Scalability

### NR-SCAL-001 — Objects per VAE
- **Statement:** A VAE MUST support large numbers of objects and versions.
- **Target:** PH-2: 50,000,000 current objects and 500,000,000 versions with NR-PERF targets met. PH-4: 200,000,000 current objects and 2,000,000,000 versions, with tiering.
- **Rationale:** Large fund administrators and contract repositories.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a synthetic dataset of the stated size, when NR-PERF-001, NR-PERF-002 and NR-PERF-008 are run, then they pass.
- **Verification:** BENCH
- **Origin:** NEW

### NR-SCAL-002 — Tenants per cell
- **Statement:** A Cell MUST host many small tenants.
- **Target:** At least 1,000 tenants (WL-TENANTS-1K) on ENV-REF-CELL with NR-PERF targets met for all tenants.
- **Rationale:** SaaS economics.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** NOD, CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given WL-TENANTS-1K, when benchmarked, then all targets hold.
- **Verification:** BENCH
- **Origin:** IMP-08

### NR-SCAL-003 — Concurrent sessions
- **Statement:** A Server cluster MUST support many concurrent users.
- **Target:** 5,000 concurrent active sessions with subscriptions on ENV-REF-SERVER with NR-PERF-014 met.
- **Rationale:** Enterprise deployments.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** NOD
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given simulated users, when benchmarked, then the targets hold.
- **Verification:** BENCH
- **Origin:** NEW

### NR-SCAL-004 — Horizontal scaling
- **Statement:** Application nodes MUST scale horizontally.
- **Target:** Read throughput scales from 1 to 8 application nodes with at least 0.8 linear efficiency; commit throughput with at least 0.6 efficiency on disjoint objects.
- **Rationale:** Growth without redesign.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** NOD
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given 1, 2, 4 and 8 nodes, when benchmarked, then the efficiency targets hold.
- **Verification:** BENCH
- **Origin:** NEW

### NR-SCAL-005 — Model size
- **Statement:** A VAE MUST support large models.
- **Target:** 5,000 classes, 20,000 extensions, 10,000 governance-sheet blocks and 5,000 logic assets with NR-PERF-009 met.
- **Rationale:** Many Buks in one VAE.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given a synthetic model of that size, when used, then the targets hold.
- **Verification:** BENCH
- **Origin:** NEW

### NR-SCAL-006 — Branch count
- **Statement:** A VAE MUST support many branches.
- **Target:** 100,000 branches (active and archived) per VAE without degrading NR-PERF-001.
- **Rationale:** One branch per agent run.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** DVM
- **Personas:** PER-AgentSupervisor
- **Acceptance:**
  1. Given 100,000 branches, when head reads are benchmarked, then NR-PERF-001 holds.
- **Verification:** BENCH
- **Origin:** IMP-10

### NR-SCAL-007 — Ledger size
- **Statement:** Ledgers MUST support very long histories.
- **Target:** 1,000,000,000 entries per ledger class with NR-PERF-003 append targets met.
- **Rationale:** Decades of postings.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a synthetic billion-entry ledger, when appends are benchmarked, then the targets hold.
- **Verification:** BENCH
- **Origin:** NEW

### NR-SCAL-008 — Box data size
- **Statement:** The Box MUST handle substantial personal and small-team data.
- **Target:** 1,000,000 objects and 10 GB of files with NR-PERF-001 p95 ≤ 10 ms on ENV-REF-BOX.
- **Rationale:** Serious personal and field use.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** NOD
- **Personas:** PER-PersonalUser
- **Acceptance:**
  1. Given the dataset, when benchmarked, then the target holds.
- **Verification:** BENCH
- **Origin:** NEW

### NR-SCAL-009 — File storage per cell
- **Statement:** File storage MUST scale with the object store.
- **Target:** At least 100 TB per Cell without degradation of file-read latency (p95 ≤ 300 ms to first byte).
- **Rationale:** Document-heavy tenants.
- **Priority:** Should · **Phase:** PH-4 · **Systems:** NOD
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a filled store, when reads are benchmarked, then the target holds.
- **Verification:** BENCH
- **Origin:** NEW

## COST — Resource efficiency

### NR-COST-001 — Versioning storage overhead
- **Statement:** Full history MUST be affordable.
- **Target:** For WL-GENERIC-10M (10% monthly change over 24 months), total authoritative storage ≤ 3.5× the size of the current state alone, with chunking and deduplication.
- **Rationale:** IMP-03 must deliver real savings.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given the workload, when storage is measured, then the ratio ≤ 3.5.
- **Verification:** BENCH
- **Origin:** IMP-03

### NR-COST-002 — Idle footprint
- **Statement:** Nodes MUST be lightweight at idle.
- **Target:** Box idle memory ≤ 300 MB; Server application node idle memory ≤ 512 MB; Box idle CPU ≤ 1%.
- **Rationale:** Laptops and dense hosting.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** NOD
- **Personas:** PER-PersonalUser, PER-PlatformOperator
- **Acceptance:**
  1. Given idle nodes for 10 minutes, when measured, then the targets hold.
- **Verification:** BENCH
- **Origin:** NEW

### NR-COST-003 — Small-tenant footprint
- **Statement:** Small tenants in a Cell MUST have a small marginal resource footprint.
- **Target:** Marginal steady-state memory per idle small tenant ≤ 5 MB; marginal storage equals its data plus ≤ 10 MB overhead.
- **Rationale:** SaaS unit economics.
- **Priority:** Should · **Phase:** PH-4 · **Systems:** NOD
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given 1,000 idle tenants, when measured, then the targets hold.
- **Verification:** BENCH
- **Origin:** IMP-08

### NR-COST-004 — Cost attribution completeness
- **Statement:** Every consumed resource MUST be attributable to a tenant.
- **Target:** At least 99% of measured compute, storage and AI cost attributed to tenant and category each month.
- **Rationale:** Billing and cost control.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-TenantAdministrator
- **Acceptance:**
  1. Given a month, when reconciled, then the unattributed share is ≤ 1%.
- **Verification:** AUDIT, INSP
- **Origin:** NEW
