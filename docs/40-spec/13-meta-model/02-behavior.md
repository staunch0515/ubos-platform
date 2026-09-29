---
id: SPEC-13-2
title: "Meta-Model — Behavior"
status: complete
phase: P4
depends_on: [SPEC-13, SPEC-02, SPEC-11, SPEC-12]
sources: [UP, LC, UB, UC, US, LS]
---

# Meta-Model — Behavior

Part 2 of SPEC-13 (`13-meta-model/`). Header, concepts and file list: `00-index.md`.

---

## 3. Behavior

### 3.1 Bootstrap and self-description

### REQ-META-001 — Closed bootstrap
- **Statement:** Genesis (SPEC-28) MUST create, in tenant `logrums` on branch `main` and in one process:
  - `Type/Entity` (type `Type`, `_extends: []`, abstract, kind ENTITY);
  - `Type/Type` (type `Type`, `_extends: [Entity]`, kind ENTITY);
  - the L2 primitives;
  - the platform types of this chapter (`Behavior`, `Relationship`, `RelationshipType`, `Dictionary`, `TypeExtension`).

  The kernel MUST recognise `Type/Type` as the meta type by locator. It MUST validate these definitions with a built-in copy of the meta-schema, because `Type` cannot validate itself before it exists.
- **Rationale:** A self-describing, single-rooted model (UB L0/L1, LS MetaType/MetaType, UC root/meta).
- **Origin:** VRD-02-01, CON-UB-013, CON-LS-011, CON-UC-018
- **Acceptance:**
  1) After genesis, `ubos://logrums/Type/Type` has type `Type`.
  2) The effective definition of `Type` validates every genesis type, including itself.
- **Priority:** P0

### REQ-META-002 — Open types
- **Statement:** Any committed, non-abstract entity type is immediately usable as the type of new entities, with no schema migration or restart. Creating an entity whose type code is unknown, abstract or of kind VALUE MUST fail with `META.TYPE_NOT_INSTANTIABLE`.
- **Origin:** CON-UC-011, VRD-02-01
- **Acceptance:** Commit `Type/acme.Invoice`, then create `acme.Invoice/inv-1` in the next request.
- **Priority:** P0

### REQ-META-003 — Meta-schema enforcement
- **Statement:** Every commit of an entity whose type chain contains `Type`, `Behavior`, `RelationshipType`, `Dictionary` or `TypeExtension` MUST be validated against the meta-schema: the effective definition of that meta type plus the rules of REQ-META-004. A violation fails with `META.DEFINITION_INVALID`, with causes and paths. This applies to genesis, the package loader and every later commit (VRD-02-02).
- **Rationale:** "Without a meta-schema, piling up JSON is producing garbage data" (UC, D-UC-54).
- **Origin:** VRD-02-02, CON-UC-017, CON-US-014
- **Acceptance:** A type without `title`, or a REFERENCE property without `target`, is rejected with a path such as `$.properties[2].target`.
- **Priority:** P0

### REQ-META-004 — Definition rules
- **Statement:** The meta-schema MUST enforce at least the following:
  1) `code` equals the slug and matches REQ-CONV-017.
  2) `kind` is consistent along `_extends`.
  3) A value type below L2 has no own `primitive`.
  4) A REFERENCE property has a `target` of kind ENTITY.
  5) A property type is a non-abstract VALUE type.
  6) Property names are unique in the effective type and follow REQ-CONV-020…022.
  7) The names of properties added by a TypeExtension carry the extension's namespace.
  8) `default` validates against the property.
  9) Constraints narrow (REQ-META-014).
  10) A slot binding names a slot of the right level, and its implementation's declared contract matches (§2.6).
  11) A `dict` constraint references an existing Dictionary.
  12) Reserved keys (REQ-CONV-023) are not used as property names.
  13) A property with `unique ≠ NONE` or `on_target_delete ≠ IGNORE` is `searchable` (SPEC-18 REQ-RULE-020, REQ-RULE-045).
