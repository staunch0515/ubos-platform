---
id: SPEC-26
title: "Client Shells"
status: complete
phase: P4
depends_on: [SPEC-19, SPEC-22, SPEC-24, SPEC-25, SPEC-28, ADR-003, ADR-006]
sources: [UP, FU, UB, UC, US, UW]
---

# Client Shells

## 0. Chapter header

- **Scope:** This chapter specifies the five clients (ADR-003) and the IDE tooling:
  - shell boot and server-defined shell layouts;
  - the web **cockpit**: identity, context, omnibar with command grammar, the active stage and the passive dock;
  - the **studio**: builder and operator features;
  - the desktop app with its embedded kernel;
  - the mobile app with its reduced cockpit;
  - the CLI;
  - the SDK and agent clients;
  - the IDE file-system integration;
  - client state and caching rules.
- **MOD:** `SHELL`
- **depends_on:** SPEC-19, SPEC-22, SPEC-24, SPEC-25, SPEC-28, ADR-003, ADR-006.
- **Terms used:** TERM-Client, TERM-Cockpit, TERM-Omnibar, TERM-Dock, TERM-Studio, TERM-Intent, TERM-IdeIntegration, TERM-RenderPayload, TERM-PushedUi.
- **Origin summary:**
  - Verdicts: VRD-14-01…04, VRD-06-06, VRD-19-04, VRD-15-02.
  - UC: bicameral UI, menu-less command cockpit, resource browser (CON-UC-052, 053, 057).
  - UW: workspace + dock, single intent state, command grammar, context switcher (CON-UW-001…003, 013, 020, 050…053).
  - UP: Web OS shell, boot handshake, copilot console (CON-UP-053, 054, 056).
  - FU: studio, time travel, diff and merge UIs, governance managers, git-like terminal (CON-FU-050…055).
  - UB: Meta-IDE and inheritance-aware studio (CON-UB-053, 054).
  - US: action automator, Rhai authoring, scenario CLI, VS Code design, tooling (CON-US-004, 055…057, 065).

---

## 1. Concepts

| Client | Audience | Tech (ADR-001/003/006) | Transport | Shell |
|---|---|---|---|---|
| Web | end users, builders, operators | React/TS/Vite, `@ubos/shell` | WebSocket (+ HTTP for blobs) | cockpit + studio |
| Desktop | personal edition, offline, developers | Tauri 2 + the web shell + embedded kernel | in-process (local) or WebSocket/TCP (remote) | cockpit + studio |
| Mobile | end users on the move | Tauri 2 mobile + the web shell in mobile layout | WebSocket | reduced cockpit |
| CLI | operators, developers, CI | Rust `ubos_cli` | TCP/WebSocket/HTTP or in-process (`--local`) | command line |
| SDK / agent | programs, integrations, AI agents | Rust `ubos_sdk`, TS `@ubos/sdk` | any binding | none |
| IDE integration (tooling) | developers | VS Code extension on the TS SDK | WebSocket | editor tree + files |

**Rules for every client** (VRD-14-04):
- use UBTP only;
- render server-resolved payloads;
- hard-code no business screens or menus.

---

## 2. Model

```yaml
ENT-Shell:
  purpose: Server-defined shell layout (UP desktop as data; VRD-14-01) - entity of type Shell.
  origin: [VRD-14-01, CON-UP-054, CON-UC-052, CON-UW-001]
  fields:
    - {name: title, type: string, required: true, description: "Name"}
    - {name: kind, type: "enum{COCKPIT|STUDIO|MOBILE}", required: true, description: "Shell family"}
    - {name: home, type: "Intent?", required: false, description: "What the stage shows after context selection (e.g. OPEN a dashboard View)"}
    - {name: zones, type: "{active_ratio?: float (default 0.62), dock_min_px?: int (default 320)}", required: false, description: "Bicameral proportions (golden ratio, UC)"}
    - {name: dock_sources, type: "list<enum{TASKS|APPROVALS|NOTIFICATIONS|JOBS|CARDS}>", required: true, description: "What the passive dock aggregates"}
    - {name: omnibar_sources, type: "list<enum{ACTIONS|ENTITIES|VIEWS|APPS|CONTEXTS|RECENT|COMMANDS|ASK}>", required: true, description: "Omnibar result groups"}
    - {name: apps, type: "list<uri<App>>?", required: false, description: "Pinned apps"}
    - {name: theme, type: "{tokens: map<string,string>, logo?: uri<File>, dark?: bool}?", required: false, description: "Branding (design tokens)"}
    - {name: roles, type: "list<uri<Role>>?", required: false, description: "Roles this shell applies to (first matching shell wins)"}
```

