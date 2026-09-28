---
id: CMP-14
title: "Comparison: Client Shells and UX Paradigms"
status: complete
phase: P2
depends_on: [CMP-13]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Comparison: Client Shells and UX Paradigms

## 1. Question

What end-user and builder shells does the platform provide, and which interaction paradigm organises them? The candidates include a desktop, a studio, a cockpit, a command bar, a dock and a terminal.

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | Web OS shell (desktop, windows, taskbar; apps = VIEW entities); boot handshake state machine; Python copilot with Dev/User/Audit modes | CON-UP-053, 054, 056, HL-UP-009 | OS metaphor, server-defined desktop |
| FU | UBOS Studio: namespace tree, entity grid, Monaco, time-travel slider with blame, diff/merge resolver, governance managers, dashboards; git-like web terminal | CON-FU-050…055, HL-FU-010, 013 | Builder/operator IDE |
| LC | Two AI-generated React apps (end-user, admin) | CON-LC-051, 052 | Conventional apps |
| UB | Inheritance-aware Studio (inherited vs overridden, X-ray, lineage), self-hosting Meta-IDE, Executable Book (design) | CON-UB-053, 054, 055, HL-UB-011, 014 | Meta-model-aware editing |
| UC | Cockpit: menu-less, User → Workspace → Command → Stage; Active vs Passive zones; omnibar; REPL; resource browser; desktop windows | CON-UC-052, 053, 057, HL-UC-007, 010 | Bicameral command-centric |
| US | UC cockpit + action automator (FSM wizard), Rhai editor with diagnostics, VS Code extension (design) | CON-US-055, 056, 057 | Workflow + developer tooling |
| LS | None (protocol test clients) | CON-LS-050 | – |
| UW | Intent command bar; context switcher (tenant/branch); dock inbox ↔ task pane | CON-UW-001, 002, 050…052, HL-UW-001…003 | Intent + dock |

## 3. Dimension matrix

| Dimension | UP | FU | UB | UC/US | UW |
|---|---|---|---|---|---|
| End-user shell | ● OS | – | – | ● cockpit | ● |
| Builder / operator IDE | copilot | ● Studio | ● design | REPL, browser, Rhai IDE | – |
| Command / intent entry | – | terminal | – | ● omnibar | ● intents |
| Context visible and switchable | branch echo | tenant | – | ● workspace | ● tenant+branch |
| Passive / incoming zone | – | – | – | ● feed | ● dock |
| History / time travel UI | audit mode | ● slider + blame | X-ray | ● browser timeline | – |
| Inheritance awareness | – | drafts | ● | lens | – |
| Server-defined shell | ● | – | – | ◐ | – |

## 4. Analysis

- **Two families of shell.**
  - *End-user* shells: UP's OS, the UC/US cockpit, UW's intent/dock.
  - *Builder/operator* shells: FU Studio, UB Studio/Meta-IDE, UC REPL and resource browser, US Rhai IDE and VS Code design, UP copilot.
- **Shared principles.** Both families already agree on:
  - a single command entry point;
  - an always-visible context (tenant + branch/workspace);
  - an incoming/passive zone that turns into a task pane;
  - no hard-coded menus.
- **Which shell should be server-defined.**
  - UP's server-defined desktop and UC's bicameral layout do not conflict. The shell layout itself can be a View entity, so tenants can theme or rearrange it.
  - Builder tools are more specialised and can stay native apps, provided they only use the public API and protocol.

## 5. Verdicts

### VRD-14-01 — End-user shell: bicameral cockpit (fusion UC + UW + UP)
- **Decision:** fusion.
- **Consequences:**
  - Layout: an active zone (stage) and a passive zone (dock), both defined by a server-provided shell View (UP).
  - Hierarchy: Identity → Context (tenant, branch or environment always visible) → Command (omnibar with fuzzy search over actions, entities and views, plus a typed intent grammar from UW) → Stage.
  - Switching context resets the stage.
  - The passive dock lists work items, approvals, notifications, jobs and cards; selecting one opens it as a task on the stage or in the dock.

### VRD-14-02 — Builder studio (fusion FU Studio + UB Studio + UC tools + US IDE)
- **Decision:** fusion.
- **Consequences:** Required features:
  - namespace tree and entity grid;
  - an inheritance-aware editor (inherited vs overridden, reset-to-parent, lineage);
  - a code editor with language support and server diagnostics;
  - a resource browser by URI with a time-travel timeline, blame and diff/merge resolver;
  - governance managers (users, policies, keys, webhooks, approvals);
  - a REPL or console;
  - a process/job monitor.
- The studio is itself built from platform actions and views where practical (UB Meta-IDE principle).

### VRD-14-03 — Developer-native access via a virtual file system (adapt US VS Code design)
- **Decision:** adapt US VS Code design.
- **Consequences:** Logic, views and types are exposed as a `ubos:` file system to IDE extensions and a CLI. Save = commit on the draft branch; the terminal verbs are FU-style (`show / commit / checkout / diff / log`).

### VRD-14-04 — Shells are clients of the public protocol only (NEW rule)
- **Decision:** NEW rule.
- **Consequences:** No shell uses private endpoints. Mock services, like those used by UC/US feeds, must be replaced by protocol subscriptions (VRD-11-04).