- **Origin:** VRD-02-02, CON-UC-017
- **Acceptance:** One negative test per rule.
- **Priority:** P0

### REQ-META-005 — Published meta-schema
- **Statement:** The kernel MUST export the meta-schema as JSON Schema (draft 2020-12) at the protocol query `ubos://system/Query/meta.schema@v1`. The export covers `Type`, `PropertyDefinition`, `SlotBinding`, `Behavior`, `RelationshipType`, `Dictionary`, `TypeExtension` and `Relationship`. The same file ships in `spec-fixtures/meta-schema.json` for AI tools and editors.
- **Origin:** VRD-02-02, NFR-AIR
- **Acceptance:** An external JSON Schema validator accepts every genesis definition and rejects the negative fixtures of REQ-META-004.
- **Priority:** P1

### 3.2 Type chain and composition

### REQ-META-010 — Linearisation of `_extends`
- **Statement:** The type chain of a type MUST be the C3 linearisation of its `_extends` graph (self first, `Entity` last). Parents in `_extends` are listed in order of precedence.
  - A cycle fails with `META.INHERITANCE_CYCLE`.
  - A graph with no C3 order fails with `META.INHERITANCE_CONFLICT`.
  - A chain longer than 32 fails with `META.INHERITANCE_TOO_DEEP`.

  These checks run at commit time of the type. Reads that meet a cycle (e.g. created concurrently on different branches) fail with the same codes.
- **Rationale:**
  - Multiple inheritance occurs in the corpus (UC `_extends` lists, LS multiple parents).
  - C3 gives a deterministic, monotonic order. Depth limits in the corpus were 10 (UB) and 20 (UC).
- **Origin:** VRD-02-05, CON-UC-012, CON-LS-023, CON-US-012, CON-UB-027
- **Acceptance:** A diamond `D extends [B, C]`, `B, C extends [A]` linearises to `D, B, C, A, Entity`. A cycle is rejected at commit.
- **Priority:** P0

### REQ-META-011 — Effective type definition
- **Statement:** The effective definition of type T MUST be composed from the chain in reverse (root first), and then from the TypeExtensions visible in the current tenant, ordered by extension slug. The composition rules are:

| Field | Composition |
|---|---|
| `properties` | merged by `name`; a redefinition in a descendant overlays the inherited definition field by field (narrowing only); order = ancestor order then declaration order |
| `slots` (type and property level) | per slot: REPLACE → nearest binding wins; CHAIN → all bindings in chain order (root first) sorted by `priority`; DISABLE → removes inherited bindings of that slot |
| `ui_modes`, `display`, `i18n`, `default_views` | deep-merged per key, descendant wins |
| `constraints` (value types) | intersection (narrowing) |
| `invariants`, `rule_refs`, `schema_extra` | concatenated, de-duplicated by name/URI (`schema_extra` combined with allOf) |
| `primitive`, `kind` | inherited, unchanged |
| `is_abstract` | not inherited (own value, default false) |
| `open`, `searchable`, `lifecycle`, `embedding`, `write_policy`, `overlay_policy` | nearest non-absent value wins |
| `title`, `description`, `icon`, `tags`, `code` | own value only |

  Property definitions also take the effective definition of their value type (its constraints, slots, `ui_modes`, `searchable`, `sensitivity`). Property-level settings override value-type settings under the same rules.
- **Origin:** VRD-02-04, VRD-02-05, CON-LS-023, CON-US-012, CON-UB-027
- **Acceptance:** A property that is required in a parent stays required in the child. A CHAIN `validate` binding on a parent and one on a child both run, parent first.
- **Priority:** P0

### REQ-META-012 — Defaults
- **Statement:** A property default MUST be computed only when the key is **absent** from the composed data (REQ-CONV-041). If the property has a `default` slot, the slot is used. Otherwise the static `default` is used.
  - Defaults are applied at read time (effective data) and are written into own data only at CREATE when the property is `required` (so required values are stored explicitly).
  - A default that fails validation is a definition error (REQ-META-004).
