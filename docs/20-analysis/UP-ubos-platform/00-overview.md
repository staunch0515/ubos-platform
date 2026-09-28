---
id: ANA-UP-00
title: "UP Overview"
status: complete
phase: P1
depends_on: [INV-UP]
sources: [UP]
---

# UP (ubos-platform) — Overview

## Vision as stated by the repository

- First commit: **"UBOS Genesis v0.1 – The Digital Lifeform Engine."**
- The platform is described through a living-organism metaphor: *Genesis* creates the world,
  an *Immune System* protects it, a *Nervous System* propagates events, a *Bio-Clock*
  schedules work, and a *family tree* of branches carries inheritance (CON-UP-003).

## Problem it addresses

A business platform whose behavior (logic), presentation (UI) and configuration evolve at
runtime, **safely and traceably**, without redeploying code — with the ability to:
branch the world per tenant/region/environment, see who changed what as part of which
business operation, run logic as it was at any past point, and let an AI author new logic.

## Design in one paragraph

A Java/WebFlux kernel stores every artifact (data, Groovy logic, UI schemas, cron jobs,
event listeners, policies, users, types) as a **versioned entity**: an identity row, an
append-only chain of full JSON snapshot commits, and per-branch head pointers.
**Branches form an inheritance tree** with read fallback. **Processes** group commits into
auditable business operations. An **engine** executes logic entities in a sandbox with kernel
services injected, and wires listeners and cron entities to events and time. A small generic
HTTP API (boot / run / commit / view / audit) serves a **web OS shell** that renders
server-defined apps and a **Python AI copilot** that writes logic, browses views and replays
processes.

## File map

| File | Content | Key IDs |
|---|---|---|
| `01-architecture.md` | Layers, flows, deployment | CON-UP-001…005 |
| `02-core-model.md` | Identity, commits, heads, entity types, relations, process, auth context | CON-UP-010…018 |
| `03-runtime.md` | Logic execution, bindings, sandbox, time travel, events, cron, genesis | CON-UP-020…030 |
| `04-data-persistence.md` | Schema, snapshots, branch hierarchy, search index | CON-UP-040…045 |
| `05-interface-ui.md` | API, identity-driven branches, OS shell, SDUI, copilot | CON-UP-050…056 |
| `06-extensibility-ai.md` | Extension by commit, AI architect | CON-UP-060…062 |
| `07-design-docs-digest.md` | Commit messages, SQL comments, config comments | – |
| `08-highlights.md` | 12 highlights | HL-UP-001…012 |
| `09-concept-index.md` | Flat index | – |

## Top highlights (see 08)

1. HL-UP-001 Minimal Git-for-Data core.
2. HL-UP-002 Branch inheritance as overlay.
3. HL-UP-003 Process as auditable business change.
4. HL-UP-004 Everything is a versioned entity.
5. HL-UP-005 Events and cron as data.

## Notes for later phases

- The concepts here form the **baseline vocabulary** reused by later analyses: *entity
  instance, version chain, branch head, process log, process-entity map*.
- `db/sqls/improt_export.sql` already uses the `bpu_*` schema and `ubos://` handler URIs
  (CON-UP-045) — link to ANA-UB when analyzing actions.
- Open detail for P2: semantics of writes/deletes under branch inheritance are not defined
  in UP (only reads fall back).
