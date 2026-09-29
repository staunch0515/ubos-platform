---
id: UBS-PH-5
title: PH-5 — Federation and the Open Network
status: draft
phase: PH-5
depends_on: [UBS-PH-00, UBS-PH-4]
---

# PH-5 — Federation and the Open Network

## 1. Goal and business value

PH-5 connects organisations. Independently operated nodes resolve each other through the
federation registry, agree trust relationships, run VAE trees across nodes with network
fallback, and share BPUs (for example a contract or an investor register) whose Definition
changes need the parties' signatures. The browser kernel brings local-first use to the web.
It delivers business goal **G6**: two independent organisations operate a shared contract BPU
across their own nodes.

Measurable outcomes:
1. Two organisations, each on its own node, operate a shared contract BPU for ≥ 30 days with
   equal roots after every convergence (MET-BIZ-030).
2. A headquarters–subsidiary VAE tree across nodes survives a 24-hour parent outage with local
   work continuing (SCN-408, MET-OPS-050).
3. The browser kernel passes the conformance suite and meets NR-PERF-024 (MET-PERF-050).
4. Cell availability rises to 99.95% (NR-AVAIL-001 PH-5 target).

## 2. Systems and components in scope

| System | Components | Depth |
|---|---|---|
| FED | registry service, trust relationships, FED node module, shared-BPU coordination, aggregated child queries | new |
| NOD | cross-organisation sync transport, remote parent fallback, QUIC binding | extended |
| DVM | browser build (WASM, OPFS), shared-BPU structures, federation fallback | extended |
| WSP | browser-local kernel mode | extended |
| BPA | federation and shared-BPU clauses finalised; formal model STD-SYNC-012 | extended |
| EXC | federation-aware mirrors for sovereign and air-gapped deployments | extended |
| CTL | EU management region and EU Cells (if not advanced earlier by decision) | extended |

## 3. Feature list

The generated list is UBS-PHF-5: 17 FR, 1 NR and 3 scenarios (SCN-408, SCN-409, SCN-411).
Design coverage: all DSN items with **Phase** PH-5, including the whole FED chapter.

## 4. Dependencies

| Dependency | Kind | Needed by |
|---|---|---|
| EXIT-4-01…11 | prior exit | start |
| two pilot organisations for the shared-BPU pilot (for example a fund administrator and an asset manager) with legal agreements for data sharing | external | month 1 |
| browser support for OPFS and WASM performance (ASM-011) | external | month 2 |
| registry operators (UBOS plus at least one independent operator) | external | month 4 |

## 5. Out of scope for this phase

The long-term narratives excluded by the charter (UBS-META-00 §3.2) remain excluded:
organisational-governance protocols, intent economy, compute currencies and tokens are not
part of this phase or of any phase. Public blockchains are used only as optional timestamp
evidence (NTY), never as a system of record.

## 6. Deliverables

| Deliverable | Form |
|---|---|
| Federation registry service and FED node module | releases and operator guide |
| BPA 1.2 with federation and shared-BPU clauses | signed release |
| Browser kernel package | npm package with WASM kernel and OPFS adapter |
| Shared-BPU pilot report | signed by both organisations |

## 7. Verification plan

### 7.1 Methods used and why

| Method | Why in PH-5 |
|---|---|
| FORM | multi-party protocols need proofs (STD-SYNC-012) |
| SIM | partitions and concurrency across organisations |
| SEC | trust enforcement across organisational boundaries |
| CONF | browser kernel conformance; federation vectors |
| PILOT | G6 is defined by two real organisations |
| BENCH | browser local reads; cross-node fallback latency |

### 7.2 Suites and verification ranges

| Scope | Suite | VER range |
|---|---|---|
| shared-BPU protocol with 2–5 parties | SUITE-FED-SHARED | VER-SIM-8500…8599, VER-FORM-8500…8549 |
| trust enforcement and DID-bound mTLS | SUITE-FED-TRUST | VER-SEC-8500…8599 |
| remote parent outages | SUITE-FED-FALLBACK | VER-SIM-8600…8649 |
| shared-BPU pilot | SUITE-FED-PILOT | VER-PILOT-5000…5049 |
| browser kernel conformance and performance | SUITE-DVM-CONF, SUITE-DVM-BENCH | VER-CONF-0001…6299 (claimed profiles), VER-BENCH-0400…0449 |
| federation scenarios | SUITE-PH5-FED | VER-SCN-5000…5099 |

