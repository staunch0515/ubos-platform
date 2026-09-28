---
id: CMP-13
title: "Comparison: Metadata-driven UI"
status: complete
phase: P2
depends_on: [CMP-02, CMP-06]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Comparison: Metadata-driven UI

## 1. Question

How is UI derived from metadata, so that new types and actions need no front-end code? How are layouts, widgets, modes, actions and server-driven updates expressed?

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | SDUI JSON tree `{type, props, children}`; component registry; button → logic slug; apps = VIEW entities | CON-UP-054, 055, HL-UP-009 | Working minimal SDUI |
| FU | Hand-built Studio (not SDUI) | CON-FU-050 | Tooling UI |
| UB | Render pipeline Resolution → Hydration → ACL Transformation; versioned layouts (A/B by branch); action chains; smart forms with realtime deltas; types drive widgets/masking | CON-UB-050…057, HL-UB-010, 011 | Most complete SDUI design |
| UC | Schema-to-form from `input_schema`; `ui_schema.widget`; lens system `component = f(type chain, mode)` with UI matrix entity (design); push cards | CON-UC-054, 055, 056, HL-UC-006 | Type-driven lens |
| US | `ui_modes {edit, view, list, execute}` on types; universal renderer with fallback; widgets as `ubos://…/Widget/*`; recursive schema renderer; view engine by layout type; slice projection (design); FSM wizard | CON-US-015, 050…055, 026, HL-US-004, 005 | Implemented lens |
| LS | SchemaProvide slot per relation → fields; UI slot behaviours; `RenderForm` pushed | CON-LS-051…053 | UI from slots |
| UW | Hand-built | – | – |

## 3. Dimension matrix

| Dimension | UP | UB | UC | US | LS |
|---|---|---|---|---|---|
| Layout as versioned entity | ● VIEW | ● | View entities | ● View | – |
| Widget chosen by type + mode | – | ● | ● lens | ● ui_modes | slot |
| Inheritance fallback of widgets | – | – | ● | ● | – |
| Field-level ACL / masking in render | – | ● | – | – | mask slot |
| Action binding | logic slug | ● chains | invoke | FSM events | emit |
| Server-pushed UI | – | – | cards | – | ● RenderForm |
| Projection (view entity as ancestor type) | – | – | – | ● design | – |
| Widgets addressable/versioned | – | – | – | ● URI | component names |
| Realtime delta editing | – | ● design | – | – | – |

## 4. Analysis

- **Complementary layers.** Every component of a full solution exists somewhere in the corpus:
  - type → widget per mode with inheritance fallback (UC lens design, US implementation);
  - layouts as versioned entities (UP, UB);
  - a server render pipeline with ACL trimming (UB);
  - action chains (UB) and FSM events (US);
  - projection onto ancestor types (US);
  - slot-based schema contribution (LS).
- **The render payload.** UB's three-part payload (`schema`, `data`, `meta` with commit id) is the clearest contract. It supports optimistic editing (base commit) and pairs with US's `{ui_schema, data, available_events, ui_policy}` transition response.
- **Widgets as addressable resources.** US's `ubos://…/Widget/*` makes the widget catalogue data and allows versioning and remote loading later.

## 5. Verdicts

### VRD-13-01 — Widget resolution: component = f(type chain, mode) (fusion UC lens + US ui_modes)
- **Decision:** fusion.
- **Consequences:**
  - Modes: `edit`, `view`, `list` (cell), `card`, `execute`, `mask`.
  - Lookup order:
    1. property-level override;
    2. the property type's `ui_modes[mode]`, walking its `_extends` chain;
    3. the UI matrix entity (tenant/app overrides);
    4. primitive fallback (map → JSON editor, text → text input);
    5. base entity fallback.
  - Widgets are `Widget` entities addressed by URI and registered in clients.

### VRD-13-02 — Server render pipeline and payload (fusion UB + US)
- **Decision:** fusion.
- **Consequences:**
  - Pipeline: resolve (view/layout or derived-from-type) → hydrate (data, options) → transform (policy: hide/readonly/mask fields and actions; device/role variants).
  - Payload: `{ui_schema, data, meta: {uri, commit, branch}, actions | available_events, ui_policy, errors[path]}`.

### VRD-13-03 — Views and layouts as versioned entities (fusion UP + UB)
- **Decision:** fusion.
- **Consequences:**
  - `View` entities have `layout_type` (dashboard, list, form, board, detail, custom tree) and `config`, or a recursive component tree.
  - A/B variants and rollback use branches (VRD-08).
  - Apps are bundles of views, actions and types (CMP-17).

### VRD-13-04 — Projection: edit an entity through any ancestor type (best-of US design)
- **Decision:** best-of US design.
- **Consequences:**
  - `project(entity, view_type)` returns data + schema restricted to the view type's properties.
  - `merge(entity, view_type, edited)` writes back only those properties.
  - The generic "Entity" view is the raw JSON editor.
  - Include/exclude/override options follow CON-US-026.

### VRD-13-05 — Actions in the UI are platform actions (fusion UB chains + US FSM + UP binding)
- **Decision:** fusion.
- **Consequences:**
  - Buttons and events reference Action URIs or FSM events. Client-only steps (navigate, set state, open drawer) form a small action chain vocabulary (UB).
  - Unauthorized actions are removed server-side.

### VRD-13-06 — Server-pushed UI instructions (best-of LS + UC cards)
- **Decision:** best-of LS, combined with UC cards.
- **Consequences:** Logic may emit `ui.render`, `ui.card` and `ui.notify` to `client://current` or to a user or session. Clients render them in the passive zone (CMP-14).

### VRD-13-07 — Realtime delta editing (adapt UB design, later version)
- **Decision:** adapt UB design; deferred.
- **Consequences:** Forms may send path/value deltas with a base commit; the server applies and commits them (micro-commits, squashable). Deferred to spec v2 unless required by an app.
