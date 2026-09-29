---
id: UBS-SYS-SDK-05
title: Client SDKs — State Machines
status: draft
phase: PH-1
depends_on: [UBS-SYS-SDK-02]
---

# Client SDKs — State Machines

## Connection

| From | Event | To |
|---|---|---|
| — | `connect` | connecting |
| connecting | handshake ok | ready |
| connecting | auth failure | failed |
| ready | transport lost | reconnecting |
| reconnecting | handshake ok | ready (subscriptions resumed) |
| reconnecting | deadline exceeded | offline (application notified; retries continue) |
| ready | session revoked frame | signed-out |
| any | `close` | closed |

## Subscription

| From | Event | To |
|---|---|---|
| — | subscribe acknowledged | live |
| live | change frame | live (cursor advanced) |
| live | reset frame | resetting (application refetch) |
| resetting | refetch done | live |
| live | connection lost | suspended |
| suspended | reconnected | live (resumed from cursor) |
| any | `close` | closed |
