---
id: UBS-REQ-03
title: Capability Map
status: complete
phase: ALL
depends_on: [UBS-REQ-01, UBS-REQ-02, UBS-META-01]
---

# Capability Map

A **capability** is a stable business or platform ability. Functional requirements refine
capabilities.

## 0. Numbering rule (normative)

Functional requirement numbers encode their capability. The FR ID has the form
`FR-<DOM>-<NN><k>`:
- `<NN>` is the two-digit capability number;
- `<k>` is a sequence digit from 1 to 9.

Example: `FR-VER-051` is the first requirement of `CAP-VER-05`. A capability therefore
holds at most nine requirements. More are expressed by adding a capability.

**Phase** is the earliest phase in which the capability is delivered at Must level. Later
phases may extend it with their own FR items.

Columns: ID · Name · Description · Main personas · Phase · FR range.

## 1. MODEL — Types, classes, inheritance, polymorphism

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-MODEL-01 | Class definition | Business classes are versioned objects with typed fields, keys, labels and documentation | PER-BusinessArchitect | PH-1 | FR-MODEL-011…019 |
| CAP-MODEL-02 | Inheritance | Classes form a single-rooted hierarchy with traits; subclasses narrow and extend | PER-BusinessArchitect | PH-1 | FR-MODEL-021…029 |
| CAP-MODEL-03 | Polymorphism | Ports, rules and views resolve by the instance's actual class, with overriding along the hierarchy | PER-BusinessArchitect, PER-LogicDeveloper | PH-1 | FR-MODEL-031…039 |
| CAP-MODEL-04 | Value types and constraints | Standard value types (text, number, decimal, money, quantity, date, time, instant, period, boolean, enum, reference, composite, list, map, document) with constraints | PER-BusinessArchitect | PH-1 | FR-MODEL-041…049 |
| CAP-MODEL-05 | State kinds | Every class declares its kind: Definition, Ledger, System or RawDocument (IMP-01) | PER-BusinessArchitect | PH-1 | FR-MODEL-051…059 |
| CAP-MODEL-06 | Relationships | Typed, directed relationships with cardinality, inverse names and referential rules | PER-BusinessArchitect | PH-1 | FR-MODEL-061…069 |
| CAP-MODEL-07 | Class extension | Tenants and Buks extend classes (add fields, narrow constraints) without forking | PER-ImplementationConsultant | PH-1 | FR-MODEL-071…079 |
| CAP-MODEL-08 | Schema evolution | Class changes carry migration lenses; old versions are readable in the new shape (IMP-04) | PER-BusinessArchitect, PER-IsvDeveloper | PH-2 | FR-MODEL-081…089 |
| CAP-MODEL-09 | Base ontology | Shared classes: Party, Person, Organisation, Role, Address, ContactPoint, Money, Account, Calendar, Document, Agreement | PER-BusinessArchitect | PH-2 | FR-MODEL-091…099 |
| CAP-MODEL-10 | Model introspection and export | Reflection over the model; export as JSON Schema and OpenAPI (IMP-12) | PER-LogicDeveloper, PER-AiAgent | PH-1 | FR-MODEL-101…109 |

## 2. VER — Versioning, branches, merge, release

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-VER-01 | Commit history | Every state change is a commit in a DAG with parents, author, message and process | all | PH-1 | FR-VER-011…019 |
| CAP-VER-02 | Branches | Zero-copy branches of defined kinds: main, draft, feature, release, agent, overlay, loader | PER-BusinessArchitect, PER-AiAgent | PH-1 | FR-VER-021…029 |
| CAP-VER-03 | Change sets | A change set groups the commits of a branch for review, simulation, approval and merge | PER-BusinessArchitect, PER-Approver | PH-1 | FR-VER-031…039 |
| CAP-VER-04 | Diff | Object, field and semantic diffs between any two states | PER-Approver, PER-BusinessArchitect | PH-1 | FR-VER-041…049 |
| CAP-VER-05 | Merge | Three-way merge of Definition state with complete conflict reports and per-field strategies | PER-BusinessArchitect | PH-1 | FR-VER-051…059 |
| CAP-VER-06 | Tags and releases | Immutable named states; releases are tagged, approved states of a branch | PER-BusinessArchitect, PER-TenantAdministrator | PH-1 | FR-VER-061…069 |
| CAP-VER-07 | Revert and undo | Revert a commit, a change set or a whole process by a compensating commit | PER-BusinessArchitect, PER-AgentSupervisor | PH-1 | FR-VER-071…079 |
| CAP-VER-08 | Branch inheritance | A child branch reads through to its parent for objects it does not override | PER-ImplementationConsultant | PH-1 | FR-VER-081…089 |
| CAP-VER-09 | History tiering | Old history moves to cheaper storage and stays queryable and provable | PER-PlatformOperator | PH-4 | FR-VER-091…099 |
| CAP-VER-10 | Transaction-time travel | Read any object or query at any past commit or instant | PER-InternalAuditor | PH-1 | FR-VER-101…109 |

