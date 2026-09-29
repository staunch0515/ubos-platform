---
id: UBS-META-02
title: Templates
status: stable
phase: PH-0
depends_on: [UBS-META-01]
---

# Templates

Each template below is mandatory for its item kind. Headings and field names are fixed.
An AI MUST NOT rename, reorder or drop fields. When a field does not apply, write `—`.

## T1. Persona (`PER-*`)

```markdown
### PER-<PascalName> — <display name>
- **Segment:** <organisation type and size>
- **Role:** <job title(s)>
- **Goals:** 1. … 2. … 3. …
- **Pains today:** 1. … 2. …
- **Skills:** business depth <low|medium|high> · technical depth <none|low|medium|high>
- **Primary systems:** <SYS, …>
- **Concepts visible to this persona:** <GL terms the UI may show>
- **Concepts hidden from this persona:** <GL terms the UI must not show>
- **Success signal:** <measurable outcome>
```

## T2. Capability (`CAP-*`)

```markdown
| CAP-<DOM>-<NN> | <name> | <one-sentence description> | <PER-*> | PH-<N> | <FR ID range> |
```
Capabilities are listed in tables grouped by domain. The FR range is filled once the
functional catalogue exists.

## T3. Scenario (`SCN-*`)

```markdown
### SCN-<NNN> — <title>
- **Goal:** <business outcome>
- **Personas:** <PER-*>
- **Systems:** <SYS, …>
- **Phase:** PH-<N> (first phase in which the scenario MUST pass end to end)
- **Preconditions:** 1. …
- **Main flow:**
  1. <actor> <action> → <system response>
  2. …
- **Alternate and failure flows:**
  - A1. <condition> → <behaviour>
- **Postconditions (observable):** 1. …
- **Business rules exercised:** <FR-*, CR-*>
- **Verification:** SCN (+ other methods)
```

## T4. Functional, non-functional and compliance requirement

This template is defined in `UBS-META-01` §5.

`NR-*` items add one line after **Statement**:
`- **Target:** <metric, threshold, percentile, workload, environment>`

`CR-*` items add one line after **Statement**:
`- **Control reference:** <regulation section or control ID>`

## T5. Standard clause (`STD-*`)

```markdown
### STD-<AREA>-<NNN> — <title>
- **Clause:** <normative text with RFC 2119 keywords>
- **Grammar / Schema:** <ABNF, JSON Schema or table, or `—`>
- **Conformance vectors:** <VER-CONF-* range>
- **Satisfies:** <FR-*, NR-*>
- **Notes:** <non-normative explanation>
```

## T6. Data structure (`DAT-*`)

````markdown
### DAT-<PascalName>
- **Purpose:** <one sentence>
- **Kind:** Definition | Ledger | System | Message | Projection
- **Stored in:** <table or store, or `message only`>
- **Schema:**
```yaml
DAT-<PascalName>:
  field_name: { type: <type>, required: true|false, description: <text> }
```
- **Invariants:** 1. …
- **Satisfies:** <FR-*, STD-*>
````

## T7. Interface (`IF-*`)

```markdown
### IF-<SYS>-<NNN> — <operation name>
- **Kind:** UBTP message | instruction | HTTP endpoint | CLI command | event | library API
- **Caller → Callee:** <SYS> → <SYS>
- **Request:** <DAT-* or inline schema>
- **Response:** <DAT-* or inline schema>
- **Errors:** <error codes>
- **Idempotency:** <rule>
- **Authorization:** <permission or policy action>
- **Satisfies:** <FR-*, STD-*>
```

## T8. Design requirement (`DSN-*`)

Design requirements use the requirement block in `UBS-META-01` §5. **Origin** lists the
`FR-*`, `NR-*` or `STD-*` items the design satisfies.

## T9. System chapter (folder `40-systems/<NN>-<sys>/`)

Each system folder MUST contain the following files. A file may be split into parts.

| File | Content |
|---|---|
| `00-index.md` | purpose, responsibilities, non-responsibilities, personas served, phase map |
| `01-context.md` | context diagram, neighbouring systems, contracts consumed and provided |
| `02-functions.md` | functions as `DSN-*` items grouped by capability |
| `03-interfaces.md` | `IF-*` items |
| `04-data.md` | `DAT-*` items and storage layout (tables with purpose) |
| `05-state-machines.md` | lifecycles as transition tables |
| `06-configuration.md` | configuration keys, defaults, ranges, reload behaviour |
| `07-operations.md` | deployment, scaling, backup, monitoring, runbooks, failure modes |
| `08-verification.md` | system-level verification plan: suites, VER ranges, environments |

## T10. Phase plan (`50-phases/PH-<N>`)

Each phase document MUST contain these sections, in this order:

1. **Goal and business value**: one paragraph and three to seven measurable outcomes.
2. **Systems and components in scope**: a table of system, components and depth
   (`new` | `extended` | `hardened`).
3. **Feature list**: FR, NR and CR IDs grouped by capability, with priority.
4. **Dependencies**: prior exit criteria and external prerequisites.
5. **Out of scope for this phase**: an explicit list.
6. **Deliverables**: artifacts, releases and documents.
7. **Verification plan**:
   - 7.1 methods used and why;
   - 7.2 suites and VER ranges;
   - 7.3 environments and datasets;
   - 7.4 pass thresholds;
   - 7.5 who accepts and how acceptance is recorded;
   - 7.6 regression policy for earlier phases.
8. **Metrics**: `MET-*` with target values.
9. **Exit criteria**: `EXIT-<N>-<NN>`, each one binary and evidenced.
10. **Risks and mitigations**: `RSK-*`.
11. **Reference duration and team assumption**: non-normative.

## T11. Verification item (`VER-*`)

```markdown
### VER-<METHOD>-<NNNN> — <title>
- **Verifies:** <FR-*, NR-*, CR-*, STD-*, DSN-*>
- **Suite:** SUITE-<NAME> · **Phase:** PH-<N>
- **Environment:** <ENV-* or description>
- **Procedure:** 1. … 2. …
- **Pass criterion:** <binary, measurable>
- **Evidence:** <artifact produced>
- **Supports:** <EXIT-N-NN items this verification item evidences>
```

## T12. Decision record (`DEC-*`)

```markdown
### DEC-<NNN> — <title>
- **Status:** Proposed | Accepted | Superseded by DEC-<NNN> · **Date:** YYYY-MM-DD
- **Context:** <problem and forces>
- **Decision:** <what is decided>
- **Consequences:** <positive, negative, follow-ups>
- **Alternatives considered:** <list with one-line reason for rejection>
- **Affects:** <IDs or documents>
```

## T13. Glossary term (`GL-*`)

```markdown
### GL-<PascalName> — <Display Term>
- **Definition:** <one or two sentences>
- **Not to be confused with:** <GL-*>
- **Visible to:** <personas or `platform only`>
- **Defined normatively in:** <STD-* or document ID>
```
