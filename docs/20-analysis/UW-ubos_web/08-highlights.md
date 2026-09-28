---
id: ANA-UW-08
title: "UW Highlights"
status: complete
phase: P1
depends_on: [ANA-UW-01, ANA-UW-02, ANA-UW-03, ANA-UW-05]
sources: [UW]
---

# UW — Highlights

### HL-UW-001 — Intent-driven command bar
- **Summary:** One keyboard-first input for search (features + entities) and short commands parsed into typed intents.
- **Why it stands out:** A minimal, concrete grammar for "say what you want" UX that AI can later generate into.
- **Concepts:** CON-UW-012, CON-UW-020, CON-UW-021, CON-UW-050
- **Reuse recommendation:** adopt (align intents with platform actions; merge with UC/US omnibar in P2 topic 14)
- **Sources:** [UW:components/CommandBar.tsx]
- **Tags:** frontend, workflow

### HL-UW-002 — Context with tenant and branch as a first-class UX element
- **Summary:** Working context = project + tenant + branch + methodology + sprint, always visible and switchable.
- **Concepts:** CON-UW-011, CON-UW-051
- **Reuse recommendation:** adopt
- **Sources:** [UW:components/ContextSwitcher.tsx]
- **Tags:** context, multi-tenancy

### HL-UW-003 — Unified dock: inbox that becomes the task pane
- **Summary:** Issues, notifications and approvals (from people and bots) in one prioritized list; clicking or commanding opens the same task pane.
- **Concepts:** CON-UW-002, CON-UW-013, CON-UW-052
- **Reuse recommendation:** adopt (as the passive zone contract; compare CON-UC-052)
- **Sources:** [UW:components/TaskPanel.tsx]
- **Tags:** events, frontend

### HL-UW-004 — Guardrails visible on workflow transitions
- **Summary:** Transition pane shows preconditions as a checklist before resolving.
- **Concepts:** CON-UW-022
- **Reuse recommendation:** adapt (drive from rule/guard metadata)
- **Sources:** [UW:components/TaskPanel.tsx]
- **Tags:** workflow, rules

### HL-UW-005 — Work rituals generated from entity state (stand-up)
- **Concepts:** CON-UW-023
- **Reuse recommendation:** inspiration-only
- **Sources:** [UW:components/TaskPanel.tsx]
- **Tags:** workflow
