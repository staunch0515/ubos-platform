---
id: ANA-US-06
title: "US Extensibility & AI"
status: complete
phase: P1
depends_on: [ANA-US-03, ANA-UC-06]
sources: [US]
---

# US — Extensibility & AI

## Extension model (additions to ANA-UC-06)

| Extension point | Mechanism | Status |
|---|---|---|
| New interaction / workflow | `Action` entity extending `TwoStepAction`/`StatefulAction` + generator/submitter Logic | implemented |
| New widget | register component under `ubos://logrums/Widget/<name>`; reference from `ui_widget`/`ui_modes` | implemented (client) |
| Rendering per mode | `ui_modes` on types | implemented (data + renderer) |
| Behaviour slices | relation types with scripts, voting, blackboard | design |
| Cross-tenant call | `sys_invoke(uri, args)` | stub |
| Client | new app in workspace using `ubos_kernel` (CLI done; desktop, SDK planned) | partial |
| IDE | VS Code extension | design |

### CON-US-060 — Neuro-symbolic integration: LLM + UBOS (design)
- **What:** UBOS is the deterministic "left brain" (rules, causality, memory, audit); the LLM is the probabilistic "right brain" (language, generation). Four integration levels: (1) **LLM as compiler** — natural language → UBOS ChangeSet JSON, executed transactionally with all relation checks; (2) **UBOS as hallucination firewall** — agent proposals pass relation-type validation with veto (e.g. scaling below `min_instances` rejected); (3) **logic-as-data generator** — LLM writes Rhai/WASM into relation entities as new commits ("self-programming"); (4) **holographic context for explainability** — graph + slice traces give the LLM an exact "autopsy report" to explain outcomes.
- **Why (intent):** An "Agentic OS": LLM supplies understanding, UBOS supplies the deterministic anchor enterprises need.
- **Sources:** [US:docs/front/FullSlice.md].
- **Tags:** ai, rules, codegen, observability

### CON-US-061 — Explainable decisions from structured traces
- **What:** The same trace data that debugs slices (CON-US-029) is the input for AI explanations ("why was Zhang San's salary wrong?").
- **Tags:** ai, observability

### CON-US-062 — Hot-swappable business rules via versioned relation entities (design)
- **What:** Change or roll back a rule by committing a relation-type version; remove a promotion by removing a relation from `_extends`; no restart.
- **Sources:** [US:docs/front/FullSlice.md].
- **Tags:** versioning, rules, plugin

### CON-US-063 — AI-assisted delivery protocol: the "three-piece set"
- **What:** For each feature request the AI returns Action JSON + Generator logic + Submitter logic (+ incremental widget code if needed), instead of scattered patches.
- **Why (intent):** Predictable, complete increments that fit the metadata model; reduces "toothpaste squeezing".
- **Sources:** [US:docs/front/三件套.md].
- **Tags:** ai, codegen, process

### CON-US-064 — One core, many clients
- **What:** Kernel as a library crate (`ubos_kernel`) consumed by server, CLI and (planned) Tauri desktop and client SDK; web talks over HTTP; target binary protocol UBTP (CON-US-006).
- **Sources:** [US:Cargo.toml], [US:README.md].
- **Tags:** deployment, protocol

### CON-US-065 — Development tooling as part of the platform
- **What:** Scenario CLI, syntax endpoint, Monaco Rhai language, source-bundling commands for AI context (`all_src_merged.txt`, `all_code.txt` in `console.md`), integration test harness against Docker Postgres/Redis.
- **Sources:** [US:console.md], [US:crates/ubos_kernel/tests/kernel_integration_test.rs].
- **Tags:** ai, codegen, observability
