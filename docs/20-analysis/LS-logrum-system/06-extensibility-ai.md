---
id: ANA-LS-06
title: "LS Extensibility & AI"
status: complete
phase: P1
depends_on: [ANA-LS-02, ANA-LS-03]
sources: [LS]
---

# LS — Extensibility & AI

## Extension model

| Extension point | Mechanism | Status |
|---|---|---|
| New kind of behaviour | `BehaviorType` entity (slot with I/O schema) | data |
| New implementation | `Behavior` entity implementing a slot (script / native / component), refine chain | data / refine design |
| New property semantics | `MetaRelationType` with `behaviorConfig` | data |
| New business type | `MetaType` listing relation types (+ extends) | data |
| New workflow | `MetaProcessType` logic + instances | implemented (process step) |
| New interaction | `ActionType` entity with 3 stage processes and parameter relations | data (stage runner via Emit) |
| Kernel law | new `KernelMechanism` in the pipeline | design |
| Storage | new backend implementing store op traits | implemented (SQLite, Postgres) |

### CON-LS-060 — Everything extensible is an entity in four layers
- **What:** Slot (BehaviorType) → implementation (Behavior) → configuration (RelationType) → composition (MetaType); processes and actions are entities built on the same model.
- **Why (intent):** Extending the system never requires kernel code, only committing entities.
- **Tags:** plugin, meta-model

### CON-LS-061 — `native::` host function namespace (design in data)
- **What:** Behaviour scripts reference host functions: `native::store_text/store_json/store_ref(ctx)`, `native::hash_sha256`, `native::now`, `native::emit_signal`, `native::load_entity`, `native::load_type`, `native::exec_behavior(rel, slot, value)`; code exposes `ctx.emit`, `ctx.user_id`, `ctx.process_id`.
- **Sources:** [LS:genesis_data/03_base_behavior.json], [LS:genesis_data/06_action.json].
- **Tags:** scripting, api

### CON-LS-062 — Editions by build feature
- **What:** `personal` (SQLite, default) vs `enterprise` (PostgreSQL) compiled from one code base (CON-LS-006).
- **Tags:** deployment, persistence

### CON-LS-063 — Prompt contract for AI-driven refactoring
- **What:** `claude.md` is a context file for an AI coding agent: date, task, verbatim rules, core logic and the three-stage design; commit messages record AI explanations of milestones (e.g. "client sends, server routes, kernel resolves the full relation URI").
- **Why (intent):** Same AI-first development practice as UC/US (CON-UC-065), now with a stable, repository-level instruction file.
- **Sources:** [LS:claude.md], [LS:.claude/settings.local.json].
- **Tags:** ai, codegen, process

### CON-LS-064 — Instrumented kernel
- **What:** Nearly every function carries `#[tracing::instrument]` with structured fields (uri, commit_id, visited depth…); server logs file/line/thread; default filter `debug`.
- **Why (intent):** Explainability of semantic assembly and dispatch during development.
- **Tags:** observability
