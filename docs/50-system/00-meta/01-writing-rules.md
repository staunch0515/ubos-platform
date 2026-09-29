---
id: UBS-META-01
title: Writing Rules and Identifier Scheme
status: stable
phase: PH-0
depends_on: [UBS-META-00]
---

# Writing Rules and Identifier Scheme

These rules are **normative** for every file in `docs/50-system/`. An AI that writes or
edits this volume MUST follow them. `tools/check_system.py` enforces the mechanical rules.

## 1. Language, encoding and style

- W1.1 Write in **English** (US spelling). Chinese source terms may appear once in
  parentheses when they carry meaning.
- W1.2 Use UTF-8, LF line endings and GitHub-flavored Markdown.
- W1.3 Normative keywords follow RFC 2119 / RFC 8174: **MUST, MUST NOT, SHOULD, SHOULD NOT,
  MAY**. They carry that meaning only when written in capitals.
- W1.4 **Structure over prose.** Prefer tables, numbered lists, schemas, grammars, state
  tables and Given/When/Then criteria. Use prose only for rationale.
- W1.5 **Be explicit.** Do not write "etc.", "and so on", "as usual" or "similar". Enumerate.
- W1.6 **Self-contained sections.** A `##` section MUST be understandable after reading the
  documents in the file's `depends_on`. Do not reference a section by number alone across
  files. Reference by ID.
- W1.7 **One term, one meaning.** Use glossary terms (`GL-*`) exactly. Do not use synonyms
  for a defined term.
- W1.8 **Diagrams** are Mermaid or ASCII inside fenced blocks. Every diagram MUST also be
  described in a table or list, because an AI reader may not render it.
- W1.9 **Examples** are marked `Example:` and are never normative.

## 2. File layout and size

- W2.1 File names are `NN-kebab-name.md`. `NN` is a two-digit ordering prefix.
- W2.2 A file SHOULD stay under about 800 lines. A larger topic becomes a folder
  `NN-name/` containing `00-index.md` and the parts.
- W2.3 Each file has one topic.
- W2.4 Files in `INDEX/` are generated. Never edit them by hand.

## 3. Front matter (mandatory)

```yaml
---
id: UBS-<AREA>-<NN>          # stable document ID
title: <Human title>
status: draft | review | complete | stable
phase: PH-0 | PH-1 | PH-2 | PH-3 | PH-4 | PH-5 | ALL
depends_on: [<document IDs>]   # what an AI must read first
---
```

`phase` in front matter is the earliest phase the document's content is needed in.

## 4. Identifier scheme

IDs are uppercase ASCII and hyphen-separated. **IDs are never renumbered or reused.** A
withdrawn item keeps its ID and is marked `**Status:** Withdrawn` with a reason.

| Kind | Pattern | Example | Defined in |
|---|---|---|---|
| Document | `UBS-<AREA>-<NN>` or `UBS-SYS-<SYS>-<NN>` | `UBS-REQ-05`, `UBS-SYS-DVM-03` | front matter |
| Persona | `PER-<PascalName>` | `PER-FundAccountant` | `10-requirements` |
| Capability | `CAP-<DOM>-<NN>` | `CAP-VER-03` | capability map |
| Scenario | `SCN-<NNN>` | `SCN-012` | scenarios |
| Functional requirement | `FR-<DOM>-<NNN>` | `FR-TIME-004` | functional catalogue |
| Non-functional requirement | `NR-<AREA>-<NNN>` | `NR-PERF-010` | NR catalogue |
| Compliance requirement | `CR-<REG>-<NNN>` | `CR-SEC-003` | compliance catalogue |
| Constraint | `CST-<NNN>` | `CST-004` | constraints |
| Assumption | `ASM-<NNN>` | `ASM-002` | constraints |
| Standard clause | `STD-<AREA>-<NNN>` | `STD-ISA-012` | `30-standard` |
| Design requirement | `DSN-<SYS>-<NNN>` | `DSN-DVM-101` | `40-systems` |
| Interface | `IF-<SYS>-<NNN>` | `IF-NOD-004` | `40-systems`, `30-standard` |
| Data structure | `DAT-<PascalName>` | `DAT-Commit` | `30-standard`, `40-systems` |
| Error code | `<SYS or DOM>.<UPPER_SNAKE>` | `DVM.HEAD_MOVED` | error registry |
| Glossary term | `GL-<PascalName>` | `GL-Commit` | glossary |
| Phase | `PH-<N>` | `PH-2` | `50-phases` |
| Exit criterion | `EXIT-<N>-<NN>` | `EXIT-1-07` | phase plans |
| Verification item | `VER-<METHOD>-<NNNN>` | `VER-CONF-0042` | `60-verification` |
| Verification suite | `SUITE-<NAME>` | `SUITE-DVM-CORE` | `60-verification` |
| Metric | `MET-<AREA>-<NNN>` | `MET-PERF-003` | phase plans, NR |
| Risk | `RSK-<NNN>` | `RSK-017` | phase plans |
| Decision | `DEC-<NNN>` | `DEC-011` | `90-decisions` |
| Open question | `OQ-<NNN>` | `OQ-004` | `00-meta/03-progress.md` |
| Input reference | `IN-<NN>` / `IMP-<NN>` | `IMP-02` | `00-meta/06-inputs.md` |

