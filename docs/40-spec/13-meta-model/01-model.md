---
id: SPEC-13-1
title: "Meta-Model — Model"
status: complete
phase: P4
depends_on: [SPEC-13, SPEC-02, SPEC-11, SPEC-12]
sources: [UP, LC, UB, UC, US, LS]
---

# Meta-Model — Model

Part 1 of SPEC-13 (`13-meta-model/`). Header, concepts and file list: `00-index.md`.

---

## 2. Model

### 2.1 Root type `Entity`

```yaml
ENT-Entity:
  purpose: Root type of all entities; defines the universal descriptor properties.
  origin: [VRD-02-01, CON-UB-013, CON-UC-018, CON-US-011, CON-US-016]
  fields:
    - name: _extends
      type: list<uri<Entity>>?
      required: false
      description: Prototype parents (instances) or supertypes (types); a single string is accepted and normalised to a one-element list.
    - name: title
      type: string(255)?
      required: false
      description: Human-readable name (US sys_descriptor); copied to entity_instance.display_name.
    - name: description
      type: text?
      required: false
      description: Human-readable description.
    - name: icon
      type: string(64)?
      required: false
      description: Icon name from the UI icon set (SPEC-25).
    - name: tags
      type: list<string>?
      required: false
      description: Free tags; copied to entity_instance.tags.
  invariants:
    - Entity is abstract and has no _extends.
    - Every other type has Entity in its type chain.
```

### 2.2 Type definition (own data of a `Type` entity)

```yaml
ENT-Type:
  purpose: Definition of a value type or an entity type; instances of Type are types (TERM-Type).
  origin: [VRD-02-01, VRD-02-02, CON-UC-017, CON-US-014, CON-UB-013, CON-LS-011, CON-UP-016]
  fields:
    - name: code
      type: type_code
      required: true
      description: Must equal the entity's slug (REQ-CONV-017).
    - name: title
      type: string(255)
      required: true
      description: Display name of the type (the UODS "name").
    - name: description
      type: text?
      required: false
      description: Purpose, written for people and AI.
    - name: _extends
      type: list<uri<Type>>
      required: true
      description: Supertypes; empty only for Entity. Value types extend value types; entity types extend entity types.
    - name: kind
      type: enum{VALUE|ENTITY}
      required: true
      description: Value type (for properties) or entity type (for entities). Must equal the kind of every supertype.
    - name: is_abstract
      type: bool
      required: false
      description: Abstract types cannot be instantiated (entity) or used directly in properties (value). Not inherited. Default false.
    - name: primitive
      type: enum{TEXT|NUMBER|INTEGER|BOOLEAN|DATE|TIMESTAMP|MAP|LIST|REFERENCE|JSON}?
      required: false
      description: VALUE only; set on L2 primitives, inherited by descendants, never changed below L2.
    - name: constraints
      type: ValueConstraints?
      required: false
      description: VALUE only; declarative value rules (regex, min, max, max_length, scale, unit, dict, allowed values); narrowed by descendants (REQ-META-014).
    - name: masking_rule
      type: string?
      required: false
      description: VALUE only; mask pattern (e.g. "keep_last:4", "email", "full"); default for the mask slot.
    - name: properties
      type: list<PropertyDefinition>?
      required: false
      description: ENTITY only; own property definitions (merged along the type chain).
    - name: open
      type: bool?
      required: false
      description: ENTITY only; when true, instance data may contain undeclared keys (configuration-like types). Default false.
    - name: slots
      type: map<slot_name, SlotBinding>?
      required: false
      description: Type-level slot bindings (VALUE - defaults for properties of this type; ENTITY - lifecycle slots).
    - name: ui_modes
      type: map<mode, UiModeSpec>?
      required: false
      description: Widget per mode (view, edit, create, list, filter, cell) with config (CON-US-015); default for the ui slot.
    - name: display
      type: DisplaySpec?
      required: false
      description: ENTITY only; title_property, summary_template, icon, color.
    - name: searchable
      type: bool?
      required: false
      description: VALUE only; default searchability of properties of this type. Default false.
    - name: invariants
      type: list<InvariantSpec>?
      required: false
      description: ENTITY only; PREV/NEW rules (SPEC-18, ADR-002). Additive along the chain.
    - name: rule_refs
      type: list<uri>?
      required: false
      description: ENTITY only; Constraint entities that apply (SPEC-18). Additive.
    - name: lifecycle
      type: uri?
      required: false
      description: ENTITY only; the state machine definition that governs _state (SPEC-19).
    - name: default_views
      type: map<mode, uri>?
      required: false
      description: ENTITY only; View entities per mode (SPEC-25).
    - name: i18n
      type: map<string, string>?
      required: false
      description: i18n keys for title, description and property labels (SPEC-27).
    - name: embedding
      type: EmbeddingSpec?
      required: false
      description: ENTITY only; which properties feed version embeddings (SPEC-21). Default none.
    - name: schema_extra
      type: json_schema?
      required: false
      description: ENTITY only; explicit JSON Schema combined with the derived schema by allOf (SPEC-18 REQ-RULE-001, FU schema entities). Additive along the chain.
    - name: write_policy
      type: WritePolicy?
      required: false
      description: ENTITY only; {mutate - ALLOW|DENY|ADMIN_ONLY (direct UBTP Mutate, SPEC-15 REQ-TX-030), require_expected_head - bool (REQ-TX-005)}. Nearest value wins. Default {ALLOW, false}.
  invariants:
    - The instance's type is Type (or a subtype of Type).
    - kind(T) = kind(p) for every supertype p; Entity and Type are ENTITY.
    - primitive is set exactly on L2 primitives and inherited unchanged.
    - VALUE types have no properties, open, display, invariants, rule_refs, lifecycle, default_views.
    - The linearised type chain is acyclic and at most 32 deep (REQ-META-017).
```

