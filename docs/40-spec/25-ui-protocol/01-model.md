---
id: SPEC-25-1
title: "UI Protocol — Model"
status: complete
phase: P4
depends_on: [SPEC-25, SPEC-13, SPEC-18, SPEC-19, SPEC-21, SPEC-22, SPEC-24]
sources: [UP, FU, UB, UC, US, LS, UW]
---

# UI Protocol — Model

Part 1 of SPEC-25 (`25-ui-protocol/`). Header, concepts and file list: `00-index.md`.

---

## 2. Model

### 2.1 Modes

| Mode | Purpose | Read-only | Typical widgets |
|---|---|---|---|
| `view` | show one value or entity | yes | TextLabel, MoneyLabel, ProfileCard, CodeBlock |
| `edit` | change an existing value or entity | no | TextInput, MoneyInput, Form, ScriptEditor |
| `create` | enter a new entity (defaults, required markers) | no | Form |
| `cell` | compact rendering in lists, tables and boards (US `list`) | yes | TextCell, Badge, Avatar, StateChip |
| `card` | summary tile in feeds, docks and search results | yes | EntityCard |
| `filter` | input for query conditions | no | RangeFilter, MultiSelectFilter |
| `execute` | run a Logic, Action or Pipeline (params + output console) | no | ActionAutomator, ScriptConsole |

`mask` is not a mode. Masking is applied by the `mask` slot in every mode (REQ-SEC-021).

```yaml
ENT-Widget:
  purpose: A UI component entity addressed by URI (TERM-Widget); clients hold implementations keyed by this URI.
  origin: [VRD-13-01, CON-US-051, CON-LS-053, CON-UP-055]
  fields:
    - {name: title, type: string, required: true, description: "Name"}
    - {name: kind, type: "enum{FIELD|OBJECT|COLLECTION|LAYOUT|CHART|FEED|CUSTOM}", required: true, description: "What it renders"}
    - {name: modes, type: "list<Mode>", required: true, description: "Modes it supports"}
    - {name: accepts, type: "list<uri<Type>>?", required: false, description: "FIELD - value types (and descendants) it can render; OBJECT/COLLECTION - entity types"}
    - {name: props_schema, type: json_schema?, required: false, description: "Configuration accepted in UiModeSpec.config / component props"}
    - {name: platforms, type: "list<enum{WEB|DESKTOP|MOBILE}>", required: true, description: "Clients that implement it"}
    - {name: fallback, type: "uri<Widget>?", required: false, description: "Widget to use where not implemented"}
    - {name: description, type: text?, required: false, description: "For builders and AI"}
  invariants:
    - Root widgets (logrums) form the standard catalogue (§2.2); tenants may add widgets but only clients that implement them render them (others use fallback).
```

### 2.2 Standard widget catalogue (root package)

| Kind | Widgets |
|---|---|
| FIELD | TextInput, TextArea, TextLabel, TextCell, NumberInput, NumberLabel, MoneyInput, MoneyLabel, PercentInput, DatePicker, DateTimePicker, DateLabel, Switch, BoolLabel, Select, MultiSelect, RefPicker, RefLink, TagInput, EmailInput, PhoneInput, UrlInput, ColorPicker, FileUpload, FileLink, ImageView, MarkdownEditor, MarkdownView, JsonEditor, JsonView, ScriptEditor (Monaco + Rhai), CodeBlock, PasswordInput, MaskedLabel, Badge, StateChip, Avatar |
| OBJECT | Form, Detail, EntityCard, ProfileCard, InheritanceForm (studio: inherited vs overridden markers, CON-UB-053) |
| COLLECTION | Table, List, Board (lanes by a property, e.g. `_state`), Calendar, Tree, Gallery, Timeline (history) |
| LAYOUT | Stack, Grid, Tabs, Section, Split, Drawer, Dialog |
| CHART | Kpi, LineChart, BarChart, PieChart |
| FEED | CardFeed (the passive zone, SPEC-26), NotificationList |
| CUSTOM | ActionAutomator, ScriptConsole, DiffView, ConflictResolver, BranchStatus |

```yaml
ENT-UiMatrix:
  purpose: Tenant/app-level overrides of widget choice by (type, mode) (UC ui_matrix).
  origin: [VRD-13-01, CON-UC-055]
  fields:
    - {name: priority, type: int32, required: true, description: "Higher wins among matrices of the same tenant"}
    - {name: entries, type: "list<{type: uri<Type>, mode: Mode, widget: uri<Widget>, config?: json, platforms?: list}>", required: true, description: "Overrides"}
```

