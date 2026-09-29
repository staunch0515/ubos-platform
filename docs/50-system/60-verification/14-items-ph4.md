---
id: UBS-VER-14
title: Verification Items — PH-4
status: draft
phase: PH-4
depends_on: [UBS-VER-01, UBS-PH-4]
---

# Verification Items — PH-4

### VER-BENCH-0300 — Multi-tenant scale and fairness
- **Verifies:** NR-SCAL-002, NR-PERF-022, NR-COST-003, FR-TEN-024, DSN-NOD-002, DSN-NOD-222
- **Suite:** SUITE-DVM-BENCH, SUITE-NOD-BENCH · **Phase:** PH-4
- **Environment:** ENV-REF-CELL with WL-TENANTS-1K
- **Procedure:**
  1. Load 1,000 small and 3 large tenants; run WL-MIXED-OLTP per tenant.
  2. Drive one large tenant to 10× load; measure small-tenant p95 read latency and job start latency.
  3. Measure marginal memory and storage per idle small tenant.
- **Pass criterion:** NR-PERF targets met for all tenants; small-tenant latency increase ≤ 30%; job start p95 ≤ 5 s; NR-COST-003 met.
- **Evidence:** scale report.
- **Supports:** EXIT-4-01

### VER-FAULT-7500 — Online tenant migration
- **Verifies:** FR-TEN-072, NR-AVAIL-004, DSN-CTL-203, DSN-NOD-411, SCN-404
- **Suite:** SUITE-CTL-ORCH, SUITE-PH4-PLATFORM · **Phase:** PH-4
- **Environment:** two ENV-REF-CELL instances
- **Procedure:**
  1. Migrate 10 test tenants up to 1 TB under write load, injecting CTL and node crashes at each step in some runs.
  2. Migrate one real tenant during a maintenance window agreed with the customer.
  3. Compare roots at cut-over; measure write pause.
- **Pass criterion:** all migrations complete or abort safely; equal roots; write pause ≤ 30 s; reads uninterrupted.
- **Evidence:** migration reports.
- **Supports:** EXIT-4-02

### VER-SEC-7500 — Control Plane blindness and orchestration safety
- **Verifies:** AR-006, CST-017, DSN-CTL-001, DSN-CTL-003, DSN-NOD-641
- **Suite:** SUITE-CTL-BLIND · **Phase:** PH-4
- **Environment:** staging Cell and CTL
- **Procedure:**
  1. Architecture inspection of every CTL credential and interface.
  2. Penetration test targeting business-data access from CTL and operator tooling.
  3. Attempt destructive operations with a single operator.
- **Pass criterion:** 0 business-data paths; destructive operations require two approvals.
- **Evidence:** inspection record, pen-test report.
- **Supports:** EXIT-4-03

### VER-CONF-8700 — Usage chain and rating reproducibility
- **Verifies:** FR-BILL-011, FR-BILL-012, DSN-CTL-401, DSN-CTL-402, DSN-CTL-404
- **Suite:** SUITE-CTL-USAGE · **Phase:** PH-4
- **Environment:** staging Cell and CTL
- **Procedure:**
  1. Push usage with duplicates, gaps and tampered records; import air-gapped files.
  2. Rate the same usage twice.
- **Pass criterion:** duplicates ignored; gaps and tampering detected; identical rating results.
- **Evidence:** suite report.
- **Supports:** EXIT-4-03

### VER-CONF-7900 — Exchange end to end with third parties
- **Verifies:** SCN-403, FR-PKG-061…063, FR-BILL-031, FR-BILL-032, FR-BILL-041, FR-BILL-042, DSN-EXC-001…009
- **Suite:** SUITE-EXC-PUB, SUITE-EXC-VERIFY, SUITE-EXC-LIC, SUITE-EXC-MONEY · **Phase:** PH-4
- **Environment:** production Exchange (pilot publishers)
- **Procedure:**
  1. Three third-party publishers publish Buks; EXC verifies twice on different workers.
  2. Customers license them; nodes validate licences online and offline.
  3. Invoice customers; pay out publishers; reconcile the EXC ledger.
