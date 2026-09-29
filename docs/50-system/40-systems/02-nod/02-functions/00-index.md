---
id: UBS-SYS-NOD-02
title: UBOS Node — Design Requirements Index
status: draft
phase: PH-1
depends_on: [UBS-SYS-NOD-01]
---

# UBOS Node — Design Requirements Index

| File | Range | Sub-area |
|---|---|---|
| `01-edge-sessions.md` | DSN-NOD-0xx, DSN-NOD-1xx | process architecture, UBTP server, REST facade, webhooks, authentication, sessions, provisioning, secrets and keys, API credentials |
| `02-events-jobs-services.md` | DSN-NOD-2xx, DSN-NOD-3xx | outbox release, events, jobs, schedules, timers, workers, connectors, files, search adapters, notifications, retention and WORM |
| `03-tenancy-sync-ops.md` | DSN-NOD-4xx…6xx | tenancy, genesis Buks, usage and entitlements, sync transport, installation, configuration, telemetry, health, backup, DR, upgrades, cluster coordination, admin endpoint |

Every DSN item uses the requirement block. **Systems** always includes NOD.
