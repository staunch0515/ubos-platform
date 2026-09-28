---
id: ANA-UW-05
title: "UW Interfaces & UI"
status: complete
phase: P1
depends_on: [ANA-UW-01, ANA-UW-03]
sources: [UW]
---

# UW — Interfaces & UI

### CON-UW-050 — Command bar as the single entry point
- **What:** Spotlight-style input (Ctrl/⌘+K), live mode indicator (search icon vs pulsing terminal icon in command mode), dropdown grouped into features and entities, command preview [UW:components/CommandBar.tsx].
- **Tags:** frontend, search

### CON-UW-051 — Context switcher showing tenant and branch
- **What:** Trigger shows context name plus tenant and branch (git-branch icon); dropdown lists available contexts with methodology and sprint; switching updates the global context [UW:components/ContextSwitcher.tsx].
- **Tags:** frontend, context, multi-tenancy

### CON-UW-052 — Dock with passive and active modes
- **What:** Passive: "Incoming — N pending" list, priority colour bar, type icon (approval / notification / issue), context badge, relative time, unread pulse, "via <source>". Active: coloured header by intent type (New Entity, Workflow, Daily Sync, Details), context label, close button, intent-specific body [UW:components/TaskPanel.tsx].
- **Why (intent):** Same area serves as notification inbox and task execution pane; incoming work flows directly into doing work.
- **Tags:** frontend, events, workflow

### CON-UW-053 — Workspace feature views
- **What:** `kanban` → board with To Do / In Progress / Done columns, cards with priority dot and assignee initial, filter, add task; default → dashboard with stat cards (users, uptime, sessions) and an area chart [UW:components/Workspace.tsx].
- **Tags:** frontend, ui-rendering

### CON-UW-054 — Category sidebar (unmounted)
- **What:** Feature catalog grouped by category (the "three-column" left column), present but not used in the current layout [UW:components/FeatureSidebar.tsx].
- **Tags:** frontend
