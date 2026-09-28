---
id: ANA-LC-03
title: "LC Runtime"
status: complete
phase: P1
depends_on: [ANA-LC-02]
sources: [LC]
---

# LC — Runtime Mechanisms

### CON-LC-020 — Command execution algorithm
- **What:** The fixed sequence every command process goes through.
- **How:**
  1. Create context (`appId`, `targetBranchName` default `draft`), copy request into `state`.
  2. Load definition (hot-reload in dev); error if missing/empty.
  3. Apply `default` values from `inputSchema.properties`.
  4. Validate request against `inputSchema` → `ValidationException` ⇒ status `INTERRUPTED`.
  5. `authorizationHandler` (process `requiredRoles`).
  6. Initialize change sets from `changes[]`.
  7. Execute steps (CON-LC-021).
  8. Unless INTERRUPTED/FAILED ⇒ `SUCCESS`. Other exceptions ⇒ `FAILED` and error propagated.
- **Sources:** [LC:src/main/java/com/logicorum/core/LogicExecutionEngine.java].
- **Tags:** process, workflow

### CON-LC-021 — Flow control by named jumps and decision steps
- **What:** Steps run sequentially unless a step writes `nextStepName` into the state; then execution jumps to that step.
- **How:** Engine reads and removes `state.nextStepName` after each step and continues at the step with that `stepName` (unknown name ⇒ error). `DecisionHandlerV1` evaluates a Spring Expression Language `condition` over the context (`#state` variable) and sets `trueStep` / `falseStep`. Status `INTERRUPTED` stops the flow.
- **Why (intent):** Branching and loops in declarative processes without a full BPMN engine.
- **Sources:** [LC:src/main/java/com/logicorum/handler/DecisionHandlerV1.java], [LC:src/main/java/com/logicorum/core/LogicExecutionEngine.java] (`executeStepRecursive`).
- **Tags:** workflow, rules, dsl

### CON-LC-022 — Atomic persistence of all change sets (the commit step)
- **What:** `persistenceSaveHandlerV1` commits every staged change set of the process in one DB transaction and links them to one process log row.
- **How:**
  1. Insert `lcm_process_commit_log` (app, process name, operator) → `process_log_id`.
  2. For each change set, in order (`concatMap`): ensure instance exists — absent ⇒ insert with slug from fields, `CREATE`; present and `newModel == null` ⇒ `DELETE` (error if already deleted), mark `is_deleted`; else `UPDATE`.
  3. Read head on branch → parent; insert version (`commit_type`, parent).
  4. Insert every field as a typed EAV row for this commit (CON-LC-040).
  5. Upsert head; insert process ↔ commit map.
  6. Write a JSON transaction log file (CON-LC-044).
  Validation errors ⇒ `INTERRUPTED`; other errors ⇒ "Persistence failed for aggregate".
- **Why (intent):** The persistence step is just another handler; processes decide when (and whether) to commit.
- **Sources:** [LC:src/main/java/com/logicorum/handler/PersistenceSaveHandlerV1.java].
- **Tags:** persistence, versioning, process

### CON-LC-023 — Staged validation levels (DRAFT_SAVE → FINAL_PUBLISH)
- **What:** Constraints declare the level at which they apply; the request chooses the level.
- **How:** `ValidationLevel {DRAFT_SAVE, FINAL_PUBLISH}`; request field `runOnLevel` (schema-validation handler defaults to `FINAL_PUBLISH`); a constraint with `runOnLevel: DRAFT_SAVE` runs at both levels, one with `FINAL_PUBLISH` (or none) only at publish. Processes `QUIZ_DRAFT_V1` ("Standard Validation (DRAFT_SAVE)") and `QUIZ_PUBLISH_V1` ("Load & Merge" → "Strict Validation (FINAL_PUBLISH)").
- **Why (intent):** Incomplete business objects can be saved and iterated; completeness is enforced at publish time.
- **Sources:** [LC:src/main/java/com/logicorum/model/ValidationLevel.java], [LC:src/main/java/com/logicorum/validation/BusinessConstraintManager.java], [LC:data/processes/sponsor/quiz_publish_V1.process.json].
- **Tags:** rules, schema, workflow

### CON-LC-024 — Two-layer validation with pluggable business constraints
- **What:** Each change set is validated by its JSON Schema, then by business-constraint validators selected from schema metadata.
- **How:**
  - `interface BusinessConstraintValidator { getConstraintName(); supports(metadata); validate(ctx, changeSet, property, metadata, allChanges); }` — all beans auto-collected.
  - `isUnique` (`UniqueAttributeValidator`): EAV query for same type/attribute/value, excluding self and deleted.
  - `mandatoryRelationships` (`MandatoryRelationshipValidator`): count pending `relationship_v1` change sets in this process + active relationships in DB (end_date null or in future) ≥ `minCount`.
  - `RelationshipEntityCheck` (`RelationshipConstraintValidator`): for `RELATIONSHIP_V1`, source/target types allowed by the catalogue; `validityRequired` ⇒ `start_date` required; symmetric flag recognized.
  - Validators see **all pending changes** of the process, so a quiz and its owner relationship can be created in the same request.
