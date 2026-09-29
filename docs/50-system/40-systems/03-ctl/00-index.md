---
id: UBS-SYS-CTL-00
title: Control Plane — Overview
status: draft
phase: PH-4
depends_on: [UBS-SYS-00, UBS-SYS-NOD-00, UBS-ARC-01]
---

# Control Plane (CTL) — Overview

## 1. Purpose

The Control Plane operates fleets of nodes: it registers nodes, places tenants in Cells,
orchestrates upgrades and tenant migrations, collects usage, manages plans, entitlements and
budgets, and provides the operator console. It is blind to business data (AR-006): it never
reads objects, and none of its interfaces can.

## 2. Responsibilities and non-responsibilities

| Responsible for | Not responsible for |
|---|---|
| node registry, health and version inventory | serving business requests (NOD) |
| tenant placement, lifecycle orchestration and migration between Cells | tenant business configuration (tenant VAE) |
| upgrade waves and configuration rollout | kernel semantics (DVM) |
| usage collection, verification, aggregation | invoices and publisher payouts (EXC) |
| plans, entitlements, budgets and caps | Buk licences as objects (EXC) |
| capacity planning and tiering policy | executing tiering (NOD) |
| operator console, operator identity and audit | tenant user identity (NOD) |

## 3. Personas served

PER-PlatformOperator (primary), PER-SecurityOfficer (operator audit), PER-TenantAdministrator
(plan, usage and budget views through a tenant portal), PER-ExchangeOperator (licence and
billing integration).

## 4. Phase map

| Phase | CTL scope |
|---|---|
| PH-1 … PH-3 | none as a product; Server and managed Server are operated with node CLI and infrastructure tooling; a minimal internal "CTL-lite" script set tracks design-partner nodes (PH-3) |
| PH-4 | full CTL: registry, placement, lifecycle, migration, upgrade waves, config rollout, usage, plans, entitlements, budgets, capacity and tiering, operator console, tenant portal |
| PH-5 | federation-aware registry hooks; multi-region Cell groups |

## 5. Deployment

CTL runs as a separate highly available service (`ubos-control`) with its own PostgreSQL
database, in a management region per jurisdiction (US first, EU region before EU tenants are
admitted). It talks to nodes only through CTR-050 and CTR-051 over mTLS.

## 6. Files

| File | Content |
|---|---|
| `01-context.md` | context and contracts |
| `02-functions.md` | DSN-CTL-* |
| `03-interfaces.md` | IF-CTL-* |
| `04-data.md` | CTL data model |
| `05-state-machines.md` | node, placement, migration, upgrade wave |
| `06-configuration.md` | CTL configuration |
| `07-operations.md` | operating CTL itself |
| `08-verification.md` | CTL verification plan |
