---
id: UBS-SYS-BRG-02
title: Bridge — Design Requirements
status: draft
phase: PH-2
depends_on: [UBS-SYS-BRG-01]
---

# Bridge — Design Requirements (DSN-BRG-*)

| Range | Sub-area |
|---|---|
| DSN-BRG-0xx | migration and import |
| DSN-BRG-1xx | integration adapters |
| DSN-BRG-2xx | analytics exit |

## Migration and import

### DSN-BRG-001 — Spreadsheet import with model inference
- **Statement:** BRG MUST read XLSX and CSV files, infer candidate classes, fields, types and keys (FR-MIG-011), and present them for confirmation in Studio before any model change is committed on an import branch.
- **Rationale:** FR-MIG-011, CAP-MIG-01.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** BRG, STU
- **Personas:** PER-ImplementationConsultant
- **Acceptance:**
  1. Given a fund register spreadsheet, when inferred, then decimal, date and reference columns are detected with at least 95% accuracy on the reference corpus.
- **Verification:** CONF
- **Origin:** FR-MIG-011

### DSN-BRG-002 — Row-level results and idempotent re-import
- **Statement:** Imports MUST report per row: created, updated, unchanged or failed with reasons (FR-MIG-012); re-importing the same file MUST be idempotent by natural keys and row hashes (FR-MIG-013).
- **Rationale:** FR-MIG-012, FR-MIG-013.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** BRG
- **Personas:** PER-ImplementationConsultant
- **Acceptance:**
  1. Given the same file imported twice, when the second run completes, then all rows are "unchanged".
- **Verification:** CONF
- **Origin:** FR-MIG-012, FR-MIG-013

### DSN-BRG-003 — Legacy bulk load with history
- **Statement:** Bulk loads MUST support loading historical versions with valid time and original timestamps recorded as attributes (FR-MIG-031), run as bulk processes (FR-MIG-032, DSN-DVM-560), and create ledger opening balances as balanced opening entries (FR-MIG-033).
- **Rationale:** FR-MIG-031, FR-MIG-032, FR-MIG-033.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** BRG, DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given 5 years of fee schedule history, when loaded, then as-of queries on the loaded data return the historical values.
- **Verification:** SCN
- **Origin:** FR-MIG-031, FR-MIG-032, FR-MIG-033

### DSN-BRG-004 — Versioned mappings with tests
- **Statement:** Mapping definitions MUST be versioned objects (FR-MIG-041) executed through the DVM expression engine, with mapping tests of sample inputs and expected outputs (FR-MIG-042).
- **Rationale:** FR-MIG-041, FR-MIG-042, CAP-MIG-04.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** BRG
- **Personas:** PER-ImplementationConsultant
- **Acceptance:**
  1. Given a mapping change that breaks a test, when saved, then activation is blocked.
- **Verification:** CONF
- **Origin:** FR-MIG-041, FR-MIG-042

### DSN-BRG-005 — Reconciliation and signed migration report
- **Statement:** Every migration MUST compute control totals (counts, sums per currency, balances per account) on source and target (FR-MIG-051), record-level reconciliation (FR-MIG-052), and produce a signed migration report sealed against the target commit (FR-MIG-053).
- **Rationale:** FR-MIG-051, FR-MIG-052, FR-MIG-053, CAP-MIG-05.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** BRG, DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a migration with one missing record, when reconciled, then the report shows the difference and the record key.
- **Verification:** SCN
- **Origin:** FR-MIG-051, FR-MIG-052, FR-MIG-053

### DSN-BRG-006 — Document import
- **Statement:** BRG MUST convert DOCX templates into Live Doc templates with bindings suggested (FR-MIG-021) and extract clauses into the clause library with AI assistance through AGT, as proposals reviewed by users (FR-MIG-022).
- **Rationale:** FR-MIG-021, FR-MIG-022, CAP-MIG-02.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** BRG, AGT, WSP
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given a 30-page agreement, when imported, then proposed clauses are listed for review, and nothing enters the library without acceptance.
- **Verification:** SCN
- **Origin:** FR-MIG-021, FR-MIG-022, FR-AI-062

## Integration adapters

### DSN-BRG-101 — Adapter framework
- **Statement:** Adapters MUST be packaged as signed components with declared operations, schemas, schedules and capability needs; they run in BRG workers, fetch or receive data through CTR-041, map it, and submit it through CTR-040 with idempotency keys (FR-INT-041).
- **Rationale:** CAP-INT-04, AR-010.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** BRG
- **Personas:** PER-IsvDeveloper
- **Acceptance:**
  1. Given an adapter crash mid-file, when restarted, then processing resumes without duplicates.
