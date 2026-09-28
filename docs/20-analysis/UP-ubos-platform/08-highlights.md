---
id: ANA-UP-08
title: "UP Highlights"
status: complete
phase: P1
depends_on: [ANA-UP-01, ANA-UP-02, ANA-UP-03, ANA-UP-04, ANA-UP-05, ANA-UP-06]
sources: [UP]
---

# UP — Highlights

Ranked by expected value for the target platform.

### HL-UP-001 — Minimal "Git for Data" core
- **Summary:** Three tables (identity registry, commit chain with parent pointer and full snapshot, branch heads) give every platform artifact history, branches and time travel.
- **Why it stands out:** Typical business platforms bolt audit tables onto mutable rows; here immutability and branching are the storage model itself, and it is small enough to reason about completely.
- **Concepts:** CON-UP-010, CON-UP-011, CON-UP-012, CON-UP-040, CON-UP-044
- **Reuse recommendation:** adopt as-is (as the conceptual baseline; compare physical details in P2 topic 1)
- **Sources:** [UP:ubos-server/src/main/resources/db/migration/V1__init_ubos_schema.sql]
- **Tags:** versioning, persistence

### HL-UP-002 — Branch inheritance as overlay ("family tree")
- **Summary:** Branches have parents; reads fall back up the tree, so a child branch (region, tenant, environment) stores only its overrides.
- **Why it stands out:** Gives multi-tenant / multi-region customization of *logic, UI and data* with zero duplication and full history — a rare and powerful mechanism.
- **Concepts:** CON-UP-041, CON-UP-051, CON-UP-052
- **Reuse recommendation:** adopt (define precise semantics for writes, deletes/tombstones and search in the spec)
- **Sources:** [UP:ubos-server/src/main/resources/db/migration/V3__add_branch_config.sql], [UP:ubos-server/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L54-L89]
- **Tags:** versioning, multi-tenancy

### HL-UP-003 — Process = auditable unit of business change
- **Summary:** A named process groups all commits of one business operation; the audit API/console replays it as a timeline with snapshots.
- **Why it stands out:** Audit at the level of *business meaning* ("Employee Onboarding by sys_admin") instead of rows.
- **Concepts:** CON-UP-017, CON-UP-028, CON-UP-056
- **Reuse recommendation:** adopt
- **Sources:** [UP:ubos-server/src/main/resources/db/migration/V4__add_process_log.sql], [UP:commit_onboard.json]
- **Tags:** process, observability

### HL-UP-004 — Everything is a versioned entity (logic, views, schedules, listeners, policies, users, types)
- **Summary:** One model and one API cover all platform artifacts.
- **Why it stands out:** Configuration, code and data share lifecycle, branching, audit and deployment ("deploy = commit").
- **Concepts:** CON-UP-013, CON-UP-016, CON-UP-060
- **Reuse recommendation:** adopt
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/server/bootstrap/Genesis.java]
- **Tags:** entity, metadata

### HL-UP-005 — Events and cron jobs declared as data
- **Summary:** `LISTENER {trigger}` and `CRON {cron, target, params}` entities wire logic to events and time.
- **Why it stands out:** Reactive and scheduled behavior become versioned, branchable configuration.
- **Concepts:** CON-UP-025, CON-UP-026
- **Reuse recommendation:** adopt
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/engine/service/LcmEventService.java], [UP:ubos-server/src/main/java/org/logrum/ubos/engine/service/LcmSchedulerService.java]
- **Tags:** event

### HL-UP-006 — "Immune system" for runtime-authored logic
- **Summary:** Compile-time AST restrictions + interrupt injection + hard timeout with cancellation.
- **Why it stands out:** Treats stored logic (including AI-written logic) as untrusted by default.
- **Concepts:** CON-UP-022, CON-UP-023
- **Reuse recommendation:** adapt (principle adopted; mechanism depends on chosen script engine — compare with Rhai limits in P2 topic 5)
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/engine/config/ScriptSecurityConfig.java]
- **Tags:** scripting, permission

### HL-UP-007 — Time-travel execution
- **Summary:** Run any logic exactly as of a given commit.
- **Why it stands out:** Reproducibility and debugging of historical behavior, not just historical data.
- **Concepts:** CON-UP-024
- **Reuse recommendation:** adopt
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/server/controller/RunController.java]
- **Tags:** versioning, scripting

### HL-UP-008 — Identity → branch routing through policy entities
- **Summary:** A user's policy entity selects the branch (world) they operate in; the server echoes the resolved branch.
- **Why it stands out:** Couples authorization/tenancy with the versioning model in a data-driven way.
- **Concepts:** CON-UP-018, CON-UP-051, CON-UP-052
- **Reuse recommendation:** adapt
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/server/config/AuthWebFilter.java]
- **Tags:** auth, multi-tenancy

### HL-UP-009 — Web OS shell with apps as data
- **Summary:** Desktop, icons and windows are defined by a VIEW entity; each app carries its own UI schema rendered by a component registry; buttons bind to logic slugs.
- **Why it stands out:** A complete "install app = commit JSON" loop from kernel to pixels.
- **Concepts:** CON-UP-053, CON-UP-054, CON-UP-055
- **Reuse recommendation:** inspiration-only (compare shells in P2 topic 14)
- **Sources:** [UP:ubos-shell/src/]
- **Tags:** frontend, desktop, ui-rendering

### HL-UP-010 — AI Architect over a small kernel API
- **Summary:** An LLM converts requirements into logic commits using the same kernel API scripts use.
- **Why it stands out:** The architecture (small API + logic as data + sandbox + branches) is what makes AI-built business logic safe and reviewable.
- **Concepts:** CON-UP-061, CON-UP-062
- **Reuse recommendation:** adapt
- **Sources:** [UP:ubos-copilot/app.py]
- **Tags:** ai, copilot, codegen

### HL-UP-011 — Genesis through the kernel API, with SAFE/RESET/UPDATE strategies
- **Summary:** The initial world is committed like any other data; boot strategy is configuration; reset is an operation.
- **Why it stands out:** No special seed path; genesis has history.
- **Concepts:** CON-UP-027
- **Reuse recommendation:** adopt
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/server/bootstrap/SystemBootstrapper.java]
- **Tags:** boot

### HL-UP-012 — Backward-compatible, versioned script API
- **Summary:** Host API exposed to stored scripts is versioned (`kernel.v1`, `kernel.v2`) and overloads are kept for old scripts.
- **Why it stands out:** Recognizes that logic stored as data outlives the host code.
- **Concepts:** CON-UP-029
- **Reuse recommendation:** adapt
- **Sources:** commit `17a4d0d`
- **Tags:** api, scripting, versioning
