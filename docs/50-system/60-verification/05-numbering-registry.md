---
id: UBS-VER-05
title: Verification Volume — Numbering Registry
status: draft
phase: ALL
depends_on: [UBS-VER-00]
---

# Verification Volume — Numbering Registry

This registry defines every VER number range (UBS-VER-00 §3). A row whose first cell is a
range `VER-<METHOD>-NNNN…MMMM` defines all IDs inside it. Ranges of one method never overlap.

Individual verification items (template T11) are written in `10-items-*` files and MUST lie
inside a registered range of their method and owner.

## CONF — standard conformance vectors

| Range | Standard area |
|---|---|
| VER-CONF-0001…0299 | FND foundations |
| VER-CONF-0300…0499 | OBJ object model |
| VER-CONF-0500…0999 | CLS class system |
| VER-CONF-1000…1199 | KIND state kinds |
| VER-CONF-1200…1699 | VER versioning |
| VER-CONF-1700…1999 | TIME bitemporal |
| VER-CONF-2000…2299 | TXN transactions |
| VER-CONF-2300…2599 | PROOF proofs |
| VER-CONF-2600…2999 | BPU processing units |
| VER-CONF-3000…3999 | ISA instruction set |
| VER-CONF-4000…4399 | CTX execution |
| VER-CONF-4400…4799 | EXPR expressions |
| VER-CONF-4800…5199 | LGS governance sheets |
| VER-CONF-5200…5399 | ADDR addressing |
| VER-CONF-5400…5699 | PROTO UBTP |
| VER-CONF-5700…5999 | SYNC sync and federation |
| VER-CONF-6000…6199 | ERR errors |
| VER-CONF-6200…6299 | CONF conformance |
| VER-CONF-6300…6999 | reserved |

## CONF — system-level conformance tests

| Range | Owner system |
|---|---|
| VER-CONF-7000…7499 | NOD |
| VER-CONF-7500…7699 | SDK |
| VER-CONF-7700…7899 | FRG |
| VER-CONF-7900…8099 | EXC |
| VER-CONF-8100…8299 | NTY |
| VER-CONF-8300…8399 | BPA certification |
| VER-CONF-8400…8699 | DVM system-level |
| VER-CONF-8700…8799 | CTL |
| VER-CONF-8800…8899 | FED |
| VER-CONF-8900…8999 | BRG |
| VER-CONF-9000…9099 | WSP |
| VER-CONF-9100…9299 | AGT |
| VER-CONF-9300…9399 | STU |
| VER-CONF-9400…9999 | reserved |

## PROP — property-based tests

| Range | Owner system |
|---|---|
| VER-PROP-0001…1999 | DVM |
| VER-PROP-2000…3999 | NOD |
| VER-PROP-4000…4499 | FRG |
| VER-PROP-4500…4999 | SDK |
| VER-PROP-5000…5499 | WSP |
| VER-PROP-5500…5999 | STU |
| VER-PROP-6000…6499 | AGT |
| VER-PROP-6500…6999 | NTY |
| VER-PROP-7000…7499 | BRG |
| VER-PROP-7500…7999 | CTL |
| VER-PROP-8000…8499 | EXC |
| VER-PROP-8500…8999 | FED |
| VER-PROP-9000…9499 | BPA |
| VER-PROP-9500…9999 | cross-system |

## SIM — deterministic simulation

| Range | Owner system |
|---|---|
| VER-SIM-0001…1999 | DVM |
| VER-SIM-2000…3999 | NOD |
| VER-SIM-4000…4499 | FRG |
| VER-SIM-4500…4999 | SDK |
| VER-SIM-5000…5499 | WSP |
| VER-SIM-5500…5999 | STU |
| VER-SIM-6000…6499 | AGT |
| VER-SIM-6500…6999 | NTY |
| VER-SIM-7000…7499 | BRG |
| VER-SIM-7500…7999 | CTL |
| VER-SIM-8000…8499 | EXC |
| VER-SIM-8500…8999 | FED |
| VER-SIM-9000…9499 | BPA |
| VER-SIM-9500…9999 | cross-system |

## FAULT — fault injection

| Range | Owner system |
|---|---|
| VER-FAULT-0001…1999 | DVM |
| VER-FAULT-2000…3999 | NOD |
| VER-FAULT-4000…4499 | FRG |
| VER-FAULT-4500…4999 | SDK |
| VER-FAULT-5000…5499 | WSP |
| VER-FAULT-5500…5999 | STU |
| VER-FAULT-6000…6499 | AGT |
| VER-FAULT-6500…6999 | NTY |
| VER-FAULT-7000…7499 | BRG |
| VER-FAULT-7500…7999 | CTL |
| VER-FAULT-8000…8499 | EXC |
| VER-FAULT-8500…8999 | FED |
| VER-FAULT-9000…9499 | BPA |
| VER-FAULT-9500…9999 | cross-system |

