---
id: UBS-REQ-06
title: Non-Functional Requirements — Index, Reference Environments and Workloads
status: complete
phase: ALL
depends_on: [UBS-REQ-05, UBS-META-01]
---

# Non-Functional Requirements — Index

Every `NR-*` item has a measurable **Target** that states the metric, threshold, percentile,
workload and environment. Targets apply from the item's phase onwards and MUST be
re-verified in every later phase (regression rule).

## Files

| File | Areas |
|---|---|
| `01-performance-scale-cost.md` | PERF, SCAL, COST |
| `02-reliability-security-privacy.md` | AVAIL, DUR, DET, SEC, PRIV |
| `03-quality-attributes.md` | OBS, OPER, USE, ACC, PORT, COMPAT, MAINT, LOC |

## Reference environments

Benchmarks (method `BENCH`) run on these environments unless the item states otherwise.
The environments are defined here so that targets are reproducible. The verification
volume references them.

| ID | Description |
|---|---|
| ENV-REF-SERVER | A 3-node Server cluster. Each application node has 16 vCPU and 64 GB RAM. The database is PostgreSQL 16 on a dedicated 32 vCPU, 128 GB RAM host with local NVMe (at least 200k IOPS), plus one synchronous standby. The object store is S3-compatible. The network is 10 Gbps with 0.5 ms RTT inside the cluster. |
| ENV-REF-SMALL | A single-node Server with 8 vCPU and 32 GB RAM, PostgreSQL on the same host, NVMe, and the embedded runtime store. |
| ENV-REF-BOX | A laptop-class machine: 8 cores, 16 GB RAM, NVMe SSD, Windows 11 or macOS 14. |
| ENV-REF-MOBILE | A mid-range phone from the last 3 years (iOS or Android). |
| ENV-REF-BROWSER | Current stable Chrome on ENV-REF-BOX hardware. |
| ENV-REF-CELL | A Cell of 6 application nodes with the ENV-REF-SERVER node size, a clustered PostgreSQL primary with 2 replicas, and a tiered object store. |
| ENV-REF-WAN | Client-to-server path with 50 ms RTT, 50 Mbps, 0.1% packet loss. |

## Reference workloads

| Name | Content |
|---|---|
| WL-FUND-M | Fund operations, medium: 200 funds, 1,000 share classes, 500,000 investor accounts, 20,000,000 ledger entries, 10,000 prices per day for 5 years (about 12,500,000 price versions), 2,000 dealing orders per day |
| WL-FUND-L | 10× WL-FUND-M |
| WL-CONTRACT-M | 200,000 agreements, 3,000,000 obligations, 1,000,000 documents with an average of 12 versions each |
| WL-GENERIC-10M | 10,000,000 current Definition objects across 200 classes, 10% changed per month for 24 months |
| WL-MIXED-OLTP | Operation mix: 70% reads, 20% simple commits (1–10 objects), 5% queries, 5% ledger appends, with concurrency sized to the environment |
| WL-TENANTS-1K | 1,000 small tenants (10 users, 100,000 objects each) plus 3 large tenants (WL-FUND-M each) in one Cell |

## Measurement rules

1. Latency percentiles are measured at the server boundary, unless an item says "client
   perceived".
2. Every benchmark run records the environment, workload, software versions, duration
   (at least 30 minutes of steady state), warm-up and raw data.
3. A target is met when three consecutive runs meet it.