## 3. TIME — Bitemporal time

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-TIME-01 | Transaction time | Every version records when it became known to the system | all | PH-1 | FR-TIME-011…019 |
| CAP-TIME-02 | Valid time | Definition versions carry the business period in which they are effective (IMP-02) | PER-FundAccountant, PER-ComplianceOfficer | PH-1 | FR-TIME-021…029 |
| CAP-TIME-03 | Bitemporal queries | Queries combine valid time (`asof`) and transaction time (`time`) | PER-InternalAuditor, PER-RegulatorExaminer | PH-1 | FR-TIME-031…039 |
| CAP-TIME-04 | Retroactive correction | Back-dated corrections with automatic identification of affected results and restatement | PER-FundAccountant | PH-2 | FR-TIME-041…049 |
| CAP-TIME-05 | Future-dated change | Changes that become effective at a future valid time without manual action | PER-FundOperationsManager | PH-1 | FR-TIME-051…059 |
| CAP-TIME-06 | Rule-version binding | Results record the rule and logic versions that produced them; re-evaluation uses the versions effective at the relevant valid time | PER-ComplianceOfficer | PH-1 | FR-TIME-061…069 |

## 4. LEDG — Ledger-kind state

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-LEDG-01 | Ledger classes | Append-only classes whose entries are never updated or merged | PER-FundAccountant | PH-1 | FR-LEDG-011…019 |
| CAP-LEDG-02 | Reversal and adjustment | Corrections are new entries referencing the corrected entry | PER-FundAccountant | PH-1 | FR-LEDG-021…029 |
| CAP-LEDG-03 | Balances as projections | Balances and positions are derived projections of ledger entries | PER-FundAccountant | PH-1 | FR-LEDG-031…039 |
| CAP-LEDG-04 | Sequencing | Gap-free, per-ledger sequence numbers and monotonic ordering | PER-InternalAuditor | PH-1 | FR-LEDG-041…049 |
| CAP-LEDG-05 | Double entry | Balanced postings across accounts enforced on commit | PER-FundAccountant | PH-2 | FR-LEDG-051…059 |
| CAP-LEDG-06 | Period close | Accounting periods with close and lock; post-close corrections go to open periods with valid-time references | PER-FundAccountant | PH-2 | FR-LEDG-061…069 |
| CAP-LEDG-07 | Reconciliation | Matching of internal ledger entries with external statements and breaks management | PER-FundAccountant | PH-2 | FR-LEDG-071…079 |

## 5. TXN — Transactions and processes

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-TXN-01 | Process and atomic commit | A process is the unit of work; its changes commit atomically or not at all | — | PH-1 | FR-TXN-011…019 |
| CAP-TXN-02 | Optimistic concurrency | Every write names its base; head moves are detected and reported | — | PH-1 | FR-TXN-021…029 |
| CAP-TXN-03 | Idempotency | Repeated submissions with the same key have exactly one effect | PER-LogicDeveloper | PH-1 | FR-TXN-031…039 |
| CAP-TXN-04 | Provenance and causality | Every commit and event records its process, trigger and dependencies | PER-InternalAuditor | PH-1 | FR-TXN-041…049 |
| CAP-TXN-05 | Post-commit effects | Emits, jobs and external calls are released only after commit (outbox) | — | PH-1 | FR-TXN-051…059 |
| CAP-TXN-06 | Long-running processes | Multi-step processes with compensation and resumption after failure | PER-LogicDeveloper | PH-2 | FR-TXN-061…069 |

## 6. LOGIC — Logic runtime and instruction set

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-LOGIC-01 | Instruction set | Seven versioned instruction families: commit, read, run, emit, search, call, ask | PER-LogicDeveloper, PER-ThirdPartyImplementer | PH-1 | FR-LOGIC-011…019 |
| CAP-LOGIC-02 | Execution context | Immutable `ctx` input and capability gateway `sys`; identity and tenant come only from the context | PER-LogicDeveloper | PH-1 | FR-LOGIC-021…029 |
| CAP-LOGIC-03 | L1 expressions | A declarative expression language for computed fields, conditions and selectors | PER-BusinessAnalyst | PH-1 | FR-LOGIC-031…039 |
| CAP-LOGIC-04 | L2 typed scripts | Rhai scripts type-checked against the model | PER-LogicDeveloper | PH-1 | FR-LOGIC-041…049 |
| CAP-LOGIC-05 | L3 WASM components | Polyglot components with declared capabilities | PER-LogicDeveloper, PER-IsvDeveloper | PH-3 | FR-LOGIC-051…059 |
| CAP-LOGIC-06 | Sandbox and metering | Profiles with limits on operations, memory, time and calls; uncatchable termination | PER-SecurityOfficer | PH-1 | FR-LOGIC-061…069 |
| CAP-LOGIC-07 | Determinism and replay | Recorded non-determinism; any execution can be replayed bit-for-bit | PER-InternalAuditor, PER-KernelEngineer | PH-1 | FR-LOGIC-071…079 |
| CAP-LOGIC-08 | Logic asset lifecycle | Draft → committed → verified → deprecated; only verified logic runs in production branches | PER-LogicDeveloper | PH-1 | FR-LOGIC-081…089 |
| CAP-LOGIC-09 | Static analysis | The immune system rejects forbidden patterns and unbounded constructs before execution | PER-SecurityOfficer | PH-1 | FR-LOGIC-091…099 |
| CAP-LOGIC-10 | Connectors | External calls only through declared connector objects with credentials held by the platform | PER-LogicDeveloper | PH-1 | FR-LOGIC-101…109 |

