---
id: ADR-001
title: "ADR-001: Technology stack"
status: complete
phase: P3
depends_on: [CMP-20, CMP-19, SPEC-02]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# ADR-001: Technology stack

- **Status:** accepted (2026-09-28). The user delegated the choice (Q-001).

## Context

- The corpus has two backend stacks and one front-end stack (CMP-20).
  - **J:** Java 17, Spring Boot WebFlux, R2DBC and Groovy 4 (UP, FU, UB); LC has no scripts.
  - **R:** Rust, Tokio, axum, sqlx and Rhai (UC, US, LS).
  - **F:** React, TypeScript and Vite in every UI repository.
- The stated platform goals require:
  - one core with five clients (ADR-003);
  - an embeddable personal edition on SQLite (VRD-19-02);
  - a binary-capable protocol over TCP (VRD-15-02);
  - a sandbox in which AI-written logic runs safely (VRD-05-03).
- The Java repositories hold the richest governance, merge and pipeline designs (FU, UB, LC). All of them are designs over the same six-table model (VRD-01-01) and port to any language.

## Options considered

### Option A — Java / Spring WebFlux / Groovy (stack J)
- **Pros:**
  - Mature enterprise ecosystem (JSON Schema, JWT, mail, IAM).
  - The largest share of the governance features is already written in it (FU, UB, LC).
  - Groovy is a powerful script language that many developers know.
- **Cons:**
  - The JVM is heavy on desktops, mobile companions and the edge; the embedded edition is weak.
  - Reactive (Reactor) code adds cognitive load (CON-UP-002).
  - A Groovy sandbox needs AST restrictions and interrupts to reach the containment Rhai has by default (CON-UP-022).
- **Sources:** [CMP-20 §3], CON-UP-001, CON-UP-022, CON-FU-001, CON-LC-001.

### Option B — Rust / Tokio / Rhai (stack R)
- **Pros:**
  - Native binaries that other programs can embed: Tauri desktop, CLI, edge.
  - The SQLite edition is proven (CON-LS-006).
  - Rhai exposes only registered functions and enforces operation, depth, size and time limits. This matches the syscall ABI (VRD-05-02).
  - The syscall ABI, jobs, anchors, sessions, the protocol and editions were built in this stack (UC, US, LS).
  - Performance and memory use are the best of the options.
- **Cons:**
  - Async Rust with synchronous scripts needs a bridge (CON-UC-005).
  - Rhai is less expressive than Groovy.
  - The Java-only features must be re-specified and reimplemented.
- **Sources:** [CMP-20 §3], CON-UC-004, CON-UC-005, CON-US-001, CON-LS-006, CON-LS-062.

### Option C — Hybrid (Java server, Rust embedded edition)
- **Pros:** reuses both code bases.
- **Cons:**
  - Two kernels must stay behaviourally identical, which contradicts "one core".
  - Every rule would be implemented twice.
  - The protocol and URI conformance burden doubles (VRD-03-05).
- **Sources:** VRD-19-01.

## Decision

Option B. The stack is:

| Layer | Choice | Notes |
|---|---|---|
| Kernel language | Rust stable, edition 2021+ | Cargo workspace, crates per SPEC-02 §11.1 |
| Async runtime | Tokio | |
| HTTP / WebSocket binding | axum | UBTP HTTP and WebSocket bindings (SPEC-24) |
| TCP binding | tokio-util codec, length-prefixed frames | SPEC-24 |
| Serialisation | serde, serde_json; binary encoding negotiated (MessagePack first candidate) | VRD-20-05, VRD-15-02 |
| Database access | sqlx | store traits, two backends (VRD-01-05) |
| Enterprise database | PostgreSQL 16 + pgvector | JSONB, partitions, vector index |
| Personal database | SQLite 3 (WAL mode) | TEXT JSON, no partitions, vectors degraded (SPEC-31) |
| Runtime store (enterprise) | Redis 7 | queue signals, cache, idempotency keys, rate limits, memory cache (ADR-004) |
| Runtime store (personal) | in-process equivalents | same trait |
| Script language | Rhai with `sync` + `serde` features | full language for Logic; restricted subset for rules (ADR-002) |
| JSON Schema | `jsonschema` crate | derived schemas and the meta-schema (SPEC-13, SPEC-18) |
| Auth tokens | `jsonwebtoken` | SPEC-22 |
| Telemetry | `tracing` + OpenTelemetry exporter | SPEC-30 |
| CLI | `clap` | SPEC-26 |
| Front end | React 19 + TypeScript (strict) + Vite | |
| UI state | zustand; server state only through the TS SDK | SPEC-02 §11.2 |
| Components and styling | Tailwind CSS + shadcn/ui (Radix primitives) | chosen over antd, matching the newest shells (UW, UC) |
| Code editor | Monaco with a Rhai language definition | CON-US-056 |
| Command palette | cmdk | omnibar (SPEC-26) |
| Desktop | Tauri 2, wrapping the web shell with an embedded kernel | personal edition |
| Mobile | Tauri 2 mobile targets or a React Native shell over the TS SDK; the choice is made in SPEC-26 | ADR-003 |

## Consequences

- P4 specifies every feature in language-neutral terms (ENT, REQ, API). Rust trait signatures are given for kernel interfaces.
- The following features are re-specified from the Java sources and implemented in Rust: FU governance, UB merge/diff/revert, LC pipelines, constraints and idempotency.
- One URI library and one protocol library are written in Rust and TypeScript and contract-tested (VRD-03-05).
- The `language` field of Logic stays open, so other languages (for example WASM modules) can be added later without a new ADR for the kernel (VRD-05-01).
- Java code in the corpus is reference material only.

## Origin

Q-001, VRD-20-01, VRD-20-02, VRD-20-03, VRD-20-04, VRD-20-05, VRD-19-01, VRD-19-02, HL-UC-003, HL-LS-004, HL-US-009.
