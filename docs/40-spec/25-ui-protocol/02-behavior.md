---
id: SPEC-25-2
title: "UI Protocol — Behavior"
status: complete
phase: P4
depends_on: [SPEC-25, SPEC-13, SPEC-18, SPEC-19, SPEC-21, SPEC-22, SPEC-24]
sources: [UP, FU, UB, UC, US, LS, UW]
---

# UI Protocol — Behavior

Part 2 of SPEC-25 (`25-ui-protocol/`). Header, concepts and file list: `00-index.md`.

---

## 3. Behavior

### 3.1 Widget resolution

### REQ-UI-001 — Modes
- **Statement:** The kernel and clients MUST use exactly the modes of §2.1. `cell` replaces the corpus's `list` and `read` mode names, which are accepted as input aliases by the TS SDK only and normalised there.
- **Origin:** VRD-13-01, CON-US-015, CON-UC-055
- **Acceptance:** The render request `mode: "list"` from an old client becomes `cell`, and the server never returns non-normalised modes.
- **Priority:** P0

### REQ-UI-002 — Widget resolution algorithm
- **Statement:** For a property *p* of an effective entity type, in mode *m*, on platform *c*, the widget MUST be the first match of:
  1) the view's FieldSpec `widget` for *p* (when rendering a View);
  2) *p*'s own `ui_modes[m]`, or its `ui` slot override (SPEC-13);
  3) the `ui_modes[m]` of *p*'s value type, walking the value type's chain (nearest first);
  4) UiMatrix entries for (a type in *p*'s value type chain, *m*): tenant matrices by priority, then root matrices;
  5) the primitive fallback of §2.7 of SPEC-13 for *m*;
  6) `JsonView` (read-only modes) or `JsonEditor` (edit modes).

  For entity-level rendering (the whole object), the same order applies with the entity type's chain. The base fallback is `Form` for edit/create, `Detail` for view, `EntityCard` for card and `TextCell` (title) for cell.

  If the chosen widget does not list *c* in `platforms`, the server follows its `fallback` chain, then restarts at step 5. Config objects merge from general to specific: widget defaults < type `ui_modes` config < matrix config < property config < FieldSpec config.
- **Rationale:** UC's lens system and US's lookup order, turned into one deterministic function. Changing `Money`'s widget changes every money field (UB, UC).
- **Origin:** VRD-13-01, CON-UC-055, CON-US-052, CON-US-058, CON-UB-057
- **Acceptance:**
  1) A new value type `acme.Iban` extending `Iban` renders with Iban's widget with no configuration.
  2) On mobile, a web-only `MarkdownEditor` falls back to `TextArea`.
- **Priority:** P0

### REQ-UI-003 — Resolution caching
- **Statement:** Resolved widget trees MUST be cached by (type fingerprint, view commit, UiMatrix heads fingerprint, mode, platform). Data-independent parts of payloads are reused across requests. Caches are keyed by immutable versions (REQ-META-040) and are never invalidated by time.
- **Origin:** CON-LS-044, VRD-13-02
- **Acceptance:** Rendering 1 000 invoices in `cell` mode resolves widgets once.
- **Priority:** P1

### 3.2 Views and derived layouts

### REQ-UI-010 — View selection
- **Statement:** A render request `{target, mode, view?, platform, params?}` MUST select the View as follows:
  1) the explicit `view` URI;
  2) else the target type's `default_views[mode]`, walking the type chain;
  3) else a **derived** layout (REQ-UI-011).

  Then the first `variants` entry whose `when` matches the platform and the principal's roles is applied (deep merge of `override`). Views are resolved in the context's tenant and branch like any entity. Tenant patches of root Views are allowed with overlay policy FULL (REQ-TEN-016).
- **Origin:** VRD-13-03, CON-UB-050, CON-US-054
- **Acceptance:** A tenant patch of the root `Person` detail view changes it only for that tenant. A `MOBILE` variant hides the side panel on phones.
- **Priority:** P0

### REQ-UI-011 — Derived layouts
- **Statement:** Without a View, the kernel MUST derive one:
  - **FORM/DETAIL:** effective properties in definition order, excluding `deprecated`, reserved keys and hidden paths. Properties are grouped into Sections by their `ui.group` config, if any, and inherited properties are grouped by the ancestor that declared them.
  - **TABLE/LIST:** the title, `_state` (if a lifecycle exists), up to 6 properties that are searchable and single-valued, and `$modified_at`.
  - **CARD:** the title, icon, summary slot output and `_state`.

  Derived layouts are deterministic, so the same type fingerprint always gives the same layout.
