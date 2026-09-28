---
id: ANA-UC-07
title: "UC Design Docs Digest — Index"
status: complete
phase: P1
depends_on: [INV-UC]
sources: [UC]
---

# UC — Design Documents Digest (index)

Canonical digest of the corpus shared by UC and US. US analysis (ANA-US-07) references these
D-UC ids and digests only its own additional or differing documents.

| Part | File | Covers |
|---|---|---|
| 1 | `01-master-build-log.md` | `docs/master/01…35, 51` — the build log (D-UC-01…28) |
| 2 | `02-plan-roadmaps.md` | `docs/plan/Plan_01…17, plan_18, plan_19` (D-UC-30…48) |
| 3 | `03-front-data-vision.md` | `docs/front/front_01…04`, `init_data.md`, `book.md`, `prompt.md`, `db/*` (D-UC-50…58) |

## Document map

| D-id | Document | Topic | Concepts |
|---|---|---|---|
| D-UC-01 | master/01–05 | In-memory prototype: Entity, SysFunction/Logrum, kernel wiring, executor, salary demo | CON-UC-010, 011, 013, 014, 020, 026 |
| D-UC-02 | master/06 | Postgres via sqlx, ids `tenant:type:slug` | CON-UC-010, 041 |
| D-UC-03 | master/07 | Rhai chosen over Deno/Wasmtime | CON-UC-001 |
| D-UC-04 | master/08 | Axum web: deploy / invoke / get | CON-UC-050 |
| D-UC-05 | master/09 | Redis queue: server/worker split | CON-UC-003, 043 |
| D-UC-06 | master/10 | Split into model/store/kernel/api | CON-UC-001 |
| D-UC-07 | master/11 | DB syscalls, async bridge | CON-UC-004, 005 |
| D-UC-08 | master/12 | HTTP syscalls | CON-UC-004 |
| D-UC-09 | master/13 | Tenant/user context, isolation by prefix | CON-UC-005, 019 |
| D-UC-10 | master/14 | `sys_invoke_async`, parent links | CON-UC-026 |
| D-UC-11 | master/15 | `sys_db_query` JSONB containment | CON-UC-046 |
| D-UC-12 | master/16 | Cron scheduler | CON-UC-027 |
| D-UC-13 | master/17 | Hooks + event bus | CON-UC-027 |
| D-UC-14 | master/18 | Execution log capture | CON-UC-030 |
| D-UC-15 | master/19 | JSON Schema validation + coercion | CON-UC-014 |
| D-UC-16 | master/20 | DB module resolver (`import`) | CON-UC-029 |
| D-UC-17 | master/21 | `sys_ai_chat` | CON-UC-060 |
| D-UC-18 | master/22 | Virtual file system | CON-UC-044 |
| D-UC-19 | master/23 | Policy as code | CON-UC-028 |
| D-UC-20 | master/24, 25 | Function definition API (25 empty) | CON-UC-054 |
| D-UC-21 | master/26 | Direct entity save + ephemeral eval | CON-UC-025 |
| D-UC-22 | master/27 | Atomic multi-entity commit + process tables | CON-UC-020, 042 |
| D-UC-23 | master/28 | Logrum context manager in Redis | CON-UC-015, 043 |
| D-UC-24 | master/29 | Holographic ProcessContext + archive | CON-UC-022 |
| D-UC-25 | master/30 | Robustness: one transaction, lock, truncation | CON-UC-020, 035 |
| D-UC-26 | master/31–34 | Ambassador mode: memory, syscalls, handshake, worker | CON-UC-023 |
| D-UC-27 | master/35 | Separation of environment and execution | CON-UC-024 |
| D-UC-28 | master/51 | Job status API | CON-UC-059 |
| D-UC-30 | plan/Plan_01 | Five-stage foundation plan + distributed critique | CON-UC-002, 003 |
| D-UC-31 | plan/Plan_02 | Core 2.0: stdlib, context, orchestration | CON-UC-004 |
| D-UC-32 | plan/Plan_03 | Core 3.0: query, cron, events | CON-UC-027, 046 |
| D-UC-33 | plan/Plan_04 | Core 3.0b: logs, schema, import | CON-UC-014, 029, 030 |
| D-UC-34 | plan/Plan_05 | Core 4.0: AI, VFS, policy | CON-UC-028, 044, 060 |
| D-UC-35 | plan/Plan_06 | 5.0 productization: IDE, stdlib, demo | CON-UC-063 |
| D-UC-36 | plan/Plan_07 | SDUI + command pattern shell, three phases | CON-UC-052, 054 |
| D-UC-37 | plan/Plan_08 | Change set, Logrum cache, big ProcessContext | CON-UC-020, 022 |
| D-UC-38 | plan/Plan_09 | Four-defect robustness audit | CON-UC-035 |
| D-UC-39 | plan/Plan_10 | Ambassador mode + agent-OS positioning | CON-UC-023, 062 |
| D-UC-40 | plan/Plan_11 | Phase 5.5 environment/execution | CON-UC-024 |
| D-UC-41 | plan/Plan_12 | Bicameral UI, MVS genesis, search API | CON-UC-031, 033, 052 |
| D-UC-42 | plan/Plan_13 | `logrums` root, constitution | CON-UC-019, 034 |
| D-UC-43 | plan/Plan_14 | Handoff document + roadmap | CON-UC-006, 035, 065 |
| D-UC-44 | plan/Plan_15 | Version anchoring | CON-UC-021 |
| D-UC-45 | plan/Plan_16 | Dynamic EntityType + provenance API | CON-UC-011, 021, 042 |
| D-UC-46 | plan/Plan_17 | Async UX, branch display | CON-UC-045, 059 |
| D-UC-47 | plan/plan_18_ubos | `ubos://` protocol | CON-UC-016, 066 |
| D-UC-48 | plan/plan_19 | Universal Resource Browser | CON-UC-057 |
| D-UC-50 | front/front_01 | Linear-style Web IDE prompt | CON-UC-054 |
| D-UC-51 | front/front_02 | Cockpit: identity → Logrum → command | CON-UC-053 |
| D-UC-52 | front/front_03 | Copilot orchestration prompt | CON-UC-058 |
| D-UC-53 | front/front_04 | Lens system | CON-UC-055 |
| D-UC-54 | init_data.md | UODS v4.0 + 15 genesis files | CON-UC-017, 018 |
| D-UC-55 | book.md | Executable Book levels | CON-UC-061 |
| D-UC-56 | prompt.md | House rules | CON-UC-040, 065, 067 |
| D-UC-57 | db/genesis_data | Genesis data (as loaded) | CON-UC-018, 033 |
| D-UC-58 | db/V1_init.sql, clear.sql | Schema v2.1 | CON-UC-040 |
