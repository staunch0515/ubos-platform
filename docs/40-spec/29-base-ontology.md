---
id: SPEC-29
title: "Base Ontology"
status: complete
phase: P4
depends_on: [SPEC-02, SPEC-13, SPEC-18, SPEC-22, SPEC-25, SPEC-28]
sources: [UP, LC, UB, UC, US, LS]
---

# Base Ontology

## 0. Chapter header

- **Scope:** This chapter defines the base domain library:
  - the semantic value types: abstract business bases, concrete semantic types and measures;
  - the universal business ontology:
    - party, role and relationship;
    - contact and geography;
    - reference data;
    - documents and messages;
    - IT assets and PKI;
  - the platform relationship-type catalogue;
  - how types are split between the `system` and `ontology` root packages;
  - the normalisation table from corpus codes to spec codes;
  - rules for vertical domain packs.
- **MOD:** `ONT`
- **depends_on:** SPEC-02, SPEC-13, SPEC-18, SPEC-22, SPEC-25, SPEC-28.
- **Terms used:** TERM-SemanticType, TERM-Primitive, TERM-Ontology, TERM-Relationship, TERM-RelationshipType, TERM-Template, TERM-Package.
- **Origin summary:**
  - Verdicts: VRD-02-01, VRD-02-06, VRD-17-01.
  - UB: the layered semantic type system and the universal business ontology, 112 boot types (CON-UB-013, 014, 057).
  - UC: the genesis semantic ontology, 15 files and about 150 definitions (CON-UC-018).
  - US: genesis deltas with `ui_modes` (CON-US-015, 041).
  - LC: the relationship-type catalogue with 22 types (CON-LC-014).
  - UP: typed money (CON-UP-016).
  - LS: relation types such as `prop_email` (CON-LS-012, 015).

---

## 1. Concepts

- **"UBOS does not define *customer*, it defines Party + Role(Customer)"** (UB). Verticals assemble their domain from the ontology through `_extends`, TypeExtensions and relationships. They never re-define people, organisations, money or addresses.
- **Semantic types carry behaviour.** Validation, masking, formatting, widgets and index rules come from the value type. When one definition changes, every usage changes with it (UB, UC; SPEC-13 REQ-META-015).
- **The ontology is data.** It ships as the `ontology` root package (REQ-PKG-040). It is versioned, upgradable by merge, and each tenant can adapt it through overlay patches and extensions (SPEC-23).

### 1.1 Package split

| Package | Contains |
|---|---|
| `system` | meta-model (SPEC-13); L2 primitives; the value types needed by the kernel (Text, Code, Email, Url, Money, Password, Uuid, Semver, CronExpression, Json, FileRef); platform entity types of all chapters (Branch, Tag, Context, Logic, Native, Action, Pipeline, Workflow, ProcessInstance, Lifecycle, Query, Constraint, Job, Hook, Cron, Webhook, Notification, User, ServiceAccount, Group, Role, Policy, ApiKey, Secret, IdentityProvider, ApprovalRequest, Tenant, Namespace, Package, Widget, View, UiMatrix, Shell, App, CommandGrammar, AiProvider, AiRouting, AiBuildSession, Agent, I18nKey, File, MessageTemplate, MailTransport, BillingBatch, AuditSeal, Sequence); platform RelationshipTypes (SPEC-22 §2.2); generic actions; kernel queries; the standard widgets; default shells |
| `ontology` | every other semantic value type (§2); business entity types (§3); business RelationshipTypes (§4); dictionaries (§5) |

---

## 2. Semantic value types

All codes are root codes (no namespace, REQ-CONV-018). The UI column gives the `edit` / `view` / `cell` widgets. Masking uses `masking_rule` (SPEC-22 REQ-SEC-021). Sensitivity uses the property default (SPEC-13 §2.3).

### 2.1 Abstract business bases (L3)