- **Origin:** CON-US-012, CON-UC-012, VRD-02-05
- **Acceptance:** Changing a type's default changes the effective value of existing instances that did not set the key. Instances that stored a value are unaffected.
- **Priority:** P0

### REQ-META-013 — Instance type conformance
- **Statement:** An instance's own data MUST contain only the following keys:
  - declared properties of its effective type;
  - reserved keys (REQ-CONV-023);
  - undeclared keys, but only when the effective type is `open`.

  Otherwise the write fails with `META.UNKNOWN_PROPERTY`. On read, unknown keys (e.g. after a property was removed from the type) are kept in own data and listed in `_meta.unknown_keys` of the effective view, and never cause a read error.
- **Origin:** CON-UC-014 (strict input wiring), CON-US-014, NEW: lenient reads
- **Acceptance:**
  1) Writing `{"colour": "red"}` to a type without `colour` fails.
  2) After removing a property from the type, old versions still read, and the key appears in `_meta.unknown_keys`.
- **Priority:** P0

### REQ-META-014 — Narrowing only
- **Statement:** When a descendant type, property redefinition, property-level constraint or TypeExtension changes an inherited definition, it MUST only narrow it. The rules:
  - `min` may increase and `max` may decrease;
  - `regex` is combined with the inherited regex, and both must match;
  - `values` and `dict` may be restricted to a subset;
  - `required` may change from false to true, never the reverse;
  - `type` may change only to a descendant value type;
  - `target` may change only to a descendant entity type;
  - `many` and `primitive` may not change.

  A violation fails with `META.WIDENING`.
- **Rationale:** Substitutability: an instance of a subtype is valid wherever its supertype is expected (projection, SPEC-25; references with subtype targets).
- **Origin:** NEW: required for VRD-13-04 projection and reference typing
- **Acceptance:** A child that sets `max_length: 500` under a parent's `max_length: 100` is rejected.
- **Priority:** P0

### REQ-META-015 — Type attributes compile to slot defaults
- **Statement:** The declarative attributes of a value type MUST compile into default slot bindings using native handlers. Explicit `slots` override them by the rules of REQ-META-011.

| Attribute | Slot | Native |
|---|---|---|
| `constraints.regex/min/max/max_length/scale/values/format` | `validate` | `ubos://system/Native/validate.constraints@v1` |
| `constraints.dict` | `validate` | `ubos://system/Native/validate.dict@v1` |
| all constraints | `schema` | `ubos://system/Native/schema.from_constraints@v1` |
| `masking_rule` | `mask` | `ubos://system/Native/mask.pattern@v1` |
| `ui_modes` | `ui` | `ubos://system/Native/ui.modes@v1` |
| `searchable` + primitive | `index` | `ubos://system/Native/index.primitive@v1` |
| `display.summary_template` | `summary` | `ubos://system/Native/format.template@v1` |
- **Rationale:** "One regex change updates every usage" (UB), while any single behaviour stays replaceable (LS).
- **Origin:** VRD-02-04, CON-UB-057, CON-LS-012
- **Acceptance:** An `Email` property with no explicit slots validates, masks and renders through these natives. Overriding `validate` on the property replaces only that slot.
- **Priority:** P0

### REQ-META-016 — Slot execution contract
- **Statement:** The kernel MUST invoke slot implementations through the logic runtime (SPEC-17), with the slot's input as `params` and a slot-specific syscall profile:
  - Pure slots use the read-only profile: reads are recorded as provenance, and writes and emits fail with `RT.CAPABILITY_DENIED`.
  - Non-pure slots may stage changes and return emits, inside the current session.
  - An implementation returning an output that does not match the contract fails with `META.SLOT_CONTRACT`.
  - CHAIN execution stops at the first veto Decision (SPEC-18) or error.
- **Origin:** VRD-02-04, CON-LS-013, VRD-05-02
- **Acceptance:** A `mask` implementation that calls `entity.save` fails with `RT.CAPABILITY_DENIED`.
- **Priority:** P0

