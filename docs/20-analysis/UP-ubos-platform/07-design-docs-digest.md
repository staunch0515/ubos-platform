---
id: ANA-UP-07
title: "UP Design Documents Digest"
status: complete
phase: P1
depends_on: [INV-UP]
sources: [UP]
---

# UP — Design Documents Digest

`UP` contains **no design documents** (the two `README.md` files are Vite boilerplate).
Design intent is carried by (a) commit messages, (b) SQL comments, (c) log strings and code
comments, (d) sample JSON. This file digests those sources.

## 1. Commit messages (chronological, translated)

| Commit | Message (translated) | Design content |
|---|---|---|
| `1888559` | Initial commit: UBOS Genesis v0.1 – The Digital Lifeform Engine | Vision: self-evolving "lifeform" |
| `8f7657c` | feat(engine): static blacklist and runtime timeout for immune system | Sandbox (CON-UP-022) |
| `10b4c9b` | kill the loop thread | Interrupt runaway scripts |
| `6c4443f` | Inject `LcmKernelService` into Groovy scripts | Kernel bindings (CON-UP-021) |
| `a264c33` | Time Travel: Kernel find-by-CommitID; Engine commit-first strategy; Server API parameter | CON-UP-024 |
| `6ca7375` | add search function | CON-UP-042 |
| `38cb9a7` | DB: define the "family tree" (branch hierarchy); Code: recursive lookup in the kernel | CON-UP-041 |
| `c124e5b` | Add LLM | CON-UP-061 |
| `3e3f277` | Deploy with Docker | CON-UP-004 |
| `a9365a9` | add Event Bus | CON-UP-025 |
| `ac86532` | Fixed all bugs …; dual mode switch Dev/Run in the sidebar; universal renderer with a mock view parser demonstrating dynamic forms | CON-UP-055/056 |
| `17a4d0d` | API versioning: `kernel.v1.commit` and `kernel.v2.commit` coexist; overloading keeps a 6-param `commit` for old scripts | CON-UP-029 |
| `022940d` | Identity governance mechanism | CON-UP-052 |
| `e9516da` | add relationship | CON-UP-015 |
| `dac72ec` | view branch | CON-UP-051 |
| `4642460` | add ubos-web | CON-UP-005 |
| `5313149` | Multi-role | CON-UP-052 |

## 2. SQL comments (translated)

| Table | Comment |
|---|---|
| `lcm_entity_instance` | "Entity registry (does not care about versions, only about existence)"; `entity_type` examples 'LOGIC', 'VIEW', 'DATA'; `slug` "business unique identifier, e.g. 'logic.calc.tax'" |
| `lcm_entity_version_chain` | "Version chain (core storage, Git Commit Node)"; `commit_id` "auto-increment id as commit number"; `parent_commit_id` "points to the parent node"; `snapshot_data` "full data in JSON" |
| `lcm_entity_branch_head` | "Branch pointer table (like Git Refs)" |
| `lcm_entity_search_index` | "Attribute index table (gives the system search capability)" |
| `sys_branch_config` | "Branch configuration (defines inheritance between branches)"; "if empty it is the root branch"; seed "beijing inherits from master" |
| `lcm_process_commit_log` | "Process log (records who, in which scenario, initiated this change)"; example `PROC_20251129_001` / "Employee onboarding process" |
| `lcm_process_entity_map` | "Process–entity association (link one business operation to multiple commits)" |
| `lcm_entity_relation` | "Entity relation table (stores all connections between entities)"; examples OWNS, BELONGS_TO, APPROVES |

## 3. Configuration comments

`application.yml` → `ubos.genesis.strategy`: "SAFE – production default, check and skip if
initialized; RESET – development, wipe all data at startup and re-create the world;
UPDATE – advanced, only add missing data, never delete old data."

## 4. Samples

| File | Content |
|---|---|
| `commit_onboard.json` | LOGIC `logic.hr.onboard`: process-wrapped two-entity onboarding (CON-UP-028) |
| `commit_search_logic.json` | empty file |

## 5. Implied principles (Interpretation)

> Interpretation: taken together, UP's implicit principles are
> 1) *everything is a versioned entity*; 2) *business change = process of commits*;
> 3) *branches are overlays with inheritance*; 4) *logic, UI, schedules, listeners and
> policies are data*; 5) *the kernel API is the only contract, shared by scripts and AI*;
> 6) *the platform protects itself from its own content (immune system)*.