## 7. RULE — Governance sheets, validation, decisions

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-RULE-01 | Governance sheets | LGS rule blocks: selectors over classes, attributes, tags and context, plus declarations | PER-ComplianceOfficer, PER-BusinessAnalyst | PH-1 | FR-RULE-011…019 |
| CAP-RULE-02 | Cascade and explanation | Deterministic cascade by layer, specificity and priority, with an explanation of every effective value | PER-ComplianceOfficer | PH-1 | FR-RULE-021…029 |
| CAP-RULE-03 | Validation and invariants | Field, object and cross-object rules evaluated on commit | PER-BusinessAnalyst | PH-1 | FR-RULE-031…039 |
| CAP-RULE-04 | Decision tables | Tabular rules with hit policies and completeness checks | PER-BusinessAnalyst | PH-2 | FR-RULE-041…049 |
| CAP-RULE-05 | Policy decisions with veto | Rules that allow, deny or require extra steps for actions | PER-ComplianceOfficer | PH-1 | FR-RULE-051…059 |
| CAP-RULE-06 | Attach, detach and effective periods | Sheets are attached for valid-time periods, as time-boxed campaigns or controls | PER-ComplianceOfficer | PH-2 | FR-RULE-061…069 |
| CAP-RULE-07 | Simulation and impact analysis | Evaluate a rule change against history and live data before publishing | PER-BusinessAnalyst, PER-FundOperationsManager | PH-2 | FR-RULE-071…079 |
| CAP-RULE-08 | Rule testing | Test cases attached to rules and run on every change | PER-BusinessAnalyst | PH-2 | FR-RULE-081…089 |

## 8. FLOW — Lifecycles, actions, workflows, approvals

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-FLOW-01 | Lifecycles | Class state machines with states, transitions, entry and exit effects | PER-BusinessArchitect | PH-1 | FR-FLOW-011…019 |
| CAP-FLOW-02 | Actions (ports) | Typed input ports of a class, with parameters, results and errors | PER-BusinessArchitect | PH-1 | FR-FLOW-021…029 |
| CAP-FLOW-03 | Guards | Preconditions on actions and transitions, with explanations on failure | PER-BusinessAnalyst | PH-1 | FR-FLOW-031…039 |
| CAP-FLOW-04 | Approvals and segregation of duties | Multi-level, rule-driven approvals as deferred commits; maker–checker enforcement | PER-Approver, PER-ComplianceOfficer | PH-2 | FR-FLOW-041…049 |
| CAP-FLOW-05 | Tasks and work queues | Human tasks, assignment rules, claim and release, queues | PER-BusinessUser | PH-2 | FR-FLOW-051…059 |
| CAP-FLOW-06 | Durable workflows | Multi-step flows with timers, waits for events and human steps; resumable | PER-LogicDeveloper | PH-2 | FR-FLOW-061…069 |
| CAP-FLOW-07 | Dialogues | Actions that request additional input interactively before completing | PER-BusinessUser | PH-2 | FR-FLOW-071…079 |
| CAP-FLOW-08 | SLAs and escalation | Due dates, business-calendar deadlines, escalation rules | PER-FundOperationsManager | PH-2 | FR-FLOW-081…089 |
| CAP-FLOW-09 | Delegation | Temporary delegation of tasks and approval authority within policy | PER-Approver | PH-2 | FR-FLOW-091…099 |

## 9. EVT — Events, jobs, schedules, workers

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-EVT-01 | Events | Typed business events emitted after commit, with CloudEvents representation | PER-LogicDeveloper | PH-1 | FR-EVT-011…019 |
| CAP-EVT-02 | Subscriptions | Listeners are versioned objects; "who listens to what" is queryable | PER-LogicDeveloper | PH-1 | FR-EVT-021…029 |
| CAP-EVT-03 | Durable jobs | Jobs are objects with state; queues are only hints; recovery after runtime-store loss | PER-PlatformOperator | PH-1 | FR-EVT-031…039 |
| CAP-EVT-04 | Schedules | Schedule objects (cron and business calendar) with time zones and hot changes | PER-FundOperationsManager | PH-1 | FR-EVT-041…049 |
| CAP-EVT-05 | Retries and dead letters | Retry policies, backoff and dead-letter handling with operator actions | PER-PlatformOperator | PH-1 | FR-EVT-051…059 |
| CAP-EVT-06 | Worker pool | Enterprise-internal workers run verified sandboxed jobs (DEC-002) | PER-PlatformOperator | PH-4 | FR-EVT-061…069 |
| CAP-EVT-07 | Replay and backfill | Re-deliver events to a new subscriber, or backfill projections | PER-LogicDeveloper | PH-2 | FR-EVT-071…079 |

## 10. QRY — Query, search, retrieval

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-QRY-01 | Criteria queries | Typed filters, sorting and projection over classes including subclasses | PER-BusinessUser, PER-LogicDeveloper | PH-1 | FR-QRY-011…019 |
| CAP-QRY-02 | Full-text search | Ranked text search across permitted records and documents | PER-BusinessUser | PH-2 | FR-QRY-021…029 |
| CAP-QRY-03 | Semantic search | Vector search over knowledge and documents for users and agents | PER-AiAgent | PH-3 | FR-QRY-031…039 |
| CAP-QRY-04 | Aggregation | Grouping and aggregate functions with as-of semantics | PER-DataAnalyst | PH-2 | FR-QRY-041…049 |
| CAP-QRY-05 | History queries | Queries over versions, changes and transitions in a time range | PER-InternalAuditor | PH-1 | FR-QRY-051…059 |
| CAP-QRY-06 | Saved queries and list views | Named, shareable, versioned queries | PER-BusinessUser | PH-2 | FR-QRY-061…069 |
| CAP-QRY-07 | Consistent pagination | Snapshot-consistent paging over large results | — | PH-1 | FR-QRY-071…079 |

