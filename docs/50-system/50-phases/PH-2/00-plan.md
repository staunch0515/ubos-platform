---
id: UBS-PH-2
title: PH-2 — First Verticals and Production Server
status: draft
phase: PH-2
depends_on: [UBS-PH-00, UBS-PH-1]
---

# PH-2 — First Verticals and Production Server

## 1. Goal and business value

PH-2 ships the first sellable product (DEC-016): a highly available Server with the
Workspace, Studio and Bridge, and two first-vertical Buks — Fund Operations and Contract
Management (DEC-015) — built entirely as Buks. It delivers business goal **G2**: the verticals
run with zero kernel code specific to them. It also completes the governance kernel (lenses,
decision tables, approvals, workflows, sagas, ReBAC, masking, crypto-shredding, simulation)
and the production edge (REST, webhooks, SSO, SCIM, notifications, files, search).

Measurable outcomes:
1. Both vertical Buks pass their scenario families (SCN-1xx, SCN-2xx) on an HA Server, with
   0 lines of vertical-specific kernel code (MET-BIZ-001).
2. BPA 1.0 final is released with vectors for all PH-2 kernel features (MET-QUAL-020).
3. Usability: ≥ 90% unassisted task success and SUS ≥ 75 for business users (NR-USE-001,
   NR-USE-002).
4. HA and DR: node loss causes no failed requests beyond retries; RPO/RTO met (NR-AVAIL-002,
   NR-DUR-002, NR-DUR-003).
5. A design partner (ASM-002) has migrated representative legacy data with a signed
   reconciliation report (MET-BIZ-002).

## 2. Systems and components in scope

| System | Components | Depth |
|---|---|---|
| BPA | 1.0 final; errata process; extension registry for first-party Buks | hardened |
| DVM | lenses and schema evolution, decision tables, approvals and deferred commits, workflows, sagas, ReBAC, masking, crypto-shredding structures, simulation, projections at scale, impact and restatement, matching engine, periods and close | extended |
| NOD | HA Server, REST facade, webhooks, brokers, OIDC, SAML, SCIM, step-up, KMS adapters, notifications and action messages, files (scanning, previews, extraction), text search, DR, rolling upgrades, bank and fund file formats | extended |
| FRG | Buk pack, sign, verify, dependency solver, simulator, replay debugger, LSP, VS Code extension, docs, Forge service | extended |
| SDK | optimistic UI support, skew suite | extended |
| WSP | production workspace: renderer, catalogue 1.0, shell, forms, lists, details, timeline and as-of, diff review, tasks, explanations, Live Doc, clauses, rendering, e-signatures, localisation, accessibility | new |
| STU | model, sheet, decision-table, lifecycle, view and script editors; change sets; simulation; releases; integrations; import wizard | new |
| BRG | spreadsheet import, bulk loads with history, mappings, reconciliation, mail and chat adapters | new |
| NTY | evidence packs in NOD, verifier CLI | prototype |
| Buks | `fund-ops` (share classes, investors, orders, NAV, fees, restrictions, close, reconciliation), `contracts` (templates, clauses, approvals, signatures, obligations) | new |

## 3. Feature list

The generated list is UBS-PHF-2: 274 FR, 51 NR, 20 CR and 22 scenarios. Design coverage: all
DSN items with **Phase** PH-2 across DVM, NOD, FRG, SDK, WSP, STU and BRG.

The two Buks are specified by the scenario families SCN-101…110 and SCN-201…208 and built
only from kernel features; any kernel gap found while building them is raised as a standard
change (DSN-BPA-010), never as Buk-specific kernel code.

## 4. Dependencies

| Dependency | Kind | Needed by |
|---|---|---|
| EXIT-1-01…20 | prior exit | start |
| design partner committed (ASM-002) with anonymised data (ASM-012) | external | month 1 |
| HA reference environment ENV-REF-SERVER with standby site | external | month 3 |
| accessibility auditor and usability-study participants (business users, approvers, analysts) | external | month 6 |
| legal review of E-SIGN/UETA ceremony design (CR-ESIGN-001…004) | external | month 5 |
| penetration-test vendor | external | month 8 |

