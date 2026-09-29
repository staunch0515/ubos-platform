---
id: UBS-SYS-FED-01
title: Federation — Context
status: draft
phase: PH-5
depends_on: [UBS-SYS-FED-00]
---

# Federation — Context

```
  Org A node (NOD + FED module) ◀──CTR-062 (UBTP Sync, mTLS bound to DIDs)──▶ Org B node
            │  CTR-061                                                          │ CTR-061
            └──────────────────▶ FED registry (DNS + HTTPS) ◀──────────────────┘
```

| Neighbour | Direction | Contract | What flows |
|---|---|---|---|
| NOD | NOD ↔ FED registry | CTR-061 | registrations, resolution, trust proposals |
| NOD | NOD ↔ NOD | CTR-062 | fallback fetch, shared-object replication, signatures |
| DVM | FED module ↔ DVM | CTR-001 | shared BPU structures (DSN-DVM-955), sync sessions |
