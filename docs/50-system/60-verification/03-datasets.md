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
| WL-FIN-M | `gen finance` using the `basic-finance` Buk model: Northwind chart of accounts, customers, vendors, invoices, bills, receipts, payments, bank statement lines, fixed assets, foreign-currency documents and journals over 3 years | fixed seed | GL line count within 1%; every period balances; trial balance equals the generator's own totals |
| WL-FUND-M, WL-FUND-L | **Deferred (DEC-022).** `gen fund` using the `fund-ops` Buk model: funds, share classes, investors, orders, prices, NAVs, fees, ledger postings | fixed seed | ledger entry count within 1% |
| WL-CONTRACT-M | **Deferred (DEC-022).** `gen contracts` using the `contracts` Buk: agreements, clauses, obligations, documents | fixed seed | document version count within 1% |
| WL-MIXED-OLTP | load profile for the load generator | per run | operation mix within 2% |
| WL-TENANTS-1K | `gen tenants` creating 1,000 small tenants and 3 large ones | fixed seed | tenant count exact |

## 2. Fixtures

| Fixture | Content | Used by |
|---|---|---|
| conformance fixture models | the model set shipped in the conformance package | SUITE-DVM-CONF, third parties |
| sample company | Northwind Components, Inc.: chart of accounts, opening balances and one year of transactions, with expected trial balances, statements, agings, bank reconciliations, depreciation schedules, revaluation results and 1099 summary computed independently in a reference workbook and signed by the accounting reviewer | SUITE-PH2-FINANCE, SUITE-PH3-PILOT |
| finance edge fixtures | rounding and currency cases, period cut-off, closed-period postings, 1099 thresholds, sales-tax rates by state, partially matched bank lines | SUITE-PH2-FINANCE |
| bank file fixtures | BAI2, camt.053 and MT940 statements; NACHA, pain.001 and positive-pay outputs with validator results | SUITE-PH2-FINANCE, SUITE-BRG-MIG |
| vertical fixtures | **Deferred (DEC-022).** datasets for fund operations and contracts | SUITE-PH2-FUND, SUITE-PH2-CONTRACTS |
| replay corpus | ≥ 10,000 journaled processes from scenario runs and generators | SUITE-DVM-REPLAY |
| rendering vector set | view payloads and documents with expected PDF hashes | SUITE-WSP-RENDER |
| migration corpus | the sample company's opening balances and one year of history as spreadsheet and CSV extracts in the formats of common small-business ledgers, with deliberate data-quality defects | SUITE-BRG-MIG |

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

All datasets are synthetic (DEC-022, DEC-023): no customer or production data is used. The
sample company is fictitious. Benchmark datasets are synthetic unless an NR states otherwise.
