---
id: ANA-FU-07
title: "FU Design Documents Digest"
status: complete
phase: P1
depends_on: [INV-FU]
sources: [FU]
---

# FU — Design Documents Digest

## 1. `frontend/UBOS_STUDIO.md` (Chinese, 132 lines)

- **Purpose:** "UBOS Studio is a professional IDE-style web console for managing UBOS (Git-for-Data) entity data."
- **Layout:** sidebar navigation (Entities, Commits, Branches, Settings); left panel entity grid; right panel snapshot editor ("split-screen IDE layout").
- **Entity grid:** TanStack Table; columns ID, Slug, Type, Branch; row checkboxes; batch toolbar when several rows selected (Batch Commit, Diff Selected, Merge); search filter; click row → load into editor.
- **Snapshot editor:** Monaco (VS Code-style JSON editor); raw JSON editing; branch dropdown; commit with optional message; History tab visualizing the version chain.
- **Workflow described:** view → select → edit → switch branch → commit → view history → batch operations.
- **Stated tech:** React 18, TypeScript, Vite, Tailwind, shadcn/ui, Monaco, TanStack Table, lucide.

## 2. `frontend/RTK_QUERY_SETUP.md` (Chinese/English, 214 lines)

- API base `/api/console` via Vite proxy; endpoints documented: `GET /entities (branch, type?, search?)`, `GET /snapshot (slug, branch)`, `POST /batch-commit`.
- Cache tags: *Entity* list invalidated after a successful batch commit; *Snapshot* cached per `slug-branch`.
- Component notes: `EntityManager` (antd Table, row selection, batch operations).

## 3. `frontend/README.md` (Chinese, 203 lines)

Generic project README: React 18, TypeScript, Vite, Ant Design, Axios, React Router, Zustand; `/api` proxy to backend; scripts.

## 4. Commit history (chronological)

| Date | Messages | Design meaning |
|---|---|---|
| 12-05 | Initial commit; add coding standards to CONTRIBUTING.md; NOTICE; **"Update system name from Logrum to UBOS"** | naming: Logrum → UBOS |
| 12-06 | **"Update product name to Universal Business OS (UBOS)"** | product name fixed |
| 12-10 | first run; can batchCommit; merger branchs; Implement Global Search and Process Log APIs; add environment | CON-FU-025, 042, 031 |
| 12-11 | schema validator; add appkey; correct Environment; **ubos protocol** | CON-FU-020, 023, 010 |
| 12-12 | merger success; add user service; add Identity; add tree; add rename and copy; add branchStatus; add cache; add dashboard; ubos protocol; add merge; add history | CON-FU-022, 013, 016, 029, 040, 032, 044, 026, 041 |

## 5. Code-level design statements (Javadoc, translated where needed)

| Location | Statement |
|---|---|
| `UbosUriUtil` | "The UBOS URI format follows the pattern `ubos://{scope}/{entity_type}/{entity_id}?{modifiers}` … Scope: the Tenant ID or App ID … Key: optional query parameter to extract a specific property path … IMPORTANT: does NOT use java.net.URI as ubos:// is a custom protocol." (`@since 2.0`, "UBOS Kernel Team") |
| `LcmKernelService.findBySlugPrefix` | "Slug is treated as a dot-separated virtual path … prefix query so callers can list everything under a directory." |
| `LcmKernelService.mergeJsonContent` | "Source Wins … arrays: source array completely replaces target array." |
| `LcmKernelService.commitWithoutValidation` | "for user draft branches / stash. This must never be used for 'real' branches like main/master." |
| `LcmKernelService.renameEntity` | "Crucial behavior: atomically update slug …, create a new version chain record documenting the action, move the HEAD." |
| `V6__add_environment.sql` | "Maps running environments (UAT, PROD, etc.) to specific UBOS states (Branch/Commit)"; "Optional: pin to a specific commit for stable testing." |
| `JsonSchemaValidator` | "Skip validation for SCHEMA type itself (to allow bootstrapping)"; "No schema defined – validation passes by default." |
| `ApiKeyService.ApiKeyGenerationResult` | "plainTextKey — Only shown once!" |

## 6. Implied principles (Interpretation)

> Interpretation: FU turns UP's "Git for data" into a **governed** Git for data:
> every governance concern (who may change, what shape data must have, who must approve,
> which version runs where, who is notified) is itself versioned data addressed by URI, and
> the developer experience is an IDE plus a terminal.