## 11. IAM — Identity and access

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-IAM-01 | Local authentication | Passwords, passkeys and TOTP for Box and small Server installs; API keys | PER-PersonalUser, PER-TenantAdministrator | PH-1 | FR-IAM-011…019 |
| CAP-IAM-02 | Enterprise SSO | OIDC and SAML federation with the organisation's identity provider | PER-TenantAdministrator | PH-2 | FR-IAM-021…029 |
| CAP-IAM-03 | Principals and keys | Human, service and agent principals with DIDs and signing keys | PER-SecurityOfficer | PH-1 | FR-IAM-031…039 |
| CAP-IAM-04 | Policy authorization | Default-deny policy decisions (roles, attributes, conditions) at every enforcement point (IMP-06) | PER-SecurityOfficer | PH-1 | FR-IAM-041…049 |
| CAP-IAM-05 | Relationship authorization | Permissions derived from relationships in the party and role graph | PER-SecurityOfficer, PER-BusinessArchitect | PH-2 | FR-IAM-051…059 |
| CAP-IAM-06 | Masking and filtering | Field masking and row-level filtering by policy | PER-ComplianceOfficer | PH-2 | FR-IAM-061…069 |
| CAP-IAM-07 | Provisioning | SCIM user and group provisioning and deprovisioning | PER-TenantAdministrator | PH-2 | FR-IAM-071…079 |
| CAP-IAM-08 | Policy analysis and access review | "Who can do what" answers and periodic access reviews | PER-SecurityOfficer, PER-InternalAuditor | PH-3 | FR-IAM-081…089 |
| CAP-IAM-09 | Secrets and keys | Platform-held secrets and keys; logic and agents never see raw secrets | PER-SecurityOfficer | PH-1 | FR-IAM-091…099 |
| CAP-IAM-10 | Sessions and devices | Session policies, device registration, revocation | PER-SecurityOfficer | PH-2 | FR-IAM-101…109 |

## 12. TEN — Tenancy and VAEs

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-TEN-01 | Tenants and VAEs | A tenant owns one or more VAEs; a VAE is a complete logical world | PER-TenantAdministrator | PH-1 | FR-TEN-011…019 |
| CAP-TEN-02 | Isolation | No data, logic, cache or event leaks across tenants | PER-SecurityOfficer | PH-1 | FR-TEN-021…029 |
| CAP-TEN-03 | Overlays | Tenant customisation as overlay branches inheriting vendor releases | PER-ImplementationConsultant | PH-1 | FR-TEN-031…039 |
| CAP-TEN-04 | VAE hierarchy on one node | Parent and child VAEs (headquarters and subsidiaries) with inherited logic and local data | PER-TenantAdministrator | PH-2 | FR-TEN-041…049 |
| CAP-TEN-05 | Tenant lifecycle | Create, configure, suspend, resume, archive and delete tenants | PER-TenantAdministrator | PH-2 | FR-TEN-051…059 |
| CAP-TEN-06 | Portability | Complete tenant export and import in open, verifiable formats | PER-TenantAdministrator | PH-2 | FR-TEN-061…069 |
| CAP-TEN-07 | Placement and migration | Place tenants in cells and move them online (IMP-08) | PER-PlatformOperator | PH-4 | FR-TEN-071…079 |

## 13. AUD — Audit, lineage, explanations

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-AUD-01 | Audit trail | Who, what, when, why, on behalf of whom, through which channel, for every change | PER-InternalAuditor | PH-1 | FR-AUD-011…019 |
| CAP-AUD-02 | Lineage | Trace any value to its inputs, rules, logic versions and actors | PER-InternalAuditor, PER-FundAccountant | PH-2 | FR-AUD-021…029 |
| CAP-AUD-03 | Explanations | Human-readable explanations of denials, validations, computed values and cascade results | PER-BusinessUser, PER-ComplianceOfficer | PH-2 | FR-AUD-031…039 |
| CAP-AUD-04 | Audit views and reports | Standard audit reports and interactive audit views | PER-InternalAuditor | PH-2 | FR-AUD-041…049 |
| CAP-AUD-05 | Access logging | Reads of sensitive classes and fields are logged | PER-ComplianceOfficer | PH-2 | FR-AUD-051…059 |
| CAP-AUD-06 | Legal hold | Holds that suspend retention-based disposal for scoped records | PER-ComplianceOfficer | PH-3 | FR-AUD-061…069 |
| CAP-AUD-07 | Retention and WORM | Retention schedules and write-once storage compliant with SEC 17a-4 | PER-ComplianceOfficer | PH-3 | FR-AUD-071…079 |

## 14. UX — User experience

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-UX-01 | Server-driven UI | Views are data; widgets derive from class, mode and context | PER-BusinessArchitect | PH-2 | FR-UX-011…019 |
| CAP-UX-02 | Workspace shell | Navigation, inbox, global search, notifications, command palette | PER-BusinessUser | PH-2 | FR-UX-021…029 |
| CAP-UX-03 | Forms, lists, details | Standard record interactions with validation feedback | PER-BusinessUser | PH-2 | FR-UX-031…039 |
| CAP-UX-04 | Progressive disclosure | Vocabulary and features per product layer (IMP-11) | all | PH-2 | FR-UX-041…049 |
| CAP-UX-05 | History and as-of viewer | Timeline, as-of browsing and comparison for any record | PER-BusinessUser, PER-InternalAuditor | PH-2 | FR-UX-051…059 |
| CAP-UX-06 | Change review | Diff and simulation review for change sets and approvals | PER-Approver, PER-AgentSupervisor | PH-2 | FR-UX-061…069 |
| CAP-UX-07 | Desktop and mobile clients | Native shells for Box and mobile use | PER-FieldWorker, PER-PersonalUser | PH-4 | FR-UX-071…079 |
| CAP-UX-08 | Offline experience | Offline indicators, queued actions, conflict resolution UI | PER-FieldWorker | PH-4 | FR-UX-081…089 |
| CAP-UX-09 | External portal | Restricted interface for external parties | PER-ExternalParty | PH-3 | FR-UX-091…099 |
| CAP-UX-10 | Theming and branding | Tenant branding and accessible themes | PER-TenantAdministrator | PH-3 | FR-UX-101…109 |

