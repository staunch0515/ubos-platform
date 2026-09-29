---
id: UBS-PH-3
title: PH-3 — Regulated Pilot, Devices and Governed AI
status: draft
phase: PH-3
depends_on: [UBS-PH-00, UBS-PH-2]
---

# PH-3 — Regulated Pilot, Devices and Governed AI

## 1. Goal and business value

PH-3 proves regulated value and governed AI in production. A design partner in asset
management runs the fund-operations Buk in a pilot on a managed Server (DEC-016) with
anchored, independently verifiable evidence (goal **G3**). Agents work only on their own
branches and submit change sets that humans review (goal **G4**). Devices arrive: Box desktop
and mobile apps with offline work and sync. Office surfaces complete (Smart Grid, War Room,
Action Messages, negotiation), and L3 WASM components open the platform to more languages.

Measurable outcomes:
1. The design partner signs pilot acceptance with reproducibility and evidence metrics met
   (MET-BIZ-010).
2. ≥ 80% of agent change sets in the pilot are accepted after review, and 100% are
   revertible (MET-AI-001, MET-AI-002).
3. An examination request is answered with a verified evidence pack within 1 business day
   (SCN-109, MET-BIZ-011).
4. A field worker completes five offline days and syncs without data loss (SCN-401).
5. The L3 escape suite and cross-architecture determinism pass (NR-SEC-002 for L3).

## 2. Systems and components in scope

| System | Components | Depth |
|---|---|---|
| AGT | model gateway, agent runtime, branches, kill switch, tools and MCP, knowledge and retrieval, Copilot, AI builder, evaluations, governed evolution | new |
| NTY | anchoring service with two TSAs, evidence pack service, open verifier (CLI, WASM, web) | new |
| NOD | Box desktop and mobile hosts, device sync, vector index adapter, legal holds, retention, WORM, managed Server operations, air-gapped installation | extended |
| DVM | L3 WASM runtime, `ask.model`, agent-branch policies, sync structures, crypto-shredding with proofs, legal holds and retention | extended |
| WSP | desktop and mobile apps, offline experience, portal, Smart Grid, War Room, Action Messages, negotiation, dashboards, theming, Copilot panel | extended |
| STU | AI drafting panel, Buk packaging, test authoring | extended |
| FRG | WASM toolchains (Rust, TypeScript, Go), AI machine interface | extended |
| SDK | Python SDK | extended |
| BRG | document import, CDC export to Iceberg, DuckDB engine, streaming out, custodian and market-data adapters | extended |
| BPA | Sync profile vectors; sync and shared-BPU formal models | extended |
| CTL | CTL-lite inventory for managed Servers | prototype |
| EXC | private registry mode | prototype |

## 3. Feature list

The generated list is UBS-PHF-3: 152 FR, 12 NR, 19 CR and 13 scenarios. Design coverage:
all DSN items with **Phase** PH-3.

## 4. Dependencies

| Dependency | Kind | Needed by |
|---|---|---|
| EXIT-2-01…15 | prior exit | start |
| design-partner pilot agreement, including the managed-Server deployment and data processing terms | external | month 1 |
| model providers with no-training terms and US residency (ASM-004, DSN-AGT-006) | external | month 1 |
| two RFC 3161 TSAs under contract (ASM-005) | external | month 3 |
| WORM-capable object storage in the pilot environment (ASM-006) | external | month 3 |
| Tauri 2 mobile maturity verified (ASM-009) | external | month 2 |
| app store developer accounts and code-signing certificates | external | month 4 |

## 5. Out of scope for this phase

- Multi-tenant Cells, Control Plane (beyond CTL-lite), public Exchange, certification,
  workers, tiering (PH-4).
- Federation, cross-node VAE trees, shared BPUs, browser kernel, QUIC (PH-5).
- Executable Books and external e-signature providers (PH-4).

## 6. Deliverables

| Deliverable | Form |
|---|---|
| UBOS Server 1.x with AGT and NTY services | signed releases |
| Box desktop (Windows, macOS, Linux) and mobile (iOS, Android) apps | signed installers and store listings |
| Open verifier 1.0 | open-source release (Apache-2.0), web page, WASM package |
| Python SDK 1.0 | PyPI |
| BPA 1.1 (Sync profile) | signed release |
| Pilot evidence bundle | pilot report, examination drill report, AI governance report |

