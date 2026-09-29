---
id: UBS-PH-4
title: PH-4 — Devices, Platform Scale and Ecosystem
status: draft
phase: PH-4
depends_on: [UBS-PH-00, UBS-PH-3]
---

# PH-4 — Devices, Platform Scale and Ecosystem

> **Not scheduled (DEC-023).** PH-4 is specified so that the design stays complete, but it is
> not planned until the owner decides after PH-3. Its content now also includes SQLite, the Box
> edition, desktop and mobile apps and device sync, moved from PH-1 and PH-3 (DEC-024,
> DEC-025).

## 1. Goal and business value

PH-4 turns the product into a platform. Multi-tenant Cells operated through the Control
Plane serve many small and mid-sized customers; the public Buk Exchange lets third parties
publish, license and get paid; the certification programme lets third parties implement or
embed the BPA; enterprise worker pools run batch work inside customers' networks. It
delivers business goal **G5**: at least 3 third-party Buks are published, and at least 1
third-party BPA implementation or embedding passes conformance. Devices arrive: the SQLite
adapter, the Box edition, desktop and mobile apps with offline work and sync, and BPA 1.1 with
the Sync profile.

Measurable outcomes:
1. A Cell serves ≥ 1,000 tenants (WL-TENANTS-1K) with NR-PERF targets met for all tenants
   (NR-SCAL-002, MET-PERF-040).
2. An online tenant migration between Cells completes with a write pause ≤ 30 s
   (NR-AVAIL-004).
3. ≥ 3 third-party Buks listed, licensed, invoiced and paid out (MET-BIZ-020).
4. ≥ 1 third-party implementation or embedding certified (MET-BIZ-021).
5. SOC 2 Type II audit period started for the Cell service with a clean readiness assessment
   (CR-SOC2-001, MET-SEC-040).
6. A field worker completes five offline days and syncs without data loss (SCN-401), and the
   SQLite adapter passes the storage suite and all BPA vectors (MET-QUAL-040).

## 2. Systems and components in scope

| System | Components | Depth |
|---|---|---|
| CTL | registry, placement, lifecycle, migration, upgrade waves, config rollout, usage, plans, entitlements, budgets, capacity, tiering policy, operator console, tenant portal | new |
| EXC | public Exchange: publishers, verification service, listings, licences, invoicing, payouts, advisories | new |
| NOD | Cell profile, per-tenant fairness at scale, worker role, usage metering, history tiering, admin endpoint, customer-operated node connectivity | extended |
| DVM | tiering reads, Cell-scale performance, embedding API for third parties | hardened |
| BPA | certification programme, certificate registry, extension registry for third parties, reference licensing | extended |
| FRG | publish to EXC; reproducible verification service | extended |
| WSP | Executable Books reader; languages ES, FR, DE, JA, ZH (NR-LOC-001) | extended |
| BRG | external e-signature providers; ERP and CRM adapters | extended |
| AGT | CTL budgets integration; self-hosted inference route for Cells | extended |
| DVM (devices) | SQLite adapter, sync structures, Box embedding | extended |
| NOD (devices) | Box desktop and mobile hosts, device sync, Box headless | extended |
| WSP (devices) | desktop and mobile apps, offline experience | extended |
| BPA (sync) | 1.1 Sync profile vectors; sync formal model | extended |

## 3. Feature list

The generated list is UBS-PHF-4: 69 FR, 13 NR, 10 CR and 6 scenarios (SCN-401, SCN-402,
SCN-403, SCN-404, SCN-407, SCN-410). Design coverage: all DSN items with **Phase** PH-4, including the whole
CTL and EXC chapters.

## 4. Dependencies

| Dependency | Kind | Needed by |
|---|---|---|
| EXIT-3-01…15 (except withdrawn exits) | prior exit | start |
| Tauri 2 mobile maturity verified (ASM-009) | external | month 2 |
| app store developer accounts and code-signing certificates | external | month 4 |
| cloud accounts in the US region for Cells; HSM for CTL CA and EXC keys | external | month 1 |
| SOC 2 auditor engaged; policies and controls in place (CR-SOC2-001…010) | external | month 1 |
| payment processor (PCI DSS Level 1) and tax service contracts | external | month 3 |
| at least three third-party publishers recruited; at least one implementer or embedder recruited (ASM-010) | external | month 2 |
| translation vendor | external | month 3 |

## 5. Out of scope for this phase

- Federation registry, cross-node VAE trees, shared BPUs, cross-organisation sync (PH-5).
- Browser kernel and QUIC (PH-5).
- EU Cells (require the EU management region; planned with PH-5 unless a decision
  advances them).

## 6. Deliverables

| Deliverable | Form |
|---|---|
| UBOS Cloud (Cells) general availability in the US region | service with status page and SLA 99.9% (NR-AVAIL-001) |
| Control Plane with operator console and tenant portal | service |
| Buk Exchange | public service and publisher documentation |
| Certification programme | procedure, certification environment, public certificate registry |
| Worker role | signed images and deployment guide |
| SOC 2 readiness report and audit period start | auditor letter |
| Box desktop (Windows, macOS, Linux) and mobile (iOS, Android) apps | signed installers and store listings |
| BPA 1.1 (Sync profile) | signed release |

