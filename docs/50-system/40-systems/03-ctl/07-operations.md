---
id: UBS-SYS-CTL-07
title: Control Plane — Operations
status: draft
phase: PH-4
depends_on: [UBS-SYS-CTL-02]
---

# Control Plane — Operations

| Topic | Specification |
|---|---|
| deployment | 3 stateless API instances across zones; PostgreSQL with synchronous standby; HSM-backed CA |
| failure impact | CTL outage does not affect serving tenants: nodes keep running with cached entitlements (grace) and buffer usage (DSN-NOD-431) |
| backups | daily base backup and WAL archive; 1-year retention for usage and audit |
| monitoring | operation failures, wave gates, usage gaps, certificate expiry, missing nodes |
| runbooks | stuck migration (abort or resume), halted wave, usage chain gap, CA compromise (re-issue all node certificates) |
| access | operators through bastion and SSO with WebAuthn; no direct database access except break-glass |
