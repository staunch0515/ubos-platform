---
id: ANA-US-00
title: "US Overview"
status: complete
phase: P1
depends_on: [INV-US, ANA-UC-00, ANA-UC-07]
sources: [US, UC]
---

# US (ubos-system) — Overview

## Vision as stated by the repository

- README: **"One core, five clients; cross-platform; own binary protocol; zero-copy; high-performance vector processing."**
- `docs/front/UBOS.md` — *Architecture Whitepaper v2.0 "Compositional Era"*: **Git-for-Data + Pure Composition**. Behavior (atom) → Relation Type (molecule) → Entity Type (organism); a kernel reached over a length-prefixed JSON protocol (UBTP) with five atomic instructions (Handshake, Mutate, Emit, Query, Subscribe).
- `docs/front/EOP.md` — *Entity-Oriented Programming*: an entity is a state stream over time, behaviour is a pure function `S(n+1) = Action(S(n), Context, Input)`, relations are first-class; "DDD + Event Sourcing + FRP".
- `docs/front/FullSlice.md` — *Full-slice development*: the kernel is an **event bus + plugin scheduler**; business logic lives in relation types as data (Rhai/WASM) that vote on each lifecycle phase.
- `docs/prompt.md` — the same house rules as UC (everything is an entity, 6 tables, `logrums`, explicit `_extends`).

## Problem it addresses

The same platform goal as UC (a versioned, entity-only, script-driven business kernel). US
adds three things on top of that shared base:
- **metadata-driven interaction**: actions defined as finite-state machines, generator/submitter
  logic, and UI projection from types;
- **a transactional object graph** (inheritance-assembled composites and a unit-of-work session)
  inside every script execution;
- a **modular, multi-client code base** (Cargo workspace; server, CLI, web; desktop planned).

## Relation to UC

The two repositories are peers with a large shared base: 82 byte-identical files, mostly
documents. The digest of the shared corpus is **ANA-UC-07** (D-UC-01…58); this analysis only
covers what is different. The shared differences are:
- **Documents:** differ only in whitespace for shared files, apart from the genesis data.
  US-only documents: `front/UBOS.md`, `EOP.md`, `FullSlice.md`, `VsCode.md`, `front_05.md`,
  `三件套.md`, `渲染.md`; genesis `16_entity_actions.json`, `17_logic_actions.json`;
  `db/init_data/01_meta_type.json`; `console.md`.
- **Genesis:** every type in 01–15 gains `ui_modes`. Files 16/17 add the Action type
  hierarchy and two concrete actions.
- **Code:** see the file map in ANA-US-01 §"Mapping to UC".
  - Concepts CON-UC-001…046 apply to US unchanged unless restated here.
  - The new US modules are `graph/`, `session/`, `action_engine`, `schema_engine`,
    `executor/sync`, `syscalls/host_functions`, the server `action` and `syntax` handlers,
    the CLI, and the web `kernel/`, `engine/`, `Action/` and `widgets/`.

## Design in one paragraph

A Cargo workspace splits the UC-style kernel into `ubos_proto` (types), `ubos_store`
(Postgres) and `ubos_kernel`, with `ubos_server` (axum + worker/scheduler/event bus),
`ubos_cli` (boot + run `.rhai` scenario files) and `ubos_web` (React). Each script execution
gets an **EntitySession**: an identity map of **CompositeEntities** (head plus `_extends`
ancestors, assembled by an **ObjectAssembler** with cycle detection). In that session:
- `sys_db_get` returns the inherited, merged view;
- `sys_db_commit` stages changes;
- the executor flushes the staged changes at the end, as one unit of work.

**Actions** are entities in an inheritance chain Action → StatefulAction → TwoStepAction. They
carry `states` (each with a UI policy) and `transitions`. `transitions` holds event rules
(`from`, `to`, and `logic` or `logic_ref`) that bind to generator and submitter Logic entities.

`POST /action/transition` runs the bound logic synchronously and returns:
- the next state;
- its UI policy;
- data and a UI schema;
- the events that are now available.

The web client renders the returned schema with a **widget registry**. Widgets are addressable
as `ubos://logrums/Widget/<name>` and chosen per mode from the type's `ui_modes`.

## File map

| File | Content | Key IDs |
|---|---|---|
| `01-architecture.md` | Workspace, UC mapping, executors, CLI, UBTP whitepaper, event-bus/plugin design | CON-US-001…008 |
| `02-core-model.md` | Composition model, meta-relations, composite entity, EOP calculus, metadata spec, ui_modes, action types | CON-US-010…018 |
| `03-runtime.md` | Unit-of-work session, merged reads, action FSM, two-step pattern, sync executor, slicing, voting/blackboard/trace, emit flow | CON-US-020…031 |
| `04-data-persistence.md` | Schema reuse, genesis deltas, dev/prod DB, append-only checks | CON-US-040…043 |
| `05-interface-ui.md` | New endpoints, client kernel, URI widgets, universal/schema renderer, view engine, action automator, Rhai IDE | CON-US-050…058 |
| `06-extensibility-ai.md` | Extension points, neuro-symbolic integration, logic in relation types, VS Code, three-piece delivery, multi-client | CON-US-060…065 |
| `07-design-docs-digest.md` | US-only documents (D-US-01…12); shared corpus → ANA-UC-07 | – |
| `08-highlights.md` | 12 highlights | HL-US-001…012 |
| `09-concept-index.md` | Flat index | – |

## Top highlights

1. HL-US-001: metadata-defined action FSM, with inherited transitions, logic indirection and a UI policy per state.
2. HL-US-002: a unit-of-work session over inheritance-assembled composite entities.
3. HL-US-003: a composition model (behaviours → relation types → entity types) and entity-oriented programming axioms.
4. HL-US-004: mode-aware universal rendering, with widgets addressed as `ubos://` resources.
5. HL-US-005: slice projection and merge, which lets you view and edit one entity through any ancestor type.

## Notes for later phases

- Some US-only code is not wired in (recorded, not evaluated):
  - `action/transition.rs` and `engine/schema.rs` are not declared in `lib.rs`.
  - `schema_engine::sys_load_and_project` is not registered as a syscall.
  - `sys_invoke` is a stub that only handles `std_math`.
  - The web client calls `/view/{slug}/init`, `executePrepare` and `executeSubmit`, which the current server and client kernel do not provide.
  - Ephemeral eval never reaches its session flush, because it sets status `PENDING` on success but flushes only on `DONE`.
  - `exec_logic_sync` runs with a fixed identity (`logrums`/`dev`).
  - The worker flushes the session and then commits the job record in a separate transaction; UC committed the job in a single transaction.
- `ubos_client_sdk` is a cargo stub; the desktop (Tauri) member is commented out.
- The whitepaper names SQLite for development and describes the TCP/UBTP protocol; neither exists in US. LS should be checked for them in P2 topic 15.
