---
id: ANA-LC-08
title: "LC Highlights"
status: complete
phase: P1
depends_on: [ANA-LC-01, ANA-LC-02, ANA-LC-03, ANA-LC-04, ANA-LC-05, ANA-LC-06]
sources: [LC]
---

# LC — Highlights

### HL-LC-001 — API = catalogue of versioned declarative processes
- **Summary:** Every endpoint is a named, versioned process defined in JSON as a pipeline of configured, reusable handler steps; input is validated by the process's own JSON Schema with defaults; access by `requiredRoles`.
- **Why it stands out:** New business operations need no controller code; processes are reviewable text; versions coexist.
- **Concepts:** CON-LC-001, CON-LC-005, CON-LC-010, CON-LC-020
- **Reuse recommendation:** adopt (compare with UP/US script logic and LS behavior slots in P2 topics 5–6)
- **Sources:** [LC:data/processes/], [LC:src/main/java/com/logicorum/core/LogicExecutionEngine.java]
- **Tags:** process, dsl, api

### HL-LC-002 — Unit of work: stage change sets, commit once
- **Summary:** Steps stage `EntityChangeSet`s; one persistence step commits them atomically under one process log entry, deriving CREATE/UPDATE/DELETE.
- **Why it stands out:** Clean transaction boundary per business operation; validators can reason over the whole pending change graph.
- **Concepts:** CON-LC-012, CON-LC-022
- **Reuse recommendation:** adopt
- **Sources:** [LC:src/main/java/com/logicorum/handler/PersistenceSaveHandlerV1.java]
- **Tags:** process, persistence, versioning

### HL-LC-003 — Relationships as versioned entities with a typed catalogue
- **Summary:** `RELATIONSHIP_V1` instances carry source, target, type, validity dates and metadata; a catalogue defines allowed endpoint types, symmetry, validity requirement and a metadata schema.
- **Why it stands out:** Real business relations (reporting lines, marriage, role assignments, ownership) with history, attributes and time validity — beyond plain foreign keys or UP's unversioned edge table.
- **Concepts:** CON-LC-014, CON-LC-017
- **Reuse recommendation:** adopt (fuse with LS/US "everything is a relation" in P2 topic 2)
- **Sources:** [LC:data/relationships/common.rtp.json], [LC:docs/Redis.md]
- **Tags:** relationship, metadata

### HL-LC-004 — Staged validation: JSON Schema + graph constraints, DRAFT_SAVE vs FINAL_PUBLISH
- **Summary:** Structural schema validation plus pluggable business constraints (unique, mandatory relationship cardinality, relationship type checks), each tagged with the level at which it applies.
- **Why it stands out:** Lets users save incomplete work and enforces completeness at publish — a lifecycle concept expressed purely in metadata.
- **Concepts:** CON-LC-015, CON-LC-023, CON-LC-024
- **Reuse recommendation:** adopt (compare with UB draft→publish, FU schema-on-commit, P2 topic 9)
- **Sources:** [LC:src/main/java/com/logicorum/validation/], [LC:data/schemas/quiz/QUIZ_V1.schema.json]
- **Tags:** rules, schema, workflow

### HL-LC-005 — Cardinality constraints over pending + persisted relationships
- **Summary:** `mandatoryRelationships` counts relationships staged in the current process plus active ones in the database (respecting `end_date`).
- **Why it stands out:** Aggregate-level invariants that work within a single request creating several related objects.
- **Concepts:** CON-LC-024, CON-LC-040
- **Reuse recommendation:** adopt
- **Sources:** [LC:src/main/java/com/logicorum/validation/MandatoryRelationshipValidator.java]
- **Tags:** rules, relationship

