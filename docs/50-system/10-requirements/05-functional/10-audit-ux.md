---
id: UBS-REQ-05-10
title: Functional Requirements — AUD (Audit, Lineage, Retention) and UX (User Experience)
status: draft
phase: PH-1
depends_on: [UBS-REQ-05, UBS-REQ-05-04]
---

# Functional Requirements — AUD and UX

## CAP-AUD-01 — Audit trail

### FR-AUD-011 — Business audit trail from commits
- **Statement:** For every change the platform MUST be able to report who (principal and on-behalf-of), what (object, fields, before and after), when (transaction time and valid time), why (message, reason code, change set, approvals), how (channel, client) and under which rules (rule binding). The source is the commit and process records, not a separate log.
- **Rationale:** An audit trail that cannot drift from the data (UBS-REQ-01 §1).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given any field change, when audited, then every listed element is available.
- **Verification:** CONF
- **Origin:** EXT-TRI, L40:SPEC-30

### FR-AUD-012 — Security audit log
- **Statement:** Security-relevant events (authentication, session, permission and policy changes, key operations, administrative actions, exports, break-glass access) MUST be recorded in a tamper-evident, hash-chained security log per tenant, plus a platform log.
- **Rationale:** SOC 2 and incident response.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** NOD, DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a removed log entry in storage, when the chain is verified, then the gap is detected.
- **Verification:** SEC, CONF
- **Origin:** NEW

### FR-AUD-013 — SIEM export
- **Statement:** Security and audit events MUST be streamable to external SIEM systems in a documented JSON schema aligned with OCSF, with at-least-once delivery.
- **Rationale:** Enterprise security operations.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** NOD, BRG
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a configured sink, when events occur, then they arrive within the NR-OBS bound.
- **Verification:** CONF
- **Origin:** IMP-12

### FR-AUD-014 — Audit data immutability
- **Statement:** Audit records MUST be immutable and retained according to retention policy (minimum defaults per compliance profile).
- **Rationale:** Evidence integrity.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, NOD
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given an administrator, when attempting to modify an audit record through any API, then no such operation exists.
- **Verification:** SEC, INSP
- **Origin:** NEW

## CAP-AUD-02 — Lineage

### FR-AUD-021 — Value lineage
- **Statement:** For any derived value, the platform MUST trace its producing process, logic and rule versions, and input object versions, recursively to a configurable depth.
- **Rationale:** Explain every number (SCN-103).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-FundAccountant, PER-InternalAuditor
- **Acceptance:**
  1. Given a NAV per share, when traced, then the valuation, prices, FX rates, accruals and their versions are returned.
- **Verification:** CONF, SCN
- **Origin:** L40:SPEC-15, IMP-09

### FR-AUD-022 — Lineage graph API
- **Statement:** Lineage MUST be retrievable as a graph (nodes are versions and processes, edges are `produced_by`, `depends_on` and `triggered_by`) with filters and depth limits.
- **Rationale:** Tools and agents consume lineage.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, SDK
- **Personas:** PER-DataAnalyst, PER-AiAgent
- **Acceptance:**
  1. Given a depth of 3, when requested, then the graph has no nodes beyond depth 3.
- **Verification:** CONF
- **Origin:** L40:SPEC-15

### FR-AUD-023 — Contribution analysis
- **Statement:** For numeric derived values the platform SHOULD rank input changes by their contribution to the change of the result between two versions.
- **Rationale:** "Which price moves explain the NAV move?" (SCN-103).
- **Priority:** Should · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given two NAVs, when contribution is requested, then the top contributors are listed with amounts.
- **Verification:** SCN
- **Origin:** NEW

## CAP-AUD-03 — Explanations

