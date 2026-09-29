---
id: UBS-SYS-WSP-02-01
title: Workspace — Shell, Views and Clients
status: draft
phase: PH-2
depends_on: [UBS-SYS-WSP-01]
---

# Workspace — Shell, Views and Clients (DSN-WSP-0xx…2xx)

| Range | Sub-area |
|---|---|
| DSN-WSP-0xx | renderer and widget catalogue |
| DSN-WSP-1xx | shell, forms, lists, details, history, review, tasks |
| DSN-WSP-2xx | desktop, mobile, offline, portal, theming, localisation, accessibility |

## Renderer and widget catalogue

### DSN-WSP-001 — Server-driven renderer
- **Statement:** WSP MUST render screens only from view payloads (IF-DVM-014) and MUST NOT contain business rules, field lists or permission logic. The renderer maps payload nodes to widgets from the catalogue, applies field states, and binds actions to SDK invocations.
- **Rationale:** FR-UX-011, FR-UX-013, AR-003.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given a new class added by a Buk, when a user opens it, then a usable screen appears without a WSP release.
- **Verification:** SCN, INSP
- **Origin:** FR-UX-011, FR-UX-012, FR-UX-013

### DSN-WSP-002 — Widget catalogue
- **Statement:** The widget catalogue (FR-UX-014) MUST be versioned; version 1.0 MUST cover text, number, decimal, money, date, instant, period, enum, reference picker, multi-reference, rich text, file, table (inline lines), address, party, status badge, timeline, chart and action bar. Unknown widgets MUST fall back to a generic renderer and be reported (CTR-010).
- **Rationale:** FR-UX-014.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-IsvDeveloper
- **Acceptance:**
  1. Given a payload with an unknown widget, when rendered, then a generic read-only rendering appears, and telemetry records the widget name.
- **Verification:** CONF
- **Origin:** FR-UX-014, CTR-010

### DSN-WSP-003 — Context-aware rendering
- **Statement:** The renderer MUST send device class, viewport and locale in view requests so the DVM can resolve the most specific view (FR-UX-015); the same payload MUST render on web, desktop and mobile (FR-UX-073).
- **Rationale:** FR-UX-015, FR-UX-073.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-FieldWorker
- **Acceptance:**
  1. Given a class with a mobile card view, when opened on a phone, then the card view is used.
- **Verification:** CONF
- **Origin:** FR-UX-015, FR-UX-073

### DSN-WSP-004 — Performance budget
- **Statement:** Detail and form screens MUST render interactive content within 1 s at p75 on a mid-range laptop over a 50 ms RTT link, given NR-PERF-014 payload latency. Lists MUST virtualise rows beyond 100.
- **Rationale:** NR-USE-001, NR-PERF-014.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given the reference fund detail page, when measured with the lab profile, then time-to-interactive is under 1 s at p75.
- **Verification:** BENCH
- **Origin:** NR-USE-001, NR-PERF-014

## Shell, forms, lists, details, history, review, tasks

### DSN-WSP-101 — Navigation
- **Statement:** The shell MUST provide navigation generated from installed Buks' navigation declarations, filtered by permissions, with recent items and favourites (FR-UX-021).
- **Rationale:** FR-UX-021.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given a user without access to contracts, when viewing navigation, then no contract entries appear.
- **Verification:** SEC, USE
- **Origin:** FR-UX-021

### DSN-WSP-102 — Global search and command palette
- **Statement:** The shell MUST offer global search across permitted objects (text search through NOD) and a command palette for actions and navigation (FR-UX-022), keyboard-first.
- **Rationale:** FR-UX-022, NR-ACC-002.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given the palette, when a user types "approve", then available approval actions for their tasks are offered.
- **Verification:** USE
- **Origin:** FR-UX-022, NR-ACC-002