### 4.1 Domain codes (`<DOM>`) for capabilities and functional requirements

| Code | Domain | Code | Domain |
|---|---|---|---|
| MODEL | Types, classes, inheritance, polymorphism | UX | User experience, SDUI, workspace |
| VER | Versioning, branches, merge, release | OFFICE | Live documents, smart grids, war rooms, action messages |
| TIME | Bitemporal time, as-of queries | AI | AI gateway, agents, copilot, builder |
| LEDG | Ledger-kind state, postings, corrections | PKG | Buks (packages), install, upgrade |
| TXN | Commit, processes, atomicity, idempotency | INT | Integration, connectors, APIs, webhooks |
| LOGIC | Logic runtime, instruction set, sandbox | ANL | Analytics, projections, reporting, export |
| RULE | Governance sheets (LGS), validation, decisions | PROOF | Signatures, Merkle proofs, anchoring, evidence |
| FLOW | Lifecycles, actions, workflows, approvals | SYNC | Replication, offline, federation, VAE tree |
| EVT | Events, jobs, schedules, workers | OPS | Operations, backup, upgrade, monitoring |
| QRY | Query, search, retrieval | BILL | Metering, licensing, billing |
| IAM | Identity, authentication, authorization | DEV | Developer experience, SDK, CLI, toolchain |
| TEN | Tenancy, VAEs, isolation, overlays | MIG | Migration and import from legacy systems |
| AUD | Business audit trail, lineage, explanations | NTF | Notifications and messaging |
| FILE | Files, attachments, blobs | SIGN | Electronic signatures |
| LOC | Localization, calendars, currencies, time zones | STD | Standard governance and conformance |

### 4.2 System codes (`<SYS>`)

| Code | System | Code | System |
|---|---|---|---|
| BPA | BPA standard and conformance suite | AGT | Agent Hub |
| DVM | DVM reference kernel | EXC | Buk Exchange |
| FRG | Logic Forge toolchain | FED | Federation |
| NOD | UBOS Node (Box, Server, Cell, Worker role) | NTY | Notary |
| CTL | Control Plane | BRG | Bridge (data and integration) |
| WSP | Workspace | SDK | Client SDKs (shared by all clients) |
| STU | Studio | | |

### 4.3 Non-functional areas (`<AREA>` in `NR-*`)

PERF (performance), SCAL (scalability), AVAIL (availability), DUR (durability and
recovery), DET (determinism), SEC (security), PRIV (privacy), OBS (observability),
OPER (operability), USE (usability), ACC (accessibility), PORT (portability), COMPAT
(compatibility and evolution), MAINT (maintainability), COST (resource efficiency),
LOC (internationalisation).

### 4.4 Compliance codes (`<REG>` in `CR-*`)

SOC2, SEC (SEC Rule 17a-4 and Rule 204-2), FINRA (Rule 4511), SOX, HIPAA, GDPR, CCPA
(including CPRA), ESIGN (E-SIGN Act and UETA), WCAG (WCAG 2.2 AA and Section 508), NIST
(NIST SP 800-53 moderate baseline, used as the control vocabulary).

