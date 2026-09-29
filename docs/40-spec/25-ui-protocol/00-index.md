---
id: SPEC-25
title: "UI Protocol"
status: complete
phase: P4
depends_on: [SPEC-13, SPEC-18, SPEC-19, SPEC-21, SPEC-22, SPEC-24]
sources: [UP, FU, UB, UC, US, LS, UW]
---

# UI Protocol

## 0. Chapter header

- **Scope:** This chapter covers the server side of metadata-driven UI:
  - presentation modes;
  - `Widget` and `UiMatrix` entities and widget resolution (component = f(type chain, mode));
  - `View` entities with layouts and component trees;
  - the render pipeline (resolve → hydrate → transform) and the render payload;
  - action bindings and client action chains;
  - projection of an entity onto an ancestor type;
  - pushed UI instructions;
  - formatting, i18n and client-side validation hints;
  - the client widget-registry contract.

  Client shells (cockpit, studio, mobile) are specified in SPEC-26.
- **MOD:** `UI`
- **depends_on:** SPEC-13, SPEC-18, SPEC-19, SPEC-21, SPEC-22, SPEC-24.
- **Terms used:** TERM-Mode, TERM-Widget, TERM-View, TERM-RenderPayload, TERM-Projection, TERM-PushedUi, TERM-Action, TERM-Intent, TERM-EffectiveData, TERM-Masking.
- **Origin summary:**
  - Verdicts: VRD-13-01…07, VRD-06-06, VRD-14-04.
  - UC: the lens system, schema-to-UI and pushed cards (CON-UC-054…056).
  - US: widgets as resources, the universal and recursive renderers, the view engine, the lookup order, `ui_modes`, projection, the action automator (CON-US-015, 026, 051…055, 058).
  - UB: the render pipeline with versioned layouts, action chains, smart form state, types driving widgets and masking (CON-UB-050…052, 057).
  - UP: the recursive renderer with action binding (CON-UP-055).
  - LS: the universal editor as data, pushed UI instructions, widgets as UI-slot behaviours (CON-LS-051, 053, 054).
  - UW: intents and view/task routing (CON-UW-003, 012).

## Parts of this chapter

This chapter is split into files (WRITING-RULES R2.2). Read them in order.

| File | Sections |
|---|---|
| `00-index.md` (this file) | §0, §1, §6, §7, §8 |
| `01-model.md` | Model — §2 |
| `02-behavior.md` | Behavior — §3 |
| `03-interfaces-nfr.md` | Interfaces and non-functional — §4, §5 |

## 1. Concepts

- **UI is data.** Widgets, views and layouts are versioned entities. A new type gets working UIs without any front-end code, because the kernel derives forms, tables and cards from the type chain.
- **The server decides, the client renders.**
  - The server resolves widgets, trims fields and actions by permission, masks values, and sends a **render payload**.
  - The client maps widget URIs to its own components and runs client-side action chains.
- **Degrade, never break.** An unknown widget falls back along a chain to a primitive widget, and finally to a JSON view (UC: "the system never breaks").

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-UI-001…003 | Mode normalisation; resolution order incl. platform fallback; cache reuse |
| REQ-UI-010…013 | View selection with variants and tenant patches; derived layouts for new types; pipeline trimming/masking/read-only; client validation hints |
| REQ-UI-020, 021 | Binding authorisation and chains; board drag transitions |
| REQ-UI-030 | Projection save touches only projected paths |
| REQ-UI-040 | Pushed instructions rendered under recipient permissions |
| REQ-UI-050 | Inheritance hints and reset-to-inherited |
| REQ-UI-060, 061 | Fallback rendering of unknown widgets; accessibility and i18n |

## 7. Implementation notes

- **Reference code:**
  - US renderers [US:apps/ubos_web/src/engine/UniversalRenderer.tsx], [US:apps/ubos_web/src/engine/SchemaRenderer.tsx], [US:apps/ubos_web/src/engine/ViewEngine.tsx], registry [US:apps/ubos_web/src/kernel/registry/WidgetRegistry.ts].
  - UP renderer [UP:ubos-shell/src/engine/SchemaRenderer.tsx].
  - UC executor and feed [UC:ui/components/Command/DynamicExecutor.tsx], [UC:ui/components/Passive/FeedStream.tsx].
- **Design references:** UB render pipeline and form state [UB:docs/动态视图渲染器.md]; UC lens design [UC:docs/front/front_04.md]; US projection [US:docs/front/渲染.md].
- **Divergences resolved:**
  - US's `/view/{slug}/init` becomes `ui.render`.
  - UC's cards in the execution log become pushed UI instructions.
  - UB's `ViewSchema` + `data` + `meta` is kept as the payload's `ui` + `data` + `meta`.
- **Web implementation:** each standard widget is a React component registered under its URI. Forms use the payload `schema` with a JSON Schema validator (e.g. Ajv). Monaco hosts ScriptEditor with the Rhai language and `rt.check` diagnostics.

## 8. Open questions

None.
- Deferred to later versions: remote loading of tenant widget code (sandboxed web components), and realtime delta editing (REQ-UI-070).
- Deferred to SPEC-26: visual designers for views and lifecycles.
