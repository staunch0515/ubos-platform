---
id: UBS-VER-15
title: Verification Items — PH-5
status: draft
phase: PH-5
depends_on: [UBS-VER-01, UBS-PH-5]
---

# Verification Items — PH-5

### VER-FORM-8500 — Shared-BPU protocol model
- **Verifies:** STD-SYNC-011, STD-SYNC-012, FR-SYNC-071…075, DSN-FED-006
- **Suite:** SUITE-FED-SHARED · **Phase:** PH-5
- **Environment:** model-checking workstation
- **Procedure:**
  1. Model parties, proposals, signatures, concurrent proposals, party-appended ledger entries, crashes and partitions.
  2. Check: no Definition change effective without required signatures; deterministic ordering of concurrent proposals; convergence after healing; final copies on share end.
- **Pass criterion:** 0 violations at 3 parties with crash injection.
- **Evidence:** model and checker output.
- **Supports:** EXIT-5-01

### VER-SIM-8500 — Shared-BPU simulation
- **Verifies:** FR-SYNC-071…075, DSN-FED-006, DSN-DVM-955
- **Suite:** SUITE-FED-SHARED · **Phase:** PH-5
- **Environment:** simulation cluster
- **Procedure:**
  1. Simulate 2–5 parties with partitions, crashes and concurrent proposals over 1,000,000 seeds.
- **Pass criterion:** 0 divergence after convergence; 0 effective changes without signatures.
- **Evidence:** simulation report.
- **Supports:** EXIT-5-02

### VER-SEC-8500 — Trust enforcement
- **Verifies:** FR-SYNC-053, FR-SYNC-054, DSN-FED-003, DSN-FED-004
- **Suite:** SUITE-FED-TRUST · **Phase:** PH-5
- **Environment:** two ENV-REF-SERVER instances over ENV-REF-WAN
- **Procedure:**
  1. Attempt interactions outside the trust relationship, with a wrong DID certificate, and with revoked keys.
- **Pass criterion:** 100% refused and logged.
- **Evidence:** security report.
- **Supports:** EXIT-5-02

### VER-SIM-8600 — Remote parent fallback
- **Verifies:** FR-SYNC-061…064, STD-SYNC-010, DSN-FED-005
- **Suite:** SUITE-FED-FALLBACK · **Phase:** PH-5
- **Environment:** simulation cluster
- **Procedure:**
  1. Simulate parent outages from seconds to 24 hours with local work and reconnection.
- **Pass criterion:** local processes succeed with staleness markers; reconciliation converges.
- **Evidence:** simulation report.
- **Supports:** EXIT-5-02

### VER-PILOT-5000 — Shared contract BPU pilot
- **Verifies:** goal G6; SCN-409; MET-BIZ-030
- **Suite:** SUITE-FED-PILOT · **Phase:** PH-5
- **Environment:** the two organisations' production nodes
- **Procedure:**
  1. Agree success criteria and legal terms; operate a shared contract for ≥ 30 days with amendments requiring both signatures and party-appended payment entries.
  2. Compare roots after every convergence.
- **Pass criterion:** ≥ 30 days; equal roots after every convergence; both organisations sign acceptance.
- **Evidence:** signed pilot report.
- **Supports:** EXIT-5-03

### VER-SCN-5000 — Headquarters and subsidiary across nodes
- **Verifies:** SCN-408, MET-OPS-050
- **Suite:** SUITE-PH5-FED · **Phase:** PH-5
- **Environment:** two ENV-REF-SERVER instances over ENV-REF-WAN
- **Procedure:**
  1. Run the scenario including a 24-hour parent outage with local work and reconciliation.
- **Pass criterion:** 100% of local processes succeed during the outage; reconciliation completes.
- **Evidence:** scenario report.
- **Supports:** EXIT-5-04

### VER-CONF-8407 — Browser kernel conformance and performance
- **Verifies:** SCN-411, FR-SYNC-081…084, NR-PERF-024, DSN-DVM-956
- **Suite:** SUITE-DVM-CONF, SUITE-DVM-BENCH · **Phase:** PH-5
- **Environment:** ENV-REF-BROWSER
- **Procedure:**
  1. Run the claimed-profile vectors in the browser build.
  2. Measure point reads with a 200,000-object scope; simulate storage eviction.
- **Pass criterion:** 100% vectors pass; NR-PERF-024 met; eviction recovered by re-sync.
- **Evidence:** conformance and benchmark reports.
- **Supports:** EXIT-5-05

### VER-INSP-9002 — BPA 1.2 release and registry replication
- **Verifies:** DSN-BPA-001, DSN-FED-008
- **Suite:** SUITE-BPA-COVERAGE · **Phase:** PH-5
- **Environment:** release pipeline; registry operators
- **Procedure:**
  1. Release BPA 1.2 with federation vectors and full coverage.
  2. Confirm an independent operator replicates registry records and clients verify them by signature.
- **Pass criterion:** release signed with 100% coverage; independent replica operational.
- **Evidence:** release manifest, operator report.
- **Supports:** EXIT-5-06, EXIT-5-07

### VER-INSP-9505 — PH-5 traceability and regression
- **Verifies:** all PH-5 Must FR and NR; all earlier suites
- **Suite:** SUITE-REGRESSION · **Phase:** PH-5
- **Environment:** CI
- **Procedure:**
  1. Generate the traceability report; confirm all earlier suites passed.
- **Pass criterion:** no uncovered Must requirement; all earlier suites green.
- **Evidence:** traceability and regression reports.
- **Supports:** EXIT-5-08

### VER-SCN-5100 — Contract-only capabilities and E-SIGN/UETA review
- **Verifies:** CR-ESIGN-001, CR-ESIGN-002, CR-ESIGN-003, CR-ESIGN-004, FR-SIGN-011, DSN-WSP-501, SCN-204
- **Suite:** SUITE-WSP-E2E · **Phase:** PH-5
- **Environment:** ENV-REF-SERVER with the contract capabilities enabled; offline verification machine
- **Procedure:**
  1. Counsel reviews ceremony flows, consent texts, audit trail, retention and copies against E-SIGN and UETA (continues deferred item VER-INSP-5001).
  2. Run clause editing, the signature ceremony, the external portal and a War Room session end to end.
  3. Verify an executed PDF with the independent verifier on a machine without network.
- **Pass criterion:** signed legal memo with no open blocking comments; all flows pass; independent verification succeeds.
- **Evidence:** legal memo, scenario report, verifier report.
- **Supports:** EXIT-5-09
