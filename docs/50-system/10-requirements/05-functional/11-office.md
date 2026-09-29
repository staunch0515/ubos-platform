---
id: UBS-REQ-05-11
title: Functional Requirements — OFFICE (Live Doc, Smart Grid, War Room, Action Messages, Executable Books)
status: complete
phase: PH-2
depends_on: [UBS-REQ-05, UBS-REQ-05-10]
---

# Functional Requirements — OFFICE

The Office suite replaces static files with views over BPUs (EXT-WP §6, EXT-TRI Book III
chapter 2). Documents, grids and decks are views and logic bound to versioned data.

## CAP-OFFICE-01 — Live Doc

### FR-OFFICE-011 — Documents as bound views
- **Statement:** A Live Doc MUST be a view object that combines rich text with bindings to object fields (`{{path}}`), embedded logic blocks and conditional sections. Rendering MUST resolve bindings at the requested `asof` and `time`.
- **Rationale:** Documents that never go stale (SCN-201).
- **Priority:** Must · **Phase:** PH-5 · **Systems:** WSP, DVM
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given SCN-201 step 2, when the party address changes, then the next render shows the new address. A render `asof` before the change shows the old one.
- **Verification:** SCN, CONF
- **Origin:** EXT-TRI, EXT-WP
- **Phase note:** moved to PH-5 by DEC-022 (contract or fund scope deferred).

### FR-OFFICE-012 — Embedded logic blocks
- **Statement:** Live Docs MUST support embedded logic blocks (L1 expressions or references to logic assets) that compute values at render time. Each rendered value MUST be traceable to its logic version.
- **Rationale:** Self-calculating documents (for example late fees).
- **Priority:** Must · **Phase:** PH-5 · **Systems:** WSP, DVM
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given a `late_fee` block, when rendered on two dates, then the values differ by the rule, and each value's lineage names the logic version.
- **Verification:** CONF
- **Origin:** EXT-TRI
- **Phase note:** moved to PH-5 by DEC-022 (contract or fund scope deferred).

### FR-OFFICE-013 — Deterministic rendering
- **Statement:** Rendering the same document version with the same data commit, temporal parameters and locale MUST produce byte-identical output for each output format. The output MUST carry the document state hash and the data commit ID.
- **Rationale:** Signatures and seals bind to exact renderings (SCN-204).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP, DVM
- **Personas:** PER-ContractManager, PER-RegulatorExaminer
- **Acceptance:**
  1. Given two renders with identical inputs on different nodes, when compared, then the hashes match.
- **Verification:** CONF, PROP
- **Origin:** NEW

### FR-OFFICE-014 — Collaborative editing of drafts
- **Statement:** Several users MUST be able to edit the text of a draft Live Doc concurrently with real-time merging. The session state MUST be checkpointed into commits at least every configured interval and on session end.
- **Rationale:** Modern co-authoring without losing versioned history.
- **Priority:** Should · **Phase:** PH-5 · **Systems:** WSP, DVM
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given two editors in one paragraph, when both type, then no keystroke is lost, and the checkpoint commit attributes both authors.
- **Verification:** SCN, SIM
- **Origin:** NEW
- **Phase note:** moved to PH-5 by DEC-022 (contract or fund scope deferred).

### FR-OFFICE-015 — Binding safety
- **Statement:** Bindings MUST be resolved with the viewer's permissions. Masked or invisible values MUST render as placeholders, and export or signing MUST be blocked when required bindings are not visible to the actor.
- **Rationale:** SCN-201 A1.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** WSP, DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a hidden binding, when the user exports, then the export is refused with an explanation.
- **Verification:** SEC
- **Origin:** NEW
- **Phase note:** moved to PH-5 by DEC-022 (contract or fund scope deferred).

## CAP-OFFICE-02 — Templates and clauses

### FR-OFFICE-021 — Templates and clause library
- **Statement:** The platform MUST support document templates composed of clauses from a versioned clause library. Clause variants are selected by decision tables or expressions.
- **Rationale:** Standardised contracting (SCN-201, SCN-208).
- **Priority:** Must · **Phase:** PH-5 · **Systems:** WSP, DVM
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given a contract value of 2.4M, when the template is instantiated, then the "2x fees" liability variant is selected.
- **Verification:** SCN
- **Origin:** NEW
- **Phase note:** moved to PH-5 by DEC-022 (contract or fund scope deferred).

