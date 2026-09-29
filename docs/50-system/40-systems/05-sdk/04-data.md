---
id: UBS-SYS-SDK-04
title: Client SDKs — Data
status: draft
phase: PH-1
depends_on: [UBS-SYS-SDK-02]
---

# Client SDKs — Data

| Item | Rule |
|---|---|
| object cache | keyed by version ID; entries record the subscription cursor that confirmed them |
| pending mutations | kept in memory (and in IndexedDB for WSP drafts, FR-UX-034) with idempotency key and base version |
| cursors | opaque server strings; stored per subscription |
| generated types | one module per namespace; file header records model hash and ABI |
| decimals | string-backed decimal type; money = `{ amount: Decimal, currency: string }` |
| time | instants as ISO 8601 strings with `Z`; dates as `YYYY-MM-DD` |