### DSN-WSP-103 — Notification centre
- **Statement:** The shell MUST show in-app notifications from subscriptions (FR-UX-023) with read state and deep links, and a preferences screen (FR-NTF-031).
- **Rationale:** FR-UX-023, FR-NTF-031.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given a new task assigned, when it arrives, then the badge updates within NR-PERF-015.
- **Verification:** BENCH
- **Origin:** FR-UX-023, FR-NTF-031, FR-NTF-032

### DSN-WSP-104 — Forms with live validation
- **Statement:** Forms MUST validate on change through `resolve_view` with staged edits (DSN-DVM-921), showing all violations with plain-language messages (NR-USE-006) and field states updated by sheets (FR-UX-031).
- **Rationale:** FR-UX-031, FR-RULE-035.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given an amount above a limit, when typed, then the message appears before submit.
- **Verification:** USE, CONF
- **Origin:** FR-UX-031, FR-RULE-035, NR-USE-006

### DSN-WSP-105 — Draft autosave
- **Statement:** Unsaved form state MUST autosave locally (and optionally to a draft branch for long edits) and restore after reload (FR-UX-034).
- **Rationale:** FR-UX-034.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given a browser crash mid-edit, when reopened, then the draft is restored.
- **Verification:** SCN
- **Origin:** FR-UX-034

### DSN-WSP-106 — Lists
- **Statement:** Lists MUST be driven by saved queries (FR-QRY-061) with column selection, sorting, filtering (only on visible fields), grouping, bulk actions and export (FR-UX-032).
- **Rationale:** FR-UX-032, FR-QRY-062.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a saved list, when a bulk approve is run on 50 rows, then per-row outcomes are shown.
- **Verification:** SCN
- **Origin:** FR-UX-032, FR-QRY-062, FR-FLOW-023

### DSN-WSP-107 — Detail pages
- **Statement:** Detail pages MUST show the object, related objects, available actions with reasons (DSN-DVM-732), comments, attachments and history (FR-UX-033).
- **Rationale:** FR-UX-033, FR-FLOW-024.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given a disabled action, when hovered, then its reason is shown.
- **Verification:** USE
- **Origin:** FR-UX-033, FR-FLOW-024

### DSN-WSP-108 — Timeline and as-of browsing
- **Statement:** Every object MUST have a timeline view of valid-time segments and transaction history (FR-UX-051), and the whole workspace MUST support an as-of mode that pins all views to a chosen `(asof, time)` with a persistent banner (FR-UX-052).
- **Rationale:** FR-UX-051, FR-UX-052, CAP-UX-05.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given as-of mode set to last quarter-end, when navigating, then every screen shows quarter-end values and the banner.
- **Verification:** SCN, USE
- **Origin:** FR-UX-051, FR-UX-052

### DSN-WSP-109 — Compare versions
- **Statement:** Users MUST be able to compare any two versions or dates of an object with a semantic diff (FR-UX-053).
- **Rationale:** FR-UX-053, FR-VER-042.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given two versions, when compared, then changed fields are highlighted with old and new values.
- **Verification:** CONF
- **Origin:** FR-UX-053, FR-VER-042

### DSN-WSP-110 — Diff review screen
- **Statement:** The change review screen (FR-UX-061) MUST show the review summary (DSN-DVM-222), grouped semantic diffs, risk highlights (FR-UX-063), conflicts with resolution choices, comments per item, and approve or reject with step-up where required.
- **Rationale:** FR-UX-061, FR-UX-063, NR-USE-003.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-Approver
- **Acceptance:**
  1. Given a change set with 200 items, when reviewed by a trained approver, then review time meets NR-USE-003.
- **Verification:** USE
- **Origin:** FR-UX-061, FR-UX-063, NR-USE-003

### DSN-WSP-111 — Simulation presentation
- **Statement:** Simulation results (FR-UX-062) MUST be presented as before/after comparisons with counts, distributions and drill-down to individual changed outcomes.
- **Rationale:** FR-UX-062.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP, STU
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given a simulation with 37 changed outcomes, when shown, then each is reachable in two clicks.
- **Verification:** USE
- **Origin:** FR-UX-062