### FR-OFFICE-022 — Clause references are versioned
- **Statement:** A document MUST reference clause versions explicitly. Executed documents MUST keep their clause versions permanently.
- **Rationale:** SCN-208 postcondition.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** DVM
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given a new clause version, when an executed agreement is rendered, then the old clause text appears.
- **Verification:** CONF
- **Origin:** NEW
- **Phase note:** moved to PH-5 by DEC-022 (contract or fund scope deferred).

### FR-OFFICE-023 — Conditional sections
- **Statement:** Templates MUST support sections shown or hidden by L1 conditions over bound data.
- **Rationale:** One template for many cases.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** WSP
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given "show the data-protection annex if personal data is processed", when the flag is false, then the annex is absent.
- **Verification:** CONF
- **Origin:** NEW
- **Phase note:** moved to PH-5 by DEC-022 (contract or fund scope deferred).

### FR-OFFICE-024 — Clause impact analysis
- **Statement:** For a clause change, the platform MUST list the templates and documents that reference the clause, grouped by lifecycle state (SCN-208).
- **Rationale:** Controlled legal change.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** DVM, STU
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given SCN-208, when analysed, then the counts match.
- **Verification:** SCN
- **Origin:** NEW
- **Phase note:** moved to PH-5 by DEC-022 (contract or fund scope deferred).

## CAP-OFFICE-03 — Redlining and negotiation

### FR-OFFICE-031 — Negotiation rounds
- **Statement:** Sharing a document with external parties MUST create negotiation rounds. Each round is recorded as a ledger entry with the shared version, the proposed changes and the author party.
- **Rationale:** SCN-203.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** WSP, DVM
- **Personas:** PER-ContractManager, PER-ExternalParty
- **Acceptance:**
  1. Given two rounds, when history is shown, then each round has author, time and diff.
- **Verification:** SCN
- **Origin:** NEW
- **Phase note:** moved to PH-5 by DEC-022 (contract or fund scope deferred).

### FR-OFFICE-032 — Tracked changes by party
- **Statement:** Proposed text changes MUST be shown as tracked changes attributed to the proposing party, with accept, reject and counter operations.
- **Rationale:** Familiar redlining.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** WSP
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given a counterparty proposal, when accepted, then the change enters the draft attributed to the counterparty.
- **Verification:** SCN, USE
- **Origin:** NEW
- **Phase note:** moved to PH-5 by DEC-022 (contract or fund scope deferred).

### FR-OFFICE-033 — Non-negotiable clauses
- **Statement:** Clauses MAY be marked non-negotiable, in which case external edits to them MUST be refused.
- **Rationale:** SCN-203 A1.
- **Priority:** Should · **Phase:** PH-5 · **Systems:** WSP
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given a non-negotiable clause, when the counterparty edits it, then the edit is refused.
- **Verification:** CONF
- **Origin:** NEW
- **Phase note:** moved to PH-5 by DEC-022 (contract or fund scope deferred).

### FR-OFFICE-034 — Internal and external comments
- **Statement:** Comments MUST be marked internal or external, and internal comments MUST never reach external parties (FR-UX-093).
- **Rationale:** Confidential negotiation strategy.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** WSP
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given an internal comment, when the external view renders, then it is absent.
- **Verification:** SEC
- **Origin:** NEW
- **Phase note:** moved to PH-5 by DEC-022 (contract or fund scope deferred).

## CAP-OFFICE-04 — Smart Grid

### FR-OFFICE-041 — Grid over records
- **Statement:** A Smart Grid MUST present the result of a saved query as a spreadsheet: one row per object and one column per field, computed column or aggregate. Cell edits are changes to the underlying objects.
- **Rationale:** An Excel replacement with a real data model (EXT-TRI).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP, DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a grid of fee accruals, when a cell is edited, then the underlying object changes through normal validation.
- **Verification:** SCN
- **Origin:** EXT-TRI, EXT-WP
- **Phase note:** moved to PH-2 by DEC-025 (Smart Grid for batch journal entry).

### FR-OFFICE-042 — Computed columns from shared logic
- **Statement:** Computed columns MUST be backed by L1 expressions or logic assets shared across grids. Cell-local formulas MUST NOT exist.
- **Rationale:** "Fix the formula in one place" (EXT-TRI).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP, DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given SCN-105 A1, when a user tries to edit a column formula, then the grid opens the shared logic as a change set instead.
- **Verification:** SCN
- **Origin:** EXT-TRI
- **Phase note:** moved to PH-2 by DEC-025 (Smart Grid for batch journal entry).