```yaml
ENT-ValueConstraints:
  purpose: Declarative constraints of a value type or a property (compiled to validate/schema slots).
  origin: [CON-UB-013, CON-UC-017, CON-FU-020]
  fields:
    - {name: regex, type: string?, required: false, description: "ECMA-262 regex subset, anchored, max 1 KiB"}
    - {name: min, type: decimal?, required: false, description: "Minimum (numbers) or minimum length (text, lists)"}
    - {name: max, type: decimal?, required: false, description: "Maximum (numbers) or maximum length"}
    - {name: max_length, type: int32?, required: false, description: "Maximum text length in characters"}
    - {name: scale, type: int32?, required: false, description: "Decimal places for NUMBER (money 2, fx 6, conversion 10)"}
    - {name: unit, type: string?, required: false, description: "Unit symbol for measures (kg, m, s)"}
    - {name: dict, type: uri<Dictionary>?, required: false, description: "Allowed values from a Dictionary entity (UODS dict_code)"}
    - {name: values, type: list<json>?, required: false, description: "Inline allowed values (small enums)"}
    - {name: format, type: string?, required: false, description: "JSON Schema format keyword (email, uri, date, date-time, uuid)"}
  invariants:
    - A descendant or property may only narrow constraints (REQ-META-014).
```

### 2.3 Property definition