| Type | Extends | Adds |
|---|---|---|
| `AbstractText` | `Text` | `constraints.max_length` default 255; `searchable` default true; index slot: lower-case |
| `AbstractMath` | `Number` | `constraints.scale`; `unit` optional; widgets NumberInput/NumberLabel |
| `AbstractEnum` | `Text` | requires `constraints.dict` or `values`; widgets Select/Badge |
| `AbstractFile` | `Reference` | `target: File`; constraints `max_size`, `mime_types` (in `config`) |
| `AbstractComposite` | `Map` | requires `constraints.shape` (composite values such as Money) |
| `Measure` | `AbstractComposite` | shape `{value: decimal string, unit: UOM code}`; a unit-safe algebra in rules (REQ-RULE-026) |

### 2.2 Concrete semantic types (L4)

| Type | Extends | Constraints / shape | Masking | Sensitivity | UI (edit / view / cell) |
|---|---|---|---|---|---|
| `Name` | AbstractText | max 200 | – | NONE | TextInput / TextLabel / TextCell |
| `PersonName` | Name | – | – | PERSONAL | TextInput / TextLabel / TextCell |
| `Code` | AbstractText | `^[A-Za-z0-9._-]{1,64}$` | – | NONE | TextInput / TextLabel / Badge |
| `LongText` | Text | max 100 000; not searchable by default | – | NONE | TextArea / TextLabel / TextCell (truncated) |
| `RichText` | LongText | Markdown | – | NONE | MarkdownEditor / MarkdownView |
| `Email` | AbstractText | format `email`, max 254; index lower-case | `email` (a•••@x.com) | PERSONAL | EmailInput / TextLabel |
| `Phone` | AbstractText | E.164 `^\+[1-9][0-9]{6,14}$` | `keep_last:4` | PERSONAL | PhoneInput / TextLabel |
| `Url` | AbstractText | format `uri`, max 2048 | – | NONE | UrlInput / RefLink |
| `Money` | AbstractComposite | shape `{amount: decimal string, currency: Currency code}`; amount scale from currency (default 2) | – | NONE | MoneyInput / MoneyLabel |
| `Percentage` | AbstractMath | scale 4; 1 = 100 % in storage; display ×100 | – | NONE | PercentInput / NumberLabel |
| `Quantity` | Measure | unit any | – | NONE | NumberInput + unit / NumberLabel |
| `Length`, `Mass`, `Volume`, `Area`, `Duration`, `Temperature`, `Speed`, `Energy`, `Power`, `DataSize` | Measure | unit restricted to the dimension's UOMs (§5) | – | NONE | as Quantity |
| `Enum` | AbstractEnum | `dict` required | – | NONE | Select / Badge |
| `Tag` | AbstractText | max 64, lower-case | – | NONE | TagInput / Badge |
| `Color` | AbstractText | `^#[0-9a-fA-F]{6}$` | – | NONE | ColorPicker / Badge |
| `Semver` | AbstractText | SemVer 2.0 regex | – | NONE | TextInput / Badge |
| `Uuid` | AbstractText | UUID regex | – | NONE | TextInput / CodeBlock |
| `Ipv4`, `Ipv6`, `MacAddress`, `Hostname` | AbstractText | respective regexes | – | NONE | TextInput / TextLabel |
| `HashDigest` | AbstractText | hex, 32–128 chars | – | NONE | TextInput / CodeBlock |
| `PemText` | LongText | PEM armour | – | NONE | TextArea / CodeBlock |
| `CronExpression` | AbstractText | cron grammar (SPEC-20) | – | NONE | TextInput / TextLabel |
| `Password` | AbstractText | min 12; transform `hash_argon2`; read denied | `full` | SECRET | PasswordInput / – |
| `NationalId` | AbstractText | per-country pattern via `config.country` | `keep_last:2` | PERSONAL | TextInput / MaskedLabel |
| `PassportNumber` | AbstractText | `^[A-Z0-9]{5,20}$` | `keep_last:2` | PERSONAL | TextInput / MaskedLabel |
| `CreditCardNumber` | AbstractText | Luhn check; 12–19 digits | `keep_last:4` | SECRET (PCI) | TextInput / MaskedLabel |
| `Iban` | AbstractText | IBAN checksum | `keep_last:4` | PERSONAL | TextInput / MaskedLabel |
| `Sku` | Code | – | – | NONE | TextInput / Badge |
| `PostalCode` | AbstractText | per-country pattern via `config.country` | – | PERSONAL (with address) | TextInput / TextLabel |
| `Latitude` / `Longitude` | AbstractMath | min −90/−180, max 90/180, scale 7 | – | PERSONAL (person location) | NumberInput / NumberLabel |
| `GeoPoint` | AbstractComposite | shape `{lat: Latitude, lon: Longitude}` | – | as above | MapPicker (fallback JsonEditor) / MapView |
| `DateRange` | AbstractComposite | shape `{from: Date, to: Date}`, from ≤ to | – | NONE | DateRangePicker / DateLabel |
| `BirthDate` | Date | max = today | `year_only` | PERSONAL | DatePicker / DateLabel |
| `FileRef` | AbstractFile | – | – | NONE | FileUpload / FileLink |
| `ImageRef` | FileRef | mime `image/*` | – | NONE | FileUpload / ImageView / Avatar |

