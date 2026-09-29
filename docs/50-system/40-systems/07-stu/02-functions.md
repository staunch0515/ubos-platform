---
id: UBS-SYS-STU-02
title: Studio — Design Requirements
status: draft
phase: PH-2
depends_on: [UBS-SYS-STU-01]
---

# Studio — Design Requirements (DSN-STU-*)

### DSN-STU-001 — Builder branch workflow
- **Statement:** Opening Studio on a VAE MUST place the builder on a personal work branch (created on demand from `main` or a chosen release), show the branch status against its base, and route all edits through commits on that branch.
- **Rationale:** CTR-011: no builder-only write paths.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** STU
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given a builder edit, when inspected, then it is a commit on the builder's branch with the builder as author.
- **Verification:** CONF
- **Origin:** FR-VER-021, FR-VER-022, CTR-011

### DSN-STU-002 — Model designer
- **Statement:** The model designer MUST edit classes, traits, fields, value types, relationships, extensions and lenses both visually (class diagram, field tables) and textually (YAML), with live diagnostics from the Forge service and effective-definition previews with provenance.
- **Rationale:** FR-MODEL-012, FR-MODEL-025, NR-USE-004.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** STU
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given a narrowing violation introduced visually, when saved, then the diagnostic appears on the field within 1 s.
- **Verification:** USE, CONF
- **Origin:** FR-MODEL-012, FR-MODEL-023, FR-MODEL-025, NR-USE-004

### DSN-STU-003 — Impact before commit
- **Statement:** Before committing a model change, Studio MUST show the impact report (DSN-DVM-108): classification, affected artefacts and instance counts, and whether a lens or migration is needed.
- **Rationale:** FR-MODEL-027, FR-MODEL-087.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** STU
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given a breaking change, when the builder tries to commit, then Studio requires a lens or migration choice.
- **Verification:** SCN
- **Origin:** FR-MODEL-027, FR-MODEL-087

### DSN-STU-004 — Sheet editor
- **Statement:** The sheet editor MUST offer a textual editor with the LSP (completion, hover with cascade explanation) and a structured editor for selectors and declarations, plus a live impact count (DSN-DVM-763).
- **Rationale:** FR-RULE-011, FR-RULE-074.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** STU
- **Personas:** PER-BusinessAnalyst
- **Acceptance:**
  1. Given a selector edit, when typed, then the count of affected objects updates within 2 s.
- **Verification:** BENCH, USE
- **Origin:** FR-RULE-011, FR-RULE-012, FR-RULE-074

### DSN-STU-005 — Decision table editor
- **Statement:** The decision table editor MUST support hit policies, typed input and output columns, gap and overlap highlighting with counter-examples (DSN-DVM-722), test cases per table, and DMN import/export.
- **Rationale:** FR-RULE-041, FR-RULE-042, FR-RULE-044.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** STU
- **Personas:** PER-BusinessAnalyst
- **Acceptance:**
  1. Given a gap, when detected, then the missing input combination is shown as a row suggestion.
- **Verification:** USE
- **Origin:** FR-RULE-041, FR-RULE-042, FR-RULE-044, FR-RULE-081

### DSN-STU-006 — Lifecycle and flow editor
- **Statement:** Lifecycles and workflows MUST be editable as state diagrams with guards, effects, approvals and timers, validated for reachability and dead states.
- **Rationale:** FR-FLOW-011, FR-FLOW-061.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** STU
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given an unreachable state, when validated, then it is flagged.
- **Verification:** CONF
- **Origin:** FR-FLOW-011, FR-FLOW-014, FR-FLOW-061

### DSN-STU-007 — View designer
- **Statement:** The view designer MUST edit view definitions with a live preview rendered by the WSP renderer against sample objects, for each device class.
- **Rationale:** FR-UX-011, FR-UX-015.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** STU
- **Personas:** PER-ImplementationConsultant
- **Acceptance:**
  1. Given a field moved in the designer, when previewed, then the preview matches the production rendering.
- **Verification:** SCN
- **Origin:** FR-UX-011, FR-UX-015

### DSN-STU-008 — Script editor
- **Statement:** Scripts MUST be edited in a Monaco-based editor connected to the Forge language server, with run-against-sample and test execution on the builder branch.
- **Rationale:** FR-DEV-071, NR-PERF-016.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** STU
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given a script, when run against a sample object, then the result and trace are shown.
- **Verification:** SCN
- **Origin:** FR-DEV-071, FR-LOGIC-041