- **Why (intent):** Graph-aware rules evaluated against the unit of work plus the persisted state.
- **Sources:** [LC:src/main/java/com/logicorum/validation/], [LC:src/main/java/com/logicorum/handler/SchemaValidationHandlerV1.java].
- **Tags:** rules, relationship, schema

### CON-LC-025 — Generic cross-cutting handlers
- **What:** Reusable steps for recurring concerns.
- **How:**

| Handler | Metadata keys | Function |
|---|---|---|
| `entityLoadAndMergeHandlerV1` | `entityType`, `entityIdContextKey`, `inputDataKey` | load current entity, overlay partial input, stage change set (partial update) |
| `optimisticLockValidationHandler` | – | require `version` in new model equal to stored `version` |
| `genericCascadeDeleteHandler` | `rules: [{targetEntity, targetField}]` | stage deletion of entities whose `targetField` references the deleted source |
| `resourceLookupHandler` | lookup config | load entity into state |
| `slugValidationHandler`, `usernameUniquenessHandler`, `entityExistenceValidationHandler` | – | identity checks |
| `setCreatorIdHandler`, `creatorOwnershipValidationHandler` | – | ownership stamping / checking |
| `timeLimitValidationHandler`, `sessionValidationHandler`, `sessionActivityUpdateHandler` | – | session/time rules |
| `responseFinalizerHandlerV1` | – | write request log and shape response |

- **Sources:** [LC:src/main/java/com/logicorum/handler/].
- **Tags:** process, plugin, rules

### CON-LC-026 — Idempotency keys with response replay
- **What:** Repeated POSTs with the same `Idempotency-Key` are safe.
- **How:** Table `lcm_process_idempotency(key, process_name, status, response_data, created_at, updated_at)`. Absent ⇒ insert `PROCESSING`, run. `COMPLETED` ⇒ return stored response. `PROCESSING` ⇒ 409, unless older than 5 minutes ⇒ reset and run. `FAILED` ⇒ reset and run. Completion stores the response body.
- **Why (intent):** `docs/Redis.md` names idempotency the "must-have" for high concurrency (proposes Redis `SET NX EX`).
- **Sources:** [LC:src/main/java/com/logicorum/service/IdempotencyService.java], [LC:data/database/init_db/007_process_idempotency.sql].
- **Tags:** api, persistence

### CON-LC-027 — Scheduled work invokes processes
- **What:** Background jobs are expressed as processes run by schedulers under a system role.
- **How:** `SessionCleanupScheduler` runs `CLEANUP_EXPIRED_SESSIONS_V1` with `user_roles_slugs=[SYSTEM_INTERNAL_ROLE]`; `EmailJobScheduler` (every 15 s) drains `lcm_email_dispatch_queue` (outbox: recipient, template name/version, payload, scheduled_at, status 0/1/-1, error).
- **Why (intent):** One execution model for user-triggered and time-triggered work; asynchronous side effects via an outbox.
- **Sources:** [LC:src/main/java/com/logicorum/scheduler/], [LC:data/database/init_db/011_email_dispatch_queue.sql].
- **Tags:** event, process, integration

### CON-LC-028 — Composite business handlers (QuizBucks domain)
- **What:** Domain steps that stage several related change sets at once.
- **How:**
  - `userRoleProvisioningHandlerV1`: find-or-create user (password hashed) + `WALLET_V1` (balance 0, currency) + `HAS_WALLET` relationship entity + `HAS_ROLE` to default role; output user id key configurable.
  - `answerSubmissionHandler` → `rewardAndNotificationHandler`: if the answer is correct, move `reward_per_correct_answer` from issuer wallet to taker wallet (two wallet change sets); insufficient balance ⇒ email the issuer instead.
  - `docs/finance.md`: double-entry needs service logic beyond schema validation — top-up creates `TRANSACTION_V1` + atomic wallet update; quiz settlement creates `CREATOR_FEE` (debit payer, with funds check) and `QUIZ_REWARD` (credit taker) transactions in one transaction.
- **Sources:** [LC:src/main/java/com/logicorum/handler/UserRoleProvisioningHandlerV1.java], [LC:src/main/java/com/logicorum/handler/RewardAndNotificationHandler.java], [LC:docs/finance.md].
- **Tags:** finance, domain-pack, process

### CON-LC-029 — Remote steps (designed)
- **What:** Step definitions carry `isRemote` and `remoteServiceUrl`, anticipating steps executed by external services; the engine does not act on them yet.
- **Sources:** [LC:src/main/java/com/logicorum/core/Step.java].
- **Tags:** integration, process
