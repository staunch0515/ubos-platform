---
id: SPEC-13
title: "Meta-Model"
status: complete
phase: P4
depends_on: [SPEC-02, SPEC-11, SPEC-12]
sources: [UP, LC, UB, UC, US, LS]
---

# Meta-Model

## 0. Chapter header

- **Scope:** This chapter defines how the platform describes itself:
  - the root type and the meta type;
  - value types (primitives and semantic types) and entity types;
  - type definitions and property definitions, and the meta-schema;
  - the closed slot catalogue and behaviours;
  - reference properties, Relationship and RelationshipType;
  - type extensions;
  - the inheritance resolution algorithm, effective vs. own data;
  - commit-keyed semantic caches.

  Validation *timing* and rule *combination* are in SPEC-18. Actions and state machines are in SPEC-19. Widgets and views are in SPEC-25. The domain ontology is in SPEC-29.
- **MOD:** `META`
- **depends_on:** SPEC-02, SPEC-11, SPEC-12.
- **Terms used:** TERM-Entity, TERM-Type, TERM-TypeCode, TERM-MetaType, TERM-RootType, TERM-Extends, TERM-Property, TERM-SemanticType, TERM-Primitive, TERM-MetaSchema, TERM-Slot, TERM-Behavior, TERM-ReferenceProperty, TERM-Relationship, TERM-RelationshipType, TERM-InheritanceResolution, TERM-OwnData, TERM-EffectiveData, TERM-Template.
- **Origin summary:**
  - Verdicts: VRD-02-01…07.
  - UB: layered semantic types (CON-UB-013), effective vs. self data (CON-UB-015), compose → resolve → execute (CON-UB-027), raw-fragment caching (CON-UB-045), types driving widgets and masking (CON-UB-057).
  - UC: open types and explicit `_extends` (CON-UC-011/012), UODS (CON-UC-017).
  - US: composition model and metadata spec (CON-US-010, 014), `ui_modes` (CON-US-015), CompositeEntity merge (CON-US-012).
  - LS: meta bootstrap, slot catalogue and behaviours (CON-LS-011…015), assembly (CON-LS-023), commit-keyed caches (CON-LS-007).
  - LC: relationship entities and catalogue (CON-LC-014), JSON Schema extensions (CON-LC-015).
  - UP: types as entities with validator references (CON-UP-016).

## Parts of this chapter

This chapter is split into files (WRITING-RULES R2.2). Read them in order.

| File | Sections |
|---|---|
| `00-index.md` (this file) | §0, §1, §6, §7, §8 |
| `01-model.md` | Model — §2 |
| `02-behavior.md` | Behavior — §3 |
| `03-interfaces-nfr.md` | Interfaces and non-functional — §4, §5 |

## 1. Concepts

### 1.1 Everything is described by types that are entities

- Every entity has exactly one **type**: `entity_instance.entity_type`, a type code.
- A type is itself an entity, of type `Type` (the meta type). `Type` is of type `Type`, which closes the model.
- Every type extends, directly or indirectly, the single root type `Entity`.

| Layer | Examples | Kind | Chapter |
|---|---|---|---|
| L0 root | `Entity` | ENTITY (abstract) | here |
| L1 meta | `Type` | ENTITY | here |
| L2 primitives | `Text`, `Number`, `Integer`, `Boolean`, `Date`, `Timestamp`, `Map`, `List`, `Reference`, `Json` | VALUE (abstract) | here |
| L3 abstract business bases | `AbstractText`, `AbstractMath`, `AbstractEnum`, `AbstractFile` | VALUE (abstract) | SPEC-29 |
| L4 semantic types | `Email`, `Phone`, `Money`, `Percentage`, `Iban`, `Zipcode`, `CreditCard`, `Semver`, `FileRef` | VALUE | SPEC-29 |
| Platform entity types | `Behavior`, `Relationship`, `RelationshipType`, `Dictionary`, `TypeExtension` (here); `Context`, `Logic`, `Action`, `Pipeline`, `Job`, `Policy`, `View`, `Widget`, `Package` … (their chapters) | ENTITY | various |
| Ontology entity types | `Party`, `Person`, `Organization`, `Currency`, `Document` | ENTITY | SPEC-29 |
| Tenant/app types | `acme.Invoice`, `acme.crm.Lead`; value types `acme.CompanyEmail` | ENTITY / VALUE | packages |

