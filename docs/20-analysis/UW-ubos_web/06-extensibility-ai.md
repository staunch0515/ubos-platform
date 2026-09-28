---
id: ANA-UW-06
title: "UW Extensibility & AI"
status: complete
phase: P1
depends_on: [ANA-UW-02]
sources: [UW]
---

# UW — Extensibility & AI

| Extension point | Mechanism | Status |
|---|---|---|
| New function | add `Feature` to catalog + case in `Workspace` | code (mock) |
| New intent | add `IntentType`, parser rule, dock pane | code |
| New inbox source | add `DockItem` | mock |

### CON-UW-060 — Hard-coded extension, metadata-shaped
- **What:** Features, intents and dock items are data-shaped types but rendered by code switches; in the platform these would be entities (features = functions/actions, intents = action invocations, dock items = events/notifications).
- **Tags:** plugin, metadata

### CON-UW-061 — Generated with an AI app builder
- **What:** Created as a Google AI Studio app (README run instructions; `GEMINI_API_KEY` in config template); core UI does not call an AI model.
- **Sources:** [UW:README.md], [UW:metadata.json], [UW:vite.config.ts].
- **Tags:** ai, codegen