```yaml
ENT-PropertyDefinition:
  purpose: One property of an entity type (inline element of Type.properties).
  origin: [VRD-02-02, VRD-02-04, VRD-02-07, CON-UC-017, CON-US-014, CON-LS-012, CON-LC-015]
  fields:
    - name: name
      type: string(160)
      required: true
      description: Payload key (REQ-CONV-020…022); unique within the effective type.
    - name: type
      type: uri<Type>
      required: true
      description: A non-abstract VALUE type (semantic type preferred).
    - name: title
      type: string(255)?
      required: false
      description: Label; default from the value type.
    - name: description
      type: text?
      required: false
      description: Help text for people and AI.
    - name: required
      type: bool?
      required: false
      description: Required at PUBLISH level (SPEC-18). Default false.
    - name: many
      type: bool?
      required: false
      description: Value is a list of the value type. Default false.
    - name: merge_key
      type: string?
      required: false
      description: For many=true with MAP items - key used to merge inherited lists element-wise (REQ-META-021).
    - name: default
      type: json?
      required: false
      description: Default value; must validate against the property (REQ-META-012).
    - name: target
      type: uri<Type>?
      required: false
      description: Required when the value type's primitive is REFERENCE - allowed entity type (subtypes accepted).
    - name: ref_form
      type: enum{LOCATOR|ID}?
      required: false
      description: REFERENCE only; stored reference form (SPEC-12 REQ-URI-012). Default LOCATOR.
    - name: pinned
      type: bool?
      required: false
      description: REFERENCE only; store the target with ?commit (version-pinned reference). Default false.
    - name: on_target_delete
      type: enum{IGNORE|RESTRICT|NULLIFY}?
      required: false
      description: REFERENCE only; behaviour when the target is deleted on the same branch (SPEC-18). Default IGNORE.
    - name: constraints
      type: ValueConstraints?
      required: false
      description: Narrowing of the value type's constraints.
    - name: unique
      type: enum{NONE|TENANT|BRANCH}?
      required: false
      description: Uniqueness among ACTIVE instances of the declaring type and its subtypes (SPEC-18 constraint). Default NONE.
    - name: searchable
      type: bool?
      required: false
      description: Write search_index rows (REQ-STO-007). Default from value type.
    - name: read_only
      type: bool?
      required: false
      description: Not writable by clients; only by slots (compute, transform) and kernel. Default false.
    - name: slots
      type: map<slot_name, SlotBinding>?
      required: false
      description: Per-property slot overrides (property-level slots only, §2.6).
    - name: ui_modes
      type: map<mode, UiModeSpec>?
      required: false
      description: Per-mode widget overrides.
    - name: sensitivity
      type: enum{NONE|PERSONAL|SECRET}?
      required: false
      description: Privacy class for masking, logging and AI exposure (SPEC-22, SPEC-27). Default from value type or NONE.
    - name: deprecated
      type: bool?
      required: false
      description: Kept for old data; not offered in create/edit UIs.
  invariants:
    - target is set iff the value type's primitive is REFERENCE.
    - merge_key only with many = true.
    - A subtype may redefine an inherited property only by narrowing (REQ-META-014); it may not change type to a non-descendant.
```

```yaml
ENT-SlotBinding:
  purpose: Binding of a slot to an implementation.
  origin: [VRD-02-04, CON-LS-012, CON-LS-014]
  fields:
    - {name: impl, type: uri, required: true, description: "Logic, Native (ubos://system/Native/...) or Behavior URI"}
    - {name: config, type: json?, required: false, description: "Configuration passed to the implementation"}
    - {name: mode, type: "enum{REPLACE|CHAIN|DISABLE}?", required: false, description: "Override or chain with inherited bindings; default per slot (§2.6)"}
    - {name: priority, type: int32?, required: false, description: "Order within a CHAIN (lower first); default 100"}
```

```yaml
ENT-UiModeSpec:
  purpose: Widget choice and configuration for one presentation mode.
  origin: [CON-US-015, CON-UC-055, CON-UB-057]
  fields:
    - {name: widget, type: "uri<Widget>", required: true, description: "Widget entity (SPEC-25)"}
    - {name: config, type: json?, required: false, description: "Widget options (e.g. {language: rhai}, {copyable: true})"}
```

### 2.4 Behavior entity

```yaml
ENT-Behavior:
  purpose: A reusable, configured implementation of one slot (TERM-Behavior).
  origin: [CON-LS-013, CON-LS-014, CON-US-010]
  fields:
    - {name: slot, type: slot_name, required: true, description: "The slot it implements (§2.6)"}
    - {name: impl, type: uri, required: true, description: "Logic or Native URI doing the work"}
    - {name: config, type: json?, required: false, description: "Default configuration, merged under binding config"}
    - {name: config_schema, type: json_schema?, required: false, description: "Schema of accepted configuration"}
    - {name: pure, type: bool, required: true, description: "Declares no writes and no emits; required true for pure slots"}
  invariants:
    - impl's declared input/output matches the slot contract (§2.6).
    - _extends to another Behavior means config inheritance (refine chain, CON-LS-014).
```

### 2.5 Relationship and RelationshipType

