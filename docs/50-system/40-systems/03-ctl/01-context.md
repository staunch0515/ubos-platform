---
id: UBS-SYS-CTL-01
title: Control Plane — Context and Contracts
status: draft
phase: PH-4
depends_on: [UBS-SYS-CTL-00, UBS-ARC-02]
---

# Control Plane — Context and Contracts

```
  operators (MFA, SSO)        tenant administrators (portal)
          │                              │
          ▼                              ▼
   ┌──────────────────────── CTL ────────────────────────┐
   │ registry ▸ placement ▸ lifecycle ▸ migration ▸ waves │
   │ usage ▸ plans/entitlements ▸ budgets ▸ capacity      │
   └──────┬──────────────────────────────┬────────────────┘
          │ CTR-050 (mTLS, admin)         │ CTR-053
          ▼          ▲ CTR-051 (usage)    ▼
        NOD fleet (Server, Cell)         EXC (licences, invoices, payouts)
```

| Neighbour | Direction | Contract | What flows |
|---|---|---|---|
| NOD | CTL → NOD | CTR-050 | registration, health, config, upgrade steps, tenant lifecycle and migration steps |
| NOD | NOD → CTL | CTR-051 | usage records, heartbeats, capacity reports |
| EXC | CTL ↔ EXC | CTR-053 | entitlements, licence status, usage-based invoice lines |
| AGT | CTL → AGT | configuration | provider allow-lists and AI budget caps per tenant |
| operator IdP | IdP → CTL | OIDC | operator authentication |

## Design constraints inherited

| Constraint | Source |
|---|---|
| blind to business data | AR-006 |
| platform services hold no business truth | AR-005 |
| cell is the failure domain | AR-014 |
| idempotent cross-system writes | AR-010 |
| every contract versioned | AR-008 |
