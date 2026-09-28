---
id: ANA-UW-01
title: "UW Architecture"
status: complete
phase: P1
depends_on: [ANA-UW-00]
sources: [UW]
---

# UW — Architecture

| File | Role |
|---|---|
| `App.tsx` | Shell composition and global state (`selectedFeature`, `activeIntent`, `dockItems`, `currentContext`) |
| `components/Header.tsx` | Top bar |
| `components/ContextSwitcher.tsx` | Context picker (name + tenant + branch) |
| `components/CommandBar.tsx` | Fuzzy search + command parser (Ctrl/⌘+K) |
| `components/Workspace.tsx` | Feature renderer (dashboard, Kanban) |
| `components/TaskPanel.tsx` | Dock: inbox (passive) or intent pane (active) |
| `components/FeatureSidebar.tsx` | Category catalog (not mounted) |
| `types.ts`, `constants.ts` | Domain types and mock data |

### CON-UW-001 — Shell: workspace column + task dock
- **What:** Header over two columns: left 70 % = context switcher + command bar + workspace content; right 30 % (min 320 px) = task dock [UW:App.tsx#L48-L95].
- **Why (intent):** Separate *where I work* (workspace, driven by my choice) from *what comes to me / what I am doing now* (dock).
- **Tags:** frontend, ui-rendering

### CON-UW-002 — Single intent state drives the dock
- **What:** One `activeIntent` in the app state; the dock renders the intent when present, otherwise the inbox; clearing returns to the inbox [UW:App.tsx#L10-L47].
- **Why (intent):** Everything the user is "doing" is an explicit, typed object, so any source (command bar, inbox click, search hit) opens the same pane.
- **Tags:** frontend, context

### CON-UW-003 — Intent routing rule: view intents vs task intents
- **What:** `BOARD` switches the workspace feature (and clears the task); all other intents go to the dock [UW:App.tsx#L17-L30].
- **Tags:** frontend, workflow
