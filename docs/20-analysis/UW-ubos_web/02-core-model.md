---
id: ANA-UW-02
title: "UW Core Model"
status: complete
phase: P1
depends_on: [ANA-UW-01]
sources: [UW]
---

# UW — Core Model

### CON-UW-010 — Feature catalog
- **What:** `Feature {id, name, description, category ∈ {Analytics, Administration, Development, Finance, Project Management}, icon}`; sample: dashboard, kanban board, user management, system logs, billing & invoices, API keys, global settings [UW:types.ts#L1-L15], [UW:constants.ts].
- **Why (intent):** Functions are data items found by search, not menu entries.
- **Tags:** metadata, frontend

### CON-UW-011 — Work context: project + tenant + branch
- **What:** `ProjectContext {id, name, tenant, branch, methodology, currentSprint}` — e.g. `Mobile_App_Dev` / tenant `UBOS_Core` / branch `main`; `Infra_Upgrade` / `UBOS_Infra` / `release/v2.4` [UW:types.ts#L55-L63].
- **Why (intent):** Tenant and branch (the platform's data model) are part of the everyday working context, next to project-management attributes.
- **Tags:** context, multi-tenancy, versioning

### CON-UW-012 — Typed intent
- **What:** `Intent {type ∈ NONE | CREATE_ISSUE | TRANSITION_ISSUE | STANDUP | BOARD | VIEW_ISSUE, context?, data?}` [UW:types.ts#L37-L45].
- **Why (intent):** A small, closed vocabulary of user intentions that the UI can route and render.
- **Tags:** workflow, api

### CON-UW-013 — Dock item: unified incoming work
- **What:** `DockItem {id, type ∈ ISSUE | NOTIFICATION | APPROVAL, title, subtitle, source ("QA Team", "DevOps Bot", "PM (Alice)"), priority High|Medium|Low, timestamp, isUnread, context}` [UW:types.ts#L23-L34].
- **Why (intent):** One inbox for human and bot-originated work across contexts.
- **Tags:** events, workflow, frontend

### CON-UW-014 — Issue entity (Jira mapping)
- **What:** `Issue {id, key, title, status To Do|In Progress|Done, assignee, priority, description, timeSpent}` — the sample business entity [UW:types.ts#L47-L54].
- **Tags:** domain-pack, workflow