- **Rationale:** A new type is usable at once (US: "UI is the skin grown on the type's properties").
- **Origin:** VRD-13-02, CON-US-016, CON-UC-054, CON-LS-054
- **Acceptance:** Creating `Type/acme.Asset` with 8 properties immediately yields a form, a table and a card in the cockpit.
- **Priority:** P0

### 3.3 Render pipeline

### REQ-UI-012 — Render pipeline
- **Statement:** `ui.render` MUST run in a READ_ONLY process and perform these stages:
  1) **Resolve:** select the View (REQ-UI-010) and resolve the component tree and widgets (REQ-UI-002).
  2) **Hydrate:**
     - for an entity target, load the effective data (REQ-META-020) with the expansion the view needs;
     - for collections, run the data source query (SPEC-21) with the params and paging;
     - load option lists for `Select`/`MultiSelect` from Dictionaries and for `RefPicker` from the first page of the target type (≤ 20);
     - load the available actions (REQ-FLOW-010) and the lifecycle state.
  3) **Transform:**
     - apply field access (hide, read-only), masking and lifecycle `editable` (non-editable → READ_ONLY);
     - drop components whose `visible_when` is false;
     - remove action bindings the principal may not execute (REQ-UI-020);
     - render labels and messages in the locale, and format values with the `format` slot for `view`/`cell`/`card`.
  4) **Emit** the RenderPayload with `base_commit` set.

  The pipeline is also used by the action PREPARE stage (REQ-FLOW-012).
- **Origin:** VRD-13-02, CON-UB-050, CON-US-022
- **Acceptance:** Rendering an invoice for a clerk hides `margin`, masks `iban`, makes a POSTED invoice read-only, and omits the `approve` button.
- **Priority:** P0

### REQ-UI-013 — Client-side validation hints
- **Statement:** For edit and create modes, the payload MUST include `schema`: the derived JSON Schema at the level that will apply to the save (DRAFT on draft branches, PUBLISH otherwise), restricted to the rendered paths. Clients SHOULD validate with it before submitting and SHOULD call `rule.validate` (REQ-RULE-050) for server rules on blur. Server validation remains authoritative, and its errors come back with paths that clients map to fields.
- **Origin:** VRD-09-01, CON-US-053, REQ-CONV-086
- **Acceptance:** An invalid email is marked in the form before submit, and an invariant failure returned by the server highlights `/balance`.
- **Priority:** P1

### 3.4 Actions

### REQ-UI-020 — Action bindings
- **Statement:**
  - Every EMIT step in a binding MUST reference a platform Action, Pipeline or Query URI (VRD-13-05).
  - The server MUST remove a binding when the principal may not execute its first EMIT step on the target. Bindings whose EMIT steps come later are kept, but the steps are marked `requires_check: true`.
  - Clients execute chains serially. EMIT steps go through UBTP (SPEC-24), and a failure stops the chain and shows the error.
  - Client-only steps (INTENT, CONFIRM, OPEN, SET, REFRESH, CLOSE) never change server data.
- **Origin:** VRD-13-05, CON-UB-051, CON-UP-055, CON-US-055
- **Acceptance:** A "Save and close" binding (EMIT entity.edit EXECUTE → CLOSE) closes only after a successful save.
- **Priority:** P0

### REQ-UI-021 — Board and drag transitions
- **Statement:** For BOARD views grouped by `_state`, the payload MUST include, per lane pair, the TransitionAction that moves an entity between those states (derived from the Lifecycle, REQ-FLOW-016). A DROP binding emits that action with the dragged entity as target and `base_commit`. Illegal drops are not offered.
- **Origin:** CON-UW-022, CON-UB-032, VRD-06-03
- **Acceptance:** Dragging an issue from IN_PROGRESS to DONE runs `issue.close@v1`. Dragging it to an unreachable lane is refused by the client before any request is sent.
- **Priority:** P1

### 3.5 Projection