```yaml
ENT-View:
  purpose: A versioned screen definition (TERM-View).
  origin: [VRD-13-03, CON-UB-050, CON-US-054, CON-UP-054]
  fields:
    - {name: title, type: string, required: true, description: "Title (i18n key allowed)"}
    - {name: layout_type, type: "enum{FORM|DETAIL|TABLE|LIST|BOARD|CALENDAR|TREE|DASHBOARD|CUSTOM}", required: true, description: "Layout"}
    - {name: target_type, type: "uri<Type>?", required: false, description: "Entity type shown (FORM/DETAIL/TABLE/LIST/BOARD/CALENDAR/TREE)"}
    - {name: mode, type: Mode?, required: false, description: "Default mode (FORM: edit/create, DETAIL: view)"}
    - {name: source, type: DataSource?, required: false, description: "{criteria (CriteriaQuery template with ${params}) | query: uri<Query> , args?} for collections and dashboards"}
    - {name: fields, type: "list<FieldSpec>?", required: false, description: "FORM/DETAIL/TABLE - {path, widget?, config?, label?, width?, sortable?, read_only?, visible_when?}; default derived from type (REQ-UI-011)"}
    - {name: group_by, type: string?, required: false, description: "BOARD lanes property (e.g. _state); CALENDAR date path"}
    - {name: components, type: "list<Component>?", required: false, description: "DASHBOARD/CUSTOM - component tree (§2.3)"}
    - {name: actions, type: "list<ActionBinding>?", required: false, description: "Toolbar/row/bulk actions (§2.4)"}
    - {name: filters, type: "list<{path, widget?}>?", required: false, description: "Filter bar (filter mode)"}
    - {name: params_schema, type: json_schema?, required: false, description: "View parameters (e.g. customer)"}
    - {name: variants, type: "list<{when: {platform?: list, roles?: list}, override: json}>?", required: false, description: "Device/role variants (UB transform stage)"}
    - {name: page_size, type: int32?, required: false, description: "Collections; default 50"}
```

### 2.3 Component tree

```
Component := { id: string,
               widget: uri<Widget>,
               props?: json,                        // validated by Widget.props_schema
               bind?: string,                       // data path (e.g. "customer.name") or "$source" for collections
               visible_when?: rhai,                 // rule subset over data, principal, params
               actions?: [ActionBinding],
               children?: [Component] }
```

### 2.4 Action binding and client chains

```
ActionBinding := { id: string, title?: string, icon?: string, style?: PRIMARY|DEFAULT|DANGER,
                   placement?: TOOLBAR|ROW|BULK|MENU|DOCK,
                   on: CLICK|SUBMIT|CHANGE|DROP|SELECT,
                   chain: [ChainStep] }
ChainStep     := { kind: EMIT,     action: uri, stage?: DESCRIBE|PREPARE|EXECUTE, target?: expr, args?: map<expr> }
               | { kind: INTENT,   intent: Intent }                        // navigate / open (SPEC-19 §2.6)
               | { kind: CONFIRM,  text: string }
               | { kind: OPEN,     view: uri, as: DRAWER|DIALOG|TAB, params?: map<expr> }
               | { kind: SET,      key: string, value: expr }              // client-local state
               | { kind: REFRESH,  scope?: VIEW|COMPONENT }
               | { kind: CLOSE }
expr          := "@formData" | "@row" | "@selection" | "@params.x" | "${…}" template | literal
```

```yaml
ENT-RenderPayload:
  purpose: Server output of the render pipeline (TERM-RenderPayload).
  origin: [VRD-13-02, CON-UB-050, CON-US-022]
  fields:
    - {name: ui, type: Component, required: true, description: "Resolved tree; every widget is a canonical Widget URI with merged config"}
    - {name: data, type: json, required: true, description: "Effective, masked data (entity) or QueryResult (collections)"}
    - {name: schema, type: json_schema?, required: false, description: "Derived schema for client-side validation at the target level (REQ-RULE-001)"}
    - {name: meta, type: RenderMeta, required: true, description: "{uri, commit_id, base_commit, branch, type, type_fingerprint, view (uri+commit), mode, platform, locale, hidden_paths, read_only_paths, inherited_keys?, sources?}"}
    - {name: actions, type: "list<ActionBinding>", required: true, description: "Allowed bindings only (REQ-UI-020)"}
    - {name: ui_policy, type: "enum{EDIT|READ_ONLY}", required: true, description: "Effective policy"}
    - {name: options, type: "map<path, list<{value, title}>>?", required: false, description: "Hydrated option lists (dictionaries, first page of reference candidates)"}
    - {name: errors, type: "list<{path, code, message}>?", required: false, description: "Validation errors when re-rendering after a failed submit"}
    - {name: checklist, type: "list<ChecklistItem>?", required: false, description: "Guards (REQ-RULE-040)"}
```

```yaml
ENT-PushedUi:
  purpose: A UI instruction pushed by logic (TERM-PushedUi), delivered as Op{UI} or to sessions (REQ-PROTO-012).
  origin: [VRD-13-06, CON-LS-051, CON-UC-056]
  fields:
    - {name: instruction, type: "enum{TOAST|CARD|DIALOG|RENDER|NAVIGATE|PROGRESS|REFRESH}", required: true, description: "Kind"}
    - {name: level, type: "enum{INFO|SUCCESS|WARNING|ERROR}?", required: false, description: "TOAST/CARD"}
    - {name: text, type: string?, required: false, description: "TOAST text"}
    - {name: card_type, type: string?, required: false, description: "CARD - widget URI or card kind (UC card_type)"}
    - {name: data, type: json?, required: false, description: "CARD data"}
    - {name: payload, type: RenderPayload?, required: false, description: "DIALOG/RENDER"}
    - {name: intent, type: Intent?, required: false, description: "NAVIGATE"}
    - {name: progress, type: "{percent, message}?", required: false, description: "PROGRESS"}
    - {name: uri, type: uri?, required: false, description: "REFRESH - entity whose views should reload"}
```
