---
id: META-RULES
title: Writing Rules
status: stable
phase: P0
depends_on: [META-CHARTER]
---

# Writing Rules

These rules are **normative** for every file under `docs/`. An AI writing or editing any
document in this set MUST follow them.

## 1. Language and encoding

- R1.1 All documents are written in **English**.
- R1.2 Source material in Chinese is translated; the original term may be kept once in
  parentheses when it carries meaning, e.g. `Three-Layer System (三层体系)`.
- R1.3 UTF-8, LF line endings, GitHub-flavored Markdown.
- R1.4 Identifiers, schema field names, code names: `snake_case` for data fields,
  `PascalCase` for types/entities, `kebab-case` for file names.

## 2. File layout

```
docs/
  README.md                      entry point (P5; stub until then)
  00-meta/                       rules, templates, progress, AI guide
  10-inventory/                  P0: one file per repo + summary
  20-analysis/<KEY>-<repo>/      P1: fixed file set per repo (TEMPLATES §A)
  30-comparison/                 P2: one file per topic + matrix
  40-spec/                       P3/P4: the target specification
```

- R2.1 File names: `NN-kebab-name.md`, where `NN` is a two-digit ordering prefix.
- R2.2 **Size limit**: a file SHOULD stay under ~800 lines / ~40 KB. If larger, split into
  `NN-name/` directory with `00-index.md` + parts.
- R2.3 One topic per file. Never mix two chapters in one file.

## 3. Front matter (mandatory)

Every file starts with YAML front matter:

```yaml
---
id: <STABLE-ID>              # see §4
title: <Human title>
status: draft | review | complete | stable
phase: P0 | P1 | P2 | P3 | P4 | P5
depends_on: [<ID>, ...]      # documents an AI must read first
sources: [<KEY>, ...]        # source repos consulted (optional for meta files)
---
```

## 4. Identifier scheme

All IDs are uppercase, hyphen-separated, and **never reused or renumbered**. Deprecated IDs
are marked `status: deprecated`, not deleted.

| Kind | Pattern | Example | Where defined |
|---|---|---|---|
| Document | `META-*`, `INV-<KEY>`, `ANA-<KEY>-NN`, `CMP-NN`, `SPEC-NN` | `ANA-UC-03` | front matter |
| Highlight (from analysis) | `HL-<KEY>-NNN` | `HL-UB-004` | `20-analysis/*/08-highlights.md` |
| Design concept (observed) | `CON-<KEY>-NNN` | `CON-US-012` | analysis files |
| Comparison verdict | `VRD-NN-NN` | `VRD-03-02` | `30-comparison/*` |
| Requirement | `REQ-<MOD>-NNN` | `REQ-META-014` | `40-spec/*` |
| Non-functional req. | `NFR-<AREA>-NNN` | `NFR-PERF-003` | `40-spec/*` |
| Architecture decision | `ADR-NNN` | `ADR-001` | `40-spec/90-adr/` |
| Entity / type | `ENT-<Name>` | `ENT-EntityDefinition` | `40-spec/*` |
| Interface / API | `API-<MOD>-NNN` | `API-RT-007` | `40-spec/*` |
| Glossary term | `TERM-<Name>` | `TERM-Rehydration` | `40-spec/01-glossary/` |
| Open question | `Q-NNN` | `Q-017` | `00-meta/OPEN-QUESTIONS.md` |

`<MOD>` codes are defined in the spec outline (P3). Until then use the provisional code
`TBD`.

## 5. Writing style (AI-oriented)

- R5.1 **Structure over prose.** Prefer tables, numbered lists, YAML/pseudo-schemas,
  state tables, sequence lists. Prose only for rationale.
- R5.2 **Explicit over implicit.** No "etc.", no "and so on", no "as usual". Enumerate.
- R5.3 **Normative keywords** (spec phase): MUST, MUST NOT, SHOULD, MAY (RFC 2119 meaning).
- R5.4 **Self-contained sections.** Each `##` section should be understandable after reading
  only the file's `depends_on` list. Repeat short context rather than rely on "see above".
- R5.5 **Facts vs. interpretation.** In analysis files, mark interpretation explicitly:
  `> Interpretation:` blockquote. Everything else must be observable in the source.
- R5.6 **No quality judgement** of code (charter §4). Say *what* the design is and *why* it
  is interesting, not whether the code is good.
- R5.7 Diagrams: use Mermaid or ASCII inside fenced blocks only. Every diagram must also be
  described in text/table form (an AI may not render it).
- R5.8 Code snippets: at most ~30 lines, only when they express a design idea better than
  prose (e.g. a core trait/interface, a metadata sample). Always cite the path.

## 6. Source references

- R6.1 Format: `[KEY:path/to/file#Lnn-Lmm]` or `[KEY:path/to/dir/]`.
  Example: `[UC:src/engine/mod.rs#L10-L40]`, `[UB:docs/Rehydration.md]`.
- R6.2 Paths are relative to that repository's root on branch `claude/gifted-edison-cmqbu9`.
- R6.3 Every `CON-*` and `HL-*` item MUST have at least one source reference.
- R6.4 Every spec requirement MUST have an `Origin:` field listing `HL-*`, `CON-*`, `VRD-*`
  IDs, or `NEW` with a one-line rationale.

## 7. Status lifecycle

`draft` → `review` → `complete` → (`stable` for meta/conventions only).
- `draft`: being written; may contain `TODO(<phase>):` markers.
- `complete`: no TODO markers; all required template sections present.
- A phase is finished only when all its files are `complete`.

## 8. Cross-referencing

- R8.1 Reference by ID, and add the relative link when helpful: `ADR-003 (../40-spec/90-adr/003-*.md)`.
- R8.2 Never reference by section number alone ("see §3.2") across files.
- R8.3 Glossary terms are written in **bold** at first use in each file.

## 9. Session protocol (for every AI session working on these docs)

1. Read `00-meta/PROGRESS.md`, then `DOC-CHARTER.md`, this file and `TEMPLATES.md`.
2. Pick the next unchecked task in `PROGRESS.md`.
3. Write/modify files following the relevant template.
4. Update `PROGRESS.md` (tick tasks, append a log line, record new open questions in
   `OPEN-QUESTIONS.md`).
5. Commit with message `docs(<phase>): <what>` and push to branch
   `claude/gifted-edison-cmqbu9` of `staunch0515/ubos-platform`.
6. Commit at least once per completed file group; never leave work only in the container.
