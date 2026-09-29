---
id: UBS-SYS-NTY-02
title: Notary — Design Requirements
status: draft
phase: PH-3
depends_on: [UBS-SYS-NTY-01]
---

# Notary — Design Requirements (DSN-NTY-*)

### DSN-NTY-001 — Periodic anchoring
- **Statement:** For each tenant, NTY MUST obtain the current tenant root (DSN-DVM-934) at the configured cadence (default hourly when the root changed, and at every period close), request RFC 3161 timestamps from two independent TSAs, and append an `AnchorReceipt` ledger entry to the tenant's own VAE (FR-PROOF-041, FR-PROOF-042).
- **Rationale:** FR-PROOF-041, FR-PROOF-042, AR-005.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** NTY, DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given a day of commits, when anchoring runs, then each hour with changes has a receipt verifiable with the TSA certificate chain.
- **Verification:** CONF
- **Origin:** FR-PROOF-041, FR-PROOF-042, STD-PROOF-040, CTR-031, CR-SEC-002

### DSN-NTY-002 — Anchoring monitoring
- **Statement:** NTY MUST alert when a tenant's latest receipt is older than twice the cadence, when a TSA fails repeatedly, or when a TSA certificate nears expiry (FR-PROOF-043).
- **Rationale:** FR-PROOF-043.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** NTY
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given one TSA unavailable, when anchoring runs, then the second TSA is used, and an alert is raised.
- **Verification:** FAULT
- **Origin:** FR-PROOF-043

### DSN-NTY-003 — Public transparency anchoring (optional)
- **Statement:** Tenants MAY enable anchoring of a salted digest of their tenant root to a public transparency log; the digest MUST reveal nothing about content.
- **Rationale:** Stronger third-party evidence without disclosure.
- **Priority:** Could · **Phase:** PH-4 · **Systems:** NTY
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given the public log entry, when inspected, then only a random-looking digest is visible.
- **Verification:** SEC
- **Origin:** FR-PROOF-041

### DSN-NTY-004 — Evidence pack service
- **Statement:** Authorised tenant users MUST be able to request evidence packs by scope (query and temporal coordinates) (FR-PROOF-052); NTY MUST collect them through the DVM (IF-DVM-041) under the requesting user's rights, include anchor receipts, and deliver a reproducible archive (FR-PROOF-054).
- **Rationale:** FR-PROOF-051, FR-PROOF-052, FR-PROOF-054, CAP-PROOF-05.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** NTY, DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given a pack request for Q1 NAVs, when built twice, then the archives are byte-identical.
- **Verification:** CONF
- **Origin:** FR-PROOF-051, FR-PROOF-052, FR-PROOF-054

### DSN-NTY-005 — Provable redaction for external recipients
- **Statement:** Packs for external recipients MUST apply redaction profiles (DSN-DVM-942) chosen by the requester, with the manifest listing redaction reasons (FR-PROOF-053).
- **Rationale:** FR-PROOF-053, CR-GDPR-001.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** NTY
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given a regulator pack with investor names redacted, when verified, then verification succeeds, and names are absent.
- **Verification:** SEC, CONF
- **Origin:** FR-PROOF-053, CR-GDPR-001

### DSN-NTY-006 — Open verifier
- **Statement:** The open verifier MUST be an open-source (Apache-2.0) Rust library with a CLI, a WASM build and a static web page that runs entirely in the browser (FR-PROOF-061), sharing primitives with `ubos-proof`. It MUST verify evidence packs, seals, signed PDFs, portable archives, inclusion and consistency proofs, signatures with key histories and anchor receipts.
- **Rationale:** FR-PROOF-061, FR-PROOF-063, CR-NIST-002.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** NTY, BPA
- **Personas:** PER-RegulatorExaminer
- **Acceptance:**
  1. Given a pack, when verified in the web page with the network disconnected, then verification completes.
- **Verification:** CONF, SEC
- **Origin:** FR-PROOF-061, FR-PROOF-063, CR-NIST-002

### DSN-NTY-007 — Clear verification reports
- **Statement:** Verification reports MUST list every check with outcome and, for failures, the exact item and plain-language reason (FR-PROOF-062), exportable as PDF and JSON.
- **Rationale:** FR-PROOF-062.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** NTY
- **Personas:** PER-RegulatorExaminer
- **Acceptance:**
  1. Given a tampered pack, when verified, then the report names the tampered object and the failed check.
- **Verification:** CONF, USE
- **Origin:** FR-PROOF-062

### DSN-NTY-008 — Regulator access support
- **Statement:** NTY MUST support prompt production of records in readable form for examinations (CR-SEC-003) by generating packs with rendered human-readable views alongside machine data, and SHOULD provide a portal where tenants share packs with named examiners (PH-4).
- **Rationale:** CR-SEC-003, CR-FINRA-003.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** NTY
- **Personas:** PER-RegulatorExaminer
- **Acceptance:**
  1. Given an examination request, when a pack is produced, then it contains PDFs of the records and the verifiable data within 1 business day.
- **Verification:** SCN
- **Origin:** CR-SEC-003, CR-FINRA-003

### DSN-NTY-009 — Least-privilege service principal
- **Statement:** NTY MUST use a tenant-scoped service principal that can read roots and proofs and append receipts, and can collect evidence only within a user-authorised request (CTR-030).
- **Rationale:** AR-009, CTR-030.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** NTY
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given NTY without a pending request, when it tries to read business objects, then access is denied.
- **Verification:** SEC
- **Origin:** AR-009, CTR-030
