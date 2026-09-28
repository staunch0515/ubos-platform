---
id: ANA-LC-02
title: "LC Core Model"
status: complete
phase: P1
depends_on: [ANA-LC-01]
sources: [LC]
---

# LC — Core Model

### CON-LC-010 — Process definition format
- **What:** A JSON document declaring a business operation as an ordered list of configured steps.
- **How:**
```yaml
processName: AUTH_REGISTER_V1          # key; includes version suffix
description: string
inputSchema: <JSON Schema>             # validated before execution; "default" values applied
requiredRoles: [TAKER_ROLE_V1]         # checked against state.user_roles_slugs
steps:
  - stepName: string                   # jump target name
    handlerName | handlerClass: string # handler bean
    stepMetadata: {…}                  # handler-specific configuration
    type: string                       # optional
    isRemote: bool                     # designed, not executed
    remoteServiceUrl: string           # designed, not executed
```
  19 processes exist (auth login/logout/register/verify; generic `commit`; management cleanup; sponsor quiz draft/publish, sign-in/up; taker sign-up V1/V2, sign-in, verify email, send verification, start quiz, submit answer).
- **Why (intent):** Business behavior is configuration over a handler library.
- **Sources:** [LC:data/processes/], [LC:src/main/java/com/logicorum/core/ProcessDefinition.java], [LC:src/main/java/com/logicorum/core/Step.java].
- **Tags:** process, dsl, metadata

### CON-LC-011 — Process context as a blackboard
- **What:** One mutable context object carries everything through the steps.
- **How:** `processTransactionId` (UUID), `appId`, `operatorId`, `processName`, `status` (RUNNING, STARTED, SUCCESS, COMPLETED, INTERRUPTED, FAILED, CONFLICT), `state` (concurrent map: inputs + step outputs), `allChanges` (change sets keyed `type::id`), `messages`, `defaultBranchName`, `processDefinition`. Steps communicate only through `state` keys named in `stepMetadata` (e.g. `outputUserIdKey`, `inputUserFieldsKey`).
- **Why (intent):** Loose coupling between handlers; the wiring is in the process JSON.
- **Sources:** [LC:src/main/java/com/logicorum/model/ProcessContext.java].
- **Tags:** context, process

### CON-LC-012 — Change set + dynamic model (unit of work)
- **What:** Steps do not write to the database; they stage `EntityChangeSet`s that the persistence step commits together.
- **How:** `EntityChangeSet{entityType, entityId, branchName, txId, newModel, oldModel, commitId, commitType}`; `newModel == null` ⇒ delete; `oldModel == null` ⇒ new; `commitType` (CREATE/UPDATE/DELETE) is decided at persistence time by checking the instance table. `DynamicModel{id, modelType, dynamicFields: Map}` is a schemaless record. Clients may also send `changes: [{entityName, entityId ("new-…" or null for new), properties}]`.
- **Why (intent):** One process = one atomic business transaction across many entities.
- **Sources:** [LC:src/main/java/com/logicorum/model/EntityChangeSet.java], [LC:src/main/java/com/logicorum/model/DynamicModel.java], [LC:src/main/java/com/logicorum/core/LogicExecutionEngine.java] (`initializeChangeSets`).
- **Tags:** entity, process, versioning

### CON-LC-013 — Versioning by name for types, processes and handlers
- **What:** Contracts are versioned by suffix and versions coexist.
- **How:** Types `USER_V1`, `QUIZ_V1`, `RELATIONSHIP_V1`; processes `TAKER_SIGN_UP_V1` and `_V2`; handlers `userRoleProvisioningHandlerV1`/`V2`; URL segment `/v1/`. `docs/README.md`: "A process has a name and should also have a version number … this allows parallel business systems within one instance. A process consists of handlers; handlers must also have versions." Process log has `process_version` and `handler_versions` (JSONB).
- **Why (intent):** Evolve business contracts without breaking running clients.
- **Sources:** [LC:docs/README.md], [LC:data/database/init_db/002_process_commit_log.sql].
- **Tags:** versioning, api

