---
id: ANA-UC-05
title: "UC Interfaces & UI"
status: complete
phase: P1
depends_on: [ANA-UC-03, ANA-UC-07]
sources: [UC]
---

# UC — Interfaces & UI

## HTTP API (axum, port 8100)

| Method | Path | Handler | Notes |
|---|---|---|---|
| POST | `/deploy` | `handle_deploy` | `{slug, name, inputs, script}` → Logic entity in `logrums` |
| POST | `/invoke` | `handle_invoke_async` | `{context_job_id, func_slug, args, user_id}` → 202 `{job_id}` |
| POST | `/eval` | `handle_eval` | `{script, user_id, context_job_id}` → `{status, result}` |
| GET | `/workspace/{id}` | `handle_get_logrum` | Job by id (naming legacy) |
| GET | `/workspace/{id}/status` | `handle_get_logrum_status` | `{id, status, result, logs, updated_at}` |
| GET | `/workspace/list` | `handle_list_workspaces` | all Workspace entities |
| POST | `/entity/{id}` | `get_entity` | `?hydrate=true` defaults, `?resolve_refs=true` expansion |
| POST | `/entity/search` | `handle_search_entity` | `{tenant_id, entity_type, query}` → `{items:[{id,payload}]}` |
| GET | `/logic/{slug}` | `handle_get_logic` | Logic definition (for forms) |
| POST | `/ambassador/connect` | `handle_ambassador_connect` | handshake (CON-UC-023) |

