---
id: UBS-SYS-NTY-00
title: Notary — Overview
status: draft
phase: PH-3
depends_on: [UBS-SYS-00, UBS-SYS-DVM-00, UBS-STD-08]
---

# Notary (NTY) — Overview

## 1. Purpose

The Notary makes UBOS evidence independently checkable:
- it anchors tenant roots periodically to external time evidence (RFC 3161 TSAs, optionally
  a public transparency log), and writes receipts back into the tenant's own ledger;
- it assembles evidence packs for auditors and regulators on request;
- it ships the open verifier: a standalone, open-source tool and library that verifies packs,
  seals, exports and proofs without trusting UBOS.

## 2. Responsibilities and non-responsibilities

| Responsible for | Not responsible for |
|---|---|
| anchoring schedule, TSA clients, receipt storage | computing roots and proofs (DVM) |
| evidence pack assembly service | defining pack formats (BPA standard) |
| open verifier (CLI, library, WASM, web page) | business data storage (never holds truth, AR-005) |
| anchoring monitoring | signatures of commits (DVM, NOD) |

## 3. Personas served

PER-InternalAuditor, PER-RegulatorExaminer, PER-ComplianceOfficer, PER-ExternalParty.

## 4. Phase map

| Phase | NTY scope |
|---|---|
| PH-1 | verifier library functions used in tests (inclusion, signatures) |
| PH-2 | evidence pack assembly inside NOD (library), verifier CLI |
| PH-3 | NTY service: scheduled anchoring with two TSAs, receipts, monitoring; evidence pack service; open verifier with web page and WASM build |
| PH-4 | optional public transparency-log anchoring; regulator self-service portal for packs shared with them |

## 5. Files

| File | Content |
|---|---|
| `01-context.md` | context |
| `02-functions.md` | DSN-NTY-* |
| `03-interfaces.md` | IF-NTY-* |
| `04-data.md` | receipts and pack jobs |
| `05-state-machines.md` | anchoring cycle, pack job |
| `06-configuration.md` | settings |
| `07-operations.md` | operations |
| `08-verification.md` | verification plan |