## 5. Out of scope for this phase

- Box desktop and mobile apps, offline sync, external portal, Smart Grid, War Room, Action
  Messages, negotiation rounds (PH-3).
- AI of any kind (`ask.model` returns `LOGIC.NOT_AVAILABLE`), vector search (PH-3).
- L3 WASM components (PH-3).
- Anchoring service, legal holds, WORM retention jobs (PH-3).
- Multi-tenant Cells, Control Plane, public Exchange, workers (PH-4).

## 6. Deliverables

| Deliverable | Form |
|---|---|
| BPA 1.0 | signed release; conformance package |
| UBOS Server 1.0 | signed binaries, OCI images, Helm chart, installation and operations guides, runbooks |
| Workspace and Studio 1.0 | web clients served by the Server |
| Forge 1.0 and VS Code extension | signed binaries, marketplace listing |
| SDKs 1.0 (Rust, TypeScript) | crates.io and npm packages |
| `fund-ops` and `contracts` Buks 1.0 | signed, verified Buk archives |
| Migration toolkit (BRG) | import wizard, mappings, reconciliation reports |
| Evidence | scenario, performance, security, accessibility, usability and DR reports |

## 7. Verification plan

### 7.1 Methods used and why

| Method | Why in PH-2 |
|---|---|
| CONF | new kernel features become standard (1.0 final) |
| PROP, SIM, FORM | lenses, merge with schemas, approvals and sagas are new correctness risks |
| FAULT | HA failover, split brain, DR, rolling upgrades |
| SCN | the phase goal is defined by the vertical scenario families |
| BENCH | throughput and scale targets (NR-PERF-003, NR-SCAL-*) enter in PH-2 |
| SEC | first externally exposed product: penetration test, SSO, SCIM, masking, erasure |
| USE | first business-user product: usability and accessibility targets |
| INSP | legal review of signatures, accessibility conformance report, SBOM and provenance |
| PILOT (preparation) | design-partner data migration as the entry to the PH-3 pilot |

### 7.2 Suites and verification ranges

| Scope | Suite | VER range |
|---|---|---|
| standard vectors for lenses, decision tables, approvals, deferred commits, sagas, ReBAC, masking (new clauses of 1.0 in their areas) | SUITE-DVM-CONF | VER-CONF-0001…6299 (all 1.0 areas) |
| new kernel properties (lens application, row filters, approvals) | SUITE-DVM-PROP | VER-PROP-0400…0599 |
| approvals and sagas models | SUITE-DVM-FORM | VER-FORM-0100…0199 |
| HA, failover, split brain, upgrades, DR | SUITE-NOD-OPS | VER-FAULT-2100…2299 |
| REST, webhooks, brokers, SSO, SCIM | SUITE-NOD-PROTO, SUITE-NOD-AUTH, SUITE-NOD-EVENTS | VER-CONF-7100…7299, VER-SEC-2200…2299 |
| files and search | SUITE-NOD-FILES | VER-CONF-7300…7349, VER-SEC-2300…2349 |
| penetration test | SUITE-NOD-PEN | VER-SEC-2900…2999 |
| Buk tooling | SUITE-FRG-REPRO, SUITE-FRG-RESOLVE, SUITE-FRG-DX | VER-CONF-7800…7899, VER-PROP-4000…4049, VER-BENCH-4000…4049 |
| workspace end to end | SUITE-WSP-E2E | VER-SCN-2500…2699 |
| accessibility, rendering, performance, security | SUITE-WSP-A11Y, SUITE-WSP-RENDER, SUITE-WSP-PERF, SUITE-WSP-SEC | VER-INSP-5000…5099, VER-CONF-9000…9049, VER-BENCH-5000…5049, VER-SEC-5000…5049 |
| usability studies | SUITE-WSP-USE, SUITE-STU-USE | VER-USE-2000…2049 |
| migration | SUITE-BRG-MIG | VER-SCN-2700…2749 |
| vertical scenario families | SUITE-PH2-FUND, SUITE-PH2-CONTRACTS | VER-SCN-2000…2199 |
| edge and kernel benchmarks at scale | SUITE-NOD-BENCH, SUITE-DVM-BENCH | VER-BENCH-2100…2199, VER-BENCH-0200…0299 |

