---
id: UBS-VER-01
title: Verification Volume — Methods
status: draft
phase: ALL
depends_on: [UBS-VER-00]
---

# Verification Volume — Methods

Each method below is given with its purpose, the procedure every item of that method
follows, tooling, evidence and the default pass rule. An item may tighten the pass rule, never
loosen it.

## CONF — conformance vectors and conformance tests

- **Purpose:** show that an implementation behaves exactly as the standard (standard vectors)
  or as a system contract (system-level tests) states.
- **Procedure:**
  1. Load the conformance package of the targeted BPA release (IF-BPA-001) or the system test
     pack.
  2. Start the implementation under test with the test-only adapter operations enabled
     (STD-CONF-002) in a clean state.
  3. For each vector: `reset` → `set_now`, `set_entropy_seed` from the vector → `setup` steps
     → `steps` with expected results → compare with the match mode (`exact`, `subset`,
     `hash`).
  4. Emit the signed runner report (DAT-ConformanceReport).
- **Tooling:** `forge conform` (DSN-FRG-401…403).
- **Evidence:** signed runner report per backend and platform.
- **Default pass rule:** 100% of vectors of the claimed profiles pass; `not_applicable` only
  outside claimed profiles.

## PROP — property-based testing

- **Purpose:** check universal properties (algebraic laws, invariants) over generated inputs.
- **Procedure:**
  1. State the property formally in the item (for example "for all histories H1, H2 reaching
     equal state, root(H1) = root(H2)").
  2. Implement generators with shrinking (proptest for Rust, fast-check for TypeScript).
  3. Run the short mode on every change (≥ 10,000 cases) and the long mode nightly and
     before gates (≥ 1,000,000 cases or 1 hour per property).
  4. Store every failing case as a permanent regression example.
- **Tooling:** proptest, fast-check, custom generators for models, histories and policies.
- **Evidence:** property report (cases run, seeds, shrunk counter-examples if any).
- **Default pass rule:** 0 failures.

## SIM — deterministic simulation

- **Purpose:** find concurrency, crash and partition bugs by exploring interleavings.
- **Procedure:**
  1. Run the system components under the deterministic simulator (DSN-DVM-516): simulated
     clock, scheduler, network and storage with seeded faults.
  2. Check invariants after every step (for example linear history, dense `cseq`, no lost
     acknowledged commit, per-key event order, convergence after partitions heal).
  3. Run N seeds per night (default 10,000; 1,000,000 before gates).
  4. A failing seed is reproduced, fixed, and added to the permanent seed set.
- **Tooling:** in-house simulator in the `ubos` monorepo (madsim-class design); Elle-style
  history checker for serializability.
- **Evidence:** simulation report (seeds, simulated hours, violations).
- **Default pass rule:** 0 violations.

## FAULT — fault injection on real systems

- **Purpose:** confirm durability and recovery claims on real databases, disks and networks.
- **Procedure:**
  1. Deploy the target topology on its reference environment.
  2. Apply load (a workload from `03-datasets.md`).
  3. Inject faults from the fault matrix: SIGKILL of processes, power-loss simulation
     (dm-flakey or VM hard reset), disk full, network partition (tc/iptables), database
     primary loss, object store unavailability, clock jumps, key-service outage.
  4. After recovery, verify: acknowledged commits present (client-side acknowledgement log
     compared with the store), `verify` reports clean, outbox resumed, invariants hold.
- **Tooling:** Chaos Mesh or equivalent for Kubernetes deployments; scripted fault matrix for
  VMs.
- **Evidence:** fault report per matrix cell with before/after verification.
- **Default pass rule:** 0 lost acknowledged commits; 0 verifier findings; recovery within the
  NR targets.

## FORM — formal models

- **Purpose:** prove protocol properties before and alongside implementation.
- **Procedure:**
  1. Model the protocol in TLA+ (or an equivalent tool) in `ubos-verify`.
  2. State safety and liveness properties.
  3. Model-check with TLC or Apalache at the bounds stated in the item.
  4. Generate traces and convert them into conformance vectors (DSN-BPA-004).
- **Tooling:** TLA+ Toolbox, TLC, Apalache.
- **Evidence:** model, configuration, checker output with state counts.
- **Default pass rule:** 0 violated properties at the stated bounds.

## SCN — end-to-end scenarios

- **Purpose:** show that reference scenarios (SCN-*) work end to end through real interfaces.
- **Procedure:**
  1. Deploy the phase's reference deployment with the Buks and fixtures the scenario needs.
  2. Execute the scenario's main flow and each alternate flow as scripted steps (SDK
     scripts, Playwright for UI).
  3. Check every step's expected outcome and the scenario's acceptance statements.
  4. Record traces and screenshots.
