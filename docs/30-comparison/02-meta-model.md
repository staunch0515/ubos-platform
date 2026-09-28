---
id: CMP-02
title: "Comparison: Meta-model and Type System"
status: complete
phase: P2
depends_on: [CMP-00, CMP-01]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Comparison: Meta-model and Type System

## 1. Question

How are types, properties, inheritance and relationships defined so that the platform is self-describing? The same definitions must drive storage, validation, UI, behaviour and AI generation.

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | `TYPE_DEF` entities + URI validators; untyped edge table | CON-UP-013, 015, 016 | Early self-description; relations are unversioned edges |
| FU | `SCHEMA/{TYPE}` JSON Schema entities | CON-FU-014, HL-FU-002 | Type = versioned JSON Schema checked on commit |
| LC | JSON Schema + extensions; relationship-as-entity + relationship-type catalogue | CON-LC-014, 015, HL-LC-003 | Strong typing of properties and relationships (endpoints, symmetry, validity, metadata schema) |
| UB | Single-rooted semantic type tree + universal ontology; effective vs self data | CON-UB-013, 014, 015, 027, HL-UB-003, 004, 007 | Types carry validation, masking, widgets, units; three-pass compose/resolve/execute |
| UC | UODS strict meta-schema; explicit `_extends`; open entity type | CON-UC-011, 012, 017, 018, HL-UC-008, 009 | Rules for definitions, e.g. relation needs `target`, enum needs `dict_code`, numbers need `scale` |
| US | `ui_modes` on types; CompositeEntity; composition model and EOP (design) | CON-US-010, 012, 013, 015, HL-US-002, 003 | Merged inherited view; behaviour → relation type → entity type |
| LS | Entity = UUID + relations; RelationType with behaviour slots; 20 BehaviorTypes | CON-LS-010…015, HL-LS-001, 002 | Property semantics composed from slot implementations |
| UW | Plain TS types | CON-UW-010…014 | No meta-model |

## 3. Dimension matrix

| Dimension | UP | FU | LC | UB | UC | US | LS |
|---|---|---|---|---|---|---|---|
| Types are entities | ● | ● | files | ● | ● | ● | ● |
| Single root / meta bootstrap | ◐ | – | – | ● | ● | ● | ● |
| Inheritance mechanism | – | – | – | `_extends` compose | `_extends` recursive | composite merge | extends + deep merge |
| Semantic primitives (money, email…) | ◐ | – | – | ● | ● | ● | ◐ |
| Property-level behaviour config | validator ref | – | metadata keys | type attrs | type attrs | ui_modes | ● slots |
| Relationships as first-class | edge table | – | ● entity + catalogue | relation type | relation type w/ target | design | ● relation types |
| Definition validation (meta-schema) | – | JSON Schema | JSON Schema | ◐ | ● UODS | spec v1.0 | slot schemas |
| Effective vs own data separation | – | – | – | ● | – | ● | ● |
| Simplicity | ● | ● | ◐ | ◐ | ◐ | ◐ | ○ |
| AI-friendliness | ◐ | ● | ● | ● | ● | ● | ◐ (URI keys verbose) |

## 4. Analysis

- **Convergence.**
  - UB, UC and US share the vocabulary root → meta → primitives → abstract bases → semantic types → tenant extensions, with explicit `_extends` and type attributes that drive UI and validation (CON-UB-013, CON-UC-018).
  - This is the most developed type system in the corpus and already contains a universal business ontology (HL-UB-004).
- **Strictness.** UC's UODS makes definitions checkable. It sets mandatory fields, requires `target` on relations and `dict_code` on enums, and requires `primitive_type` and `store_type` on meta primitives (HL-UC-008). US's metadata spec adds URN categories (Type, Action, Logic, Widget) and reserved keys (CON-US-014).
- **Behaviour on properties.**
  - LS goes furthest. A relation type configures slot implementations for storage, UI, validation, masking, defaults, hooks, computation and permission (HL-LS-002).
  - UB, UC and US put the same concerns as plain attributes on types (`validation_regex`, `masking_rule`, `ui_modes`).
  - *Interpretation:* LS's slots are the generalisation. Attribute shortcuts can be kept as the default slot configuration.
- **Relationships.**
  - LC treats relationships as versioned entities with a type catalogue: allowed endpoints, symmetry, validity, and a metadata schema (HL-LC-003).
  - UP's edge table is unversioned; UB and UC model relations as a `relation` property type with `target`.
  - Both are needed. Reference properties (`relation` type) cover simple links. Relationship entities cover links that have a lifecycle, attributes and validity.