### CON-LC-014 — Relationship as a versioned entity + relationship-type catalogue
- **What:** A relationship instance is an ordinary entity of type `RELATIONSHIP_V1`; its business type is described in a catalogue.
- **How:**
  - Instance fields: `type_name`/`type`, `source_entity_id`, `target_entity_id`, `start_date`, `end_date`, `metadata`, `slug` (schema `RELATIONSHIP_V1`, `type_name` has `x-enum-source: relationship-types`).
  - Catalogue entry (`*.rtp.json`): `typeName`, `description`, `isSymmetric`, `validityRequired`, `allowedSourceEntities[]`, `allowedTargetEntities[]`, `metadataSchema` (JSON Schema for the relationship's own attributes).
  - 22 types defined, e.g. `IS_MARRIED_TO` (symmetric, validity required), `REPORTS_TO`, `WORKS_FOR`, `DEPENDS_ON` (IT assets), `OWNS`, `CONTAINS`, `HAS_OPTION`, `HAS_VALIDITY_PERIOD` (metadata validFrom/validTo), `ATTEMPTS`, `FOR_QUIZ`, `ANSWER_TO`, `SELECTED_OPTION`, `HAS_WALLET`, `AFFECTS_WALLET`, `RELATED_TO_EVENT`, `FINANCED_BY`, `HAS_ROLE` (validFrom/validTo), `GRANTS_ACCESS_TO` (role → process).
  - `docs/relationship-types.md`: "single source of truth for relationship types"; `docs/Redis.md` section "Relationship as an Entity": relationships get identity, unlimited attributes, version history, temporal validity, approval and schema validation.
- **Why (intent):** Relationships in business (marriage, reporting line, ownership, role assignment) have their own lifecycle, attributes and validity periods.
- **Sources:** [LC:data/relationships/common.rtp.json], [LC:data/relationships/role.rtp.json], [LC:data/schemas/auth/RELATIONSHIP_V1.schema.json], [LC:docs/relationship-types.md].
- **Tags:** relationship, entity, metadata, versioning

### CON-LC-015 — Entity schemas: JSON Schema + platform extensions
- **What:** Each entity type is a JSON Schema (draft-07) whose `title` is the type name, extended with platform keys.
- **How:**

| Extension | Level | Meaning |
|---|---|---|
| `properties.<p>.metadata.unique` / `validator` | property | uniqueness constraint (`UniqueAttributeValidator`) |
| `properties.<p>.metadata.runOnLevel` | property | when the constraint applies |
| `x-enum-source` | property | enum values come from a catalogue (relationship types) |
| `metadata.mandatoryRelationships[]` | type | `{constraintName, relationshipType, targetEntityTypes[], minCount, maxCount, violationMessage, runOnLevel}` |
| `metadata.relationshipsToEnforce[]` | type | softer/optional relationship expectations |
| `relationships[]` with `type: HAS_PROPERTY` | type | property-level relationship definitions (loader support) |

  Example `QUIZ_V1`: must have exactly one `OWNS` → `USER_V1` at `DRAFT_SAVE`; at least one `CONTAINS` → `QUESTION_V1` at `FINAL_PUBLISH`.
- **Why (intent):** Structural validation (JSON Schema) plus graph-level business rules in the same document.
- **Sources:** [LC:data/schemas/], [LC:src/main/java/com/logicorum/core/SchemaDefinitionLoader.java].
- **Tags:** schema, rules, relationship

### CON-LC-016 — App scoping
- **What:** Every instance and log row carries `app_id`; identity uniqueness is `(app_id, entity_type, slug)`.
- **Sources:** [LC:data/database/init_db/001_entity_instance.sql].
- **Tags:** multi-tenancy, identity

### CON-LC-017 — Access model as relationships
- **What:** Roles are entities (`ROLE_V1`), users get roles through `HAS_ROLE` (with validity), roles grant processes through `GRANTS_ACCESS_TO` (`PROCESS_V1` entity type).
- **How:** Processes declare `requiredRoles`; `AuthorizationHandler` checks `state.user_roles_slugs` ∩ `requiredRoles`; scheduler-initiated processes run with `SYSTEM_INTERNAL_ROLE`.
- **Why (intent):** Authorization data lives in the same versioned graph as business data (Interpretation: processes themselves are meant to become entities).
- **Sources:** [LC:data/relationships/role.rtp.json], [LC:src/main/java/com/logicorum/handler/AuthorizationHandler.java], [LC:src/main/java/com/logicorum/scheduler/SessionCleanupScheduler.java].
- **Tags:** permission, relationship, org-model

### CON-LC-018 — Template vs publication (frozen instance) domain pattern
- **What:** Product design separates reusable content ("Quiz Master") from each run of it ("Publishing"), which freezes a snapshot of the content; all attempts, money flows and statistics reference the publication, not the template.
- **How:** `docs/Main.md` "core architecture change": Quiz Master = content only; Publishing = core business entity, an "instantiation" that snapshots/freezes the quiz; editing the template does not affect published runs.
- **Why (intent):** Stable, auditable business commitments (rewards, statistics) while content keeps evolving.
- **Sources:** [LC:docs/Main.md].
- **Tags:** entity, versioning, domain-pack
