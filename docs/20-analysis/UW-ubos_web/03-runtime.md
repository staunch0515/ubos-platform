---
id: ANA-UW-03
title: "UW Runtime"
status: complete
phase: P1
depends_on: [ANA-UW-02]
sources: [UW]
---

# UW — Runtime Behaviour (client-side)

### CON-UW-020 — Command grammar parsed into intents
- **What:** Input is a command if it starts with `/` or with `bug`, `task`, `close`, `board`, `standup`; on Enter: `bug|task "title" @assignee` → `CREATE_ISSUE{title, assignee, priority Medium}`; `close <n>` → `TRANSITION_ISSUE{id}`; `board|kanban` → `BOARD`; `standup` → `STANDUP` [UW:components/CommandBar.tsx].
- **Why (intent):** Keyboard-first operation; natural short phrases become structured requests (a precursor of NL → intent with AI).
- **Tags:** frontend, workflow, dsl

### CON-UW-021 — Search across functions and entities
- **What:** Non-command input filters features by name and issues by title or key; selecting a feature opens it in the workspace; selecting an issue emits `VIEW_ISSUE{context: Search}`; empty query lists all features ("All Available").
- **Tags:** search, frontend

### CON-UW-022 — Workflow transition with guardrail checklist
- **What:** `TRANSITION_ISSUE` pane shows "Guardrail Active: ensure unit tests pass before resolving" with a checklist (Run Unit Tests, Code Review Approval, QA Sign-off) before "Resolve & Close"; `VIEW_ISSUE` offers "Move to Done" and a comment stream.
- **Why (intent):** Transitions carry visible preconditions (static here) — the UX face of prerequisite/guard rules (compare CON-LS-027, CON-UB-032).
- **Tags:** workflow, rules, frontend

### CON-UW-023 — Stand-up view from live work state
- **What:** `STANDUP` pane lists issues Done and In Progress and a "Next Speaker" control — a meeting ritual generated from entity state.
- **Tags:** workflow, frontend
