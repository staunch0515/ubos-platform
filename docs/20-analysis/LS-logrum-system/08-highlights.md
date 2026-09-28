---
id: ANA-LS-08
title: "LS Highlights"
status: complete
phase: P1
depends_on: [ANA-LS-01, ANA-LS-02, ANA-LS-03, ANA-LS-04, ANA-LS-05, ANA-LS-06, ANA-LS-07]
sources: [LS]
---

# LS — Highlights

### HL-LS-001 — Entity = UUID; every attribute is a typed relation keyed by URI
- **Summary:** Payload keys are relation-type URIs; identity is itself a relation; types are sets of relation types.
- **Why it stands out:** The most radical and uniform meta-model in the corpus: attribute semantics are globally addressable, versioned entities.
- **Concepts:** CON-LS-010, CON-LS-011, CON-LS-015, CON-LS-016
- **Reuse recommendation:** adapt (reconcile with UB/UC `_extends` property model and LC relationship-as-entity in P2 topic 2; consider keeping short keys with a relation-type registry)
- **Sources:** [LS:claude.md], [LS:genesis_data/]
- **Tags:** meta-model, identity

### HL-LS-002 — Behaviour slots: BehaviorType / Behavior / RelationType configurator
- **Summary:** 20 slot types with I/O contracts; behaviours implement slots (with refine chains); relation types configure which behaviour fills each slot.
- **Why it stands out:** A precise, data-driven plug-in architecture for property semantics (storage, UI, validation, masking, hooks, computation, permission).
- **Concepts:** CON-LS-012, CON-LS-013, CON-LS-014, CON-LS-053, CON-LS-060
- **Reuse recommendation:** adopt (as the behaviour/extension model; merge with US composition model CON-US-010)
- **Sources:** [LS:genesis_data/03_base_behavior.json]
- **Tags:** plugin, meta-model, rules

### HL-LS-003 — "Scripts only emit, the engine runs" with a three-stage action protocol
- **Summary:** Generic Init/Interact/Exec processes emit slot signals (schema, permission, compute, mask, default, transform, validate, hooks) and aggregate results into UI or Mutate instructions.
- **Why it stands out:** One universal interaction pipeline serves every entity type; business variation is pure configuration.
- **Concepts:** CON-LS-021, CON-LS-022, CON-LS-054
- **Reuse recommendation:** adopt (combine with US action FSM CON-US-022)
- **Sources:** [LS:claude.md], [LS:genesis_data/07_action.json]
- **Tags:** workflow, ui-rendering, validation

### HL-LS-004 — UBTP implemented: TCP frames, five operations, server push
- **Summary:** Length-prefixed JSON frames; Handshake/Mutate/Emit/Query/Subscribe; Ack + streamed ops; client addressable as `client://current`.
- **Why it stands out:** The only repo implementing the protocol target shared by UC/US documents.
- **Concepts:** CON-LS-001, CON-LS-002, CON-LS-003, CON-LS-005, CON-LS-029, CON-LS-051
- **Reuse recommendation:** adapt (add binary encoding, request ids, auth; P2 topic 15)
- **Sources:** [LS:apps/ubos_server/src/codec.rs], [LS:crates/ubos_proto/src/op.rs]
- **Tags:** protocol, api

### HL-LS-005 — Storage editions behind traits with a single atomic write path
- **Summary:** SQLite (personal) / PostgreSQL (enterprise) backends; `commit_changes` writes process log, instance, version, head, search index and lineage in one transaction.
- **Concepts:** CON-LS-006, CON-LS-019, CON-LS-040, CON-LS-041, CON-LS-042
- **Reuse recommendation:** adopt
- **Sources:** [LS:crates/ubos_store/src/engine/writer.rs]
- **Tags:** persistence, deployment

### HL-LS-006 — Processes as versioned entities; side effects as returned ops
- **Summary:** Blueprint vs instance entities; each step commits state; scripts return emits instead of calling the kernel.
- **Concepts:** CON-LS-017, CON-LS-020, CON-LS-025
- **Reuse recommendation:** adopt
- **Sources:** [LS:crates/ubos_kernel/src/engine/orchestrator/process.rs]
- **Tags:** workflow, versioning

### HL-LS-007 — Commit-keyed semantic caches
- **Summary:** URI→commit, commit→raw, commit→assembled node; immutable versions make assembled nodes permanently cacheable.
- **Concepts:** CON-LS-007, CON-LS-023, CON-LS-044
- **Reuse recommendation:** adopt
- **Sources:** [LS:crates/ubos_kernel/src/semantic/mod.rs]
- **Tags:** cache, meta-model

### HL-LS-008 — Eight kernel mechanisms as one pipeline with dry run
- **Summary:** Resolution → Extends → Policy → Prerequisite → Schema → Transformation → Implement → Reactor, uniform interface, reject/break semantics, dry-run for UI.
- **Concepts:** CON-LS-008, CON-LS-026, CON-LS-027, CON-LS-028
- **Reuse recommendation:** adapt (design; align with US lifecycle phases CON-US-007 and LC pipelines)
- **Sources:** [LS:crates/ubos_kernel/src/mechanism/]
- **Tags:** process, rules

### HL-LS-009 — Version modes in the URI
- **Summary:** `?commit`, `?tag`, `?branch`, `?time` on `ubos://` addresses.
- **Concepts:** CON-LS-016
- **Reuse recommendation:** adopt (fuse with UB/UC URI grammar, P2 topic 3)
- **Sources:** [LS:crates/ubos_kernel/src/semantic/uri.rs]
- **Tags:** identity, versioning

### HL-LS-010 — Genesis through the commit path, one process per file
- **Concepts:** CON-LS-024
- **Reuse recommendation:** adopt
- **Sources:** [LS:crates/ubos_kernel/src/engine/bootloader.rs]
- **Tags:** boot

### HL-LS-011 — Repository-level AI instruction file
- **Concepts:** CON-LS-063
- **Reuse recommendation:** inspiration-only
- **Sources:** [LS:claude.md]
- **Tags:** ai, codegen