- **Verification:** FAULT
- **Origin:** FR-INT-041, FR-INT-042, AR-010

### DSN-BRG-102 — Financial adapters
- **Statement:** BRG MUST provide adapters for bank statements (SFTP and bank APIs, using NOD parsers for camt, MT940 and BAI2), payment files (pain.001), custodian position and transaction files, and fund-platform order files (FR-INT-071…074).
- **Rationale:** CAP-INT-07, first vertical.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** BRG, NOD
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given daily custodian files for 30 days, when ingested, then positions reconcile with the fund ledger.
- **Verification:** SCN
- **Origin:** FR-INT-071, FR-INT-072, FR-INT-073, FR-INT-074

### DSN-BRG-103 — E-mail and chat
- **Statement:** BRG MUST provide adapters for Microsoft 365 and Google Workspace mail and for Slack and Teams (FR-INT-062, FR-INT-063), respecting classification for outbound content.
- **Rationale:** CAP-INT-06.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** BRG
- **Personas:** PER-BusinessUser
- **Acceptance:**
  1. Given a restricted field in a template, when sending externally, then it is masked or the send is blocked per policy.
- **Verification:** SEC
- **Origin:** FR-INT-062, FR-INT-063

### DSN-BRG-104 — Event streaming out
- **Statement:** BRG MUST publish change feeds to Kafka, Amazon Kinesis and Google Pub/Sub with schema export (FR-INT-053) and per-key ordering (FR-INT-052).
- **Rationale:** CAP-INT-05.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** BRG
- **Personas:** PER-IsvDeveloper
- **Acceptance:**
  1. Given a topic consumer, when events are read, then their schemas are available in the registry.
- **Verification:** CONF
- **Origin:** FR-INT-051, FR-INT-052, FR-INT-053

### DSN-BRG-105 — External signature providers
- **Statement:** BRG MUST adapt DocuSign and Adobe Acrobat Sign envelopes to UBOS ceremonies (DSN-WSP-504).
- **Rationale:** CAP-SIGN-04.
- **Priority:** Should · **Phase:** PH-4 · **Systems:** BRG, WSP
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given a completed envelope, when received, then the ceremony completes, and the seal is created.
- **Verification:** SCN
- **Origin:** FR-SIGN-041, FR-SIGN-042

## Analytics exit

### DSN-BRG-201 — Incremental CDC export
- **Statement:** BRG MUST export committed changes per commit range to Parquet files and Apache Iceberg tables (FR-ANL-051), exactly once per range using the Iceberg snapshot and a watermark stored with the snapshot (FR-ANL-052), and evolve table schemas with model lenses (FR-ANL-053).
- **Rationale:** FR-ANL-051, FR-ANL-052, FR-ANL-053, CAP-ANL-05.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** BRG
- **Personas:** PER-DataAnalyst
- **Acceptance:**
  1. Given a crash after writing files but before the snapshot commit, when retried, then no duplicate rows exist.
- **Verification:** FAULT
- **Origin:** FR-ANL-051, FR-ANL-052, FR-ANL-053

### DSN-BRG-202 — Masking and classification in exports
- **Statement:** Each sink MUST have a clearance level; fields above it MUST be masked, hashed or dropped (FR-ANL-054), and exports MUST include bitemporal columns (valid_from, valid_to, tx_time, commit).
- **Rationale:** FR-ANL-054, AR-022.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** BRG
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given a sink cleared for internal data, when personal fields are exported, then they are hashed.
- **Verification:** SEC
- **Origin:** FR-ANL-054, AR-022

### DSN-BRG-203 — Embedded analytical engine
- **Statement:** BRG MUST provide a single-tenant DuckDB engine over exported data (FR-ANL-041) with governed datasets (FR-ANL-043) and read-only SQL for analysts (FR-ANL-042), with row filters applied from the analyst's permissions at dataset build time.
- **Rationale:** FR-ANL-041, FR-ANL-042, FR-ANL-043, CAP-ANL-04.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** BRG
- **Personas:** PER-DataAnalyst
- **Acceptance:**
  1. Given an analyst without access to fund F, when querying the dataset, then fund F rows are absent.
- **Verification:** SEC
- **Origin:** FR-ANL-041, FR-ANL-042, FR-ANL-043

### DSN-BRG-204 — Isolation of analytical stores
- **Statement:** Analytical stores MUST be per tenant (FR-TEN-022) and marked derived and rebuildable (AR-015).
- **Rationale:** FR-TEN-022, AR-015.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** BRG
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given the isolation suite, when run against analytics, then no cross-tenant data is visible.
- **Verification:** SEC
- **Origin:** FR-TEN-022, AR-015