```yaml
ENT-App:
  purpose: A named bundle of views and actions presented as an application (UP apps are data; UB app manifest).
  origin: [CON-UP-054, CON-UB-062, CON-UW-010]
  fields:
    - {name: title, type: string, required: true, description: "App name"}
    - {name: icon, type: string, required: true, description: "Icon"}
    - {name: home, type: "uri<View>", required: true, description: "Start view"}
    - {name: views, type: "list<uri<View>>", required: false, description: "Navigable views (tabs/sections)"}
    - {name: actions, type: "list<uri<Action>>", required: false, description: "Featured actions"}
    - {name: category, type: string?, required: false, description: "Grouping in the launcher (UW feature catalog)"}
    - {name: roles, type: "list<uri<Role>>?", required: false, description: "Visible to these roles (plus normal read permission)"}
```

```yaml
ENT-CommandGrammar:
  purpose: Typed command patterns of the omnibar (UW command grammar), provided by packages.
  origin: [VRD-06-06, CON-UW-020, CON-FU-055]
  fields:
    - name: rules
      type: "list<CommandRule>"
      required: true
      description: "{pattern, intent, description, example}; pattern tokens - literal words, \"quoted\" -> string slot, @name -> principal slot, #tag, <n> -> number slot, [optional]; intent = Intent template using ${slot}"
  invariants:
    - Patterns are unambiguous within one grammar; conflicts across grammars resolved by package priority then specificity.
```

Example grammar (from UW):

```yaml
rules:
  - pattern: 'bug "title" [@assignee]'
    intent: { kind: CREATE, target: "Type/acme.dev.Issue", args: { title: "${title}", kind: "BUG", assignee: "${assignee}" }, placement: DOCK }
  - pattern: 'close <n>'
    intent: { kind: TRANSITION, target: "acme.dev.Issue/${n}", event: close, placement: DOCK }
  - pattern: 'board'
    intent: { kind: OPEN, view: "View/acme.dev.board", placement: WORKSPACE }
```

```yaml
ENT-ClientState:
  purpose: Normative client-side state model (not stored on the server).
  origin: [CON-UP-053, CON-UW-002, CON-UC-053]
  fields:
    - {name: boot, type: "enum{BOOTING|LOGIN_REQUIRED|CONTEXT_SELECT|READY|OFFLINE|ERROR}", required: true, description: "Shell state machine (REQ-SHELL-001)"}
    - {name: session, type: "Welcome?", required: false, description: "UBTP Welcome (principal, context)"}
    - {name: stage, type: "Intent?", required: false, description: "What the active zone shows"}
    - {name: dock_intent, type: "Intent?", required: false, description: "Active task in the dock (UW single intent), else inbox"}
    - {name: cache, type: "map<uri with commit, payload>", required: true, description: "Immutable server data cache (REQ-SHELL-060)"}
```

---

## 3. Behavior

### 3.1 Boot and context

### REQ-SHELL-001 — Boot state machine
- **Statement:** Every interactive client MUST implement this state machine:

