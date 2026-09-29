---
id: UBS-SYS-WSP-00
title: Workspace — Overview
status: draft
phase: PH-2
depends_on: [UBS-SYS-00, UBS-SYS-SDK-00, UBS-ARC-01]
---

# Workspace (WSP) — Overview

## 1. Purpose

The Workspace is the user-facing client family: the web workspace, the desktop and mobile
apps (Tauri 2 shells around the same renderer), the external portal, and the office surfaces
(Live Doc, Smart Grid, War Room, Action Messages, Executable Books). It is a server-driven
renderer: the DVM decides what a user may see and do (view payloads, CTR-010); WSP decides
how it looks and feels.

## 2. Responsibilities and non-responsibilities

| Responsible for | Not responsible for |
|---|---|
| rendering view payloads with the widget catalogue | deciding fields, permissions and actions (DVM) |
| shell: navigation, search, command palette, notification centre | notification rules and delivery (NOD) |
| forms, lists, details, timelines, as-of browsing, diff review | merge and diff semantics (DVM) |
| office surfaces and rendering to PDF and office formats | seals and proofs (DVM) |
| e-signature ceremonies UI and flows | signature cryptography (DVM, NOD keys) |
| offline indicators, queued actions, conflict screens (Box) | sync protocol (NOD, DVM) |
| localisation, accessibility, theming | canonical formats (standard) |

## 3. Personas served

PER-BusinessUser, PER-Approver, PER-FundAccountant, PER-FundOperationsManager,
PER-InvestorServicesClerk, PER-ContractManager, PER-Executive, PER-ExternalParty,
PER-InternalAuditor, PER-RegulatorExaminer, PER-PersonalUser, PER-FieldWorker.

## 4. Phase map

| Phase | WSP scope |
|---|---|
| PH-1b | minimal web shell used for acceptance: generated forms, lists, details, history (engineering UI) |
| PH-2 | production web workspace: shell, widget catalogue 1.0, forms with live validation, lists, details, timeline and as-of, diff review, tasks and inbox, explanations, Live Doc, rendering and export, e-signatures, localisation (EN, plus one more), accessibility WCAG 2.2 AA |
| PH-3 | desktop and mobile apps, offline experience, external portal, Smart Grid, War Room, Action Messages, redlining and negotiation, dashboards, theming, Copilot panel |
| PH-4 | Executable Books reader, external signature providers |
| PH-5 | browser-local kernel mode (reads served by the WASM DVM) |

## 5. Technology

TypeScript, React 19, a headless component library with the UBOS design system, TanStack
Query-like caching through the SDK, ProseMirror for rich text, a canvas grid engine for Smart
Grid, Tauri 2 shells for desktop and mobile.

## 6. Files

| File | Content |
|---|---|
| `01-context.md` | context and contracts |
| `02-functions/` | DSN-WSP-* |
| `03-interfaces.md` | IF-WSP-* (renderer contract, widget catalogue) |
| `04-data.md` | client-side state and drafts |
| `05-state-machines.md` | form, signing ceremony, negotiation round, offline action |
| `06-configuration.md` | tenant UI configuration and feature flags |
| `07-operations.md` | delivery, versions, telemetry |
| `08-verification.md` | WSP verification plan |
