---
id: UBS-SYS-NOD-07
title: UBOS Node — Operations
status: draft
phase: PH-1
depends_on: [UBS-SYS-NOD-04, UBS-SYS-NOD-06, UBS-ARC-03]
---

# UBOS Node — Operations

## 1. Deployment

| Topology (UBS-ARC-03) | Node layout | Minimum resources |
|---|---|---|
| Server single | 1 node process; PostgreSQL 16+ on same or separate host | 4 vCPU, 16 GB RAM, SSD with ≥ 3,000 IOPS (ENV-REF-SMALL) |
| Server HA | ≥ 3 nodes behind a load balancer across 2+ zones; PostgreSQL primary + synchronous standby + async DR replica; runtime store cluster; object store with versioning | per node 8 vCPU, 32 GB (ENV-REF-SERVER) |
| Cell | ≥ 6 nodes; clustered PostgreSQL; OpenSearch; tiered object store; CTL agent | per ENV-REF-CELL |
| Box | desktop or mobile app; headless for development | 2 cores, 4 GB RAM |
| Worker | stateless containers in the customer's network | per job profile |

Network zones follow UBS-ARC-03 §1: the admin endpoint listens only in the management zone,
and all egress passes the allow-list proxy.

## 2. Scaling

| Dimension | Mechanism | Limit indicator |
|---|---|---|
| requests | add application nodes (stateless) | CPU > 70%, p95 latency |
| commits on one branch | group commit, leader lease for hot branches | sequencer queue depth |
| total commits | database vertical scaling; tenant sharding across Cells | WAL rate, IOPS |
| subscriptions | node count; pub/sub fan-out | send queue saturation |
| search | OpenSearch shards | indexing lag |
| jobs | job workers per node; worker role | queue age |

## 3. Runbooks (NR-OPER-004)

| Runbook | Trigger | Steps (summary) |
|---|---|---|
| RB-01 database failover | primary lost | confirm standby promotion; check nodes reconnected; verify last commits; check outbox resumed |
| RB-02 runtime store loss | store unavailable | confirm degraded mode; sweeper active; restore store; verify caches reset |
| RB-03 object store degradation | blob errors | switch to read-only for uploads; confirm reads of inline content; restore; run `verify chunks` |
| RB-04 outbox backlog | backlog age alert | check dead letters and failing endpoints; scale releasers; pause offending subscriptions |
| RB-05 job poison | poison alert | inspect error signature; fix handler or data; retry dead jobs |
| RB-06 clock skew | `NOD.CLOCK_SKEW` alerts | fix NTP; confirm commits resume |
| RB-07 key service outage | `IAM.KEY_UNAVAILABLE` | restore key service; monitor cache expiry window |
| RB-08 security incident: credential leak | leaked credential reported | revoke credential; review security log; rotate related secrets |
| RB-09 tenant restore to point in time | customer request with approval | restore into a staging VAE; verify; swap or export selected data |
| RB-10 DR region failover | region loss | promote DR replica; restore object store replica; repoint DNS; verify |
| RB-11 index corruption | verifier alert | `ubos-node rebuild index` for affected tenant |
| RB-12 upgrade rollback | failed health after upgrade | automatic rollback; if schema contracted, restore procedure |

## 4. Monitoring

Standard dashboards: edge (connections, rates, errors), DVM (commit latency by step,
conflicts), outbox and jobs (backlog age, dead letters), storage (database, object store),
security (auth failures, revocations), sync (device lag), capacity per tenant.

Key alerts:

| Alert | Condition |
|---|---|
| commit latency SLO burn | NR-PERF-002 burn rate over 2× for 1 h |
| outbox backlog | oldest ready row older than 5 min |
| dead letters | any new dead letter for a critical subscription |
| verifier finding | any |
| backup age | last successful base backup older than 26 h or WAL lag over 5 min |
| certificate expiry | under 14 days |
| security log chain break | any |

## 5. Backup schedule (defaults)

| Item | Frequency | Retention |
|---|---|---|
| PostgreSQL base backup | daily | 35 days (NR-DUR-004) |
| WAL archive | continuous | 35 days |
| object store versions | continuous (bucket versioning) | 35 days for non-WORM; WORM per retention |
| key service backup | daily with tombstones | per key-service policy |
| configuration versions | every change | permanent |
| restore drill | monthly (HA) | report retained 1 year |
