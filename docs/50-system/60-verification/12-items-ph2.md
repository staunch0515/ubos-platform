---
id: UBS-VER-12
title: Verification Items — PH-2
status: draft
phase: PH-2
depends_on: [UBS-VER-01, UBS-PH-2]
---

# Verification Items — PH-2

### VER-CONF-8405 — BPA 1.0 conformance on both backends
- **Verifies:** all STD clauses of BPA 1.0 claimed profiles; FR-STD-011, FR-STD-041
- **Suite:** SUITE-DVM-CONF · **Phase:** PH-2
- **Environment:** ENV-REF-SERVER, ENV-REF-BOX
- **Procedure:**
  1. Run VER-CONF-0001…6299 (claimed profiles) on PostgreSQL and SQLite, including all PH-1 vectors.
  2. Run the previous release's vectors to confirm compatibility (DSN-BPA-006).
- **Pass criterion:** 100% pass; 0 regressions of rc vectors.
- **Evidence:** signed runner reports, release manifest.
- **Supports:** EXIT-2-01

### VER-FORM-0100 — Approvals and deferred commits model
- **Verifies:** STD-TXN-060, STD-VER-021, DSN-DVM-550, DSN-DVM-551, DSN-DVM-746, FR-FLOW-043, FR-FLOW-045
- **Suite:** SUITE-DVM-FORM · **Phase:** PH-2
- **Environment:** model-checking workstation
- **Procedure:**
  1. Model pending changes, approvals with preview hashes, concurrent edits, SoD constraints.
  2. Check: no commit without valid approvals; stale approvals never count; SoD always holds.
- **Pass criterion:** 0 violations at 2 approvers × 3 editors × 3 objects.
- **Evidence:** model and checker output.
- **Supports:** EXIT-2-02

### VER-FORM-0101 — Saga model
- **Verifies:** STD-TXN-080, DSN-DVM-564, DSN-DVM-566, FR-TXN-061, FR-TXN-063
- **Suite:** SUITE-DVM-FORM · **Phase:** PH-2
- **Environment:** model-checking workstation
- **Procedure:**
  1. Model a 3-step saga with timeouts and coordinator crashes.
  2. Check: every started saga ends completed or compensated; compensations run at most once per step.
- **Pass criterion:** 0 violations.
- **Evidence:** model and checker output.
- **Supports:** EXIT-2-02

### VER-SCN-2000 — Fund-operations scenario family
- **Verifies:** SCN-101, SCN-102, SCN-103, SCN-104, SCN-105, SCN-106, SCN-107, SCN-108, SCN-110; FR-LEDG, FR-TIME and FR-RULE requirements exercised by them
- **Suite:** SUITE-PH2-FUND · **Phase:** PH-2
- **Environment:** ENV-REF-SERVER (HA) with WL-FUND-M
- **Procedure:**
  1. Install the verified `fund-ops` Buk through a change set.
  2. Run each scenario's main and alternate flows via SDK scripts and Playwright.
  3. For SCN-103 and SCN-104 verify NAV lineage and restatement values against an independent spreadsheet calculation of the same inputs.
  4. For SCN-107 verify the sealed close artefact with the verifier CLI.
- **Pass criterion:** all steps pass; NAV values equal the independent calculation to the cent.
- **Evidence:** scenario report with trace IDs; calculation workbook hash.
- **Supports:** EXIT-2-03

### VER-SCN-2100 — Contract scenario family
- **Verifies:** SCN-201, SCN-202, SCN-204, SCN-205, SCN-206, SCN-208
- **Suite:** SUITE-PH2-CONTRACTS · **Phase:** PH-2
- **Environment:** ENV-REF-SERVER (HA) with WL-CONTRACT-M
- **Procedure:**
  1. Install the verified `contracts` Buk.
  2. Run all scenarios; for SCN-204 verify the executed PDF with the independent verifier on a separate machine without network.
- **Pass criterion:** all steps pass; independent verification succeeds.
- **Evidence:** scenario report; verifier report.
- **Supports:** EXIT-2-03, EXIT-2-12

### VER-INSP-0002 — Buk purity of the kernel
- **Verifies:** CST-005, MET-BIZ-001
- **Suite:** SUITE-PH2-FUND · **Phase:** PH-2
- **Environment:** repository
- **Procedure:**
  1. Scan kernel crates for vertical identifiers (fund, NAV, share class, contract, clause) outside tests and fixtures.
  2. Review every kernel change of PH-2 tagged "kernel gap" and confirm it is generic and standardised.
- **Pass criterion:** 0 findings.
- **Evidence:** inspection record.
- **Supports:** EXIT-2-04

