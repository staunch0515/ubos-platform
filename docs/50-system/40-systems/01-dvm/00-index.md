---
id: UBS-SYS-DVM-00
title: DVM Reference Kernel — Overview
status: draft
phase: PH-1
depends_on: [UBS-SYS-00, UBS-ARC-01, UBS-ARC-04, UBS-STD-00]
---

# DVM Reference Kernel — Overview

## 1. Purpose

The DVM is the reference implementation of the BPA standard. It is an embeddable library
(AR-002) that holds all authoritative semantics:
- storage of content-addressed objects, entries and commits;
- the class system with inheritance, polymorphism and lenses;
- branches, change sets, merge and revert;
- bitemporal timelines;
- ledgers and projections;
- processes with atomic flush;
- the sandboxed deterministic logic runtime;
- governance sheets, decisions and flows;
- authorization decisions and masking;
- queries, view resolution and proofs.

## 2. Responsibilities and non-responsibilities

| Responsible for | Not responsible for |
|---|---|
| every `STD-*` clause of the profiles Core, Bitemporal, Ledger, Governance and Proof (roots, signatures, proofs), and the Sync data structures | network listeners, sessions, authentication (NOD) |
| authorization decisions (AR-024) | secret storage and connector I/O (NOD) |
| journaling and replay | job threads, schedule clocks (NOD) |
| kernel indexes and projections maintained in the flush | full-text and vector index engines (NOD/BRG adapters) |
| view resolution payloads | rendering (WSP) |
| export and import in the portable archive format | anchoring (NTY), fleet operations (CTL) |

## 3. Personas served

PER-KernelEngineer (builds it), PER-ThirdPartyImplementer (embeds it), and every persona
indirectly through the systems built on it.

## 4. Phase map (DEC-014)

| Gate or phase | DVM scope |
|---|---|
| PH-0 | prototype of the storage engine and prolly tree (to validate DEC-020 and ASM-003); formal models of commit, CAS, merge and timeline |
| PH-1a | storage, model, versioning, bitemporal, ledger, transactions, L1 and L2 runtime, proofs (roots, signatures, inclusion), portable export and import; in-process host API |
| PH-1b | governance sheets, decisions, lifecycles and ports, events and jobs objects, queries, authorization (RBAC and ABAC), view resolution basics |
| PH-2 | lenses, decision tables, approvals and deferred commits, workflows and sagas, ReBAC, masking, crypto-shredding, simulation support, projections at scale |
| PH-3 | L3 WASM, `ask.model`, agent-branch policies, vector-search hooks, sync data structures, legal holds and retention |
| PH-4 | history tiering, embedding API for third parties, performance at Cell scale |
| PH-5 | browser build (WASM), federation fallback and shared-BPU structures |

## 5. Crate map (UBS-ARC-07)

| Sub-area | Crates |
|---|---|
| storage and indexes | `ubos-store`, `ubos-tree` |
| model | `ubos-model` |
| versioning, time, ledger | `ubos-version`, `ubos-time`, `ubos-ledger` |
| transactions | `ubos-txn` |
| runtime | `ubos-runtime`, `ubos-expr` |
| rules and flow | `ubos-rules`, `ubos-flow` |
| authorization | `ubos-authz` |
| query | `ubos-query` |
| proofs | `ubos-proof` |
| facade | `ubos-dvm` |

## 6. Files

| File | Content |
|---|---|
| `01-context.md` | context and contracts |
| `02-functions/` | DSN-DVM-* by sub-area |
| `03-interfaces.md` | IF-DVM-* (host API and host-service interfaces) |
| `04-data.md` | physical storage layout (tables, keys, indexes, partitioning) |
| `05-state-machines.md` | process, change set, pending change, logic asset, branch |
| `06-configuration.md` | kernel configuration keys |
| `07-operations.md` | failure modes, recovery, rebuilds, tuning |
| `08-verification.md` | DVM verification plan |
