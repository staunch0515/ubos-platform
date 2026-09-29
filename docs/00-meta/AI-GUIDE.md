---
id: META-AI-GUIDE
title: Guide for AI Readers
status: stable
phase: P5
depends_on: [META-CHARTER, SPEC-00, SPEC-02]
---

# Guide for AI Readers

This guide is the canonical instruction file for AI agents. Implementation repositories
copy §2–§5 into their `AGENTS.md` (and `CLAUDE.md`), as required by REQ-CONV-088 and
VRD-22-01.

## 1. If you are maintaining this documentation

1. Read `00-meta/PROGRESS.md`, then `DOC-CHARTER.md`, `WRITING-RULES.md` and `TEMPLATES.md`.
2. Change the specification only through the owning chapter. Decisions that change a verdict or a chapter's basis need an ADR in `40-spec/90-adr/`.
3. Never renumber or reuse an ID (REQ-CONV-004). Mark withdrawn items as withdrawn.
4. After every change, run the two tools below. The first MUST exit 0.
   ```
   python3 docs/tools/check_refs.py
   python3 docs/tools/build_index.py
   ```
5. Log the change in `PROGRESS.md`. Commit with `docs(<phase>): <what>`.

## 2. If you are implementing the platform: what to read

1. `40-spec/00-outline.md`: the chapter map, layers, the minimal viable kernel (§5) and the build order.
2. `40-spec/01-glossary/`: use these terms and no synonyms (REQ-CONV-072).
3. `40-spec/02-conventions.md`: naming, IDs, time, types, payloads, the error model, the six-table rule, code rules.
4. `40-spec/90-adr/`: the accepted decisions (stack, rule language, clients, technical state, URI authority, mobile).
5. For the module you build: its chapter plus every document in its `depends_on`. Chapters split into folders start at `00-index.md`.
6. Look up any ID in `40-spec/INDEX/`.
7. When intent is unclear, follow the item's `Origin` to `30-comparison/` (the verdict and its reasoning), then to `20-analysis/` (the observed design and source file references), then to the source repository.

## 3. Build order and definition of done

Build in this order and verify each step before the next (VRD-22-03):

| Step | Build | Chapters | Done when |
|---|---|---|---|
| 1 | `ubos_proto`: URI library, message types, error object | SPEC-02, 12, 24 (§2) | URI conformance table C-01…C-49 passes (Rust and TS) |
| 2 | `ubos_store`: six tables, traits, PG + SQLite, runtime store, blob store | SPEC-11 | store conformance suite passes on both editions |
| 3 | semantics: type registry, meta-schema, inheritance resolution, caches | SPEC-13 | META acceptance table; genesis `system` bootstrap types validate |
| 4 | versioning and transactions: heads, branches, session, flush, CAS, idempotency, provenance | SPEC-14, 15 | flush fault injection leaves no partial state; CAS stress test passes |
| 5 | context and logic runtime: Rhai engine, profiles, syscall ABI v1, natives | SPEC-16, 17 | limit tests; the ABI published; the example logic runs |
| 6 | rules and orchestration: derived schemas, constraints, invariants, decisions, pipelines, actions, lifecycles | SPEC-18, 19 | the invoice scenario (approve with guards) passes |
| 7 | security, query, tenancy: PDP, masking, criteria queries, tenant layering | SPEC-22, 21, 23 | isolation suite and PDP property tests pass |
| 8 | protocol, packages, boot, audit | SPEC-24, 28, 30 | genesis from embedded packages; scenarios run over all bindings |
| 9 | slice 2: events and jobs, UI protocol, web cockpit, AI, deployment | SPEC-20, 25, 26, 27, 31 | the chapter acceptance tables |

A requirement is **done** when every item in its `Acceptance` passes as an automated test or
scenario, and when the NFRs of its chapter meet their targets on the reference
environment (SPEC-32).

## 4. Rules that are easy to get wrong

1. **Only the flush writes the six tables** (REQ-ARCH-003). Never add tables for business or metadata state: a new concept is a type (REQ-CONV-060).
2. **Snapshots store own data only**; effective data is computed (REQ-META-020/022). Absent means inherit, and `null` means cleared (REQ-CONV-041).
3. **Every write names its base.** CAS on the head, all conflicts reported together (REQ-TX-005, REQ-TX-011).
4. **Scripts never pass identity or tenant.** The execution context supplies them (REQ-CTX-006).
5. **Logic reaches the world only through syscalls and emits.** Limits and cancellation are not catchable (REQ-RT-009).
6. **Deny by default.** A resource that is not visible is reported as not found (REQ-CONV-052). Every enforcement point calls the PDP (REQ-SEC-020).
7. **Stored references are canonical URIs with the tenant authority and no selectors** (SPEC-12 §1.3). Short forms are only for input.
8. **Time comes from `ctx.now`.** Random and external results are recorded (REQ-RT-010).
9. **Jobs are entities, queues are hints.** The sweeper must recover everything after a runtime-store loss (ADR-004, REQ-EVT-026).
10. **AI output never bypasses validation, drafts or approval** (REQ-SEC-051, REQ-AI-010).
11. **Clients use UBTP only** and render server payloads. No business rules live in clients (VRD-14-04, REQ-CONV-086).
12. **English in code**, US spelling in identifiers, glossary terms only (REQ-CONV-070…072).

## 5. Code conventions (summary of SPEC-02 §11)

- Rust stable, edition 2021+, Tokio, axum, sqlx, Rhai (`sync`, `serde`, `decimal`), `thiserror` in libraries, `tracing` for logs. Code must be `cargo fmt` and `clippy -D warnings` clean.
- Crates: `ubos_proto`, `ubos_store`, `ubos_runtime`, `ubos_kernel`, `ubos_server`, `ubos_cli`, `ubos_desktop`, `ubos_sdk`. Hosts contain no business logic.
- TypeScript strict; React function components; zustand for UI state only; server state only through `@ubos/sdk`.
- Source files SHOULD stay under 400 lines. Commits follow Conventional Commits with the MOD as scope, e.g. `feat(VER): three-way merge`.
- Error codes are `<MOD>.<UPPER_SNAKE>` with the SPEC-02 §8 shape. Renaming a code is a breaking change.

## 6. Delivering features on the platform (not in the kernel)

Business features are **packages** (SPEC-28). Each package contains:
- types, lifecycles, actions, pipelines, logic (`.rhai`), views and seed data;
- **scenarios** as acceptance tests.

Build and check them with `ubos pkg new`, `ubos pkg validate`, `ubos pkg install --dry-run` and `ubos pkg verify`. Extend the base ontology (SPEC-29) instead of redefining parties, money or addresses.
