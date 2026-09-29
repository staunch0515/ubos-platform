---
id: UBS-SYS-NTY-05
title: Notary — State Machines
status: draft
phase: PH-3
depends_on: [UBS-SYS-NTY-02]
---

# Notary — State Machines

## Anchoring cycle (per tenant)

| From | Event | To |
|---|---|---|
| idle | cadence tick and root changed | fetching root |
| fetching root | root obtained | timestamping |
| timestamping | two tokens obtained | recording |
| timestamping | one TSA fails | timestamping (second TSA), alert |
| timestamping | both fail | retry later, alert |
| recording | receipt appended | idle |

## Pack job

| From | Event | To |
|---|---|---|
| — | request | queued |
| queued | started | collecting |
| collecting | done | packaging |
| packaging | archive written | ready |
| ready | expiry | expired (archive deleted) |
| any | error | failed |
