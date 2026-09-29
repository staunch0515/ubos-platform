---
id: UBS-PH-3
title: PH-3 — CPA Audit Drill, Evidence and Governed AI
status: draft
phase: PH-3
depends_on: [UBS-PH-00, UBS-PH-2]
---

# PH-3 — CPA Audit Drill, Evidence and Governed AI

## 1. Goal and business value

PH-3 proves audit-grade evidence and governed AI on the Basic Finance package (DEC-022,
DEC-023). The sample company's books run for a full year on a demonstration Server with
anchored, independently verifiable evidence, and an external US CPA performs an audit drill
on them (goal **G3**, SCN-511). Finance agents work only on their own branches and submit
change sets that humans review (goal **G4**, SCN-512). Office surfaces for finance complete
(Action Messages, dashboards, Copilot panel), and L3 WASM components open the platform to
more languages. Devices and sync are not in this phase (PH-4, DEC-024).

Measurable outcomes:
1. The CPA signs the audit-drill report stating that the evidence was sufficient and
   verifiable (MET-BIZ-010).
2. ≥ 80% of agent change sets in the demonstration operation are accepted after review, and
   100% are revertible (MET-AI-001, MET-AI-002).
3. An auditor request is answered with a verified evidence pack within 1 business day
   (SCN-511, MET-BIZ-011).
4. The L3 escape suite and cross-architecture determinism pass (NR-SEC-002 for L3).
5. Comparison data for PH-3 are recorded under the DEC-026 protocol (MET-BIZ-012).

## 2. Systems and components in scope

| System | Components | Depth |
|---|---|---|
| AGT | model gateway, agent runtime, branches, kill switch, tools and MCP, knowledge and retrieval, Copilot, AI builder, evaluations, governed evolution; reference agents `recon-bot` and `ap-coder` | new |
| NTY | anchoring service with two TSAs, evidence pack service, open verifier (CLI, WASM, web) | new |
| NOD | vector index adapter, legal holds, retention, WORM, demonstration Server operations, air-gapped installation | extended |
| DVM | L3 WASM runtime, `ask.model`, agent-branch policies, crypto-shredding with proofs, legal holds and retention | extended |
| WSP | Action Messages, dashboards, theming, Copilot panel, auditor views | extended |
| STU | AI drafting panel, Buk packaging, test authoring | extended |
| FRG | WASM toolchains (Rust, TypeScript, Go), AI machine interface | extended |
| SDK | Python SDK | extended |
| BRG | document import (bills, statements), CDC export to Iceberg, DuckDB engine, streaming out | extended |
| Buks | `basic-finance` 1.1: year-end close, auditor requests, agent tools | extended |

## 3. Feature list

The generated list is UBS-PHF-3: 115 FR, 8 NR, 16 CR and 9 scenarios. Design coverage: all
DSN items with **Phase** PH-3.

## 4. Dependencies

| Dependency | Kind | Needed by |
|---|---|---|
| EXIT-2-01…19 (except withdrawn exits) | prior exit | start |
| external US CPA engaged for the audit drill (ASM-002) | external | month 2 |
| model providers with no-training terms and US residency (ASM-004, DSN-AGT-006) | external | month 1 |
| two RFC 3161 TSAs under contract (ASM-005) | external | month 3 |
| WORM-capable object storage in the demonstration environment (ASM-006) | external | month 3 |

## 5. Out of scope for this phase

- SQLite, Box, desktop and mobile apps, offline sync, BPA Sync profile (PH-4, DEC-024).
- Multi-tenant Cells, Control Plane, public Exchange, certification, workers, tiering (PH-4).
- External portal, War Room, negotiation, contract-only capabilities (PH-5, DEC-022).
- Federation, cross-node VAE trees, shared BPUs, browser kernel, QUIC (PH-5).
- Fund-operations and contract packages (deferred, DEC-022).

## 6. Deliverables

| Deliverable | Form |
|---|---|
| UBOS Server 1.x with AGT and NTY services | signed releases |
| Open verifier 1.0 | open-source release (Apache-2.0), web page, WASM package |
| Python SDK 1.0 | PyPI |
| `basic-finance` Buk 1.1 and reference agents | signed Buk archive, agent definitions and evaluation sets |
| Audit-drill evidence bundle | year-end close record, auditor requests and packs, CPA drill report, AI governance report |
| Comparison data set | PH-3 records under the DEC-026 protocol |

