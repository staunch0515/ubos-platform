---
id: UBS-ARC-03
title: Deployment Topologies
status: draft
phase: ALL
depends_on: [UBS-ARC-01, UBS-ARC-02]
---

# Deployment Topologies

Each topology lists its components, stores, network zones, scaling model and the phase
from which it is supported. Topology names are descriptive and not ID-tracked. The rules
are `AR-*` items.

## 1. Network zones (common to all server topologies)

| Zone | Contents | Inbound from | Outbound to |
|---|---|---|---|
| Z1 Edge | load balancer or ingress, TLS termination (optional; UBTP also supports end-to-end TLS) | Internet or corporate network | Z2 |
| Z2 Application | NOD application nodes, Forge service, BRG, AGT, NTY services | Z1, Z3 (replication) | Z3, Z4 (egress through allow-list) |
| Z3 Data | PostgreSQL primary and replicas, runtime store, object store endpoints | Z2 only | backup targets |
| Z4 Egress | egress proxy with allow-list: connectors, model providers, TSA, notification providers, identity providers | Z2 | external |
| Z5 Management | CTL agents, monitoring collectors, bastion | operators (MFA) | Z2 admin endpoints (mTLS) |

### AR-011 — Zoned deployment
- **Rule:** Server and Cell deployments MUST place the authoritative store in a zone reachable only from application nodes. All outbound traffic MUST pass an egress allow-list.
- **Realises:** FR-LOGIC-104, CR-SOC2-003, NR-SEC-004
- **Rationale:** Defence in depth and data-exfiltration control.

## 2. Box (personal and field)

| Aspect | Specification |
|---|---|
| Components | Desktop or mobile shell (Tauri 2) + NOD(Box) + DVM + SQLite + local blob directory; optional headless mode for development |
| Stores | SQLite (authoritative), local content-addressed blob directory, embedded runtime store (in-memory) |
| Keys | OS key store (Keychain, DPAPI/TPM, Secret Service, Android Keystore, iOS Secure Enclave) |
| Network | None required. Optional sync to a Server (CTR-060). Optional connectors if configured. |
| Scaling | Single user or small team on a LAN (the Box can serve LAN peers in team mode, limited to 10 concurrent users) |
| Phases | Headless from PH-1a; desktop app PH-3; mobile PH-3 |

## 3. Server — single node

| Aspect | Specification |
|---|---|
| Components | 1 NOD(Server) process (UBTP, REST, Forge service, schedulers, connector runtime), optional BRG in-process, WSP and STU static assets served by NOD |
| Stores | PostgreSQL (same or separate host), S3-compatible store (or local file-system adapter for small installations), embedded runtime store |
| Scaling | Vertical. Targets ENV-REF-SMALL. |
| Availability | Restart-based recovery. Backups per CAP-OPS-02. |
| Phases | PH-1b |

## 4. Server — high availability

```
            ┌──────────── Z1 load balancer ─────────────┐
            │                                            │
     ┌──────┴──────┐  ┌─────────────┐  ┌─────────────┐
     │ NOD app #1  │  │ NOD app #2  │  │ NOD app #3  │   (stateless; leases in DB)
     └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
            └──────────┬──────┴────────────────┘
          ┌────────────┴────────────┐   ┌─────────────┐   ┌───────────────┐
          │ PostgreSQL primary       │──▶│ sync standby│   │ runtime store │
          └────────────┬────────────┘   └─────────────┘   └───────────────┘
                       │ async replication
                 ┌─────┴──────┐
                 │ DR site    │ (standby cluster, FR-OPS-061)
                 └────────────┘
```

| Aspect | Specification |
|---|---|
| Components | 3 or more NOD application nodes behind a load balancer; BRG, AGT and NTY as separate deployments (from PH-2 and PH-3) |
| Stores | PostgreSQL primary with synchronous standby and asynchronous DR replica; clustered runtime store; object store with versioning (and object lock for WORM classes) |
| Coordination | Schedule leadership and job leases in the database (CTR-008); cache invalidation over runtime-store pub/sub |
| Scaling | Horizontal application nodes (NR-SCAL-004); the database scales vertically, with read replicas for history and analytics reads |
| Availability | NR-AVAIL-002; DR per NR-DUR-002 and NR-DUR-003 |
| Phases | PH-2 |

