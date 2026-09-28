---
id: META-AI-GUIDE
title: Guide for AI Readers
status: draft
phase: P0
depends_on: [META-CHARTER]
---

# Guide for AI Readers

> Stub. Finalized in P5.

## If you are continuing the documentation work
1. Read `00-meta/PROGRESS.md`.
2. Read `DOC-CHARTER.md`, `WRITING-RULES.md`, `TEMPLATES.md`.
3. Continue with the first unchecked task.

## If you are implementing the platform (after P4)
1. Read `40-spec/00-outline.md` for the chapter map and module codes.
2. Read `40-spec/01-glossary.md` and `40-spec/02-conventions.md`.
3. Read `40-spec/90-adr/` (all accepted ADRs).
4. For the module you implement: read its chapter plus every document in its `depends_on`.
5. Treat `REQ-*` Acceptance sections as your definition of done.
6. When a requirement's intent is unclear, follow its `Origin` to `30-comparison/` and
   `20-analysis/` for the reasoning and to the source repository for reference code.
