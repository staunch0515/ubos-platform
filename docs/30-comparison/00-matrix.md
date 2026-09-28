---
id: CMP-00
title: "Comparison Matrix and Topic List"
status: complete
phase: P2
depends_on: [INV-SUMMARY, ANA-UP-09, ANA-FU-09, ANA-LC-09, ANA-UB-09, ANA-UC-09, ANA-US-09, ANA-LS-09, ANA-UW-09]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Comparison Matrix and Topic List

## 1. Fixed topic list

The 20 preliminary topics of INV-SUMMARY §6 are kept. Two more are added because P1 showed
them as independent design axes:
- 21 **Audit, lineage and observability** was scattered across topics 1, 4 and 7.
- 22 **Development method and AI-assisted engineering** is present in 7 of the 8 repositories and matters because the platform is meant to be built and extended by AI.

| # | File | Topic | Feeds spec area (P3 outline, provisional) |
|---|---|---|---|
| 01 | `01-storage-model.md` | Versioned storage model | Persistence |
| 02 | `02-meta-model.md` | Meta-model and type system | Meta-model |
| 03 | `03-identity-addressing.md` | Identity and addressing | Identity / URI |
| 04 | `04-change-unit.md` | Business change unit | Transactions |
| 05 | `05-logic-execution.md` | Logic execution | Scripting runtime |
| 06 | `06-orchestration-actions.md` | Process orchestration and actions | Processes / actions |
| 07 | `07-context-model.md` | Context model | Context |
| 08 | `08-branching.md` | Branching semantics | Versioning |
| 09 | `09-validation-rules.md` | Validation, constraints, rules | Rules |
| 10 | `10-security-governance.md` | Security and governance | Security |
| 11 | `11-events-scheduling.md` | Events, scheduling, async jobs, webhooks | Events |
| 12 | `12-query-search.md` | Query and search | Query |
| 13 | `13-metadata-ui.md` | Metadata-driven UI | UI protocol |
| 14 | `14-client-shells.md` | Client shells and UX paradigms | Clients |
| 15 | `15-api-protocol.md` | API and protocol | Protocol |
| 16 | `16-ai-integration.md` | AI integration | AI |
| 17 | `17-boot-genesis.md` | Boot and genesis | Boot |
| 18 | `18-multi-tenancy.md` | Multi-tenancy and environments | Tenancy |
| 19 | `19-deployment-clients.md` | Deployment topology and clients | Deployment |
| 20 | `20-technology-stack.md` | Technology stack (input to ADR-001) | ADR-001 |
| 21 | `21-audit-lineage.md` | Audit, lineage, observability | Audit |
| 22 | `22-development-method.md` | Development method and AI-assisted engineering | AI-GUIDE / conventions |

## 2. Presence and strength matrix

Legend:
- ● = implemented and substantial
- ◐ = partial, or design only
- ○ = minimal or incidental
- – = absent

| # | Topic | UP | FU | LC | UB | UC | US | LS | UW |
|---|---|---|---|---|---|---|---|---|---|
| 01 | Storage model | ● | ● | ● | ● | ● | ● | ● | – |
| 02 | Meta-model | ◐ | ◐ | ● | ● | ● | ● | ● | ○ |
| 03 | Identity / addressing | ◐ | ● | ◐ | ● | ● | ● | ● | ○ |
| 04 | Change unit | ● | ● | ● | ● | ● | ● | ● | – |
| 05 | Logic execution | ● | ● | ◐ | ● | ● | ● | ◐ | – |
| 06 | Orchestration / actions | ◐ | ○ | ● | ◐ | ● | ● | ● | ○ |
| 07 | Context model | ◐ | ◐ | ◐ | ● | ● | ● | ◐ | ◐ |
| 08 | Branching | ● | ● | ○ | ● | ○ | ○ | ○ | ○ |
| 09 | Validation / rules | ◐ | ● | ● | ◐ | ◐ | ◐ | ◐ | ○ |
| 10 | Security / governance | ● | ● | ● | ◐ | ◐ | ◐ | ◐ | ○ |
| 11 | Events / scheduling | ● | ◐ | ◐ | ◐ | ● | ● | ◐ | ○ |
| 12 | Query / search | ◐ | ◐ | ● | ◐ | ◐ | ◐ | ◐ | ○ |
| 13 | Metadata UI | ● | ◐ | ○ | ● | ◐ | ● | ◐ | ○ |
| 14 | Client shells | ● | ● | ◐ | ◐ | ● | ● | – | ● |
| 15 | API / protocol | ◐ | ● | ● | ● | ● | ● | ● | – |
| 16 | AI integration | ● | ○ | ◐ | ● | ● | ● | ○ | ○ |
| 17 | Boot / genesis | ● | ◐ | ◐ | ● | ● | ● | ● | – |
| 18 | Multi-tenancy | ○ | ● | ◐ | ● | ● | ● | ○ | ◐ |
| 19 | Deployment / clients | ◐ | ◐ | ◐ | ◐ | ◐ | ● | ● | – |
| 20 | Stack | Java | Java | Java | Java | Rust | Rust | Rust | TS |
| 21 | Audit / lineage | ● | ● | ● | ● | ● | ◐ | ● | – |
| 22 | Dev method / AI engineering | ◐ | ○ | ● | ● | ● | ● | ● | ◐ |

## 3. Verdict index

Every topic file ends with `VRD-NN-MM` verdicts. Each verdict is one of:
- **best-of KEY** — adopt one repository's approach;
- **fusion** — combine several approaches;
- **NEW** — the corpus lacks an answer and a new one is defined.

The index of all verdicts is maintained in §4 as topics are completed.

## 4. Verdicts per topic

| Topic | Verdicts |
|---|---|
| 01 | VRD-01-01 … VRD-01-06 |
| 02 | VRD-02-01 … VRD-02-07 |
| 03 | VRD-03-01 … VRD-03-05 |
| 04 | VRD-04-01 … VRD-04-05 |
| 05 | VRD-05-01 … VRD-05-06 |
| 06 | VRD-06-01 … VRD-06-06 |
| 07 | VRD-07-01 … VRD-07-05 |
| 08 | VRD-08-01 … VRD-08-06 |
| 09 | VRD-09-01 … VRD-09-06 |
| 10 | VRD-10-01 … VRD-10-07 |
| 11 | VRD-11-01 … VRD-11-05 |
| 12 | VRD-12-01 … VRD-12-05 |
| 13 | VRD-13-01 … VRD-13-07 |
| 14 | VRD-14-01 … VRD-14-04 |
| 15 | VRD-15-01 … VRD-15-05 |
| 16 | VRD-16-01 … VRD-16-07 |