| State | Entered when | Next |
|---|---|---|
| BOOTING | start | connect: LOGIN_REQUIRED (no valid token), CONTEXT_SELECT, READY (remembered context), OFFLINE/ERROR |
| LOGIN_REQUIRED | no or expired credentials | login (password, OIDC, setup token) → CONTEXT_SELECT |
| CONTEXT_SELECT | several contexts are available and none is remembered | choose → READY |
| READY | Handshake done and `shell.boot` loaded | context switch → READY (stage reset); token expiry → LOGIN_REQUIRED |
| OFFLINE | connection lost | reconnect with `resume` → READY |
| ERROR | server incompatible or unreachable at start | retry |

  In READY, the client calls `shell.boot@v1`. The server returns the Shell definition for the principal (role match), the apps visible to it, the command grammars in the context, and the dock summary. What the user sees is thus decided by server data (UP: "the UI is a thin terminal").
- **Origin:** CON-UP-053, CON-UC-053, VRD-14-01
- **Acceptance:** An admin and a clerk logging into the same context get different shells and apps, with no client change.
- **Priority:** P1

### REQ-SHELL-002 — Context visibility and switching
- **Statement:** The cockpit MUST show the current identity and context (tenant · context · branch, plus a pin/read-only badge) permanently in the header (UW context switcher). The switcher lists the Contexts the principal can use (`context.list@v1`) and session contexts.
  - Switching context performs a new Handshake (or `authority` change), then clears the stage and the dock intent. This is "like rebooting" (UC), and it prevents work in one context from leaking into another.
  - Before a write to a MERGE_ONLY or production-kind branch, the client shows a confirmation naming the branch (UC Plan_17).
- **Origin:** VRD-14-01, CON-UC-053, CON-UW-051, CON-UC-045
- **Acceptance:** Switching from `acme.prod` to `acme.uat` empties the stage, and the header shows `acme · uat · main`.
- **Priority:** P1

### 3.2 Cockpit

### REQ-SHELL-010 — Bicameral layout
- **Statement:** The web and desktop cockpit MUST have two zones:
  - **Active zone (stage)**, about 62 % of the width: the current work, i.e. views, forms, the action automator and the resource browser.
  - **Passive zone (dock)**, at least 320 px: incoming work and pushed content.

  The header holds identity, context and the omnibar. There are no menus: navigation is done through the omnibar, apps and links (CON-UC-053). On narrow screens the dock becomes a slide-over panel.
- **Origin:** VRD-14-01, CON-UC-052, CON-UW-001
- **Acceptance:** At 1440 px the stage and dock are side by side. At 800 px the dock opens as an overlay.
- **Priority:** P1

### REQ-SHELL-011 — Omnibar
- **Statement:** The omnibar (Ctrl/⌘+K) MUST:
  1) show results grouped by the Shell's `omnibar_sources`:
     - actions available in the context;
     - entities (text search, REQ-QRY-005);
     - views and apps;
     - contexts;
     - recent items;
     - commands (grammar matches with a preview of the resulting intent);
  2) enter **command mode** when the input starts with `/` or with the first token of a grammar rule, with a visible mode indicator (UW);
  3) send input that matches neither mode to **ASK** (the copilot, SPEC-27) when enabled;
  4) produce exactly one Intent on selection (REQ-FLOW-050).

  Result lists appear within 150 ms of typing (debounced server search plus local recent items).
- **Origin:** VRD-14-01, VRD-06-06, CON-UW-050, CON-UW-020, CON-UW-021, CON-UC-053
- **Acceptance:** Typing `close 42` shows the preview "Close issue 42" and runs the transition on Enter. Typing `hangzhou` lists matching customers.
- **Priority:** P1

### REQ-SHELL-012 — Intent routing
- **Statement:** The cockpit MUST route intents by `placement` (UW view/task routing):
  - WORKSPACE, and by default OPEN, VIEW and SEARCH: the stage;
  - DOCK, and by default CREATE, RUN_ACTION, TRANSITION and approvals: the dock's active pane.

  The dock shows one active intent at a time, and closing it returns to the inbox (UW single intent). Users can move a dock task to the stage and back.