- **Tooling:** SDK scripts, Playwright, scenario runner in `ubos-verify`.
- **Evidence:** scenario run report with per-step results and trace IDs.
- **Default pass rule:** all steps and acceptance statements pass.

## BENCH — benchmarks

- **Purpose:** measure NR performance, scale and cost targets.
- **Procedure:**
  1. Provision the reference environment exactly as specified (`02-environments.md`).
  2. Load the workload dataset; warm up for 10 minutes.
  3. Run the measured phase (default 30 minutes) at the load level stated in the NR.
  4. Collect latency histograms at the stated boundary, throughput, resource use.
  5. Repeat three times; report the median run.
- **Tooling:** k6 or custom Rust load generators speaking UBTP; HdrHistogram; OpenTelemetry.
- **Evidence:** benchmark report with environment hash, dataset seed, histograms.
- **Default pass rule:** the NR target is met in the median run and no run misses it by more
  than 10%.

## SEC — security assessment

- **Purpose:** show that security and isolation properties hold against adversarial input.
- **Procedure (by kind):**
  - escape suites: run every case in the sandbox; each must end in a blocked capability or
    terminated process, never in an effect outside the sandbox;
  - isolation suites: two tenants; attempt every cross-tenant access through every interface;
  - permission matrices: principal × action × resource grid compared with expected outcomes;
  - leak scans: canary values in business data, secret scanning over logs, traces and
    bundles;
  - penetration tests: external vendor, scope and rules agreed per phase.
- **Tooling:** in-house suites; external pen-test vendor; secret scanners (gitleaks class).
- **Evidence:** security report; pen-test report with findings and remediation status.
- **Default pass rule:** 100% of cases blocked; 0 leaks; 0 open critical or high findings.

## USE — usability studies

- **Purpose:** measure task success, time and satisfaction for target personas.
- **Procedure:**
  1. Recruit at least 8 participants per persona group (business users, approvers, builders,
     auditors) matching the personas.
  2. Run moderated sessions with the defined core tasks; no help beyond scripted prompts.
  3. Measure unassisted success, time on task, errors; administer SUS.
  4. Analyse and report against the NR-USE targets.
- **Tooling:** session recording, SUS questionnaire.
- **Evidence:** study report with raw measures.
- **Default pass rule:** the NR-USE target is met for each persona group.

## INSP — inspection

- **Purpose:** verify properties best checked by review: documents, schemas, configurations,
  dependency graphs, legal and accessibility conformance.
- **Procedure:** checklist-based review by a named role; automated checks where possible
  (dependency-graph lint, schema lint, vocabulary scans).
- **Evidence:** checklist record with reviewer, date and findings.
- **Default pass rule:** all checklist items satisfied or findings closed.

## PILOT — pilot acceptance

- **Purpose:** prove business value with a real customer or partner.
- **Procedure:**
  1. Agree measurable success criteria with the partner before the pilot and record them as
     a decision.
  2. Operate the pilot; measure continuously.
  3. Review weekly with the partner; log issues and resolutions.
  4. Obtain signed acceptance.
- **Evidence:** pilot report and signed acceptance record.
- **Default pass rule:** all agreed success criteria met and acceptance signed.
