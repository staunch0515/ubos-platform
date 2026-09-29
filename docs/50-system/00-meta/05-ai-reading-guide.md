---
id: UBS-META-05
title: AI Reading Guide
status: draft
phase: ALL
depends_on: [UBS-META-00, UBS-META-01]
---

# AI Reading Guide

This guide tells an AI agent how to use this volume. It is finalised in batch B27. Until
then the sections marked *(preliminary)* may change.

## 1. Orientation in five minutes

1. Read `UBS-META-00` (charter): what UBOS is, the scope and the exclusions.
2. Read `UBS-META-06` (inputs): where every idea came from and how conflicts were
   resolved.
3. Read `UBS-META-01` (writing rules): how IDs, requirement blocks and verification codes
   work.
4. Open `10-requirements/00-index.md` for the requirements, or `50-phases/` for what is
   built when.

## 2. Task-oriented entry points *(preliminary)*

| If your task is … | Read, in order |
|---|---|
| Understand the product | `UBS-REQ-01` vision → `UBS-REQ-02` personas → `UBS-REQ-04` scenarios |
| Implement a kernel feature | the FR domain file → the matching `STD-*` clauses → `UBS-SYS-DVM-*` → the phase plan → the verification items |
| Implement a product feature | the scenario → the FR items → the system chapter → the SDK interfaces |
| Plan or review a phase | `50-phases/PH-<N>` → its exit criteria → `60-verification/` suites |
| Verify a requirement | the requirement's **Verification** field → `60-verification/` items → the environment and dataset |
| Resolve an apparent conflict | the requirement's **Origin** → `UBS-META-06` §6 → the decision log |

## 3. Reading rules

1. **IDs are the contract.** Cite IDs, never page positions.
2. **Normative text** is only in `Statement`, `Clause`, `Acceptance`, `Target` and
   `Control reference` fields, and in schema or grammar blocks. Rationale, notes and
   examples are informative.
3. **Precedence** when two statements seem to conflict, highest first:
   1. a `STD-*` clause;
   2. a `CR-*` item;
   3. an `NR-*` item;
   4. an `FR-*` item;
   5. a `DSN-*` item;
   6. prose.

   Report any real conflict as an open question (`OQ-*`) instead of choosing silently.
4. **Phase discipline.** Do not implement a requirement before its phase unless a
   `DEC-*` record allows it. Do not skip a phase's exit criteria.
5. **Excluded topics** (`UBS-META-00` §3.2) never become features, even when an input
   document mentions them.

## 4. Glossary use

Terms are defined as `GL-*` items (from batch B9). Use each term exactly. A term marked
`platform only` MUST NOT appear in business-user interfaces.
