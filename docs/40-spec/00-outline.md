---
id: SPEC-00
title: "Specification Outline"
status: complete
phase: P3
depends_on: [CMP-00, SPEC-02]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Specification Outline — UBOS Universal Business Platform

## 1. Purpose of the specification

The specification (`40-spec/`) defines **one** target platform, synthesised from the eight
analysed repositories through the 121 verdicts of P2. It is written primarily for **AI
implementers**. Each chapter is self-contained after reading its `depends_on`, uses stable
IDs (REQ, ENT, API, NFR, TERM, ADR), and gives every requirement an `Origin` (VRD, HL, CON
or NEW).

**Target in one sentence:** a Rust micro-kernel with the following properties:
- It stores every business artifact (data, types, logic, UI, rules, governance) as versioned, branchable, tenant-scoped entities in six tables.
- It runs sandboxed Rhai logic behind a versioned syscall ABI inside audited processes.
- It is driven by metadata (types, actions, views, packages).
- It serves five clients (web, desktop, mobile, CLI, SDK/agent) over one protocol (UBTP), with AI acting through the same governed paths as people.

## 2. Architecture overview (layers)

| Layer | Contents | Chapters |
|---|---|---|
| L0 Conventions | Glossary, IDs, naming, notation, error model | SPEC-01, SPEC-02 |
| L1 Store | Six tables, store traits, editions, versioning primitives | SPEC-11, SPEC-14 |
| L2 Semantics | Identity/URI, meta-model, types, slots, relationships, inheritance resolution, ontology | SPEC-12, SPEC-13, SPEC-29 |
| L3 Transactions | Process, session (unit of work), change sets, CAS, idempotency, provenance, audit | SPEC-15, SPEC-30 |
| L4 Runtime | Context, logic runtime and syscalls, rules, orchestration, events and jobs, query | SPEC-16 … SPEC-21 |
| L5 Governance | Security, tenancy | SPEC-22, SPEC-23 |
| L6 Interfaces | UBTP protocol, UI protocol, client shells, AI | SPEC-24 … SPEC-27 |
| L7 Lifecycle | Packages/boot/genesis, deployment/editions, non-functional catalogue | SPEC-28, SPEC-31, SPEC-32 |

Textual form of the dependency graph (each chapter depends only on chapters listed before it):
`02 → 11 → 12 → 13 → 14 → 15 → 16 → 17 → 18 → 19 → 20 → 21 → 22 → 23 → 24 → 25 → 26 → 27 → 28 → 29 → 30 → 31 → 32`.
Exact per-chapter dependencies are in §3.

## 3. Chapter list

`MOD` = module code used in `REQ-<MOD>-NNN` and `API-<MOD>-NNN` (registry in SPEC-02).

