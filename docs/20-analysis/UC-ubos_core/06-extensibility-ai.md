---
id: ANA-UC-06
title: "UC Extensibility & AI"
status: complete
phase: P1
depends_on: [ANA-UC-03, ANA-UC-07]
sources: [UC]
---

# UC — Extensibility & AI

## Extension model

| Extension point | Mechanism | Status |
|---|---|---|
| New business type | Any `entity_type` string + `Type` entity with `_extends` and UODS properties | implemented (data) / UODS validation design |
| New operation | `Logic` entity (Rhai) via `/deploy` or `save_entity_raw` | implemented |
| Reusable library | `import` of Logic entities as Rhai modules; `sys.std.*` | implemented (resolver) / stdlib design |
| Kernel capability | new syscall registered in `syscalls/*` | implemented (code change) |
| Schedules / reactions | `Cron` / `Hook` entities | implemented |
| Authorization | `Policy` entity script | implemented (not wired to syscalls) |
| Environment | `Workspace` entity + Ambassador handshake | implemented |
| UI | `ui_schema.widget` in Logic; lens UI matrix entity | partial / design |
| Genesis | JSON files in `docs/db/genesis_data/` | implemented |

### CON-UC-060 — LLM as a syscall
- **What:** `sys_ai_chat(model, prompt) -> String` against any OpenAI-compatible endpoint (`OPENAI_API_KEY`, `OPENAI_API_BASE`, e.g. DeepSeek).
- **How:** [UC:src/kernel/syscalls/ai.rs#L5-L22]; errors are returned as strings (`Error: No Key` …).
- **Why (intent):** D-UC-17 — the system "grows a brain"; combined with logs and schemas: AI explains errors, generates test data from schemas; "UBOS uses a brain, it does not host one" (same position as CON-UB-066).
- **Tags:** ai, integration, scripting

### CON-UC-061 — Executable Book as the product vision
- **What:** A "book" whose chapters contain text + logic + data + UI and runs on UBOS; four levels: (1) **Living book** — remembers each reader (state per tenant/user, earlier answers reused later); (2) **Hybrid intelligence** — execution as skeleton, RAG as flesh (`sys_ai_explain(error, context)`); (3) **Self-updating book** — Cron/Hook keep data fresh and notify readers; (4) **Book as app generator** — reading choices configure and deploy Logic and workflows ("finish the reimbursement-policy book, get a reimbursement system").
- **Why (intent):** D-UC-55 — "the knowledge base is the brain (read-only); the executable book is the hand (read-write-execute)". Same concept family as UB's Executable Book / Smart Document (CON-UB-055).
- **Sources:** [UC:docs/book.md], [UC:docs/plan/Plan_05.md], [UC:docs/plan/Plan_06.md].
- **Tags:** document, ai, dsl

### CON-UC-062 — Stateful agent runtime positioning
- **What:** Workspaces with memory + audit + policy make UBOS an "AI Agent native OS": a persistent digital identity and workspace per agent (log into an ERP once, keep the session, act like an employee on continuous tasks, everything audited).
- **Why (intent):** D-UC-39. The documents also include valuation estimates; recorded as vision, not requirements.
- **Sources:** [UC:docs/plan/Plan_10.md].
- **Tags:** ai, context

### CON-UC-063 — Standard library and killer demo plan
- **What:** Build `sys.std.http`, `sys.std.time`, `sys.std.ai` (prompt templates) as importable scripts; demo "personal investment weekly": HTTP fetch → store `SysData` → AI analysis → file report → Cron weekly.
- **Sources:** [UC:docs/plan/Plan_06.md].
- **Tags:** scripting, domain-pack
- **Status:** design.

### CON-UC-064 — AI-native kernel roadmap
- **What:** `sys_discovery` (semantic search of available functions for agents), AI-generated scripts deployed with automatic syntax checks, long-term memory via vector search `sys_memory_vector_search` (the `embedding` column exists in the schema).
- **Sources:** [UC:docs/plan/Plan_14.md].
- **Tags:** ai, search, codegen
- **Status:** design.

### CON-UC-065 — Prompt-driven development method
- **What:** The system is built by feeding an AI a sequence of stage prompts (goal, constraints, files to change, verification command); a **handoff document** carries architecture state across chat sessions ("do not roll back runner.rs type handling …"); `prompt.md` fixes house rules (6 tables, `logrums`, explicit `_extends`, files ≤ 100 lines, no Chinese in code, complete files, preserve existing logic).
- **Why (intent):** Continuity and consistency of AI-written code; the documents themselves are reusable prompts (relevant to this documentation project, which is AI-targeted).
- **Sources:** [UC:docs/plan/Plan_01.md], [UC:docs/plan/Plan_14.md], [UC:docs/prompt.md].
- **Tags:** ai, codegen, process

### CON-UC-066 — URI as the universal argument
- **What:** Every API parameter, Cron/Hook target, import path, UI resource parameter and lineage record can be a `ubos://` address; remapping a namespace switches a whole logic set to a shadow/test environment without code changes.
- **Sources:** [UC:docs/plan/plan_18_ubos.md].
- **Tags:** protocol, integration, versioning
- **Status:** partly implemented (reads, `_extends`, browser).

### CON-UC-067 — "One core, five clients" stated platform target
- **What:** `prompt.md` states the full target architecture: one kernel, five client ends, cross-platform, own binary protocol, zero-copy, high-performance vector processing. None of it is implemented in UC (HTTP/JSON only); the LS/US repositories should be checked for the binary protocol and multi-client parts (P2 topic 15).
- **Sources:** [UC:docs/prompt.md].
- **Tags:** protocol, deployment