### FR-AUD-031 — Standard explanation object
- **Statement:** Denials, validation failures, guard failures, cascade results and computed values MUST return explanations in one standard structure: code, subject, rule reference and version, evaluated values, a message template and parameters.
- **Rationale:** Uniform, machine-readable reasoning for users and agents.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-BusinessUser, PER-AiAgent
- **Acceptance:**
  1. Given errors from four different sources, when compared, then all share the explanation schema.
- **Verification:** CONF
- **Origin:** L40:SPEC-02

### FR-AUD-032 — Localised rendering
- **Statement:** Explanation messages MUST render in the user's language from templates, and parameters MUST be formatted by locale.
- **Rationale:** Understandable to business users.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given a user with a German locale, when an explanation is shown, then it is in German with German number formats.
- **Verification:** CONF
- **Origin:** NEW

### FR-AUD-033 — Explanations do not leak
- **Statement:** Explanations MUST NOT reveal values, objects or rules the recipient may not see. They MUST substitute generic text instead.
- **Rationale:** Consistency with FR-IAM-043.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a denial by a confidential rule, when shown to a user, then the message says "not permitted by policy" without the rule's content.
- **Verification:** SEC
- **Origin:** NEW

## CAP-AUD-04 — Audit views and reports

### FR-AUD-041 — Standard audit reports
- **Statement:** The platform MUST provide the following audit reports:
  - changes by principal, class and period;
  - privileged and administrative actions;
  - SoD conflicts and overrides;
  - failed logins and denials;
  - retroactive changes;
  - agent activity.
- **Rationale:** Recurring control testing.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP, DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given a quarter, when each report is run, then results are produced and exportable.
- **Verification:** SCN
- **Origin:** NEW

### FR-AUD-042 — Auditor role
- **Statement:** A read-only auditor role MUST access history, lineage and audit reports across a VAE, subject to masking. It MUST be able to use historical-authorization mode (FR-VER-102).
- **Rationale:** Independent assurance access.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given the auditor role, when a write is attempted, then it is denied. When reading history, it is allowed.
- **Verification:** CONF, SEC
- **Origin:** NEW

## CAP-AUD-05 — Access logging

### FR-AUD-051 — Read logging for sensitive data
- **Statement:** Reads of classes or fields marked `log_reads` (default: `personal` and `sensitive_personal`) MUST be logged with principal, object, fields and purpose where provided.
- **Rationale:** HIPAA and privacy accountability.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given a view of a person's date of birth, when the access log is queried, then the entry exists.
- **Verification:** CONF
- **Origin:** NEW

### FR-AUD-052 — Export logging
- **Statement:** Every export, report download and evidence-pack creation MUST be logged with scope, principal and a content hash.
- **Rationale:** Data exfiltration accountability.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** NOD
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given an export, when logged, then the hash matches the exported file.
- **Verification:** CONF
- **Origin:** NEW

## CAP-AUD-06 — Legal hold

### FR-AUD-061 — Hold objects
- **Statement:** A legal hold MUST be an object with a matter reference, a scope (queries and object lists, including relationship expansion), custodians, a start time and a state.
- **Rationale:** SCN-207.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given a hold scoped to an agreement with relationship expansion, when evaluated, then its amendments, obligations and documents are included.
- **Verification:** CONF, SCN
- **Origin:** NEW

### FR-AUD-062 — Holds override disposal and erasure
- **Statement:** Objects in the scope of an active hold MUST be excluded from retention disposal, tiering deletion and privacy erasure, and every exclusion MUST be logged.
- **Rationale:** Preservation duties.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given SCN-207 and SCN-016 A1, when executed, then the outcomes match.
- **Verification:** SCN, AUDIT
- **Origin:** NEW

### FR-AUD-063 — Hold lifecycle
- **Statement:** Holds MUST support release with approval. After release, normal retention resumes from the original schedule.
- **Rationale:** Controlled end of preservation.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given a released hold, when disposal runs, then objects past retention are disposed of.
- **Verification:** CONF
- **Origin:** NEW

## CAP-AUD-07 — Retention and WORM