VER numbering follows the allocation of the verification volume (UBS-VER-00 §3).

### 7.3 Environments and datasets

| Use | Environment |
|---|---|
| HA, failover, DR, scale benchmarks | ENV-REF-SERVER (with standby site) |
| installation and small-customer profile | ENV-REF-SMALL |
| client-perceived latency and usability sessions | ENV-REF-WAN |

Datasets: WL-FUND-M (fund-operations workload), WL-CONTRACT-M (contract workload),
WL-GENERIC-10M scaled to NR-SCAL-001 PH-2 size (50,000,000 current objects), the design
partner's anonymised extracts, and the rendering vector set.

### 7.4 Pass thresholds

| Check | Threshold |
|---|---|
| BPA 1.0 vectors | 100% pass on both backends, including all PH-1 vectors |
| vertical scenarios | 100% of SCN-101…110 (PH-2 subset) and SCN-201…208 (PH-2 subset) pass on the HA Server |
| Buk purity | 0 kernel commits referencing vertical names or Buk-specific logic (automated scan plus inspection) |
| throughput | NR-PERF-003 met for 30 minutes |
| scale | NR-SCAL-001 (PH-2 target), NR-SCAL-003, NR-SCAL-004, NR-SCAL-005 met |
| availability | NR-AVAIL-002, NR-AVAIL-003 and NR-AVAIL-006 met in the fault matrix |
| durability | NR-DUR-002 and NR-DUR-003 met in the DR drill; NR-DUR-005 audit running |
| security | NR-SEC-001: 0 open critical or high findings; NR-SEC-005, -006, -007 (SLSA L2), -011 met |
| privacy | NR-PRIV-001, -002, -005 met in the privacy scenario (SCN-016) |
| usability | NR-USE-001 ≥ 90%; NR-USE-002 SUS ≥ 75 (business) and ≥ 70 (builders); NR-USE-004 ≤ 1 working day; NR-USE-006 and NR-USE-007 met |
| accessibility | NR-ACC-001 and NR-ACC-002 met; Accessibility Conformance Report published |
| operations | NR-OPER-001, -002, -004 met |
| determinism of rendering | NR-DET-004 met |

### 7.5 Acceptance

The Phase Acceptance Board, including the design-partner representative, accepts PH-2. The
design partner signs acceptance of the migration reconciliation report and of a
demonstration of the fund-operations scenarios on their anonymised data. Legal counsel
signs the E-SIGN/UETA review. Records are DEC entries with evidence hashes.

### 7.6 Regression policy

All PH-1 suites and scenarios re-run on every PH-2 release candidate; kernel minors must
pass the vertical Buks' scenarios before release (NR-COMPAT-004).

## 8. Metrics

| Metric | Name | Target |
|---|---|---|
| MET-BIZ-001 | vertical-specific kernel code | 0 lines |
| MET-BIZ-002 | design-partner migration reconciliation differences unexplained | 0 |
| MET-QUAL-020 | BPA 1.0 vectors passing on both backends | 100% |
| MET-QUAL-021 | vertical scenarios passing on HA Server | 100% |
| MET-PERF-020 | sustained simple commits/s (NR-PERF-003) | ≥ 2,000 |
| MET-PERF-021 | sustained ledger entries/s (NR-PERF-003) | ≥ 5,000 |
| MET-PERF-022 | detail view server time p95 (NR-PERF-014) | ≤ 150 ms |
| MET-PERF-023 | commit to client update p95 on WAN (NR-PERF-015) | ≤ 1 s |
| MET-OPS-020 | write unavailability on primary loss (NR-AVAIL-002) | ≤ 60 s |
| MET-OPS-021 | DR RTO (NR-DUR-003) | ≤ 30 min |
| MET-USE-001 | unassisted task success, business users (NR-USE-001) | ≥ 90% |
| MET-USE-002 | SUS business / builders (NR-USE-002) | ≥ 75 / ≥ 70 |
| MET-USE-003 | new class with lifecycle, 3 rules and views released by a trained architect (NR-USE-004) | ≤ 1 working day |
| MET-SEC-020 | open critical or high penetration-test findings | 0 |

