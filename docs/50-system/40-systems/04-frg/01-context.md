---
id: UBS-SYS-FRG-01
title: Logic Forge — Context and Contracts
status: draft
phase: PH-1
depends_on: [UBS-SYS-FRG-00, UBS-ARC-02]
---

# Logic Forge — Context and Contracts

```
 developer / CI ──CLI──▶ ┌──────────── FRG (ubos-forge) ────────────┐ ──CTR-015──▶ EXC
 editor ──LSP──────────▶ │ build ▸ check ▸ types ▸ test ▸ simulate  │
 STU/AGT ─(via NOD)─────▶│ pack ▸ sign ▸ verify ▸ conform ▸ docs    │ ◀─CTR-014── BPA vectors
                         │   in-process DVM (ubos-dvm)               │
                         └───────────────┬───────────────────────────┘
                                         │ CTR-013 (UBTP via Rust SDK)
                                         ▼
                                    NOD (dev Box, Server)
```

| Neighbour | Direction | Contract | What flows |
|---|---|---|---|
| DVM | FRG embeds | CTR-001 | in-process kernel for checks, tests, simulation |
| NOD | NOD → FRG | CTR-012 | server-side checks, types, simulations, Buk verification |
| NOD | FRG → NOD | CTR-013 | install, run scenarios, pull history, push types |
| BPA | BPA → FRG | CTR-014 | conformance package |
| EXC | FRG → EXC | CTR-015 | publish signed Buks |
| AGT | AGT → FRG (via NOD) | CTR-012 | generate–check–fix loop |
