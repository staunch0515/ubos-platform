---
id: UBS-REQ-04-03
title: Scenarios — Contract Management (First Vertical)
status: review
phase: PH-2
depends_on: [UBS-REQ-04]
---

# Scenarios — Contract Management (SCN-201 … SCN-208)

## Domain context (informative)

| Class | Kind | Notes |
|---|---|---|
| `Agreement` (base), `ServiceAgreement`, `NDA`, `SupplyAgreement` | Definition with lifecycle | `drafting → internal_review → negotiation → approved → signing → executed → amended → terminated` or `expired` |
| `Clause`, `ClauseVariant`, `Template` | Definition | versioned clause library |
| `Obligation` | Definition with lifecycle | extracted from executed agreements; `open → due → fulfilled` or `breached` / `waived` |
| `SignatureRecord`, `NegotiationRound` | Ledger | append-only evidence |
| `LiveDoc` | view object | document rendering bound to agreement data |

### SCN-201 — Draft a contract from a template with live party data
- **Goal:** A contract draft always shows current party data and the correct clause variants.
- **Personas:** PER-ContractManager
- **Systems:** WSP, DVM
- **Phase:** PH-2
- **Preconditions:** 1. The template `Service Agreement v4` binds `{{party.legal_name}}`, `{{party.registered_address}}`, clause `LiabilityCap` (variant chosen by contract value) and the logic `late_fee`.
- **Main flow:**
  1. The contract manager creates a `ServiceAgreement` for counterparty `Acme Corp` with value 2.4M → the Live Doc renders, with the liability-cap variant "2x fees" selected by the decision table.
  2. Acme's registered address changes in the party master (another user) → the open draft shows the new address at the next render. The change is marked "updated since you last viewed".
  3. The manager exports a PDF for internal reading → the PDF footer holds the document state hash and the data commit ID.
- **Alternate and failure flows:**
  - A1. A binding refers to a field the user may not read → the rendering shows a masked placeholder, and the export is blocked for that user.
- **Postconditions (observable):** 1. The draft has a version history with every content and data change.
- **Business rules exercised:** FR-OFFICE-011, FR-OFFICE-021, FR-RULE-041, FR-OFFICE-081, FR-IAM-061
- **Verification:** SCN, USE

### SCN-202 — Internal approval with segregation of duties and obligation extraction
- **Goal:** Contracts are approved by the right people, and obligations are captured as structured objects.
- **Personas:** PER-ContractManager, PER-Approver, PER-ComplianceOfficer
- **Systems:** WSP, DVM
- **Phase:** PH-2
- **Preconditions:** 1. The approval sheet requires a legal approver for liability caps above 1M and a finance approver for values above 2M.
- **Main flow:**
  1. The contract manager submits for internal review → legal and finance approval tasks are created in parallel.
  2. Both approve with the diff of any clause deviations from the standard template visible.
  3. On approval, the obligations defined by clause metadata (payment terms, reporting, renewal notice) are created as `Obligation` objects with due-date rules.
- **Alternate and failure flows:**
  - A1. The contract manager is also the finance approver in the org chart → maker–checker blocks self-approval, and the task routes to a delegate.
- **Postconditions (observable):** 1. The obligations link to the clause versions that created them.
- **Business rules exercised:** FR-FLOW-041, FR-FLOW-091, FR-RULE-011, FR-MODEL-061
- **Verification:** SCN, PILOT

### SCN-203 — Negotiation rounds with a counterparty
- **Goal:** Redlines are exchanged with an external party, and each round is versioned and attributable.
- **Personas:** PER-ContractManager, PER-ExternalParty
- **Systems:** WSP (external portal), DVM
- **Phase:** PH-3
- **Preconditions:** 1. The counterparty has an external-portal identity scoped to this agreement.
- **Main flow:**
  1. The contract manager shares round 1 → the counterparty sees the document, not the internal comments.
  2. The counterparty proposes changes to two clauses → a `NegotiationRound` entry is appended, and the proposed changes appear as a change set against the internal draft.
  3. The contract manager accepts one change and counters the other → round 2 is shared.
  4. Both parties mark the text as agreed → the agreement moves to `approved` (subject to internal approvals).
- **Alternate and failure flows:**
  - A1. The counterparty edits a clause marked non-negotiable → the edit is refused in the portal with the reason.
- **Postconditions (observable):** 1. Each round shows author, timestamp and diff. 2. Internal notes never appear in external views or exports.
- **Business rules exercised:** FR-OFFICE-031, FR-UX-091, FR-VER-041, FR-IAM-041
- **Verification:** SCN, USE, SEC

