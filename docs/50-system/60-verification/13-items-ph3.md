---
id: UBS-VER-13
title: Verification Items — PH-3
status: draft
phase: PH-3
depends_on: [UBS-VER-01, UBS-PH-3]
---

# Verification Items — PH-3

### VER-PILOT-3000 — Regulated pilot acceptance
- **Verifies:** goal G3; SCN-103, SCN-104, SCN-107, SCN-109 in production conditions; MET-BIZ-010
- **Suite:** SUITE-PH3-PILOT · **Phase:** PH-3
- **Environment:** pilot managed Server
- **Procedure:**
  1. Record the pilot success criteria (UBS-PH-3 §7.5) as a DEC entry at pilot start.
  2. Run the daily NAV in parallel with the legacy system for the pilot funds; compare daily with tolerance zero and log explanations.
  3. Execute one back-dated correction with restatement, one month-end close and one examination drill.
  4. Hold weekly reviews; obtain signed acceptance.
- **Pass criterion:** ≥ 60 consecutive business days with 0 unexplained differences; all events executed; acceptance signed.
- **Evidence:** pilot report, signed acceptance.
- **Supports:** EXIT-3-01

### VER-PILOT-3001 — Agent acceptance and revertibility in the pilot
- **Verifies:** goal G4; FR-AI-025, FR-AI-031, FR-AI-035; MET-AI-001, MET-AI-002
- **Suite:** SUITE-PH3-PILOT · **Phase:** PH-3
- **Environment:** pilot managed Server with AGT
- **Procedure:**
  1. Run the reconciliation agent daily; reviewers decide on each change set.
  2. Revert a random 10% of merged agent change sets on a copy of the pilot VAE and verify that state returns to the pre-merge values.
- **Pass criterion:** ≥ 80% accepted; 100% of sampled reverts succeed as single revert commits.
- **Evidence:** AI governance report.
- **Supports:** EXIT-3-02

### VER-SEC-6000 — AI governance suite
- **Verifies:** FR-AI-013, FR-AI-015, FR-AI-022, FR-AI-043, NR-PERF-019, DSN-AGT-002, DSN-AGT-106, DSN-AGT-202, DSN-AGT-206
- **Suite:** SUITE-AGT-GOV · **Phase:** PH-3
- **Environment:** ENV-REF-SERVER with AGT and a provider sandbox
- **Procedure:**
  1. Send prompts containing each classification level to providers with each allowed level; verify blocking or redaction at egress (captured by the egress proxy).
  2. Run the injection corpus (≥ 500 documents) against agents with side-effect tools.
  3. Trigger the tenant kill switch with 20 running agents; measure time to cancellation.
- **Pass criterion:** 0 classification leaks; 0 successful injections with side effects; kill switch ≤ 2 s p99.
- **Evidence:** security report.
- **Supports:** EXIT-3-03

### VER-CONF-8100 — Anchoring and tamper detection
- **Verifies:** FR-PROOF-041, FR-PROOF-042, FR-PROOF-043, FR-PROOF-061, DSN-NTY-001, DSN-NTY-002, DSN-NTY-006
- **Suite:** SUITE-NTY-ANCHOR, SUITE-NTY-PACK · **Phase:** PH-3
- **Environment:** pilot environment; offline verification machine
- **Procedure:**
  1. Verify hourly receipts for 30 days with the open verifier; inject TSA failures.
  2. Run the tamper corpus (≥ 1,000 modified packs, seals and archives) through the verifier.
- **Pass criterion:** 100% receipts verify; failover to the second TSA observed with alert; 100% tampered items detected with correct failing item named.
- **Evidence:** NTY reports.
- **Supports:** EXIT-3-04

### VER-SCN-3100 — Examination drill
- **Verifies:** SCN-109, CR-SEC-003, CR-FINRA-003, DSN-NTY-004, DSN-NTY-008
- **Suite:** SUITE-NTY-EXAM · **Phase:** PH-3
- **Environment:** pilot environment
- **Procedure:**
  1. An auditor role-plays an examiner request for Q-end NAVs, fee calculations and approvals.
  2. Produce the pack with readable views; the examiner verifies it offline.
- **Pass criterion:** delivered within 1 business day; verification succeeds (MET-BIZ-011).
- **Evidence:** drill report.
- **Supports:** EXIT-3-05

### VER-FORM-0200 — Sync protocol model
- **Verifies:** STD-SYNC-006, FR-SYNC-015, DSN-DVM-952
- **Suite:** SUITE-DVM-FORM · **Phase:** PH-3
- **Environment:** model-checking workstation
- **Procedure:**
  1. Model device and server branches, scopes, have/want, integration and rejections.
  2. Check convergence, no data loss, scope safety (no out-of-scope data on devices).
- **Pass criterion:** 0 violations at 2 devices × 1 server × 3 objects × 4 changes.
- **Evidence:** model and checker output.
- **Supports:** EXIT-3-06

### VER-SIM-2200 — Offline sync simulation and efficiency
- **Verifies:** FR-SYNC-011…014, FR-SYNC-031…034, NR-PERF-018, DSN-NOD-501…506, SCN-401
- **Suite:** SUITE-NOD-SYNC · **Phase:** PH-3
- **Environment:** simulation cluster; ENV-REF-WAN for efficiency
- **Procedure:**
  1. Simulate 10,000 offline/online cycles with conflicting edits, scope changes and interrupted transfers.
  2. Measure sync of 100 changes in 1 M objects over WAN.
