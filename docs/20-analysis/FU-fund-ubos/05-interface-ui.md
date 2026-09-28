---
id: ANA-FU-05
title: "FU Interfaces & UI"
status: complete
phase: P1
depends_on: [ANA-FU-01]
sources: [FU]
---

# FU — Interfaces & UI

## Studio REST API (`/api/console`)

| Group | Endpoints |
|---|---|
| Entities | `GET /entities`, `GET /entities/navigate` (namespace), `POST /entity/rename`, `POST /entity/copy` |
| Snapshots & history | `GET /snapshot` (by `uri` or `type/slug/branch`, with inheritance), `GET /snapshot/{commitId}`, `GET /history` |
| Commit | `POST /batch-commit` |
| Branches | `GET /branches`, `POST /branch/create`, `POST /revert`, `POST /merge`, `GET /status/diff` |
| Search | `GET /search` |
| Processes | `GET /process/recent`, `GET /process/{id}` |
| Environments | `GET /environments`, `GET /environment/{name}`, `POST /environment/save`, `DELETE /environment/{name}`, `GET /environment/resolve`, `POST /environment/{name}/pin`, `POST /environment/{name}/unpin` |
| Schemas | `POST /schema/commit`, `GET /schema/{type}`, `GET /schemas`, `POST /schema/cache/clear`, `GET /schema/cache/stats` |
| Approvals | `POST /approval/request`, `POST /approval/approve`, `GET /approval/pending`, `GET /approval/{id}`, `POST /approval/{id}/cancel` |
| Terminal | `POST /exec` |
| Other | `GET /api/v1/history/{entityId}/blame` (NDJSON), `POST/GET /api/v1/stash`, `POST /merge/conflicts` |

### CON-FU-050 — UBOS Studio: an IDE for business data
- **What:** A single-page operator/developer console in IDE layout.
- **How:**
  - Sidebar views: Entities (all / per type), Process Log, Environments, Schema, Approvals, Identity, Branch Status, Cache, Dashboard.
  - Entities view: `NamespaceTree` (dotted slug folders) + `EntityGrid` (TanStack Table; selection; batch toolbar: Batch Commit, Diff Selected, Merge) + `SnapshotEditor` (Monaco JSON, branch switcher, commit with message, History tab).
  - Context menus on entities and namespaces (rename, copy, …).
  - Typed API layer with RTK Query (tags: entity list refreshed after batch commit; snapshots cached per `slug-branch`).
  - Theme selector; 403 responses surfaced as permission errors.
- **Why (intent):** `UBOS_STUDIO.md`: "a professional IDE-style web console for managing the entity data of UBOS (Git-for-Data)".
- **Sources:** [FU:frontend/UBOS_STUDIO.md], [FU:frontend/src/components/UbosStudioLayout.tsx], [FU:frontend/src/store/ubosApi.ts].
- **Tags:** frontend, versioning

### CON-FU-051 — Time-travel slider with blame mode
- **What:** A timeline slider across an entity's commits; selecting a marker loads that version; blame mode annotates fields with author/time/commit.
- **Sources:** [FU:frontend/src/components/TimeTravelSlider.tsx], [FU:frontend/src/components/HistoryViewer.tsx].
- **Tags:** frontend, versioning

### CON-FU-052 — Visual diff and conflict resolution
- **What:** Side-by-side JSON diff (`react-diff-viewer-continued`) and a resolver presenting base/ours/theirs per conflicting path, producing a resolved document + message.
- **Sources:** [FU:frontend/src/components/DiffViewer.tsx], [FU:frontend/src/components/MergeConflictResolver.tsx], [FU:frontend/src/components/BranchMergeModal.tsx].
- **Tags:** frontend, versioning

### CON-FU-053 — Branch status view
- **What:** Table of entities `NEW` / `MODIFIED` / `DELETED` between current and base branch (CON-FU-040).
- **Sources:** [FU:frontend/src/components/BranchStatusViewer.tsx].
- **Tags:** frontend, versioning

### CON-FU-054 — Governance managers
- **What:** Dedicated managers for each governance entity type.
- **How:** `IdentityManager` (users, groups, policies), `ApprovalManager`, `SchemaManager` + `SchemaEditModal`, `EnvironmentManager` + `EnvironmentEditModal`, `WebhookManager` + `WebhookEditModal`, `APIKeyManager` + `APIKeyGenerateModal` (show-once key), `CacheManager`, `DraftManager`, `ProcessLogViewer`, `OperationalDashboard`.
- **Sources:** [FU:frontend/src/components/].
- **Tags:** frontend, permission, integration, observability

### CON-FU-055 — Git-like web terminal
- **What:** A browser terminal that accepts Git-style commands against the kernel.
- **How:** `POST /api/console/exec {command}` → `CommandParser` (quoted tokens, `--opt value`, `-o value`, flags) → commands `commit`, `show|cat`, `checkout --cid`, `exists`, `search`, `help|?`, `clear`, `version` ("UBOS Kernel v1.0.0 – Git for Data"); positional form `commit LOGIC tax-calc master '{"rate":0.1}' "Initial version"`; ANSI-colored output; client `public/console.html` (479 lines).
- **Why (intent):** Scriptable, keyboard-first operation for developers.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/server/controller/WebConsoleController.java], [FU:backend/src/main/java/org/logrum/ubos/server/controller/console/CommandParser.java], [FU:frontend/public/console.html].
- **Tags:** cli, frontend

### CON-FU-056 — Client-side tenant context
- **What:** The active tenant is global client state sent with requests.
- **Sources:** [FU:frontend/src/store/tenantSlice.ts].
- **Tags:** multi-tenancy, frontend