### SCN-204 — Electronic signature with a seal and independent verification
- **Goal:** Signatures legally bind the exact document state and data, and anyone can verify them later.
- **Personas:** PER-ContractManager, PER-ExternalParty, PER-RegulatorExaminer
- **Systems:** WSP, DVM, NTY (PH-3 for anchoring)
- **Phase:** PH-2 (two-party signing), PH-3 (multi-party ceremonies, anchoring)
- **Preconditions:** 1. The agreement is `approved`. 2. The signers have verified e-mail identities, and internal signers use platform keys.
- **Main flow:**
  1. The contract manager starts signing → each signer receives a link, consents to electronic records (E-SIGN) and signs.
  2. The platform computes a seal: a hash of the rendered document, the data snapshot (object versions) and the signer's intent statement. The seal is signed.
  3. After the last signature the agreement is `executed`, and a `SignatureRecord` entry per signer is appended.
  4. A year later the counterparty verifies the signed PDF with the verifier → the seal matches the document, and the signatures verify.
- **Alternate and failure flows:**
  - A1. The document is changed after the first signature → the pending signatures are voided, and signing restarts.
- **Postconditions (observable):** 1. The executed agreement's version is immutable. Later changes are amendments (SCN-205).
- **Business rules exercised:** FR-SIGN-011, FR-SIGN-021, FR-PROOF-021, FR-PROOF-061
- **Verification:** SCN, INSP, AUDIT

### SCN-205 — Amend an executed contract
- **Goal:** An amendment changes the terms from a date, and the original terms remain provable.
- **Personas:** PER-ContractManager
- **Systems:** WSP, DVM
- **Phase:** PH-2
- **Preconditions:** 1. The agreement was executed on 2026-03-01 with a monthly fee of 20,000.
- **Main flow:**
  1. The contract manager creates amendment 1: the fee becomes 18,000 with valid time from 2026-11-01.
  2. The amendment goes through approval and signature (SCN-202, SCN-204).
  3. On execution the agreement's effective terms change from 2026-11-01. The obligations generated after that date use 18,000.
- **Alternate and failure flows:**
  - A1. The amendment's valid time is before the last invoiced period → a warning lists the affected invoices, and the correction process of SCN-104 applies.
- **Postconditions (observable):** 1. `asof=2026-10-15` returns 20,000. 2. `asof=2026-11-15` returns 18,000. 3. Both signed documents verify.
- **Business rules exercised:** FR-TIME-021, FR-TIME-041, FR-SIGN-021, FR-FLOW-011
- **Verification:** SCN, PROP

### SCN-206 — Obligation tracking and renewal alert
- **Goal:** No renewal or reporting obligation is missed.
- **Personas:** PER-ContractManager, PER-BusinessUser
- **Systems:** DVM, WSP
- **Phase:** PH-2
- **Preconditions:** 1. The agreement renews automatically unless notice is given 90 days before 2027-03-01.
- **Main flow:**
  1. The obligation `RenewalNotice` has a due date of 2026-12-01, computed by the business calendar (moved to the previous business day if needed).
  2. At 30, 14 and 3 days before the due date, notifications are sent to the owner according to preferences.
  3. The owner records "renew" → the obligation is fulfilled, and the next term's obligations are generated.
- **Alternate and failure flows:**
  - A1. The due date passes without action → the obligation becomes `breached`, and an escalation goes to the owner's manager.
- **Postconditions (observable):** 1. The obligation history shows every notification and action.
- **Business rules exercised:** FR-EVT-041, FR-NTF-011, FR-FLOW-081, FR-LOC-041
- **Verification:** SCN

### SCN-207 — Legal hold during a dispute
- **Goal:** All records related to a disputed agreement are preserved beyond normal retention.
- **Personas:** PER-ComplianceOfficer, PER-ContractManager
- **Systems:** DVM, NTY
- **Phase:** PH-3
- **Preconditions:** 1. The retention policy disposes of drafts of expired agreements after 7 years.
- **Main flow:**
  1. Legal places a hold on agreement `SA-2019-044`, scoped to related objects (amendments, obligations, communications, documents).
  2. The disposal job skips all held objects and records the skip.
  3. When the hold is released, the disposal resumes according to policy.
- **Alternate and failure flows:**
  - A1. An erasure request (SCN-016) targets a person in the hold scope → it is refused while the hold is active.
- **Postconditions (observable):** 1. The hold, its scope and every skip are auditable.
- **Business rules exercised:** FR-AUD-061, FR-AUD-071, FR-MODEL-061
- **Verification:** SCN, AUDIT

### SCN-208 — Clause library change impact
- **Goal:** Before changing a standard clause, legal sees which agreements and templates use it.
- **Personas:** PER-ContractManager, PER-BusinessArchitect
- **Systems:** STU, DVM
- **Phase:** PH-2
- **Preconditions:** 1. The clause `DataProtection` v3 is used by 412 agreements and 6 templates.
- **Main flow:**
  1. Legal drafts `DataProtection` v4 → the impact analysis lists templates (auto-updated on publish) and agreements (unchanged unless amended), grouped by lifecycle state.
  2. Legal publishes v4 → the templates reference v4. The drafts in `drafting` are flagged "clause update available".
- **Alternate and failure flows:**
  - A1. An agreement in `negotiation` uses v3 → it is not changed automatically, and the manager is notified.
- **Postconditions (observable):** 1. Executed agreements keep their v3 references permanently.
- **Business rules exercised:** FR-RULE-071, FR-OFFICE-021, FR-VER-061
- **Verification:** SCN