## 15. OFFICE — Office suite on BPUs

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-OFFICE-01 | Live Doc | Documents as views with data bindings and embedded logic | PER-ContractManager | PH-2 | FR-OFFICE-011…019 |
| CAP-OFFICE-02 | Templates and clauses | Clause libraries, templates, conditional sections | PER-ContractManager | PH-5 | FR-OFFICE-021…029 |
| CAP-OFFICE-03 | Redlining and negotiation | Tracked changes between parties with versioned rounds | PER-ContractManager, PER-ExternalParty | PH-5 | FR-OFFICE-031…039 |
| CAP-OFFICE-04 | Smart Grid | Spreadsheet interface in which rows are records and column formulas are shared logic | PER-FundAccountant | PH-2 | FR-OFFICE-041…049 |
| CAP-OFFICE-05 | War Room | Live, drillable, actionable presentations | PER-Executive | PH-3 | FR-OFFICE-051…059 |
| CAP-OFFICE-06 | Action Messages | Messages that carry executable, governed actions | PER-Approver | PH-3 | FR-OFFICE-061…069 |
| CAP-OFFICE-07 | Executable Books | Authoring of knowledge Buks that mix narrative, logic and views | PER-IsvDeveloper, PER-BusinessArchitect | PH-4 | FR-OFFICE-071…079 |
| CAP-OFFICE-08 | Rendering and export | PDF, DOCX, XLSX and HTML output with embedded proofs | PER-ContractManager | PH-2 | FR-OFFICE-081…089 |

## 16. AI — Gateway, agents, copilot, builder

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-AI-01 | Model gateway | Provider-neutral model access with routing, metering, redaction and recording | PER-SecurityOfficer | PH-3 | FR-AI-011…019 |
| CAP-AI-02 | Agent identity and budget | Agents are principals with roles, scopes, budgets and kill switches | PER-AgentSupervisor | PH-3 | FR-AI-021…029 |
| CAP-AI-03 | Agent branches | Agents work in their own branches and submit change sets (IMP-10) | PER-AgentSupervisor, PER-AiAgent | PH-3 | FR-AI-031…039 |
| CAP-AI-04 | Tool exposure | Class ports and queries exposed as typed tools, including over MCP | PER-AiAgent | PH-3 | FR-AI-041…049 |
| CAP-AI-05 | BPU knowledge | The knowledge element of BPU classes: documentation, examples, retrieval index | PER-AiAgent, PER-BusinessUser | PH-3 | FR-AI-051…059 |
| CAP-AI-06 | Copilot | In-context assistance that proposes actions and drafts | PER-BusinessUser | PH-3 | FR-AI-061…069 |
| CAP-AI-07 | AI builder | Generate class, rule, view and Buk drafts from requirements | PER-BusinessArchitect | PH-3 | FR-AI-071…079 |
| CAP-AI-08 | Evaluation and monitoring | Offline evaluation with scenarios; online quality and cost monitoring | PER-AgentSupervisor | PH-3 | FR-AI-081…089 |
| CAP-AI-09 | AI inside logic | The `ask` instruction with recorded results for determinism | PER-LogicDeveloper | PH-3 | FR-AI-091…099 |
| CAP-AI-10 | Governed evolution | BPUs and agents propose class and rule changes on evolution branches; merge only through governance | PER-BusinessArchitect | PH-3 | FR-AI-101…109 |

## 17. PKG — Buks

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-PKG-01 | Buk format | A signed manifest and content: classes, sheets, logic, views, knowledge, seed data, scenarios | PER-IsvDeveloper | PH-2 | FR-PKG-011…019 |
| CAP-PKG-02 | Dependencies | Semantic versions and dependency resolution with conflict detection | PER-IsvDeveloper | PH-2 | FR-PKG-021…029 |
| CAP-PKG-03 | Installation | Install through a loader branch and a governed merge | PER-TenantAdministrator | PH-2 | FR-PKG-031…039 |
| CAP-PKG-04 | Upgrade | Upgrades merge into overlays with conflicts reported per overlay | PER-TenantAdministrator, PER-IsvDeveloper | PH-2 | FR-PKG-041…049 |
| CAP-PKG-05 | Removal and deprecation | Uninstall with data retention rules; deprecation notices | PER-TenantAdministrator | PH-2 | FR-PKG-051…059 |
| CAP-PKG-06 | Verification | Static checks, scenario runs and conformance checks before a Buk may be installed in production | PER-IsvDeveloper | PH-2 | FR-PKG-061…069 |
| CAP-PKG-07 | Genesis Buks | The `system` and `ontology` Buks that bootstrap every VAE | PER-KernelEngineer | PH-1 | FR-PKG-071…079 |
| CAP-PKG-08 | Licence hooks | Licence-check logic and entitlements enforced at run time | PER-IsvDeveloper | PH-4 | FR-PKG-081…089 |

