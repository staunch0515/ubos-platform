---
id: UBS-META-06
title: Inputs and Their Reconciliation
status: stable
phase: PH-0
depends_on: [UBS-META-00]
---

# Inputs and Their Reconciliation

This document records every input that shaped this volume, so that the volume stays
self-contained. Requirements cite these inputs in their **Origin** field.

## 1. Input sources

| ID | Input | What it contributed |
|---|---|---|
| IN-01 | The research volume `docs/10-inventory` … `docs/40-spec`: an analysis of eight peer repositories (four Java/Groovy, three Rust/Rhai, one TypeScript front end) and a unified specification with 429 requirements | The kernel mechanics: versioned entities, branches and overlays, processes and provenance, the logic runtime and syscall ABI, rules and orchestration, events and jobs, security and tenancy, the UBTP protocol, server-driven UI, AI, packages, deployment and NFRs |
| IN-02 | The external documents (§2) | Sovereignty and local-first principles, VAE federation, network fallback, Logic Governance Sheets, the digital immune system, the Office-replacement suite, Buks and the marketplace, cryptographic identity and signatures |
| IN-03 | The twelve improvement proposals (§3), accepted by the product owner | Structural corrections to the data, time, logic, AI, scale and product model |
| IN-04 | The product owner's positioning statement on BPU, DVM and "software logic hardware-ization" (§4) | The architecture framing: standard, engine, unit, toolchain, and the ARM-style business model |
| IN-05 | The multi-system decomposition (§5), accepted by the product owner | The list of systems, their roles and the phase logic |
| IN-06 | The product owner's decisions of 2026-09-29 | DEC-001 … DEC-013 |

## 2. External documents (`EXT-*`)

| Code | Title | Nature | Key content |
|---|---|---|---|
| EXT-WP | *UBOS: Digital Sovereignty Protocol* (Whitepaper v0.4, Dec 2025) | Vision paper | Temporal kernel with an append-only ledger. Code as data. VFS paths. Logic Governance Sheets (CSS-like rules, "old data follows old logic"). Federation with network fallback. A VAE registry. Reactive retry. Office replacement (documents, grid, protocol messages, executable books). The compute grid and intent economy are excluded here. |
| EXT-RFC | *UBOS Protocol Specification: The Sovereign Web* (UBOS-RFC-001 v1.0.0, two editions) | Draft standard | ABNF URI `ubos://vae/category/slug@version`. Entity header `{id, type, ver=SHA-256, ts, sig=Ed25519}`. Commit DAG with zero-copy branches. RFC 8785 canonical JSON. `commit()` with optimistic concurrency. Sandbox limits (3000 ms, 512 MB, no sockets, CONNECTOR assets). Logic asset lifecycle DRAFT → COMMITTED → VERIFIED → DEPRECATED. VAE discovery. UTP over HTTP/3. Handshake and mTLS. A network-fallback state machine. LGS grammar. Conflict strategies (LWW, manual, policy). DID `did:ubos:<key>`. Signed AuthContext. Error codes 0x00–0x30. |
| EXT-TRI | *The UBOS Trilogy: A Blueprint for the Future* | Book outline with chapter text | **Book I**: philosophy (local-first, immutability, federation). Its economic chapters are excluded. **Book II**: kernel design (commit log, data–logic isomorphism, a small kernel), governance (VAE topology, branch inheritance, cascading logic sheets), nervous system (event bus with process IDs, CRON entities, immune system), universal interface (VFS, five syscalls, SDUI). **Book III**: logic assets with `ctx`/`sys`, the commit workflow, Live Contract, Smart Ledger, War Room, Buks, licensing, niche ERPs, the sovereign node. Its compute-miner chapter is excluded. |
| EXT-BP | *Blueprint* (table of contents of EXT-TRI) | Outline | Same structure as EXT-TRI |
| EXT-BPU | The product owner's positioning statements (§4) | Positioning | BPU, DVM, hardware-ization, "ARM architecture of business logic", universal business operating system |

## 3. The twelve accepted improvements (`IMP-*`)

| ID | Improvement | Consequence in this volume |
|---|---|---|
| IMP-01 | **Two kinds of state.** *Definition* state (configuration, master data, rules, types, views) branches and merges. *Ledger* state (transactions, postings, approvals, events) is append-only, never merged, and corrected only by reversal or adjustment. | The `MODEL` and `LEDG` domains, and the class attribute `kind` |
| IMP-02 | **Bitemporality.** Every Definition version carries a valid-time interval in addition to transaction time. Queries accept `asof` (valid time) and `time` (transaction time). | The `TIME` domain |
| IMP-03 | **Content addressing and structural sharing.** Canonical JSON, SHA-256 object IDs, chunked large documents, a Merkle root per commit, deduplication, fast diff and cheap proofs. | `VER` and `PROOF`, and the storage design |
| IMP-04 | **Schema evolution with migration lenses.** Every class change carries a declarative, preferably bidirectional, lens. Old versions are projected on read, and merges align schema versions first. | `MODEL` and `PKG` |
| IMP-05 | **Three-tier logic.** L1 declarative (expressions, decision tables, governance sheets, computed fields, state machines). L2 typed scripts (Rhai, type-checked against the model). L3 WASM components for heavy or third-party logic. | `LOGIC` and `RULE` |
| IMP-06 | **Mature authorization.** An analysable policy language (Cedar-class) plus relationship-based authorization (Zanzibar-class) over the party and role graph. | `IAM` |
| IMP-07 | **Local-first sync.** Merkle-based exchange of missing objects. The kernel compiled to WASM runs in the browser with local SQLite. | `SYNC` |
| IMP-08 | **Cell architecture.** Tenants are placed in cells and can be migrated. Cold history is tiered to object storage. | `OPS` and `TEN`, and the Control Plane |
| IMP-09 | **CQRS projections and an analytics exit.** Declared, rebuildable projections. CDC export to Parquet/Iceberg with versions and lineage. An embedded analytical engine for single-tenant reports. | `ANL` and the Bridge |
| IMP-10 | **One branch per agent.** Agents work in their own branch, simulate, submit a change set for review, and are merged or rolled back. Every class port is exposed as an MCP tool. The meta-model is the agent's world model. | `AI` and the Agent Hub |
| IMP-11 | **Progressive disclosure.** Three product layers (business, builder, platform). Business users never see "branch". | `UX` and the personas |
| IMP-12 | **Open standards.** JSON Schema, OpenAPI, CloudEvents, OpenTelemetry, OIDC, SCIM, MCP, Parquet/Iceberg. | `INT` and `ANL` |