### FR-AUD-071 — Retention schedules
- **Statement:** Retention schedules MUST be declarable per class (and per sheet selector) with minimum and maximum retention, trigger events (for example agreement end) and disposal method.
- **Rationale:** Records management.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given "7 years after end", when an agreement ends, then its disposal date is computed and stored.
- **Verification:** CONF
- **Origin:** NEW

### FR-AUD-072 — Disposal with certificates
- **Statement:** Disposal MUST destroy the content (or its keys) of eligible versions. It MUST keep the hashes so that proofs over later states remain verifiable, and MUST issue a disposal certificate.
- **Rationale:** Lawful deletion with intact integrity.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given disposed versions, when a later root's inclusion proofs are verified, then they still succeed, and the disposed content reads as `disposed`.
- **Verification:** CONF, PROP
- **Origin:** NEW

### FR-AUD-073 — WORM mode for records
- **Statement:** Classes designated as regulatory records MUST be storable in WORM mode: non-rewriteable and non-erasable for the retention period, using storage with compliance-mode object lock or equivalent. The platform MUST verify the WORM configuration at start-up.
- **Rationale:** SEC Rule 17a-4(f).
- **Priority:** Must · **Phase:** PH-3 · **Systems:** NOD, DVM
- **Personas:** PER-ComplianceOfficer, PER-RegulatorExaminer
- **Acceptance:**
  1. Given a WORM record before its retention end, when deletion is attempted through any path, then the storage refuses it.
- **Verification:** AUDIT, SEC
- **Origin:** NEW

### FR-AUD-074 — Retention reporting
- **Statement:** The platform MUST report upcoming disposals, disposals performed, exclusions by hold and WORM coverage.
- **Rationale:** Evidence of records management.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given a quarter, when the report runs, then each category shows counts and details.
- **Verification:** SCN
- **Origin:** NEW

## CAP-UX-01 — Server-driven UI

### FR-UX-011 — Views are data
- **Statement:** User interfaces MUST be described by view objects: layouts, sections and widgets bound to fields, actions, queries and documents. Clients MUST render view payloads delivered by the server and MUST NOT embed business rules.
- **Rationale:** SDUI: UI changes without client releases (EXT-TRI, L40 hard rule).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, WSP, SDK
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given a view change merged, when a client refreshes, then the new layout appears without a client update.
- **Verification:** SCN, INSP
- **Origin:** EXT-TRI, L40:SPEC-25

### FR-UX-012 — Generated default views
- **Statement:** For every class and mode without a declared view, the platform MUST generate a usable view from the effective definition (field types, classifications, lifecycle, ports).
- **Rationale:** Instant usability for new classes (SCN-015).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, WSP
- **Personas:** PER-BusinessArchitect, PER-PersonalUser
- **Acceptance:**
  1. Given a new class with 10 fields, when opened, then create, edit, detail and list views are available.
- **Verification:** SCN
- **Origin:** L40:SPEC-25

### FR-UX-013 — Payloads respect permissions and state
- **Statement:** View payloads MUST contain only fields and actions the user may see and use in the object's current state, with read-only and required flags computed server-side.
- **Rationale:** Clients never decide authorization.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given an approver and a clerk, when they open the same invoice, then only the approver's payload contains `approve`.
- **Verification:** CONF, SEC
- **Origin:** EXT-TRI, L40:SPEC-25

### FR-UX-014 — Widget catalogue
- **Statement:** The set of widgets MUST be a versioned catalogue that every client implements. Views MUST reference only catalogue widgets. Buks MAY register new widgets as sandboxed web components with declared data access.
- **Rationale:** Consistent rendering across web, desktop and mobile.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP, SDK
- **Personas:** PER-BusinessArchitect, PER-IsvDeveloper
- **Acceptance:**
  1. Given a view referencing an unknown widget, when committed, then it fails with `UX.UNKNOWN_WIDGET`.