### DSN-STU-009 — Change sets for builders
- **Statement:** Studio MUST open change sets from the builder branch, show the Forge check results, test results, simulation summaries and the review summary, and require green checks before submission to protected targets.
- **Rationale:** FR-VER-031, FR-RULE-082.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** STU
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given a failing rule test, when submitting, then submission is blocked with the failing test named.
- **Verification:** CONF
- **Origin:** FR-VER-031, FR-VER-033, FR-RULE-082

### DSN-STU-010 — Simulation workbench
- **Statement:** Studio MUST start simulations (IF-DVM-036) on selected history periods and populations with budgets, and present results (DSN-WSP-111), storing runs as evidence.
- **Rationale:** FR-RULE-071, FR-RULE-073.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** STU
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given a simulation, when finished, then the run record is linked to the change set.
- **Verification:** SCN
- **Origin:** FR-RULE-071, FR-RULE-072, FR-RULE-073

### DSN-STU-011 — Branches, tags and releases
- **Statement:** Studio MUST manage builder-relevant branches (create from any point, inheritance setup, upstream advance reports) and releases (tag, release notes, compatibility statement).
- **Rationale:** CAP-VER-02, CAP-VER-06, CAP-VER-08.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** STU
- **Personas:** PER-ImplementationConsultant
- **Acceptance:**
  1. Given an upstream template update, when advanced, then the override report is shown before confirming.
- **Verification:** SCN
- **Origin:** FR-VER-027, FR-VER-062, FR-VER-086

### DSN-STU-012 — Integrations management
- **Statement:** Studio MUST manage subscriptions, webhooks, connectors and mapping definitions, with connection tests executed through NOD.
- **Rationale:** CAP-EVT-02, CAP-INT-03.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** STU
- **Personas:** PER-TenantAdministrator
- **Acceptance:**
  1. Given a connector, when "test connection" runs, then the result is shown without exposing the secret.
- **Verification:** SEC
- **Origin:** FR-EVT-021, FR-INT-033, FR-INT-042

### DSN-STU-013 — Spreadsheet import wizard
- **Statement:** Studio MUST provide the wizard for spreadsheet imports: model inference preview (FR-MIG-011), mapping, row-level results (FR-MIG-012), and idempotent re-import (FR-MIG-013), executed by BRG on an import branch.
- **Rationale:** CAP-MIG-01.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** STU, BRG
- **Personas:** PER-ImplementationConsultant
- **Acceptance:**
  1. Given a 10,000-row fund register, when imported, then row results show created, updated and failed counts, and failures are downloadable.
- **Verification:** SCN
- **Origin:** FR-MIG-011, FR-MIG-012, FR-MIG-013

### DSN-STU-014 — AI drafting panel
- **Statement:** Studio MUST host an AI panel that asks AGT for drafts (classes, sheets, tests) from requirements, applies them as commits on an agent branch, and shows Forge findings for iterative fixing (FR-AI-071, FR-AI-072). Drafts MUST carry AI provenance (FR-AI-073).
- **Rationale:** FR-AI-071, FR-AI-072, FR-AI-073.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** STU, AGT
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given a requirement text, when drafted, then a branch with draft artefacts and a findings list is produced, and nothing reaches `main` without a change set.
- **Verification:** SCN
- **Origin:** FR-AI-071, FR-AI-072, FR-AI-073, FR-AI-062

### DSN-STU-015 — Buk packaging
- **Statement:** Studio MUST package a VAE scope as a Buk (manifest editor, dependency selection, seed selection) through the Forge service and show the install plan for target VAEs.
- **Rationale:** CAP-PKG-01, FR-PKG-033.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** STU, FRG
- **Personas:** PER-ImplementationConsultant
- **Acceptance:**
  1. Given a packaged Buk, when verified, then the verification record is shown in Studio.
- **Verification:** SCN
- **Origin:** FR-PKG-011, FR-PKG-033, FR-PKG-061

### DSN-STU-016 — Builder productivity target
- **Statement:** Studio MUST meet NR-USE-004 and NR-USE-005 for trained builders on the reference modelling tasks.
- **Rationale:** NR-USE-004, NR-USE-005.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** STU
- **Personas:** PER-BusinessAnalyst
- **Acceptance:**
  1. Given the reference tasks, when performed by study participants, then the targets are met.
- **Verification:** USE
- **Origin:** NR-USE-004, NR-USE-005
