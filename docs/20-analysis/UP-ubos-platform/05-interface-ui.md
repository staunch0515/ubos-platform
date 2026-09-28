---
id: ANA-UP-05
title: "UP Interfaces & UI"
status: complete
phase: P1
depends_on: [ANA-UP-01]
sources: [UP]
---

# UP — Interfaces & UI

## HTTP API surface

| Method & path | Controller | Semantics |
|---|---|---|
| `GET /api/boot` | `BootController` | Returns `{mode: OWNER|GUEST, desktop_schema}` from VIEW `schema.desktop.default@master` |
| `POST /api/run/{slug}?branch=master&commitId=` | `RunController` | Execute LOGIC; body = context |
| `POST /api/dev/commit` | `DevController` | Commit code entity `{type, slug, branch, code, message, processId}` → `{status, commitId, slug}` |
| `GET /api/view/{slug}` | `ViewController` | Resolve VIEW on identity-selected branch; echo branch in `X-UBOS-BRANCH` |
| `GET /api/audit/processes` | `AuditController` | Last 50 processes |
| `GET /api/audit/process/{id}` | `AuditController` | All commits (with snapshots) of a process |
| `GET /api/audit/history?slug=` | `AuditController` | Last 20 commits of an entity |
| `POST /api/auth/login` | `AuthController` | Credential check against USER entity |
| `POST /api/admin/ops/reset-world` | `AdminOpsController` | Re-run genesis (RESET) |

### CON-UP-050 — Small, generic API: boot / run / commit / view / audit
- **What:** The API is not per business object; it exposes the kernel's generic verbs.
- **How:** see table. Business features appear as new LOGIC/VIEW entities, not as new endpoints.
- **Why (intent):** A universal platform should need no new endpoints for new business.
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/server/controller/].
- **Tags:** api

### CON-UP-051 — Identity-driven branch resolution for views
- **What:** Which branch's version of a view a user sees depends on who they are.
- **How:** `ViewController`: `ADMIN` → branch `dev`; otherwise branch = `tenantId.toLowerCase()`; the resolved branch is echoed in the `X-UBOS-BRANCH` header; the copilot displays "Branch: X" above the rendered view. Commit `dac72ec`: "view branch".
- **Why (intent):** Same URL, different world per audience (developers see work-in-progress; tenants/regions see their overlay).
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/server/controller/ViewController.java], [UP:ubos-copilot/app.py] (`render_view`).
- **Tags:** ui-rendering, multi-tenancy, versioning

### CON-UP-052 — Identity governance via policy entities
- **What:** A `CONFIG` entity `policy.user.<userId>` = `{policyBranch}` decides which branch a user is bound to.
- **How:** `AuthWebFilter` reads the policy on `master`; its `policyBranch` becomes `AuthContext.tenantId`; missing policy → default context. Commit `022940d` "身份治理机制" (identity governance mechanism), `5313149` "多角色" (multi-role).
- **Why (intent):** Authorization/tenancy rules are versioned data editable at runtime.
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/server/config/AuthWebFilter.java].
- **Tags:** auth, permission, multi-tenancy

### CON-UP-053 — Boot handshake + client OS state machine
- **What:** The client "powers on", asks the kernel for its desktop definition, and moves through explicit states.
- **How:** `kernelStore.status ∈ {BOOTING, LOGIN_REQUIRED, READY, ERROR}`; `bootSystem()` → `GET /api/boot`; `login()` stores token then re-boots so the server can return an identity-specific desktop ("returns the admin desktop, e.g. with the quiz authoring app"); ERROR screen is a "blue screen" hint to start the kernel.
- **Why (intent):** The UI is a thin *terminal*; what the user sees is decided by server data.
- **Sources:** [UP:ubos-shell/src/store/kernelStore.ts], [UP:ubos-shell/src/App.tsx], [UP:ubos-server/src/main/java/org/logrum/ubos/server/controller/BootController.java].
- **Tags:** frontend, boot, ui-rendering

### CON-UP-054 — Web OS shell: apps are data
- **What:** A desktop metaphor where each icon is an app manifest carrying its own UI schema.
- **How:** desktop VIEW = `{wallpaper, theme, icons:[{id, title, icon, ui_schema}], start_menu}`; double-click → `openApp` creates a window `{id, title, icon, contentSchema, isMinimized, zIndex}`; window manager supports focus (z-order), minimize, close; drag via `react-draggable`; taskbar.
- **Why (intent):** Business applications are installed by committing a VIEW; the shell never changes.
- **Sources:** [UP:ubos-shell/src/components/Desktop.tsx], [UP:ubos-shell/src/components/WindowFrame.tsx], [UP:ubos-shell/src/store/osStore.ts], Genesis `commitDesktopSchema`.
- **Tags:** frontend, desktop, ui-rendering

### CON-UP-055 — Server-driven UI renderer with action binding
- **What:** A recursive renderer maps `{type, props, children}` nodes to registered components; buttons bind to logic slugs.
- **How:** Registry: `v-stack`, `text`, `button`, `input`; unknown types render an inline error; `button.props.action` = logic slug → `POST /api/run/{action}?branch=master`. Copilot has a Python twin: view `{title, components:[{type:input|button, key, label, action:{target_logic}}]}` collecting inputs from session state and running `target_logic`.
- **Why (intent):** UI definitions and their behavior are both data; UIs can be generated (by AI) and versioned.
- **Sources:** [UP:ubos-shell/src/engine/SchemaRenderer.tsx], [UP:ubos-copilot/app.py] (`render_component`, `handle_action`).
- **Tags:** ui-rendering, view, form

### CON-UP-056 — Copilot console with three modes
- **What:** A single operator console with Dev (AI Architect), User (App Browser) and Audit (Time-Travel) modes.
- **How:**

| Mode | Function |
|---|---|
| AI Architect (Dev) | Chat; system prompt: "convert natural language requirements into UBOS Groovy script logic", example `kernel.commit(...)` |
| App Browser (User) | Enter a view id → fetch `/api/view/{slug}` → render with branch banner |
| Time-Travel Audit | List processes → select → timeline of steps (entity, message, type, author, time, snapshot JSON) |

  Commit `ac86532`: "dual mode switch dev/run; universal renderer demonstrating dynamic forms".
- **Why (intent):** Build, use and audit the system from one place, with AI as the builder.
- **Sources:** [UP:ubos-copilot/app.py].
- **Tags:** ai, copilot, observability, frontend