## 7. Verification plan

### 7.1 Methods used and why

| Method | Why in PH-3 |
|---|---|
| PILOT | G3 and G4 are defined by a real regulated pilot |
| SEC | AI governance (classification, injection, kill switch), L3 sandbox, device security |
| SIM, FORM | offline sync convergence and rejections; sync formal model |
| CONF | Sync profile vectors; L3 determinism across architectures |
| SCN | agents, devices, office and evidence scenarios |
| BENCH | sync efficiency, kill switch latency, Box start-up, analytics |
| USE | review efficiency and learnability targets enter in PH-3 |

### 7.2 Suites and verification ranges

| Scope | Suite | VER range |
|---|---|---|
| AI governance, kill switch, injection, MCP, evaluations | SUITE-AGT-GOV, SUITE-AGT-KILL, SUITE-AGT-INJECT, SUITE-AGT-MCP, SUITE-AGT-EVAL | VER-SEC-6000…6299, VER-CONF-9100…9199 |
| agent scenarios | SUITE-AGT-SCN | VER-SCN-3000…3099 |
| anchoring, packs, verifier, examination drill | SUITE-NTY-ANCHOR, SUITE-NTY-PACK, SUITE-NTY-VERIFIER, SUITE-NTY-EXAM | VER-CONF-8100…8299, VER-SCN-3100…3149 |
| device sync | SUITE-NOD-SYNC | VER-SIM-2200…2399 |
| L3 sandbox and determinism | SUITE-DVM-SEC, SUITE-DVM-REPLAY | VER-SEC-0500…0799, VER-CONF-8550…8599 |
| Sync profile vectors | SUITE-DVM-CONF | VER-CONF-5700…5899 |
| desktop, mobile, portal, office | SUITE-WSP-E2E | VER-SCN-3200…3399 |
| CDC export and analytics isolation | SUITE-BRG-EXPORT | VER-FAULT-7000…7049, VER-SEC-7000…7049 |
| pilot acceptance | SUITE-PH3-PILOT | VER-PILOT-3000…3049 |

VER numbering follows the allocation of the verification volume (UBS-VER-00 §3).

### 7.3 Environments and datasets

| Use | Environment |
|---|---|
| managed Server for the pilot (single tenant, dedicated account) | pilot environment (ENV-REF-SERVER class) |
| device tests | ENV-REF-BOX, ENV-REF-MOBILE |
| sync over poor networks | ENV-REF-WAN |
| AI evaluations | provider sandboxes plus a self-hosted model endpoint |

Datasets: the partner's production-like data under the pilot agreement (ASM-012); WL-FUND-M
for load; an injection corpus of ≥ 500 adversarial documents; evaluation sets for the
reference agents (reconciliation assistant, clause reviewer).

### 7.4 Pass thresholds

| Check | Threshold |
|---|---|
| pilot | partner-signed acceptance with the pilot success criteria of §7.5 met |
| agent acceptance | ≥ 80% of agent change sets accepted after review; 100% revertible |
| AI governance | 0 classification leaks; 0 successful injections with side effects; kill switch ≤ 2 s p99 (NR-PERF-019) |
| evidence | 100% of tampered packs in the corpus detected; examination drill answered within 1 business day |
| sync | NR-PERF-018 met; 0 data loss across 10,000 simulated offline/online cycles |
| L3 | 100% of the L3 escape suite blocked; identical outputs on x86-64 and ARM64 |
| devices | NR-PERF-023 and NR-SCAL-008 met; NR-PORT-001 platforms pass the device suite |
| usability | NR-USE-003 and NR-USE-005 met; NR-ACC-003 and NR-ACC-004 met |
| availability of degraded modes | NR-AVAIL-005 met with AGT, search, analytics or NTY unavailable |

### 7.5 Acceptance

Pilot success criteria agreed with the partner at pilot start and recorded as a DEC entry:
daily NAV produced in the platform for the pilot funds for 60 consecutive business days with
0 unexplained differences against the legacy system; one back-dated correction restated
with lineage (SCN-104); one month-end close with a sealed close artefact (SCN-107); one
examination drill answered with a verified pack (SCN-109); the reconciliation agent's
proposals reviewed daily. The Phase Acceptance Board accepts PH-3 on the partner's signed
acceptance plus the suite reports.