### AR-012 — Stateless application nodes
- **Rule:** NOD application nodes MUST NOT keep state that cannot be lost. Everything needed for correctness lives in the authoritative store or is recoverable from it.
- **Realises:** FR-EVT-032, NR-AVAIL-002
- **Rationale:** Any node may fail at any time.

### AR-013 — Read replicas serve only snapshot-consistent historical and analytical reads
- **Rule:** Reads served from replicas MUST carry the replica watermark and MUST NOT be used for head reads within a process or for CAS bases.
- **Realises:** FR-VER-103, FR-TXN-021
- **Rationale:** Replication lag must never break consistency.

## 5. Managed Server (DEC-016)

| Aspect | Specification |
|---|---|
| Definition | An HA Server deployed and operated by UBOS for one tenant in a dedicated cloud account or VPC |
| Differences from customer-operated Server | UBOS operators with break-glass procedures, no business-data access (AR-006 applies to operator tooling); customer-managed keys optional |
| Phases | PH-3 (design partners) |

## 6. Cell (multi-tenant hosted)

| Aspect | Specification |
|---|---|
| Components | 6+ NOD application nodes; shared BRG, AGT and NTY services per Cell; CTL (global) |
| Stores | Clustered PostgreSQL (primary + 2 replicas) with row-level security per tenant, or a database per large tenant; tiered object store (hot and cold); runtime store cluster |
| Placement | CTL places tenants by policy (FR-TEN-071); large tenants may get dedicated Cells |
| Isolation | Storage row-level security, per-tenant data keys, per-tenant quotas (FR-TEN-024) |
| Scaling | Add Cells; move tenants online (FR-TEN-072) |
| Phases | PH-4 |

### AR-014 — Cell is the failure domain
- **Rule:** A Cell MUST share no authoritative store with any other Cell. The failure of one Cell MUST NOT affect tenants in other Cells.
- **Realises:** NR-AVAIL-001, IMP-08
- **Rationale:** Blast-radius control.

## 7. Worker pool (DEC-002)

| Aspect | Specification |
|---|---|
| Components | NOD(Worker role): DVM runtime and WASM sandbox, no authoritative store, no UBTP listeners |
| Placement | Organisation-owned machines registered to one Server or Cell tenant |
| Data | Inputs streamed per job; nothing is persisted after the job except caches of verified component binaries |
| Phases | PH-4 |

## 8. Browser kernel

| Aspect | Specification |
|---|---|
| Components | DVM compiled to WASM + SDK + WSP inside the browser |
| Stores | SQLite on the origin-private file system; IndexedDB for blobs |
| Keys | Non-extractable WebCrypto keys or platform authenticators |
| Network | Sync to the user's Server or Cell (CTR-060) |
| Phases | PH-5 |

## 9. Federation

```
 Org A: Server/Cell ──(QUIC + mTLS, UBTP Sync)── Org B: Server/Cell
        │                                             │
        └────────────── FED registry (DID, endpoints, trust) ─────┘
```

| Aspect | Specification |
|---|---|
| Components | FED registry (operated by UBOS or by consortia; multiple registries allowed), FED module in each participating NOD |
| Traffic | Only the interactions permitted by trust relationships (FR-SYNC-053) |
| Phases | PH-5 |

## 10. Topology support matrix

| Topology | PH-1a | PH-1b | PH-2 | PH-3 | PH-4 | PH-5 |
|---|---|---|---|---|---|---|
| Box headless (in-process) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Server single node | — | ✓ | ✓ | ✓ | ✓ | ✓ |
| Server HA + DR | — | — | ✓ | ✓ | ✓ | ✓ |
| Desktop and mobile apps | — | — | — | ✓ | ✓ | ✓ |
| Managed Server | — | — | — | ✓ | ✓ | ✓ |
| Cell | — | — | — | — | ✓ | ✓ |
| Worker pool | — | — | — | — | ✓ | ✓ |
| Browser kernel | — | — | — | — | — | ✓ |
| Federation | — | — | — | — | — | ✓ |