## 18. INT — Integration

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-INT-01 | UBTP protocol | Handshake, Mutate, Emit, Query, Subscribe over TCP, WebSocket, HTTP and in-process | PER-LogicDeveloper | PH-1 | FR-INT-011…019 |
| CAP-INT-02 | REST facade | Generated OpenAPI endpoints per class and port | PER-LogicDeveloper | PH-2 | FR-INT-021…029 |
| CAP-INT-03 | Webhooks | Signed outbound webhooks; verified inbound webhooks | PER-LogicDeveloper | PH-2 | FR-INT-031…039 |
| CAP-INT-04 | Connector framework | Connector objects with credentials, rate limits and recorded exchanges | PER-LogicDeveloper | PH-2 | FR-INT-041…049 |
| CAP-INT-05 | Event streaming out | Publish events to external brokers (Kafka-compatible, cloud queues) as CloudEvents | PER-DataAnalyst | PH-3 | FR-INT-051…059 |
| CAP-INT-06 | Email and chat | Inbound email to records; Action Messages via email and chat tools | PER-BusinessUser | PH-2 | FR-INT-061…069 |
| CAP-INT-07 | Financial adapters | File- and message-level adapters for custodians, banks and fund platforms (ISO 20022, CSV, SFTP) | PER-FundAccountant | PH-3 | FR-INT-071…079 |
| CAP-INT-08 | API credentials | API keys, OAuth client credentials, scopes, rotation | PER-TenantAdministrator | PH-2 | FR-INT-081…089 |

## 19. ANL — Projections, reporting, analytics exit

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-ANL-01 | Declared projections | Classes declare typed projection tables maintained from commits (DEC-011) | PER-LogicDeveloper | PH-1 | FR-ANL-011…019 |
| CAP-ANL-02 | Projection rebuild | Any projection can be rebuilt from authoritative state and verified | PER-PlatformOperator | PH-1 | FR-ANL-021…029 |
| CAP-ANL-03 | Reports | Parameterised, as-of-capable tabular reports | PER-DataAnalyst, PER-FundOperationsManager | PH-2 | FR-ANL-031…039 |
| CAP-ANL-04 | Embedded analytics | A single-tenant analytical engine for heavier queries | PER-DataAnalyst | PH-3 | FR-ANL-041…049 |
| CAP-ANL-05 | CDC export | Incremental export to Parquet or Iceberg with version and lineage columns (IMP-09) | PER-DataAnalyst | PH-3 | FR-ANL-051…059 |
| CAP-ANL-06 | Dashboards | Live metric tiles and charts feeding the War Room | PER-Executive | PH-3 | FR-ANL-061…069 |

## 20. PROOF — Integrity and evidence

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-PROOF-01 | Canonical content addressing | Canonical JSON (RFC 8785) and SHA-256 object identity (IMP-03) | PER-KernelEngineer | PH-1 | FR-PROOF-011…019 |
| CAP-PROOF-02 | Signed commits | Commits signed by the author principal's key | PER-SecurityOfficer | PH-1 | FR-PROOF-021…029 |
| CAP-PROOF-03 | Merkle roots and proofs | A Merkle root per commit; inclusion and consistency proofs | PER-RegulatorExaminer | PH-1 | FR-PROOF-031…039 |
| CAP-PROOF-04 | Anchoring | Periodic anchoring of roots to external timestamping services | PER-ComplianceOfficer | PH-3 | FR-PROOF-041…049 |
| CAP-PROOF-05 | Evidence packs | Self-contained, verifiable bundles for records, periods or questions | PER-RegulatorExaminer, PER-InternalAuditor | PH-3 | FR-PROOF-051…059 |
| CAP-PROOF-06 | Independent verifier | An open-source verifier that needs no vendor services | PER-RegulatorExaminer, PER-ExternalParty | PH-3 | FR-PROOF-061…069 |
| CAP-PROOF-07 | Encrypted fields and crypto-shredding | Field-level encryption with per-subject keys; erasure by key destruction keeps proofs valid | PER-ComplianceOfficer | PH-2 | FR-PROOF-071…079 |

## 21. SYNC — Replication, offline, federation

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-SYNC-01 | Object exchange protocol | Merkle-based exchange of missing objects and commits (IMP-07) | PER-KernelEngineer | PH-4 | FR-SYNC-011…019 |
| CAP-SYNC-02 | Box–Server sync | Personal and field nodes sync with their organisation's Server | PER-FieldWorker, PER-PersonalUser | PH-4 | FR-SYNC-021…029 |
| CAP-SYNC-03 | Offline conflicts | Definition conflicts resolved by merge; ledger entries appended; user resolution UI | PER-FieldWorker | PH-4 | FR-SYNC-031…039 |
| CAP-SYNC-04 | Selective replication | Replicate by scope and data classification | PER-SecurityOfficer | PH-4 | FR-SYNC-041…049 |
| CAP-SYNC-05 | Federation registry | Registration and discovery of VAEs across organisations with DID resolution | PER-TenantAdministrator | PH-5 | FR-SYNC-051…059 |
| CAP-SYNC-06 | Cross-node VAE tree | Parent and child VAEs on different nodes with logic inheritance and network fallback | PER-TenantAdministrator | PH-5 | FR-SYNC-061…069 |
| CAP-SYNC-07 | Shared BPUs | One BPU shared by several organisations, with signed, mutually approved changes | PER-ContractManager, PER-ExternalParty | PH-5 | FR-SYNC-071…079 |
| CAP-SYNC-08 | Browser kernel | The DVM compiled to WASM with local storage in the browser | PER-PersonalUser | PH-5 | FR-SYNC-081…089 |

