---
id: ADR-003
title: "ADR-003: Client set"
status: complete
phase: P3
depends_on: [CMP-19, CMP-14, ADR-001]
sources: [UC, US, UP, UW, FU, UB]
---

# ADR-003: Client set

- **Status:** accepted (2026-09-28). The user replaced the IDE with mobile (Q-008).

## Context

- The corpus states "one core, five clients" (CON-UC-067, CON-US-064) and "one API, several client shells" (CON-UP-005). It never fixes the list.
- VRD-19-04 proposed web, desktop, CLI, IDE extension and SDK/agent, and asked for confirmation.
- VRD-14-03 designed developer-native access through a virtual file system in an IDE (CON-US-057, CON-UC-044).
- VRD-14-04: shells are clients of the public protocol only.

## Options considered

### Option A — web, desktop, CLI, IDE, SDK/agent
- **Pros:** strong developer story; IDE access is designed in the corpus.
- **Cons:** no presence on phones, although approvals, tasks and notifications are daily mobile work.

### Option B — web, desktop, mobile, CLI, SDK/agent; IDE as tooling
- **Pros:**
  - Covers end users on the move: approvals (VRD-10-04), the task dock and push notifications.
  - Developer IDE access remains available as tooling built on the SDK.
- **Cons:** one more UI platform to maintain.

## Decision

Option B. The five clients are:

| Client | Audience | Built on | Scope |
|---|---|---|---|
| Web | end users, builders, admins | React/TS shell + TS SDK | full cockpit and studio (SPEC-26) |
| Desktop | personal edition users, offline work | Tauri 2 + the web shell + embedded kernel | full cockpit and studio, local store |
| Mobile | end users | the web shell's reduced cockpit on a mobile target (Tauri 2 mobile or React Native) + TS SDK | dock/tasks, approvals, search/omnibar, entity views, notifications; no studio |
| CLI | operators, developers, CI | `ubos_cli` (Rust) + Rust SDK | verbs for entities, branches, packages, scenarios, admin |
| SDK / agent | programs and AI agents | Rust and TS SDKs (UBTP) | the full protocol; agents are principals (VRD-16-03) |

- IDE integration (the `ubos://` virtual file system, VRD-14-03) is **developer tooling** built on the SDK. It is specified in SPEC-26 as tooling and is not a client of record.
- Every client uses UBTP only (VRD-14-04). No client reads the database directly, including the desktop client: its embedded kernel is reached through the in-process UBTP binding.

## Consequences

- SPEC-26 specifies the mobile shell as a reduced cockpit and chooses the mobile technology.
- SPEC-24 includes push delivery for mobile notifications through subscriptions (VRD-11-04).
- SPEC-31 lists five clients; VRD-19-04 is resolved.

## Origin

Q-008, VRD-19-04, VRD-14-01, VRD-14-03, VRD-14-04, CON-UC-067, CON-US-064, CON-UP-005.
