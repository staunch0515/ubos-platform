---
id: UBS-SYS-FRG-00
title: Logic Forge — Overview
status: draft
phase: PH-1
depends_on: [UBS-SYS-00, UBS-SYS-DVM-00, UBS-ARC-01]
---

# Logic Forge (FRG) — Overview

## 1. Purpose

The Forge is the developer and verification toolchain. It turns source (model, sheets,
scripts, components, views, seed data, tests) into verified, signed Buk archives, and it is
the conformance runner of the standard. The same `ubos-forge` library runs:
- as the `forge` CLI on developer machines and in CI;
- as a service linked into NOD for Studio and agents (CTR-012);
- as the verification engine used by EXC for listings.

## 2. Responsibilities and non-responsibilities

| Responsible for | Not responsible for |
|---|---|
| project layout, build, pack, sign | runtime semantics (DVM) |
| static checks, type generation, lens verification | hosting Buks (EXC) |
| test framework, simulator, replay debugging | serving production traffic (NOD) |
| verification records for logic assets and Buks | approving merges (DVM governance) |
| conformance runner and signed reports | writing the standard (BPA) |
| language server and editor integration | Studio UI (STU) |
| documentation and change-log generation | AI generation of drafts (AGT, which calls FRG) |

## 3. Personas served

PER-LogicDeveloper, PER-IsvDeveloper, PER-BusinessArchitect (checks through Studio),
PER-ThirdPartyImplementer (conformance runner), PER-StandardsSteward (vector tooling),
PER-AiAgent (FRG as a tool through AGT).

## 4. Phase map

| Phase | FRG scope |
|---|---|
| PH-0 | conformance runner skeleton and vector format; formal-model tooling integration |
| PH-1a | `forge check`, `forge types`, `forge test` against an in-process DVM; verification records for L2 assets; conformance runner for Core, Bitemporal, Ledger, Proof |
| PH-1b | headless Box dev environment (`forge dev`), install into a VAE over UBTP, Governance profile vectors, rule tests |
| PH-2 | Buk format, dependency resolution and lock, `forge pack/sign/verify`, simulator and history replay, debugger traces, language server and VS Code extension, documentation generation, Forge service in NOD |
| PH-3 | L3 WASM component toolchain (Rust, TypeScript via componentize-js, Go via TinyGo), AI iteration loop hooks |
| PH-4 | publish to EXC, reproducible verification service for EXC |
| PH-5 | third-party certification workflow support |

## 5. Files

| File | Content |
|---|---|
| `01-context.md` | context and contracts |
| `02-functions.md` | DSN-FRG-* |
| `03-interfaces.md` | IF-FRG-* (CLI and library) |
| `04-data.md` | project layout, Buk archive, lock file, reports |
| `05-state-machines.md` | Buk build and verification |
| `06-configuration.md` | `forge.toml` |
| `07-operations.md` | CI usage and verification service operation |
| `08-verification.md` | FRG verification plan |