## 7. Verification plan

### 7.1 Methods used and why

| Method | Why in PH-3 |
|---|---|
| PILOT | G3 and G4 are defined by the CPA audit drill and a year of demonstration operation |
| SEC | AI governance (classification, injection, kill switch), L3 sandbox |
| CONF | L3 determinism across architectures |
| SCN | agents, evidence and year-end scenarios |
| BENCH | kill switch latency, analytics |
| USE | review efficiency and learnability targets enter in PH-3 |

### 7.2 Suites and verification ranges

| Scope | Suite | VER range |
|---|---|---|
| AI governance, kill switch, injection, MCP, evaluations | SUITE-AGT-GOV, SUITE-AGT-KILL, SUITE-AGT-INJECT, SUITE-AGT-MCP, SUITE-AGT-EVAL | VER-SEC-6000…6299, VER-CONF-9100…9199 |
| agent scenarios | SUITE-AGT-SCN | VER-SCN-3000…3099 |
| anchoring, packs, verifier, auditor requests | SUITE-NTY-ANCHOR, SUITE-NTY-PACK, SUITE-NTY-VERIFIER, SUITE-NTY-EXAM | VER-CONF-8100…8299, VER-SCN-3100…3149 |
| L3 sandbox and determinism | SUITE-DVM-SEC, SUITE-DVM-REPLAY | VER-SEC-0500…0799, VER-CONF-8550…8599 |
| CDC export and analytics isolation | SUITE-BRG-EXPORT | VER-FAULT-7000…7049, VER-SEC-7000…7049 |
| CPA audit drill and demonstration operation | SUITE-PH3-PILOT | VER-PILOT-3000…3049 |

VER numbering follows the allocation of the verification volume (UBS-VER-00 §3).

### 7.3 Environments and datasets

| Use | Environment |
|---|---|
| demonstration Server (single tenant) for the year of operation and the drill | ENV-REF-SERVER |
| auditor access and offline verification | ENV-REF-WAN |
| AI evaluations | provider sandboxes plus a self-hosted model endpoint |

Datasets: the Northwind sample company extended to a full year of activity (WL-FIN-M); an
injection corpus of ≥ 500 adversarial documents (bills, statements, e-mails); evaluation sets
for `recon-bot` and `ap-coder`.

### 7.4 Pass thresholds

| Check | Threshold |
|---|---|
| audit drill | CPA-signed drill report with the criteria of §7.5 met |
| agent acceptance | ≥ 80% of agent change sets accepted after review; 100% revertible |
| AI governance | 0 classification leaks; 0 successful injections with side effects; kill switch ≤ 2 s p99 (NR-PERF-019) |
| evidence | 100% of tampered packs in the corpus detected; auditor requests answered within 1 business day |
| L3 | 100% of the L3 escape suite blocked; identical outputs on x86-64 and ARM64 |
| usability | NR-USE-003 and NR-USE-005 met; NR-ACC-004 met |
| availability of degraded modes | NR-AVAIL-005 met with AGT, search, analytics or NTY unavailable |

### 7.5 Acceptance

Audit-drill criteria agreed with the CPA at engagement and recorded as a DEC entry: twelve
monthly closes with sealed close artefacts; one year-end close posting net income to retained
earnings (SCN-511); one back-dated correction restated with lineage (SCN-508); the CPA's
requests (trial-balance roll-forward, entries above threshold, entries after close, access
list, approval evidence) answered with verified packs; a sample of statements re-performed
from run records with identical outputs; the agents' proposals reviewed daily. The Phase
Acceptance Board accepts PH-3 on the CPA's signed drill report plus the suite reports.

### 7.6 Regression policy

All PH-1 and PH-2 suites re-run on every release; SUITE-PH2-FINANCE re-runs on the
demonstration data snapshot before every upgrade of the demonstration Server.

## 8. Metrics

