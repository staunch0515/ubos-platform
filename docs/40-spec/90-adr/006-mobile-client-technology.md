---
id: ADR-006
title: "ADR-006: Mobile client technology"
status: complete
phase: P4
depends_on: [ADR-001, ADR-003, SPEC-26]
sources: [UC, UW, US]
---

# ADR-006: Mobile client technology

- **Status:** accepted (2026-09-29, P4 SPEC-26). ADR-003 left the choice to SPEC-26.

## Context

- ADR-003 makes mobile one of the five clients: a reduced cockpit with the dock, approvals, search, entity views and the action automator.
- All UI rendering is server-driven (SPEC-25). The web shell (`@ubos/shell`, React/TS) implements the widget registry, the action automator and the dock.
- The desktop client already uses Tauri 2 (ADR-001).

## Options considered

1. **Tauri 2 mobile, wrapping the same React shell in a MOBILE layout.**
   - Pros: maximal reuse (widgets, automator, SDK); the same toolchain as desktop; Rust plugins available for keychain storage and notifications.
   - Cons: a web-view UI feels less native, and Tauri mobile is younger than React Native.
2. **React Native with a separate widget implementation.**
   - Pros: native feel and a mature ecosystem.
   - Cons: a second implementation of the standard widget catalogue and the automator, which doubles conformance work for REQ-UI-060.
3. **PWA only.**
   - Pros: no store deployment.
   - Cons: weaker push notifications and keychain access on iOS.

## Decision

Option 1. The PWA build of the same shell remains available as a fallback distribution.

## Consequences

- `@ubos/widgets` MUST support touch and small screens for every standard widget that lists `MOBILE` in `platforms`. Widgets that do not list MOBILE fall back per REQ-UI-002.
- Push notifications (P2) use a Tauri plugin with an APNs/FCM relay.
- If measurements show unacceptable UX, a later ADR may replace the mobile shell without affecting the protocol or the server.

## Origin

ADR-003, VRD-19-04, VRD-14-04, CON-UW-052, CON-UC-052.
