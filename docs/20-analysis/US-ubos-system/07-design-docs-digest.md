---
id: ANA-US-07
title: "US Design Docs Digest"
status: complete
phase: P1
depends_on: [ANA-UC-07]
sources: [US, UC]
---

# US — Design Documents Digest

**Shared corpus:** `docs/master/*`, `docs/plan/*`, `docs/front/front_01…04`, `docs/init_data.md`,
`docs/book.md`, `docs/prompt.md`, `docs/db/V1_init.sql` are identical to UC (whitespace-only
differences in `book.md`, `prompt.md`, `front_04.md`). Their digest is **ANA-UC-07**
(D-UC-01…58) and applies to US unchanged. Below: US-only or differing documents.

| D-id | Document | Lines | Topic | Concepts |
|---|---|---|---|---|
| D-US-01 | front/UBOS.md | 158 | Architecture whitepaper v2.0 | CON-US-006, 008, 010, 011, 042 |
| D-US-02 | front/EOP.md | 228 | Entity-Oriented Programming calculus | CON-US-013 |
| D-US-03 | front/FullSlice.md | 868 | Full-slice development, event bus + plugin scheduler, LLM integration, process prototype | CON-US-007, 008, 027–030, 060, 062 |
| D-US-04 | front/VsCode.md | 164 | VS Code extension | CON-US-057 |
| D-US-05 | front/front_05.md | 257 | Metadata specification v1.0 | CON-US-014 |
| D-US-06 | front/三件套.md | 139 | Three-piece set: types, TwoStepAction, action, renderer | CON-US-017, 023, 058, 063 |
| D-US-07 | front/渲染.md | 710 | Recursive schema, projection, type-cast view, slicing | CON-US-016, 026 |
| D-US-08 | db/genesis_data/01–15 | – | + `ui_modes` | CON-US-015, 041 |
| D-US-09 | db/genesis_data/16, 17 | 330 | Action hierarchy and actions | CON-US-017, 023 |
| D-US-10 | db/init_data/01_meta_type.json | 51 | Root + generic Entity view type | CON-US-016 |
| D-US-11 | console.md, README.md | 44 | Dev setup, AI context bundling, tagline | CON-US-042, 064, 065 |
| D-US-12 | apps/ubos_cli/tests/*.rhai | 260 | Executable scenarios | CON-US-004, 043 |

### D-US-01 — Whitepaper v2.0 "Compositional Era"
- Summary of "all core decisions" after moving from OOP to a pure entity-relation composition model.
- Philosophy: store change history, not current state; immutability; `main` branch per entity, checkout to history/time; data = Registry (identity) + Version (snapshot) + Head (pointer).
- Composition over inheritance: Behavior → Relation Type → Entity Type; core meta-relations `sys_primitive`, `sys_descriptor`.
- Architecture: Client ⇄ TCP/UBTP ⇄ Server (codec, ProcessContext, five instructions) → Kernel (ProcessOrchestrator, RhaiAdapter, SemanticLayer) → Store (PersistenceEngine; tables instance, version chain, head, process log, process-entity map).
- Flows: genesis by loading behaviors → relations → types JSON through the kernel; Emit flow (load head → inject signal/args/user → run Rhai → commit new state → dispatch side effects).
- Stack rules (tokio, rhai sync+serde, SQLite dev / Postgres prod, sqlx raw SQL, serde, anyhow). "All implementation must follow this document."

### D-US-02 — Entity-Oriented Programming (EOP)
- Critique: "low-code configuration" thinking replaced by language architecture (type system, state machines, category theory); UBOS as a distributed, temporal, content-based computing environment.
- Axioms (spacetime entity, pure behaviour, first-class relations); E-DSL JSON as AST with topology/invariants/derivations; Action descriptors with guards, mutation-as-transaction, declared effects; generic Rust `EntityFrame` with schema hash, type erasure and JIT; features: orthogonal persistence, structural polymorphism, time-travel debugging.

### D-US-03 — Full-slice development
- Part 1 "four improvements": logic as data in relation types (Rhai/WASM; hot update, versioned rules, tenant polymorphism), slice voting (Decision with priority/veto), context blackboard, causal traceability (visual Gantt of slices) → "adaptive digital life form", "generative software architecture".
- Part 2 event bus + plugin scheduler: five lifecycle phases, discovery via `_extends`/relations, priority, decisions; relation type example with behaviours; advantages (decoupling, hot-plug, sandbox, visual debugging).
- Part 3 LLM combination (symbolic vs connectionist AI; four levels, CON-US-060).
- Part 4 process prototype: five opcodes, `ProcessInstance` (current_step, variables) vs `ProcessDefinition` (Rhai), kernel dispatch loop; scripts return `emits` (side-effect wish list) → recursive orchestration.

### D-US-04 — VS Code plugin
- See CON-US-057 (FileSystemProvider on `ubos:`, tree view, auth, IntelliSense, DAP debugging; keep web editor for quick fixes).

### D-US-05 — Metadata specification v1.0
- See CON-US-014 (URN categories Type/Action/Logic/Widget, meta fields, entity/action/UI/logic protocols, standard return formats, reserved keywords).

### D-US-06 — Three-piece set
- Standard type library with `ui_widget` per type (`ubos://logrums/Type/string` → TextInput …); abstract `TwoStepAction` base carrying transitions; concrete `act_logic_maintain` reduced to params + defaults; renderer lookup order; delivery protocol (Action JSON + Generator + Submitter + widget delta).

### D-US-07 — Rendering (recursion, projection, slicing)
- Recursive `sys_type_to_schema` for inline nested properties; cross-entity references kept as URIs (avoid cycles, size, allow lazy loading).
- Criticism "type definition ≠ form definition" → kernel projection options include/exclude/overrides (hide system fields, scenario-specific forms, single-field actions).
- "Slice by type": root `Entity` type (id, type, payload→JsonEditor) as universal raw view; `sys_load_as(id, type)` returns data + that type's UI; "UI is the skin grown on the type".
- Final "slice": concrete type + id for storage; slice type (ancestor) for view; `sys_slice_project` / `sys_slice_merge` in Rust; actions `act_universal_editor`, `logic_slice_load`, `logic_slice_save`.

### D-US-08…12
- Genesis deltas, action files, meta-type file, console notes and CLI scenarios as summarized in ANA-US-02/04 and CON-US-004.