| Metric | Name | Target |
|---|---|---|
| MET-BIZ-010 | CPA audit-drill findings on evidence sufficiency or verifiability | 0 open |
| MET-BIZ-011 | auditor request to verified pack | ≤ 1 business day |
| MET-BIZ-012 | comparison-protocol records captured for PH-3 work items (DEC-026) | 100% |
| MET-AI-001 | agent change sets accepted after review | ≥ 80% |
| MET-AI-002 | merged agent change sets revertible as a unit | 100% |
| MET-AI-003 | kill switch latency p99 (NR-PERF-019) | ≤ 2 s |
| MET-AI-004 | successful injections with side effects | 0 |
| MET-SEC-030 | L3 escape cases blocked | 100% |
| MET-USE-010 | median review time for ≤ 50-item change sets (NR-USE-003) | ≤ 3 min |

## 9. Exit criteria

| Exit | Criterion | Evidence |
|---|---|---|
| EXIT-3-01 | **Withdrawn** (DEC-023): no design-partner pilot; replaced by EXIT-3-14 | — |
| EXIT-3-02 | MET-AI-001 and MET-AI-002 met in the demonstration operation with `recon-bot` and `ap-coder` (SCN-301, SCN-512) | AI governance report |
| EXIT-3-03 | AGT security suites pass: 0 leaks, 0 successful injections, kill switch within bound | security report |
| EXIT-3-04 | Anchoring active on the demonstration Server with receipts verifiable by the open verifier; tamper corpus 100% detected | NTY reports |
| EXIT-3-05 | Auditor requests of the drill answered within 1 business day with verified packs (MET-BIZ-011) | drill report |
| EXIT-3-06 | **Withdrawn** (DEC-024): device sync moves to PH-4 (EXIT-4-12) | — |
| EXIT-3-07 | L3 escape suite 100% blocked; cross-architecture determinism 100% | security and replay reports |
| EXIT-3-08 | **Withdrawn** (DEC-024): desktop and mobile apps move to PH-4 (EXIT-4-13) | — |
| EXIT-3-09 | **Withdrawn** (DEC-024): BPA 1.1 Sync profile moves to PH-4 (EXIT-4-14) | — |
| EXIT-3-10 | Crypto-shredding with proofs, legal holds, retention and WORM pass their scenarios (SCN-016 re-run, SCN-511 A2) | scenario report |
| EXIT-3-11 | NR-USE-003, NR-USE-005 and NR-ACC-004 met | study and audit reports |
| EXIT-3-12 | Open verifier released and independently reviewed by an external party | release, review report |
| EXIT-3-13 | Traceability shows every PH-3 Must requirement covered; all earlier suites pass | traceability and regression reports |
| EXIT-3-14 | The external CPA signs the audit-drill report with all §7.5 criteria met (MET-BIZ-010) | signed drill report, evidence bundle |
| EXIT-3-15 | The PH-3 comparison data set is complete under the DEC-026 protocol (MET-BIZ-012), and a comparison report for PH-1…PH-3 is published to the owner | comparison data set and report |

## 10. Risks and mitigations

| Risk | Description | L | I | Mitigation | Early warning | Owner |
|---|---|---|---|---|---|---|
| RSK-030 | The CPA finds evidence gaps that need design changes late in the phase | M | M | walk-through of the request list and a trial pack at month 3; request list agreed at engagement | open drill comments at the trial pack | audit-drill lead |
| RSK-031 | Model provider terms or availability change (ASM-004) | M | M | provider-neutral gateway; self-hosted fallback route evaluated | provider notice | AGT lead |
| RSK-032 | Prompt injection through bills, statements or e-mails triggers side effects | M | H | data marking, scope limits, human confirmation for high-risk actions, injection corpus in CI | any corpus failure | security lead |
| RSK-033 | **Withdrawn** (DEC-024): mobile embedding moves to PH-4 | — | — | — | — | — |
| RSK-034 | **Withdrawn** (DEC-024): offline conflicts move to PH-4 | — | — | — | — | — |
| RSK-035 | TSA acceptance by auditors uncertain (ASM-005) | L | M | two TSAs; consultation with the CPA during the drill | CPA feedback | compliance lead |

## 11. Reference duration and team assumption

Non-normative. 6–9 months. Team of 13–15: adds 2 AI engineers, 1 NTY/crypto engineer and an
audit-drill lead with the accounting analyst; the client engineers of PH-2 continue.