- **Origin:** CON-UW-002, CON-UW-003, VRD-06-06
- **Acceptance:** Opening a board shows it on the stage. Clicking "approve" on a dock item opens the approval form in the dock without leaving the board.
- **Priority:** P1

### REQ-SHELL-013 — Dock (passive zone)
- **Statement:** The dock MUST aggregate, per the Shell's `dock_sources`:
  - TASK notifications and pending approvals where the principal is an eligible approver;
  - the principal's running and failed jobs (JOB subscription);
  - other notifications;
  - pushed CARDs.

  Items show type icon, title, source ("via …"), context badge, priority, relative time and unread state (UW). Items update live through subscriptions (REQ-PROTO-011). Selecting an item turns its link or intent into the active task.
- **Origin:** VRD-14-01, CON-UW-013, CON-UW-052, CON-UC-052, VRD-11-04
- **Acceptance:** A new approval request appears in approvers' docks within 1 s. Approving it removes it from all their docks.
- **Priority:** P1

### REQ-SHELL-014 — Action automator
- **Statement:** Running any Action in the cockpit MUST use one generic component driven only by TransitionResponses (REQ-FLOW-015):
  - DESCRIBE → params form;
  - PREPARE → target form, guard checklist, available events as buttons;
  - EXECUTE → result, pushed UI, and a close or next step.

  Validation errors map to fields by path. `HEAD_CONFLICT` offers "reload and re-apply". ASYNC results show job progress with a cancel button.
- **Origin:** CON-US-055, CON-US-022, VRD-06-03, VRD-15-04
- **Acceptance:** A new TwoStepAction defined only as data runs end to end in the cockpit with no front-end change.
- **Priority:** P1

### REQ-SHELL-015 — Apps and home
- **Statement:** After READY, the stage MUST show the Shell's `home` intent, or an app launcher of the visible Apps grouped by `category` when there is none. Apps open their `home` view, and their `views` appear as tabs. Installing a package that defines Apps makes them appear for permitted users without a client update (UP apps as data).
- **Origin:** CON-UP-054, CON-UW-010, CON-UB-062
- **Acceptance:** Installing `acme.crm` adds a "CRM" tile for sales users.
- **Priority:** P1

### 3.3 Studio

### REQ-SHELL-020 — Studio features
- **Statement:** The studio, a Shell of kind STUDIO shown to principals with `definition.author` or `tenant.admin`, MUST provide the features below. It uses only public operations, and where practical its screens are themselves Views and Actions (the UB Meta-IDE principle).

| Feature | Built on |
|---|---|
| Namespace tree + entity grid per type, with batch actions (commit, diff, merge) | REQ-URI-015, SPEC-21 |
| Inheritance-aware editor (form with inherited/overridden markers, reset-to-inherited, X-ray) and JSON mode (Monaco) | REQ-UI-050, `entity.edit_raw` |
| Type designer (properties, value types, slots, constraints, invariants, lifecycle reference) | SPEC-13, meta-schema |
| Logic editor: Rhai with completions from `rt.abi`, diagnostics from `rt.check`, run / dry-run console with logs and staged diff | SPEC-17 REQ-RT-030, `rt.eval` |
| Resource browser: `ubos://` address bar, version timeline, time-travel slider, blame mode, diff viewer | SPEC-14, SPEC-21 REQ-QRY-020 |
| Branch tools: status, drafts (publish/discard), merge with conflict resolver (base/ours/theirs per path) | SPEC-14 |
| Package manager: install (with plan preview), export, verify | SPEC-28 |
| Governance managers: users, groups, roles, policies (with `security.explain`), API keys (show-once), secrets, webhooks, contexts, approvals | SPEC-22, SPEC-16, SPEC-20 |
| Process and job monitor; audit viewer with trace tree and lineage graph | SPEC-30, SPEC-20 |
| Operational dashboard (`audit.metrics`, health) | REQ-OBS-015 |
| Terminal: the CLI command set (REQ-SHELL-040) executed in the browser over the TS SDK | REQ-SHELL-041 |
- **Origin:** VRD-14-02, CON-FU-050…055, CON-UB-053, CON-UB-054, CON-UC-057, CON-US-056
- **Acceptance:** A builder creates a type, a lifecycle, an action and a view, tests them with a dry run, and publishes the draft, all inside the studio.
- **Priority:** P1 (P2: visual designers for lifecycles/workflows/views)