### DSN-WSP-112 — Tasks and inbox
- **Statement:** The inbox MUST show tasks with due dates, SLA state and claim, release and reassign actions (FR-FLOW-053, FR-FLOW-054), updated live.
- **Rationale:** CAP-FLOW-05, CAP-FLOW-08.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-Approver
- **Acceptance:**
  1. Given a task claimed by a colleague, when viewed, then it shows as claimed within NR-PERF-015.
- **Verification:** BENCH
- **Origin:** FR-FLOW-053, FR-FLOW-054, FR-FLOW-083

### DSN-WSP-113 — Explanations
- **Statement:** Any value, action availability or decision MUST offer "why?" that renders the DVM explanation object in plain language (FR-AUD-031, FR-AUD-032).
- **Rationale:** CAP-AUD-03.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given a fee value, when "why?" is opened, then the rule, inputs and version are shown.
- **Verification:** USE
- **Origin:** FR-AUD-031, FR-AUD-032, FR-RULE-022

### DSN-WSP-114 — Progressive disclosure by layer
- **Statement:** WSP MUST hide platform vocabulary (branch, commit, merge) from business users and show business terms ("draft change", "submit for approval") (FR-UX-041, NR-USE-007), gating builder features by role layer (FR-UX-042), with a guided "start a change" experience (FR-UX-043).
- **Rationale:** FR-UX-041, FR-UX-042, FR-UX-043, NR-USE-007.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given a business-user session, when the UI text is scanned, then no platform vocabulary term from the hidden list appears.
- **Verification:** INSP, USE
- **Origin:** FR-UX-041, FR-UX-042, FR-UX-043, NR-USE-007

### DSN-WSP-115 — Audit views and reports
- **Statement:** Auditors MUST have views for the business audit trail, lineage graphs (FR-AUD-022) and standard audit reports (FR-AUD-041) with export to evidence packs.
- **Rationale:** CAP-AUD-04.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given a NAV, when its lineage is opened, then the graph shows inputs back to prices and trades.
- **Verification:** SCN
- **Origin:** FR-AUD-011, FR-AUD-022, FR-AUD-041, FR-AUD-042

## Clients, offline, portal, theming, localisation, accessibility

### DSN-WSP-201 — Desktop application
- **Statement:** The desktop app MUST be a Tauri 2 shell with the web renderer and an embedded Box node, supporting Windows, macOS and Linux (FR-UX-071), with OS keychain integration.
- **Rationale:** FR-UX-071, NR-PORT-001.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP, NOD
- **Personas:** PER-PersonalUser
- **Acceptance:**
  1. Given the desktop app offline, when started, then the user works on local data.
- **Verification:** SCN
- **Origin:** FR-UX-071, NR-PORT-001

### DSN-WSP-202 — Mobile application
- **Statement:** The mobile app MUST use Tauri 2 mobile with the same renderer, mobile view variants, camera capture for attachments and biometric unlock (FR-UX-072).
- **Rationale:** FR-UX-072.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP, NOD
- **Personas:** PER-FieldWorker
- **Acceptance:**
  1. Given iOS and Android devices, when the field scenario suite runs, then it passes.
- **Verification:** SCN
- **Origin:** FR-UX-072

### DSN-WSP-203 — Offline indicators and queued actions
- **Statement:** When offline, WSP MUST show offline state, allow permitted local actions (committed to the device branch), list queued items and sync status (FR-UX-081, FR-UX-083).
- **Rationale:** FR-UX-081, FR-UX-083, CAP-UX-08.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-FieldWorker
- **Acceptance:**
  1. Given 10 offline edits, when reconnected, then the sync status shows progress to completion.
- **Verification:** SCN
- **Origin:** FR-UX-081, FR-UX-083

