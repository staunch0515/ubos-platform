---
id: UBS-SYS-FED-04
title: Federation — Data
status: draft
phase: PH-5
depends_on: [UBS-SYS-FED-02]
---

# Federation — Data

### DAT-TrustRelationship
- **Purpose:** The bilateral agreement that permits cross-organisation interactions.
- **Kind:** Definition
- **Stored in:** both parties' VAEs; summary in the registry
- **Schema:**
```yaml
DAT-TrustRelationship:
  id:           { type: uuid, required: true, description: identity }
  parties:      { type: list<did>, required: true, description: participating VAEs }
  interactions: { type: list<object>, required: true, description: allowed interaction kinds with scopes }
  validity:     { type: period, required: true, description: valid period }
  termination:  { type: object, required: true, description: notice and final-copy rules }
  signatures:   { type: list<DAT-Signature>, required: true, description: all parties }
```
- **Invariants:**
  1. Active only when signed by all parties.
- **Satisfies:** FR-SYNC-053
