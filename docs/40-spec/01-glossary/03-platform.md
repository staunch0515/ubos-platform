---
id: SPEC-01-3
title: "Glossary part 3 — Security, protocol, UI, AI, packages, method"
status: complete
phase: P3
depends_on: [SPEC-01]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Glossary part 3 — Security, protocol, UI, AI, packages, method

Part of SPEC-01. Entry format and rules: `00-index.md` §0.

---

## 11. Security and tenancy

### TERM-Tenant
- **Definition:** A key dimension of all stored data and an isolation boundary. Each tenant has a code.
- **Aliases:** scope (FU); DB in "Tenant = DB" (UB metaphor).
- **Not:** a namespace; a Context.
- **Chapter:** SPEC-23
- **Origin:** CON-FU-012, CON-UB-011, VRD-18-01

### TERM-RootTenant
- **Definition:** The tenant `logrums` that holds platform packages (genesis) which all tenants inherit. The authority alias `system` resolves to it.
- **Aliases:** `sys` tenant (UC, early); constitution holder (UC).
- **Chapter:** SPEC-23
- **Origin:** CON-UC-019, VRD-18-01

### TERM-Principal
- **Definition:** The acting identity of a request: a user, an API key, an agent or the system identity.
- **Chapter:** SPEC-22
- **Origin:** CON-UP-018, VRD-10-02

### TERM-Role
- **Definition:** A named set of permissions granted to principals through Relationship entities.
- **Chapter:** SPEC-22
- **Origin:** CON-FU-022, CON-LC-017, VRD-10-02

### TERM-Policy
- **Definition:** An IAM-style entity: effect (allow/deny), principals, actions, resource URI patterns, conditions. Deny by default; explicit deny wins.
- **Aliases:** SysPolicy (UC, early); policy as code (UC); policy rules over URIs (FU).
- **Chapter:** SPEC-22
- **Origin:** CON-FU-022, CON-UC-028, CON-UP-052, VRD-10-02

### TERM-ApiKey
- **Definition:** A scoped, expiring machine credential stored as a versioned secret entity (hash only).
- **Chapter:** SPEC-22
- **Origin:** CON-FU-023, VRD-10-05

### TERM-ApprovalRequest
- **Definition:** A deferred commit: a staged change set stored as an entity, applied by the kernel when approved.
- **Chapter:** SPEC-22
- **Origin:** CON-FU-021, VRD-10-04

### TERM-Custody
- **Definition:** The registry fields that separate the **owner** (responsible principal) from the **author** of each commit, plus visibility.
- **Aliases:** owner vs author (UB).
- **Chapter:** SPEC-22
- **Origin:** CON-UB-044, VRD-10-07

### TERM-Masking
- **Definition:** Hiding or partially hiding values on read according to the property's mask slot and the reader's permissions.
- **Chapter:** SPEC-22
- **Origin:** CON-UB-057, VRD-10-03

---

### TERM-ServiceAccount
- **Definition:** A non-human principal: a tenant's system identity, an integration account, the anonymous identity, or the kernel identity in `logrums`.
- **Aliases:** SYSTEM user (UP); SYSTEM_INTERNAL_ROLE (LC); scheduler user (UC).
- **Chapter:** SPEC-22
- **Origin:** CON-UP-030, CON-LC-027, VRD-11-05

### TERM-Secret
- **Definition:** An encrypted entity holding a secret value (a password, token or key). It is referenced by Context env vars and read only through `secret.get` with permission, and never appears in traces or the audit.
- **Chapter:** SPEC-22
- **Origin:** VRD-10-01, REQ-CTX-003

### TERM-TenantGrant
- **Definition:** A TENANT_GRANT relationship by which one tenant lets another tenant's principals access listed resources, optionally including embassy sessions.
- **Chapter:** SPEC-23
- **Origin:** VRD-18-04, CON-UC-024

---

## 12. Protocol

### TERM-Ubtp
- **Definition:** The UBOS Transfer Protocol: one message model with the operations Handshake, Mutate, Emit, Query, Subscribe (and Unsubscribe), bound to TCP, WebSocket and HTTP.
- **Aliases:** UBTP (LS, US).
- **Chapter:** SPEC-24
- **Origin:** CON-LS-002, CON-US-006, VRD-15-01

### TERM-UbtpOperation
- **Definition:** One of the UBTP message kinds. `Mutate` writes entities directly; `Emit` invokes a named operation (action, pipeline) by URI; `Query` reads; `Subscribe` registers for pushes; `Handshake` opens a protocol session.
- **Chapter:** SPEC-24
- **Origin:** CON-LS-002, VRD-15-01

### TERM-Envelope
- **Definition:** The common frame of every UBTP message: protocol version, request ID, context authority, idempotency key, encoding, auth.
- **Aliases:** request envelope (LC).
- **Chapter:** SPEC-24
- **Origin:** CON-LC-003, CON-LS-002