- **Verification:** CONF
- **Origin:** L40:SPEC-25

### FR-UX-015 — Context-aware rendering
- **Statement:** View resolution MUST consider role, device class, locale and VAE context, in addition to class and mode.
- **Rationale:** EXT-TRI "context-aware rendering".
- **Priority:** Should · **Phase:** PH-2 · **Systems:** DVM, WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given a mobile device, when a detail view is requested, then the mobile variant is chosen if one is declared.
- **Verification:** CONF
- **Origin:** EXT-TRI

## CAP-UX-02 — Workspace shell

### FR-UX-021 — Navigation
- **Statement:** Workspace MUST provide navigation built from the installed Buks' app definitions and the user's permissions, including favourites and recent items.
- **Rationale:** Findability.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given a user without contract permissions, when navigation loads, then no contract entries appear.
- **Verification:** SCN, USE
- **Origin:** L40:SPEC-26

### FR-UX-022 — Global search and command palette
- **Statement:** Workspace MUST provide global search across permitted records and a command palette for navigation and available actions.
- **Rationale:** Speed for frequent users.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given the command palette, when "approve" is typed on an invoice page, then the approve action is offered if it is available.
- **Verification:** USE
- **Origin:** NEW

### FR-UX-023 — Notification centre
- **Statement:** Workspace MUST show in-app notifications with read state, links to subjects, and preferences.
- **Rationale:** CAP-NTF-02 in-app channel.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given a new task, when assigned, then a notification appears within the NR-PERF bound.
- **Verification:** SCN
- **Origin:** NEW

## CAP-UX-03 — Forms, lists, details

### FR-UX-031 — Forms with live validation
- **Statement:** Forms MUST validate as the user types, using the server validation preview (FR-RULE-035), and MUST show explanations next to the affected fields.
- **Rationale:** Immediate feedback with server-side truth.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP, DVM
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given an invalid ISIN, when typed, then the field shows the explanation before submission.
- **Verification:** SCN, USE
- **Origin:** NEW

### FR-UX-032 — Lists
- **Statement:** Lists MUST support saved queries, filters, sorting, column selection, bulk actions (FR-FLOW-023), totals and export.
- **Rationale:** Core productivity.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given 20 selected rows, when a bulk action runs, then per-row results are shown.
- **Verification:** SCN, USE
- **Origin:** NEW

### FR-UX-033 — Detail pages
- **Statement:** Detail pages MUST show fields, related lists, available actions, a history tab, attachments and comments.
- **Rationale:** A 360° view of a BPU.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given an agreement, when opened, then obligations, amendments, documents and history are reachable in one page.
- **Verification:** SCN
- **Origin:** NEW

### FR-UX-034 — Draft autosave
- **Statement:** Unsaved form input MUST be preserved across reloads and crashes, locally and, for long forms, as server-side draft objects visible only to the author.
- **Rationale:** No lost work.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given a browser crash mid-form, when reopened, then the input is restored.
- **Verification:** SCN
- **Origin:** NEW

## CAP-UX-04 — Progressive disclosure

### FR-UX-041 — Vocabulary by layer
- **Statement:** User-interface text MUST map platform terms to layer vocabulary (UBS-REQ-01 §5). Business-layer users MUST NOT see the terms `branch`, `merge`, `commit`, `overlay` or `instruction`.
- **Rationale:** IMP-11.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP, STU
- **Personas:** PER-BusinessUser, PER-Approver
- **Acceptance:**
  1. Given the business-layer UI string catalogue, when scanned, then no forbidden term appears.
- **Verification:** INSP, USE
- **Origin:** IMP-11

### FR-UX-042 — Feature gating by layer
- **Statement:** Features MUST be gated by layer and permission. Builder and platform tools MUST NOT appear for business-layer users.
- **Rationale:** Simplicity.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given a business user, when the UI loads, then no Studio entries are present.
- **Verification:** SCN
- **Origin:** IMP-11

