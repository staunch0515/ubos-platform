---
id: CMP-18
title: "Comparison: Multi-tenancy and Environments"
status: complete
phase: P2
depends_on: [CMP-03, CMP-07, CMP-08, CMP-10]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Comparison: Multi-tenancy and Environments

## 1. Question

How are tenants isolated? How do tenants share platform metadata and customise it, how do environments work, and how does code from one tenant run inside another?

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | Tenant = branch (policy selects branch); overlay inherits platform | CON-UP-018, 052, 041 | Customisation by overlay |
| FU | Tenant/app scope in URI and identity; environments pinned | CON-FU-010, 012, 031 | Scoped identity |
| LC | App scoping `(app_id, type, slug)` | CON-LC-016 | Application isolation |
| UB | Tenant = database; tenant column everywhere; Context entities; RLS and dedicated partitions noted | CON-UB-011, 017, 040, HL-UB-001, 002 | Tenant as first-class dimension |
| UC/US | Sovereign root tenant `logrums` (constitution); tenant checks in syscalls; public read of root; Ambassador host vs sovereign | CON-UC-019, 024, HL-UC-004, 011 | Cross-tenant execution model |
| LS | Single tenant `logrums` | CON-LS-016 | – |
| UW | Context shows tenant and branch | CON-UW-011 | UX |

## 3. Dimension matrix

| Dimension | UP | FU | LC | UB | UC | LS |
|---|---|---|---|---|---|---|
| Tenant column / key | – | ● | app | ● | ● | fixed |
| Shared platform metadata | inheritance | – | – | sys tenant | ● root readable | – |
| Tenant customisation of platform types | ● overlay | – | – | tenant extensions | `_extends` | – |
| Cross-tenant execution | – | – | – | – | ● host/sovereign | – |
| Environments per tenant | branch | ● | draft/main | ● Context | Workspace | – |
| DB-level isolation (RLS) | – | – | – | noted | – | – |

## 4. Analysis

- **Two dimensions.** Every repository mixes two separate things:
  - tenancy (data ownership);
  - variation (overlays and environments).
- UB makes tenant a first-class key and uses contexts for environments. UP shows that customisation is best done by overlay branches. UC adds a sovereign root and rules for foreign code.
- **Shared metadata.** Platform types and packages live in a root/system tenant readable by everyone (UC `logrums`, UB `sys_boot`). Tenant extensions reference them through `_extends` (VRD-02-01).
- **Isolation strength.**
  - Only syscall-level checks exist in the corpus (UC).
  - UB lists PostgreSQL RLS and dedicated partitions for large tenants as scale measures.

## 5. Verdicts

### VRD-18-01 — Tenant is a key dimension; a root tenant holds platform packages (fusion UB + UC)
- **Decision:** fusion.
- **Consequences:**
  - `tenant_id` is on every row.
  - The root tenant (name fixed in P3 conventions; corpus uses `logrums`) owns the system and ontology packages. They are readable by all tenants and writable only by root administrators (constitution, CON-UC-019).

### VRD-18-02 — Tenant customisation by extension and overlay (fusion UB/UC + UP)
- **Decision:** fusion.
- **Consequences:**
  - A tenant extends platform types by `_extends` (new types) or by an overlay branch of the root package, scoped to the tenant (patched platform entities).
  - Resolution order: tenant draft → tenant branch → tenant overlay of root → root.

### VRD-18-03 — Environments are Contexts (→ VRD-07-01, VRD-08-06)
- **Decision:** as in VRD-07-01 and VRD-08-06.
- **Consequences:** Each tenant has Contexts (dev, uat, prod) pointing to its branches or pinned commits.

### VRD-18-04 — Cross-tenant execution rules (best-of UC)
- **Decision:** best-of UC.
- **Consequences:**
  - Foreign logic declares purity.
  - Pure logic runs with the host's data under the host's identity.
  - Effectful foreign logic writes under the sovereign tenant inside the host and is invisible to the host unless shared.
  - Cross-tenant reads need an explicit grant policy.

### VRD-18-05 — Isolation enforcement in depth (fusion UC + UB notes)
- **Decision:** fusion.
- **Consequences:**
  - Mandatory: policy enforcement at syscall and API (VRD-10-03).
  - Enterprise edition: PostgreSQL row-level security by tenant, with optional dedicated partitions or schemas for large tenants.