### 7.6 Regression policy

All PH-1 and PH-2 suites re-run on every release; vertical Buk scenarios re-run on the
pilot's data snapshot before every pilot upgrade.

## 8. Metrics

| Metric | Name | Target |
|---|---|---|
| MET-BIZ-010 | pilot business days with 0 unexplained NAV differences | ≥ 60 consecutive |
| MET-BIZ-011 | examination request to verified pack | ≤ 1 business day |
| MET-AI-001 | agent change sets accepted after review | ≥ 80% |
| MET-AI-002 | merged agent change sets revertible as a unit | 100% |
| MET-AI-003 | kill switch latency p99 (NR-PERF-019) | ≤ 2 s |
| MET-AI-004 | successful injections with side effects | 0 |
| MET-SEC-030 | L3 escape cases blocked | 100% |
| MET-PERF-030 | sync of 100 changes in 1 M objects over WAN (NR-PERF-018) | ≤ 10 s |
| MET-PERF-031 | Box cold start to usable UI (NR-PERF-023) | ≤ 3 s |
| MET-USE-010 | median review time for ≤ 50-item change sets (NR-USE-003) | ≤ 3 min |

## 9. Exit criteria

| Exit | Criterion | Evidence |
|---|---|---|
| EXIT-3-01 | Design partner signs pilot acceptance with all §7.5 criteria met | signed acceptance, pilot report |
| EXIT-3-02 | MET-AI-001 and MET-AI-002 met in the pilot | AI governance report |
| EXIT-3-03 | AGT security suites pass: 0 leaks, 0 successful injections, kill switch within bound | security report |
| EXIT-3-04 | Anchoring active for the pilot with receipts verifiable by the open verifier; tamper corpus 100% detected | NTY reports |
| EXIT-3-05 | Examination drill answered within 1 business day with a verified pack | drill report |
| EXIT-3-06 | Sync formal model checked; SUITE-NOD-SYNC passes; NR-PERF-018 met | model report, sync report |
| EXIT-3-07 | L3 escape suite 100% blocked; cross-architecture determinism 100% | security and replay reports |
| EXIT-3-08 | Box desktop and mobile apps pass the device suite on all NR-PORT-001 platforms; NR-PERF-023 met | device report |
| EXIT-3-09 | BPA 1.1 released with Sync profile vectors; 100% pass | release manifest, runner reports |
| EXIT-3-10 | Crypto-shredding with proofs, legal holds, retention and WORM pass their scenarios (SCN-016 re-run, SCN-207) | scenario report |
| EXIT-3-11 | NR-USE-003, NR-USE-005, NR-ACC-003 and NR-ACC-004 met | study and audit reports |
| EXIT-3-12 | Open verifier released and independently reviewed by an external party | release, review report |
| EXIT-3-13 | Traceability shows every PH-3 Must requirement covered; all earlier suites pass | traceability and regression reports |

## 10. Risks and mitigations

| Risk | Description | L | I | Mitigation | Early warning | Owner |
|---|---|---|---|---|---|---|
| RSK-030 | Pilot parallel run shows differences traced to legacy data quality | H | M | reconciliation tooling and daily break review; agree tolerance and explanation process up front | unexplained differences > 5 per day in week 2 | pilot lead |
| RSK-031 | Model provider terms or availability change (ASM-004) | M | M | provider-neutral gateway; self-hosted fallback route evaluated | provider notice | AGT lead |
| RSK-032 | Prompt injection through documents triggers side effects | M | H | data marking, scope limits, human confirmation for high-risk actions, injection corpus in CI | any corpus failure | security lead |
| RSK-033 | Mobile embedding of the kernel is unstable (ASM-009) | M | M | thin mobile client fallback without embedded kernel | crash rate > 1% in beta | client lead |
| RSK-034 | Offline conflicts confuse users | M | M | clear conflict screen; rejections with reasons; field-worker usability tests | conflict-resolution task success < 80% | design lead |
| RSK-035 | TSA acceptance by auditors uncertain (ASM-005) | L | M | two TSAs; auditor consultation during the pilot | auditor feedback | compliance lead |

## 11. Reference duration and team assumption

Non-normative. 9 months. Team of 16–18: adds 2 AI engineers, 1 NTY/crypto engineer, 2 client
engineers for desktop and mobile, and a pilot lead with the partner's domain staff.