### FR-UX-043 — "Start a change" experience
- **Statement:** For classes with `change_set` policy, editing MUST open a guided change: name the change, edit, review the summary, submit. Branch mechanics MUST be hidden.
- **Rationale:** Governed change made friendly (SCN-101).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-FundOperationsManager
- **Acceptance:**
  1. Given SCN-101, when performed by a business user, then it completes without exposure to branch terms, and the usability task succeeds.
- **Verification:** SCN, USE
- **Origin:** IMP-11

## CAP-UX-05 — History and as-of viewer

### FR-UX-051 — Timeline
- **Statement:** Every record MUST have a history timeline of versions and transitions with actor, time, reason and a diff per step.
- **Rationale:** Self-service history (PER-BusinessUser goal).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser, PER-InternalAuditor
- **Acceptance:**
  1. Given a record with 12 versions, when the timeline opens, then 12 entries show with diffs.
- **Verification:** SCN, USE
- **Origin:** NEW

### FR-UX-052 — As-of browsing
- **Statement:** Users with permission MUST be able to set an "as of" date (valid time) and an "as known on" date (transaction time) for a page, and the whole page MUST render in that temporal context, with a clear banner.
- **Rationale:** Bitemporal questions without writing queries.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-InternalAuditor, PER-FundAccountant
- **Acceptance:**
  1. Given as-of 2026-10-02, when a fund page is viewed, then all figures are as of that date, and the banner shows both dates.
- **Verification:** SCN, USE
- **Origin:** IMP-02

### FR-UX-053 — Compare versions
- **Statement:** Users MUST be able to compare any two versions or any two dates of a record side by side.
- **Rationale:** Investigation.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given two versions, when compared, then the changed fields are highlighted.
- **Verification:** SCN
- **Origin:** NEW

## CAP-UX-06 — Change review

### FR-UX-061 — Diff review screen
- **Statement:** Approvers MUST review changes on a screen that shows the summary, the grouped field-level diffs, the simulation results, the validation outcome, comments, and approve, reject or partial-accept controls.
- **Rationale:** Informed approvals (SCN-012, SCN-301).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP, STU
- **Personas:** PER-Approver, PER-AgentSupervisor
- **Acceptance:**
  1. Given SCN-301, when reviewed, then the review median time is under 3 minutes in usability tests.
- **Verification:** USE, SCN
- **Origin:** IMP-10

### FR-UX-062 — Simulation presentation
- **Statement:** Simulation results MUST be presented as business-level differences (for example "14 breaches on 9 dates") with drill-down to items.
- **Rationale:** Decision support.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP, STU
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given SCN-106, when the change set is reviewed, then the simulation summary and drill-down are available.
- **Verification:** SCN
- **Origin:** NEW

### FR-UX-063 — Risk highlighting
- **Statement:** The review screen MUST highlight governed classes, retroactive changes, ledger impacts, masked fields changed and AI-generated items.
- **Rationale:** Focus attention on risk.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-Approver
- **Acceptance:**
  1. Given a retroactive price change in a change set, when reviewed, then it carries the "retroactive" badge.
- **Verification:** SCN
- **Origin:** NEW

## CAP-UX-07 — Desktop and mobile clients

### FR-UX-071 — Desktop application
- **Statement:** A desktop application (Tauri 2) MUST embed the Box node for personal use and MUST also act as a client of Server and Cell nodes, using the same renderer as the web.
- **Rationale:** Local-first desktop (UBS-REQ-01 §6).
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP, NOD
- **Personas:** PER-PersonalUser
- **Acceptance:**
  1. Given a fresh install, when first launched, then a local VAE is bootstrapped in under 2 minutes.
- **Verification:** SCN
- **Origin:** L40:SPEC-26