- **Pass criterion:** ≥ 3 Buks listed and paid out; verification records identical; ledger balanced; statements match payouts.
- **Evidence:** Exchange reports, publisher statements.
- **Supports:** EXIT-4-04

### VER-CONF-8300 — Certification of the reference DVM and a third party
- **Verifies:** SCN-407, FR-STD-051, FR-STD-052, FR-STD-053, DSN-BPA-008, STD-CONF-020
- **Suite:** SUITE-BPA-CERT · **Phase:** PH-4
- **Environment:** certification environment
- **Procedure:**
  1. The vendor submits a signed report; the steward reproduces the run from the same source.
  2. Issue certificates for passing profiles; publish in the registry; verify certificates with the public API.
- **Pass criterion:** reference DVM certified; ≥ 1 third party certified (MET-BIZ-021).
- **Evidence:** certificates, reproduction reports.
- **Supports:** EXIT-4-05

### VER-SEC-2400 — Worker result verification
- **Verifies:** SCN-410, FR-EVT-061…065, DSN-NOD-240…245
- **Suite:** SUITE-NOD-JOBS · **Phase:** PH-4
- **Environment:** customer-like network with a worker pool
- **Procedure:**
  1. Run a batch on 10 workers; make one worker return corrupted results and one run a tampered component.
- **Pass criterion:** corrupted results detected by sampling or hash mismatch; tampered component refused; offending workers quarantined; batch completes correctly.
- **Evidence:** worker report.
- **Supports:** EXIT-4-06

### VER-BENCH-7500 — Cell availability SLO
- **Verifies:** NR-AVAIL-001 (PH-4)
- **Suite:** SUITE-CTL-SCALE · **Phase:** PH-4
- **Environment:** production Cell
- **Procedure:**
  1. Measure availability with synthetic probes from three regions every 30 s over 60 days.
- **Pass criterion:** ≥ 99.9% (MET-OPS-040).
- **Evidence:** SLO report.
- **Supports:** EXIT-4-07

### VER-SEC-9500 — Residency, supply chain and key custody
- **Verifies:** NR-PRIV-004, NR-SEC-006, NR-SEC-007 (SLSA L3), CR-GDPR-008
- **Suite:** SUITE-REGRESSION · **Phase:** PH-4
- **Environment:** production Cell and build system
- **Procedure:**
  1. Audit storage, processing and model-call locations against residency policies using egress logs and cloud inventories.
  2. Verify SLSA L3 provenance for all artifacts.
  3. Inspect key custody (HSM or KMS) for all platform signing keys.
- **Pass criterion:** 0 out-of-region findings; 100% artifacts with L3 provenance; 100% platform keys in HSM or KMS.
- **Evidence:** security report.
- **Supports:** EXIT-4-08

### VER-INSP-9503 — SOC 2 readiness
- **Verifies:** CR-SOC2-001…010
- **Suite:** SUITE-REGRESSION · **Phase:** PH-4
- **Environment:** organisation and Cell service
- **Procedure:**
  1. External auditor readiness assessment over controls, policies and evidence.
- **Pass criterion:** 0 high exceptions; audit period started.
- **Evidence:** auditor letter.
- **Supports:** EXIT-4-09

### VER-INSP-5002 — Localisation QA
- **Verifies:** NR-LOC-001, NR-LOC-002, FR-LOC-011
- **Suite:** SUITE-WSP-E2E · **Phase:** PH-4
- **Environment:** staging
- **Procedure:**
  1. Automated scan for externalised strings; linguistic QA of business and builder screens in ES, FR, DE, JA and ZH.
- **Pass criterion:** 100% externalised; 0 blocking linguistic issues.
- **Evidence:** localisation QA report.
- **Supports:** EXIT-4-10

### VER-INSP-9504 — PH-4 traceability and regression
- **Verifies:** all PH-4 Must FR, NR and CR; all earlier suites
- **Suite:** SUITE-REGRESSION · **Phase:** PH-4
- **Environment:** CI
- **Procedure:**
  1. Generate the traceability report; confirm all earlier suites passed.
- **Pass criterion:** no uncovered Must requirement; all earlier suites green.
- **Evidence:** traceability and regression reports.
- **Supports:** EXIT-4-11
