---
id: UBS-ARC-00
title: Architecture Volume — Index
status: draft
phase: ALL
depends_on: [UBS-REQ-00, UBS-META-06]
---

# Architecture Volume — Index

The architecture volume describes **how the system family is structured** to satisfy the
requirements volume. It covers:
- the systems and their boundaries;
- the contracts between systems;
- the deployment topologies;
- the data architecture;
- the main runtime flows;
- the cross-cutting concerns;
- the codebase organisation.

Normative content appears as:
- **architecture rules** (`AR-*`), binding on every system chapter;
- **inter-system contracts** (`CTR-*`), detailed as `IF-*` items in the system chapters (DEC-017).

## Documents

| ID | File | Content |
|---|---|---|
| UBS-ARC-01 | `01-system-family.md` | layers, the 13 systems, responsibilities, non-responsibilities, phase introduction, layering rules |
| UBS-ARC-02 | `02-contracts.md` | inter-system contracts CTR-* with guarantees |
| UBS-ARC-03 | `03-deployment-topologies.md` | Box, Server (single and HA), managed Server, Cell, Worker, browser, federation |
| UBS-ARC-04 | `04-data-architecture.md` | authoritative model, logical table inventory, indexes, key hierarchy, data lifecycle |
| UBS-ARC-05 | `05-runtime-flows.md` | end-to-end flows: commit, read, merge, event, schedule, agent, install, sync, anchor, fallback, erasure |
| UBS-ARC-06 | `06-cross-cutting.md` | security, identity, error model, observability, configuration, tenancy, performance, resilience, AI governance, evolution |
| UBS-ARC-07 | `07-codebase-and-delivery.md` | repositories, crates and packages, dependency rules, build, release trains, CI gates |
| UBS-ARC-08 | `08-capability-allocation.md` | the 220 capabilities allocated to primary and supporting systems |

## How the architecture traces to requirements

- Every `AR-*` rule lists the FR, NR or CR items it realises.
- Every `CTR-*` contract lists the capabilities it serves.
- `UBS-ARC-08` allocates each capability to one **primary system**, which is accountable
  in its system chapter, and to supporting systems.
- System chapters (`40-systems/`) refine the architecture into `DSN-*`, `IF-*` and `DAT-*`
  items.
