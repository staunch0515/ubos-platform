# UBOS — Universal Business Platform: Research and Specification

This folder contains a complete research study of eight peer repositories that each attempt
the same platform, together with **one unified target specification** synthesised from
them. It is written primarily for AI implementers (see [`00-meta/AI-GUIDE.md`](00-meta/AI-GUIDE.md)).

## What the platform is

A Rust micro-kernel with four defining properties:
- It stores every business artifact (data, types, logic, UI, rules, governance) as **versioned, branchable, tenant-scoped entities in six tables**.
- It runs **sandboxed Rhai logic** behind a versioned syscall ABI inside audited processes.
- It is driven entirely by **metadata**: types, actions, views and packages.
- It serves **five clients** (web, desktop, mobile, CLI, SDK/agent) over one protocol (**UBTP**), with AI acting through the same governed paths as people.

## How to read this set

| If you want to … | Start at |
|---|---|
| implement the platform | [`00-meta/AI-GUIDE.md`](00-meta/AI-GUIDE.md), then [`40-spec/00-outline.md`](40-spec/00-outline.md) |
| look up any ID (REQ, ENT, API, TERM, NFR) | [`40-spec/INDEX/`](40-spec/INDEX/00-index.md) |
| understand a term | [`40-spec/01-glossary/`](40-spec/01-glossary/00-index.md) |
| know the conventions (names, IDs, errors, storage rules) | [`40-spec/02-conventions.md`](40-spec/02-conventions.md) |
| see the decisions and their reasons | [`40-spec/90-adr/`](40-spec/90-adr/000-index.md) and [`30-comparison/`](30-comparison/00-matrix.md) |
| trace a requirement back to the source repositories | the requirement's `Origin` → [`30-comparison/`](30-comparison/) → [`20-analysis/`](20-analysis/) |
| see how the work was done | [`00-meta/PROGRESS.md`](00-meta/PROGRESS.md), [`00-meta/DOC-CHARTER.md`](00-meta/DOC-CHARTER.md) |

## Folder map

| Folder | Phase | Content |
|---|---|---|
| `00-meta/` | P0 | charter, writing rules, templates, progress log, open questions (all answered), AI guide |
| `10-inventory/` | P0 | inventory of the eight source repositories and a summary |
| `20-analysis/<KEY>-<repo>/` | P1 | deep analysis per repository: concepts (CON-*), highlights (HL-*), design-document digests, concept index |
| `30-comparison/` | P2 | 22 topic comparisons with 121 verdicts (VRD-*) and the matrix |
| `40-spec/` | P3–P4 | the target specification: outline, glossary, conventions, 23 chapters, 6 ADRs |
| `40-spec/INDEX/` | P5 | generated indexes and verdict traceability |
| `tools/` | P5 | `check_refs.py` (cross-reference validation) and `build_index.py` (index generation) |

## Source repositories (peers, no old/new relationship)

| Key | Repository | Stack |
|---|---|---|
| UP | staunch0515/ubos-platform | Java/Groovy + TS |
| FU | staunch-studio/fund-ubos | Java/Groovy + TS |
| LC | staunch0515/logicorum | Java + TS |
| UB | staunch0515/ubos | Java/Groovy + TS |
| UC | staunch0515/ubos_core | Rust/Rhai + TS |
| US | staunch0515/ubos-system | Rust/Rhai + TS |
| LS | staunch0515/logrum-system | Rust/Rhai |
| UW | staunch0515/ubos_web | TS (frontend only) |

## Specification at a glance

| Layer | Chapters |
|---|---|
| L0 Conventions | SPEC-01 glossary, SPEC-02 conventions |
| Architecture | SPEC-10 |
| L1 Store | SPEC-11 storage, SPEC-14 versioning and branching |
| L2 Semantics | SPEC-12 identity and URIs, SPEC-13 meta-model, SPEC-29 base ontology |
| L3 Transactions | SPEC-15 transactions, SPEC-30 audit and observability |
| L4 Runtime | SPEC-16 context, SPEC-17 logic runtime, SPEC-18 rules, SPEC-19 orchestration, SPEC-20 events and jobs, SPEC-21 query |
| L5 Governance | SPEC-22 security, SPEC-23 tenancy |
| L6 Interfaces | SPEC-24 UBTP, SPEC-25 UI protocol, SPEC-26 client shells, SPEC-27 AI |
| L7 Lifecycle | SPEC-28 packages and boot, SPEC-31 deployment, SPEC-32 NFR catalogue |

Current figures (from `40-spec/INDEX/00-index.md`):
- 429 requirements;
- 105 entities;
- 65 interfaces;
- 152 terms;
- 112 NFRs;
- 121 verdicts, all consumed or explicitly deferred.

## Maintenance

- After editing a chapter, run:
  - `python3 docs/tools/check_refs.py` (it must exit 0);
  - `python3 docs/tools/build_index.py`.
- Every change follows [`00-meta/WRITING-RULES.md`](00-meta/WRITING-RULES.md): stable IDs that are never renumbered, English, and files under about 800 lines.