### REQ-META-017 — Limits
- **Statement:** Definitions MUST respect these limits:
  - type chain depth ≤ 32;
  - prototype chain depth ≤ 16;
  - effective properties per type ≤ 500;
  - searchable property paths per type ≤ 64;
  - nesting depth of MAP values ≤ 16;
  - a type definition snapshot ≤ 256 KiB.

  Violations fail with `META.LIMIT` (details name the limit).
- **Origin:** NEW: bounded resolution cost; CON-UB-027 (depth 10), CON-UC-012 (depth 20)
- **Acceptance:** One test per limit.
- **Priority:** P1

### 3.3 Effective data of instances

### REQ-META-020 — Instance resolution algorithm
- **Statement:** The effective data of an instance version *v* of type T, read in resolved address *a*, MUST be computed as follows:
  1) **Assemble.**
     - Load the effective definition of T (REQ-META-011) as visible in *a*: the tenant, the branch chain and the pin.
     - Load *v*'s own data.
     - If own data has `_extends`, resolve each prototype URI in *a*. Each prototype must be of T or a supertype of T, else `META.PROTOTYPE_TYPE`.
     - Recursively assemble the prototypes' effective data, with C3 order, cycle detection and depth ≤ 16.
  2) **Compose**, starting from an empty object:
     a) Apply the property defaults of T for keys that stay absent after steps b and c (REQ-META-012).
     b) Overlay the prototypes' effective data in reverse linearisation order (farthest first).
     c) Overlay own data.

     Overlays deep-merge objects, replace scalars, and replace lists unless the property has `merge_key` (REQ-META-021). An explicit `null` in a nearer layer removes the value.
  3) **Compute** READ-mode `compute` properties on the composed object.
  4) **Resolve references lazily.** References stay URIs. When the caller asks to `expand` paths or a depth (default 0, max 3), reference values are replaced by `{_uri, _type, _id, …effective data of the target}` (CON-UC-051). Unresolvable references become `{_uri, _error: <code>}`.
  5) **Mask** per reader (SPEC-22), then **format** if a summary view is requested.
  6) **Execute nothing.** Actions are descriptors until invoked (SPEC-19).

  The result carries `_meta = {entity_id, type, commit_id, branch, type_chain, prototype_chain, own_keys, inherited_keys, unknown_keys}`.
- **Rationale:** UB's compose → resolve → execute split, US's merge order (defaults, then explicit, root first) and UC's hydration, made deterministic.
- **Origin:** VRD-02-05, CON-UB-027, CON-US-012, CON-UC-012, CON-UC-051
- **Acceptance:**
  1) A Page instance extending a Book template shows the template's values, except where it overrides them.
  2) `null` in own data hides an inherited value.
  3) `_meta.inherited_keys` lists the template-supplied keys.
  4) `expand=1` hydrates references one level.
- **Priority:** P0

### REQ-META-021 — List composition
- **Statement:** For a `many: true` property with `merge_key` *k*, composition MUST merge lists element-wise by `item[k]`:
  - A nearer layer overrides elements with the same key and appends new keys.
  - An element `{k: x, "_remove": true}` removes key *x*.
  - Order: inherited order first, then new elements in their layer order.

  Without `merge_key`, the nearest layer's list replaces the inherited list entirely.
- **Origin:** REQ-CONV-043, NEW: needed for inherited line items and menus
- **Acceptance:** A template with lines A and B, and an instance with line B modified and line C added, composes to A, B', C.
- **Priority:** P1

### REQ-META-022 — Writes store only own data
- **Statement:** A write through the effective view (API-META-003) MUST compute the minimal own data that yields the desired effective data:
  - A key whose desired value equals the inherited value is not stored, unless the property is `required` (REQ-META-012) or the client explicitly pins it (`_pin_keys`).
  - Setting an inherited value to `null` stores `null`.
  - "Reset to inherited" removes the key from own data.

  Direct Mutate of own data (SPEC-15) bypasses this computation.
