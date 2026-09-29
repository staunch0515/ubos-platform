---
id: FIN-DOC-40
title: Comparison Protocol
status: draft
---

# Comparison Protocol

The same requirements are implemented on two systems: **System U** (the UBOS platform with a
Basic Finance business package) and **System J** (the owner's Java system). This protocol
fixes what is measured, how, and how the results are reported, so that the comparison is
fair and repeatable.

## 1. Principles

1. **Same input.** Both teams work from this folder at the same version (git tag). A change
   to the requirements is tagged and applied to both.
2. **Same oracle.** Both implementations are accepted only by FIN-SCN-01 … FIN-SCN-15 and
   the expected results FIN-EXP-*, run by a person who did not build the implementation.
3. **Record while working.** Effort and defects are recorded when they happen, not
   estimated afterwards.
4. **Scope differences are explicit.** Every requirement not met, or met differently, is
   logged per requirement with a reason.
5. **No tuning to the test.** Expected results are not typed into either system (§3 of
   `20-sample-company.md`).

## 2. Work items

Work is recorded per **work item** = one requirement (FIN-*) or one scenario-level task
(for example "statement import formats"). Each work item record contains:

| Field | Meaning |
|---|---|
| system | U or J |
| work item | requirement or task ID |
| kind | build, change, defect fix, rework |
| effort | person-hours, by role (developer, analyst, tester, designer) |
| calendar | start and end dates |
| artefacts | files or configuration items changed; for System U, which of them are configuration or model versus code |
| size | lines of code added/changed/deleted by language; for configuration or models, the number of definitions changed |
| tests | automated tests added |
| defects | defects found later that trace to this work item, with severity and where found (review, test, acceptance, operation) |
| notes | anything that affects comparability |

## 3. Metrics

| Metric | Definition | Unit |
|---|---|---|
| FIN-MET-01 | Total effort to pass all Must requirements (sum of work items of kind build and rework) | person-hours |
| FIN-MET-02 | Effort per area (GL, PC, AR, AP, BK, FA, FX, TX, RP, CT, SC, DI, NF, UI) | person-hours |
| FIN-MET-03 | Calendar time from start to first full acceptance | weeks |
| FIN-MET-04 | Size of the implementation: code by language, and configuration or model definitions | lines and definitions |
| FIN-MET-05 | Share of requirements met without writing general-purpose code (configuration, models, rules only) | % |
| FIN-MET-06 | Defects found in acceptance and in the first 3 months of operation per 100 Must requirements, by severity | count |
| FIN-MET-07 | Expected-result mismatches at first acceptance run | count of FIN-EXP lines that differ |
| FIN-MET-08 | Change lead time for the standard change requests of §4 (from request to accepted in production) | hours |
| FIN-MET-09 | Effort for the standard change requests of §4 | person-hours |
| FIN-MET-10 | Performance at FIN-NF-001 volumes: the FIN-NF-002 percentiles | ms |
| FIN-MET-11 | Usability: task success and SUS in the same study design (FIN-UI-001) | %, score |
| FIN-MET-12 | Audit support: time to answer the audit requests of FIN-SCN-12 and whether the evidence was verifiable without system access | hours, yes/no |
| FIN-MET-13 | Requirements met: Must, Should and Could met per system | count |
| FIN-MET-14 | Operational effort: installation, upgrade and restore drill time (FIN-SCN-14) | hours |

## 4. Standard change requests

After first acceptance, both teams implement the same change requests, one at a time, with
effort and lead time recorded (FIN-MET-08, FIN-MET-09):

| Change | Content |
|---|---|
| CR-A | Lower the manual-journal approval threshold to 5,000.00 from next month and add a second approver above 50,000.00 |
| CR-B | Add a new state (for example Colorado with state and local rates) with an exemption certificate type |
| CR-C | Add a dimension "department" to journals, invoices and bills and an income statement by department |
| CR-D | Change FA-002's remaining useful life (change in estimate) and add the 150% declining-balance method |
| CR-E | Add a customer early-payment discount 2/10 net 30 with a sales-discount account |
| CR-F | Correct a posted rate for 2026-01-31 and show the effect on January's revaluation without changing issued reports |

Each change is accepted by an acceptance step written with the change and added to the
scenarios before either team starts.

## 5. Procedure

1. Tag the requirements version; both teams start from it.
2. Each team keeps its work-item records in the common template (CSV with the fields of §2).
3. When a team declares a scenario ready, an independent tester runs it and records the
   result and any FIN-EXP mismatch.
4. After first full acceptance, run the change requests of §4.
5. Run FIN-SCN-14 (performance, operations, usability, accessibility) on comparable
   hardware, stated in the report.
6. Collect three months of operation data where the systems are used for demonstrations.

## 6. Report

The comparison report contains, per metric, both values, the ratio, and the notes that
limit comparability (team size and experience, reuse of existing components, scope
differences). It separates **first-build** effort from **change** effort, because the
platform is expected to show its value mainly in changes, audit support and correctness.
The report is written for the owner; neither team edits the other's data.
