---
id: ANA-UB-06
title: "UB Extensibility & AI"
status: complete
phase: P1
depends_on: [ANA-UB-03, ANA-UB-07]
sources: [UB]
---

# UB — Extensibility & AI

## Extension model

| Extension point | Mechanism | Status |
|---|---|---|
| New type / field semantics | Commit type entity with `_extends` (semantic type chain) | implemented (data) |
| New business object | Entity with `entity_type` folder + `_extends` schema | implemented |
| New operation | Action entity (`handler` URI) + Logic entity (Groovy) | implemented |
| New environment | `Context` entity (slug → tenant, branch) | implemented |
| Kernel capability for scripts | extend `UbosScriptContext` | implemented (small) |
| Pluggable services | implement `core.api` SPI | implemented |
| Rules / invariants | BEL constitution | design |
| Apps / packages | `sys.app.manifest`, package manager | design |
| External systems | drivers + mounts | design |
| Pages | Book/Page templates, Smart Document | design |

### CON-UB-060 — Three language layers for three audiences
- **What:** BEL (architect: schema, constitution, assets) + action script "FoxPro style" (business experts: `ACTION`, `GIVEN`, `GUARD`, `TRANSFER`) + Executable Book (documentation staff: text, `VAR`, `BUTTON`).
- **Why (intent):** D-UB-01 — each audience writes in the language of its responsibility; the kernel enforces the constitution under every action.
- **Sources:** [UB:docs/三块拼图.md].
- **Tags:** dsl, rules, document

### CON-UB-061 — BEL: invariants, asset algebra and merge-time flows
- **What:** A non-Turing-complete DSL evaluated at commit/merge time.
- **How:** `INVARIANT` (hard, rejects commit) / `WARN` (soft, tags commit) over `PREV` and `NEW`; `ASSET` with units and precision (fungible vs non-fungible, unit-safe arithmetic); `CONTEXT` (currency, timezone); `ON MERGE FROM <uri>` flows with `PIN ORACLE.ExchangeRate(...)` (freeze external facts into commit metadata); collection algebra (`MAP/FILTER/SUM`); constitution inheritance (`ABSTRACT`, `INHERITS`); `COMMAND` (intent/API, `REQUIRE`) vs `ON COMMIT` (reaction); identity-based guards (`packet.Sender.Role/DID`); `NOW` rewritable by time travel; mapped to a `bpu_constitution_rule_index` of AST rules.
- **Sources:** [UB:docs/BEL Pro.md] (D-UB-18).
- **Tags:** dsl, rules, finance, versioning

### CON-UB-062 — Apps as entity bundles with dependencies (design)
- **What:** `sys.app.manifest` groups screens, workflows, permissions, logic with version and dependencies; `upm install` downloads bundles, resolves dependencies, imports entities, runs post-install; genesis bundles can be pulled from the central **UBOS Store**.
- **Sources:** [UB:docs/UBOS Three Layer System.md], [UB:docs/sys_boot.md], [UB:docs/元数据加载器.md].
- **Tags:** plugin, deployment, domain-pack

### CON-UB-063 — AI as the builder: reflect → generate → validate → commit
- **What:** An architect agent builds applications as entity bundles through the kernel.
- **How:** Reflect on `sys.type.meta` and existing types (script context / `ubos://` injection into prompts); generate a JSON bundle (types, logic, views, seed); import via `sys.action.ops.import` with dry-run validation (types exist, properties conform, Groovy compiles) and actionable rejections; atomic commit; revert if unsatisfied. Improvement loop: RAG over type definitions, fine-tuning on (requirement, entity/logic) pairs, compile/runtime error "mistake notebook", user corrections as positive samples ("data flywheel").
- **Sources:** [UB:docs/ai-dev.md] (D-UB-19).
- **Tags:** ai, codegen, copilot

### CON-UB-064 — AI-native just-in-time internationalization
- **What:** Only the source-language i18n entity is authored; target languages are AI-generated on first request with page context, persisted as entities, cached, invalidated by `source_hash`, and editable.
- **Sources:** [UB:docs/I18n.md] (D-UB-17).
- **Tags:** ai, i18n

### CON-UB-065 — Embeddings stored with versions
- **What:** `embedding vector(1536)` on each version with a cosine index enables semantic search and RAG over business data; design proposes async embedding on `EntityCommittedEvent`; FU config names a local embedding model.
- **Sources:** [UB:backend/src/main/resources/db/migration/V1__init_schema.sql], [UB:docs/design.md].
- **Tags:** ai, search

### CON-UB-066 — LLM deployment strategy and `ctx.ai()`
- **What:** LLMs are external services ("UBOS uses a brain, it does not host one"): public API, private central (Ollama/vLLM), or hybrid by task difficulty; endpoint and key stored in a config entity; scripts call `ctx.ai()`.
- **Sources:** [UB:docs/ai-dev.md].
- **Tags:** ai, integration, scripting

### CON-UB-067 — House-rule prompts for IDE assistants
- **What:** Reusable prompts constrain AI coding assistants to the project's standards (SLF4J only, Groovy Gradle DSL, constructor injection, CAS/micro-commit patterns, metadata-driven resolution).
- **Sources:** [UB:docs/高级提示词.md] (D-UB-20).
- **Tags:** ai, codegen
