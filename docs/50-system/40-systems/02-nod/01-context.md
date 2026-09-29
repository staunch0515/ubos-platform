---
id: UBS-SYS-NOD-01
title: UBOS Node — Context and Contracts
status: draft
phase: PH-1
depends_on: [UBS-SYS-NOD-00, UBS-ARC-02]
---

# UBOS Node — Context and Contracts

```
   SDK clients (WSP, STU, apps)   external REST clients   IdPs     CTL
          │ CTR-003 UBTP               │ CTR-004          │CTR-052 │CTR-050/051
          ▼                            ▼                  ▼        ▼
   ┌──────────────────────────────── NOD ────────────────────────────────┐
   │ edge ▸ session ▸ exec ▸ events ▸ jobs ▸ connectors ▸ files ▸ notify │
   │ secrets ▸ index ▸ sync ▸ ops ▸ tenancy                              │
   │                ┌────────── DVM (library) ──────────┐                │
   │                │ CTR-001 host API / CTR-002 services│                │
   └──────┬─────────┴──────┬─────────────┬──────────────┴──────┬────────┘
          │CTR-005         │CTR-006      │CTR-007              │CTR-041/071/072
     PostgreSQL/SQLite   object store  runtime store     egress: connectors, providers, collectors
```

| Neighbour | Direction | Contract | What flows |
|---|---|---|---|
| SDK clients | client → NOD | CTR-003 | handshake, mutate, query, subscribe, sync |
| external REST clients | client → NOD | CTR-004 | resource and port calls |
| WSP | client → NOD | CTR-010 | view payloads, actions |
| STU | client → NOD | CTR-011 | builder operations |
| FRG | NOD ↔ FRG | CTR-012, CTR-013 | checks, verification records, deployments |
| DVM | NOD ↔ DVM | CTR-001, CTR-002 | processes and host services |
| PostgreSQL, SQLite | NOD → store | CTR-005 | authoritative and operational tables |
| object store | NOD → store | CTR-006 | chunks, files, backups |
| runtime store | NOD → store | CTR-007 | caches, fan-out, leases (hints only) |
| other nodes | NOD ↔ NOD | CTR-008, CTR-062 | coordination; cross-organisation sync |
| AGT | NOD → AGT | CTR-020 | model calls |
| NTY | NTY → NOD | CTR-030 | anchoring |
| BRG | BRG → NOD | CTR-040 | adapters, imports, exports |
| external systems | NOD → external | CTR-041 | connector calls |
| CTL | CTL ↔ NOD | CTR-050, CTR-051 | fleet administration, usage, heartbeat |
| IdPs | IdP → NOD | CTR-052 | OIDC, SAML, SCIM |
| EXC | NOD → EXC | CTR-054 | Buk retrieval |
| devices | Box ↔ Server | CTR-060 | sync |
| FED | NOD ↔ FED | CTR-061 | registry |
| workers | Worker ↔ NOD | CTR-070 | job leases and results |
| notification providers | NOD → provider | CTR-071 | e-mail, push, chat |
| collectors, SIEM | NOD → collector | CTR-072 | telemetry, security events |

## Design constraints inherited

| Constraint | Source |
|---|---|
| identity established at the edge only | AR-007 |
| stateless application nodes | AR-012 |
| replicas only for snapshot-consistent historical reads | AR-013 |
| zoned deployment with egress allow-list | AR-011 |
| platform services hold no business truth | AR-005 |
| idempotent cross-system writes | AR-010 |
| service principals per tenant | AR-009 |
| every contract versioned | AR-008 |