### REQ-UI-030 — Project and merge
- **Statement:**
  - `ui.project {uri, as_type, mode, include?, exclude?, overrides?}` MUST check that `as_type` is in the entity's type chain (`UI.NOT_ANCESTOR` otherwise). It then renders the entity restricted to `as_type`'s effective properties, with the include/exclude filters and per-field overrides (widget, label, read_only).
  - Saving a projection (`entity.project@v1` EXECUTE, REQ-FLOW-017) MUST apply `set_effective` only to the projected paths, with `base_commit`, and leave other properties untouched.
  - Projecting to `Entity` gives the raw JSON editor over own data (`entity.edit_raw`).
- **Rationale:** US's design: the type stays the single source of truth, and many lenses exist without copying data.
- **Origin:** VRD-13-04, CON-US-026, CON-US-016
- **Acceptance:** Editing a `VipCustomer` projected as `Party` changes only Party properties. A concurrent change to a VIP-only property is preserved.
- **Priority:** P1

### 3.6 Pushed UI

### REQ-UI-040 — Pushed instructions
- **Statement:** `ui.push(target, instruction)` (SPEC-17) MUST validate the instruction against ENT-PushedUi and queue it as an emit (REQ-FLOW-040). Delivery rules are in REQ-PROTO-012.
  - Instruction payloads are masked for the receiving principal.
  - RENDER and DIALOG payloads are produced by the render pipeline at dispatch time, under the **recipient's** permissions.
  - The web cockpit shows TOAST as transient toasts, CARD in the passive zone feed, DIALOG as a modal, and RENDER in the active zone (SPEC-26).
- **Origin:** VRD-13-06, CON-LS-051, CON-UC-056
- **Acceptance:** A long pipeline pushes a PROGRESS card and a final SUCCESS toast to the caller's cockpit.
- **Priority:** P1

### 3.7 Studio support

### REQ-UI-050 — Inheritance and source hints
- **Statement:** When the requested mode is edit and the request asks for `hints: true` (the studio), the payload MUST include:
  - `meta.inherited_keys` and `meta.own_keys`;
  - `meta.sources`: per path, `{layer: DEFAULT|PROTOTYPE|OWN, from: uri}`;
  - for each inherited path, the value each ancestor provides (the X-ray view of CON-UB-053, via `meta.explain`).

  The `InheritanceForm` widget uses them to show inherited and overridden markers and a "reset to inherited" action per field. That action is an EMIT of `entity.edit` that removes the key from own data (REQ-META-022).
- **Origin:** CON-UB-053, VRD-14-02, REQ-META-020
- **Acceptance:** A page derived from a template shows 5 inherited fields with markers. Resetting one removes it from own data in a new version.
- **Priority:** P1

### 3.8 Clients

### REQ-UI-060 — Client widget registry contract
- **Statement:** Every rendering client MUST:
  - implement the standard catalogue (§2.2) for its platform;
  - keep a registry keyed by widget URI, with lookup by URI first and then by the last path segment (US);
  - render an unregistered widget with its fallback chain, as delivered by the server.

  Tenant-defined widgets without a client implementation are always resolved to fallbacks by the server (REQ-UI-002). Loading remote widget code is deferred (§8).
- **Origin:** CON-US-051, VRD-13-01, VRD-14-04
- **Acceptance:** A payload that contains a CUSTOM tenant widget unknown to the mobile app renders via its fallback without errors.
- **Priority:** P0

### REQ-UI-061 — Accessibility and i18n
- **Statement:** The standard widgets in the web shell MUST meet WCAG 2.1 AA (labels, keyboard navigation, contrast, focus order). All labels, titles and messages in payloads are rendered in the effective locale (REQ-CTX-014), using i18n keys where present (SPEC-27 i18n). Right-to-left locales set `meta.direction: rtl`.
- **Origin:** NEW: product quality; VRD-16-07
- **Acceptance:** An accessibility audit of the derived form shows no AA violations, and switching the locale changes the labels without a reload.
- **Priority:** P1

### REQ-UI-070 — Realtime delta editing (later version)
- **Statement:** A later version MAY add `ui.delta {uri, base_commit, ops: [{path, value}]}` for autosave-style editing on draft branches. Each delta batch is a commit (REQ-VER-040), with conflicts reported per path. Version 1 does not implement this. Clients autosave with ordinary drafts.
- **Origin:** VRD-13-07, CON-UB-052
- **Acceptance:** – (deferred)
- **Priority:** P2