### 4.5 Verification method codes (`<METHOD>`)

| Code | Method | Typical evidence |
|---|---|---|
| CONF | Conformance vectors: language-neutral inputs, expected outputs and errors | vector files plus a runner report |
| PROP | Property-based testing of algebraic and invariant properties | property definitions plus a shrunk counter-example log |
| SIM | Deterministic simulation of concurrency, crashes and partitions | seeds, simulated hours and a violation report |
| FAULT | Fault injection on real processes and storage | fault matrix plus recovery report |
| FORM | Formal model and model checking (TLA+ or equivalent) | model, checked invariants, state-space size |
| SCN | End-to-end scenario suites | scenario scripts plus a run report on all bindings |
| BENCH | Benchmarks against NR targets on the reference environment | benchmark report with percentiles |
| SEC | Security assessment: penetration test, sandbox escape tests, permission matrix | findings report |
| USE | Usability study with target personas | task success rate, time on task, SUS score |
| PILOT | Pilot acceptance with a design-partner customer | signed acceptance record plus metrics |
| INSP | Inspection of documents, schemas or configurations | review checklist record |
| AUDIT | Third-party audit or attestation | auditor report (for example SOC 2 Type II) |

## 5. Requirement block format

Every `FR-*`, `NR-*` and `CR-*` item uses this block. Fields are in this order and none is
omitted.

```markdown
### FR-<DOM>-<NNN> — <short title>
- **Statement:** <one or two sentences with exactly one RFC 2119 keyword per obligation>
- **Rationale:** <why this matters to a persona or to the architecture>
- **Priority:** Must | Should | Could · **Phase:** PH-<N> · **Systems:** <SYS, SYS>
- **Personas:** <PER-*, …> (or `—` for platform-internal requirements)
- **Acceptance:**
  1. Given <state>, when <action>, then <observable result>.
  2. …
- **Verification:** <METHOD, METHOD>
- **Origin:** <IN-*, IMP-*, L40:<id>, EXT-<doc>, or NEW>
```

Rules:
- R5.1 **Priority** means: *Must* = the phase cannot exit without it; *Should* = expected in
  the phase and deferrable only by a DEC record; *Could* = optional in the phase.
- R5.2 **Phase** is the phase in which the requirement is first fully delivered. A
  requirement delivered in part is split into two IDs.
- R5.3 Each acceptance criterion MUST be observable and automatable, or it MUST name a
  USE, PILOT, INSP or AUDIT method.
- R5.4 Verification items (`VER-*`) are assigned in `60-verification/`. The traceability
  index links each requirement to them.

## 6. Origin codes

| Code | Meaning |
|---|---|
| `IN-01` … | An input source summarised in `UBS-META-06` |
| `IMP-01` … `IMP-12` | The twelve accepted improvement proposals (`UBS-META-06` §3) |
| `L40:<ID>` | An item of the earlier volume, for example `L40:SPEC-14` or `L40:REQ-VER-003`. Only for traceability. This volume never depends on it. |
| `EXT-WP`, `EXT-RFC`, `EXT-TRI`, `EXT-BP`, `EXT-BPU` | The external input documents (`UBS-META-06` §2) |
| `NEW` | A requirement introduced in this volume. A one-line reason follows in `Rationale`. |

## 7. Status lifecycle

A document moves through `draft` → `review` → `complete`. Meta documents may become
`stable`.
- `draft` may contain `TODO(<PH-N or batch>):` markers.
- `review` is complete but waits for product-owner confirmation.
- `complete` has no TODO markers, and every required template section is present.

## 8. Session protocol for AI writers

1. Read `03-progress.md`, then this file, `02-templates.md` and `06-inputs.md`.
2. Take the next unchecked batch in `03-progress.md`.
3. Write the files with the templates.
4. Run `python3 docs/tools/check_system.py`. It MUST exit 0.
5. Update `03-progress.md`: tick the tasks and append a log line.
6. Commit as `docs(system): <what>` and push to `claude/gifted-edison-cmqbu9`.
