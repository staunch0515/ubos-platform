---
id: META-TEMPLATES
title: Document Templates
status: stable
phase: P0
depends_on: [META-RULES]
---

# Document Templates

Every document type has a fixed template. Section headings marked **(req)** are required;
a file cannot reach `status: complete` without them. Write "None observed." rather than
deleting a required section.

---

## §I. Inventory file — `10-inventory/NN-<KEY>-<repo>.md`

```
# Inventory: <repo>                                        (id: INV-<KEY>)
## 1. Identity (req)            table: key, repo, language(s), build tool, LoC, file counts, commit span, commit count
## 2. Top-level layout (req)    tree (depth 2-3) + one line per directory purpose
## 3. Modules / crates / packages (req)   table: name, path, role (one line)
## 4. Tech stack (req)          table: layer, technology, version if declared
## 5. Design documents (req)    table: path, language, topic (one line)  — every *.md except trivial
## 6. Entry points (req)        how the system boots / main binaries / frontend entry
## 7. Data assets (req)         schemas, seed data, sample DBs, metadata files
## 8. First impression (req)    3-7 bullets: what this repo seems to be about
## 9. Analysis reading plan (req)  ordered list of files to read in P1
```

---

## §A. Per-repo analysis set — `20-analysis/<KEY>-<repo>/`

Fixed file set (all required):

| File | ID | Content |
|---|---|---|
| `00-overview.md` | `ANA-<KEY>-00` | Vision & intent as stated by the repo; problem it tries to solve; summary of all other files |
| `01-architecture.md` | `ANA-<KEY>-01` | Layers, modules, dependency direction, process/deployment shape, request flow |
| `02-core-model.md` | `ANA-<KEY>-02` | Core abstractions: metadata/meta-model, entities, types, relationships, identity |
| `03-runtime.md` | `ANA-<KEY>-03` | Runtime mechanisms: boot, loading, execution, rules/scripts, processes/workflows, events, state |
| `04-data-persistence.md` | `ANA-<KEY>-04` | Storage model, schema strategy, multi-tenancy, versioning, migration, caching |
| `05-interface-ui.md` | `ANA-<KEY>-05` | API style, protocol, frontend architecture, dynamic rendering, UX concepts |
| `06-extensibility-ai.md` | `ANA-<KEY>-06` | Plugins, scripting, DSLs, AI/LLM integration, copilot, code generation |
| `07-design-docs-digest.md` | `ANA-<KEY>-07` | Digest of every design document in the repo (translated), one section per doc |
| `08-highlights.md` | `ANA-<KEY>-08` | **The highlights**: `HL-<KEY>-NNN` items (format below) |
| `09-concept-index.md` | `ANA-<KEY>-09` | Flat table of all `CON-<KEY>-NNN` defined in files 01-07 |

If a topic does not apply to a repo, the file still exists and states so with evidence.
Files may be split per WRITING-RULES R2.2.

### Concept item format (inside files 01-07)

```
### CON-<KEY>-NNN — <Short name>
- **What:** one-sentence definition
- **How:** mechanism, 2-8 bullets (or a table / schema)
- **Why (intent):** stated or inferred purpose (inferred → mark "Interpretation")
- **Sources:** [KEY:path#L..]
- **Tags:** comma list from the TAG VOCABULARY below
```

### Highlight item format (file 08)

```
### HL-<KEY>-NNN — <Short name>
- **Summary:** one sentence
- **Why it stands out:** what is unusual/strong compared with typical business platforms
- **Concepts:** CON-<KEY>-..., ...
- **Reuse recommendation:** adopt as-is | adapt | inspiration-only
- **Sources:** [KEY:...]
- **Tags:** ...
```

### Tag vocabulary (extend only via PROGRESS.md log entry)

`metadata`, `meta-model`, `entity`, `relationship`, `identity`, `type-system`,
`rules`, `scripting`, `dsl`, `workflow`, `process`, `event`, `state-machine`,
`context`, `rehydration`, `persistence`, `schema`, `multi-tenancy`, `versioning`,
`cache`, `query`, `search`, `api`, `protocol`, `ui-rendering`, `frontend`, `form`,
`view`, `i18n`, `auth`, `permission`, `org-model`, `finance`, `document`,
`ai`, `copilot`, `codegen`, `plugin`, `boot`, `deployment`, `desktop`, `cli`,
`observability`, `integration`, `domain-pack`.

---

## §C. Comparison topic — `30-comparison/NN-<topic>.md`

```
# Comparison: <topic>                                       (id: CMP-NN)
## 1. Question (req)            what design question this topic answers
## 2. Candidates (req)          table: KEY, approach name, CON/HL refs, one-line summary
## 3. Dimension matrix (req)    table: rows = dimensions (expressiveness, simplicity, runtime cost,
                                 AI-friendliness, extensibility, fit with platform vision, ...), cols = KEY
## 4. Analysis (req)            per-candidate strengths/limits (facts + marked interpretation)
## 5. Verdicts (req)            VRD-NN-MM items: decision (best-of KEY | fusion | NEW), rationale,
                                 consequences, what is dropped
## 6. Open questions            Q-NNN refs
```

Also `30-comparison/00-matrix.md`: topics × repos presence/strength table, and the list of
topics.

---

## §S. Spec chapter — `40-spec/NN-<chapter>.md`

```
# <Chapter title>                                           (id: SPEC-NN)
## 0. Chapter header (req)      scope, module code <MOD>, depends_on, terms used, origin summary
## 1. Concepts (req)            definitions (link TERM-*)
## 2. Model (req)               ENT-* definitions (schema format below)
## 3. Behavior (req)            REQ-* items, state machines, sequences
## 4. Interfaces (req)          API-* items
## 5. Non-functional (req)      NFR-* items relevant to this chapter
## 6. Acceptance (req)          checklist mapping REQ-* → verifiable criteria
## 7. Implementation notes      hints for the implementing AI, reference implementations in sources
## 8. Open questions            Q-NNN refs
```

### Requirement item

```
### REQ-<MOD>-NNN — <title>
- **Statement:** The system MUST ...
- **Rationale:** ...
- **Origin:** HL-..., VRD-..., or NEW: <reason>
- **Acceptance:** 1) ... 2) ...
- **Priority:** P0 (core) | P1 (important) | P2 (later)
```

### Entity definition

```yaml
ENT-<Name>:
  purpose: <one line>
  origin: [HL-..., VRD-...]
  fields:
    - name: <snake_case>
      type: <type from spec type system>
      required: true|false
      description: <one line>
  invariants:
    - <rule>
  relations:
    - <kind> -> ENT-<Other> (<cardinality>)
```

### Interface item

```
### API-<MOD>-NNN — <name>
- **Kind:** rust trait | java interface | http | event | cli
- **Signature / shape:** fenced block
- **Semantics:** pre/post conditions, errors
- **Origin:** ...
```

---

## §D. ADR — `40-spec/90-adr/NNN-<name>.md`

```
# ADR-NNN: <title>
- Status: proposed | accepted | superseded by ADR-MMM
- Context:
- Options considered: (each with pros/cons, source refs)
- Decision:
- Consequences:
- Origin: VRD-..., HL-...
```
