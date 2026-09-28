---
id: ANA-UB-07-03
title: "UB Digest 3: UI, Languages (BEL, Smart Document), AI"
status: complete
phase: P1
depends_on: [ANA-UB-07]
sources: [UB]
---

# UB Digest — Part 3: UI, Business Languages, AI

Documents covered: `动态视图渲染器.md` (Dynamic View Renderer, 2,018), `forntend.md` (291),
`IDE.md` (324), `UBOS Smart Document.md` (262), `I18n.md` (185), `BEL Pro.md` (465),
`ai-dev.md` (601), `高级提示词.md` (Advanced Prompts, 238). `EnvContext.md` is empty.

---

## D-UB-13 `动态视图渲染器.md` — Server-driven UI end to end (2,018 lines)

- **Architecture: SDUI.** Backend returns a UI schema, not HTML ("like OFBiz screen/form widgets, in JSON + reactive").
- **Three render phases**: Resolution (load layout metadata by view id + version from the version chain) → Hydration (inject record values, option lists) → Transformation (ACL trims fields/actions; device/role variants) → output **`ViewSchema`**.
- Polymorphic component model + **renderer strategy per component type** (handler chain, reactive recursive rendering of children); field-level permission handler marks hidden/removes.
- **Protocol payload**: `schema` (recursive `children`) separated from `data` (current values); `meta` includes the commit id for debugging.
- Frontend: one **Universal Renderer** (recursive component registry).
- **Layouts are versioned entities** (`VIEW_HR_ONBOARDING` on branches `main`/`dev`/`ab-test-v1`): UI **A/B testing by branch**, **instant UI rollback**, **visual layout diff**.
- **Action protocol**: events on components hold an **action chain** (API call, navigate, set state, open drawer, evaluate expression, …) with runtime variables (`@formData`, `${entityType}`); frontend `ActionDispatcher` with a handler registry (strategy pattern) executes chains serially; permission-aware (server omits or disables unauthorized actions).
- **State management**: Redux Toolkit `SmartForm` slice per view (values, dirty flags, sync status, base commit id); save modes MANUAL vs **REALTIME** ("Google Docs style") via listener middleware with debounce; only **pending changes (deltas)** sent; backend may **squash** micro-commits. Zustand discussed as lighter alternative; RTK preferred for middleware orchestration, time-travel debugging, dynamic normalized state.
- **Dynamic binding**: `useDynamicBinding(path)` reads/writes arbitrary paths (`order.lines[2].qty`); **scope/base path** context for grids and arrays.
- **Conflicts**: stale base commit ⇒ optimistic-lock failure ⇒ middleware handles rebase/notify.
- **Backend "Patch & Commit"**: receive `{baseCommitId, changes:[{path, value}]}` → load head → check base (optimistic lock) → apply patch (custom Jackson patcher that auto-creates intermediate nodes and array slots) → store **full snapshot + the delta** ("snapshot + delta hybrid").

## D-UB-14 `forntend.md` — UBOS Studio / Workbench (291 lines)

- Not an admin panel: a **"Low-Code Data IDE" = headless CMS + Git GUI**, named UBOS Studio / Workbench.
- Three tensions to solve: complexity vs usability (JSON/Git behind forms/timelines), dynamism vs standards (schema-driven UI), **inheritance visibility** (default from parent vs own override).
- Four panes: **Omni-bar** (Spotlight-like: `sys.user/admin`, natural-language search → `SearchCriteria`, `ubos://acme/order/1001@draft`) · **resource & lineage navigator** (file tree + inheritance tree) · **smart canvas** (form mode with inherited 🔗 vs override ✏️ markers and *Reset to parent*; JSON mode with Monaco) · **time & branch console** (`@main` read-only vs `@draft`, history timeline, diff, Save Draft / Publish / Discard).
- UX details: **X-ray** tooltip showing value per ancestor level; **pending changes tray** sent as one batch `saveDrafts`; **time travel** slider + "revert to here" creating a revert commit.
- Stack: React; Zustand or RTK; AntD Pro vs **Mantine** (recommended for IDE-like layout, custom inheritance inputs) + **Formily** (schema-driven forms) + Monaco; Vite.