### TERM-ProtocolSession
- **Definition:** The state a server keeps for one connected client after Handshake: principal, context, subscriptions, encoding.
- **Aliases:** session context (LS).
- **Chapter:** SPEC-24
- **Origin:** CON-LS-003

### TERM-TransportBinding
- **Definition:** A mapping of UBTP to a transport: TCP (length-prefixed frames), WebSocket, HTTP routes.
- **Chapter:** SPEC-24
- **Origin:** VRD-15-02

### TERM-NamedOperation
- **Definition:** An action, pipeline or query addressed by a versioned URI (e.g. `ubos://system/Action/entity.edit@v1`).
- **Aliases:** catalogue of named, versioned processes (LC); operation enum (UB).
- **Chapter:** SPEC-24
- **Origin:** CON-LC-001, CON-UB-003, VRD-15-03

---

---

## 13. UI and client shells

### TERM-Mode
- **Definition:** The presentation purpose of a UI element: `view`, `edit`, `create`, `cell` (list and table cells), `card`, `filter`, `execute` (running logic or scripts). Defined in SPEC-25 REQ-UI-001.
- **Aliases:** `ui_modes` (US); lens (UC).
- **Chapter:** SPEC-25
- **Origin:** CON-US-015, CON-UC-055, VRD-13-01

### TERM-Widget
- **Definition:** A UI component entity addressed by URI. The widget for a property is `f(type chain, mode)`.
- **Aliases:** UI-slot behavior (LS); widget as `ubos://` resource (US).
- **Chapter:** SPEC-25
- **Origin:** CON-LS-053, CON-US-051, CON-US-058, VRD-13-01

### TERM-View
- **Definition:** A versioned entity describing a screen: layout type, regions, bound queries and actions.
- **Aliases:** screen (UC `view_screen`); layout (UB); view engine (US).
- **Chapter:** SPEC-25
- **Origin:** CON-UB-050, CON-US-054, VRD-13-03

### TERM-RenderPayload
- **Definition:** The server-produced description of a UI (resolved widgets, data, actions) that a client renders.
- **Aliases:** SDUI payload (UB, UP).
- **Chapter:** SPEC-25
- **Origin:** CON-UB-050, CON-UP-055, VRD-13-02

### TERM-Projection
- **Definition:** Viewing and editing an entity through any ancestor type; writes merge back into the entity.
- **Aliases:** slice projection / merge (US).
- **Chapter:** SPEC-25
- **Origin:** CON-US-026, VRD-13-04

### TERM-PushedUi
- **Definition:** A UI instruction (card, dialog, toast, navigation) sent by the server or logic to a client.
- **Aliases:** server-pushed UI (LS); script-pushed cards (UC).
- **Chapter:** SPEC-25
- **Origin:** CON-LS-051, CON-UC-056, VRD-13-06

### TERM-Cockpit
- **Definition:** The end-user shell: a bicameral layout with a context/navigation side and an active work zone, an omnibar and a task dock; menu-less.
- **Aliases:** Web OS shell (UP); bicameral UI (UC); workspace column + task dock (UW).
- **Chapter:** SPEC-26
- **Origin:** CON-UC-052, CON-UC-053, CON-UW-001, CON-UP-054, VRD-14-01

### TERM-Omnibar
- **Definition:** The single command/search entry of the cockpit that turns typed text into intents.
- **Aliases:** command bar (UW); command palette.
- **Chapter:** SPEC-26
- **Origin:** CON-UC-053, CON-UW-050, VRD-14-01

### TERM-Dock
- **Definition:** The cockpit area that holds running and pending tasks (passive and active modes).
- **Aliases:** task dock, dock item (UW).
- **Chapter:** SPEC-26
- **Origin:** CON-UW-013, CON-UW-052

### TERM-Studio
- **Definition:** The builder shell for types, logic, views, packages, branches and governance, with time travel, diff and blame.
- **Aliases:** UBOS Studio IDE (FU); Meta-IDE (UB); action automator (US).
- **Chapter:** SPEC-26
- **Origin:** CON-FU-050, CON-UB-053, CON-UB-054, VRD-14-02

### TERM-Client
- **Definition:** One of the five programs that use the platform through UBTP only: web, desktop, mobile, CLI, SDK/agent.
- **Chapter:** SPEC-26, SPEC-31 (ADR-003)
- **Origin:** CON-UC-067, CON-US-064, VRD-19-04

### TERM-IdeIntegration
- **Definition:** Developer tooling that exposes entities as a virtual file system (`ubos://` FS) inside an IDE. Not one of the five clients.
- **Aliases:** VS Code `ubos://` FS (US); VFS as entities (UC).
- **Chapter:** SPEC-26 (ADR-003)
- **Origin:** CON-US-057, CON-UC-044, VRD-14-03

