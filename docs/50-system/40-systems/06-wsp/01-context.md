---
id: UBS-SYS-WSP-01
title: Workspace — Context and Contracts
status: draft
phase: PH-2
depends_on: [UBS-SYS-WSP-00, UBS-ARC-02]
---

# Workspace — Context and Contracts

```
 users (browser, desktop, mobile, portal) ──▶ WSP renderer ──SDK──▶ NOD ──▶ DVM (view payloads)
                                                 │  CTR-024
                                                 └──────────▶ AGT (Copilot sessions)
```

| Neighbour | Direction | Contract | What flows |
|---|---|---|---|
| NOD | WSP → NOD | CTR-010 (over CTR-003) | view payloads, actions, subscriptions, timelines, diffs |
| AGT | WSP → AGT | CTR-024 | Copilot sessions and proposals |
| NOD (Box) | shell ↔ local node | in-process UBTP | offline-first local data |
| NOD | NOD → WSP | CTR-071 via in-app channel | notifications |