### VER-BENCH-2100 — Throughput, freshness and experience latency
- **Verifies:** NR-PERF-003, NR-PERF-013, NR-PERF-014, NR-PERF-015, NR-PERF-016, NR-PERF-017
- **Suite:** SUITE-NOD-BENCH · **Phase:** PH-2
- **Environment:** ENV-REF-SERVER, ENV-REF-WAN, ENV-REF-BOX (developer loop)
- **Procedure:**
  1. Sustain 2,000 commits/s and 5,000 ledger entries/s for 30 minutes.
  2. Measure commit-to-searchable, detail-view server time, commit-to-client update over WAN, the developer loop for the reference Buk, and simulation throughput on WL-FUND-M.
- **Pass criterion:** all six NR targets met (MET-PERF-020…023).
- **Evidence:** benchmark report.
- **Supports:** EXIT-2-05

### VER-BENCH-0200 — Scale
- **Verifies:** NR-SCAL-001 (PH-2), NR-SCAL-003, NR-SCAL-004, NR-SCAL-005
- **Suite:** SUITE-DVM-BENCH · **Phase:** PH-2
- **Environment:** ENV-REF-SERVER (1 to 8 application nodes)
- **Procedure:**
  1. Load 50,000,000 current objects and 500,000,000 versions; re-run NR-PERF-001 and -002.
  2. Run 5,000 concurrent sessions with subscriptions.
  3. Scale application nodes 1→8 and measure read and commit scaling efficiency.
  4. Load a model of 5,000 classes and 10,000 sheet blocks; measure NR-PERF-009.
- **Pass criterion:** targets met; read efficiency ≥ 0.8, commit efficiency ≥ 0.6.
- **Evidence:** scale report.
- **Supports:** EXIT-2-06

### VER-FAULT-2100 — HA fault matrix, split brain and rolling upgrade
- **Verifies:** NR-AVAIL-002, NR-AVAIL-003, NR-AVAIL-006, FR-OPS-031, FR-OPS-061, FR-OPS-062, DSN-NOD-624, DSN-NOD-625, DSN-NOD-630, DSN-NOD-632
- **Suite:** SUITE-NOD-OPS · **Phase:** PH-2
- **Environment:** ENV-REF-SERVER (HA)
- **Procedure:**
  1. Under WL-MIXED-OLTP kill application nodes, the database primary, and partition the primary from its standby.
  2. Perform a rolling minor upgrade and a faulty release that must roll back automatically.
- **Pass criterion:** no failed requests beyond client retries on node loss; ≤ 60 s write unavailability on primary loss; only the legitimate primary accepts writes; 0 minutes of planned downtime; automatic rollback succeeds.
- **Evidence:** fault report.
- **Supports:** EXIT-2-07

### VER-FAULT-2101 — Disaster recovery drill
- **Verifies:** NR-DUR-002, NR-DUR-003, FR-OPS-063, DSN-NOD-626, SCN-406
- **Suite:** SUITE-NOD-OPS · **Phase:** PH-2
- **Environment:** ENV-REF-SERVER with standby site
- **Procedure:**
  1. Simulate loss of the primary site during load.
  2. Execute the DR runbook; measure RPO from the acknowledgement log and RTO from failure to service.
- **Pass criterion:** RPO and RTO within targets (MET-OPS-021).
- **Evidence:** DR drill report.
- **Supports:** EXIT-2-07

### VER-SEC-2900 — Penetration test and supply chain
- **Verifies:** NR-SEC-001, NR-SEC-007 (SLSA L2), NR-SEC-011, CR-SOC2-004
- **Suite:** SUITE-NOD-PEN · **Phase:** PH-2
- **Environment:** ENV-REF-SERVER staging with production configuration
- **Procedure:**
  1. External pen test of UBTP, REST, Workspace, SSO and SCIM endpoints, file upload and webhooks.
  2. Verify SBOMs and signatures for all release artifacts and SLSA L2 provenance.
- **Pass criterion:** 0 open critical or high findings; 100% artifacts signed with SBOM and provenance.
- **Evidence:** pen-test report, supply-chain report.
- **Supports:** EXIT-2-08

### VER-SCN-2200 — Privacy erasure and subject access
- **Verifies:** SCN-016, NR-PRIV-001, NR-PRIV-002, NR-PRIV-005, CR-GDPR-002, CR-GDPR-004, CR-CCPA-002, DSN-DVM-830, DSN-DVM-831
- **Suite:** SUITE-PH2-FUND · **Phase:** PH-2
- **Environment:** ENV-REF-SERVER
- **Procedure:**
  1. Create an investor with personal data across versions, documents and ledger references.
  2. Produce a subject export; measure time.
  3. Approve erasure; verify values erased in all versions and exports, proofs still verify, and read logging recorded all personal reads.
