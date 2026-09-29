---
id: UBS-SYS-STU-00
title: Studio — Overview
status: draft
phase: PH-2
depends_on: [UBS-SYS-00, UBS-SYS-WSP-00, UBS-SYS-FRG-00]
---

# Studio (STU) — Overview

## 1. Purpose

Studio is the builder environment inside the Workspace for business architects, analysts and
implementation consultants. It edits the model, sheets, decision tables, lifecycles, views,
logic and Buk packaging on branches, and drives change sets, simulations, impact analysis and
releases. Studio has no special write path: every edit is an ordinary commit on a branch
(CTR-011).

## 2. Responsibilities and non-responsibilities

| Responsible for | Not responsible for |
|---|---|
| visual and textual editors for model artefacts | checking and verifying (FRG service) |
| branch and change-set workflows for builders | merge semantics (DVM) |
| impact, simulation and test UIs | simulation engine (DVM, FRG) |
| release and Buk packaging UI | publishing and listing (EXC) |
| spreadsheet-to-model import wizard UI | import engine (BRG) |
| AI-assisted drafting UI | generation (AGT) |

## 3. Personas served

PER-BusinessArchitect, PER-BusinessAnalyst, PER-ImplementationConsultant, PER-LogicDeveloper
(light edits), PER-TenantAdministrator (subscriptions, webhooks, connectors).

## 4. Phase map

| Phase | STU scope |
|---|---|
| PH-2 | model designer, sheet and decision-table editors, lifecycle editor, view designer, script editor with LSP, change-set review for builders, impact and simulation screens, release tagging, connectors and webhook management, spreadsheet import wizard |
| PH-3 | AI drafting panel, Buk packaging and install-plan preview, test authoring UI |
| PH-4 | publish to EXC from Studio, Book authoring |

## 5. Files

| File | Content |
|---|---|
| `01-context.md` | context |
| `02-functions.md` | DSN-STU-* |
| `03-interfaces.md` | IF-STU-* |
| `04-data.md` | editor state |
| `05-state-machines.md` | builder change flow |
| `06-configuration.md` | settings |
| `07-operations.md` | delivery |
| `08-verification.md` | STU verification plan |