- REQ-ONT-001 (below) fixes this table as the minimum content.
- Adding semantic types is allowed through packages. Changing the semantics of an existing root type is a MAJOR ontology release.

---

## 3. Business entity types

### 3.1 Party, role, relationship

```yaml
ENT-Party:
  purpose: Abstract base of persons and organisations (UB/UC party model).
  origin: [VRD-02-06, CON-UB-014, CON-UC-018]
  fields:
    - {name: title, type: Name, required: true, description: "Display name"}
    - {name: tax_ids, type: "list<{scheme: Code, value: Code}>?", required: false, description: "Tax/registration identifiers"}
    - {name: preferred_locale, type: Code?, required: false, description: "Locale"}
    - {name: notes, type: RichText?, required: false, description: "Notes"}
  invariants:
    - Abstract; instantiate Person or Organization.
```

| Type | Extends | Key properties |
|---|---|---|
| `Party` | Entity | as above (abstract) |
| `Person` | Party | `given_name` PersonName, `family_name` PersonName, `birth_date` BirthDate?, `gender` Enum(`gender`)?, `national_id` NationalId?, `photo` ImageRef? |
| `Organization` | Party | `legal_name` Name, `kind` Enum(`organization_kind`), `registration_number` Code?, `industry` Enum(`industry`)?, `parent` Reference(Organization)? |
| `LegalOrganization` | Organization | `incorporation_country` Reference(Country), `incorporated_on` Date? |
| `OrganizationUnit` | Organization | `unit_kind` Enum(`org_unit_kind`: DEPARTMENT, TEAM, BRANCH_OFFICE …) |
| `PartyRole` | Entity (abstract) | `party` Reference(Party) required, `organization` Reference(Organization)? (the organisation in whose context the role exists), `valid` DateRange?, `status` Enum(`role_status`) |
| `Customer` | PartyRole | `customer_number` Code (unique TENANT), `credit_limit` Money?, `payment_terms` Enum(`payment_terms`)? |
| `Supplier` | PartyRole | `supplier_number` Code (unique), `payment_terms`?, `rating` Enum? |
| `Employee` | PartyRole | `employee_number` Code (unique), `job_title` Name?, `hired_on` Date?, `manager` Reference(Employee)? |
| `ContactPerson` | PartyRole | `function` Name? |
| `PartnerRole` | PartyRole | `partner_kind` Enum? |

- Party-to-party links with attributes or validity use Relationship entities with the RelationshipTypes of §4, such as WORKS_FOR, REPORTS_TO, SUBSIDIARY_OF and IS_MARRIED_TO (VRD-02-03).
- Role-specific business attributes stay on PartyRole entities.
- `User` (system package) links to `Person` through `person`, and a login is not a person (UB: UserLogin ≠ Person).

### 3.2 Contact and geography

| Type | Extends | Key properties |
|---|---|---|
| `ContactMech` | Entity (abstract) | `label` Name? |
| `EmailAddress` | ContactMech | `email` Email (searchable) |
| `TelecomNumber` | ContactMech | `number` Phone, `kind` Enum(`telecom_kind`: MOBILE, LANDLINE, FAX) |
| `PostalAddress` | ContactMech | `lines` list<Text> (max 4, PERSONAL), `city` Name, `postal_code` PostalCode, `region` Reference(Region)?, `country` Reference(Country), `location` GeoPoint? |
| `WebAddress` | ContactMech | `url` Url |
| `GeoBoundary` | Entity (abstract) | `code` Code, `name` Name, `parent` Reference(GeoBoundary)?, `boundary` Json? (GeoJSON) |
| `Country` | GeoBoundary | slug = ISO 3166-1 alpha-2 (e.g. `CN`), `iso3` Code, `numeric` Code, `phone_prefix` Code, `currency` Reference(Currency) |
| `Region` | GeoBoundary | province or state; slug `<CC>-<code>` (ISO 3166-2) |
| `City` | GeoBoundary | – |
| `District` | GeoBoundary | – |