[UC:src/api/routes.rs#L13-L34]

### CON-UC-050 — Minimal verb set: deploy / invoke / eval / connect / search / get
- **What:** The API surface is a handful of generic verbs; business operations are Logic invoked by slug.
- **Why (intent):** The kernel stays generic; new capabilities are data. Front ends discover functions through search, not hard-coded routes.
- **Sources:** as above.
- **Tags:** api, protocol

### CON-UC-051 — Reference hydration by naming convention
- **What:** On read, any string field ending in `_id` whose value is a `ubos://` URI or a UUID is replaced by the loaded entity (key without `_id`), recursively; `hydrate=true` fills missing fields from type defaults.
- **How:** [UC:src/api/handlers/entity.rs#L20-L105].
- **Why (intent):** Clients receive a ready object graph; references are stored as links, never copies (D-UC-47 "field-level references").
- **Tags:** rehydration, api, protocol

### CON-UC-052 — Bicameral UI: Active zone and Passive zone
- **What:** The screen is split at the golden ratio: the **Active zone** (left; user intent — omnibar, forms, editors, stage) and the **Passive zone** (right; system push — notifications, cards, approvals, forms pushed "to your face").
- **How:** Design D-UC-36/41/52; UI components `ActiveZone/Stage`, `ActiveZone/DesktopEnvironment` (windowed apps), `Passive/FeedStream` (cards: json/markdown/image, queue stats; currently fed by a mock service) [UC:ui/components/].
- **Why (intent):** Separate intent-driven from event-driven interaction; the system proactively brings the work window to the user.
- **Tags:** frontend, ui-rendering, events

### CON-UC-053 — Menu-less, command-centric cockpit with strict context hierarchy
- **What:** No menus; everything via a search/command bar. Order: **User** (who am I) → **Workspace/Logrum** (where; fixes tenant and branch) → **Command** (what) → **Stage** (how).
- **How:** Login → list Workspaces for the identity → slide-out drawer to choose → omnibar searches `Logic` (commands) and `Workspace` (context switch) → selected command renders in the Active zone; switching Workspace resets the Active zone "like rebooting" to avoid deploying tenant A's code into tenant B [UC:docs/front/front_02.md], [UC:ui/store/useContextStore.ts]. An axios interceptor injects `X-User-ID`, `X-Tenant-ID`, `X-Context-ID`, `X-Branch` and body `tenant_id`/`user_id` [UC:ui/lib/api.ts].
- **Why (intent):** Linear/Superhuman-style efficiency; context isolation as a UX rule.
- **Tags:** frontend, context, multi-tenancy

### CON-UC-054 — Schema-to-UI: function definition drives the form
- **What:** Selecting a function fetches its definition; the client renders a form from `input_schema` (or a widget named in `ui_schema.widget`: `JsonEditor`, `Form`, `Card`, `Settings`, …), submits to `/invoke`, polls status and shows result/logs.
- **How:** `DynamicExecutor`, `EntityEditor` (submit → poll every 1.5 s), `JsonSaver` [UC:ui/components/Command/DynamicExecutor.tsx], [UC:ui/components/Widgets/EntityEditor.tsx]; D-UC-36 phases: dynamic form engine → split screen with streaming → console/direct entity.
- **Why (intent):** Metadata-driven UI: a new function needs no front-end code.
- **Tags:** ui-rendering, form, metadata

### CON-UC-055 — Lens system: component = f(type chain, mode)
- **What:** Instead of shipping a UI schema per entity, the client computes the component from the entity's type, its `_extends` chain and the current mode (`edit`, `read`, `cell`, `card`, …) via a **UI matrix** stored as an entity (`SysConf:ui_matrix`).
- **How:** Resolver: look up `(entity_type, mode)`; if absent walk `_extends[0..]`; else fall back to the `BaseEntity` component. Examples: `SysFunction+edit → IDEComponent`, `+read → CodeHighlightViewer`, `+cell → FunctionBadge`; `SysUser+cell → UserAvatar`; a new `SysJavaFunction extends SysFunction` needs zero UI work; `Money` gets thousand separators everywhere by one change; an unknown `CreditCardInput` degrades to a text field ("semantic fallback").
- **Why (intent):** D-UC-53 — UI work from O(N) to O(1); UI becomes a configurable, inheritable asset; global consistency; the system "never breaks".
- **Status:** design.
- **Sources:** [UC:docs/front/front_04.md].
- **Tags:** ui-rendering, meta-model, type-system

### CON-UC-056 — Script-pushed cards
- **What:** `sys_push_card(card_type, map)` emits `{__ui_type: "card", card_type, data}` into the execution log; the Passive feed renders by `card_type`.
- **How:** [UC:src/kernel/syscalls/ui.rs#L9-L29], [UC:ui/components/Passive/FeedStream.tsx]. Design alternative: SSE/WebSocket streaming (D-UC-36, Plan_14).
- **Why (intent):** Scripts can talk to the user during execution without a UI deployment.
- **Tags:** ui-rendering, scripting, events

### CON-UC-057 — Universal Resource Browser
- **What:** An address bar accepting only `ubos://…`; left pane shows the JSON of the selected version (editable only at HEAD, read-only for history); right pane is a timeline of versions (commit id, relative time, operator) — clicking time-travels.
- **How:** Uses `/eval` with `sys_db_get` and `sys_db_history` in parallel [UC:ui/components/Tools/UbosResourceBrowser.tsx#L94-L96]; design D-UC-48 (future wildcard `ubos://sys/SysFunction/*`).
- **Why (intent):** The admin/developer workbench for governing data — "everything is an object" made browsable.
- **Tags:** frontend, versioning, protocol

### CON-UC-058 — Front-end copilot contract (AI orchestrates UI)
- **What:** A system prompt for an in-browser AI "orchestration hub": it keeps context state (host, sovereign, branch, workspace id, user), maps intents to tools (`search_entity`, `load_function_ui`, `invoke_kernel`), and emits structured instructions ("render JsonEditor at MainStage") that the front end executes through a **component registry** and **zone map** (`Active Stage`, `Command Palette`, `Passive Zone`); error rules (reconnect on lost embassy, red card on policy denial).
- **Why (intent):** D-UC-52 — decouple business logic and UI implementation; natural language becomes a first-class input.
- **Status:** design.
- **Sources:** [UC:docs/front/front_03.md].
- **Tags:** ai, frontend, copilot

### CON-UC-059 — Submit → poll → present async UX
- **What:** Accepted (202 + job id) → poll status (or WebSocket) → final state; UI variants: status tag, live console (incremental logs), notification center linking to provenance.
- **Sources:** [UC:docs/plan/Plan_17.md]. Divergence: UI calls `/job/{id}/status`; API exposes `/workspace/{id}/status`.
- **Tags:** frontend, process
