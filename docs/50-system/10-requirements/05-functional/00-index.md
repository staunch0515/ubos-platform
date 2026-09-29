---
id: UBS-REQ-05
title: Functional Requirements — Index
status: review
phase: ALL
depends_on: [UBS-REQ-03, UBS-META-01]
---

# Functional Requirements — Index

Functional requirements (`FR-*`) refine the capabilities in `UBS-REQ-03`. The number
encodes the capability: `FR-<DOM>-<NN><k>` belongs to `CAP-<DOM>-<NN>`.

## Files

| File | Domains |
|---|---|
| `01-model.md` | MODEL |
| `02-versioning.md` | VER |
| `03-time-ledger.md` | TIME, LEDG |
| `04-transactions.md` | TXN |
| `05-logic.md` | LOGIC |
| `06-rules.md` | RULE |
| `07-flow.md` | FLOW |
| `08-events-query.md` | EVT, QRY |
| `09-iam-tenancy.md` | IAM, TEN |
| `10-audit-ux.md` | AUD, UX |
| `11-office.md` | OFFICE |
| `12-ai.md` | AI |
| `13-packages-integration.md` | PKG, INT |
| `14-analytics-proof.md` | ANL, PROOF |
| `15-sync.md` | SYNC |
| `16-operations-billing.md` | OPS, BILL |
| `17-developer-migration.md` | DEV, MIG |
| `18-common-services.md` | NTF, FILE, SIGN, LOC |
| `19-standard.md` | STD |

## Conventions used in the statements

1. "The platform" means the set of systems named in the item's **Systems** field.
2. "Commit" means a DVM commit (GL-Commit). "Object" means a versioned DVM object of
   any class.
3. "Explained error" means an error object that carries a code, a message and a
   machine-readable `explanation` array. Each entry names the rule, sheet, guard or
   constraint and the values involved.
4. Default limits written in statements (for example "at least 64 levels") are minimums
   that every conforming implementation MUST support.