### FR-UX-072 — Mobile application
- **Statement:** Mobile applications (iOS and Android) MUST support tasks, approvals, records, documents, signatures and offline scopes, using the same view protocol.
- **Rationale:** Approvals and field work on the go.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP, NOD
- **Personas:** PER-Approver, PER-FieldWorker
- **Acceptance:**
  1. Given an approval task, when opened on mobile, then the diff review and approval complete with step-up authentication.
- **Verification:** SCN, USE
- **Origin:** L40:SPEC-26

### FR-UX-073 — One renderer contract
- **Statement:** Web, desktop and mobile clients MUST pass the same renderer conformance suite for the widget catalogue.
- **Rationale:** Consistency across clients.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP, SDK
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given the renderer suite, when run on the three clients, then all pass.
- **Verification:** CONF
- **Origin:** NEW

## CAP-UX-08 — Offline experience

### FR-UX-081 — Offline indicators and queued actions
- **Statement:** Offline-capable clients MUST show connectivity state and pending local commits, and MUST allow working on replicated scopes while offline.
- **Rationale:** SCN-401.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-FieldWorker
- **Acceptance:**
  1. Given offline mode, when 5 edits are made, then "5 pending" is shown.
- **Verification:** SCN, USE
- **Origin:** IMP-07

### FR-UX-082 — Conflict resolution screen
- **Statement:** Sync conflicts MUST be presented per object with both values and base, and the choices keep mine, keep theirs or edit. Resolutions MUST be attributed.
- **Rationale:** Understandable conflict handling.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-FieldWorker
- **Acceptance:**
  1. Given 2 conflicts, when resolved, then the sync completes, and the audit shows the resolutions.
- **Verification:** SCN, USE
- **Origin:** IMP-07

### FR-UX-083 — Sync status
- **Statement:** Clients MUST show last sync time, errors and progress, and MUST allow manual sync.
- **Rationale:** User confidence.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-FieldWorker
- **Acceptance:**
  1. Given a sync error, when shown, then the error has an explanation and a retry.
- **Verification:** SCN
- **Origin:** NEW

## CAP-UX-09 — External portal

### FR-UX-091 — External identities with narrow scopes
- **Statement:** External parties MUST authenticate with external principals limited to explicitly shared objects and actions. Internal navigation, search and data MUST be unreachable.
- **Rationale:** SCN-203.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP, NOD
- **Personas:** PER-ExternalParty
- **Acceptance:**
  1. Given an external user, when enumerating internal URIs, then all return not found.
- **Verification:** SEC, SCN
- **Origin:** NEW

### FR-UX-092 — Branded portal
- **Statement:** The external portal MUST apply tenant branding and MUST support e-mail-verified and SSO-federated external identities.
- **Rationale:** Professional external experience.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-ExternalParty
- **Acceptance:**
  1. Given tenant branding, when the portal loads, then the logo and colours apply.
- **Verification:** SCN
- **Origin:** NEW

### FR-UX-093 — Internal-only content separation
- **Statement:** Comments, fields and attachments marked internal MUST never be included in external views, exports or notifications.
- **Rationale:** SCN-203 postcondition.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP, DVM
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given an internal comment, when the external party exports the document, then the comment is absent.
- **Verification:** SEC
- **Origin:** NEW

## CAP-UX-10 — Theming and branding

### FR-UX-101 — Tenant branding
- **Statement:** Tenants MUST be able to configure logo, colours and typography within accessible bounds, as versioned configuration.
- **Rationale:** Customer identity.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-TenantAdministrator
- **Acceptance:**
  1. Given a colour that fails contrast requirements, when configured, then it is rejected.
- **Verification:** CONF
- **Origin:** NEW

### FR-UX-102 — Accessible themes
- **Statement:** Clients MUST provide light, dark and high-contrast themes that meet WCAG 2.2 AA contrast.
- **Rationale:** Accessibility (CR-WCAG).
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given the automated contrast checker, when run on all themes, then no violations remain.
- **Verification:** CONF, USE
- **Origin:** NEW