- **Pass criterion:** 0 data loss; convergence in 100% of cycles; NR-PERF-018 met.
- **Evidence:** sync report.
- **Supports:** EXIT-3-06

### VER-SEC-0500 — L3 escape and determinism
- **Verifies:** NR-SEC-002 (L3), FR-LOGIC-053, FR-LOGIC-054, FR-LOGIC-055, DSN-DVM-614, DSN-DVM-615, DSN-FRG-505
- **Suite:** SUITE-DVM-SEC, SUITE-DVM-REPLAY · **Phase:** PH-3
- **Environment:** CI workers on x86-64 and ARM64
- **Procedure:**
  1. Run the L3 escape corpus (≥ 300 cases).
  2. Execute the component determinism set on both architectures and compare outputs.
- **Pass criterion:** 100% blocked; 100% identical outputs.
- **Evidence:** security and replay reports.
- **Supports:** EXIT-3-07

### VER-SCN-3200 — Device suite
- **Verifies:** FR-UX-071, FR-UX-072, NR-PORT-001, NR-PERF-023, NR-SCAL-008, DSN-WSP-201…204
- **Suite:** SUITE-WSP-E2E · **Phase:** PH-3
- **Environment:** ENV-REF-BOX (Windows, macOS, Linux), ENV-REF-MOBILE (iOS, Android)
- **Procedure:**
  1. Run install, unlock, offline work, sync, conflict resolution, backup and restore on each platform.
  2. Measure cold start with 100,000 objects and reads with 1 M objects.
- **Pass criterion:** all steps pass on every platform; NR-PERF-023 and NR-SCAL-008 met.
- **Evidence:** device report.
- **Supports:** EXIT-3-08

### VER-CONF-8406 — BPA 1.1 Sync profile
- **Verifies:** STD-SYNC-001…006 vectors (VER-CONF-5700…5899)
- **Suite:** SUITE-DVM-CONF · **Phase:** PH-3
- **Environment:** ENV-REF-SERVER, ENV-REF-BOX
- **Procedure:**
  1. Run the Sync profile vectors and all earlier vectors.
- **Pass criterion:** 100% pass.
- **Evidence:** signed runner reports, release manifest.
- **Supports:** EXIT-3-09

### VER-SCN-3150 — Holds, retention, WORM and crypto-shredding
- **Verifies:** SCN-016, SCN-207, FR-AUD-061…063, FR-AUD-071…073, FR-PROOF-073…075, CR-SEC-001, CR-GDPR-004
- **Suite:** SUITE-PH3-PILOT · **Phase:** PH-3
- **Environment:** ENV-REF-SERVER with WORM bucket
- **Procedure:**
  1. Place a hold on a counterparty; attempt disposal and erasure; verify refusal.
  2. Release the hold; run retention disposal; verify certificates and proofs.
  3. Attempt deletion of WORM objects through the storage API.
- **Pass criterion:** refusals under hold; disposal with certificates after release; proofs verify; storage refuses WORM deletion.
- **Evidence:** scenario report.
- **Supports:** EXIT-3-10

### VER-USE-3000 — Review efficiency, learnability and assistive technology
- **Verifies:** NR-USE-003, NR-USE-005, NR-ACC-003, NR-ACC-004
- **Suite:** SUITE-WSP-USE, SUITE-WSP-A11Y · **Phase:** PH-3
- **Environment:** pilot users; assistive technology lab
- **Procedure:**
  1. Measure review times for ≤ 50-item change sets and single-object approvals with pilot approvers.
  2. Train new users for ≤ 2 hours and measure core task times against the legacy baseline.
  3. Run core tasks with NVDA, JAWS, VoiceOver and TalkBack; validate accessible PDFs with PDF/UA tooling.
- **Pass criterion:** all four NR targets met.
- **Evidence:** study and audit reports.
- **Supports:** EXIT-3-11

### VER-INSP-6500 — Open verifier release and external review
- **Verifies:** FR-PROOF-061, FR-PROOF-063, DSN-NTY-006, CR-NIST-002
- **Suite:** SUITE-NTY-VERIFIER · **Phase:** PH-3
- **Environment:** public repository
- **Procedure:**
  1. Publish the verifier source, reproducible build instructions and binary hashes.
  2. An external party reviews the code against the published formats and reproduces the build.
- **Pass criterion:** release published; review report with no open critical findings; reproduced hashes equal.
- **Evidence:** release, review report.
- **Supports:** EXIT-3-12

### VER-INSP-9502 — PH-3 traceability and regression
- **Verifies:** all PH-3 Must FR, NR and CR; all earlier suites
- **Suite:** SUITE-REGRESSION · **Phase:** PH-3
- **Environment:** CI
- **Procedure:**
  1. Generate the traceability report; confirm all earlier suites passed on the release candidate.
- **Pass criterion:** no uncovered Must requirement; all earlier suites green.
- **Evidence:** traceability and regression reports.
- **Supports:** EXIT-3-13