| ID | File | Title | MOD | Depends on | Main verdicts | Priority |
|---|---|---|---|---|---|---|
| SPEC-01 | `01-glossary/` (index + 3 parts) | Glossary | – | – | all | P3 |
| SPEC-02 | `02-conventions.md` | Conventions and ID registry | CONV | SPEC-01 | VRD-22-01, 22-04 | P3 |
| SPEC-10 | `10-architecture.md` | Architecture overview | ARCH | 01, 02 | VRD-19-01…03, 20-01…05 | P0 |
| SPEC-11 | `11-storage/` (index + 3 parts) | Storage model and store traits | STO | 10 | VRD-01-01…06 | P0 |
| SPEC-12 | `12-identity-uri.md` | Identity and `ubos://` addressing | URI | 11 | VRD-03-01…05 | P0 |
| SPEC-13 | `13-meta-model/` (index + 3 parts) | Meta-model, types, properties, slots, relationships, inheritance | META | 11, 12 | VRD-02-01…07 | P0 |
| SPEC-14 | `14-versioning-branching.md` | Commits, heads, branches, merge, revert, diff | VER | 11, 12, 13 | VRD-08-01…06, 01-03, 01-04 | P0 |
| SPEC-15 | `15-transactions.md` | Processes, session, change sets, CAS, idempotency, provenance | TX | 13, 14 | VRD-04-01…05 | P0 |
| SPEC-16 | `16-context.md` | Context entity and execution context | CTX | 12, 15 | VRD-07-01…05 | P0 |
| SPEC-17 | `17-logic-runtime.md` | Logic entities, Rhai runtime, syscall ABI, sandbox, native handlers | RT | 15, 16 | VRD-05-01…06 | P0 |
| SPEC-18 | `18-rules-validation.md` | Derived schemas, levels, constraints, invariants, decisions | RULE | 13, 17 | VRD-09-01…06 | P0 |
| SPEC-19 | `19-orchestration/` (index + 3 parts) | Pipelines, action state machines, process instances | FLOW | 17, 18 | VRD-06-01…06 | P0 |
| SPEC-20 | `20-events-jobs.md` | Outbox events, hooks, cron, jobs, webhooks | EVT | 15, 17, 19 | VRD-11-01…05 | P1 |
| SPEC-21 | `21-query-search.md` | Derived index, criteria queries, history, vectors | QRY | 11, 13, 14 | VRD-12-01…05 | P0 |
| SPEC-22 | `22-security.md` | Identity, governance entities, RBAC + policies, enforcement, approvals, keys | SEC | 13, 15, 17, 18 | VRD-10-01…07 | P0 |
| SPEC-23 | `23-tenancy.md` | Tenants, root package tenant, extension and overlay, cross-tenant execution | TEN | 14, 16, 22 | VRD-18-01…05 | P0 |
| SPEC-24 | `24-protocol.md` | UBTP messages, envelope, transports, encoding | PROTO | 15, 19, 20, 21 | VRD-15-01…05 | P0 |
| SPEC-25 | `25-ui-protocol.md` | Widget resolution, render pipeline, views, projection, pushed UI | UI | 13, 19, 24 | VRD-13-01…07 | P1 |
| SPEC-26 | `26-client-shells.md` | Cockpit, studio, desktop, mobile, CLI, SDK | SHELL | 24, 25 | VRD-14-01…04, 19-04 | P1 |
| SPEC-27 | `27-ai.md` | AI builder, AI syscalls, agents, copilot, explanations, i18n | AI | 17, 21, 22, 24 | VRD-16-01…07 | P1 |
| SPEC-28 | `28-packages-boot.md` | Packages, loader, genesis, boot sequence | PKG | 13, 15, 18 | VRD-17-01…05 | P0 |
| SPEC-29 | `29-base-ontology.md` | Base domain library (semantic types and universal ontology) | ONT | 13, 28 | VRD-02-06 | P1 |
| SPEC-30 | `30-audit-observability.md` | Audit record, lineage, trace tree, analytics, telemetry | OBS | 15, 16 | VRD-21-01…05 | P0 |
| SPEC-31 | `31-deployment-editions.md` | Crates/hosts, editions, topology, five clients, sync | DEP | 10, 24 | VRD-19-01…05 | P1 |
| SPEC-32 | `32-nfr-catalogue.md` | Non-functional requirements catalogue | NFR | all | CMP-01 scale notes, CON-UC-035 | P1 |
| SPEC-90 | `90-adr/` | Architecture decision records | ADR | – | – | – |

Priority means the order in which P4 writes the chapters (P0 first). It also marks the
minimal viable kernel.

## 4. Chapter content plans

Each P4 chapter follows TEMPLATES §S. The plans below fix what each chapter MUST contain.

