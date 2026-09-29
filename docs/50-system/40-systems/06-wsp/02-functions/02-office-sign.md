---
id: UBS-SYS-WSP-02-02
title: Workspace — Office Surfaces and Signatures
status: draft
phase: PH-2
depends_on: [UBS-SYS-WSP-02-01]
---

# Workspace — Office Surfaces and Signatures (DSN-WSP-3xx…5xx)

| Range | Sub-area |
|---|---|
| DSN-WSP-3xx | Live Doc, templates, clauses, negotiation |
| DSN-WSP-4xx | Smart Grid, War Room, Action Messages, Executable Books, rendering |
| DSN-WSP-5xx | electronic signatures and seals |

## Live Doc, templates, clauses, negotiation

### DSN-WSP-301 — Live Doc as bound view
- **Statement:** A Live Doc MUST be a rich-text document (STD-FND-025) whose data fields are bindings to object fields and query results (FR-OFFICE-011), rendered from the DVM snapshot. Bound values MUST NOT be editable as text; edits go through the bound object's form (FR-OFFICE-015).
- **Rationale:** FR-OFFICE-011, FR-OFFICE-015.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** WSP
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given a contract document bound to a fee schedule, when the schedule changes on the branch, then the document shows the new values.
- **Verification:** SCN
- **Origin:** FR-OFFICE-011, FR-OFFICE-015
- **Phase note:** moved to PH-5 by DEC-022 (contract-only scope).

### DSN-WSP-302 — Embedded logic blocks
- **Statement:** Documents MAY embed logic blocks (expressions, decision-table outputs, computed tables) evaluated by the DVM at the document's snapshot (FR-OFFICE-012).
- **Rationale:** FR-OFFICE-012.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** WSP, DVM
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given a fee example block, when rendered, then values equal the DVM calculation for the stated inputs.
- **Verification:** CONF
- **Origin:** FR-OFFICE-012
- **Phase note:** moved to PH-5 by DEC-022 (contract-only scope).

### DSN-WSP-303 — Deterministic rendering
- **Statement:** Rendering a document for a snapshot, template version and locale MUST be deterministic (FR-OFFICE-013, NR-DET-004) so that seals can reference it.
- **Rationale:** FR-OFFICE-013, NR-DET-004.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given the same inputs, when rendered to PDF twice, then the byte hashes are equal.
- **Verification:** CONF
- **Origin:** FR-OFFICE-013, NR-DET-004

### DSN-WSP-304 — Collaborative editing of drafts
- **Statement:** Free text in draft documents MUST support real-time co-editing (ProseMirror with a CRDT such as Yjs) on a draft branch; each save point MUST commit the canonical document version (FR-OFFICE-014).
- **Rationale:** FR-OFFICE-014.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** WSP
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given two editors typing concurrently, when a save point is reached, then one commit contains both edits.
- **Verification:** SCN
- **Origin:** FR-OFFICE-014
- **Phase note:** moved to PH-5 by DEC-022 (contract-only scope).

### DSN-WSP-305 — Templates and clause library
- **Statement:** Templates and clauses MUST be versioned objects (FR-OFFICE-021); documents MUST reference clause versions (FR-OFFICE-022); conditional sections MUST be expressions over bound data (FR-OFFICE-023); clause changes MUST show impacted documents (FR-OFFICE-024).
- **Rationale:** FR-OFFICE-021…024.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** WSP, DVM
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given a clause update, when impact is shown, then all documents referencing the old version are listed.
- **Verification:** SCN
- **Origin:** FR-OFFICE-021, FR-OFFICE-022, FR-OFFICE-023, FR-OFFICE-024
- **Phase note:** moved to PH-5 by DEC-022 (contract-only scope).

### DSN-WSP-306 — Negotiation rounds
- **Statement:** Negotiation MUST run as rounds on a negotiation branch per counterparty (FR-OFFICE-031) with tracked changes attributed by party (FR-OFFICE-032), locked non-negotiable clauses (FR-OFFICE-033), and internal versus external comments (FR-OFFICE-034).
- **Rationale:** FR-OFFICE-031…034, CAP-OFFICE-03.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** WSP, DVM
- **Personas:** PER-ContractManager, PER-ExternalParty
- **Acceptance:**
  1. Given a counterparty editing a locked clause, when saved, then the edit is refused.
- **Verification:** SCN, SEC
- **Origin:** FR-OFFICE-031, FR-OFFICE-032, FR-OFFICE-033, FR-OFFICE-034
- **Phase note:** moved to PH-5 by DEC-022 (contract-only scope).

## Smart Grid, War Room, Action Messages, Executable Books, rendering

### DSN-WSP-401 — Smart Grid
- **Statement:** Smart Grid MUST present records as a spreadsheet-like grid over a saved query (FR-OFFICE-041) with computed columns from shared logic (FR-OFFICE-042), validated edits and bulk paste staged in a process (FR-OFFICE-043), overrides recorded as adjustments (FR-OFFICE-044), summaries and pivots (FR-OFFICE-045), and XLSX import/export (FR-OFFICE-046). Rendering MUST use a canvas engine handling 100,000 rows smoothly.
- **Rationale:** CAP-OFFICE-04.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP
- **Personas:** PER-Accountant
- **Acceptance:**
  1. Given a paste of 5,000 journal lines with 12 invalid, when submitted, then 12 errors are shown in place, and nothing commits until resolved or excluded.
  2. Given a batch journal entered in the grid, when submitted, then it is one balanced entry per voucher and routes to approval like any entry.
- **Verification:** SCN, BENCH
- **Origin:** FR-OFFICE-041, FR-OFFICE-042, FR-OFFICE-043, FR-OFFICE-044, FR-OFFICE-045, FR-OFFICE-046
- **Phase note:** moved to PH-2 by DEC-025 (Smart Grid for batch journal entry).

