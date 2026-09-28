---
id: ADR-005
title: "ADR-005: Tenant-qualified context authority in URIs"
status: complete
phase: P4
depends_on: [CMP-03, SPEC-12]
sources: [UB, UC, FU]
---

# ADR-005: Tenant-qualified context authority in URIs

- **Status:** accepted (2026-09-28, during P4 SPEC-12). This ADR adapts VRD-03-03.

## Context

- VRD-03-03 follows UB (CON-UB-017). The URI authority is looked up as a Context slug in a system-wide space (`sys_boot`), with a fallback to a tenant code on `main`.
- VRD-18-03 says every tenant has its own Contexts (dev, uat, prod). With a global lookup, context slugs such as `prod` collide across tenants. They would need global uniqueness or a global registry in the root tenant, which conflicts with "tenant data lives in the tenant" (REQ-STO-006, SPEC-23).

## Options considered

1. **Global Context slugs in the root tenant** (UB as is).
   - Pros: short authorities.
   - Cons: every tenant's environments live in the root tenant, slugs collide, and tenant admins need root write access.
2. **Tenant-qualified authority `tenant[.context]`.**
   - Pros: no collisions, no global lookup, Contexts stay tenant entities, and the tenant is visible in every URI.
   - Cons: authorities are slightly longer (`acme.prod`).
3. **Context as a query parameter (`?context=prod`).**
   - Pros: keeps the authority a pure tenant code.
   - Cons: the context is no longer the first-class address root that UB intended, and URIs become longer.

## Decision

Option 2. The grammar is `authority = tenant-code ["." context-slug]` (SPEC-12 REQ-URI-006):
- The tenant code has no dots, so the first dot separates the tenant from the Context slug.
- Without a Context slug, the tenant's `default` Context applies if it exists; otherwise branch `main` applies (REQ-URI-009).

## Consequences

- SPEC-12 and SPEC-16 use this form.
- Stored references use the bare tenant code (SPEC-12 §1.3).
- VRD-03-03 is consumed in adapted form.

## Origin

VRD-03-03, VRD-18-03, CON-UB-017, CON-UC-015, CON-FU-031.