- **Value types** describe property values. They cannot have instances as entities.
- **Entity types** describe entities. They can be instantiated unless they are abstract.
- Business properties MUST reference value types, and SHOULD reference semantic types rather than raw primitives (VRD-02-01).

### 1.2 Two inheritance chains

1. **Type chain.** The linearised `_extends` ancestry of a type. An entity inherits its **definition** (properties, slots, UI modes, rules) and **property defaults** from its type chain.
2. **Prototype chain.** An *instance* MAY also set `_extends` to other instances of the same type or of a supertype (templates, e.g. Book/Page, CON-UB-019). An instance inherits **data values** from its prototype chain.

Effective data = type-chain defaults ⊕ prototype-chain effective data ⊕ own data (REQ-META-020).

### 1.3 Properties get behaviour from slots

A property has a value type. The value type supplies default **slot** bindings:
- `validate` from `regex`, `min`, `max`, `scale`, `dict`;
- `mask` from `masking_rule`;
- `ui` from `ui_modes`;
- `index` from `searchable`.

A property definition MAY override single slots. Slots are filled by **behaviours**: Logic entities, native handlers, or `Behavior` entities that wrap either one with configuration. The slot catalogue is closed (§2.6) (VRD-02-04, CON-LS-013).

### 1.4 Two relationship forms

- A **reference property** is a property whose value type is `Reference` (or a subtype), with a `target` entity type. It stores a canonical URI.
- A **Relationship entity** is used when a link has its own attributes, lifecycle or validity (marriage, role assignment, reporting line). It is governed by a **RelationshipType** catalogue entry (VRD-02-03, CON-LC-014).

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-META-001…005 | Bootstrap closure; open types; meta-schema negative tests per rule; exported schema validates genesis |
| REQ-META-010…017 | C3 diamond test; composition table tests per field; default and narrowing tests; slot compilation tests; limits |
| REQ-META-020…023 | Template/page scenario; null masking; list merge; minimal own data; evolution warning |
| REQ-META-030…032 | Reference typing; relationship cardinality/validity; role grants as relationships |
| REQ-META-040 | Parent change is visible with no child invalidation; commit-keyed caches never serve a stale chain |

## 7. Implementation notes

- **Reference code:**
  - LS assembler and merge [LS:crates/ubos_kernel/src/semantic/assembler.rs#L8-L102], [LS:crates/ubos_kernel/src/semantic/merge.rs]; relation assembler [LS:crates/ubos_kernel/src/semantic/relation/assembler.rs#L11-L116].
  - US CompositeEntity [US:crates/ubos_kernel/src/graph/composite.rs#L17-L111].
  - UC recursive property lookup [UC:src/kernel/resolution.rs#L11-L59].
  - UB composer and resolver [UB:backend/src/main/java/com/ubos/ese/lifecycle/UbosSchemaComposer.java], [UB:backend/src/main/java/com/ubos/ese/lifecycle/UbosReferenceResolver.java].
- **Divergences resolved:**
  - LS uses full relation-type URIs as payload keys. This spec uses short keys plus definitions (VRD-02-07).
  - LS has both a `…/extends` relation and `_extends`. This spec uses `_extends` only.
  - UC injects `_extends` into every instance to point at its type. Here the type comes from the registry, and instance `_extends` is reserved for prototypes.
  - UB removes `_extends` from effective output. Here it is kept, and the chains are listed in `_meta`.
  - UB interpolates `{{ ubos://… }}` in text. This is deferred: P2, template properties only (§8).
- **C3 linearisation** is the algorithm used by Python's MRO. Implement it with memoisation by chain fingerprint.
- **Genesis normalisation** of corpus type codes (`sys.biz.text.email`, `abstract_text`, …) to REQ-CONV-017 codes is specified in SPEC-29.

## 8. Open questions

None.
- Deferred to P2: text interpolation of `{{ ubos://… }}` in properties marked as templates (CON-UB-027 pass 2, D-UB-20).
- Deferred to P2: behaviour refine chains beyond config inheritance (CON-LS-014 design).
