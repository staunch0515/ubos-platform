---
id: ANA-UC-07-3
title: "UC Digest Part 3 — Front End, Data Standard, Vision, Rules"
status: complete
phase: P1
depends_on: [ANA-UC-07]
sources: [UC]
---

# UC digest — `docs/front`, `init_data.md`, `book.md`, `prompt.md`, `docs/db`

### D-UC-50 — front_01: Web IDE generation prompt
- React 18 + TS + Vite + Tailwind (slate/indigo) + lucide + recharts + CodeMirror; two columns: Left Workspace (header with context switcher "UBOS_Core / main", Spotlight-style command bar where `/script` opens Logic Studio; views Dashboard / Kanban / **Logic Studio**: function list, editor, JSON params, Deploy, Run, console) and Right Task Panel (Inbox, or Execution History/Audit Log of Logrums with status colors). Second prompt adapts to `tenant_id`/`user_id` and 2 s polling. "English-only code" constraint.

### D-UC-51 — front_02: "The Cockpit"
- User rules: no menu; search box finds functions/commands; command selection shows its page in the Active zone; a slide-out Logrum picker before the search box; choosing a Logrum fixes tenant and branch; functions run under that Logrum's data scope; switching Logrum re-initializes the Active zone; identity first, then list that identity's Logrums.
- Three prompts: identity & skeleton (zustand auth store, login by `user_id`, top bar [Logrum][⌘K][avatar]); context engine (drawer, `currentLogrum`, reset, tenant on every request); command palette & active area (commands Deploy Function, View Logs, Chat with AI, File Manager). Philosophy: strict hierarchy User → Logrum → Command → Workspace; state isolation.

### D-UC-52 — front_03: Cockpit copilot system prompt
- Role: in-browser orchestration hub. State: `current_host`, `current_sovereign`, `current_branch`, `current_logrum_id`, `user_identity`; genesis boot handshake `/ambassador/connect {host: sys, sovereign: sys, branch: main, alias: boot, user_id: root}`. Tools: `search_entity` (`/entity/search`), `load_function_ui` (read `payload.ui_schema.widget`: JsonEditor → stage Monaco; Form → stage auto-form; Card → passive), `invoke_kernel` (`/invoke` with `logrum_id`). SOP "Genesis flow", error handling (embassy lost → re-handshake; policy denied → red passive card). Integration: zone map + component registry so the AI emits only structured render instructions. Follow-up prompt wires the omnibar to real search (debounced, parallel search of functions and Logrums; "Commands" vs "Switch Context" groups).

### D-UC-53 — front_04: Lens system (polymorphic rendering)
- `Component = f(Data.Type, Context.Mode)`; backend only returns `entity_type` and `_extends` (`_meta`); global UI matrix (entity `sys:SysConf:ui_matrix`) maps type × mode → component; recursive resolver up the `_extends` chain → `BaseEntity` fallback; walkthroughs (grid cells, read viewer, edit IDE, new subtype with zero work); benefits: rules not pages, O(1) UI work, consistency, instant global change; UI assets inherit (Money formatting everywhere), semantic fallback to base widgets.

### D-UC-54 — init_data.md: UODS v4.0 Strict + generation of 15 genesis files
- Standard (CON-UC-017) and a self-check that found garbage data (relations without `target`, enums without `dict_code`, primitive metadata placed as top-level keys instead of meta-type properties).
- Per-file rationale and quality checklists for files 01–15 (CON-UC-018): e.g. separation of `country_iso` (type) vs `country` (entity); login account separate from person ("one person, many accounts; bot accounts"); bcrypt `password` type; audit log with `client_ip` (ipv4); `view_menu` self-reference; RBAC via `sec_group_perm_map`; `sys_function` with `script_content` + `input_schema` ("core of the serverless architecture"); workflow states with `on_enter/exit` actions; app manifest with vendor party ("app store" model); FX rate scale 6, UOM factor scale 10; measure types for IT/power monitoring; PKI with pem/hash; file 13 intended: `sys_context` type + tenant `logrums`, root person/login (bcrypt placeholder), groups superuser/admin/developer, root→superuser map; file 14: Groovy scripts rewritten in Rhai; file 15: screens and menus with `config_json` component trees and `ubos://` references.

### D-UC-55 — book.md: Executable Book vs knowledge base
- Knowledge base/RAG = static consultant (OCR → chunk → embed → retrieve; tells *how*); executable book = dynamic engineer (DSL/Rhai → kernel → state → APIs; *does* it). Four levels (living, hybrid intelligence, self-updating, app generator) mapped to UBOS features: Rhai = micro-logic, Logrum = reader's interaction trace, event bus/cron = the book's life, SysData = memory.

### D-UC-56 — prompt.md: house rules
- "One core, five clients, cross-platform, own binary protocol, zero-copy, high-performance vector processing"; everything is an entity; 6 tables only; default tenant `logrums`; no gratuitous `sys` prefix; pure explicit `_extends`; base changes on latest `all_src.txt`; split files > 100 lines; no Chinese in code; no citation markers; complete code; never drop existing logic.

### D-UC-57 — db/genesis_data (as loaded)
- 15 files, 150 items (01: 11 meta types, 02: 20, 03: 14, 04: 16, 05: 11, 06: 11, 07: 9, 08: 10, 09: 8, 10: 7, 11: 13, 12: 3, 13: duplicate of 01, 14: 3 Logic scripts, 15: 3 Views). Divergence: `fn_save_raw` script calls `sys_db_commit(full_id, data)` (2-arg, legacy id) while the kernel syscall is `sys_db_commit(slug, type, data)`; the kernel-built `save_entity_raw` is the working one.

### D-UC-58 — db/V1_init.sql, clear.sql
- Schema v2.1 (see ANA-UC-04 / CON-UB-040); `clear.sql` truncates the tables for a fresh genesis.
