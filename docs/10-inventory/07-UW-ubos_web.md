---
id: INV-UW
title: "Inventory: ubos_web"
status: complete
phase: P0
depends_on: [META-TEMPLATES]
sources: [UW]
---

# Inventory: ubos_web (`UW`)

## 1. Identity

| Field | Value |
|---|---|
| Key | `UW` |
| Repository | staunch0515/ubos_web |
| Languages | TypeScript / React 19 (frontend only, no backend) |
| Build | npm + Vite 6 |
| Size (source lines) | TS/TSX 1,364 |
| Commits | 2 |
| Commit span | 2025-12-28 (single day) |
| Origin | Generated as a Google AI Studio app (README, `metadata.json`) |
| metadata.json description | "A modern, three-column layout enterprise system … featuring fuzzy search, workspace management, and task tracking." |

## 2. Top-level layout

```
ubos_web/
  App.tsx              three-column shell composition
  components/
    Header.tsx         top bar
    ContextSwitcher.tsx  project / tenant / branch context switcher
    FeatureSidebar.tsx   left: feature catalog by category
    Workspace.tsx        center: intent-driven workspace
    CommandBar.tsx       fuzzy command / intent input
    TaskPanel.tsx        right dock: issues, notifications, approvals
  constants.ts         mock features, dock items, issues
  types.ts             domain types (Feature, DockItem, Intent, Issue, ProjectContext)
```

## 3. Modules

| Name | Path | Role |
|---|---|---|
| Shell | `App.tsx` | Left feature sidebar · center workspace · right task dock |
| Context | `ContextSwitcher.tsx`, `types.ts#ProjectContext` | Context = project + **tenant** + **branch** + methodology + sprint |
| Intent | `CommandBar.tsx`, `types.ts#Intent` | Typed intents (`CREATE_ISSUE`, `TRANSITION_ISSUE`, `STANDUP`, `BOARD`, `VIEW_ISSUE`) resolved from fuzzy input |
| Dock | `TaskPanel.tsx`, `types.ts#DockItem` | Unified inbox of ISSUE / NOTIFICATION / APPROVAL items with priority, unread, source, context |

## 4. Tech stack

| Layer | Technology |
|---|---|
| UI | React 19, TypeScript 5.8, Vite 6, Tailwind (CDN), lucide-react, recharts |
| AI | Gemini API key expected by AI Studio template (not used by the core UI logic) |

## 5. Design documents

| Path | Lang | Topic |
|---|---|---|
| `README.md` | EN | AI Studio run instructions only |

## 6. Entry points

`index.html` → `index.tsx` → `App.tsx`.

## 7. Data assets

`constants.ts`: mock catalog of features across categories (Analytics, Administration, Development, Finance, Project Management), sample dock items and Jira-like issues.

## 8. First impression

- A **UX concept prototype** for the platform's end-user shell, not a platform implementation.
- Key ideas: **context switcher including tenant and branch** (the versioned-data model surfacing in the UX), **intent-driven command bar** (natural/fuzzy input mapped to typed intents), and a **unified right-hand dock** for work items (issues, notifications, approvals).
- Maps a Jira-like domain onto UBOS concepts ("JIRA/UBOS Mapping Types").

## 9. Analysis reading plan (P1)

Read all 11 source files in full (small repo). Focus on: intent model, context model, dock item model, layout concept.