VER numbering follows the allocation of the verification volume (UBS-VER-00 §3).

### 7.3 Environments and datasets

| Use | Environment |
|---|---|
| two organisations on separate nodes over the internet | two ENV-REF-SERVER instances connected by ENV-REF-WAN |
| browser kernel | ENV-REF-BROWSER |

Datasets: a shared contract with obligations and payment schedules; an investor register
shared between administrator and manager; synthetic partition schedules for simulation.

### 7.4 Pass thresholds

| Check | Threshold |
|---|---|
| shared-BPU formal model | 0 violations with 3 parties and crash injection |
| simulation | 0 divergence after convergence in 1,000,000 seeds |
| trust | 0 interactions outside a trust relationship accepted |
| pilot | 30 days of operation, equal roots after every convergence, both organisations sign acceptance |
| browser | 100% of claimed-profile vectors pass; NR-PERF-024 met |
| availability | NR-AVAIL-001 99.95% over 60 days |

### 7.5 Acceptance

The Phase Acceptance Board accepts PH-5 on the signed pilot report of both organisations, the
formal-model and simulation reports, the security report and the browser conformance report.

### 7.6 Regression policy

All earlier suites re-run; federation suites join the nightly runs.

## 8. Metrics

| Metric | Name | Target |
|---|---|---|
| MET-BIZ-030 | days of shared-BPU operation with equal roots after convergence | ≥ 30 |
| MET-OPS-050 | subsidiary local work during a 24-hour parent outage | 100% of local processes succeed |
| MET-OPS-051 | Cell availability over 60 days | ≥ 99.95% |
| MET-PERF-050 | browser point read p95 with 200,000 objects (NR-PERF-024) | ≤ 10 ms |

## 9. Exit criteria

| Exit | Criterion | Evidence |
|---|---|---|
| EXIT-5-01 | Shared-BPU formal model (STD-SYNC-012) checked with 0 violations | model report |
| EXIT-5-02 | SUITE-FED-SHARED, SUITE-FED-TRUST and SUITE-FED-FALLBACK pass | suite reports |
| EXIT-5-03 | Two organisations sign the shared-BPU pilot report (MET-BIZ-030) | signed pilot report |
| EXIT-5-04 | SCN-408 passes with a 24-hour parent outage (MET-OPS-050) | scenario report |
| EXIT-5-05 | Browser kernel passes the claimed-profile vectors and NR-PERF-024 (SCN-411) | conformance and benchmark reports |
| EXIT-5-06 | BPA 1.2 released with federation and shared-BPU vectors | release manifest |
| EXIT-5-07 | Registry replicated by at least one independent operator with signature-verified records | operator report |
| EXIT-5-08 | Traceability shows every PH-5 Must requirement covered; all earlier suites pass | traceability and regression reports |

## 10. Risks and mitigations

| Risk | Description | L | I | Mitigation | Early warning | Owner |
|---|---|---|---|---|---|---|
| RSK-050 | Legal and data-sharing agreements between organisations take longer than the engineering | H | M | start legal work at phase start; template agreements | unsigned agreement at month 3 | product owner |
| RSK-051 | Shared-BPU protocol defects under partitions | M | H | formal model first; simulation with million-seed runs | any divergence | kernel lead |
| RSK-052 | Browser storage eviction and quotas limit usefulness (ASM-011) | M | M | eviction handling and re-sync (FR-SYNC-083); scope limits | eviction rate in beta | client lead |
| RSK-053 | Registry becomes a single point of failure or control | L | M | multiple operators, DNS fallback, cached resolution | registry outage alerts | platform lead |

## 11. Reference duration and team assumption

Non-normative. 9–12 months. Team of 18–20, with 3 engineers dedicated to federation and
shared-BPU protocols and 2 to the browser kernel.
