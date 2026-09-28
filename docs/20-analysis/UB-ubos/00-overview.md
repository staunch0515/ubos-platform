---
id: ANA-UB-00
title: "UB Overview"
status: complete
phase: P1
depends_on: [INV-UB]
sources: [UB]
---

# UB (ubos) — Overview

## Vision as stated by the repository

- README: **"Universal Business OS – A Git-like Logic Engine."**
- Schema header: "Everything is an Entity" · "Git for Data" · "Audit by Default" · "Billing Ready".
- `三块拼图.md`: the system is three layers of language — **BEL** (the Creator's language: schema, constitution, assets), **action scripts** (the manager's language), **Executable Book** (the employee's desk).
- `IDE.md`: "from writing code to defining life" — logic is data, the IDE lives inside the system, deployment disappears.

## Problem it addresses

A business operating system in which *every* business concept — types, organizations,
money, documents, screens, workflows, permissions, logic — is versioned data in a single
self-describing model, addressed uniformly by URI within a context (tenant + branch + time),
evaluated through inheritance and references, and changed only through auditable,
optimistically-locked transactions; the system should be extendable by people and AI without
redeployment.

## Design in one paragraph

A Java/WebFlux kernel with three layers: **BPU** (versioned storage: batch commits with CAS on
branch heads, branches, merge with four strategies incl. three-way, revert, diff as JSON Patch,
micro-commits) exposed as an **opcode enum**, **ESE** (entity state: initialize, draft/publish on
per-user branches, evaluation with inheritance composition and `{{ubos://}}` reference
resolution, actions → logic scripts, search) as a second opcode enum composed from the first,
and a **protocol** layer resolving `ubos://tenant[/branch]/type/slug` via **context entities**.
Every call carries one **ProcessContext** that records a tree of handler frames. Storage is a
six-table, tenant-aware, partitioned PostgreSQL schema with JSONB snapshots, vector embeddings,
automatic property index, lifecycle status, owner and billing linkage. A 17-phase **genesis
protocol** seeds a self-describing semantic type system and a universal business ontology. A
16.7k-line design corpus specifies SDUI, an inheritance-aware Studio/IDE, executable documents,
the BEL rules language and AI-driven development.

## File map

| File | Content | Key IDs |
|---|---|---|
| `01-architecture.md` | Layers, SPI, opcode enums, handler convention, API, OS blueprint | CON-UB-001…006 |
| `02-core-model.md` | Entity definition, identity, registry, type system, ontology, UbosEntity, context entity, URI, templates | CON-UB-010…019 |
| `03-runtime.md` | ProcessContext, commit, drafts, merge, revert, diff, micro-commit, lifecycle, evaluation, actions, script SDK, genesis, state machine, boot | CON-UB-020…033 |
| `04-data-persistence.md` | Schema v2.1, branch height, search index, billing, tombstones, caching, archives | CON-UB-040…047 |
| `05-interface-ui.md` | SDUI, actions, smart forms, Studio, Meta-IDE, smart document, pages, type-driven widgets | CON-UB-050…057 |
| `06-extensibility-ai.md` | Language layers, BEL, apps, AI builder, i18n, embeddings, LLM strategy, prompts | CON-UB-060…067 |
| `07-design-docs-digest/` | Index + 3 parts covering all 23 documents (D-UB-01…20) | – |
| `08-highlights.md` | 16 highlights | HL-UB-001…016 |
| `09-concept-index.md` | Flat index | – |

## Top highlights

1. HL-UB-001 Entity as logical reference with context; context is an entity.
2. HL-UB-002 Tenant = DB, Type = table, Slug = key; canonical `ubos://` URI.
3. HL-UB-003 Semantic type system from a single root.
4. HL-UB-004 Universal business ontology as genesis data.
5. HL-UB-005 Reified execution stack + opcode enums.
6. HL-UB-016 Intent / Rules / Facts separation.

## Notes for later phases

- Much of UB is **design without implementation** (BEL, ESE state machines, Meta-IDE, smart documents, package manager, network boot); the analysis marks "(design)" where applicable.
- Divergence: cache stores effective JSON although the design says cache raw fragments.
- Default branch here is `main` (UP/FU use `master`); draft naming `draft/{user}` (FU: `draft/{user}/{base}`).
- UB's genesis type chain (`sys.entity.root`, `sys.meta.type`, `_extends`) uses the same vocabulary as the UC/US genesis data; compare in P2 topic 17.
