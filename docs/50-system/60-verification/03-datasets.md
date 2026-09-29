---
id: UBS-VER-03
title: Verification Volume — Datasets, Fixtures and Corpora
status: draft
phase: ALL
depends_on: [UBS-VER-00, UBS-REQ-06]
---

# Verification Volume — Datasets, Fixtures and Corpora

Workloads (WL-*) are defined in UBS-REQ-06. This document adds generation rules and the
fixtures and corpora used by the suites.

## 1. Workload generation

| Workload | Generator | Seed rule | Size check |
|---|---|---|---|
| WL-GENERIC-10M | `ubos-verify gen generic` with 200 classes of 5–40 fields, 3 inheritance levels, 10% monthly change for 24 months | fixed seed per BPA release | object and version counts within 1% |
| WL-FUND-M, WL-FUND-L | `gen fund` using the `fund-ops` Buk model: funds, share classes, investors, orders, prices, NAVs, fees, ledger postings | fixed seed | ledger entry count within 1% |
| WL-CONTRACT-M | `gen contracts` using the `contracts` Buk: agreements, clauses, obligations, documents | fixed seed | document version count within 1% |
| WL-MIXED-OLTP | load profile for the load generator | per run | operation mix within 2% |
| WL-TENANTS-1K | `gen tenants` creating 1,000 small tenants and 3 large ones | fixed seed | tenant count exact |

## 2. Fixtures

| Fixture | Content | Used by |
|---|---|---|
| conformance fixture models | the model set shipped in the conformance package | SUITE-DVM-CONF, third parties |
| vertical fixtures | minimal, typical and edge-case datasets for fund operations and contracts | SUITE-PH2-FUND, SUITE-PH2-CONTRACTS |
| replay corpus | ≥ 10,000 journaled processes from scenario runs and generators | SUITE-DVM-REPLAY |
| rendering vector set | view payloads and documents with expected PDF hashes | SUITE-WSP-RENDER |
| migration corpus | anonymised legacy extracts (design partner) and synthetic equivalents | SUITE-BRG-MIG |

## 3. Adversarial corpora

| Corpus | Content | Minimum size | Used by |
|---|---|---|---|
| L2 escape corpus | scripts attempting file, network, time, entropy, float, reflection, resource exhaustion and host-function abuse | 300 cases (PH-1), growing | SUITE-DVM-SEC |
| L3 escape corpus | WASM components attempting imports, memory abuse, non-determinism, fuel evasion | 300 cases (PH-3) | SUITE-DVM-SEC |
| isolation corpus | cross-tenant requests through every interface and ID-guessing strategies | 500 cases | SUITE-NOD-ISO |
| injection corpus | documents and records containing prompt-injection payloads with side-effect goals | 500 cases | SUITE-AGT-INJECT |
| tamper corpus | evidence packs, seals and archives with single-bit and structural modifications | 1,000 cases | SUITE-NTY-PACK |
| invalid configuration corpus | node configurations violating schema and ranges | 200 cases | SUITE-NOD-OPS |

## 4. Data protection for test data

Design-partner data is used only under the pilot agreement, in the pilot or a dedicated
isolated environment, anonymised where possible, and deleted at the end of its retention.
Benchmark datasets are synthetic unless an NR states otherwise.
