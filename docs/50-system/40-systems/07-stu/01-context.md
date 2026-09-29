---
id: UBS-SYS-STU-01
title: Studio — Context
status: draft
phase: PH-2
depends_on: [UBS-SYS-STU-00]
---

# Studio — Context

| Neighbour | Direction | Contract | What flows |
|---|---|---|---|
| NOD (and DVM) | STU → NOD | CTR-011 | reflection, commits on builder branches, change sets, merge previews, simulations, impact, tags |
| FRG service on NOD | STU → NOD → FRG | CTR-012 | checks, types, lens verification, Buk verification |
| AGT | STU → AGT | CTR-024 | drafting assistance, explanations |
| BRG | STU → NOD → BRG | CTR-040 | spreadsheet and document imports |
| WSP | shared shell | — | Studio is a set of WSP surfaces gated by role layer |