---

### TERM-Shell
- **Definition:** A server-defined layout entity of a client (COCKPIT, STUDIO or MOBILE). It sets the home intent, dock and omnibar sources, pinned apps and theme per role.
- **Aliases:** desktop VIEW (UP).
- **Chapter:** SPEC-26
- **Origin:** CON-UP-054, VRD-14-01

### TERM-App
- **Definition:** A named bundle of views and actions shown as an application in the launcher.
- **Aliases:** app manifest (UB); feature (UW catalog).
- **Chapter:** SPEC-26
- **Origin:** CON-UP-054, CON-UB-062, CON-UW-010

---

## 14. AI

### TERM-AiProvider
- **Definition:** An entity configuring a model endpoint (kind, model, limits, cost).
- **Chapter:** SPEC-27
- **Origin:** CON-FU-061, CON-UB-066

### TERM-AiBuilder
- **Definition:** The loop in which AI generates a package of definitions, dry-runs it, stores it on a draft branch and waits for approval.
- **Aliases:** AI Architect (UP); AI builder loop (UB).
- **Chapter:** SPEC-27
- **Origin:** CON-UP-061, CON-UB-063, VRD-16-01

### TERM-Agent
- **Definition:** A principal driven by an AI model, with its own Context and memory, acting only through syscalls and UBTP.
- **Aliases:** stateful agent (UC).
- **Chapter:** SPEC-27
- **Origin:** CON-UC-062, VRD-16-03

### TERM-Copilot
- **Definition:** The assistant inside shells that turns natural language into intents for the user to confirm; never writes directly.
- **Aliases:** copilot console (UP); front-end copilot contract (UC).
- **Chapter:** SPEC-27
- **Origin:** CON-UP-056, CON-UC-058, VRD-16-04

### TERM-Explanation
- **Definition:** A human-readable account of why a decision or result happened, built from trace trees and decisions.
- **Chapter:** SPEC-27
- **Origin:** CON-US-061, VRD-16-06

---

---

## 15. Packages, boot and deployment

### TERM-Package
- **Definition:** The unit of installation and delivery: a manifest plus entities (types, actions, logic, views, rules, seed data) and scenarios.
- **Aliases:** bundle, app (UB design); feature (UW catalog).
- **Chapter:** SPEC-28
- **Origin:** CON-UB-062, CON-US-063, VRD-17-01, VRD-22-02

### TERM-Genesis
- **Definition:** The root package that creates the meta-model, the ontology and the platform types in the root tenant on first boot.
- **Aliases:** genesis bootstrap (UP, UC); genesis bootloader (LS); boot data (UB).
- **Chapter:** SPEC-28
- **Origin:** CON-UP-027, CON-UC-033, CON-LS-024, CON-UB-031, VRD-17-01

### TERM-Loader
- **Definition:** The kernel component that validates a package against the meta-schema and writes its entities through normal processes in dependency order.
- **Chapter:** SPEC-28
- **Origin:** CON-UB-031, CON-LC-004, VRD-17-02

### TERM-Kernel
- **Definition:** The platform library (store, meta-model, transactions, runtime, rules, orchestration, security). Hosts embed it.
- **Aliases:** engine (UP); micro-kernel (UC).
- **Chapter:** SPEC-10
- **Origin:** CON-UP-001, CON-UC-001, VRD-19-01

### TERM-Host
- **Definition:** A thin executable that embeds the kernel and exposes transport bindings (server, desktop app, CLI).
- **Aliases:** workspace server (LS); apps (US).
- **Chapter:** SPEC-10, SPEC-31
- **Origin:** CON-LS-001, CON-US-001, VRD-19-01

### TERM-TopologyRole
- **Definition:** A runtime function a node performs: gateway, worker, scheduler, dispatcher, database, runtime store.
- **Chapter:** SPEC-31
- **Origin:** CON-UC-003, VRD-19-03

---

---

## 16. Development method

### TERM-Scenario
- **Definition:** A file of ordered UBTP messages with expected results, shipped in a package and runnable by the CLI. The acceptance unit of a feature.
- **Aliases:** scenario runner (US); declarative test cases (LC).
- **Chapter:** SPEC-28
- **Origin:** CON-US-004, CON-US-043, CON-LC-061, VRD-22-02

### TERM-InstructionFile
- **Definition:** The repository file that tells implementing agents the conventions and rules (`AGENTS.md` / `CLAUDE.md`), derived from `00-meta/AI-GUIDE.md`.
- **Aliases:** coding rules (LC); house rules (UB).
- **Chapter:** SPEC-02
- **Origin:** CON-LC-063, CON-UB-067, CON-UC-065, VRD-22-01