## D-UB-15 `IDE.md` — Meta-IDE, the cockpit (324 lines)

- **Meta-Shell**: the IDE is defined by UBOS entities (`sys.ui.menu`, `sys.ui.page`, `sys.ui.action_button` → `sys.act.*`) so the IDE can modify itself (self-hosting); the IDE itself is entity `sys.app.studio`.
- Quadrants: Universe Explorer (physical tenant/type/entity view; logical view; **virtual folders** as entities) · Polymorphic Canvas (structure designer for meta entities; **Monaco with Groovy + `ctx.` autocompletion**; runtime dashboard; relation topology with React Flow/G6) · Holographic Inspector (time slider, "who references me / whom I reference", action buttons) · Signal Stream (script `ctx.log` console, live event bus).
- Killer features: **Logic simulator / dry run** (`dryRun=true`: execute without committing, return logs and result), **graph walker** (impact of changing `sys.entity.root`), **action terminal**, **hot patching** (edit logic entity → next request uses it, audited).
- Roadmap: data plane (done) → logic plane (Monaco, dry run) → relation plane (graphs) → meta plane (menus/routes as entities).
- Manifesto ("paradigm shift"): death of deployment; bootstrapping (use UBOS to build UBOS); data is application (export/import = app migration); users may change even the logic and the IDE, bounded by security entities; five shifts — mindset (define DNA), method (logic is data), tools (IDE inside the system), platform (business-aware OS), process (edit → save → live).

## D-UB-16 `UBOS Smart Document.md` — Executable document architecture (262 lines)

| Layer | Name | Technology | Role |
|---|---|---|---|
| 1 | Skin — business syntax | Markdown with **block directives** `:::` and inline magic `{{…}}` (FoxPro-style) | business experts, teachers, finance write pages like fill-in-the-blank |
| 2 | Compiler | unified / remark / remark-directive → **AST** (never regex replacement) | robust nesting, code-block isolation |
| 3 | Soul | **Rust → WebAssembly page engine** (state machine) | single source of truth for page state and validation; tamper-resistant logic |
| 4 | Glue | React renderer; components register with the Wasm engine | React displays, Wasm decides |
Data flow: input `-5` → `engine.on_input_change` → Rust sets `has_error` → React re-reads view model → `::: if` block shows "answer cannot be negative".

## D-UB-17 `I18n.md` — AI-native just-in-time translation (185 lines)

- Maintain only the default-language entity (e.g. `ubos://sys_boot/i18n.app.dashboard`); target-language entities (`…ja_JP`) are **generated by AI on first request, persisted and cached**.
- Translate **whole pages with context** (form structure disambiguates "Open"); admins can correct generated entities or request re-translation; any language without code change.
- `source_hash` detects outdated translations; stream/fallback while generating; strict caching (Caffeine + DB) for cost control; profile lists `supportedLanguages` for background pre-generation.

## D-UB-18 `BEL Pro.md` — Business Engineering Language (465 lines)

### BEL 1.0 — a non-Turing-complete DSL "defining how the kernel processes a commit"
| Group | Keywords |
|---|---|
| Schema | `ENTITY`, `ASSET` (conserved resource), `CONTEXT` (boundary: branch office, currency, timezone), `FIELD`, `REF`, `ENUM` |
| Constitution (C-layer) | `CONSTITUTION`, `INVARIANT` (violation rejects commit), `WARN` (soft, tag commit), `PREV` (state before), `NEW` (candidate state) |
| Flow (F-layer) | `FLOW`, `ON MERGE` (core trigger when data merges), `TRANSITION`, `EMIT` (bus message across contexts), `PIN` (freeze an external value, e.g. exchange rate, into commit metadata), `REJECT` |
| System | `ORACLE` (external data), `ME`, `NOW` (logical time, rewritable by time travel), `DIFF` |
Files `.schema.bel` + `.logic.bel`. Examples: `INVARIANT NonNegativeAmount { NEW.total_amount >= 0 }`; `NoResurrection` (Void stays Void); `ON MERGE FROM "ubos://US_Branch"` pins USD→JPY rate and converts. Properties: implicit transaction (any violation discards `NEW`), **asset algebra** (units must match: USD + JPY is a compile error). Mapping: `CONSTITUTION` → `bpu_constitution_rule_index` AST rules; `FLOW` → commit hooks; `PREV/NEW` → version chain.