- **Pass criterion:** export ≤ 1 h; technical erasure ≤ 24 h; 100% proofs verify; 100% personal reads logged.
- **Evidence:** privacy report.
- **Supports:** EXIT-2-09

### VER-USE-2000 — Business-user and builder usability study
- **Verifies:** NR-USE-001, NR-USE-002, NR-USE-004, NR-USE-006, NR-USE-007
- **Suite:** SUITE-WSP-USE · **Phase:** PH-2
- **Environment:** ENV-REF-WAN with the HA Server
- **Procedure:**
  1. Run moderated sessions: ≥ 8 business users, ≥ 8 approvers, ≥ 6 business architects.
  2. Core tasks: create record, submit for approval, approve with diff, find a record, view history as of a date, correct an error; builders: new class with lifecycle, 3 rules and views.
- **Pass criterion:** MET-USE-001…003 met; ≥ 85% can state next action after an error; 0 forbidden terms found by the vocabulary scan.
- **Evidence:** study report.
- **Supports:** EXIT-2-10

### VER-INSP-5000 — Accessibility audit
- **Verifies:** NR-ACC-001, NR-ACC-002, CR-WCAG-001, CR-WCAG-002
- **Suite:** SUITE-WSP-A11Y · **Phase:** PH-2
- **Environment:** ENV-REF-BROWSER plus screen readers
- **Procedure:**
  1. Automated axe scans of all catalogue widgets and core screens.
  2. Manual audit of core tasks by keyboard only and with NVDA and VoiceOver by an external auditor.
  3. Publish the Accessibility Conformance Report (VPAT).
- **Pass criterion:** 0 AA violations; 100% of core tasks keyboard-completable; ACR published.
- **Evidence:** audit report, ACR.
- **Supports:** EXIT-2-11

### VER-INSP-5001 — E-SIGN and UETA legal review
- **Verifies:** CR-ESIGN-001, CR-ESIGN-002, CR-ESIGN-003, CR-ESIGN-004, FR-SIGN-011, DSN-WSP-501
- **Suite:** SUITE-PH2-CONTRACTS · **Phase:** PH-2
- **Environment:** staging with the contracts Buk
- **Procedure:**
  1. Counsel reviews ceremony flows, consent texts, audit trail, retention and copies against E-SIGN and UETA.
- **Pass criterion:** signed legal memo with no open blocking comments.
- **Evidence:** legal memo.
- **Supports:** EXIT-2-12

### VER-SCN-2700 — Design-partner migration reconciliation
- **Verifies:** FR-MIG-031, FR-MIG-033, FR-MIG-051, FR-MIG-052, FR-MIG-053, SCN-015, MET-BIZ-002
- **Suite:** SUITE-BRG-MIG · **Phase:** PH-2
- **Environment:** isolated partner environment
- **Procedure:**
  1. Import the partner's anonymised legacy extracts (funds, investors, holdings, 24 months of prices and NAVs, opening balances).
  2. Produce control totals and record-level reconciliation; seal the migration report.
- **Pass criterion:** 0 unexplained differences; partner signs acceptance.
- **Evidence:** sealed reconciliation report, signed acceptance.
- **Supports:** EXIT-2-13

### VER-SCN-2701 — Installation and upgrade operability
- **Verifies:** NR-OPER-001, NR-OPER-002, NR-OPER-004
- **Suite:** SUITE-NOD-OPS · **Phase:** PH-2
- **Environment:** ENV-REF-SMALL, ENV-REF-SERVER
- **Procedure:**
  1. A trained operator unfamiliar with the build installs a production-configured Server from the guide.
  2. The operator performs a rolling minor upgrade.
  3. Check that every shipped alert links to a runbook.
- **Pass criterion:** install ≤ 30 min; upgrade ≤ 60 min operator time without downtime; 100% alerts linked.
- **Evidence:** operations report.
- **Supports:** EXIT-2-14

### VER-INSP-9501 — PH-2 traceability and regression
- **Verifies:** all PH-2 Must FR, NR and CR; all PH-1 suites
- **Suite:** SUITE-REGRESSION · **Phase:** PH-2
- **Environment:** CI
- **Procedure:**
  1. Generate the traceability report; list uncovered Must requirements.
  2. Confirm all PH-1 suites passed on the release candidate.
- **Pass criterion:** empty list; all PH-1 suites green.
- **Evidence:** traceability and regression reports.
- **Supports:** EXIT-2-15