### FR-OFFICE-043 — Validated edits and bulk paste
- **Statement:** Grid edits, including bulk paste, MUST be validated per cell and per row with explanations. The user commits them in one process or discards them.
- **Rationale:** Spreadsheet speed with data integrity.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP, DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given 500 pasted rows with 3 invalid cells, when committed, then the invalid cells are marked, and nothing commits until they are fixed or excluded.
- **Verification:** SCN
- **Origin:** NEW
- **Phase note:** moved to PH-2 by DEC-025 (Smart Grid for batch journal entry).

### FR-OFFICE-044 — Overrides as adjustments
- **Statement:** Where a computed value on a Ledger-based grid needs correction, the grid MUST create an adjustment entry with a reason. It MUST NOT overwrite the computed value.
- **Rationale:** SCN-105 step 3.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP, DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given an override, when saved, then an adjustment entry exists, and the logic is unchanged.
- **Verification:** SCN
- **Origin:** IMP-01
- **Phase note:** moved to PH-2 by DEC-025 (Smart Grid for batch journal entry).

### FR-OFFICE-045 — Summaries and pivots
- **Statement:** Grids MUST support group subtotals and pivot views, computed by server aggregation (CAP-QRY-04).
- **Rationale:** Analysis in place.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a pivot by fund and month, when displayed, then the totals equal the query aggregates.
- **Verification:** CONF
- **Origin:** NEW
- **Phase note:** moved to PH-2 by DEC-025 (Smart Grid for batch journal entry).

### FR-OFFICE-046 — XLSX interchange
- **Statement:** Grids MUST export to XLSX with values, and optionally with a read-only formula sheet documenting the logic. They MUST import XLSX through the migration mapping (CAP-MIG-01).
- **Rationale:** Coexistence with spreadsheets.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP, BRG
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given an export, when opened in a spreadsheet tool, then the values and headers match the grid.
- **Verification:** CONF
- **Origin:** NEW
- **Phase note:** moved to PH-2 by DEC-025 (Smart Grid for batch journal entry).

## CAP-OFFICE-05 — War Room

### FR-OFFICE-051 — Live decks
- **Statement:** A War Room deck MUST be a sequence of slides whose content is bound to dashboards, queries and records, and rendered from live data at view time.
- **Rationale:** A PPT replacement (EXT-TRI).
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-Executive
- **Acceptance:**
  1. Given a new commit affecting a chart, when the slide refreshes, then the chart updates.
- **Verification:** SCN
- **Origin:** EXT-TRI

### FR-OFFICE-052 — Drill-down
- **Statement:** Every chart and figure on a slide MUST drill down to the underlying records, within the viewer's permissions.
- **Rationale:** "Click the dip, see the transactions."
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-Executive
- **Acceptance:**
  1. Given a revenue chart, when a bar is clicked, then the contributing records are listed.
- **Verification:** SCN, USE
- **Origin:** EXT-TRI

### FR-OFFICE-053 — Actions on slides
- **Statement:** Slides MAY embed actions (ports) that run with the viewer's identity and all governance applied.
- **Rationale:** "Approve variance" from the slide.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP, DVM
- **Personas:** PER-Executive, PER-Approver
- **Acceptance:**
  1. Given an embedded approve action, when invoked, then the normal approval flow applies.
- **Verification:** SCN
- **Origin:** EXT-TRI

### FR-OFFICE-054 — Snapshot export
- **Statement:** A deck MUST be exportable as PDF or PPTX pinned to a data commit and temporal context, with the pin printed on each page.
- **Rationale:** Board packs must be reproducible.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-Executive
- **Acceptance:**
  1. Given an export, when re-rendered with the pinned commit, then the numbers are identical.
- **Verification:** CONF
- **Origin:** NEW

## CAP-OFFICE-06 — Action Messages

### FR-OFFICE-061 — Messages carrying actions
- **Statement:** The platform MUST send messages (in-app, e-mail, chat) that carry executable actions bound to a specific object version, with a signed, single-use, expiring action token.
- **Rationale:** "Communication is a transaction" (EXT-WP §6).
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP, NOD
- **Personas:** PER-Approver
- **Acceptance:**
  1. Given an approval message, when the link is used twice, then the second use fails with `OFFICE.TOKEN_USED`.
- **Verification:** SEC, SCN
- **Origin:** EXT-WP