- **Inheritance resolution.** UB separates compose → resolve → execute (HL-UB-007) and effective vs own data (CON-UB-015). US assembles a composite with cycle detection and a root-to-head merge (CON-US-012). LS caches assembled nodes by commit id (HL-LS-007). These are compatible layers of one algorithm.
- **Keys.** LS keys payloads by full relation-type URIs. This is unambiguous but verbose for humans, LLM prompts and JSON size. The other repositories use short property names defined by the type.
- **Composition design.**
  - US's "behaviour → relation type → entity type" (CON-US-010) is the conceptual statement of which LS is the implementation.
  - EOP adds invariants and derivations at the type level (CON-US-013); rules are covered in CMP-09.

## 5. Verdicts

### VRD-02-01 — Single-rooted, self-describing semantic type system as entities (fusion UB + UC)
- **Decision:** fusion.
- **Consequences:**
  - Types are entities of the meta type with explicit `_extends` (string or list).
  - Layers: root, meta, abstract primitives (with primitive and store type), abstract business bases, semantic types, and tenant or app extensions.
  - Business properties reference semantic types, never raw primitives.
  - Open entity types: any registered type code is a "logical table" (CON-UC-011).
- **Dropped:** hard-coded type enums; UP's ad-hoc `TYPE_DEF`.

### VRD-02-02 — Definitions are validated by a meta-schema (fusion UC UODS + US metadata spec)
- **Decision:** fusion.
- **Consequences:**
  - Mandatory definition fields: `code`, `name`, `_extends`, `entity_type`.
  - Property fields: `name`, `type`, plus type-specific requirements (relation → `target`, enum → `dict_code`, numeric → `scale`/`unit`).
  - Naming regex; reserved keys.
  - The genesis loader and every type commit reject violations.
  - The spec publishes the meta-schema as a JSON Schema for AI tools.

### VRD-02-03 — Two relationship forms: reference property and relationship entity (fusion UB/UC + LC)
- **Decision:** fusion.
- **Consequences:**
  - **Reference property:** a `relation` property with `target` type, storing a `ubos://` URI.
  - **Relationship entity:** for links with attributes, lifecycle or validity. It uses a `Relationship` type with `type`, `source`, `target`, `valid_from`, `valid_to` and `metadata`, governed by a relationship-type catalogue (allowed endpoints, symmetric, validity required, metadata schema).
  - Access control (roles, grants) uses relationship entities (see VRD-10).
- **Dropped:** UP's unversioned edge table.

### VRD-02-04 — Property behaviour through slots, with type attributes as defaults (fusion LS + UB/UC/US)
- **Decision:** fusion.
- **Consequences:**
  - The spec defines a closed catalogue of behaviour slots (from LS's 20, CON-LS-013) with input and output contracts.
  - A property's semantic type supplies default slot implementations: validation regex → validation slot, masking rule → mask slot, `ui_modes` → UI slot.
  - A property definition or relation type may override individual slots.
  - Implementations are Logic entities or native functions.
- **Dropped:** hard-coded dispatch (LS mock).

### VRD-02-05 — Inheritance resolution algorithm (fusion UB + US + LS)
- **Decision:** fusion.
- **Consequences:**
  - (1) Assemble the ancestor chain via `_extends` with cycle detection.
  - (2) Compose: deep-merge root → … → head, applying schema defaults before explicit values.
  - (3) Resolve `ubos://` references (lazily for cross-entity types).
  - (4) Execute actions only on interaction.
  - Keep `effective` and `own` data separate; writes store only own data.
  - Cache assembled nodes by commit id.

### VRD-02-06 — Universal business ontology as the base domain library (fusion UB + UC)
- **Decision:** fusion.
- **Consequences:**
  - Genesis ships the ontology normalised to VRD-02-02: party/role/relationship, contact and geo, reference data, documents, security, IT, measures, PKI, kernel logic, UI.
  - Verticals extend it; the spec lists the ontology as a separate chapter ("domain packs").

### VRD-02-07 — Short property keys in payloads; relation-type URIs in type definitions (NEW, from LS critique)
- **Decision:** NEW.
- **Rationale:** Full-URI keys (LS) are unambiguous but inflate payloads and prompts. The ambiguity they solve is removed if each type definition maps its short property names to property or relation-type URIs.
- **Consequences:** Payload `{"email": "…"}`; the type declares `properties[{name: "email", type: "ubos://…/Type/email", relation_type?: "ubos://…/RelationType/prop_email"}]`.
- **Open:** Q-006.

## 6. Open questions

- Q-006: Should cross-tenant or extension properties use namespaced keys (`acme.email`) to avoid collisions when a tenant extends a platform type? Candidate: yes, dotted namespaces for non-root extensions.