## 4. Positioning (`EXT-BPU`)

The product owner's positioning, restated as design intent:
1. UBOS standardises the **temporal state management** of enterprise data, as GPUs
   standardised graphics processing. Business logic becomes "industrial-grade digital
   circuitry".
2. The goal is an **"ARM architecture of business logic"**: a standard that others
   implement and license, not a single competing application.
3. The core is a **Data Virtual Machine**. It gives data inheritance and polymorphism, as a
   JVM does for code, and native Git-level version control.
4. Every contract and every order is a **BPU**. It records data, embeds a knowledge base
   and validates itself. It can propose its own evolution, and that evolution is governed.

This volume turns "hardware-ization" into six verifiable properties. They are specified
normatively in `30-standard`:

| Hardware property | BPA property |
|---|---|
| Fixed instruction set | A versioned instruction set (ABI). It is only extended, never changed. |
| Clocked state transitions | One commit equals one clock cycle. State changes atomically at commit boundaries. |
| Determinism | The same inputs and the same logic versions produce the same outputs. Time, randomness and external results come from the context and are recorded. |
| Pin-out | A BPU interacts only through typed ports: actions in, events out, queries read. |
| Power and timing budgets | Every execution is metered and has hard limits. |
| Pre-silicon verification | Conformance vectors, a formal model and a simulator are required before release. |

The volume uses the "hardened mechanism, soft policy" framing throughout. Claims of
invention or priority are not part of the specification.

## 5. System decomposition (IN-05)

This decomposition was accepted after removing the excluded items:

| Code | System | Role in one line |
|---|---|---|
| BPA | BPA standard and conformance suite | The instruction-set manual and the certification test vectors |
| DVM | DVM reference kernel | The engine, embeddable as a library, native or WASM |
| FRG | Logic Forge | The compiler, checker, simulator, tester and packager |
| NOD | UBOS Node | The host: Box (personal, desktop), Server (enterprise), Cell (hosted multi-tenant) and the Worker role (enterprise-internal worker pool) |
| CTL | Control Plane | Manages nodes, cells, tenants, licences, upgrades, metering. It never reads business data. |
| WSP | Workspace | The end-user product: tasks, forms, Live Doc, Smart Grid, War Room, Action Message |
| STU | Studio | The builder product: modelling, governance sheets, lifecycles, views, change-set review, Buk authoring |
| AGT | Agent Hub | The AI gateway, agent identities and budgets, agent branches, MCP, copilot, evaluation |
| EXC | Buk Exchange | The package registry, verification badges, licensing and fiat billing |
| FED | Federation | VAE registry, DID, secure transport, Merkle sync, the VAE tree, shared BPUs across organisations |
| NTY | Notary | Anchoring, evidence packs, verifier tools, regulator views |
| BRG | Bridge | Connectors, import, CDC export, analytics exit |
| SDK | Client SDKs | The shared TypeScript and Rust client libraries used by all clients |

## 6. Reconciliation of conflicting inputs

When inputs disagree, this volume applies the resolution below. The resolutions are
normative.

| Topic | EXT-* position | IN-01 position | Resolution in this volume |
|---|---|---|---|
| Data validation | Schema-on-read, no kernel enforcement | Strong meta-model, validated on commit | **Validated on commit.** A class `RawDocument` allows schema-on-read explicitly. |
| Conflict handling | Last-write-wins by default for data | CAS on heads, all conflicts reported | **CAS plus reported conflicts.** Definition state uses a three-way merge. Ledger state has no merge. LWW applies only to fields that declare it. |
| Entity kinds | Fixed enum LOGIC/DATA/VIEW/CONFIG | Open type system, types are entities | **Open class system.** The four become base classes under the root. |
| Implementation stack | Java, Groovy, R2DBC, Electron, embedded PostgreSQL | Rust, Rhai, sqlx, Tauri, SQLite/PostgreSQL | **The standard is language-neutral. The reference is Rust.** Groovy is not supported in the reference runtime. WASM covers polyglot logic. |
| Sandbox limits | Fixed 3000 ms and 512 MB | Configurable profiles | **Profiles with standard defaults.** The EXT values are the default of profile `standard`. |
| Addressing | `ubos://vae/category/slug@version` | `ubos://tenant/Type/slug?selectors` | **Class-based canonical URI.** VFS category paths are aliases (a view). |
| Syscalls | Five: commit, readSnapshot, runLogic, publishEvent, search | A larger ABI | **Seven instruction families**: commit, read, run, emit, search, call, ask. Each has versioned instructions. |
| Signatures | Ed25519 on every commit | Not present | **Adopted.** Commits are signed by the author's key. A DID identifies the principal. |
| Storage tables | — | "Six tables only" | **Withdrawn** (DEC-011). Tables are designed per need and documented. |
| Rules | LGS/CLS selector sheets | Decisions, derived schemas, invariants | **LGS is the L1 declarative layer.** It includes selectors, cascade, specificity, explanations and hot attach/detach. |
