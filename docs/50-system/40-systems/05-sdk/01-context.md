---
id: UBS-SYS-SDK-01
title: Client SDKs — Context
status: draft
phase: PH-1
depends_on: [UBS-SYS-SDK-00]
---

# Client SDKs — Context

```
 WSP, STU, Box shell, AGT, BRG, NTY, third-party apps
                 │ (library calls, generated types from FRG)
                 ▼
          ┌────────── SDK ──────────┐
          │ transport ▸ session     │
          │ ops ▸ cache ▸ typed API │
          └──────────┬──────────────┘
                     │ CTR-003 UBTP (WebSocket, HTTP, in-process; QUIC PH-5)
                     ▼
                    NOD
```

| Neighbour | Direction | Contract | What flows |
|---|---|---|---|
| NOD | SDK → NOD | CTR-003 | all UBTP operations |
| FRG | FRG → SDK users | generated types | typed models and query builders |
| identity providers | via NOD | CTR-052 | browser sign-in redirects |
