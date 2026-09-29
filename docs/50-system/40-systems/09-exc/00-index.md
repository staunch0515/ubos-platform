---
id: UBS-SYS-EXC-00
title: Buk Exchange — Overview
status: draft
phase: PH-4
depends_on: [UBS-SYS-00, UBS-SYS-FRG-00, UBS-ARC-01]
---

# Buk Exchange (EXC) — Overview

## 1. Purpose

The Exchange distributes Buks: publishers upload signed archives, EXC verifies them
reproducibly, lists them, issues licences, and handles fiat invoicing and publisher payouts
(DEC-012). Nodes fetch archives and licences from EXC (CTR-054), or install them offline from
files. EXC never receives tenant business data.

## 2. Responsibilities and non-responsibilities

| Responsible for | Not responsible for |
|---|---|
| publisher accounts and namespace ownership | installing Buks into VAEs (DVM, NOD) |
| archive storage, verification service, listings, search | writing Buks (FRG, STU) |
| licence objects and lifecycle | enforcing licences inside VAEs (DVM licence hooks, NOD) |
| invoicing customers in fiat, payouts to publishers | tenant plans and usage (CTL) |
| security advisories and yanking | tenant data (never) |

## 3. Personas served

PER-ExchangeOperator, PER-IsvDeveloper (publisher), PER-TenantAdministrator (buyer),
PER-ImplementationConsultant.

## 4. Phase map

PH-4 and PH-5 are specified but not scheduled for execution (DEC-023).

| Phase | EXC scope |
|---|---|
| PH-2 | none as a service; Buks distributed as signed files; namespaces reserved in a registry file |
| PH-3 | none as a service (DEC-023); the finance Buk is distributed as a signed file |
| PH-4 | private registry mode and mirrors (DSN-EXC-010); public Exchange: publishers, verification service, listings, licences, invoicing, payouts, advisories |
| PH-5 | federation-aware mirrors for air-gapped and sovereign deployments |

## 5. Files

| File | Content |
|---|---|
| `01-context.md` | context |
| `02-functions.md` | DSN-EXC-* |
| `03-interfaces.md` | IF-EXC-* |
| `04-data.md` | data model |
| `05-state-machines.md` | listing, licence, payout |
| `06-configuration.md` | settings |
| `07-operations.md` | operations |
| `08-verification.md` | verification plan |
