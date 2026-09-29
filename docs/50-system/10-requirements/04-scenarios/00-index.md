---
id: UBS-REQ-04
title: Reference Scenarios — Index
status: review
phase: ALL
depends_on: [UBS-REQ-02, UBS-REQ-03]
---

# Reference Scenarios — Index

Scenarios are **end-to-end, observable business stories**. They serve three purposes:
1. They validate that the requirements are complete. Every step maps to capabilities and
   functional requirements.
2. They are the **acceptance backbone** of the phases. A phase cannot exit unless every
   scenario assigned to it passes as an automated scenario run (method `SCN`) on all
   supported bindings.
3. They give AI implementers concrete behaviour to reason about.

**Business rules exercised** cites the first requirement of each capability involved
(`FR-<DOM>-<NN>1`). The functional catalogue lists that capability's other requirements.

## Files

| File | Range | Theme |
|---|---|---|
| `01-platform-core.md` | SCN-001 … SCN-018 | kernel and platform mechanics, first run in PH-1 and PH-2 |
| `02-fund-operations.md` | SCN-101 … SCN-110 | first vertical: fund and asset-management back office (DEC-004) |
| `03-contracts.md` | SCN-201 … SCN-208 | first vertical: contract management (DEC-004) |
| `04-ai-agents.md` | SCN-301 … SCN-307 | governed AI (IMP-10) |
| `05-operations-federation.md` | SCN-401 … SCN-412 | offline, ecosystem, operations, federation |

## Scenario-to-phase summary

| Phase | Scenarios that MUST pass at exit |
|---|---|
| PH-1 | SCN-001 … SCN-011, SCN-017, SCN-018 |
| PH-2 | SCN-012 … SCN-016, SCN-101 … SCN-108, SCN-110, SCN-201, SCN-202, SCN-204 … SCN-206, SCN-208, SCN-405, SCN-406 |
| PH-3 | SCN-109, SCN-203, SCN-207, SCN-301 … SCN-307, SCN-401, SCN-402, SCN-412 |
| PH-4 | SCN-403, SCN-404, SCN-407, SCN-410 |
| PH-5 | SCN-408, SCN-409, SCN-411 |

Every scenario that passed in an earlier phase MUST still pass in every later phase
(regression rule).
