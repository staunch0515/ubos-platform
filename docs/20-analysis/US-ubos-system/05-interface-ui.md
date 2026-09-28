---
id: ANA-US-05
title: "US Interfaces & UI"
status: complete
phase: P1
depends_on: [ANA-US-03, ANA-UC-05]
sources: [US]
---

# US — Interfaces & UI

UC concepts retained: minimal verb API (CON-UC-050), reference hydration (051), bicameral UI
(052), menu-less cockpit (053), schema-to-UI executor (054), cards (056), resource browser
(057), async UX (059).

## HTTP API additions

| Method | Path | Purpose |
|---|---|---|
| GET | `/action/:slug/init` | Action definition (`params_schema`, defaults) from `logrums/Action/<slug>` |
| POST | `/action/prepare` | run generator logic synchronously (scope `params`) |
| POST | `/action/submit` | run submitter logic synchronously (scope `payload`, `context_params`) |
| POST | `/action/transition` | FSM step (CON-US-022) |
| POST | `/check_syntax` | compile-only Rhai check (CON-US-005) |
| POST | `/eval` | as UC; `context_job_id = "genesis"` resolved to the genesis Workspace |

[US:apps/ubos_server/src/api/routes.rs#L10-L47]

### CON-US-050 — Client-side kernel layer
- **What:** The web app talks to the server only through a `ClientKernel` singleton (`fetchActionDefinition`, `executeActionTransition`, `fetchViewDefinition`) over `UbosRpc`, which injects `X-User-ID`, `X-Tenant-ID`, `X-Context-ID`, `X-Branch` and body `tenant_id`, `context_workspace_id`, `user_id`, and centralizes errors; `ProtocolResolver` walks a schema to collect `ubos://` widget dependencies before rendering; protocol types `KernelContext`, `ActionRequest{options: dryRun, async, timeout}`, `ActionResponse{status, result, job_id, logs, meta.duration_ms}`, `EntityRef{scheme, type, id, version}` [US:apps/ubos_web/src/kernel/].
- **Why (intent):** A client mirror of the server kernel: one place for protocol, context and transport (future WebSocket/UBTP).
- **Tags:** frontend, protocol, context

### CON-US-051 — Widgets as addressable resources
- **What:** `WidgetRegistry` keyed by full URI (`ubos://logrums/Widget/TextInput`) or short name; lookup falls back from URI to last path segment. Core widgets: TextInput, TextArea, Select, Switch, JsonEditor; business: RhaiEditor [US:apps/ubos_web/src/kernel/registry/WidgetRegistry.ts], [US:apps/ubos_web/src/widgets/].
- **Why (intent):** `Widget` is a metadata category (CON-US-014); schemas reference widgets by URI, enabling future dynamic widget loading.
- **Tags:** ui-rendering, protocol, plugin

### CON-US-052 — Universal renderer: mode-aware widget resolution
- **What:** For `(schema, value, mode ∈ edit|view|list|mask)`: widget = `schema.ui_modes[mode].widget` → `schema.ui_widget` → type fallback (`map` → JsonEditor, `string` → TextInput); config = `ui_props` < `ui_options` < mode config; read-only in `view`/`list` or when `readOnly`; script widgets convert line arrays ↔ text [US:apps/ubos_web/src/engine/UniversalRenderer.tsx].
- **Why (intent):** Front half of the lens system (CON-UC-055) driven by type `ui_modes` (CON-US-015).
- **Tags:** ui-rendering, type-system

### CON-US-053 — Recursive schema renderer
- **What:** Object schemas render their `properties` recursively (nested objects as cards, labels with required markers, `Hidden` skipped); leaves render through the registry [US:apps/ubos_web/src/engine/SchemaRenderer.tsx].
- **Tags:** ui-rendering, form

### CON-US-054 — View engine for View entities
- **What:** `ViewEngine(viewSlug)` loads a View definition and switches on `layout_type`: `DASHBOARD` (grid of registry widgets from `config_json.widgets`), `LIST_VIEW` (entity type, columns, actions — grid planned with `list` mode cells), `FORM_VIEW` (dynamic form) [US:apps/ubos_web/src/engine/ViewEngine.tsx]; views come from genesis `15_ui_config`.
- **Divergence:** calls `/view/{slug}/init`, not served by the current server.
- **Tags:** ui-rendering, metadata

### CON-US-055 — Action automator: FSM-driven wizard UI
- **What:** Selecting an Action in the omnibar ("Actions / Workflows" group) opens `ActionAutomator`: header shows name and current state; body renders `uiSchema` with `formData`; footer shows one button per `available_events`; each click calls `/action/transition` and replaces schema/data from the response; `DONE` closes; Esc closes [US:apps/ubos_web/src/components/Action/ActionAutomator.tsx], [US:apps/ubos_web/src/components/Action/hooks/useActionFsm.ts]. Earlier staged runner: `LOADING → PARAMS → PREPARING (generator) → EDITOR (JSON review) → SUBMITTING → DONE` [US:apps/ubos_web/src/components/Action/ActionRunner.tsx], [US:apps/ubos_web/src/store/useActiveStore.ts].
- **Why (intent):** Zero bespoke screens for workflows: buttons and forms are functions of server-side action metadata.
- **Tags:** frontend, workflow, ui-rendering

### CON-US-056 — Rhai authoring support
- **What:** Monaco language definition for Rhai with completion providers (syntax + UBOS syscalls), server-side diagnostics (CON-US-005), `ScriptEditor` widget in Logic `ui_modes` [US:apps/ubos_web/src/lib/monaco/], [US:apps/ubos_web/src/components/Editor/RhaiEditor.tsx].
- **Tags:** frontend, scripting

### CON-US-057 — VS Code extension over a `ubos://` virtual file system (design)
- **What:** A VS Code extension implementing `FileSystemProvider` for the `ubos:` scheme: tree view Tenant → Logics (`.rhai`) / Actions (UI/flow); reads/writes go to the UBOS HTTP API (Ctrl+S = commit); global search over server code; reuse of existing Rhai extensions; injected typings for `ctx.`; connect command with token in secret storage; V2 remote debugging via Debug Adapter Protocol. Web editor kept for quick view, hot-fix and demos.
- **Why (intent):** "Developer native": browser IDEs lack shortcuts, screen space and the local tool chain (Git, diff, Copilot).
- **Sources:** [US:docs/front/VsCode.md].
- **Tags:** frontend, integration, protocol

### CON-US-058 — Type-driven widget lookup order
- **What:** Instance `ui_widget` (unless `Hidden`) → type's widget via `ubos://…/Type/<t>` definition → base fallback; documented as the "final logic" of the schema renderer.
- **Sources:** [US:docs/front/三件套.md].
- **Tags:** ui-rendering, type-system
