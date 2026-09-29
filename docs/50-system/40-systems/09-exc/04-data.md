---
id: UBS-SYS-EXC-04
title: Buk Exchange — Data
status: draft
phase: PH-4
depends_on: [UBS-SYS-EXC-02]
---

# Buk Exchange — Data

| Store | Content |
|---|---|
| WORM object storage | archives by root hash, verification records |
| PostgreSQL | publishers, namespaces, packages, versions, listings, licences, advisories |
| EXC ledger VAE (UBOS) | invoices, payments, revenue shares, payouts as ledger entries |
| payment processor | card and bank data (never stored by EXC) |

### DAT-Licence
- **Purpose:** A signed right to use a Buk.
- **Kind:** Definition
- **Stored in:** EXC database; delivered as a signed file and installed as a System-kind object in the tenant VAE
- **Schema:**
```yaml
DAT-Licence:
  licence_id:    { type: uuid, required: true, description: identity }
  tenant:        { type: uuid, required: true, description: licensee tenant }
  package:       { type: string, required: true, description: publisher/name }
  versions:      { type: string, required: true, description: semver range }
  term:          { type: period, required: true, description: validity }
  terms:         { type: object, required: true, description: seats, metered terms, features }
  offline_until: { type: instant, required: false, description: offline validity limit }
  signature:     { type: DAT-Signature, required: true, description: EXC signature }
```
- **Invariants:**
  1. A licence verifies with the EXC key valid at its issue time.
- **Satisfies:** FR-BILL-031, FR-PKG-083