### 3.4 Desktop

### REQ-SHELL-030 — Desktop app
- **Statement:** The desktop app (Tauri 2) MUST bundle the web shell and the kernel (personal edition, SQLite). It offers two modes:
  - **Local:** the embedded kernel over the in-process binding (REQ-PROTO-023). Data is stored in the OS user data directory. A first-run wizard creates the tenant `local` and its owner user (REQ-PKG-043 step 5).
  - **Remote:** connect to a server URL like the web client.

  The app exposes the local file system for package development (`dev --watch`) and for import and export. It keeps working offline in local mode. Sync with a server is deferred (VRD-19-05).
- **Origin:** VRD-19-02, ADR-003, CON-US-001, CON-LS-062
- **Acceptance:** On a machine without network access, the desktop app boots, creates the local tenant and runs the cockpit and studio.
- **Priority:** P1

### 3.5 Mobile

### REQ-SHELL-035 — Mobile app
- **Statement:** The mobile app (ADR-006: Tauri 2 mobile, same React shell in the MOBILE layout) MUST provide a reduced cockpit:
  - the dock (tasks, approvals, notifications, jobs) as the home screen;
  - the omnibar with search and commands;
  - entity views and edit forms using MOBILE view variants and mobile widget fallbacks (REQ-UI-002);
  - the action automator;
  - approvals with one-tap approve or reject (plus comment).

  It provides no studio, no raw editing and no package or admin tools. Push notifications through APNs/FCM relays are P2. Until then, notifications arrive while the app is connected.
- **Origin:** ADR-003, VRD-19-04, VRD-10-04, CON-UW-052
- **Acceptance:** An approver receives a TASK in the mobile dock and approves it, and the requester's web dock updates.
- **Priority:** P1

### 3.6 CLI

### REQ-SHELL-040 — CLI verbs
- **Statement:** `ubos` (Rust, `clap`) MUST provide these verbs over any binding, or in local mode (`--local <path>`):

| Group | Verbs (FU git-like where applicable) |
|---|---|
| session | `login`, `logout`, `whoami`, `context [set <authority>]` |
| entities | `get <uri> [--own] [--expand n]`, `ls <Type> [--where …] [--sort …]`, `search <text>`, `put <uri> --data file` (Mutate UPSERT), `patch <uri> --merge json`, `rm <uri>`, `mv <uri> <new-slug>`, `cp <src> <dst>`, `history <uri>`, `diff <a> <b>`, `blame <uri>`, `revert <uri> <commit>` |
| branches | `branch ls\|create\|delete\|status`, `merge <src> <dst> [--strategy]`, `draft publish\|discard`, `tag <name> …` |
| operations | `run <action\|pipeline uri> [--target uri] [--args json] [--dry-run] [--async]`, `query <named uri> [--args]`, `eval <file.rhai> [--commit]` |
| jobs | `job ls\|status\|cancel\|retry` |
| packages | `pkg new\|validate\|install\|export\|verify` (SPEC-28), `dev --watch` |
| scenarios | `scenario run <file…> [--binding tcp\|ws\|http\|local]` |
| admin | `tenant create\|suspend\|…`, `boot [--strategy]`, `audit record\|export`, `index rebuild` |

  Output is a table by default, or `--output json|yaml|ndjson`. Exit codes: 0 on success, 1 on command errors (the error code is printed), 2 on usage errors. Credentials go in the OS keychain.
