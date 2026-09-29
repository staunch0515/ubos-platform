---
id: UBS-META-00
title: Charter of the System Specification Volume
status: stable
phase: PH-0
depends_on: []
---

# Charter of the System Specification Volume

## 1. Purpose

This volume (`docs/50-system/`) is the **authoritative specification** of the UBOS
Business Processing Architecture and the family of software systems built on it.

It is written for two readers:
1. **AI implementers and reviewers.** They are the primary audience. Every statement is
   structured, identified and cross-referenced so that an AI can locate, cite and verify
   it without reading the whole volume.
2. **Human architects, product owners and auditors.** They read the requirement
   catalogues, the phase plans and the verification plans.

The volume is **self-contained**. A reader does not need the older volume `docs/40-spec/`,
the research files in `docs/10-inventory/` … `docs/30-comparison/`, or any chat history.
Everything this volume depends on is summarised in `UBS-META-06` (inputs).

## 2. What is being specified

**UBOS** is a *business processing architecture*: a published standard, a reference
kernel, a standard business unit and a toolchain, together with the products and network
services that run on them.

| Layer | Name | One-line definition |
|---|---|---|
| Standard | **BPA** (Business Processing Architecture) | The public, language-neutral specification of the Data Virtual Machine, the Business Processing Unit, the instruction set, the governance-sheet language, addressing, protocols and error codes, plus its conformance suite. |
| Engine | **DVM** (Data Virtual Machine) | The reference implementation of the BPA semantics: typed, inheritable, polymorphic, versioned, bitemporal, provable business state, and a sandboxed deterministic logic runtime. |
| Unit | **BPU** (Business Processing Unit) | The standard business unit. Every contract, order, policy or ticket is a BPU instance with seven elements: class, state, lifecycle, ports, governance binding, knowledge and proof. |
| Toolchain | **Logic Forge** | The compiler, checker, simulator, test runner and packager that turn business knowledge into verified BPU classes and Buks (packages). |
| Products and services | Node, Control Plane, Workspace, Studio, Agent Hub, Buk Exchange, Federation, Notary, Bridge | The systems that host, operate, use, build, extend, connect and prove BPUs. |

The guiding principle is **"hardened mechanism, soft policy"**:
- The *mechanism* is standardised, deterministic and verifiable, like a processor's
  instruction set. It covers how state changes, how versions are recorded, how access is
  decided and how execution is metered.
- The *policy* stays fully changeable as versioned data. It covers business types, rules,
  flows and views.

## 3. Scope

### 3.1 In scope

1. A complete **requirements volume**: vision, personas, capability map, end-to-end
   scenarios, functional requirements, non-functional requirements, compliance
   requirements, constraints and assumptions.
2. The **architecture** of the system family: system boundaries, contracts between
   systems, deployment topologies and data flows.
3. The **BPA standard**: normative semantics, grammars, schemas, instruction set and
   error registry.
4. One **system specification** per system: purpose, functions, interfaces, data,
   state machines, configuration and operations.
5. The **phase plan**: six phases (PH-0 … PH-5), each with a goal, scope, features,
   dependencies, exclusions, deliverables, a verification plan, metrics, exit criteria and
   risks.
6. The **verification volume**: methods, suites, environments, datasets, thresholds and
   the traceability from requirement to verification item to phase.
7. The **decision log** (DEC-*) and generated **indexes**.

### 3.2 Explicitly out of scope

The following long-term narratives from the input documents are **excluded**. They MUST
NOT appear as requirements, features, phases or design elements in this volume:

| Excluded topic | Reason |
|---|---|
| "The end of the corporation", protocol-as-CEO, algorithmic governance of organisations | Long-term narrative, not a product capability |
| Intent economy, reverse marketplace for consumers, rebate or "bribe" advertising model | Long-term narrative, not a product capability |
| Compute as currency, UBOS tokens (UBT), energy-backed money, compute mining | Long-term narrative, high regulatory risk |
| Public open compute grid (public "Gaia Grid") | Depends on the excluded economic model |

Permitted and related items that remain **in scope**:
- Licensing, metering and billing in **fiat currency** (Buk Exchange, Control Plane).
- An **enterprise-internal worker pool**: an organisation runs sandboxed jobs on its own
  machines. There is no money and no public participation (DEC-002).

## 4. Principles for this volume

1. **Requirements before design.** The requirements volume is written and confirmed by
   the product owner before the architecture, standard and system chapters (DEC-010).
2. **Verification is first-class.** Every requirement names its verification methods.
   Every phase has a verification plan and measurable exit criteria.
3. **Storage is designed, not dogmatic.** The earlier "six tables only" rule is withdrawn
   (DEC-011). The kernel uses as many system tables as the design justifies, and each table
   is documented with its purpose. Business types remain metadata by default. Typed
   projection tables are allowed when they are declared and can be rebuilt.
4. **Language-neutral standard, Rust reference implementation.** The BPA is independent of
   implementation language. Any implementation that passes the conformance suite is
   compliant. The official implementation uses Rust (DEC-007).
5. **English only** in the volume. Chat reports to the product owner are in Chinese.

## 5. Structure of the volume

| Folder | Content | Document ID prefix |
|---|---|---|
| `00-meta/` | charter, writing rules, templates, progress, inputs, AI reading guide | `UBS-META-NN` |
| `10-requirements/` | vision, personas, capabilities, scenarios, FR, NR, CR, constraints | `UBS-REQ-NN` |
| `20-architecture/` | system family, boundaries, contracts, topologies, data flows | `UBS-ARC-NN` |
| `30-standard/` | the BPA standard (normative) | `UBS-STD-NN` |
| `40-systems/<sys>/` | one folder per system | `UBS-SYS-<SYS>-NN` |
| `50-phases/` | PH-0 … PH-5 plans | `UBS-PH-N` |
| `60-verification/` | methods, suites, environments, traceability | `UBS-VER-NN` |
| `90-decisions/` | decision records DEC-* | `UBS-DEC-NNN` |
| `INDEX/` | generated indexes (do not edit by hand) | `UBS-IDX-NN` |

## 6. Confirmed product-owner decisions

The product owner accepted all proposals on 2026-09-29. They are recorded as DEC-001 …
DEC-013 in `90-decisions/000-index.md`. In summary:
- the location and title of the volume;
- the internal worker pool is kept;
- US-first compliance;
- the first vertical is fund and asset-management back office plus contract management;
- six phases;
- reference durations with stated team assumptions;
- the technology stack;
- the older volume is kept as history;
- reporting is per batch;
- the writing order puts requirements first.