- Contacts are linked to parties with `HAS_CONTACT` relationships. The metadata is `{purpose: Enum(contact_purpose: PRIMARY, BILLING, SHIPPING, WORK, HOME), is_default: bool}`, and validity is allowed (UB PartyContactMap).

### 3.3 Reference data

| Type | Key properties | Notes |
|---|---|---|
| `Currency` | slug = ISO 4217 (e.g. `CNY`), `name`, `symbol`, `minor_units` Integer (scale), `numeric` Code | seeds: all active ISO 4217 codes |
| `ExchangeRate` | `from` Reference(Currency), `to` Reference(Currency), `rate` Number (scale 6), `valid_from` Timestamp, `source` Code | queried as-of a time (REQ-QRY-003) |
| `Uom` | slug = UCUM-like code (`kg`, `m`, `h`, `GiB`), `name`, `dimension` Enum(`uom_dimension`), `is_base` Boolean | seeds per dimension |
| `UomConversion` | `from` Reference(Uom), `to` Reference(Uom), `factor` Number (scale 10), `offset` Number? | used by `convert()` in rules and formatting |
| `PeriodType` | slug (`MONTH`, `QUARTER`, `FISCAL_YEAR` …), `months` Integer? | – |
| `Period` | `period_type` Reference, `range` DateRange, `code` Code (e.g. `2026-Q3`) | – |
| `Calendar` | `timezone` Code, `working_days` list<Integer> | – |
| `Holiday` | `calendar` Reference(Calendar), `date` Date, `name` Name | – |
| `Sequence` (system) | `prefix`, `next` Integer, `padding`, `reset` Enum(NEVER, YEARLY, MONTHLY) | used by `default.sequence@v1`; next value advanced with CAS |

### 3.4 Documents and messages

