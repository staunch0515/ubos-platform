---
id: UBS-VER-02
title: Verification Volume — Environments
status: draft
phase: ALL
depends_on: [UBS-VER-00, UBS-REQ-06]
---

# Verification Volume — Environments

The seven reference environments are defined in UBS-REQ-06. This document states how
they are provisioned so that results are comparable across runs.

| Provisioning rule | Specification |
|---|---|
| infrastructure as code | every environment is created from a versioned definition in `ubos-verify/envs/`; the definition hash is recorded in every report |
| isolation | benchmark environments are dedicated during runs (no shared tenants on the hosts) |
| versions | exact versions of OS, PostgreSQL, SQLite, object store and runtime store are pinned per phase |
| time | NTP-synchronised hosts; clock-skew faults only when injected deliberately |
| observability | OpenTelemetry collector and metrics store attached; exported with the report |
| data | datasets are generated from seeds (`03-datasets.md`) or restored from signed snapshots |

| Use | Environment | Notes |
|---|---|---|
| kernel and edge benchmarks, HA and fault injection | ENV-REF-SERVER | PH-1 kernel benchmarks use one application node against the reference database host |
| small installations, installation tests | ENV-REF-SMALL | also the single-node Server of PH-1b |
| SQLite, Box and developer loop | ENV-REF-BOX | Windows 11 and macOS 14 images |
| mobile apps | ENV-REF-MOBILE | one iOS and one Android device class; device farm for the platform matrix |
| browser kernel and web client measurements | ENV-REF-BROWSER | current stable Chrome; Firefox and Safari for functional tests |
| multi-tenant scale | ENV-REF-CELL | two instances for migration tests |
| client-perceived latency and sync | ENV-REF-WAN | network emulation (netem) applied between client and server |
| certification | certification environment | ENV-REF-SERVER-class definition published with each BPA release |
