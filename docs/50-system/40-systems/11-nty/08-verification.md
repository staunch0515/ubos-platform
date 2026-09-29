---
id: UBS-SYS-NTY-08
title: Notary — Verification Plan
status: draft
phase: PH-3
depends_on: [UBS-SYS-NTY-02]
---

# Notary — Verification Plan

| Suite | Content | Method |
|---|---|---|
| SUITE-NTY-ANCHOR | anchoring with TSA faults; receipt verification | CONF, FAULT |
| SUITE-NTY-PACK | pack reproducibility, redaction, tamper detection corpus | CONF, SEC |
| SUITE-NTY-VERIFIER | verifier against the conformance vectors for proofs and packs; offline web build | CONF |
| SUITE-NTY-EXAM | mock examination: produce records within 1 business day | SCN |

| Phase | NTY evidence |
|---|---|
| PH-2 | verifier CLI verifies packs produced by the DVM |
| PH-3 | anchoring for all design-partner tenants; tamper corpus 100% detected; independent review of the verifier by an external party |
