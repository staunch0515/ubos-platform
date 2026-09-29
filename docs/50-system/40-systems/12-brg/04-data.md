---
id: UBS-SYS-BRG-04
title: Bridge — Data
status: draft
phase: PH-2
depends_on: [UBS-SYS-BRG-02]
---

# Bridge — Data

| Data | Store | Authority |
|---|---|---|
| mappings, adapter configurations, sink definitions | tenant VAE (Definition objects) | authoritative |
| import runs and row results | BRG operational tables; summary object in VAE | operational |
| reconciliation reports | tenant VAE (sealed) | authoritative evidence |
| export watermarks | stored with the Iceberg snapshot metadata and in BRG tables | derived |
| analytical datasets | per-tenant DuckDB files in object storage | derived, rebuildable |