### DSN-WSP-204 — Conflict resolution screen
- **Statement:** Rejections and conflicts returned by sync (DSN-DVM-953) MUST be presented with the server value, the local value, the reason and resolution options (FR-UX-082).
- **Rationale:** FR-UX-082, CAP-SYNC-03.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-FieldWorker
- **Acceptance:**
  1. Given a rejected offline edit, when viewed, then the user can re-apply, discard or edit it.
- **Verification:** USE
- **Origin:** FR-UX-082, FR-SYNC-033

### DSN-WSP-205 — External portal
- **Statement:** The portal MUST serve external identities with narrow scopes (FR-UX-091) under tenant branding (FR-UX-092), showing only content marked external (FR-UX-093).
- **Rationale:** FR-UX-091, FR-UX-092, FR-UX-093.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-ExternalParty
- **Acceptance:**
  1. Given an investor in the portal, when viewing a document, then internal comments are absent.
- **Verification:** SEC
- **Origin:** FR-UX-091, FR-UX-092, FR-UX-093

### DSN-WSP-206 — Theming and branding
- **Statement:** Tenants MUST be able to brand the workspace and portal (logo, colours, typography within accessible contrast) (FR-UX-101, FR-UX-102).
- **Rationale:** FR-UX-101, FR-UX-102.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-TenantAdministrator
- **Acceptance:**
  1. Given a colour choice with contrast below AA, when saved, then it is rejected with a suggestion.
- **Verification:** CONF
- **Origin:** FR-UX-101, FR-UX-102

### DSN-WSP-207 — Localisation
- **Statement:** All UI strings MUST be externalised (NR-LOC-002); model labels and content fields MUST be translatable (FR-LOC-011, FR-LOC-012); layouts MUST support right-to-left (FR-LOC-013); display formats MUST follow the user locale while storage stays canonical (FR-LOC-051, FR-LOC-052); instants MUST display in the user's or the business time zone with the zone shown (FR-LOC-031).
- **Rationale:** CAP-LOC-01, CAP-LOC-05.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given the Arabic locale, when rendering a form, then layout is mirrored, and numbers are formatted per locale.
- **Verification:** CONF, USE
- **Origin:** FR-LOC-011, FR-LOC-012, FR-LOC-013, FR-LOC-031, FR-LOC-051, FR-LOC-052, NR-LOC-001, NR-LOC-002

### DSN-WSP-208 — Accessibility
- **Statement:** All surfaces MUST meet WCAG 2.2 AA (NR-ACC-001), be fully keyboard operable (NR-ACC-002), and support screen readers on each platform (NR-ACC-003). Every widget MUST have automated axe checks and manual audits per release.
- **Rationale:** NR-ACC-001, NR-ACC-002, NR-ACC-003.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given the release candidate, when audited by an external accessibility auditor, then no AA failures remain open.
- **Verification:** INSP, USE
- **Origin:** NR-ACC-001, NR-ACC-002, NR-ACC-003

### DSN-WSP-209 — Dashboards
- **Statement:** Dashboards (FR-ANL-061) MUST render saved queries and projections as charts with live updates (FR-ANL-062) and drill-down to records (FR-ANL-063).
- **Rationale:** CAP-ANL-06.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-Executive
- **Acceptance:**
  1. Given an AUM dashboard, when a trade commits, then the chart updates within NR-PERF-015.
- **Verification:** BENCH
- **Origin:** FR-ANL-061, FR-ANL-062, FR-ANL-063

### DSN-WSP-210 — Browser-local kernel mode
- **Statement:** From PH-5, WSP MAY run the WASM DVM (DSN-DVM-956) for local reads of synced scopes, falling back to the server transparently.
- **Rationale:** NR-PERF-024.
- **Priority:** Could · **Phase:** PH-5 · **Systems:** WSP
- **Personas:** PER-PersonalUser
- **Acceptance:**
  1. Given local mode, when browsing synced objects, then reads meet NR-PERF-024.
- **Verification:** BENCH
- **Origin:** FR-SYNC-081, NR-PERF-024
