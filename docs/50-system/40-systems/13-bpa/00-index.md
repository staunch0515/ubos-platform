---
id: UBS-SYS-BPA-00
title: BPA Standard Governance — Overview
status: draft
phase: PH-0
depends_on: [UBS-SYS-00, UBS-STD-00, UBS-STD-18]
---

# BPA Standard Governance (BPA) — Overview

## 1. Purpose

BPA is the standard itself treated as a product: the normative text (UBS-STD-*), conformance
vectors, formal models, the extension and algorithm registries, the change process, the
certification programme ("BPU Compliant"), and the reference-implementation licensing. The
normative content lives in `30-standard/`; this chapter designs the machinery that produces,
releases and governs it, so that the standard stays ahead of the code (AR-033).

## 2. Responsibilities and non-responsibilities

| Responsible for | Not responsible for |
|---|---|
| authoring workflow, releases, errata | implementing kernels (DVM and third parties) |
| vector corpus, coverage, formal models and their alignment | running the runner (FRG) |
| extension and algorithm registries | Buk distribution (EXC) |
| certification procedure and certificate registry | commercial licensing of UBOS products (DEC-019) |
| compatibility policy | product roadmaps |

## 3. Personas served

PER-StandardsSteward, PER-ThirdPartyImplementer, PER-KernelEngineer, PER-RegulatorExaminer
(as reader of the published specifications).

## 4. Phase map

| Phase | BPA scope |
|---|---|
| PH-0 | repository `bpa-standard` with Core, Bitemporal and Ledger clauses, first vectors, TLA+ models for commit/CAS, merge and timelines, release 0.x |
| PH-1 | release 1.0-rc with Core, Bitemporal, Ledger, Proof (subset), Governance; vector coverage of all MUST clauses of these profiles; change process running |
| PH-2 | 1.0 final; errata process; extension registry opened to first-party Buks |
| PH-3 | Sync profile vectors; formal model of sync and shared BPUs |
| PH-4 | certification programme, public certificate registry, reference licensing terms, extension registry open to third parties |
| PH-5 | 1.x minor releases with third-party input; promotion of extensions into the standard |

## 5. Files

| File | Content |
|---|---|
| `01-context.md` | context |
| `02-functions.md` | DSN-BPA-* |
| `03-interfaces.md` | IF-BPA-* |
| `04-data.md` | repository and registries |
| `05-state-machines.md` | change proposal, certificate |
| `06-configuration.md` | release parameters |
| `07-operations.md` | governance operations |
| `08-verification.md` | verification plan |