## 22. OPS — Operations

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-OPS-01 | Installation and bootstrap | Install every edition and bootstrap a VAE from genesis Buks | PER-PlatformOperator, PER-PersonalUser | PH-1 | FR-OPS-011…019 |
| CAP-OPS-02 | Backup and restore | Consistent backups, point-in-time restore, restore verification | PER-PlatformOperator | PH-1 | FR-OPS-021…029 |
| CAP-OPS-03 | Upgrade | Kernel and node upgrades without data conversion downtime | PER-PlatformOperator | PH-2 | FR-OPS-031…039 |
| CAP-OPS-04 | Telemetry | Metrics, traces and logs with OpenTelemetry | PER-PlatformOperator | PH-1 | FR-OPS-041…049 |
| CAP-OPS-05 | Health and diagnostics | Health endpoints, self-checks, diagnostic bundles without business data | PER-PlatformOperator | PH-1 | FR-OPS-051…059 |
| CAP-OPS-06 | Disaster recovery | Replication, failover and documented RPO and RTO | PER-PlatformOperator | PH-2 | FR-OPS-061…069 |
| CAP-OPS-07 | Capacity and tiering | Storage growth management and tiering policies | PER-PlatformOperator | PH-4 | FR-OPS-071…079 |
| CAP-OPS-08 | Fleet management | Control Plane management of nodes, cells and upgrade waves | PER-PlatformOperator | PH-4 | FR-OPS-081…089 |
| CAP-OPS-09 | Configuration management | Versioned node configuration with validation and hot reload | PER-PlatformOperator | PH-1 | FR-OPS-091…099 |

## 23. BILL — Metering, licensing, billing (fiat only)

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-BILL-01 | Metering | Usage records for compute, storage, AI, API calls and Buk usage | PER-TenantAdministrator | PH-4 | FR-BILL-011…019 |
| CAP-BILL-02 | Plans and entitlements | Plans, quotas and feature entitlements per tenant | PER-TenantAdministrator | PH-4 | FR-BILL-021…029 |
| CAP-BILL-03 | Licences | Buk and kernel licences, keys and offline licences for Server and Box | PER-IsvDeveloper, PER-TenantAdministrator | PH-4 | FR-BILL-031…039 |
| CAP-BILL-04 | Invoicing and payouts | Customer invoices and publisher payouts in fiat currency | PER-ExchangeOperator | PH-4 | FR-BILL-041…049 |
| CAP-BILL-05 | Cost controls | Budgets, alerts and hard caps per tenant, VAE and agent | PER-TenantAdministrator | PH-4 | FR-BILL-051…059 |

## 24. DEV — Developer experience

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-DEV-01 | CLI | The `forge` CLI for project, check, test, simulate, pack, publish | PER-LogicDeveloper | PH-1 | FR-DEV-011…019 |
| CAP-DEV-02 | Local environment | A headless Box for local development with seeded data | PER-LogicDeveloper | PH-1 | FR-DEV-021…029 |
| CAP-DEV-03 | Type generation | Types generated from the model for scripts, WASM and SDKs | PER-LogicDeveloper | PH-1 | FR-DEV-031…039 |
| CAP-DEV-04 | Simulator | Replay history and scenarios on a branch with changed logic | PER-LogicDeveloper, PER-BusinessAnalyst | PH-2 | FR-DEV-041…049 |
| CAP-DEV-05 | Test framework | Unit, rule and scenario tests with deterministic runs | PER-LogicDeveloper | PH-1 | FR-DEV-051…059 |
| CAP-DEV-06 | SDKs | TypeScript and Rust client SDKs over UBTP | PER-LogicDeveloper | PH-1 | FR-DEV-061…069 |
| CAP-DEV-07 | IDE integration | Language server for LGS, expressions and scripts | PER-LogicDeveloper | PH-2 | FR-DEV-071…079 |
| CAP-DEV-08 | Documentation generation | Generated reference docs for Buks and models | PER-IsvDeveloper | PH-2 | FR-DEV-081…089 |
| CAP-DEV-09 | Debugging and tracing | Step traces of logic executions and rule cascades | PER-LogicDeveloper | PH-2 | FR-DEV-091…099 |

## 25. MIG — Migration and import

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-MIG-01 | Spreadsheet import | Infer classes and import records from XLSX and CSV | PER-ImplementationConsultant, PER-PersonalUser | PH-2 | FR-MIG-011…019 |
| CAP-MIG-02 | Document import | Convert DOCX documents into Live Doc templates with bindings | PER-ContractManager | PH-5 | FR-MIG-021…029 |
| CAP-MIG-03 | Legacy bulk load | Bulk load with historical versions and valid times | PER-ImplementationConsultant | PH-2 | FR-MIG-031…039 |
| CAP-MIG-04 | Mapping definitions | Versioned source-to-class mappings and transformations | PER-ImplementationConsultant | PH-2 | FR-MIG-041…049 |
| CAP-MIG-05 | Migration reconciliation | Control totals and record-level reconciliation reports | PER-ImplementationConsultant, PER-InternalAuditor | PH-2 | FR-MIG-051…059 |

