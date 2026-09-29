---
id: UBS-SYS-BRG-01
title: Bridge — Context
status: draft
phase: PH-2
depends_on: [UBS-SYS-BRG-00]
---

# Bridge — Context

| Neighbour | Direction | Contract | What flows |
|---|---|---|---|
| NOD | BRG → NOD | CTR-040 | bulk import processes, port invocations, change feeds |
| external systems | BRG → external | CTR-041 | files, bulk APIs, brokers, mail, chat |
| lakehouse | BRG → sinks | CTR-042 | Parquet and Iceberg snapshots |
| STU, WSP | via NOD | CTR-011 | import wizards, reconciliation reports |
| AGT | via NOD | CTR-020 | document understanding for clause extraction |
