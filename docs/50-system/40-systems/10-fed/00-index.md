---
id: UBS-SYS-FED-00
title: Federation — Overview
status: draft
phase: PH-5
depends_on: [UBS-SYS-00, UBS-SYS-NOD-00, UBS-STD-16]
---

# Federation (FED) — Overview

## 1. Purpose

Federation connects independently operated UBOS nodes of different organisations. It
provides:
- a registry of VAEs and their `did:ubos` identities and endpoints, with signed
  registrations (FR-SYNC-051) and federated discovery (FR-SYNC-052);
- trust relationships between organisations (FR-SYNC-053) that define which interactions are
  allowed;
- a node module that runs cross-node VAE trees with network fallback (FR-SYNC-061…064) and
  shared BPUs with multi-party approval (FR-SYNC-071…075).

## 2. Responsibilities and non-responsibilities

| Responsible for | Not responsible for |
|---|---|
| registry service (resolution, registrations, trust records) | business semantics of shared objects (DVM) |
| FED node module: trust enforcement, peer transport policy, fallback state | sync data structures (DVM), transport (NOD) |
| shared-BPU coordination (proposals, signature collection) | the merge and validation of shared state (DVM) |

## 3. Personas served

PER-ExternalParty, PER-TenantAdministrator, PER-SecurityOfficer, PER-ComplianceOfficer.

## 4. Phase map

PH-4 and PH-5 are specified but not scheduled for execution (DEC-023).

| Phase | FED scope |
|---|---|
| PH-1 … PH-4 | none; `did:ubos` identifiers, key histories and sync structures are already in the standard and the DVM so that federation needs no data migration |
| PH-5 | registry service, trust relationships, cross-node VAE trees with fallback, shared BPUs between two or more organisations, federated discovery |

## 5. Files

| File | Content |
|---|---|
| `01-context.md` | context |
| `02-functions.md` | DSN-FED-* |
| `03-interfaces.md` | IF-FED-* |
| `04-data.md` | registry and trust data |
| `05-state-machines.md` | trust, shared object, fallback |
| `06-configuration.md` | settings |
| `07-operations.md` | operations |
| `08-verification.md` | verification plan |