- **Origin:** VRD-14-03, CON-US-004, CON-FU-055, VRD-22-02
- **Acceptance:** A CI job runs `ubos pkg install`, `ubos pkg verify` and `ubos scenario run` against a test server with an API key and fails on the first failing scenario.
- **Priority:** P0 (session, entities, run/query, pkg, scenario), P1 (others)

### REQ-SHELL-041 — Shared command grammar
- **Statement:** The CLI verbs MUST be specified once, as a machine-readable command table (`spec-fixtures/cli-commands.json`). The Rust CLI and the studio's web terminal (TS) both implement it with identical parsing: quoted tokens, `--opt value`, `-o value` and flags (FU CommandParser). Both map each command to UBTP messages.
- **Origin:** CON-FU-055, VRD-22-04 (one vocabulary)
- **Acceptance:** The same command line produces the same UBTP message in the CLI and in the web terminal (shared test fixtures).
- **Priority:** P2

### 3.7 SDK and agents

### REQ-SHELL-050 — SDKs
- **Statement:** The Rust SDK (`ubos_sdk`) and the TypeScript SDK (`@ubos/sdk`) MUST provide:
  - connect (all bindings) with Handshake and resume;
  - `mutate`, `emit`, `query` and `subscribe`, with streaming callbacks;
  - the URI library (REQ-URI-016) and error types;
  - blob upload and download;
  - the scenario runner (Rust).

  They carry no business logic. Agent programs (SPEC-27) use them with AGENT principals.
- **Origin:** VRD-19-04, VRD-14-04, CON-US-050
- **Acceptance:** A 20-line TS script creates an entity, subscribes to its type and prints change events.
- **Priority:** P0

### 3.8 IDE integration (tooling)

### REQ-SHELL-055 — `ubos:` file system for IDEs
- **Statement:** The VS Code extension (P2) MUST implement a FileSystemProvider for the `ubos:` scheme over the TS SDK:
  - **Tree:** tenant · context → namespaces → Types (`.json`), Logic (`.rhai`), Actions, Pipelines, Views (`.json`).
  - **Reading** a file loads the entity's own data, and Logic appears as its source.
  - **Saving** is a Mutate on the developer's draft branch of the context (never on main), with the expected head of the opened version. A conflict opens a diff.
  - **Language support:** diagnostics from `rt.check`, completions and hovers from `rt.abi`.
  - **Commands:** `Publish draft`, `Discard draft`, `Show history`, `Blame`.

  The web studio stays the tool for quick edits and demos.
- **Origin:** VRD-14-03, CON-US-057, CON-UC-044
- **Acceptance:** Editing a Logic file in VS Code and saving it creates a draft version, and the web cockpit on the same draft uses it immediately.
- **Priority:** P2

### 3.9 Client state

### REQ-SHELL-060 — State and caching rules
- **Statement:** Clients MUST:
  - keep server data in an SDK cache keyed by canonical URI **with commit**, which is immutable and never revalidated;
  - keep "current head" lookups in a separate map, invalidated by subscription events (`entity.committed`);
  - keep UI state only in the UI store (zustand, REQ-CONV-085);
  - never derive permissions or business rules locally, beyond rendering what the payload allows;
  - include `base_commit` on every write that comes from a form.
- **Origin:** REQ-CONV-085, CON-LS-044, VRD-04-03
- **Acceptance:** Revisiting a previously opened version makes no request, and an entity change by another user refreshes the open view within 1 s.
- **Priority:** P1

---

## 4. Interfaces

### API-SHELL-001 — Named operations for shells
- **Kind:** UBTP targets
- **Signature / shape:**

