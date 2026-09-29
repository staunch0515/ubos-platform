---
id: UBS-SYS-00
title: Systems Volume — Index
status: draft
phase: ALL
depends_on: [UBS-ARC-00, UBS-STD-00]
---

# Systems Volume — Index

Each system has a folder with the files of template T9 (UBS-META-02):
- `00-index`, `01-context`, `02-functions`, `03-interfaces`, `04-data`;
- `05-state-machines`, `06-configuration`, `07-operations`, `08-verification`.

Design requirements are `DSN-<SYS>-<NNN>`, interfaces are `IF-<SYS>-<NNN>`, and data
structures are `DAT-*`. Design requirements use the requirement block. Their **Origin** lists
the FR, NR, STD and AR items they realise.

| Folder | System | Primary capabilities (UBS-ARC-08) | Batch |
|---|---|---|---|
| `01-dvm/` | DVM reference kernel | MODEL, VER, TIME, LEDG, TXN, LOGIC, RULE, FLOW core, QRY, IAM decisions, PROOF core | B15–B16 |
| `02-nod/` | UBOS Node | INT-01, EVT, OPS, IAM edge, SYNC transport, connectors | B17 |
| `03-ctl/` | Control Plane | OPS-08, TEN-05/07, BILL | B17 |
| `04-frg/` | Logic Forge | DEV, STD runner, PKG verification | B18 |
| `05-sdk/` | Client SDKs | DEV-06 | B18 |
| `06-wsp/` | Workspace | UX, OFFICE | B19 |
| `07-stu/` | Studio | builder UX, change review | B19 |
| `08-agt/` | Agent Hub | AI | B20 |
| `09-exc/` | Buk Exchange | PKG distribution, BILL-03/04 | B20 |
| `10-fed/` | Federation | SYNC-05…07 | B21 |
| `11-nty/` | Notary | PROOF-04…06 | B21 |
| `12-brg/` | Bridge | INT, ANL-03…05, MIG | B21 |
| `13-bpa/` | BPA standard governance | STD | B22 |

## DSN number ranges per system

Numbers are grouped by sub-area (the first digit) so that related designs cluster. For
example DVM uses:

| Range | DVM sub-area |
|---|---|
| DSN-DVM-0xx | storage engine, chunks, kernel indexes, sequencer |
| DSN-DVM-1xx | model registry, effective definitions, lenses |
| DSN-DVM-2xx | versioning, change sets, merge, inheritance |
| DSN-DVM-3xx | bitemporal timelines |
| DSN-DVM-4xx | ledgers and projections |
| DSN-DVM-5xx | processes, flush, concurrency, outbox, idempotency |
| DSN-DVM-6xx | logic runtime, instruction dispatch, metering, journal |
| DSN-DVM-7xx | rules, governance sheets, decisions, flows |
| DSN-DVM-8xx | authorization, masking, encryption |
| DSN-DVM-9xx | query, views, proofs, export and import |