- **Rationale:** Editing a child must never flatten inherited values (UB `selfData`).
- **Origin:** CON-UB-015, VRD-02-05, VRD-08-02
- **Acceptance:** Editing one field of a template-derived page stores one key.
- **Priority:** P0

### REQ-META-023 — Type evolution
- **Statement:** A commit that changes an entity type MUST NOT rewrite existing instances.
  - The flush reports warning `META.INSTANCES_AFFECTED`, with the number of ACTIVE instances on the branch that would fail the new structural schema (computed asynchronously when more than 10 000).
  - Each version records the `type_commit_id` it was validated against (REQ-STO-004 context).
  - Instances are re-validated when next written.
  - Migrations of data are ordinary processes (e.g. a Pipeline over a query), never schema changes.
- **Origin:** VRD-01-02, CON-FU-020 (versioned schemas), NEW
- **Acceptance:** Adding a required property without a default returns the warning with a count, and existing reads still succeed.
- **Priority:** P1

### 3.4 Relationships

### REQ-META-030 — Reference properties
- **Statement:** A reference property MUST store the canonical URI (REQ-URI-007) in the declared `ref_form`, with `?commit=` when `pinned`. On write, the target MUST resolve, and its type MUST be the declared `target` or a subtype. Otherwise the write fails with `META.REFERENCE_INVALID` (at DRAFT level only a syntax check is made, SPEC-18). Reference values are indexed with `val_ref` when searchable (REQ-STO-007).
- **Origin:** VRD-02-03, CON-UC-051, CON-UC-066
- **Acceptance:** A `customer` reference to an `acme.Supplier` entity fails when `target` is `Customer` and Supplier is not a subtype of it.
- **Priority:** P0

### REQ-META-031 — Relationship entities
- **Statement:** Committing a `Relationship` MUST check, at PUBLISH level:
  - the `relationship_type` exists;
  - the source and target types are allowed;
  - validity is present when required, and ordered;
  - `metadata` conforms to `metadata_schema`;
  - the cardinality (`max_per_source`, `max_per_target`) and overlap rules hold over ACTIVE relationships on the branch whose validity intersects.

  Symmetric types are stored once. Queries treat them as bidirectional (SPEC-21). Violations fail with `META.RELATIONSHIP_INVALID` and a cause per rule.
- **Origin:** VRD-02-03, CON-LC-014, CON-LC-024
- **Acceptance:**
  1) A second concurrent WORKS_FOR with `max_per_source: 1` and overlapping validity fails.
  2) A symmetric IS_MARRIED_TO is found from both endpoints.
- **Priority:** P1

### REQ-META-032 — Relationships for access
- **Statement:** Role grants and group memberships MUST be Relationship entities with platform RelationshipTypes (`HAS_ROLE`, `MEMBER_OF`, `GRANTS_ACCESS_TO`). SPEC-22 defines them. The meta-model imposes no special storage.
- **Origin:** VRD-02-03, VRD-10-02, CON-LC-017
- **Acceptance:** Revoking a role is ending a relationship's validity or deleting it, and both are versioned.
- **Priority:** P0

### 3.5 Caches

### REQ-META-040 — Commit-keyed semantic caches
- **Statement:** The `semantic` component MUST cache:
  1) raw own data by commit ID (permanent, LRU-evicted);
  2) effective type definitions by **chain fingerprint**: the ordered list of (entity ID, commit ID) of the type chain, prototype chain and applicable TypeExtensions;
  3) head pointers per (tenant, branch, entity), invalidated by flush events.

  Effective definitions MUST NOT be invalidated by time. A change to any chain member produces a new fingerprint. Instance effective data MAY be cached per request only, unless a read is pinned by commit.
- **Rationale:** Immutable versions make commit-keyed caches permanently valid (LS). Caching raw fragments and assembling just in time avoids cascade invalidation (UB design).
- **Origin:** CON-LS-007, CON-LS-044, CON-UB-045, CON-UB-028
- **Acceptance:** Changing a parent type makes children's effective definitions reflect the change at the next read, with no explicit invalidation of the children.
- **Priority:** P0
