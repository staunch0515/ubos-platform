---
id: UBS-SYS-BRG-00
title: Bridge — Overview
status: draft
phase: PH-2
depends_on: [UBS-SYS-00, UBS-SYS-NOD-00, UBS-ARC-01]
---

# Bridge (BRG) — Overview

## 1. Purpose

The Bridge connects UBOS to the outside world in bulk:
- migration: spreadsheet and document imports, legacy bulk loads with history, mapping
  definitions and reconciliation reports;
- integration adapters: connector framework for bulk and file-based systems, financial
  adapters (bank, custodian, fund platforms), e-mail and chat, event streaming out;
- analytics exit: CDC export to Parquet and Iceberg, a single-tenant embedded analytical
  engine (DuckDB) with governed datasets and read-only SQL.

Every ingestion is a normal governed process through NOD (CTR-040); BRG holds no business
truth (AR-005).

## 2. Responsibilities and non-responsibilities

| Responsible for | Not responsible for |
|---|---|
| import pipelines, parsing, mapping execution, reconciliation | validation and commit semantics (DVM) |
| adapters for files, bulk APIs, brokers, mail and chat | per-call connectors from logic (NOD) |
| CDC export and analytics engine | authoritative reporting (reports are DVM saved queries and report definitions) |
| external signature provider adapters | signing ceremonies (WSP) |

## 3. Personas served

PER-ImplementationConsultant, PER-DataAnalyst, PER-FundAccountant, PER-IsvDeveloper,
PER-PlatformOperator.

## 4. Phase map

| Phase | BRG scope |
|---|---|
| PH-2 | spreadsheet import, legacy bulk load with history and opening balances, mapping definitions and tests, reconciliation reports, e-mail and chat adapters, bank statement and payment file handling (with NOD parsers) for Basic Finance (DEC-022) |
| PH-3 | bank connectivity adapters (SFTP, bank APIs), CDC export to Parquet and Iceberg, embedded analytics engine and governed datasets, event streaming out |
| PH-4 | additional ERP and CRM adapters |
| PH-5 | document import (DOCX to Live Doc, clause extraction) and external e-signature providers (DEC-022) |

## 5. Files

| File | Content |
|---|---|
| `01-context.md` | context |
| `02-functions.md` | DSN-BRG-* |
| `03-interfaces.md` | IF-BRG-* |
| `04-data.md` | import runs, mappings, watermarks |
| `05-state-machines.md` | import run, export watermark |
| `06-configuration.md` | settings |
| `07-operations.md` | operations |
| `08-verification.md` | verification plan |
