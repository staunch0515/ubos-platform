---
id: UBS-README
title: UBOS Business Processing Architecture — System Specification
status: draft
phase: ALL
depends_on: []
---

# UBOS Business Processing Architecture — System Specification

This is the **authoritative, self-contained specification** of the UBOS Business
Processing Architecture (BPA) and the systems built on it. It supersedes `docs/40-spec/`
as the current authority. The earlier volume is kept as history (DEC-008).

**The idea in one sentence:** UBOS standardises how business state changes over time.
State is typed, inheritable, versioned, bitemporal, governed and provable. Business policy
stays soft and versioned. Every contract or order runs as a standard **Business
Processing Unit (BPU)** on a **Data Virtual Machine (DVM)**.

## Start here

| You want to … | Open |
|---|---|
| know what this volume is and is not | [`00-meta/00-charter.md`](00-meta/00-charter.md) |
| understand the sources and resolved conflicts | [`00-meta/06-inputs.md`](00-meta/06-inputs.md) |
| know how IDs and requirement blocks work | [`00-meta/01-writing-rules.md`](00-meta/01-writing-rules.md) |
| work as an AI agent on this volume | [`00-meta/05-ai-reading-guide.md`](00-meta/05-ai-reading-guide.md) |
| see progress and the batch plan | [`00-meta/03-progress.md`](00-meta/03-progress.md) |
| see decisions | [`90-decisions/000-index.md`](90-decisions/000-index.md) |

## Folder map

| Folder | Content | Status |
|---|---|---|
| `00-meta/` | charter, rules, templates, progress, inputs, AI guide | stable / draft |
| `10-requirements/` | vision, personas, capabilities, scenarios, FR, NR, CR, constraints, glossary | complete (confirmed 2026-09-29) |
| `20-architecture/` | system family, boundaries, contracts, topologies | planned |
| `30-standard/` | the BPA standard (normative) | planned |
| `40-systems/` | one folder per system (13 systems) | planned |
| `50-phases/` | PH-0 … PH-5 with verification plans and exit criteria | planned |
| `60-verification/` | methods, suites, environments, traceability | planned |
| `90-decisions/` | DEC-* records | active |
| `INDEX/` | generated indexes | planned |

## Validation

```
python3 docs/tools/check_system.py
```
The command must exit 0 before every commit.