### DSN-WSP-402 — War Room (live decks)
- **Statement:** Live decks MUST bind slides to queries and objects (FR-OFFICE-051), support drill-down (FR-OFFICE-052), actions on slides (FR-OFFICE-053) and snapshot export with the snapshot coordinates (FR-OFFICE-054).
- **Rationale:** CAP-OFFICE-05.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-Executive
- **Acceptance:**
  1. Given a board deck, when exported, then every figure carries its `(asof, commit)` footnote.
- **Verification:** SCN
- **Origin:** FR-OFFICE-051, FR-OFFICE-052, FR-OFFICE-053, FR-OFFICE-054

### DSN-WSP-403 — Action Messages rendering
- **Statement:** WSP MUST render Action Message landing pages that verify the action token, show the current object state and require sign-in or step-up before executing (DSN-NOD-365).
- **Rationale:** CAP-OFFICE-06.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** WSP
- **Personas:** PER-Approver
- **Acceptance:**
  1. Given an e-mail approval link, when opened on mobile, then the approver sees the item and approves with biometrics.
- **Verification:** SCN
- **Origin:** FR-OFFICE-061, FR-OFFICE-062, FR-OFFICE-063

### DSN-WSP-404 — Executable Books reader
- **Statement:** The Book reader MUST render Book objects (FR-OFFICE-071) shipped as Buks (FR-OFFICE-072) and execute their examples in a sandboxed simulation branch of the reader's VAE (FR-OFFICE-073).
- **Rationale:** CAP-OFFICE-07.
- **Priority:** Should · **Phase:** PH-4 · **Systems:** WSP
- **Personas:** PER-BusinessAnalyst
- **Acceptance:**
  1. Given a book example, when run, then it executes on a simulation branch, and no production state changes.
- **Verification:** SEC
- **Origin:** FR-OFFICE-071, FR-OFFICE-072, FR-OFFICE-073

### DSN-WSP-405 — Rendering and export
- **Statement:** WSP MUST render documents to PDF/A-3 with embedded metadata and source data (FR-OFFICE-081), to DOCX and XLSX (FR-OFFICE-082), with print layouts (FR-OFFICE-083) and tagged accessible PDF (FR-OFFICE-084, NR-ACC-004). Server-side rendering MUST use the same renderer in a headless runtime for determinism.
- **Rationale:** CAP-OFFICE-08.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** WSP, NOD
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given a rendered PDF, when validated with veraPDF, then it passes PDF/A-3 and PDF/UA checks.
- **Verification:** CONF
- **Origin:** FR-OFFICE-081, FR-OFFICE-082, FR-OFFICE-083, FR-OFFICE-084, NR-ACC-004

## Electronic signatures and seals

### DSN-WSP-501 — Signing ceremony
- **Statement:** Signing ceremonies MUST follow E-SIGN and UETA requirements (FR-SIGN-011): consent to electronic records, intent to sign, association of the signature with the record, and retention; signers MUST be authenticated at the configured level (FR-SIGN-012); all parties MUST receive copies (FR-SIGN-013).
- **Rationale:** FR-SIGN-011, FR-SIGN-012, FR-SIGN-013.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** WSP, DVM
- **Personas:** PER-ContractManager, PER-ExternalParty
- **Acceptance:**
  1. Given a ceremony, when completed, then the audit trail shows consent, authentication, intent and delivery for each signer.
- **Verification:** SCN, INSP
- **Origin:** FR-SIGN-011, FR-SIGN-012, FR-SIGN-013
- **Phase note:** moved to PH-5 by DEC-022 (contract-only scope).

### DSN-WSP-502 — Multi-party ordering and void on change
- **Statement:** Ceremonies MUST support ordered and parallel signers (FR-SIGN-031) and MUST void automatically when the sealed document or data changes before completion (FR-SIGN-032), with a full audit trail (FR-SIGN-033).
- **Rationale:** FR-SIGN-031, FR-SIGN-032, FR-SIGN-033.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** WSP, DVM
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given a change after the first signature, when detected, then the ceremony is voided, and signers are notified.
- **Verification:** SCN
- **Origin:** FR-SIGN-031, FR-SIGN-032, FR-SIGN-033
- **Phase note:** moved to PH-5 by DEC-022 (contract-only scope).

### DSN-WSP-503 — Seal over document and data
- **Statement:** On completion, WSP MUST request a seal (IF-DVM-042) over the rendered document hash and the source commit (FR-SIGN-021), embed verification data in the PDF (FR-SIGN-022), and the executed state MUST be immutable (FR-SIGN-023).
- **Rationale:** FR-SIGN-021, FR-SIGN-022, FR-SIGN-023.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** WSP, DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given an executed contract PDF, when verified with the open verifier, then the seal, signatures and source commit verify.
- **Verification:** CONF
- **Origin:** FR-SIGN-021, FR-SIGN-022, FR-SIGN-023
- **Phase note:** moved to PH-5 by DEC-022 (contract-only scope).

### DSN-WSP-504 — External signature providers
- **Statement:** WSP MUST integrate external providers (DocuSign, Adobe Acrobat Sign) through BRG adapters (FR-SIGN-041) under the same seal model (FR-SIGN-042).
- **Rationale:** FR-SIGN-041, FR-SIGN-042.
- **Priority:** Should · **Phase:** PH-5 · **Systems:** WSP, BRG
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given a DocuSign-completed envelope, when returned, then the UBOS seal binds the returned PDF to the commit.
- **Verification:** SCN
- **Origin:** FR-SIGN-041, FR-SIGN-042
- **Phase note:** moved to PH-5 by DEC-022 (contract-only scope).