## 26. NTF — Notifications

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-NTF-01 | Notification rules | Rules that create notifications from events and deadlines | PER-BusinessArchitect | PH-2 | FR-NTF-011…019 |
| CAP-NTF-02 | Channels | In-app, email, mobile push and chat channels | PER-BusinessUser | PH-2 | FR-NTF-021…029 |
| CAP-NTF-03 | Preferences and digests | User preferences, quiet hours, digests | PER-BusinessUser | PH-2 | FR-NTF-031…039 |

## 27. FILE — Files and attachments

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-FILE-01 | Blob storage | Content-addressed file storage linked to objects and versions | PER-BusinessUser | PH-1 | FR-FILE-011…019 |
| CAP-FILE-02 | Content safety | Malware scanning, type checks and size limits | PER-SecurityOfficer | PH-2 | FR-FILE-021…029 |
| CAP-FILE-03 | Previews and extraction | Previews, text extraction and indexing | PER-BusinessUser | PH-2 | FR-FILE-031…039 |
| CAP-FILE-04 | VFS paths | Human-readable path aliases over objects (`/data`, `/usr`, `/etc`, `/view`, `/lib`) | PER-LogicDeveloper | PH-2 | FR-FILE-041…049 |

## 28. SIGN — Signatures

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-SIGN-01 | Electronic signatures | E-SIGN and UETA compliant consent, intent and records | PER-ContractManager, PER-ExternalParty | PH-5 | FR-SIGN-011…019 |
| CAP-SIGN-02 | Document seals | Cryptographic binding of a signature to the document state and data snapshot | PER-ContractManager | PH-5 | FR-SIGN-021…029 |
| CAP-SIGN-03 | Signing ceremonies | Multi-party, ordered or parallel signing with reminders | PER-ContractManager | PH-5 | FR-SIGN-031…039 |
| CAP-SIGN-04 | External signature providers | Integration with advanced and qualified signature providers | PER-ContractManager | PH-5 | FR-SIGN-041…049 |

## 29. LOC — Localisation, calendars, currencies

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-LOC-01 | Languages | Multi-language UI and translatable model labels and content | PER-BusinessUser | PH-2 | FR-LOC-011…019 |
| CAP-LOC-02 | Currencies and rates | Currencies and bitemporal FX rates | PER-FundAccountant | PH-2 | FR-LOC-021…029 |
| CAP-LOC-03 | Time zones | Instants in UTC; zone-aware display and scheduling | all | PH-1 | FR-LOC-031…039 |
| CAP-LOC-04 | Business calendars | Holiday calendars, business days and cut-offs | PER-FundOperationsManager | PH-2 | FR-LOC-041…049 |
| CAP-LOC-05 | Formats | Locale-aware number, date and address formats | PER-BusinessUser | PH-2 | FR-LOC-051…059 |

## 30. STD — Governance of the standard

| ID | Name | Description | Personas | Phase | FR range |
|---|---|---|---|---|---|
| CAP-STD-01 | Standard versioning | Versioned BPA releases, errata and change logs | PER-StandardsSteward | PH-0 | FR-STD-011…019 |
| CAP-STD-02 | Conformance vectors | Language-neutral vectors and a reference runner | PER-ThirdPartyImplementer | PH-0 | FR-STD-021…029 |
| CAP-STD-03 | Formal models | Formal models of commit, merge, bitemporal and sync semantics | PER-KernelEngineer | PH-0 | FR-STD-031…039 |
| CAP-STD-04 | Compatibility policy | ABI stability, deprecation windows, extension rules | PER-ThirdPartyImplementer | PH-0 | FR-STD-041…049 |
| CAP-STD-05 | Certification | The "BPU Compliant" certification programme | PER-StandardsSteward, PER-ThirdPartyImplementer | PH-4 | FR-STD-051…059 |
| CAP-STD-06 | Reference licensing | Licensing of the reference DVM for embedding | PER-ThirdPartyImplementer | PH-4 | FR-STD-061…069 |
| CAP-STD-07 | Extension registry | Registered vendor extensions (instructions, value types, declarations) | PER-StandardsSteward | PH-4 | FR-STD-071…079 |

## 31. Summary

| Domain | Capabilities | Earliest phase | Domain | Capabilities | Earliest phase |
|---|---|---|---|---|---|
| MODEL | 10 | PH-1 | INT | 8 | PH-1 |
| VER | 10 | PH-1 | ANL | 6 | PH-1 |
| TIME | 6 | PH-1 | PROOF | 7 | PH-1 |
| LEDG | 7 | PH-1 | SYNC | 8 | PH-3 |
| TXN | 6 | PH-1 | OPS | 9 | PH-1 |
| LOGIC | 10 | PH-1 | BILL | 5 | PH-4 |
| RULE | 8 | PH-1 | DEV | 9 | PH-1 |
| FLOW | 9 | PH-1 | MIG | 5 | PH-2 |
| EVT | 7 | PH-1 | NTF | 3 | PH-2 |
| QRY | 7 | PH-1 | FILE | 4 | PH-1 |
| IAM | 10 | PH-1 | SIGN | 4 | PH-2 |
| TEN | 7 | PH-1 | LOC | 5 | PH-1 |
| AUD | 7 | PH-1 | STD | 7 | PH-0 |
| UX | 10 | PH-2 | AI | 10 | PH-3 |
| OFFICE | 8 | PH-2 | PKG | 8 | PH-1 |

Total: **220 capabilities** across 30 domains.