| ID | Required content |
|---|---|
| SPEC-10 | Layer model; crate/module map (`ubos_proto`, `ubos_store`, `ubos_kernel`, hosts, `ubos_sdk`); runtime roles (gateway, worker, scheduler, dispatcher, database, runtime store); request lifecycle (sequence list from protocol message to commit and events); editions summary |
| SPEC-11 | ENT for six tables with full columns (v2.1 + additions); partitions; index rules; store operation traits (API-STO-*); edition differences; runtime store contract (ADR-004) |
| SPEC-12 | UUID generation, locator rules, slug regex and dotted namespaces, rename/copy; URI grammar (ABNF), selectors, normalisation, conformance table; context authority resolution |
| SPEC-13 | Meta root and meta type; type definition schema (UODS + metadata spec); property definition; semantic primitives; `relation` properties; Relationship entity and RelationshipType catalogue; behaviour slot catalogue with I/O contracts; inheritance resolution algorithm (assemble, compose, resolve, execute); effective vs own data; commit-keyed caches |
| SPEC-14 | Commit semantics; head CAS and height; branch entity and kinds; overlay read/write/delete (masking tombstone); drafts; merge strategies, ancestor search, path conflicts; revert and reset; diff (JSON Patch); tombstones and status |
| SPEC-15 | Process ENT and lifecycle states; session (identity map, states); ChangeSet ENT and actions; flush algorithm; CAS; idempotency (runtime store, TTL); provenance relation types; dry run |
| SPEC-16 | Context ENT (sovereign/host tenant, branch or pin, env vars, memory, policies); resolution; execution context structure; identity propagation; memory persistence; system identity |
| SPEC-17 | Logic ENT; Rhai engine configuration; syscall namespaces v1 (signatures, errors, policy checks); sandbox limits; compile cache; module import by URI; native handlers registry; execution modes (sync, job, dry run); emits |
| SPEC-18 | Schema derivation from types; validation levels; constraint kinds; invariant language (Rhai subset, ADR-002); Decision model and combination; guard evaluation; error format with paths |
| SPEC-19 | Pipeline ENT and engine; Action type hierarchy, states, transitions, stage steps; transition response; ProcessInstance ENT and signals; emits dispatch; intent mapping |
| SPEC-20 | Outbox ENT; event catalogue; Hook, Cron, Webhook ENTs; Job ENT and states; worker pool; retries, dead letter; client subscriptions |
| SPEC-21 | Index derivation; criteria query shape; relationship joins; `as_of`; history, blame, diff, lineage queries; named query pipelines; embeddings and similarity |
| SPEC-22 | User/Group/Role/Policy/ApiKey/Webhook/ApprovalRequest ENTs; authentication (tokens, keys); authorisation algorithm; enforcement points; masking on read; approval flow; logic authorship governance; custody fields |
| SPEC-23 | Tenant ENT; root tenant `logrums`; namespaces; overlay of root package; resolution order; cross-tenant rules; RLS (enterprise) |
| SPEC-24 | Message schemas (Handshake, Mutate, Emit, Query, Subscribe, Unsubscribe; Ack, Result, Op, Error); request ids; error codes; transports (TCP framing, WebSocket, HTTP routes); encoding negotiation; session semantics; versioning of the protocol |
| SPEC-25 | Modes; widget resolution order; Widget ENT; UI matrix; render pipeline; payload format; View ENT and layout types; projection and merge; action binding; pushed UI ops; realtime deltas (P2 priority) |
| SPEC-26 | Web cockpit (zones, hierarchy, omnibar, intent grammar, dock); builder studio features; desktop (Tauri + embedded kernel); mobile (reduced cockpit: dock, tasks, approvals, search); CLI (verbs, scenarios); SDK (protocol client); IDE file-system integration as tooling |
| SPEC-27 | AiProvider ENT; AI syscalls; builder loop (bundle, dry run, draft, approval); agent identities; copilot contract; explanation from traces; AI i18n |
| SPEC-28 | Package ENT and manifest; package file format; loader algorithm; boot sequence and strategies; development hot reload |
| SPEC-29 | Semantic type layers and codes; ontology areas (party/role/relationship, contact/geo, reference data, documents/messages, security, IT, kernel logic, measures, PKI, UI); normalisation rules from UB/UC genesis data |
| SPEC-30 | Audit record; lineage; trace tree limits and offload; history analytics; telemetry (spans, request log, metrics) |
| SPEC-31 | Crate layout; hosts; editions; topology roles; the five clients; sync (later version) |
| SPEC-32 | NFRs: performance, scalability, availability, durability, security, privacy, portability, AI-readiness, operability, with measurable targets |

## 5. Minimal viable kernel (implementation slice 1)

The P0 chapters, restricted to one tenant, one edition (PostgreSQL) and the HTTP binding of UBTP:
- SPEC-11, 12, 13, 14 (without merge), 15, 16, 17, 18 (without invariants), 19 (pipelines and action state machines), 21 (criteria queries), 22 (RBAC + policies), 24, 28, 30.

Slice 2 adds:
- merge, jobs and events, UI protocol, cockpit, the SQLite edition and AI.

## 6. Traceability rule

Every verdict VRD-NN-MM MUST be consumed by at least one chapter, or listed as deferred in
that chapter's §8. SPEC-90 records decisions that change a verdict.
