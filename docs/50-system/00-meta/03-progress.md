---
id: UBS-META-03
title: Progress, Batch Plan and Open Questions
status: draft
phase: ALL
depends_on: [UBS-META-00, UBS-META-01]
---

# Progress, Batch Plan and Open Questions

## 1. Current state

- **Stage:** Architecture volume (Stage C).
- **Next batch:** B10.
- **Stop point:** after B9, wait for product-owner confirmation (DEC-010).

## 2. Batch plan

### Stage A — Meta
- [x] **B1** Charter, writing rules, templates, inputs, decision log, progress, AI reading guide, README, `tools/check_system.py`

### Stage B — Requirements volume (`10-requirements/`)
- [x] **B2** `00-index`, `01-vision-and-scope`, `02-personas`, `03-capability-map`
- [x] **B3** `04-scenarios/`: platform core (SCN-0xx), fund operations (SCN-1xx), contracts (SCN-2xx), AI agents (SCN-3xx), operations and federation (SCN-4xx)
- [x] **B4** Functional: MODEL, VER, TIME, LEDG, TXN
- [x] **B5** Functional: LOGIC, RULE, FLOW, EVT, QRY
- [x] **B6** Functional: IAM, TEN, AUD, UX, OFFICE
- [x] **B7** Functional: AI, PKG, INT, ANL, PROOF, SYNC
- [x] **B8** Functional: OPS, BILL, DEV, MIG, NTF, FILE, SIGN, LOC, STD
- [x] **B9** Non-functional catalogue, compliance catalogue, constraints and assumptions, glossary seed, requirements index
- [x] **STOP** Product-owner confirmation of the requirements volume (2026-09-29; OQ-001…003 resolved by DEC-014…016)

### Stage C — Architecture (`20-architecture/`)
- [ ] **B10** System family, boundaries, contracts between systems, topologies, data flows, cross-cutting concerns

### Stage D — Standard (`30-standard/`)
- [ ] **B11** DVM semantics: object model, classes, inheritance, polymorphism, kinds
- [ ] **B12** Versioning, bitemporality, content addressing, commit semantics, merge
- [ ] **B13** BPU model, instruction set (ABI), execution context, determinism, metering
- [ ] **B14** LGS language, addressing and URIs, UBTP protocol, sync protocol, error registry, conformance rules

### Stage E — Systems (`40-systems/`)
- [ ] **B15–B16** DVM
- [ ] **B17** NOD, CTL
- [ ] **B18** FRG, SDK
- [ ] **B19** WSP, STU
- [ ] **B20** AGT, EXC
- [ ] **B21** FED, NTY, BRG
- [ ] **B22** BPA (governance of the standard and its conformance programme)

### Stage F — Phases (`50-phases/`)
- [ ] **B23** PH-0, PH-1, PH-2
- [ ] **B24** PH-3, PH-4, PH-5

### Stage G — Verification (`60-verification/`)
- [ ] **B25** Methods, environments, datasets, suites
- [ ] **B26** Verification items by phase, traceability

### Stage H — Closing
- [ ] **B27** Generated indexes, final AI reading guide, README update, full validation

## 3. Open questions

| ID | Question | Status |
|---|---|---|
| OQ-001 | PH-1 carries 341 requirements (kernel breadth). Should PH-1 be split into PH-1a (store, model, versioning, transactions, logic) and PH-1b (rules, flow basics, events, IAM, proofs), each with its own exit gate? | Resolved by DEC-014 |
| OQ-002 | The first vertical covers fund operations and contracts in PH-2. Should either be deferred to PH-3 to shorten PH-2? | Resolved by DEC-015 |
| OQ-003 | Hosted Cell (PH-4) comes after Server (PH-1/PH-2). Is on-premises/private-cloud Server the confirmed first commercial form? | Resolved by DEC-016 |

## 4. Log

| Date | Batch | Summary |
|---|---|---|
| 2026-09-29 | B1 | Meta layer created: charter, writing rules and ID scheme, templates, inputs and reconciliation, DEC-001…013, progress plan, AI reading guide, README, checker. |
| 2026-09-29 | B2 | Requirements index, vision and scope, 28 personas, capability map (220 capabilities, FR numbering rule). |
| 2026-09-29 | B3 | 55 reference scenarios (platform core 18, fund operations 10, contracts 8, AI 7, operations/federation 12) with phase assignment. |
| 2026-09-29 | B4 | Functional requirements MODEL (58), VER (50), TIME+LEDG (52), TXN (24): 184 FR in total. Checker now validates FR→capability mapping. |
| 2026-09-29 | B5 | Functional requirements LOGIC (46), RULE (31), FLOW (34), EVT (32), QRY (17): FR total 344. |
| 2026-09-29 | B6 | Functional requirements IAM, TEN, AUD, UX, OFFICE: FR total 493. |
| 2026-09-29 | B7 | Functional requirements AI, PKG, INT, ANL, PROOF, SYNC: FR total 672. FR-PROOF-075 moved to PH-3 (holds arrive in PH-3). |
| 2026-09-29 | B8 | Functional requirements OPS, BILL, DEV, MIG, NTF, FILE, SIGN, LOC, STD: FR total 800 (PH-0 8, PH-1 304, PH-2 274, PH-3 152, PH-4 45, PH-5 17). Every capability has FRs; 3 capability phases aligned to earliest FR. |
| 2026-09-29 | B9 | Non-functional catalogue (106 NR, 7 reference environments, 6 workloads), compliance catalogue (54 CR), 18 constraints, 12 assumptions, glossary (146 terms). Requirements volume set to `review`. Checker strict for requirement kinds. STOP for confirmation. |
| 2026-09-29 | STOP | Product owner confirmed the requirements volume and accepted the recommendations: DEC-014 (PH-1a/PH-1b gates), DEC-015 (both verticals in PH-2), DEC-016 (Server first). DEC-017 adds AR and CTR ID kinds. Requirements set to `complete`. |
