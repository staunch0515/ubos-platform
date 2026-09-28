---
id: ANA-UW-00
title: "UW Overview"
status: complete
phase: P1
depends_on: [INV-UW]
sources: [UW]
---

# UW (ubos_web) — Overview

## Vision as stated by the repository

- `metadata.json` (Google AI Studio app): **"UBOS System — A modern, three-column layout enterprise system … featuring fuzzy search, workspace management, and task tracking."**
- `types.ts` section header: **"JIRA/UBOS Mapping Types"** — a Jira-like work-management domain expressed with UBOS shell concepts.

## Problem it addresses

The end-user shell of a business operating system: how a user finds functions without menus,
states intent, works in a context (project + tenant + branch), and receives incoming work
(issues, notifications, approvals) in one place.

## Design in one paragraph

A frontend-only React 19 prototype (1.4k lines, mock data, no backend). A header sits above a
**70 % workspace column** and a **30 % task dock**:
- **Workspace column:** a context switcher and a command bar are on top, and the selected feature is rendered below it (a dashboard with charts, or a Kanban board).
- **Command bar:** fuzzy-searches features and entities (issues). It recognises command-like input (`/…`, `bug "…" @user`, `task`, `close 123`, `board`, `standup`) and parses it into **typed intents**.
- **Intent routing:** `BOARD` changes the workspace view. Every other intent (`CREATE_ISSUE`, `TRANSITION_ISSUE`, `STANDUP`, `VIEW_ISSUE`) turns the dock into a task pane, which shows a form, a workflow step with a guardrail checklist, or a stand-up view.
- **Idle dock:** when no intent is active, the dock is a prioritized inbox of dock items (issue / notification / approval, with source, context and unread flag). Clicking an item creates a `VIEW_ISSUE` intent.

## File map

| File | Content | Key IDs |
|---|---|---|
| `01-architecture.md` | Shell layout, state model, intent routing | CON-UW-001…003 |
| `02-core-model.md` | Feature catalog, context, intent, dock item, issue | CON-UW-010…014 |
| `03-runtime.md` | Command parsing, intent-to-pane rendering, guardrails, stand-up | CON-UW-020…023 |
| `04-data-persistence.md` | Mock data only | CON-UW-040 |
| `05-interface-ui.md` | Command bar, context switcher, dock, workspace views | CON-UW-050…054 |
| `06-extensibility-ai.md` | Extension points, AI Studio origin | CON-UW-060…061 |
| `07-design-docs-digest.md` | README, metadata (D-UW-01…02) | – |
| `08-highlights.md` | 5 highlights | HL-UW-001…005 |
| `09-concept-index.md` | Flat index | – |

## Top highlights

1. HL-UW-001 Intent-driven command bar (text commands → typed intents).
2. HL-UW-002 Context = project + tenant + branch + methodology + sprint, switched in one place.
3. HL-UW-003 Unified work dock (issues, notifications, approvals) that doubles as the task pane.

## Notes for later phases

- A UX concept prototype, earliest in the corpus by date (single day, 2025-12-28). Its shell ideas reappear in richer form in UC/US (Cockpit: CON-UC-052/053, CON-US-055) — compare in P2 topic 14 (client shells) without assuming lineage.
- `FeatureSidebar.tsx` exists but is not mounted; the layout is two columns under a header, while metadata says "three-column".
- Workflow guardrails and stand-up content are static UI.