## FORM — formal models

| Range | Owner system |
|---|---|
| VER-FORM-0001…1999 | DVM |
| VER-FORM-2000…3999 | NOD |
| VER-FORM-4000…4499 | FRG |
| VER-FORM-4500…4999 | SDK |
| VER-FORM-5000…5499 | WSP |
| VER-FORM-5500…5999 | STU |
| VER-FORM-6000…6499 | AGT |
| VER-FORM-6500…6999 | NTY |
| VER-FORM-7000…7499 | BRG |
| VER-FORM-7500…7999 | CTL |
| VER-FORM-8000…8499 | EXC |
| VER-FORM-8500…8999 | FED |
| VER-FORM-9000…9499 | BPA |
| VER-FORM-9500…9999 | cross-system |

## BENCH — benchmarks

| Range | Owner system |
|---|---|
| VER-BENCH-0001…1999 | DVM |
| VER-BENCH-2000…3999 | NOD |
| VER-BENCH-4000…4499 | FRG |
| VER-BENCH-4500…4999 | SDK |
| VER-BENCH-5000…5499 | WSP |
| VER-BENCH-5500…5999 | STU |
| VER-BENCH-6000…6499 | AGT |
| VER-BENCH-6500…6999 | NTY |
| VER-BENCH-7000…7499 | BRG |
| VER-BENCH-7500…7999 | CTL |
| VER-BENCH-8000…8499 | EXC |
| VER-BENCH-8500…8999 | FED |
| VER-BENCH-9000…9499 | BPA |
| VER-BENCH-9500…9999 | cross-system |

## SEC — security assessments

| Range | Owner system |
|---|---|
| VER-SEC-0001…1999 | DVM |
| VER-SEC-2000…3999 | NOD |
| VER-SEC-4000…4499 | FRG |
| VER-SEC-4500…4999 | SDK |
| VER-SEC-5000…5499 | WSP |
| VER-SEC-5500…5999 | STU |
| VER-SEC-6000…6499 | AGT |
| VER-SEC-6500…6999 | NTY |
| VER-SEC-7000…7499 | BRG |
| VER-SEC-7500…7999 | CTL |
| VER-SEC-8000…8499 | EXC |
| VER-SEC-8500…8999 | FED |
| VER-SEC-9000…9499 | BPA |
| VER-SEC-9500…9999 | cross-system |

## INSP — inspections

| Range | Owner system |
|---|---|
| VER-INSP-0001…1999 | DVM |
| VER-INSP-2000…3999 | NOD |
| VER-INSP-4000…4499 | FRG |
| VER-INSP-4500…4999 | SDK |
| VER-INSP-5000…5499 | WSP |
| VER-INSP-5500…5999 | STU |
| VER-INSP-6000…6499 | AGT |
| VER-INSP-6500…6999 | NTY |
| VER-INSP-7000…7499 | BRG |
| VER-INSP-7500…7999 | CTL |
| VER-INSP-8000…8499 | EXC |
| VER-INSP-8500…8999 | FED |
| VER-INSP-9000…9499 | BPA |
| VER-INSP-9500…9999 | cross-system |

## SCN — end-to-end scenarios

| Range | Phase |
|---|---|
| VER-SCN-0001…0999 | PH-0 |
| VER-SCN-1000…1999 | PH-1 |
| VER-SCN-2000…2999 | PH-2 |
| VER-SCN-3000…3999 | PH-3 |
| VER-SCN-4000…4999 | PH-4 |
| VER-SCN-5000…5999 | PH-5 |
| VER-SCN-6000…9999 | reserved |

## USE — usability studies

| Range | Phase |
|---|---|
| VER-USE-0001…0999 | PH-0 |
| VER-USE-1000…1999 | PH-1 |
| VER-USE-2000…2999 | PH-2 |
| VER-USE-3000…3999 | PH-3 |
| VER-USE-4000…4999 | PH-4 |
| VER-USE-5000…5999 | PH-5 |
| VER-USE-6000…9999 | reserved |

## PILOT — pilot acceptance

| Range | Phase |
|---|---|
| VER-PILOT-0001…0999 | PH-0 |
| VER-PILOT-1000…1999 | PH-1 |
| VER-PILOT-2000…2999 | PH-2 |
| VER-PILOT-3000…3999 | PH-3 |
| VER-PILOT-4000…4999 | PH-4 |
| VER-PILOT-5000…5999 | PH-5 |
| VER-PILOT-6000…9999 | reserved |