### FR-OFFICE-062 — Authentication and policy on execution
- **Statement:** Executing a message action MUST require authentication (step-up when policy demands) and MUST apply all decisions, guards and approvals as if the action were invoked in Workspace.
- **Rationale:** No weaker side channel.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP, DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a forwarded message, when a different user uses the link, then the action is denied.
- **Verification:** SEC
- **Origin:** NEW

### FR-OFFICE-063 — Version-bound actions
- **Statement:** If the subject object changed after the message was sent, executing the action MUST show the diff and require confirmation, or fail when policy says so.
- **Rationale:** Approve what you saw.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP, DVM
- **Personas:** PER-Approver
- **Acceptance:**
  1. Given an edited invoice after the message, when approve is clicked, then the diff is shown before confirmation.
- **Verification:** SCN
- **Origin:** NEW

### FR-OFFICE-064 — Threads tied to objects
- **Statement:** Messages and replies about an object MUST be stored as a thread linked to the object and visible in its detail page.
- **Rationale:** Context stays with the record.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** WSP, DVM
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given an e-mail reply to an Action Message, when received, then it appears in the object's thread.
- **Verification:** SCN
- **Origin:** NEW

## CAP-OFFICE-07 — Executable Books

### FR-OFFICE-071 — Book objects
- **Statement:** An Executable Book MUST combine narrative (rich text), live logic blocks, views and sample data in one versioned object, readable as a document and runnable as an application.
- **Rationale:** Knowledge packaged as capability (EXT-TRI "Buk").
- **Priority:** Should · **Phase:** PH-4 · **Systems:** WSP, STU
- **Personas:** PER-IsvDeveloper, PER-BusinessArchitect
- **Acceptance:**
  1. Given a "loan amortisation" book, when a reader changes inputs, then the schedule recomputes in place.
- **Verification:** SCN
- **Origin:** EXT-TRI, EXT-WP

### FR-OFFICE-072 — Books ship as Buks
- **Statement:** Books MUST be packaged, versioned, verified and licensed through the Buk mechanisms (CAP-PKG-01, CAP-PKG-08).
- **Rationale:** One distribution path.
- **Priority:** Should · **Phase:** PH-4 · **Systems:** FRG, EXC
- **Personas:** PER-IsvDeveloper
- **Acceptance:**
  1. Given a book, when published, then it passes Buk verification and appears in the Exchange.
- **Verification:** SCN
- **Origin:** EXT-TRI

### FR-OFFICE-073 — Sandboxed execution in the reader's VAE
- **Statement:** A book's logic MUST run in the reader's VAE sandbox with only the capabilities granted at installation. Books MUST NOT access reader data outside declared bindings.
- **Rationale:** Safe consumption of third-party knowledge.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a book declaring no data access, when it attempts a query, then the query is denied.
- **Verification:** SEC
- **Origin:** NEW

## CAP-OFFICE-08 — Rendering and export

### FR-OFFICE-081 — Archival PDF with metadata
- **Statement:** Documents MUST render to PDF/A-2 or later, with embedded metadata: document ID, version, data commit, temporal parameters, state hash and signature references.
- **Rationale:** Long-term, verifiable records.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-ContractManager, PER-RegulatorExaminer
- **Acceptance:**
  1. Given a rendered PDF, when validated, then it conforms to PDF/A, and the metadata is present.
- **Verification:** CONF
- **Origin:** NEW

### FR-OFFICE-082 — Office format export
- **Statement:** Documents MUST export to DOCX and grids to XLSX, with a footer or property that records the source version and hash.
- **Rationale:** Interoperability with external parties.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given a DOCX export, when opened, then the text matches the render, and the properties include the hash.
- **Verification:** CONF
- **Origin:** NEW

### FR-OFFICE-083 — Print layouts
- **Statement:** Views MUST support print layouts with page headers, footers, page numbering and temporal stamps.
- **Rationale:** Formal outputs.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given a report, when printed, then each page shows "as of" and "as known on".
- **Verification:** SCN
- **Origin:** NEW

### FR-OFFICE-084 — Accessible documents
- **Statement:** PDF output MUST be tagged for accessibility (PDF/UA) when the template declares accessible structure.
- **Rationale:** Section 508 (CR-WCAG).
- **Priority:** Should · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-ExternalParty
- **Acceptance:**
  1. Given an accessible template, when rendered, then a PDF/UA checker reports no errors.
- **Verification:** CONF
- **Origin:** NEW
