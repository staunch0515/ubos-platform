---
id: SPEC-01
title: "Glossary — Index"
status: complete
phase: P3
depends_on: [SPEC-00]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Glossary — Index

## 0. How to use this glossary

- This glossary (this index plus three part files) fixes **one vocabulary** for the specification and for every implementation (VRD-22-04).
- Each term has a stable ID, `TERM-<Name>`. Chapters link to terms by that ID.
- **Aliases** list the words the eight source repositories use for the same idea. An alias is
  never used in the spec or in implementation code, except to quote a source.
- **Not** lists nearby ideas the term must not be confused with.
- **Chapter** names the spec chapter that defines the term normatively. The glossary gives a
  short definition only; when the two differ, the chapter wins.
- **Origin** lists the observations (CON/HL) and verdicts (VRD) the term comes from.
- Implementations MUST NOT introduce synonyms. A new term is added here first.

Entry format:

```
### TERM-<Name>
- **Definition:** ...
- **Aliases:** <corpus word> (<KEY>), ...
- **Not:** ...
- **Chapter:** SPEC-NN
- **Origin:** CON-..., VRD-...
```

Term groups and the files that hold them:
1. Entities, identity and addressing (§1, `01-model-and-change.md`)
2. Storage and versioning (§2, `01-model-and-change.md`)
3. Meta-model (§3, `01-model-and-change.md`)
4. Change and transactions (§4, `01-model-and-change.md`)
5. Context and execution (§5, `01-model-and-change.md`)
6. Logic runtime (§6, `02-runtime.md`)
7. Rules and validation (§7, `02-runtime.md`)
8. Orchestration (§8, `02-runtime.md`)
9. Events and asynchronous work (§9, `02-runtime.md`)
10. Query and search (§10, `02-runtime.md`)
11. Security and tenancy (§11, `03-platform.md`)
12. Protocol (§12, `03-platform.md`)
13. UI and client shells (§13, `03-platform.md`)
14. AI (§14, `03-platform.md`)
15. Packages, boot and deployment (§15, `03-platform.md`)
16. Development method (§16, `03-platform.md`)

The alias quick map (§17, below) lists every corpus word in one table.

---

## 17. Alias quick map

Corpus word → spec term. Words with several meanings map to several terms, chosen by the source.

| Corpus word | Source | Spec term |
|---|---|---|
| Logrum (execution record) | UC early | TERM-Process / TERM-Job |
| Logrum (environment, session) | UC | TERM-Context |
| logrum-system (product) | LS | the platform (no term) |
| Workspace (environment) | UC, US | TERM-Context |
| Workspace (protocol server) | LS | TERM-Host |
| Workspace column | UW | TERM-Cockpit (active zone) |
| SysFunction, sys_function, script | UC, UP, UB | TERM-Logic |
| Job (execution record, "cast") | UC | TERM-Process (record) / TERM-Job (async request) |
| SysCron / SysHook / SysPolicy | UC early | TERM-Cron / TERM-Hook / TERM-Policy |
| sys tenant | UC early | TERM-RootTenant (`logrums`) |
| process (definition) | LC | TERM-Pipeline |
| process blueprint | LS | TERM-Pipeline |
| process instance | LS | TERM-ProcessInstance |
| process (business change) | UP | TERM-Process |
| ProcessContext, blackboard | UB, UC, LC, US | TERM-ExecutionContext |
| universe | UB | TERM-Branch |
| seq_num, block height | UB | TERM-Height |
| stash | FU | TERM-Draft |
| environment | FU | TERM-Context (with TERM-Pin) |
| scope | FU | TERM-Authority / TERM-Tenant |
| app scope | LC | TERM-Namespace |
| UODS, metadata specification | UC, US | TERM-MetaSchema |
| BehaviorType, behaviour | LS, US | TERM-Slot / TERM-Behavior |
| RelationType (prop) | LS, US | TERM-Property (with its slots) |
| relationship type | LC | TERM-RelationshipType |
| composite entity, effective entity | US, UB | TERM-EffectiveData |
| self data | UB | TERM-OwnData |
| BEL, EOP | UB, US | TERM-Invariant |
| slice voting | US | TERM-Decision |
| lens, ui_modes | UC, US | TERM-Mode |
| SDUI | UP, UB | TERM-RenderPayload |
| bicameral UI, Web OS shell | UC, UP | TERM-Cockpit |
| command bar | UW | TERM-Omnibar |
| Studio, Meta-IDE | FU, UB | TERM-Studio |
| AI Architect | UP | TERM-AiBuilder |
| Ambassador, Embassy | UC | TERM-HostTenant / TERM-Memory |
| handler bean, native:: | LC, LS | TERM-NativeHandler |
| kernel API, script context | UP, UB | TERM-Syscall |
| immune system | UP | TERM-Sandbox |
| bundle, app | UB | TERM-Package |
| typed intent, command grammar | UW | TERM-Intent |
