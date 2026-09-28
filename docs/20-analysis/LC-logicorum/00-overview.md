---
id: ANA-LC-00
title: "LC Overview"
status: complete
phase: P1
depends_on: [INV-LC]
sources: [LC]
---

# LC (logicorum) — Overview

## Vision as stated by the repository

- `docs/README.md`: processes and handlers carry versions so that "parallel business systems can run in one instance"; a six-table core where a **process** (business operation) is decoupled from **entity versions** and linked to them by a map.
- The repo is the backend of **QuizBucks** ("answer quizzes, earn cash") and its admin platform, built as a generic engine ("LCM" = life-cycle management prefix of all tables).

## Problem it addresses

Build real business applications quickly and safely by **declaring** operations instead of
coding them: each API call is a versioned process made of reusable steps, all writes of a
process are one atomic unit of work recorded with history, entities and relationships are
validated by schemas and graph constraints, and production concerns (idempotency, audit,
sessions, email) are provided by the platform.

## Design in one paragraph

A Java/WebFlux engine exposes `/{version}/{domain}/{operation}` and maps it to a process
definition file (`AUTH_REGISTER_V1`). The engine validates input against the process's JSON
Schema, checks required roles, then runs a pipeline of **handler beans** configured by
`stepMetadata`, with jumps and SpEL decisions. Steps stage **change sets** on a shared
context; a persistence step commits them in one transaction into a Git-like store
(instance / version chain with commit types / branch head / process log / process-entity
map) whose content is **typed EAV rows per commit**. **Relationships are entities** typed by
a catalogue (allowed endpoints, symmetry, validity, metadata schema). Validation combines JSON
Schema with pluggable business constraints applied at **DRAFT_SAVE** or **FINAL_PUBLISH**.
Idempotency keys, request logs, JSON transaction files and an email outbox make it
production-oriented. Two React apps (taker app, admin) consume it.

## File map

| File | Content | Key IDs |
|---|---|---|
| `01-architecture.md` | Layers, process API, CQRS, envelope, loaders, handler beans, flow | CON-LC-001…005 |
| `02-core-model.md` | Process format, context, change sets, versioning by name, relationships, schemas, app scope, access, template/publication | CON-LC-010…018 |
| `03-runtime.md` | Execution, jumps/decisions, atomic persistence, staged validation, constraints, generic handlers, idempotency, schedulers, domain handlers | CON-LC-020…029 |
| `04-data-persistence.md` | Schema, EAV, process log, soft delete, operational tables, dual audit, branch staging, Redis plan | CON-LC-040…046 |
| `05-interface-ui.md` | Response contract, taker app, admin app, generated frontends | CON-LC-050…053 |
| `06-extensibility-ai.md` | Extension model, data-driven tests, AI features, coding rules | CON-LC-060…063 |
| `07-design-docs-digest.md` | All docs + commit themes | – |
| `08-highlights.md` | 13 highlights | HL-LC-001…013 |
| `09-concept-index.md` | Flat index | – |

## Top highlights

1. HL-LC-001 API = catalogue of versioned declarative processes.
2. HL-LC-002 Unit of work: stage change sets, commit once.
3. HL-LC-003 Relationships as versioned entities with a typed catalogue.
4. HL-LC-004 Staged validation (DRAFT_SAVE / FINAL_PUBLISH).
5. HL-LC-006 Production concerns as platform features.

## Notes for later phases

- LC stores version content as EAV rows; UP/FU/UB store JSON snapshots — P2 topic 1.
- LC branches are per-request staging (`draft`, `main`) with no inheritance or merge — contrast UP/FU.
- Handler library is code; process composition is data — contrast UP/UC/US (logic as scripts) and LS (behavior slots).
- Several designed-but-unused hooks: `isRemote` steps, `handler_versions`, symmetric-relationship reverse creation, `relationshipsToEnforce`.
