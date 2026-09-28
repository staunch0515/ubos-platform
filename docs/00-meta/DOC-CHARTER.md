---
id: META-CHARTER
title: Documentation Charter
status: stable
phase: P0
depends_on: []
---

# Documentation Charter

## 1. Purpose

This documentation set is a **complete research study and target specification** for a
**General-Purpose Business Platform** (working name: **UBOS**).

It is produced by studying eight existing repositories, all of which are independent
attempts at building the same platform. None of them is "the old one" or "the new one";
they are treated as **peer design experiments**. The goal is to extract every good idea,
compare alternatives, and synthesize one unified, buildable target design.

## 2. Primary audience

| Audience | Priority | Implication |
|---|---|---|
| Future AI coding sessions | **Primary** | Structured, explicit, self-contained, machine-navigable. Redundancy is acceptable; ambiguity is not. |
| Human architects | Secondary | Readability for humans is nice-to-have, never a constraint. |

An AI reading this set must be able to:
1. Understand what the platform is and why each design decision was made.
2. Locate any requirement, entity, interface or decision by a stable ID.
3. Implement a module by reading only its chapter plus the chapters it declares as dependencies.
4. Verify completion against explicit acceptance criteria.
5. Trace any design point back to the source repository that inspired it.

## 3. Source repositories (all peers)

| Key | Repository | Primary language (at a glance) |
|---|---|---|
| `UP` | staunch0515/ubos-platform | Java + TS |
| `LC` | staunch0515/logicorum | Java + TS |
| `UB` | staunch0515/ubos | Java + TS |
| `LS` | staunch0515/logrum-system | Rust |
| `US` | staunch0515/ubos-system | Rust + TS |
| `UC` | staunch0515/ubos_core | Rust + TS |
| `UW` | staunch0515/ubos_web | TS (frontend only) |
| `FU` | staunch-studio/fund-ubos | Java + TS |

The two-letter key is used everywhere as a short source reference (see WRITING-RULES §6).

## 4. Scope

In scope:
- Design ideas, architecture, core abstractions, domain/metadata models, runtime mechanisms,
  extension mechanisms, UI/rendering concepts, AI integration concepts, deployment shape.
- The design notes already present in each repo (`docs/`, `README.md`, `*.md`) — these are
  first-class sources of design intent, often more important than the code.
- Highlights ("what this repo does better than the others").
- A recommended target technology stack (Java / Rust / hybrid), justified in an ADR.

Explicitly **out of scope** (user instruction):
- Running tests or builds.
- Finding bugs, vulnerabilities, security issues.
- Judging code quality, style or completeness as a goal in itself.
  (Incompleteness may be *noted* only where it matters for understanding design intent.)

## 5. Phases

| Phase | Name | Output location | Exit condition |
|---|---|---|---|
| P0 | Meta + Inventory | `00-meta/`, `10-inventory/` | Rules fixed; every repo inventoried |
| P1 | Per-repo analysis | `20-analysis/<KEY>-<repo>/` | Every repo has the full analysis file set (TEMPLATES §A) |
| P2 | Cross-repo comparison | `30-comparison/` | Every comparison topic has a verdict (best-of / fusion) |
| P3 | Target outline + global conventions | `40-spec/00-*` | Spec outline, glossary, ID registry, stack ADR accepted |
| P4 | Spec writing | `40-spec/NN-*` | Every outline chapter written to `status: complete` |
| P5 | Consolidation | `docs/README.md`, `40-spec/INDEX.md`, `AI-GUIDE.md` | Cross-references valid; glossary consistent |

Progress is tracked **only** in `00-meta/PROGRESS.md`. Each working session must read it
first and update it last.

## 6. Guiding principles

1. **Research first, synthesize second.** No target design statement without a traceable
   origin (a source repo, or an explicit "NEW" marker with rationale).
2. **Write everything down.** Analysis notes are kept permanently; they are the evidence base.
3. **Small files, stable IDs.** Every file fits in one AI context read; everything referable
   has an ID.
4. **Structure over prose.** Tables, YAML, schemas, state machines, numbered rules.
5. **Session-independent.** A fresh session with no chat history must be able to continue
   the work from `PROGRESS.md` alone.