### BEL 2.0 — "commercial physics"
1. **Fungible vs non-fungible assets** (`ASSET FUNGIBLE CNY PRECISION 2`; `ASSET NON_FUNGIBLE RealEstate {IDENTIFIER: UUID}` — only `TRANSFER`, no arithmetic).
2. **Collection algebra** in invariants (`NEW.items.MAP(i => i.price * i.qty).SUM() == NEW.total`, `FILTER`, `SUM`) instead of loops.
3. **Constitution inheritance** (`ABSTRACT` global law; `INHERITS`, local additions/overrides: Japan consumption tax).
4. **Intent vs reaction**: `COMMAND Pay(amount)` (API: `REQUIRE`, produce pending `COMMIT {…}`) vs `ON COMMIT` handlers; UI derives buttons from `COMMAND`s.
5. **Zero-trust identity guard**: `ON MERGE (packet) { CASE packet.Sender.Role == "Supplier" … CASE packet.Sender.DID == "did:ubos:…" }`.
Relation to D-UB-01: BEL 2.0 = piece 1 (schema/constitution/asset); "FoxPro script" (`ACTION/GIVEN/GUARD/TRANSFER`) = piece 2.

## D-UB-19 `ai-dev.md` — AI as the builder (601 lines)

- Vision: "AI generates a bundle of JSON → commit → the website exists": AI emits entity bundles (types, logic, views, seed data) and imports them via `sys.action.ops.import`; no build/deploy; changes are **patches** to single entities; logic is data; AI gets a **perfect, token-cheap self-description** via export; rollback by revert.
- Making AI follow the development model: (1) **reflection** — read `sys.type.meta` and existing types via `ctx.findOne`/`ubos://`; (2) **schema-guided generation** — the architect agent's prompt embeds the composed meta-schema via `{{ ubos://sys_boot/sys.type.meta }}`; (3) **strict landing** — import runs dry-run validation (types exist, properties conform, **Groovy compiles**), rejects with actionable errors, commits atomically.
- Training a dedicated model: corpus = `sys.type.*` (grammar), `sys.logic.*` (API style), `sys.action.*` ↔ logic (behavior pairs); stage 1 prompt + **RAG over type definitions**; stage 2 fine-tune open models (Llama 3, CodeQwen); data augmentation (generate requirement text for existing logic); **error loop** (compile/validation errors + fix = "mistake notebook"); runtime loop (process logs, user edits as positive samples) → "data flywheel: more entities, smarter AI".
- Deployment of LLMs: public API (recommended to start; e.g. DeepSeek/OpenAI), private central (Ollama/vLLM with Llama/Qwen), or **hybrid** (small local model for chores, large cloud model for architecture); integrate via `ctx.ai()` in the script context; endpoint and key stored in a `sys.config` entity; "UBOS uses a brain, it does not host one".

## D-UB-20 `高级提示词.md` — Prompts for IDE AI (238 lines)

- Reusable prompt templates to make IDE assistants (Cursor, Copilot, IntelliJ AI) generate production code in the house style: role "senior architect"; constraints (**no `System.out`, SLF4J only**; Gradle Groovy DSL, not Kotlin DSL; constructor injection); context slots.
- **Micro-commit** prompt: Git-like content-addressable storage (hash → blob → parent link → head CAS), deduplication of unchanged content, optimistic locking, DEBUG size/latency logs.
- **Metadata-driven resolver** prompt: resolution strategy decided by the field's declared type — `ENTITY_REFERENCE` values resolved as pointers (no `{{}}`), `TEXT` values interpolated; protocol parameters express presentation (`?render=image`); `_extends` handled because its field type is a reference, not by name.
