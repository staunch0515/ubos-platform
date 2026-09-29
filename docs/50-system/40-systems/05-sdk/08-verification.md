---
id: UBS-SYS-SDK-08
title: Client SDKs — Verification Plan
status: draft
phase: PH-1
depends_on: [UBS-SYS-SDK-02]
---

# Client SDKs — Verification Plan

| Suite | Content | Method |
|---|---|---|
| SUITE-SDK-PROTO | protocol suite shared by all SDKs against a reference node: every operation, error mapping, retries, idempotency, paging | CONF |
| SUITE-SDK-NET | network faults (drops, latency, partitions) with reconnection and resume | FAULT, SIM |
| SUITE-SDK-SKEW | SDK N against node N−1 and N+1 minor | CONF |
| SUITE-SDK-TYPES | generated types compile and narrow correctly for the fixture models | CONF |
| SUITE-SDK-SIZE | bundle size budget (DSN-SDK-017) | BENCH |

| Phase | SDK evidence |
|---|---|
| PH-1b | Rust and TypeScript SDKs pass SUITE-SDK-PROTO and -NET; WSP uses only the SDK |
| PH-2 | skew suite; optimistic UI support used by WSP |
| PH-3 | Python SDK passes SUITE-SDK-PROTO |