```yaml
ENT-Relationship:
  purpose: A link with attributes, lifecycle or validity (TERM-Relationship).
  origin: [VRD-02-03, CON-LC-014, CON-LC-017]
  fields:
    - {name: relationship_type, type: "uri<RelationshipType>", required: true, description: "Governing catalogue entry"}
    - {name: source, type: "uri<Entity>", required: true, description: "Source entity (stored reference, searchable)"}
    - {name: target, type: "uri<Entity>", required: true, description: "Target entity (stored reference, searchable)"}
    - {name: valid_from, type: timestamp?, required: false, description: "Start of validity"}
    - {name: valid_to, type: timestamp?, required: false, description: "End of validity (exclusive)"}
    - {name: metadata, type: json?, required: false, description: "Attributes validated by the type's metadata_schema"}
  invariants:
    - source type and target type are allowed by the RelationshipType (subtypes accepted).
    - valid_from < valid_to when both are set; both required if validity_required.
    - Slug default - "<relationship_type code>.<base32 of 10 random bytes>".
```

```yaml
ENT-RelationshipType:
  purpose: Catalogue entry governing Relationship entities (TERM-RelationshipType).
  origin: [VRD-02-03, CON-LC-014]
  fields:
    - {name: code, type: slug, required: true, description: "UPPER_SNAKE code, e.g. WORKS_FOR, HAS_ROLE; equals slug"}
    - {name: title, type: string, required: true, description: "Label"}
    - {name: inverse_title, type: string?, required: false, description: "Label from the target side (e.g. EMPLOYS)"}
    - {name: allowed_source_types, type: "list<uri<Type>>", required: true, description: "Entity types allowed as source"}
    - {name: allowed_target_types, type: "list<uri<Type>>", required: true, description: "Entity types allowed as target"}
    - {name: symmetric, type: bool, required: true, description: "A-B implies B-A (queries match both directions)"}
    - {name: validity_required, type: bool, required: true, description: "valid_from and valid_to required"}
    - {name: max_per_source, type: int32?, required: false, description: "Cardinality limit of active relationships per source (at a point in time)"}
    - {name: max_per_target, type: int32?, required: false, description: "Cardinality limit per target"}
    - {name: allow_overlap, type: bool?, required: false, description: "Whether validity periods of the same pair may overlap; default false"}
    - {name: metadata_schema, type: json_schema?, required: false, description: "Schema of Relationship.metadata"}
```

### 2.6 Slot catalogue (closed)

Each slot has a contract. Every implementation receives a `SlotInput` with the fields listed
and returns the output shown. Pure slots MUST NOT write or emit (SPEC-17 enforces this with
a read-only syscall profile).

**Property-level slots** (bound on value types or property definitions):

| Slot | Input | Output | Default mode | Pure | When | Replaces (LS) |
|---|---|---|---|---|---|---|
| `default` | ctx, property | value | REPLACE | yes | create, when key absent | DefaultValueSlot |
| `transform` | value, prev_value, ctx | value | CHAIN | yes | staging, before validation | TransformInputSlot |
| `validate` | value, entity (NEW), ctx | Decision | CHAIN | yes | validation (SPEC-18) | ValidationSlot |
| `schema` | property | JSON Schema fragment | REPLACE | yes | schema derivation (SPEC-18) | SchemaProvideSlot |
| `compute` | entity (effective), ctx | value | REPLACE | yes | on write (stored) or read (virtual), per config `on: WRITE\|READ` | ComputeFieldSlot, AggregationSlot |
| `mask` | value, ctx (reader) | value | REPLACE | yes | read, after authorisation (SPEC-22) | FieldMaskSlot |
| `format` | value, ctx (locale) | string | REPLACE | yes | summaries, lists, AI prompts | SummaryRenderSlot |
| `index` | value | list of index values | REPLACE | yes | flush (REQ-STO-007) | (StorageSlot indexing part) |
| `ui` | mode, config | widget ref + config | REPLACE | yes | render (SPEC-25) | UISlot |
| `access` | ctx, entity | {read: bool, write: bool} | CHAIN (all must allow) | yes | read masking, write check (SPEC-22) | PermissionSlot |
| `navigate` | value, ctx | uri | REPLACE | yes | UI links | NavigationSlot |

**Type-level slots** (bound on entity types; all run inside the process):

