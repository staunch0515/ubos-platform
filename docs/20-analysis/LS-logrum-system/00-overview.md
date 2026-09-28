---
id: ANA-LS-00
title: "LS Overview"
status: complete
phase: P1
depends_on: [INV-LS]
sources: [LS]
---

# LS (logrum-system) — Overview

## Vision as stated by the repository

- README: "One core, five clients; cross-platform; own binary protocol; zero-copy; high-performance vector processing".
- `claude.md` ("Relational Architecture Refactoring", 2026-01-25) states four rules:
  - **"Entity is just a UUID; everything else is a Relation."**
  - "tenantId can only be `logrums`."
  - "Data can only be saved by calling `commit_changes`."
  - "No comments in JSON files."
- The core logic in `claude.md`:
  - A **BehaviorType** is a slot (interface).
  - A **Behavior** is an implementation of a slot, with a *refine* inheritance chain executed layer by layer (onion / chain of responsibility).
  - A **RelationType** is a configurator that fills each slot with a Behavior.
  - A Process emits a slot signal; the engine walks the entity's relation types and executes the configured behaviors.
- The principle: **"scripts only give orders, the engine does the running"**, with a three-stage Action protocol (Init → Interact → Exec).

## Problem it addresses

A business kernel in which no business code is written per entity type. Behaviour is attached to
**relations** (properties) through slot configurations. A small set of generic processes (for
example a universal editor) drives any entity by emitting slot signals. Clients speak a minimal
binary protocol, and the same kernel runs on SQLite (personal/edge) or PostgreSQL (enterprise).

## Design in one paragraph

A Cargo workspace (`ubos_proto`, `ubos_store`, `ubos_kernel`, `apps/ubos_server`) serves a
**TCP protocol (UBTP)**:
- Frames are 4-byte big-endian length-prefixed JSON.
- There are five operations: `Handshake`, `Mutate`, `Emit`, `Query`, `Subscribe`.
- Each operation is answered with an `Ack`, followed by streamed side-effect ops pushed to `client://current`.

Entity payloads are maps keyed by **relation-type URIs**
(`ubos://logrums/MetaRelationType/label`, `…/prop_email`, `…/behaviorConfig`, …):
- A `MetaType` lists its relation types.
- A `MetaRelationType` carries a `behaviorConfig` that maps BehaviorType URIs to Behavior implementations.
- `BehaviorType`s (20 slots) and `Behavior`s are entities too.

The `ProcessOrchestrator` handles incoming operations:
- `Mutate` becomes `ChangeSet`s committed atomically across the six `bpu_*` tables, including the search index and lineage.
- `Emit` on a process entity runs one state-machine step. The script returns `next_step`, `variables` and `emits`; the new state is committed and the emits are returned to the client.
- `Emit` on any other entity dispatches a behavior slot (currently a static mock for `SchemaProvideSlot`).
- `Query` returns a `QueryResult` emit.

At boot:
- A genesis bootloader loads JSON files 01–06 idempotently.
- Two semantic layers cache type and relation-type nodes by commit id (DashMap: URI→commit, commit→raw, commit→assembled).

A set of 8 kernel mechanisms (resolution, extends, policy, prerequisite, schema, transformation, implement, reactor) is sketched as a pipeline design.

## File map

| File | Content | Key IDs |
|---|---|---|
| `01-architecture.md` | Workspace, UBTP server, orchestrator routing, client-as-target, backends, semantic caches, mechanism pipeline | CON-LS-001…008 |
| `02-core-model.md` | UUID + relations, meta bootstrap, relation type as slot configurator, slot catalogue, behaviors/refine, entity types, identity/URI, process/action entities, ChangeSet | CON-LS-010…019 |
| `03-runtime.md` | Process step, emit-only scripts, three-stage action, assembly/merge, genesis bootloader, emits as intents, dry-run, blocking mechanisms, reactor, query | CON-LS-020…029 |
| `04-data-persistence.md` | SQLite schema, atomic commit pipeline, auto search index, criteria reader, commit-keyed cache | CON-LS-040…044 |
| `05-interface-ui.md` | Protocol clients, server-pushed UI ops, schema from slots, UI slot behaviors, universal editor as data | CON-LS-050…054 |
| `06-extensibility-ai.md` | Extension model, native function namespace, editions, prompt contract, tracing | CON-LS-060…064 |
| `07-design-docs-digest.md` | claude.md, genesis 01–07, docs/data, console, README (D-LS-01…08) | – |
| `08-highlights.md` | 11 highlights | HL-LS-001…011 |
| `09-concept-index.md` | Flat index | – |

## Top highlights

1. HL-LS-001: Entity = UUID; everything else is a relation keyed by a relation-type URI.
2. HL-LS-002: Behavior slots — BehaviorType (interface), Behavior (implementation), RelationType (slot configurator).
3. HL-LS-003: "Scripts only emit, the engine runs" — a three-stage action protocol over slot signals.
4. HL-LS-004: UBTP implemented — a TCP length-prefixed protocol with five operations and server push to `client://current`.
5. HL-LS-005: Pluggable storage editions (SQLite personal / Postgres enterprise) behind store traits, with a fully atomic six-table commit.

## Notes for later phases

- **Design-sketch modules.** `mechanism/*`, `kernel.rs`, `pipeline/`, `engine/process_runner.rs`, `executor/adapter.rs`, `process/def.rs` and `apps/ubos_server/src/tcp_server.rs` reference types that are not defined in the crates (`RecursiveMerger`, `PolicyDecisionPoint`, `Entity.relations`, `UbosOp::Notify`, `kernel_bus` …). They are treated as design, not as working code.
- **Divergences:**
  - The type and relation assemblers look for the `_extends` key, but genesis data uses the `ubos://logrums/MetaRelationType/extends` relation, so inheritance is not applied at runtime.
  - Behavior dispatch is a static mock (the `SchemaProvideSlot` fields are hard-coded by entity id).
  - `07_action.json` is not valid JSON (it contains a stray token) and is not in the bootloader list; the loader loads `06_action.json`, an earlier action variant.
  - Process scripts use two conventions: `current_step/variables/signal → next_step/variables/emits` in code and tests, and `CURSOR/SIGNAL/MEMORY/PAYLOAD → next/instructions/ui_schema` in genesis.
  - Emit routing treats only the entity types `MetaProcessType` and `PROCESS` as processes; the genesis type `ProcessInstanceType` is not routed.
- **Cross-repo comparison (for P2):**
  - LS **implements** the UBTP/five-instruction design stated in the US whitepaper (CON-US-006) and the SQLite development backend (CON-US-042); P2 topic 15.
  - The relation/behavior model fulfils the US composition model (CON-US-010) and relates to LC's relationship-as-entity and UB's semantic types; P2 topic 2.
