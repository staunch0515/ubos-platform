---
id: UBS-META-03
title: Progress, Batch Plan and Open Questions
status: draft
phase: ALL
depends_on: [UBS-META-00, UBS-META-01]
---

# Progress, Batch Plan and Open Questions

## 1. Current state

- **Stage:** Requirements volume (Batches B2–B9).
- **Next batch:** B6.
- **Stop point:** after B9, wait for product-owner confirmation (DEC-010).

## 2. Batch plan

### Stage A — Meta
- [x] **B1** Charter, writing rules, templates, inputs, decision log, progress, AI reading guide, README, `tools/check_system.py`

### Stage B — Requirements volume (`10-requirements/`)
- [x] **B2** `00-index`, `01-vision-and-scope`, `02-personas`, `03-capability-map`
- [x] **B3** `04-scenarios/`: platform core (SCN-0xx), fund operations (SCN-1xx), contracts (SCN-2xx), AI agents (SCN-3xx), operations and federation (SCN-4xx)
- [x] **B4** Functional: MODEL, VER, TIME, LEDG, TXN
- [x] **B5** Functional: LOGIC, RULE, FLOW, EVT, QRY
- [ ] **B6** Functional: IAM, TEN, AUD, UX, OFFICE
- [ ] **B7** Functional: AI, PKG, INT, ANL, PROOF, SYNC
- [ ] **B8** Functional: OPS, BILL, DEV, MIG, NTF, FILE, SIGN, LOC, STD
- [ ] **B9** Non-functional catalogue, compliance catalogue, constraints and assumptions, glossary seed, requirements index
- [ ] **STOP** Product-owner confirmation of the requirements volume

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
| — | none yet | — |

## 4. Log

| Date | Batch | Summary |
|---|---|---|
| 2026-09-29 | B1 | Meta layer created: charter, writing rules and ID scheme, templates, inputs and reconciliation, DEC-001…013, progress plan, AI reading guide, README, checker. |
| 2026-09-29 | B2 | Requirements index, vision and scope, 28 personas, capability map (220 capabilities, FR numbering rule). |
| 2026-09-29 | B3 | 55 reference scenarios (platform core 18, fund operations 10, contracts 8, AI 7, operations/federation 12) with phase assignment. |
| 2026-09-29 | B4 | Functional requirements MODEL (58), VER (50), TIME+LEDG (52), TXN (24): 184 FR in total. Checker now validates FR→capability mapping. |
| 2026-09-29 | B5 | Functional requirements LOGIC (46), RULE (31), FLOW (34), EVT (32), QRY (17): FR total 344. |
