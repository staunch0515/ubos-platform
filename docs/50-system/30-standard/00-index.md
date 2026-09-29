---
id: UBS-STD-00
title: BPA Standard — Index and Conformance Language
status: draft
phase: PH-0
depends_on: [UBS-META-01, UBS-REQ-05-19]
---

# BPA Standard — Index and Conformance Language

This folder is the **normative text of the Business Processing Architecture (BPA)**
standard, version 1.0 (draft). It is language-neutral. The reference implementation (DVM)
is one implementation among possible others (FR-STD-061).

## 1. Conformance language

1. The key words MUST, MUST NOT, SHOULD, SHOULD NOT and MAY have the RFC 2119 / RFC 8174
   meaning.
2. Each normative clause is a `STD-<AREA>-<NNN>` item. Informative text is marked
   **Notes** or `Example:`.
3. An **implementation** conforms to a **profile** when it satisfies every MUST clause of
   that profile and passes every conformance vector of the profile (FR-STD-014).
4. A clause marked `[Profile: X]` belongs to profile X. A clause without a marker belongs
   to profile `Core`.

## 2. Areas

| Area code | Topic | File |
|---|---|---|
| FND | Foundations: canonical encoding, hashing, identifiers, value types | `01-foundations.md` |
| OBJ | Object model: objects, versions, envelopes | `02-object-model.md` |
| CLS | Class system: declarations, inheritance, polymorphism, extensions, lenses | `03-class-system.md` |
| KIND | State kinds: Definition, Ledger, System, RawDocument | `04-state-kinds.md` |
| VER | Versioning: commits, branches, change sets, diff, merge, inheritance, revert | `05-versioning.md` (B12) |
| TIME | Bitemporal semantics | `06-bitemporal.md` (B12) |
| TXN | Processes and commit semantics | `07-transactions.md` (B12) |
| PROOF | Content addressing, Merkle trees, proofs, formats | `08-proofs.md` (B12) |
| BPU | The BPU model | `09-bpu.md` (B13) |
| ISA | Instruction set (ABI) | `10-instruction-set/` (B13) |
| CTX | Execution context, determinism, metering | `11-execution.md` (B13) |
| EXPR | L1 expression language | `12-expressions.md` (B14) |
| LGS | Logic Governance Sheets | `13-governance-sheets.md` (B14) |
| ADDR | Addressing and URIs | `14-addressing.md` (B14) |
| PROTO | UBTP protocol | `15-ubtp.md` (B14) |
| SYNC | Sync protocol | `16-sync.md` (B14) |
| ERR | Error registry | `17-errors.md` (B14) |
| CONF | Profiles, vectors, certification | `18-conformance.md` (B14) |

## 3. Profiles (FR-STD-014)

| Profile | Areas | First phase |
|---|---|---|
| Core | FND, OBJ, CLS, KIND (Definition, System, RawDocument), VER, TXN, ISA, CTX, EXPR, ADDR, ERR | PH-1a |
| Bitemporal | TIME | PH-1a |
| Ledger | KIND (Ledger), ledger clauses of VER and ISA | PH-1a |
| Governance | LGS, decisions and guards in ISA and CTX | PH-1b |
| Protocol | PROTO | PH-1b |
| Proof | PROOF | PH-1a (roots and signatures), PH-3 (anchors, packs) |
| Sync | SYNC | PH-3 |
| Browser | a declared subset of Core, Bitemporal and Sync | PH-5 |

## 4. Data definitions in the standard

Schemas are given as `DAT-*` YAML blocks. Field types use the BPA value types (STD-FND-020).
JSON Schema renderings are generated from them for the conformance package.
