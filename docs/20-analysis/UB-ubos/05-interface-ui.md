---
id: ANA-UB-05
title: "UB Interfaces & UI"
status: complete
phase: P1
depends_on: [ANA-UB-01, ANA-UB-07]
sources: [UB]
---

# UB — Interfaces & UI

UB's frontend folder is a Vite placeholder; the UI design lives in the documents
(D-UB-13 … D-UB-16) and in a server-rendered page controller.

### CON-UB-050 — Server-driven UI with a render pipeline and versioned layouts
- **What:** The backend produces a `ViewSchema` from a layout entity; a generic frontend renders it.
- **How:** Pipeline Resolution (layout by view id + version from the version chain) → Hydration (values, option lists) → Transformation (ACL trims fields/actions; device/role variants). Payload separates `schema` (recursive `children`), `data` (values) and `meta` (commit id). Renderer strategy per component type; reactive recursive rendering. **Layouts are versioned entities**: UI A/B testing by branch (`feature/new-dashboard`), instant rollback, layout diff.
- **Sources:** [UB:docs/动态视图渲染器.md] (D-UB-13).
- **Tags:** ui-rendering, view, versioning

### CON-UB-051 — Action protocol with chained actions
- **What:** Component events carry action chains (API call, navigate, set state, open drawer, evaluate expression …) with runtime variables (`@formData`, `${entityType}`), executed serially by a frontend dispatcher with a handler registry; unauthorized actions omitted or disabled server-side.
- **Sources:** [UB:docs/动态视图渲染器.md].
- **Tags:** ui-rendering, workflow, permission

### CON-UB-052 — Smart form state: realtime deltas, binding paths, conflicts
- **What:** Per-view form state (values, dirty flags, sync status, base commit id); save modes MANUAL / REALTIME (debounced autosave of deltas, "Google Docs style"); dynamic binding by path with scopes for grids/arrays; optimistic-lock conflicts surfaced and handled.
- **Sources:** [UB:docs/动态视图渲染器.md].
- **Tags:** frontend, form, versioning

### CON-UB-053 — Inheritance-aware Studio / Workbench
- **What:** A "low-code data IDE = headless CMS + Git GUI".
- **How:** Omni-bar (address by slug/URI incl. `@draft`, natural-language search); resource tree + **inheritance (lineage) tree**; smart canvas with form mode showing **inherited (🔗) vs overridden (✏️) values and "reset to parent"**, and JSON mode (Monaco); **X-ray** hover listing each ancestor's value; pending-changes tray committed as one batch; time/branch console (`@main` read-only, `@draft` editable, history, diff, Save Draft / Publish / Discard, time-travel with "revert to here").
- **Sources:** [UB:docs/forntend.md] (D-UB-14).
- **Tags:** frontend, meta-model, versioning

### CON-UB-054 — Self-hosting Meta-IDE
- **What:** The IDE is itself defined by entities (`sys.ui.menu`, `sys.ui.page`, `sys.ui.action_button`, `sys.app.studio`) and can modify itself.
- **How:** Explorer (physical/logical views, virtual folders as entities), polymorphic canvas (structure designer, Monaco with `ctx.` completion, runtime dashboard, relation topology), holographic inspector (time slider, references in/out, action buttons), signal stream (script logs, event bus). Features: **dry-run** execution without commit, graph walker (impact analysis), action terminal, hot patching of logic.
- **Sources:** [UB:docs/IDE.md] (D-UB-15).
- **Tags:** frontend, scripting, metadata

### CON-UB-055 — Executable Book / Smart Document
- **What:** Business pages written in Markdown with block directives (`:::`) and inline magic (`{{VAR …}}`, `{{BUTTON ACTION "…"}}`), compiled to an AST, with page state and validation in a Rust→WebAssembly engine and React for display.
- **Why (intent):** D-UB-01, D-UB-16 — the third "puzzle piece": the employee's desk is an interactive document bound to data and actions.
- **Sources:** [UB:docs/UBOS Smart Document.md], [UB:docs/三块拼图.md].
- **Tags:** document, ui-rendering, dsl

### CON-UB-056 — Server-rendered entity pages chosen by metadata
- **What:** `GET /{contextSlug}/{type}/{slug}.view` resolves the context, evaluates the entity and picks an HTML template via a view-strategy based on entity metadata (404 if not found).
- **Sources:** [UB:backend/src/main/java/com/ubos/web/controller/PageController.java].
- **Tags:** ui-rendering, api

### CON-UB-057 — Types drive widgets, formatting and masking
- **What:** Semantic type definitions carry `ui_widget` (e.g. `input-email`, `input-currency`, `textarea`, `editor-markdown`), display rules (`multiply_display`, currency decimals, unit conversion) and `masking_rule` applied server-side by permission.
- **Sources:** [UB:docs/sys_boot.md] (D-UB-09 phases 2–4, 11).
- **Tags:** type-system, ui-rendering, permission

## Technology choices discussed
React; Redux Toolkit (listener middleware, devtools) preferred over Zustand for the SDUI
state engine; Mantine + Formily recommended over Ant Design Pro for an IDE-like tool; Monaco;
React Flow / AntV X6 for graphs; Vite.
