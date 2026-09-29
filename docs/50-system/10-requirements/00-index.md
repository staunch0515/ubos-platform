---
id: UBS-REQ-00
title: Requirements Volume — Index
status: review
phase: ALL
depends_on: [UBS-META-00, UBS-META-01]
---

# Requirements Volume — Index

This volume states **what** the UBOS system family must do and how well it must do it. It
does not describe how the requirements are implemented. The architecture, standard and
system chapters (`20-`, `30-`, `40-`) do that and trace back to these IDs.

## Documents

| ID | File | Content |
|---|---|---|
| UBS-REQ-01 | `01-vision-and-scope.md` | problem, vision, principles, value propositions, product layers, editions, goals, scope, competitive frame |
| UBS-REQ-02 | `02-personas.md` | 28 personas across business, assurance, builder, platform, AI and ecosystem layers |
| UBS-REQ-03 | `03-capability-map.md` | 220 capabilities in 30 domains, with phases and FR numbering ranges |
| UBS-REQ-04 | `04-scenarios/` | end-to-end reference scenarios (SCN-*) |
| UBS-REQ-05 | `05-functional/` | functional requirements (FR-*) by domain |
| UBS-REQ-06 | `06-nonfunctional/` | non-functional requirements (NR-*) with measurable targets |
| UBS-REQ-07 | `07-constraints-assumptions.md` | constraints (CST-*) and assumptions (ASM-*) |
| UBS-REQ-08 | `08-compliance.md` | compliance requirements (CR-*) |
| UBS-REQ-09 | `09-glossary/` | glossary terms (GL-*) |

## How to read a requirement

Every FR, NR and CR item follows the block in `UBS-META-01` §5:
- **Statement** holds the normative obligation.
- **Acceptance** lists observable Given/When/Then checks.
- **Verification** lists method codes. Their meaning is in `UBS-META-01` §4.5.
- **Phase** is the phase that delivers the item.
- **Origin** traces the item to its inputs (`UBS-META-06`).

The FR number encodes the capability: `FR-VER-051` refines `CAP-VER-05`.

## Volume statistics (end of batch B9)

| Item kind | Count |
|---|---|
| Personas (PER) | 28 |
| Capabilities (CAP) | 220 in 30 domains |
| Scenarios (SCN) | 55 |
| Functional requirements (FR) | 800 |
| Non-functional requirements (NR) | 106 |
| Compliance requirements (CR) | 54 |
| Constraints (CST) and assumptions (ASM) | 18 and 12 |
| Glossary terms (GL) | 146 |
| Reference environments (ENV) | 7 |

Requirements by kind, phase and priority:

| Kind | PH-0 | PH-1 | PH-2 | PH-3 | PH-4 | PH-5 | Total | Must | Should | Could |
|---|---|---|---|---|---|---|---|---|---|---|
| FR | 8 | 304 | 274 | 152 | 45 | 17 | 800 | 673 | 118 | 9 |
| NR | 0 | 32 | 51 | 12 | 10 | 1 | 106 | 82 | 24 | 0 |
| CR | 0 | 5 | 20 | 19 | 10 | 0 | 54 | 37 | 17 | 0 |
| **All** | 8 | 341 | 345 | 183 | 65 | 18 | 960 | 792 | 159 | 9 |

## Status

All requirement documents are in `review` status and wait for product-owner confirmation
(DEC-010). After confirmation they move to `complete`, and writing continues with the
architecture volume.