## 9. Exit criteria

| Exit | Criterion | Evidence |
|---|---|---|
| EXIT-2-01 | BPA 1.0 is released; 100% of its vectors pass on PostgreSQL and SQLite | release manifest, runner reports |
| EXIT-2-02 | New formal models (approvals, sagas) checked with 0 violations | model reports |
| EXIT-2-03 | `fund-ops` and `contracts` Buks are verified by Forge and pass all PH-2 scenarios of SCN-1xx and SCN-2xx on the HA Server | Buk verification records, scenario report |
| EXIT-2-04 | 0 lines of vertical-specific kernel code (MET-BIZ-001) | code scan and inspection record |
| EXIT-2-05 | NR-PERF-003, -013, -014, -015, -016, -017 met | benchmark report |
| EXIT-2-06 | NR-SCAL-001 (PH-2), -003, -004, -005 met | scale report |
| EXIT-2-07 | HA fault matrix, split-brain test and rolling upgrade pass; DR drill meets RPO and RTO | fault and DR reports |
| EXIT-2-08 | Penetration test with 0 open critical or high findings; SBOM and signed artifacts with SLSA L2 provenance | pen-test report, supply-chain report |
| EXIT-2-09 | Privacy scenario SCN-016 passes with NR-PRIV-001, -002 and -005 met | scenario and privacy report |
| EXIT-2-10 | Usability study meets MET-USE-001…003 | study report |
| EXIT-2-11 | WCAG 2.2 AA audit passed; Accessibility Conformance Report published | audit report, ACR |
| EXIT-2-12 | E-SIGN/UETA ceremony design approved by legal counsel; SCN-204 passes including independent verification | legal memo, scenario report |
| EXIT-2-13 | Design partner accepts the migration reconciliation (MET-BIZ-002) | signed acceptance |
| EXIT-2-14 | Installation (≤ 30 min) and rolling upgrade (≤ 60 min operator time) meet NR-OPER-001 and NR-OPER-002; all alerts link to runbooks | operations report |
| EXIT-2-15 | Traceability report shows every PH-2 Must requirement covered by passing verification items, and all PH-1 suites still pass | traceability and regression reports |

## 10. Risks and mitigations

| Risk | Description | L | I | Mitigation | Early warning | Owner |
|---|---|---|---|---|---|---|
| RSK-020 | Vertical needs a kernel feature not in the standard | H | M | fast standard change process with vectors; kernel team embedded with Buk team | Buk backlog items tagged "kernel gap" > 5 open | product owner |
| RSK-021 | Business users find branching concepts confusing (ASM-007) | M | H | progressive disclosure, business vocabulary, early usability tests at month 3 and 6 | SUS < 70 in the month-3 test | design lead |
| RSK-022 | Design partner delays or withdraws (ASM-002) | M | H | two partner candidates; synthetic WL-FUND-M as fallback | no signed data agreement by month 2 | product owner |
| RSK-023 | Lens and schema-merge complexity causes defects | M | H | property tests with generated schemas; FORM for schema alignment | property failures in nightly runs | kernel lead |
| RSK-024 | Accessibility retrofitting late | M | M | accessible widget catalogue from the start; axe checks in CI | axe violations trending up | design lead |
| RSK-025 | E-SIGN ceremony not accepted by counsel | L | H | early legal review at design time; standard patterns | review comments requiring redesign | legal lead |

## 11. Reference duration and team assumption

Non-normative. 9 months. Team of 14–16: 4 kernel, 3 node/platform, 3 front-end (WSP, STU),
2 Buk developers with domain analysts (fund operations, contracts), 1 Forge, 1 quality, 1
security, plus design, the standards steward, the engineering lead and the product owner.
The growth beyond the 8–12 core follows ASM-001.
