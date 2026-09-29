# Handoff — status of the UBOS specification work

Last updated: 2026-09-29. This file lets a new Claude session (or a new engineer) continue
the work without the earlier conversations. Read `CLAUDE.md` first for the working rules.

## 1. Background in brief

- The owner earlier built eight experimental repositories of the same platform idea with AI
  help. They diverged. The research volume (`docs/10-inventory` … `docs/40-spec`) analysed
  them; the **system specification** `docs/50-system/` replaced them as the single
  authority (DEC-001, DEC-008).
- The core idea: a public standard (BPA) for how business state changes over time — typed,
  versioned, bitemporal, governed and provable — run by a reference kernel (DVM); business
  behaviour ships as data in business packages (Buks). Principle: *harden the mechanism,
  soften the policy.*
- 13 systems: DVM, NOD, CTL, FRG, SDK, WSP, STU, AGT, EXC, NTY, BRG, FED, BPA.
- 6 phases: PH-0 foundations; PH-1 kernel and single-node server (gates 1a and 1b); PH-2
  first business package and production server; PH-3 CPA audit drill, evidence and governed
  AI; PH-4 devices, scale and ecosystem; PH-5 federation. **PH-4 and PH-5 are specified but
  not scheduled** (DEC-023).

## 2. Decisions that shape the current plan (all Accepted)

| Decision | Content |
|---|---|
| DEC-022 | The first business package is **US Basic Finance** for one company (GL, journals with approval, periods and close, AR, AP with Form 1099, bank reconciliation, fixed assets, multi-currency ASC 830, basic state sales tax, four statements, audit trail and controls). Fund operations and contract management are **Deferred** with IDs kept; contract-only capabilities move to PH-5. Excluded: payroll, inventory, consolidation, budgeting, project accounting, complex ASC 606, ASC 842. |
| DEC-023 | The product is **not for sale**; it demonstrates the capabilities the platform imposes and is compared with the owner's Java system. PH-3 acceptance is an external CPA audit drill, not a customer pilot. |
| DEC-024 | **PostgreSQL only** until PH-4; SQLite, Box and device sync move to PH-4. |
| DEC-025 | PH-2 is **Web only**, professional and practical; Smart Grid (bulk journal grid) is in PH-2; desktop and mobile apps in PH-4. |
| DEC-026 | The finance requirements are a separate, implementation-neutral English document (`docs/finance-requirements/`, IDs `FIN-*`) shared by both implementations, with a sample company, expected results, acceptance scenarios and a comparison protocol. |
| DEC-019 | Licensing: standard text CC BY 4.0; vectors, schemas, SDKs and verifier Apache-2.0; kernel, node and Forge AGPL-3.0 plus commercial; first-party Buks commercial. |
| DEC-027 | The specification and finance requirements moved with history to `staunch0515/ubos-unit`; `ubos-platform` is the archive. |
| DEC-018 | Planned implementation repositories: `ubos` (monorepo), `bpa-standard`, `ubos-verify`, `ubos-buks`; the specification lives in this repository (see §6). |

The full log with context and alternatives is `docs/50-system/90-decisions/000-index.md`.

Values confirmed by the owner (2026-09-29): Form 1099-NEC/MISC threshold for tax year 2026
= USD 2,000.00; Texas, City of Austin combined sales tax rate = 8.25%.

## 3. What exists

**System specification (`docs/50-system/`)** — about 250 documents, fully cross-checked:
33 personas, 220 capabilities, 67 scenarios (19 deferred), 800 FR, 106 NR, 54 CR, 239
standard clauses, 695 design items, 118 interfaces, 85 exit criteria (7 withdrawn), 77
suites, 90 verification items, 27 decisions. Traceability has 0 Must gaps in every phase.
Annex `10-requirements/10-finance-mapping.md` maps the finance requirements to the platform.

**Finance requirements (`docs/finance-requirements/`)** — 153 requirements (130 Must, 20
Should, 3 Could) in 14 areas; sample company Northwind Components, Inc. with a CSV dataset
and generated expected results (January 2026 net income 5,127.10; total assets 617,415.00);
15 acceptance scenarios covering every Must requirement; comparison protocol with 14
metrics and 6 standard change requests.

**Reports to the owner (Chinese):** `docs/reports/ubos-system-spec-summary-zh.md`,
`docs/reports/finance-requirements-summary-zh.md`.

**Tools (`docs/tools/`):** `check_system.py`, `gen_phase_features.py`,
`gen_traceability.py`, `gen_indexes.py`, `gen_finance_expected.py`, `check_finance.py`, all
run by `regen_all.sh`. Expected result today: system volume `errors: 0, warnings: 0`;
finance `errors: 0`.

## 4. Next steps (proposed, not started)

1. **Accounting review.** A US CPA or equivalent reviews and signs the chart of accounts and
   the expected results of the sample company (needed for EXIT-2-16).
2. **Freeze the finance requirements** with a tag (for example `fin-req-v1.0`) so that UBOS
   and the Java system start from the same version; the Java team starts recording work
   items per `docs/finance-requirements/40-comparison-protocol.md`.
3. **Prepare PH-0** (`docs/50-system/50-phases/PH-0/00-plan.md`): reference environment
   ENV-REF-SERVER, an engineer able to write formal models, and the implementation
   repository layout (see §6).
4. Start PH-0 work only after the owner confirms a PH-0 plan in Chinese.

## 5. Open items

| Item | Note |
|---|---|
| Implementation repository layout | DEC-018 plans `ubos`, `bpa-standard`, `ubos-verify`, `ubos-buks`. The owner created `ubos-unit` as the new home of this work; whether the implementation goes into `ubos-unit` as the monorepo or into the DEC-018 repositories is to be decided (record it as a new DEC). |
| Legal review of licences | DEC-019 accepted; legal review before any public release. |
| Accounting reviewer | to be named (ASM-002, PH-3 CPA). |

## 6. Repositories

- `staunch0515/ubos-unit` — **current home** of the specification and the finance
  requirements (this content, with its git history). Continue work here.
- `staunch0515/ubos-platform` — original repository; its `main` branch contains the same
  documents up to the migration, plus the older Java code of the research phase. Kept as an
  archive.
- The seven other research repositories (`ubos`, `ubos-system`, `ubos_core`, `ubos_web`,
  `logicorum`, `logrum-system`, `fund-ubos`) are inputs only (`docs/50-system/00-meta/06-inputs.md`).

## 7. How to resume

1. Read `CLAUDE.md`, this file, `docs/50-system/README.md` and
   `docs/50-system/00-meta/05-ai-reading-guide.md`.
2. Run `bash docs/tools/regen_all.sh` and confirm 0 errors.
3. Report to the owner in Chinese: your understanding of the state, and a recommended next
   step from §4. Wait for confirmation before changing documents.