| Slot | Input | Output | Default mode | Pure | When | Replaces (LS) |
|---|---|---|---|---|---|---|
| `summary` | entity, ctx | string | REPLACE | yes | display name, lists | SummaryRenderSlot (type) |
| `before_create` | NEW, ctx | Decision, staged changes, emits | CHAIN | no | staging, before validation | BeforeCreateSlot |
| `before_update` | PREV, NEW, ctx | Decision, staged changes, emits | CHAIN | no | staging, before validation | BeforeUpdateSlot |
| `before_delete` | PREV, ctx | Decision, staged changes, emits | CHAIN | no | staging | BeforeDeleteSlot |
| `dependency_check` | PREV, ctx | Decision | CHAIN | yes | before delete/archive | DependencyCheckSlot |
| `cascade` | PREV, NEW, ctx | staged changes, emits | CHAIN | no | after validation, before flush | CascadeUpdateSlot |
| `quota` | ctx, NEW | Decision | CHAIN | yes | create | QuotaSlot |
| `after_commit` | commit info, ctx | emits | CHAIN | no (emits only) | after flush, from the outbox (SPEC-20) | AfterCreate/AfterUpdateSlot |

- **Dropped:** LS `StorageSlot`. Storage is always the JSON snapshot, and the representation follows the primitive (§2.7).
- **Rule:** adding a slot to the catalogue requires an ADR.

### 2.7 Primitives

| Primitive type | `primitive` | JSON representation | Index column | Default widget (edit / view) |
|---|---|---|---|---|
| `Text` | TEXT | string | `val_text` | TextInput / TextLabel |
| `Number` | NUMBER | string decimal (scale from constraints) | `val_num` | NumberInput / NumberLabel |
| `Integer` | INTEGER | number (≤ 2^53) or string (larger) | `val_num` | NumberInput / NumberLabel |
| `Boolean` | BOOLEAN | `true`/`false` | `val_bool` | Switch / BoolLabel |
| `Date` | DATE | `YYYY-MM-DD` | `val_date` | DatePicker / DateLabel |
| `Timestamp` | TIMESTAMP | RFC 3339 UTC | `val_date` | DateTimePicker / DateTimeLabel |
| `Map` | MAP | object | nested paths only | JsonEditor / JsonView |
| `List` | LIST | array (untyped; prefer `many: true`) | per element | JsonEditor / JsonView |
| `Reference` | REFERENCE | canonical URI string | `val_ref` (+ `val_text` URI) | RefPicker / RefLink |
| `Json` | JSON | any | none | JsonEditor / JsonView |

### 2.8 Other meta entities

```yaml
ENT-Dictionary:
  purpose: Reference list of allowed values (UODS dict_code, CON-UC-017).
  origin: [VRD-02-02, CON-UC-018]
  fields:
    - {name: entries, type: "list<DictionaryEntry{value: json, title: string, i18n?: map, deprecated?: bool, order?: int32}>", required: true, description: "Allowed values"}
    - {name: open, type: bool?, required: false, description: "If true, other values are warnings, not errors"}
```

```yaml
ENT-TypeExtension:
  purpose: Adds namespaced properties and slot/ui/rule additions to a type the author does not own (Q-006, VRD-02-07).
  origin: [VRD-02-07, VRD-18-02, NEW]
  fields:
    - {name: target, type: "uri<Type>", required: true, description: "Extended entity type"}
    - {name: properties, type: "list<PropertyDefinition>", required: false, description: "New properties; names MUST carry the extension's namespace (e.g. acme.score)"}
    - {name: slots, type: "map<slot_name, SlotBinding>?", required: false, description: "Type-level slot bindings, CHAIN mode only"}
    - {name: ui_modes, type: "map<mode, UiModeSpec>?", required: false, description: "Presentation overrides"}
    - {name: invariants, type: "list<InvariantSpec>?", required: false, description: "Additional invariants"}
    - {name: rule_refs, type: "list<uri>?", required: false, description: "Additional constraints"}
  invariants:
    - Applies to the target and its subtypes, in the tenant of the extension (and in all tenants when owned by logrums).
    - May not redefine or remove properties of the target.
```