## 7. Verification plan

### 7.1 Methods used and why

| Method | Why in PH-4 |
|---|---|
| BENCH | multi-tenant scale, fairness, tiering, cost per tenant |
| FAULT, SIM | orchestration (migration, waves) under crashes; Cell failure isolation; offline sync convergence and rejections |
| FORM | sync formal model |
| SEC | CTL blindness, Exchange supply chain, residency, SOC 2 controls |
| CONF | certification reproduction; licence validation; usage chains |
| SCN | platform scenarios SCN-403, SCN-404, SCN-407, SCN-410 |
| INSP | SOC 2 control evidence; CTL architecture review |
| PILOT | third-party publishers and implementer as ecosystem pilots |

### 7.2 Suites and verification ranges

| Scope | Suite | VER range |
|---|---|---|
| CTL blindness, orchestration, usage, scale | SUITE-CTL-BLIND, SUITE-CTL-ORCH, SUITE-CTL-USAGE, SUITE-CTL-SCALE | VER-SEC-7500…7549, VER-FAULT-7500…7599, VER-CONF-8700…8749, VER-BENCH-7500…7549 |
| Exchange publishing, verification, licences, money | SUITE-EXC-PUB, SUITE-EXC-VERIFY, SUITE-EXC-LIC, SUITE-EXC-MONEY | VER-CONF-7900…8099 |
| certification dry runs | SUITE-BPA-CERT | VER-CONF-8300…8399 |
| Cell-scale kernel and edge benchmarks | SUITE-DVM-BENCH, SUITE-NOD-BENCH | VER-BENCH-0300…0399, VER-BENCH-2200…2299 |
| cross-implementation equivalence | SUITE-DVM-XIMPL | VER-CONF-8600…8649 |
| worker result verification | SUITE-NOD-JOBS | VER-SEC-2400…2449 |
| platform scenarios | SUITE-PH4-PLATFORM | VER-SCN-4000…4099 |
| device sync, sync model, Sync profile vectors (items keep their PH-3 numbers, DEC-024) | SUITE-NOD-SYNC, SUITE-DVM-FORM, SUITE-DVM-CONF | VER-SIM-2200…2399, VER-FORM-0200, VER-CONF-5700…5899 |
| desktop and mobile apps (item keeps its PH-3 number) | SUITE-WSP-E2E | VER-SCN-3200…3399 |
| SQLite adapter | SUITE-DVM-STORE, SUITE-DVM-CONF | VER-CONF-8400…8403 (re-run on SQLite) |

VER numbering follows the allocation of the verification volume (UBS-VER-00 §3).

### 7.3 Environments and datasets

| Use | Environment |
|---|---|
| multi-tenant scale and fairness | ENV-REF-CELL with WL-TENANTS-1K |
| migration between Cells | two ENV-REF-CELL instances |
| worker pools | customer-like network with egress restrictions |
| device tests | ENV-REF-BOX, ENV-REF-MOBILE |
| sync over poor networks | ENV-REF-WAN |

### 7.4 Pass thresholds

| Check | Threshold |
|---|---|
| scale | NR-SCAL-002, NR-SCAL-007, NR-SCAL-009 and NR-SCAL-001 (PH-4 target) met |
| fairness | NR-PERF-022 met |
| tiering | NR-PERF-021 met; tiered versions remain provable |
| cost | NR-COST-003 and NR-COST-004 met |
| availability | NR-AVAIL-001 99.9% over the last 60 days before exit |
| migration | NR-AVAIL-004 met in 10 consecutive migrations of test tenants and 1 real tenant |
| CTL | 0 business-data paths found; orchestration exactly-once under crash injection |
| Exchange | reproducible verification 100%; licences validated offline; money ledger balanced |
| certification | reference DVM certified; ≥ 1 third party certified |
| residency | NR-PRIV-004: 0 out-of-region storage, processing or model calls |
| security | NR-SEC-006 and NR-SEC-007 (SLSA L3) met; 0 open critical or high findings |
| sync | NR-PERF-018 met; 0 data loss across 10,000 simulated offline/online cycles |
| devices | NR-PERF-023 and NR-SCAL-008 met; NR-PORT-001 platforms pass the device suite; NR-ACC-003 mobile targets met |
| SQLite | 100% of BPA vectors and the storage suite pass on SQLite (NR-PORT-003, NR-DET-002) |

### 7.5 Acceptance

The Phase Acceptance Board accepts PH-4 with: the SOC 2 readiness report; signed statements
from three publishers that their Buks are listed and paid; the certificate of the third-party
implementation; and the scale and availability reports.

### 7.6 Regression policy

All earlier suites re-run; Exchange Buks' scenarios run on every kernel minor
(NR-COMPAT-004); certified implementations are re-tested against every BPA patch release.

## 8. Metrics

