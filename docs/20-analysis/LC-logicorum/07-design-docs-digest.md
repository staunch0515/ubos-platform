---
id: ANA-LC-07
title: "LC Design Documents Digest"
status: complete
phase: P1
depends_on: [INV-LC]
sources: [LC]
---

# LC — Design Documents Digest

## 1. `docs/README.md` (ZH, 21 lines) — Overview
- **Process versioning:** processes have names *and* version numbers, enabling "parallel business systems in one instance"; handlers must also be versioned.
- **Six-table core model:** `process_commit_log` (business event log, no entity_id) · `entity_instance` (existence) · `entity_version_chain` (one commit = one version of one entity) · `process_entity_map` (**key decoupling point**: links a process operation to all affected versions) · `entity_attribute_value` (commit_id is the only link, no redundant entity_id) · `entity_branch_head` (current pointer).

## 2. `docs/schema.md` (ZH, 10 lines) — Model merging
- "Model merging technique: merge the base content with the user-customized version." (Only a statement of intent; relates to base/custom overlays — compare UP branch inheritance.)

## 3. `docs/finance.md` (ZH, 10 lines) — Double-entry needs services
- Pure schema validation cannot move money; required services: **Top-up** (create `TRANSACTION_V1 TOP_UP COMPLETED`, atomically increase wallet) and **Quiz settlement** (on pass: get `FINANCED_BY` payer and reward; debit payer with `CREATOR_FEE` transaction after a funds check; credit taker with `QUIZ_REWARD`; update both wallets atomically in one transaction).

## 4. `docs/relationship-types.md` (ZH, 67 lines)
- Relationship-type file = **single source of truth**; field table: `typeName`, `description`, `isSymmetric` (A→B implies B→A; persistence must create reverse), `validityRequired` (start/end date), `allowedSourceEntities`, `allowedTargetEntities`, `metadataSchema` (JSON Schema of relationship metadata).

## 5. `docs/Redis.md` (ZH, 111 lines)
- Part 1: **Relationship as an Entity** — rationale table (identity via instance table, unlimited versioned attributes, change sets make relationship changes trackable/approvable/versioned, schema validation); core attributes `entity_type`, `type`, `source_entity_id`, `target_entity_id`, `start_date`, `end_date`, `metadata`; storage and query recipe over EAV.
- Part 2: **Redis for high concurrency** — five scenarios (idempotency, session validation, entity snapshot cache, distributed wallet lock, rate limiting) and rollout order (CON-LC-046).

## 6. `docs/email-template.md` (ZH, 17 lines)
- Unified placeholder syntax `{{…}}` for simple fields (`{{user_name}}`, `{{order_id}}`), links (`{{verify_link}}`, `{{unsubscribe_url}}`); lists need a template engine (Jinja2/Handlebars).

## 7. `docs/Main.md` (ZH, 347 lines) — QuizBucks product requirements
- **App requirements** (owner's words): quiz feed, detail, one question per page, results, rewards, bottom bar, registration (location, bank account, currency), messages, advanced filters, reference materials, completion statistics by time windows, RTK Query.
- **Admin v1:** dashboard, quiz management (list, create/edit: title, cover, description, publisher, reward, currency, passing score, max score, time limit, target region, reference materials, question builder), analytics, user management, finance, notifications, CMS, technical requirements.
- **Admin v2 — "core architecture change":** separate **Quiz Master (template)** from **Publishing (instance)**; publishing snapshots/freezes content; attempts, ledger and statistics reference the publication (CON-LC-018). AI cover and question generation with human review; template version numbers; publishing with region/period/reward; workflow: create material → define strategy → run & monitor → settle.

## 8. `docs/RTK.md` (ZH, 103 lines)
- Explanation of RTK Query (data fetching and caching built into Redux Toolkit) and how to structure API slices, tags and hooks.

## 9. `docs/Google.md` (190 lines) and `docs/Supabase/readme.md` (60 lines)
- Google sign-up flow notes; Supabase as hosted Postgres profile (`config/supabase/application.yaml`).

## 10. Frontend docs
- `apps/quiz_admin_app/src/API_INTEGRATION.md`: endpoint groups (auth, dashboard, quiz management, **AI generation**, publishing, analytics, transactions & finance, notifications), caching tiers (1 h / 30 s–5 min / 10 s–1 min / polling), tag invalidation, optimistic updates, backend response/error formats, mock mode.
- `apps/quiz_bucks_app/src/API_INTEGRATION.md`, `QUICKSTART.md`, `CHANGELOG.md`: app integration and change history.

## 11. Commit history (themes)
| Tickets | Themes |
|---|---|
| LCM-1000 (11-09/10) | user, quiz, slug, remove entity name from URL, README |
| LCM-1001 (11-11..15) | remove comments, logging, version management reset, delete, reload JSON, create/update/delete, deleted-entity error |
| LCM-1003 (11-15..18) | `val_json`, relationship types, loader update |
| LCM-1004/1005 (11-21/22) | apps, Vercel, authentication |
| dev/lcm-#7 (11-22..26) | proxy, register process, check relationship, verify, coding rules |

## 12. Implied principles (Interpretation)
> Interpretation: LC's principles are (1) *the API is a set of versioned processes*, (2) *a process is a pipeline of reusable, configurable steps*, (3) *all writes of a process are one unit of work*, (4) *relationships are first-class entities with types, cardinalities and validity*, (5) *validation is staged (draft vs publish)*, (6) *production concerns (idempotency, audit, outbox) belong to the platform, not the app*.
