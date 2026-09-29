---
id: UBS-SYS-NTY-01
title: Notary — Context
status: draft
phase: PH-3
depends_on: [UBS-SYS-NTY-00]
---

# Notary — Context

| Neighbour | Direction | Contract | What flows |
|---|---|---|---|
| NOD (DVM) | NTY → NOD | CTR-030 | tenant roots, proofs, evidence collection, receipt appends |
| TSAs | NTY → TSA | CTR-031 | RFC 3161 timestamp requests and tokens |
| any verifier user | files → verifier | CTR-032 | evidence packs, sealed PDFs, exports |