| Metric | Name | Target |
|---|---|---|
| MET-BIZ-020 | third-party Buks listed, licensed and paid out | ≥ 3 |
| MET-BIZ-021 | certified third-party implementations or embeddings | ≥ 1 |
| MET-PERF-040 | tenants on one Cell with NR-PERF targets met | ≥ 1,000 |
| MET-PERF-041 | small-tenant p95 read latency increase under noisy neighbour (NR-PERF-022) | ≤ 30% |
| MET-OPS-040 | Cell availability over 60 days (NR-AVAIL-001) | ≥ 99.9% |
| MET-OPS-041 | migration write pause (NR-AVAIL-004) | ≤ 30 s |
| MET-OPS-042 | cost attributed to tenant and category (NR-COST-004) | ≥ 99% |
| MET-SEC-040 | SOC 2 readiness exceptions | 0 high |
| MET-PERF-030 | sync of 100 changes in 1 M objects over WAN (NR-PERF-018) | ≤ 10 s |
| MET-PERF-031 | Box cold start to usable UI (NR-PERF-023) | ≤ 3 s |
| MET-QUAL-040 | BPA vectors and storage suite passing on SQLite | 100% |

## 9. Exit criteria

| Exit | Criterion | Evidence |
|---|---|---|
| EXIT-4-01 | A Cell meets NR-SCAL-002 and NR-PERF-022 with WL-TENANTS-1K | scale report |
| EXIT-4-02 | 10 consecutive test migrations and 1 real migration meet NR-AVAIL-004 with equal roots | migration reports |
| EXIT-4-03 | CTL suites pass, including the blindness inspection and penetration test | CTL reports |
| EXIT-4-04 | Exchange live with ≥ 3 third-party Buks listed, licensed, invoiced and paid out | Exchange reports, publisher statements |
| EXIT-4-05 | Certification programme live; reference DVM and ≥ 1 third party certified | certificates |
| EXIT-4-06 | Worker role passes result verification and quarantine tests; SCN-410 passes | worker report |
| EXIT-4-07 | Cell availability ≥ 99.9% over 60 days | SLO report |
| EXIT-4-08 | Residency, SLSA L3 and key-custody requirements met (NR-PRIV-004, NR-SEC-007, NR-SEC-006) | security report |
| EXIT-4-09 | SOC 2 readiness with 0 high exceptions; audit period started | auditor letter |
| EXIT-4-10 | Six UI languages shipped (NR-LOC-001) | release notes, localisation QA report |
| EXIT-4-11 | Traceability shows every PH-4 Must requirement covered; all earlier suites pass | traceability and regression reports |
| EXIT-4-12 | Sync formal model checked; SUITE-NOD-SYNC passes; NR-PERF-018 met (moved from EXIT-3-06, DEC-024) | model report, sync report |
| EXIT-4-13 | Box desktop and mobile apps pass the device suite on all NR-PORT-001 platforms; NR-PERF-023 met (moved from EXIT-3-08) | device report |
| EXIT-4-14 | BPA 1.1 released with Sync profile vectors; 100% pass (moved from EXIT-3-09) | release manifest, runner reports |
| EXIT-4-15 | The SQLite adapter passes the storage suite and 100% of BPA vectors, with the same results as PostgreSQL (MET-QUAL-040) | runner reports |

## 10. Risks and mitigations

| Risk | Description | L | I | Mitigation | Early warning | Owner |
|---|---|---|---|---|---|---|
| RSK-040 | Noisy neighbours degrade small tenants | M | H | weighted fair queuing, per-tenant caps, dedicated Cells for large tenants | NR-PERF-022 miss in load tests | platform lead |
| RSK-041 | Migration orchestration corrupts or loses data | L | H | root comparison before cut-over, abort path, 10 rehearsals | any root mismatch in rehearsals | platform lead |
| RSK-042 | Few third parties publish or implement (ASM-010) | M | H | partner programme, revenue share, first-party templates, developer relations | fewer than 3 publishers in beta at month 4 | ecosystem lead |
| RSK-043 | SOC 2 findings delay GA | M | M | readiness assessment in month 2; control owners named | readiness gaps | security lead |
| RSK-044 | Exchange becomes a supply-chain attack vector | M | H | reproducible verification, signatures, namespace control, advisories and yanking | suspicious uploads | Exchange operator |
| RSK-045 | Mobile embedding of the kernel is unstable (ASM-009) | M | M | thin mobile client fallback without embedded kernel | crash rate > 1% in beta | client lead |
| RSK-046 | Offline conflicts confuse users | M | M | clear conflict screen; rejections with reasons; field-worker usability tests | conflict-resolution task success < 80% | design lead |
| RSK-047 | SQLite behaviour differs from PostgreSQL late in the product life (DEC-024) | M | M | the storage adapter contract and all vectors run on both from the first SQLite build; differences are standard errata | any vector passing on one backend only | kernel lead |

## 11. Reference duration and team assumption

Non-normative. 9 months. Team of 18–20 plus operations (SRE on-call rotation of at least 5),
an ecosystem and developer-relations function, and compliance support.
