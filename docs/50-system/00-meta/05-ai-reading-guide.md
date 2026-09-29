---
id: UBS-META-05
title: AI Reading Guide
status: draft
phase: ALL
depends_on: [UBS-META-00, UBS-META-01, UBS-IDX-00]
---

# AI Reading Guide

This guide tells an AI agent (or a new engineer) how to use this volume efficiently and
safely. The volume is written to be machine-navigable: every normative statement has a
stable ID, every ID is defined exactly once, and every reference is checked by
`docs/tools/check_system.py`.

## 1. Orientation in five minutes

1. Read the README (`UBS-README`) and the charter (`UBS-META-00`): what UBOS is, the scope
   and the excluded long-term narratives.
2. Skim `UBS-META-01` (writing rules): ID patterns, the requirement block, method codes.
3. Open `INDEX/00-index.md`: generated catalogues of documents and IDs, statistics, and the
   design-by-system-and-phase matrix.
4. Read `UBS-PH-00`: the six phases, the systems per phase, and the common exit rules.

## 2. The layers and how they connect

| Layer | Folder | Main IDs | Answers the question |
|---|---|---|---|
| Requirements | `10-requirements/` | PER, CAP, SCN, FR, NR, CR, CST, ASM, GL | what must the product do, for whom, how well, under which rules |
| Architecture | `20-architecture/` | AR, CTR | which systems exist, how they talk, where they run, which rules bind all of them |
| Standard | `30-standard/` | STD, DAT, error codes | what exactly must any conformant kernel do (normative, language-neutral) |
| Systems | `40-systems/` | DSN, IF, DAT | how each of the 13 systems is designed and operated |
| Phases | `50-phases/` | PH, EXIT, MET, RSK | what is built when, and what evidence ends each phase |
| Verification | `60-verification/` | SUITE, VER | how every requirement and exit is verified |
| Decisions | `90-decisions/` | DEC | why choices were made |
| Indexes | `INDEX/` | UBS-IDX | generated catalogues (never edit) |

The links between layers are explicit fields:
- FR, NR and CR → **Verification** methods; CAP by numbering (FR-DOM-NNk → CAP-DOM-NN).
- STD clause → **Satisfies** (FR, NR) and **Conformance vectors** (VER-CONF ranges).
- DSN → **Origin** (FR, NR, CR, STD, AR it realises) and **Verification** methods.
- EXIT → evidence; VER → **Verifies** and **Supports** (EXIT).
- `60-verification/2x-trace-ph*.md` join all of this per phase (generated).

## 3. Task-oriented entry points

| If your task is … | Read, in order |
|---|---|
| Understand the product | `UBS-REQ-01` vision → `UBS-REQ-02` personas → `UBS-REQ-04` scenarios → `UBS-ARC-01` system family |
| Implement a kernel feature | the FR domain file → the STD clauses that satisfy it → the DVM design file (`UBS-SYS-DVM-02-*`) → `UBS-SYS-DVM-04` data → the phase plan → the VER items and vectors |
| Implement a feature of another system | the scenario → the FR items → the system chapter (`40-systems/NN-sys/02-functions*`) → its interfaces (`03-interfaces.md`) → contracts in `UBS-ARC-02` |
| Build a client or integration | `UBS-SYS-SDK-*` → UBTP (`UBS-STD-15`) → `UBS-SYS-NOD-03` interfaces |
| Build a Buk (business package) | `UBS-SYS-FRG-*` → class system (`UBS-STD-03`) → governance sheets (`UBS-STD-13`) → instruction set (`UBS-STD-10`) |
| Plan or review a phase | `UBS-PH-N` → `UBS-PHF-N` (feature list) → its exits → `60-verification/1x-items-*` → `2x-trace-phN` |
| Verify a requirement | `2x-trace-phN` row → the realising DSN/STD → the suite in `UBS-VER-04` → the method in `UBS-VER-01` → environment `UBS-VER-02` and dataset `UBS-VER-03` |
| Operate a system | the system's `07-operations.md` → `06-configuration.md` → `05-state-machines.md` |
| Resolve an apparent conflict | the item's **Origin** → `UBS-META-06` reconciliation → the decision log |

## 4. Reading rules

1. **IDs are the contract.** Cite IDs, never page positions or headings.
2. **Normative text** is only in `Statement`, `Clause`, `Acceptance`, `Target`,
   `Control reference` and `Pass criterion` fields, in exit criteria, and in schema or grammar
   blocks. Rationale, notes and examples are informative.
3. **Precedence** when two statements seem to conflict, highest first:
   1. a `STD-*` clause;
   2. a `CR-*` item;
   3. an `NR-*` item;
   4. an `FR-*` item;
   5. an `AR-*` rule;
   6. a `DSN-*` item;
   7. prose.

   Report any real conflict as an open question (`OQ-*`) in `UBS-META-03` instead of choosing
   silently.
4. **Phase discipline.** Do not implement a requirement before its phase unless a `DEC-*`
   record allows it. A phase ends only when every exit criterion has evidence (CST-015).
5. **Standard before code.** A change to observable kernel semantics needs an accepted
   standard change with vectors first (AR-033, DSN-BPA-010).
6. **Excluded topics** (`UBS-META-00` §3.2) never become features, even when an input
   document mentions them.
7. **Generated files** (`INDEX/`, `50-phases/PH-*/01-features.md`,
   `60-verification/2*-trace*.md`, `60-verification/05-numbering-registry.md` structure) are
   regenerated with `docs/tools/regen_all.sh`; edit their sources, not them.

## 5. Changing the volume

1. Keep IDs stable; never renumber or reuse. Withdraw with `**Status:** Withdrawn` and a
   reason.
2. Add new items inside the correct range or block (for VER numbers see `UBS-VER-00` §3 and
   the registry `UBS-VER-05`).
3. When a requirement changes phase or scope, record a decision (`DEC-*`).
4. Run `docs/tools/regen_all.sh`. It MUST report 0 errors; aim for 0 warnings.
5. Check the traceability summary (`UBS-VER-20`): Must gaps MUST stay at 0.

## 6. Glossary use

Terms are defined as `GL-*` items. Use each term exactly. Platform vocabulary (branch, commit,
merge, cseq) MUST NOT appear in business-user interfaces (FR-UX-041); the Workspace uses the
business terms listed in the glossary.

## 7. What the checker guarantees

| Check | Effect |
|---|---|
| front matter | every document has id, title, status, phase, depends_on |
| unique definitions | every ID is defined exactly once |
| references | every referenced ID (including VER numbers through registered ranges) exists |
| requirement blocks | FR, NR, CR and DSN blocks have all fields, valid codes and numbered acceptance |
| capability mapping | every FR maps to an existing capability |
| verification ranges | every VER item lies in a registered range; ranges do not overlap |
| exit support | every exit criterion is supported by at least one VER item |