### HL-LC-006 — Production concerns as platform features
- **Summary:** Idempotency keys with response replay and stale-lock recovery; optimistic locking handler; request log with device/latency/status; JSON transaction files; email outbox with scheduler.
- **Why it stands out:** Every app built on the platform inherits them; none of the other repos covers idempotency or request-level audit.
- **Concepts:** CON-LC-003, CON-LC-025, CON-LC-026, CON-LC-027, CON-LC-044
- **Reuse recommendation:** adopt
- **Sources:** [LC:src/main/java/com/logicorum/service/IdempotencyService.java], [LC:src/main/java/com/logicorum/api/ProcessController.java]
- **Tags:** api, observability, integration

### HL-LC-007 — Versioning of contracts by name (types, processes, handlers)
- **Summary:** `_V1/_V2` suffixes on entity types, processes and handlers; process log records process and handler versions.
- **Why it stands out:** Explicit coexistence of contract versions ("parallel business systems in one instance").
- **Concepts:** CON-LC-013, CON-LC-041
- **Reuse recommendation:** adapt (combine with the version chain so contracts are also versioned entities)
- **Sources:** [LC:docs/README.md]
- **Tags:** versioning, api

### HL-LC-008 — Command/query split with one step model
- **Summary:** POST runs the transactional command engine; GET runs a read-only query engine over the same process format; queries are logged.
- **Concepts:** CON-LC-002
- **Reuse recommendation:** adopt
- **Sources:** [LC:src/main/java/com/logicorum/core/ReadOnlyQueryEngine.java]
- **Tags:** process, query

### HL-LC-009 — Declarative branching in pipelines
- **Summary:** Any step can set `nextStepName`; a decision step evaluates an expression and chooses `trueStep`/`falseStep`.
- **Why it stands out:** Minimal control flow sufficient for most business processes without a workflow engine.
- **Concepts:** CON-LC-021
- **Reuse recommendation:** adapt (expression language to be chosen in spec)
- **Sources:** [LC:src/main/java/com/logicorum/handler/DecisionHandlerV1.java]
- **Tags:** workflow, rules

### HL-LC-010 — Data-driven test cases
- **Summary:** Test suites are ordered JSON lists of process invocations.
- **Why it stands out:** Tests in the platform's own language — ideal for AI-generated and business-authored tests.
- **Concepts:** CON-LC-061
- **Reuse recommendation:** adopt
- **Sources:** [LC:data/testcase/]
- **Tags:** dsl

### HL-LC-011 — Validated by a real vertical: QuizBucks, and the template/publication pattern
- **Summary:** A complete quiz-for-cash product (sessions, wallets, rewards, transactions, relationships, admin) runs on the generic engine; the domain design separates editable templates from frozen publications.
- **Why it stands out:** Only repo with an end-to-end business application; exposes real needs (double-entry, funds checks, locks, idempotency, statistics).
- **Concepts:** CON-LC-018, CON-LC-028, CON-LC-051, CON-LC-052
- **Reuse recommendation:** adopt the template/publication pattern; use QuizBucks as a reference domain pack
- **Sources:** [LC:docs/Main.md], [LC:docs/finance.md]
- **Tags:** domain-pack, finance

### HL-LC-012 — Typed EAV per commit with temporal SQL
- **Summary:** Version content stored as typed attribute rows keyed by commit; attribute and temporal-validity queries are plain SQL.
- **Why it stands out:** Query-friendly alternative to JSON snapshots.
- **Concepts:** CON-LC-040
- **Reuse recommendation:** adapt (compare with JSON snapshot + index in UP/FU/UB, P2 topic 1/12)
- **Sources:** [LC:data/database/init_db/005_lcm_entity_attribute_value.sql]
- **Tags:** persistence, query

### HL-LC-013 — Roles and process grants as relationships
- **Summary:** `HAS_ROLE` (user→role, validity) and `GRANTS_ACCESS_TO` (role→process, validity) put access control in the business graph.
- **Concepts:** CON-LC-017
- **Reuse recommendation:** adapt (fuse with FU policies, P2 topic 10)
- **Sources:** [LC:data/relationships/role.rtp.json]
- **Tags:** permission, relationship
