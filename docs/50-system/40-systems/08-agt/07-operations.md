---
id: UBS-SYS-AGT-07
title: Agent Hub — Operations
status: draft
phase: PH-3
depends_on: [UBS-SYS-AGT-02]
---

# Agent Hub — Operations

| Topic | Specification |
|---|---|
| deployment | stateless service instances; PostgreSQL for operational tables; egress only through the allow-list proxy |
| provider incidents | automatic fallback per policy; status page per provider; operators can disable a provider globally in one action |
| AI incident response | runbook: kill switch, revert merged agent work (DSN-AGT-107), review recordings, notify supervisors |
| model upgrades | new model versions are evaluated (DSN-AGT-501) before routes change |
| monitoring | cost, latency, error rates per provider; acceptance and revert rates per agent |
