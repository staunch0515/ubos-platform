---
id: SPEC-13-3
title: "Meta-Model — Interfaces and non-functional"
status: complete
phase: P4
depends_on: [SPEC-13, SPEC-02, SPEC-11, SPEC-12]
sources: [UP, LC, UB, UC, US, LS]
---

# Meta-Model — Interfaces and non-functional

Part 3 of SPEC-13 (`13-meta-model/`). Header, concepts and file list: `00-index.md`.

---

## 4. Interfaces

### API-META-001 — TypeRegistry
- **Kind:** rust trait (`ubos_kernel::semantic`)
- **Signature / shape:**
```rust
#[async_trait]
pub trait TypeRegistry: Send + Sync {
    async fn effective_type(&self, x: &ExecutionContext, type_uri: &str) -> Result<Arc<EffectiveType>, UbosError>;
    async fn type_chain(&self, x: &ExecutionContext, type_uri: &str) -> Result<Vec<TypeRef>, UbosError>;
    async fn is_subtype(&self, x: &ExecutionContext, sub: &str, sup: &str) -> Result<bool, UbosError>;
    async fn validate_definition(&self, x: &ExecutionContext, meta_type: &str, own: &Value) -> Result<(), UbosError>;
    async fn subtypes(&self, x: &ExecutionContext, type_uri: &str, transitive: bool) -> Result<Vec<TypeRef>, UbosError>;
}
pub struct EffectiveType {
    pub type_ref: TypeRef, pub kind: TypeKind, pub primitive: Option<Primitive>,
    pub chain: Vec<TypeRef>, pub fingerprint: [u8; 32],
    pub properties: IndexMap<String, EffectiveProperty>,
    pub type_slots: HashMap<SlotName, Vec<ResolvedBinding>>,
    pub ui_modes: HashMap<Mode, UiModeSpec>, pub display: DisplaySpec, pub open: bool,
    pub invariants: Vec<InvariantSpec>, pub rule_refs: Vec<String>,
    pub lifecycle: Option<String>, pub default_views: HashMap<Mode, String>,
}
```
- **Semantics:** Implements REQ-META-010/011/040. `subtypes` uses the inheritance index (UB design, CON-UB-042): a query of Type heads whose `_extends` contains the URI, which is recursive when `transitive`.
- **Origin:** CON-UB-002 (SchemaComposer), CON-LS-023

### API-META-002 — Resolver of effective data
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait EntityResolver: Send + Sync {
    async fn effective(&self, x: &ExecutionContext, uri: &str, opts: ResolveOpts) -> Result<EffectiveEntity, UbosError>;
    async fn effective_many(&self, x: &ExecutionContext, uris: &[String], opts: ResolveOpts) -> Vec<Result<EffectiveEntity, UbosError>>;
}
pub struct ResolveOpts { pub expand: ExpandSpec /* depth 0..=3 or paths */, pub mask: bool, pub view: Option<View>, pub ref_form: RefForm }
pub struct EffectiveEntity { pub data: Value /* with _meta */, pub own: Value, pub address: ResolvedAddress }
```
- **Semantics:** Implements REQ-META-020. It is used by syscalls (`entity.get`), queries and the UI render pipeline.
- **Origin:** CON-UB-028 (EVALUATE_ENTITY), CON-US-012

### API-META-003 — Own-data diff for edits
- **Kind:** rust function
- **Signature / shape:**
```rust
pub fn own_data_for(desired_effective: &Value, inherited: &Value, current_own: &Value,
                    t: &EffectiveType, pin_keys: &[String]) -> Value;
```
- **Semantics:** Implements REQ-META-022. It is pure and deterministic.
- **Origin:** CON-UB-015

### API-META-004 — Protocol queries
- **Kind:** UBTP named queries (SPEC-24)
- **Signature / shape:**

| URI | Args | Result |
|---|---|---|
| `ubos://system/Query/meta.type@v1` | `{type: uri, effective?: bool}` | own or effective definition with `_meta.chain` |
| `ubos://system/Query/meta.schema@v1` | `{type?: uri}` | meta-schema, or the derived instance schema of a type (SPEC-18) |
| `ubos://system/Query/meta.subtypes@v1` | `{type: uri, transitive?: bool}` | list of type URIs |
| `ubos://system/Query/meta.slots@v1` | – | slot catalogue with contracts (§2.6) |
| `ubos://system/Query/meta.explain@v1` | `{uri, key?}` | for each effective key: its source layer (default, prototype URI, own) and its type-chain origin |
- **Origin:** CON-US-014, CON-UB-053 (inheritance-aware studio), VRD-16-05 (RAG over the meta-model)

### Error codes (META)

| Code | Category | Meaning |
|---|---|---|
| `META.DEFINITION_INVALID` | INVALID | meta-schema violation (causes with paths) |
| `META.TYPE_NOT_INSTANTIABLE` | INVALID | unknown, abstract or VALUE type used for an entity |
| `META.INHERITANCE_CYCLE` | INVALID | cycle in `_extends` |
| `META.INHERITANCE_CONFLICT` | INVALID | no C3 linearisation |
| `META.INHERITANCE_TOO_DEEP` | LIMIT | chain too deep |
| `META.WIDENING` | INVALID | inherited definition widened |
| `META.UNKNOWN_PROPERTY` | INVALID | undeclared key in a non-open type |
| `META.RESERVED_KEY` | INVALID | reserved key written |
| `META.PROTOTYPE_TYPE` | INVALID | prototype is not of the type or a supertype |
| `META.REFERENCE_INVALID` | INVALID | reference target missing or of wrong type |
| `META.RELATIONSHIP_INVALID` | INVALID | relationship rule violated |
| `META.SLOT_CONTRACT` | INTERNAL | slot implementation returned a wrong shape |
| `META.LIMIT` | LIMIT | a limit of REQ-META-017 exceeded |
| `META.INSTANCES_AFFECTED` | (warning) | type change affects existing instances |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-030 | Effective type definition, warm cache | p95 ≤ 50 µs |
| NFR-PERF-031 | Effective type definition, cold (chain of 6, 30 properties) | p95 ≤ 5 ms |
| NFR-PERF-032 | Effective data of an instance with 2 prototypes, warm type cache | p95 ≤ 1 ms + reads |
| NFR-AIR-010 | Definitions readable by AI | Meta-schema published; `description` recommended on every type and property; `meta.explain` available |