| URI | Args | Result |
|---|---|---|
| `ubos://system/Query/shell.boot@v1` | `{platform}` | `{shell: Shell, apps: [App], grammars: [CommandGrammar], dock: {counts}, principal, context}` |
| `ubos://system/Query/context.list@v1` | – | contexts usable by the principal (tenant, slug, title, kind, branch, pinned) |
| `ubos://system/Query/shell.omnibar@v1` | `{text, sources?, limit?}` | grouped results with intents |
| `ubos://system/Query/shell.dock@v1` | `{page?}` | dock items |
| `ubos://system/Query/shell.recent@v1` | – | recent intents of the principal (runtime store) |
- **Origin:** CON-UP-053 (`/api/boot`), CON-UW-021

### API-SHELL-002 — Web shell packages
- **Kind:** TS packages
- **Signature / shape:** `@ubos/sdk` (protocol, URI, errors) · `@ubos/widgets` (standard catalogue, REQ-UI-060) · `@ubos/shell` (cockpit, studio, mobile layouts; boot state machine; omnibar; dock; action automator). Desktop and mobile wrap `@ubos/shell`.
- **Origin:** ADR-001, ADR-006

### Error codes (SHELL)

| Code | Category | Meaning |
|---|---|---|
| `SHELL.NO_SHELL` | NOT_FOUND | no Shell definition matches the principal (a default is used and a warning logged) |
| `SHELL.GRAMMAR_CONFLICT` | INVALID | ambiguous command patterns at grammar commit |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-UX-180 | Web cockpit cold start to READY (cached assets) | ≤ 2 s |
| NFR-UX-181 | Omnibar results after typing pause | ≤ 150 ms |
| NFR-UX-182 | Dock update after server event | ≤ 1 s |
| NFR-PORT-180 | Supported platforms | evergreen browsers; Windows/macOS/Linux desktop; iOS 16+/Android 10+ |
| NFR-UX-183 | Keyboard-only operation of cockpit core flows | 100 % (omnibar, automator, dock) |

---

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-SHELL-001, 002 | Boot states; role-specific shells; context switch resets the stage; protected-branch confirmation |
| REQ-SHELL-010…015 | Layout; omnibar groups and command mode; intent routing; live dock; generic action automator; apps as data |
| REQ-SHELL-020 | Studio end-to-end builder scenario |
| REQ-SHELL-030, 035 | Offline desktop; mobile approval flow |
| REQ-SHELL-040, 041 | CLI in CI; shared grammar fixtures |
| REQ-SHELL-050 | SDK script |
| REQ-SHELL-055 | IDE save to draft |
| REQ-SHELL-060 | Immutable cache and live refresh |

---

## 7. Implementation notes

- **Reference code:**
  - UC cockpit [UC:ui/components/], context store [UC:ui/store/useContextStore.ts], resource browser [UC:ui/components/Tools/UbosResourceBrowser.tsx].
  - UW shell [UW:App.tsx], [UW:components/CommandBar.tsx], [UW:components/TaskPanel.tsx], [UW:components/ContextSwitcher.tsx].
  - UP OS shell [UP:ubos-shell/src/components/Desktop.tsx], [UP:ubos-shell/src/store/kernelStore.ts].
  - FU studio [FU:frontend/src/components/].
  - US action automator [US:apps/ubos_web/src/components/Action/ActionAutomator.tsx], Rhai editor [US:apps/ubos_web/src/components/Editor/RhaiEditor.tsx], CLI [US:apps/ubos_cli/src/main.rs].
- **Divergences resolved:**
  - UC/US feeds used mock services. Here they use subscriptions.
  - UC's axios headers become the Handshake context.
  - UP's windowed desktop is replaced by the bicameral cockpit. Apps remain data.
  - FU's localhost console becomes the studio behind permissions.
- **Libraries:** cmdk (omnibar), Monaco (JSON/Rhai), react-diff-viewer (diffs), TanStack Table (grids), Tailwind + shadcn/ui (ADR-001).

## 8. Open questions

None.
- Deferred to P2: visual designers (lifecycle/workflow/view), mobile push relays, the IDE extension.
- Deferred to a later version: offline sync (VRD-19-05).