| Type | Key properties | Notes |
|---|---|---|
| `File` (system) | `filename`, `mime_type`, `size` DataSize, `sha256` HashDigest, `blob` uri (blob://) | REQ-STO-018 (UC VFS as entities) |
| `Document` | `title`, `kind` Enum(`document_kind`), `files` list<FileRef>, `body` RichText?, `parties` list<Reference(Party)> | – |
| `Book` | `title`, `summary` RichText, `chapters` list<Reference(Page)> (merge_key `slug`) | templates and instances by the prototype chain (TERM-Template, CON-UB-019); an instance `_extends` its template |
| `Page` | `book` Reference(Book), `order` Integer, `body` RichText, `vars` Map | executable book pages may bind actions (P2, CON-UB-055) |
| `Message` | `subject`, `body` RichText, `from` Reference(Party)?, `to` list<Reference(Party)>, `channel` Enum(EMAIL, SMS, CHAT, INTERNAL), `sent_at` Timestamp? | – |
| `MessageTemplate` (system) | `channel`, `subject` I18n text, `body` I18n text (ICU placeholders), `format` Enum(TEXT, MARKDOWN, HTML) | mail and notifications (SPEC-20) |

### 3.5 IT assets and PKI

| Type | Key properties |
|---|---|
| `ItAsset` (abstract) | `name`, `serial` Code?, `owner` Reference(Party)?, `location` Reference(PostalAddress)? |
| `Host` | `hostname` Hostname, `os` Name, `ip_addresses` list<Ipv4 or Ipv6> |
| `NetworkInterface` | `host` Reference(Host), `mac` MacAddress, `ip` Ipv4? |
| `Disk` / `Volume` | `host` Reference(Host), `capacity` DataSize |
| `ServiceEndpoint` | `host` Reference(Host), `port` Integer, `protocol` Enum |
| `CryptoKey` | `algorithm` Enum, `public_key` PemText, `private_key` Reference(Secret)? |
| `Certificate` | `subject` Name, `issuer` Reference(CertificateAuthority)?, `pem` PemText, `valid` DateRange, `fingerprint` HashDigest |
| `CertificateAuthority` | `certificate` Reference(Certificate), `crl_url` Url? |

- UC's IT monitoring samples (`heartbeat`, `process`, `port`) are not ontology types. Time series belong in telemetry (SPEC-30), not in versioned entities.

---

## 4. Relationship types (ontology)

Generalised from LC's catalogue of 22 types (CON-LC-014). Platform types (HAS_ROLE, MEMBER_OF, GRANTS_ACCESS_TO, DELEGATES_TO, TENANT_GRANT) are in the system package (SPEC-22 §2.2).

| Code | Source → Target | Symmetric | Validity | Metadata | Corpus |
|---|---|---|---|---|---|
| `WORKS_FOR` | Person → Organization | no | optional | `{job_title?, employee?: uri<Employee>}` | LC WORKS_FOR |
| `REPORTS_TO` | Employee → Employee | no | optional | – | LC REPORTS_TO |
| `SUBSIDIARY_OF` | Organization → Organization | no | optional | `{ownership?: Percentage}` | UB org hierarchy |
| `PART_OF` | OrganizationUnit → Organization | no | optional | – | UC org_group |
| `IS_MARRIED_TO` | Person ↔ Person | yes | required | – | LC IS_MARRIED_TO |
| `RELATED_TO` | Party ↔ Party | yes | optional | `{kind: Enum(party_relation_kind)}` | UB party relationship |
| `HAS_CONTACT` | Party → ContactMech | no | optional | `{purpose, is_default}` | UB/UC party_contact_map |
| `OWNS` | Party → Entity | no | optional | `{share?: Percentage}` | LC OWNS |
| `CONTAINS` | Entity → Entity | no | – | `{order?: Integer}` | LC CONTAINS |
| `DEPENDS_ON` | ItAsset → ItAsset | no | – | – | LC DEPENDS_ON |
| `LOCATED_AT` | Party/ItAsset → PostalAddress | no | optional | – | NEW |
| `CUSTOMER_OF` | Party → Organization | no | optional | `{customer: uri<Customer>}` | UB roles-between-parties |
| `SUPPLIER_OF` | Party → Organization | no | optional | `{supplier: uri<Supplier>}` | UB |
| `HAS_VALIDITY_PERIOD` | Entity → Period | no | – | – | LC HAS_VALIDITY_PERIOD |
| `FINANCED_BY` | Entity → Party | no | optional | `{amount?: Money}` | LC FINANCED_BY |
| `REFERENCES` | Document/Message → Entity | no | – | – | NEW |

- LC's domain-specific types (ATTEMPTS, FOR_QUIZ, ANSWER_TO, SELECTED_OPTION, HAS_WALLET, AFFECTS_WALLET, HAS_OPTION) belong in vertical packs, not in the ontology.

---

## 5. Dictionaries

The ontology ships Dictionary entities (SPEC-13 §2.8). Each entry has an i18n title.

| Dictionary | Entries (abridged) |
|---|---|
| `gender` | FEMALE, MALE, OTHER, UNDISCLOSED |
| `organization_kind` | COMPANY, NON_PROFIT, GOVERNMENT, EDUCATION, OTHER |
| `org_unit_kind` | DEPARTMENT, TEAM, BRANCH_OFFICE, PROJECT |
| `role_status` | ACTIVE, INACTIVE, PROSPECT, BLOCKED |
| `payment_terms` | PREPAID, NET_15, NET_30, NET_60, ON_DELIVERY |
| `telecom_kind` | MOBILE, LANDLINE, FAX |
| `contact_purpose` | PRIMARY, BILLING, SHIPPING, WORK, HOME |
| `document_kind` | CONTRACT, INVOICE, RECEIPT, REPORT, POLICY, OTHER |
| `uom_dimension` | LENGTH, MASS, VOLUME, AREA, DURATION, TEMPERATURE, SPEED, ENERGY, POWER, DATA_SIZE, COUNT |
| `industry` | ISIC Rev.4 sections A–U (open dictionary) |
| `party_relation_kind` | FAMILY, FRIEND, BUSINESS, OTHER (open) |

---

## 6. Behavior

### REQ-ONT-001 — Minimum content
- **Statement:** The `ontology` package MUST contain the types of §2.2 (except those placed in `system`) and §3, the RelationshipTypes of §4, the Dictionaries of §5, and seed data for Currency (active ISO 4217 codes), Country (ISO 3166-1), Uom and UomConversion (SI plus common units per dimension), and PeriodType. The package installs with REQ-PKG-041 and validates against the meta-schema.
- **Origin:** VRD-02-06, CON-UB-014, CON-UC-018
- **Acceptance:** After genesis, `ubos://logrums/Currency/CNY` exists with `minor_units: 2`, and `Type/Customer` extends `PartyRole`.
- **Priority:** P1

### REQ-ONT-002 — Normalisation of corpus codes
- **Statement:** The corpus genesis data (UB `data/boot/init.json`; UC/US `docs/db/genesis_data/01…17`; LS `genesis_data/01…07`) MUST be converted into the `system` and `ontology` packages using the mapping below. The table is the only place where corpus codes appear (REQ-CONV-019). Unmapped corpus codes are either dropped with a reason or moved into example vertical packs.

| Corpus code(s) | Spec code | Note |
|---|---|---|
| `sys.entity.root` (UB), `root` (UC), `MetaType/MetaType` (LS) | `Entity` | root |
| `sys.meta.type`, `meta`, `MetaType` | `Type` | meta type |
| `sys.type.string` / `string`; `text` | `Text`; `LongText` | primitive / semantic |
| `number`, `integer`, `boolean`, `date`, `map`, `list` | `Number`, `Integer`, `Boolean`, `Date` (plus `Timestamp`), `Map`, `List` | primitives |
| `relation` | `Reference` | primitive |
| `sys.biz.abstract.text` / `abstract_text` | `AbstractText` | – |
| `sys.biz.abstract.math` / `abstract_math` | `AbstractMath` | – |
| `sys.biz.abstract.enum` / `abstract_enum`, `enum_single`, `enum_multi` | `AbstractEnum`, `Enum` (+ `many: true` for multi) | – |
| `sys.biz.abstract.file`, `file_ref`, `image_ref` | `AbstractFile`, `FileRef`, `ImageRef` | – |
| `switch` | `Boolean` with widget Switch | dropped as a type |
| `sys.biz.text.name` / `name` | `Name` / `PersonName` | by usage |
| `code`, `sku` | `Code`, `Sku` | – |
| `sys.biz.text.email` / `email`, `prop_email` (LS) | `Email` | – |
| `url`, `phone` | `Url`, `Phone` | – |
| `sys.biz.text.memo`, `.richtext` | `LongText`, `RichText` | – |
| `sys.biz.math.money` / `money`, `type.primitive.money` (UP) | `Money` | composite |
| `percent`, `sys.biz.math.percentage` | `Percentage` | – |
| `file_size` | `DataSize` | Measure |
| `sys.biz.meas.*` (e.g. `weight.kg`), `measure`, length … power | `Mass`, `Length`, …, with `unit` | Measure |
| `ipv4`, `ipv6`, `mac`, `semver`, `color`, `hash`, `pem`, `uuid` | `Ipv4`, `Ipv6`, `MacAddress`, `Semver`, `Color`, `HashDigest`, `PemText`, `Uuid` | – |
| `password` | `Password` | argon2id instead of bcrypt |
| `passport`, `ssn` | `PassportNumber`, `NationalId` | – |
| `sys.biz.fin.credit_card` / `credit_card`, `iban` | `CreditCardNumber`, `Iban` | – |
| `sys.biz.geo.zipcode` / `zipcode`, `lat`, `long` | `PostalCode`, `Latitude`, `Longitude` (+ `GeoPoint`) | – |
| `geo_boundary`, `country`, `province`, `city`, `district` | `GeoBoundary`, `Country`, `Region`, `City`, `District` | – |
| `contact_mech`, email/telecom/postal/web mech | `ContactMech`, `EmailAddress`, `TelecomNumber`, `PostalAddress`, `WebAddress` | – |
| `party`, `person`, `organization`, `org_legal`, `org_group` | `Party`, `Person`, `Organization`, `LegalOrganization`, `OrganizationUnit` | – |
| `party_role`, `employee`, `customer`, `supplier` | `PartyRole`, `Employee`, `Customer`, `Supplier` | – |
| `party_relationship`, `party_contact_map` | Relationship with §4 types; `HAS_CONTACT` | entities → relationships |
| `msg_root`, templates | `Message`, `MessageTemplate` | – |
| book/page templates and instances | `Book`, `Page` (templates via `_extends`) | – |
| `document`, `avatar` | `Document`, `ImageRef` | – |
| `user_login`, `sec_group`, maps, `permission` | `User`, `Group`, HAS_ROLE/MEMBER_OF relationships, `Role.permissions` | system package |
| `log_audit`, `log_data_change` | dropped | covered by process log and versions (SPEC-30) |
| `view_screen`, `scr_*` views, `view_menu` | `View`, `Shell`/`App` | menus dropped (menu-less cockpit) |
| `it_asset`, host, nic, disk, volume, port | `ItAsset`, `Host`, `NetworkInterface`, `Disk`/`Volume`, `ServiceEndpoint` | – |
| `process`, `directory`, `file` (IT), `heartbeat` | dropped | telemetry, not entities |
| `sys_function`, `SysFunction`, `LogicType`, `sys.logic.*` | `Logic` | system |
| `sys_action`, `ActionType`, `sys.action.*`, Action/StatefulAction/TwoStepAction (US) | `Action`, `StatefulAction`, `TwoStepAction` | system |
| `workflow`, `workflow_state`, `MetaProcessType`, `ProcessInstanceType` | `Workflow`, `Lifecycle`, `ProcessInstance` | system |
| `sequence`, `config`, `i18n key`, `app` | `Sequence`, Context env / Tenant settings, `I18nKey`, `App` | system |
| currency, fx rate, UOM, conversion, period type, period, holiday | `Currency`, `ExchangeRate`, `Uom`, `UomConversion`, `PeriodType`, `Period`, `Holiday` (+ `Calendar`) | – |
| key, certificate, CA | `CryptoKey`, `Certificate`, `CertificateAuthority` | – |
| `fn_save_raw`, `save_entity_raw` | `entity.edit_raw@v1` | generic action |
| `fn_view_entity` | `entity.view@v1` | generic action |
| `fn_sys_policy` | baseline `Policy` entities | declarative |
| `Workspace`, `Logrum` (environment) | `Context` | system |
| `Job` (UC) | `Job` | system (SPEC-20) |
| `Cron`, `Hook` (UC), `CRON`, `LISTENER` (UP) | `Cron`, `Hook` | system |
| `SCHEMA` (FU) | the Type's derived schema + `schema_extra` | – |
| `USER`, `GROUP`, `POLICY`, `SECURITY_KEY`, `WEBHOOK`, `APPROVAL_REQUEST` (FU) | `User`, `Group`, `Policy`, `ApiKey`, `Webhook`, `ApprovalRequest` | system |
| `BehaviorType` / `Behavior` (LS) | slot catalogue / `Behavior` | system |
| `MetaRelationType`, `prop_*` / `rel_*` (LS) | semantic value types / reference properties / RelationshipTypes | by meaning |
| `RELATIONSHIP_V1` + `*.rtp.json` (LC) | `Relationship` + `RelationshipType` | – |
| `ROLE_V1`, `PROCESS_V1` (LC) | `Role`, `Pipeline` | – |
- **Origin:** VRD-22-04, REQ-CONV-019, CON-UC-018, CON-UB-013, CON-LS-011, CON-LC-014, CON-FU-014
- **Acceptance:** A conversion script produces both packages from the corpus files, and each dropped code is listed with its reason in the conversion report.
- **Priority:** P1

### REQ-ONT-003 — Vertical packs
- **Statement:** Vertical domain packs (e.g. `crmkit`, `hrkit`, `quizkit`) MUST:
  - extend ontology types through `_extends` (e.g. `crmkit.Lead extends Party`) or TypeExtensions (e.g. `crmkit.score` on Customer);
  - model roles as PartyRole subtypes and links as Relationships with their own RelationshipTypes in their namespace;
  - not copy ontology types or define competing semantic types for the same meaning (a `crmkit.Email` is refused in favour of `Email`, unless it narrows `Email`).

  `pkg validate` warns when a pack defines a value type whose constraints equal an ontology type.
- **Rationale:** "Any vertical is assembled by data" (UB). One vocabulary (VRD-22-04).
- **Origin:** VRD-02-06, CON-UB-014
- **Acceptance:** The reference CRM pack installs using only `_extends` and extensions, and its customers are also queryable as `Party`.
- **Priority:** P1

### REQ-ONT-004 — Ontology evolution
- **Statement:** The `ontology` package follows semantic versioning:
  - **PATCH:** seed-data corrections and translations;
  - **MINOR:** new types, properties (optional only), dictionary entries and relationship types;
  - **MAJOR:** removed or renamed codes, narrowed constraints that invalidate existing data, and changed semantics.

  MAJOR releases ship migration pipelines (REQ-PKG-010). Tenants receive ontology upgrades through `UPGRADE` boot or `package.install` with MERGE, which keeps their overlay patches.
- **Origin:** VRD-17-02, REQ-TEN-016
- **Acceptance:** Upgrading the ontology from 1.2 to 1.3 in a tenant that relabelled `Customer` keeps the label and adds the new property.
- **Priority:** P1

### REQ-ONT-005 — Unit and currency semantics
- **Statement:** Money arithmetic in rules and logic MUST use `Currency.minor_units` for rounding (half-even by default, configurable per operation). Measure conversions MUST use UomConversion factors. Converting between dimensions fails with `RULE.UNIT_MISMATCH`. Formatting uses the reader's locale (REQ-CTX-014) and the currency symbol.
- **Origin:** CON-UB-061 (asset algebra), CON-UP-016 (typed money), REQ-RULE-026
- **Acceptance:** `convert(Mass{2.5 kg}, "g")` gives 2500 g. Adding 1.005 CNY and 0 CNY rounds to 1.00 with half-even.
- **Priority:** P1

---

## 7. Interfaces

### API-ONT-001 — Named queries
- **Kind:** UBTP Query targets
- **Signature / shape:**

| URI | Args | Result |
|---|---|---|
| `ubos://system/Query/ont.convert@v1` | `{value: Measure, to: uom}` | converted value |
| `ubos://system/Query/ont.fx@v1` | `{from, to, at?}` | rate as of the time |
| `ubos://system/Query/ont.types@v1` | `{area?: party\|contact\|geo\|reference\|document\|it\|pki}` | ontology types with descriptions (for studio and AI) |
- **Origin:** CON-UB-014

### Error codes (ONT)

| Code | Category | Meaning |
|---|---|---|
| `ONT.NO_CONVERSION` | INVALID | no UomConversion path between units |
| `ONT.NO_RATE` | NOT_FOUND | no exchange rate at the time |

---

## 8. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-AIR-200 | Every ontology type and property has a `description` | 100 % (builder RAG, REQ-AI-031) |
| NFR-UX-200 | Every ontology type renders with derived layouts and sensible widgets | 100 % without custom Views |

## 9. Acceptance

| REQ | Criterion |
|---|---|
| REQ-ONT-001 | Genesis content and seeds present and valid |
| REQ-ONT-002 | Conversion report covers every corpus code |
| REQ-ONT-003 | Reference pack uses extension only |
| REQ-ONT-004 | Upgrade keeps tenant patches |
| REQ-ONT-005 | Rounding and conversion cases |

## 10. Implementation notes and open questions

- **Sources for conversion:**
  - [UB:data/boot/init.json], [UB:docs/sys_boot.md];
  - [UC:docs/db/genesis_data/], [UC:docs/init_data.md];
  - [US:docs/db/genesis_data/] (adds `ui_modes`);
  - [LS:genesis_data/];
  - [LC:data/relationships/common.rtp.json], [LC:data/relationships/role.rtp.json].
- **Standard seed data** (ISO 3166, ISO 4217, UCUM) is generated from public code lists at build time and pinned in the package.
- **Open questions:** none. Deferred: the executable Book features (CON-UB-055, CON-UC-061), which are a vertical pack in a later version.
