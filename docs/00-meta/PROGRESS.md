---
id: META-PROGRESS
title: Progress Tracker
status: draft
phase: P0
depends_on: [META-CHARTER, META-RULES, META-TEMPLATES]
---

# Progress Tracker

> Every session: read this file first, update it last (WRITING-RULES §9).

## Current phase: P2 (next: fix topic list in `30-comparison/00-matrix.md`; P1 complete)

## P0 — Meta + Inventory
- [x] `00-meta/DOC-CHARTER.md`
- [x] `00-meta/WRITING-RULES.md`
- [x] `00-meta/TEMPLATES.md`
- [x] `00-meta/PROGRESS.md`
- [x] `00-meta/OPEN-QUESTIONS.md`
- [x] `00-meta/AI-GUIDE.md` (stub; finalized in P5)
- [x] `10-inventory/01-UP-ubos-platform.md`
- [x] `10-inventory/02-LC-logicorum.md`
- [x] `10-inventory/03-UB-ubos.md`
- [x] `10-inventory/04-LS-logrum-system.md`
- [x] `10-inventory/05-US-ubos-system.md`
- [x] `10-inventory/06-UC-ubos_core.md`
- [x] `10-inventory/07-UW-ubos_web.md`
- [x] `10-inventory/08-FU-fund-ubos.md`
- [x] `10-inventory/00-summary.md` (cross-repo overview, lineage hypotheses, P1 order)

## P1 — Per-repo analysis (TEMPLATES §A, 10 files each)
Order fixed in INV-SUMMARY §5. Folder: `20-analysis/<KEY>-<repo>/`.
- [x] 1. UP ubos-platform   → `20-analysis/UP-ubos-platform/` (41 concepts, 12 highlights)
- [x] 2. FU fund-ubos       → `20-analysis/FU-fund-ubos/` (37 concepts, 14 highlights; shared UP files referenced, not repeated)
- [x] 3. LC logicorum       → `20-analysis/LC-logicorum/` (40 concepts, 13 highlights)
- [x] 4. UB ubos            → `20-analysis/UB-ubos/` (57 concepts, 16 highlights; digest split into 3 parts, D-UB-01…20)
- [x] 5. UC ubos_core       → `20-analysis/UC-ubos_core/` (58 concepts, 14 highlights; canonical digest of the UC/US shared corpus, D-UC-01…58)
- [x] 6. US ubos-system     → `20-analysis/US-ubos-system/` (48 US-specific concepts, 12 highlights; shared corpus via ANA-UC-07)
- [x] 7. LS logrum-system   → `20-analysis/LS-logrum-system/` (43 concepts, 11 highlights)
- [x] 8. UW ubos_web        → `20-analysis/UW-ubos_web/` (20 concepts, 5 highlights)

## P2 — Comparison
- [x] `30-comparison/00-matrix.md` (22 topics fixed)
- [x] 01 storage · 02 meta-model · 03 identity · 04 change unit · 05 logic execution
- [ ] 06–22 (see CMP-00 §1)

## P3 — Outline + conventions
- [ ] `40-spec/00-outline.md`
- [ ] `40-spec/01-glossary.md`
- [ ] `40-spec/02-conventions.md` (MOD codes, type system notation, ID registry)
- [ ] `40-spec/90-adr/001-technology-stack.md`

## P4 — Spec chapters
- [ ] (filled from outline)

## P5 — Consolidation
- [ ] `docs/README.md`
- [ ] `40-spec/INDEX.md`
- [ ] `00-meta/AI-GUIDE.md` final
- [ ] cross-reference validation

## Session log
| Date | Phase | Work done |
|---|---|---|
| 2026-09-28 | P0 | Created meta layer (charter, rules, templates, progress, open questions, AI guide stub). |
| 2026-09-28 | P0 | Inventoried all 8 repos + summary (families, shared files, P1 order, preliminary P2 topics). P0 complete. |
| 2026-09-28 | P1 | UP analysis complete (10 files). Serves as the reference example of TEMPLATES §A. |
| 2026-09-28 | P1 | FU analysis complete (10 files). Reconciliation notes for P2 recorded in ANA-FU-00. |
| 2026-09-28 | P1 | LC analysis complete (10 files). |
| 2026-09-28 | P1 | UB analysis complete (9 files + digest folder with index and 3 parts). |
| 2026-09-28 | P1 | UC analysis complete (9 files + digest folder with index and 3 parts). |
| 2026-09-28 | P1 | US analysis complete (10 files; only US-specific concepts, UC concepts referenced). |
| 2026-09-28 | P1 | LS analysis complete (10 files). |
| 2026-09-28 | P1 | UW analysis complete (10 files). P1 complete for all 8 repositories. |
| 2026-09-28 | P2 | Matrix + topics 01–05 with verdicts; Q-005, Q-006 raised. |
