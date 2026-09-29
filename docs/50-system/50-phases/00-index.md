---
id: UBS-PH-00
title: Phase Plan — Index and Common Rules
status: draft
phase: ALL
depends_on: [UBS-REQ-00, UBS-ARC-00, UBS-SYS-00]
---

# Phase Plan — Index and Common Rules

This volume turns the requirements (UBS-REQ-*), the standard (UBS-STD-*) and the system
designs (UBS-SYS-*) into six phases. Each phase has one purpose, a closed scope, a
verification plan and binary exit criteria. A phase is finished only when every exit
criterion has recorded evidence.

Document IDs: this index is `UBS-PH-00`; the plan of phase N is `UBS-PH-N`; its generated
feature list is `UBS-PHF-N`.

## 1. The six phases

| Phase | Name | Purpose (one sentence) | Business goal (UBS-REQ-01 §7) | Reference duration |
|---|---|---|---|---|
| PH-0 | Foundations and de-risking | Prove the riskiest technical choices and fix the standard's core before building the product. | — (enables G1) | 3 months |
| PH-1 | Kernel and single-node server | Build the reference kernel and a single-node server that pass the standard's core profiles; gates PH-1a (engine core) and PH-1b (governance and services). | G1 prove the mechanism | 9 months (1a: 5, 1b: 4) |
| PH-2 | First verticals and production server | Ship the first sellable product: HA Server, Workspace, Studio, and the fund-operations and contract Buks built without kernel changes. | G2 prove an application | 9 months |
| PH-3 | Regulated pilot, devices and governed AI | Run a regulated design-partner pilot with evidence, add Box devices and offline sync, and add governed AI. | G3 regulated value, G4 governed AI | 9 months |
| PH-4 | Platform scale and ecosystem | Open the platform: multi-tenant Cells, Control Plane, Exchange, certification, workers. | G5 prove the platform | 9 months |
| PH-5 | Federation and the open network | Connect organisations: federation registry, cross-node VAE trees, shared BPUs, browser kernel. | G6 prove federation | 9–12 months |

Durations are reference values for a core team of 8–12 engineers in PH-0 to PH-2, growing
per the team assumption stated in each plan. They are non-normative; exit criteria are
normative.

## 2. Systems by phase

Depth: **P** prototype, **N** new, **E** extended, **H** hardened, **—** not in scope.

| System | PH-0 | PH-1a | PH-1b | PH-2 | PH-3 | PH-4 | PH-5 |
|---|---|---|---|---|---|---|---|
| BPA standard | N | E | E | H (1.0) | E (Sync) | E (certification) | E |
| DVM kernel | P | N | E | E | E | H | E (browser) |
| NOD node | P | N (headless Box, host services) | N (Server single) | E (HA, REST, SSO) | E (Box apps, sync) | E (Cell, worker) | E (QUIC, federation hooks) |
| FRG forge | P (runner) | N | E | E (Buks, simulator, LSP) | E (WASM) | E (publish) | — |
| SDK | — | — | N (Rust, TS) | E | E (Python) | H | — |
| WSP workspace | — | — | P (engineering UI) | N | E (apps, portal, office) | E (Books) | E (local kernel) |
| STU studio | — | — | — | N | E (AI panel) | E | — |
| AGT agent hub | — | — | — | — | N | E | E |
| NTY notary | — | P (verifier library) | — | P (packs, CLI) | N | E | — |
| BRG bridge | — | — | — | N | E | E | — |
| CTL control plane | — | — | — | — | P (CTL-lite) | N | E |
| EXC exchange | — | — | — | — | P (private registry) | N | E (mirrors) |
| FED federation | — | — | — | — | — | — | N |

## 3. Common rules for every phase

### 3.1 Entry
A phase starts when the previous phase's exit criteria are all met, or when the product
owner records a decision (DEC-*) that accepts named open criteria as carried over, each with
an owner and a due gate.

### 3.2 Scope control
The feature list (UBS-PHF-N) is generated from the requirement catalogue. A requirement
moves between phases only by changing its **Phase** field through a recorded decision.
Anything not in the list is out of scope for the phase.

### 3.3 Verification layers
Every phase uses the same layers; later phases add, never remove:
1. **Standard conformance** — BPA vectors through the FRG runner (CONF).
2. **Properties and models** — property tests (PROP), deterministic simulation (SIM),
   formal models (FORM).
3. **Faults** — injected crashes, partitions, storage faults (FAULT).
4. **Scenarios** — end-to-end reference scenarios (SCN) on real deployments.
5. **Non-functional** — benchmarks on reference environments (BENCH), security (SEC),
   usability (USE), inspections (INSP).
6. **Acceptance** — pilots with design partners (PILOT) where the phase goal requires it.

### 3.4 Pass thresholds that apply to every phase
- 100% of conformance vectors of the claimed profiles pass on every supported backend.
- 0 failures in property, simulation and formal-model suites (a failing seed is a blocker).
- 100% of Must requirements of the phase are covered by at least one passing verification
  item (traceability report from 60-verification).
- NR targets of the phase are met on the reference environment named in the NR.
- 0 open critical or high security findings (NR-SEC-001, from PH-2).
- All scenarios of the phase pass on the phase's reference deployment.

### 3.5 Acceptance and records
Exit is decided by the **Phase Acceptance Board**: product owner (chair), engineering lead,
quality lead, security lead and, from PH-2, a design-partner representative. Each exit
criterion is accepted with a link to its evidence (report, signed runner report, benchmark
report, study report or pilot acceptance). The board's decision is recorded as a DEC entry
naming the phase, the release tag and the evidence bundle hash.

### 3.6 Regression policy
From PH-1 onwards, every release candidate re-runs:
- all conformance vectors of all earlier phases;
- the scenario suites of all earlier phases;
- the NR benchmarks of earlier phases, with a regression budget of 10% (a larger regression
  blocks the release unless a decision accepts it).

### 3.7 Carry-over rule
A Should or Could requirement not finished in its phase MAY be carried over by decision. A
Must requirement MUST NOT be carried over; if it cannot be met, the phase does not exit, or
the requirement is re-phased by decision with its reason.

## 4. Conventions for metrics, exits and risks

| Item | Pattern | Rule |
|---|---|---|
| Exit criterion | `EXIT-<N>-<NN>` | binary; names its evidence; PH-1 items state gate 1a or 1b |
| Metric | `MET-<AREA>-<NNN>` | measured value with target; areas PERF, QUAL, SEC, OPS, USE, AI, BIZ, DEL |
| Risk | `RSK-<NNN>` | likelihood and impact (L/M/H), mitigation, early-warning trigger, owner role |

Metric and risk numbers are global across phases; a later phase may reuse an earlier metric
with a new target.

## 5. Files

| Folder | Plan | Feature list |
|---|---|---|
| `PH-0/` | UBS-PH-0 | UBS-PHF-0 |
| `PH-1/` | UBS-PH-1 | UBS-PHF-1 |
| `PH-2/` | UBS-PH-2 | UBS-PHF-2 |
| `PH-3/` | UBS-PH-3 | UBS-PHF-3 |
| `PH-4/` | UBS-PH-4 | UBS-PHF-4 |
| `PH-5/` | UBS-PH-5 | UBS-PHF-5 |
