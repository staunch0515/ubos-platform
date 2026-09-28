---
id: ANA-UB-07
title: "UB Design Documents Digest — Index"
status: complete
phase: P1
depends_on: [INV-UB]
sources: [UB]
---

# UB — Design Documents Digest (index)

The UB design corpus (23 files, 16,740 lines, Chinese) is split per WRITING-RULES R2.2.
Each document has a digest id `D-UB-NN` used for cross-references.

| Digest id | Source document | Lines | Part file |
|---|---|---|---|
| D-UB-01 | `三块拼图.md` (Three Puzzle Pieces) | 86 | 01 |
| D-UB-02 | `The essence of entities.md` | 98 | 01 |
| D-UB-03 | `UBOS Three Layer System.md` | 670 | 01 |
| D-UB-04 | `design.md` | 850 | 01 |
| D-UB-05 | `UbosContext.md` | 354 | 01 |
| D-UB-06 | `Rehydration.md` | 340 | 01 |
| D-UB-07 | `ProcessContext.md` | 1,049 | 01 |
| D-UB-08 | `UBOS协议的综合处理.md` (UBOS Protocol) | 1,066 | 01 |
| D-UB-09 | `sys_boot.md` (Genesis Protocol, 17 phases) | 5,166 | 02 |
| D-UB-10 | `元数据加载器.md` (Meta-Loader) | 683 | 02 |
| D-UB-11 | `boot.md` | 552 | 02 |
| D-UB-12 | `database_strong.md` | 1,367 | 02 |
| D-UB-13 | `动态视图渲染器.md` (Dynamic View Renderer) | 2,018 | 03 |
| D-UB-14 | `forntend.md` | 291 | 03 |
| D-UB-15 | `IDE.md` | 324 | 03 |
| D-UB-16 | `UBOS Smart Document.md` | 262 | 03 |
| D-UB-17 | `I18n.md` | 185 | 03 |
| D-UB-18 | `BEL Pro.md` | 465 | 03 |
| D-UB-19 | `ai-dev.md` | 601 | 03 |
| D-UB-20 | `高级提示词.md` (Advanced Prompts) | 238 | 03 |
| – | `EnvContext.md` | 0 | (empty) |

Parts:
- `01-vision-layers-context.md` — vision, three puzzle pieces, entity definition, UBOS/ESE/BPU layering, four-layer design, script context, rehydration, process context, object lifecycle and URI.
- `02-genesis-loaders-storage.md` — 17-phase genesis protocol, meta-loader, boot, storage hardening.
- `03-ui-languages-ai.md` — SDUI, Studio/IDE, smart document, i18n, BEL, AI-driven development, IDE prompts.

Nature of the corpus: conversation logs with an AI acting as "professor/architect"; many
proposals were later refined or superseded within the same document (sections marked
"[废]" discarded, "[假]" fake). The digest records the **latest position** of each discussion
and notes superseded ideas only where they add design insight.
