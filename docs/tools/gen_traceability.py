#!/usr/bin/env python3
"""Generate the traceability volume (docs/50-system/60-verification/2x-*.md).

For every FR, NR and CR it lists:
  * the design items (DSN) whose **Origin** names the requirement;
  * the suites that verify those design items (suites of the DSN's systems whose methods
    intersect the DSN's verification methods, from the suite registry UBS-VER-04);
  * the verification items (VER) that name the requirement directly in **Verifies**.
It writes one summary file and one file per phase, and reports gaps.

Usage: python3 docs/tools/gen_traceability.py
"""
import os
import re
import collections

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '50-system')
OUT = os.path.join(ROOT, '60-verification')

REQ = re.compile(r'^### ((FR|NR|CR)-[A-Z0-9]+-\d{3}) — (.+?)\n(.*?)(?=^#{1,3} |\Z)', re.M | re.S)
DSN = re.compile(r'^### (DSN-([A-Z]+)-\d{3}) — (.+?)\n(.*?)(?=^#{1,3} |\Z)', re.M | re.S)
STD = re.compile(r'^### (STD-[A-Z]+-\d{3}) — (.+?)\n(.*?)(?=^#{1,3} |\Z)', re.M | re.S)
VER = re.compile(r'^### (VER-[A-Z]+-\d{4}) — (.+?)\n(.*?)(?=^#{1,3} |\Z)', re.M | re.S)
IDS = re.compile(r'\b((?:FR|NR|CR)-[A-Z0-9]+-\d{3})\b')
RANGE = re.compile(r'\b((?:FR|NR|CR)-[A-Z0-9]+)-(\d{3})…(\d{3})\b')


def field(block, name):
    m = re.search(r'\*\*' + re.escape(name) + r':\*\* ([^\n]+)', block)
    return m.group(1).strip() if m else ''


def expand(text):
    """IDs named in a field, including ranges like FR-SYNC-071…075."""
    out = set(IDS.findall(text))
    for pre, a, b in RANGE.findall(text):
        out.update('%s-%03d' % (pre, n) for n in range(int(a), int(b) + 1))
    return out


def files():
    for d, _, fs in os.walk(ROOT):
        for f in sorted(fs):
            if f.endswith('.md'):
                p = os.path.join(d, f)
                yield os.path.relpath(p, ROOT), open(p, encoding='utf-8').read()


def main():
    reqs, dsns, vers, stds = {}, {}, {}, {}
    suites = []  # (system, suite, methods)
    for rel, text in files():
        if rel.startswith('10-requirements'):
            for m in REQ.finditer(text):
                b = m.group(4)
                pr = re.search(r'\*\*Priority:\*\* (\w+)', b)
                ph = re.search(r'\*\*Phase:\*\* (PH-\d)', b)
                reqs[m.group(1)] = (m.group(3).strip(), pr.group(1) if pr else '', ph.group(1) if ph else '',
                                    field(b, 'Verification'))
        if rel.startswith('40-systems'):
            for m in DSN.finditer(text):
                b = m.group(4)
                ph = re.search(r'\*\*Phase:\*\* (PH-\d)', b)
                systems = re.search(r'\*\*Systems:\*\* ([A-Z, ]+)', b)
                dsns[m.group(1)] = (m.group(2), ph.group(1) if ph else '', expand(field(b, 'Origin')),
                                    set(x.strip() for x in field(b, 'Verification').split(',')),
                                    [s.strip() for s in systems.group(1).split(',')] if systems else [])
        if rel.startswith('30-standard'):
            for m in STD.finditer(text):
                stds[m.group(1)] = (expand(field(m.group(3), 'Satisfies')), field(m.group(3), 'Conformance vectors'))
        if rel.startswith('60-verification'):
            for m in VER.finditer(text):
                vers[m.group(1)] = (expand(field(m.group(3), 'Verifies')), field(m.group(3), 'Supports'))
            if rel.endswith('04-suites.md'):
                for m in re.finditer(r'^\| ([A-Z]+) \| (SUITE-[A-Z0-9-]+) \| [^|]+\| ([^|]+)\|', text, re.M):
                    suites.append((m.group(1), m.group(2), set(x.strip() for x in m.group(3).split(','))))

    realised = collections.defaultdict(list)
    for d, (sysname, ph, origin, methods, systems) in dsns.items():
        for r in origin:
            realised[r].append(d)
    by_std = collections.defaultdict(list)
    for c, (sat, vec) in stds.items():
        for r in sat:
            by_std[r].append(c)
    direct = collections.defaultdict(list)
    for v, (verifies, _) in vers.items():
        for r in verifies:
            direct[r].append(v)

    def suites_for(dsn_ids):
        out = set()
        for d in dsn_ids:
            sysname, _, _, methods, _ = dsns[d]
            for (s_sys, s, s_methods) in suites:
                if s_sys == sysname and methods & s_methods:
                    out.add(s)
        return sorted(out)

    summary = collections.OrderedDict()
    for n in range(6):
        ph = 'PH-%d' % n
        mine = sorted(r for r, v in reqs.items() if v[2] == ph)
        rows, gaps = [], []
        for r in mine:
            title, prio, _, methods = reqs[r]
            ds = sorted(realised.get(r, [])) + sorted(by_std.get(r, []))
            vs = sorted(direct.get(r, []))
            ss = suites_for([d for d in ds if d.startswith('DSN-')])
            if any(d.startswith('STD-') for d in ds):
                ss = sorted(set(ss) | {'SUITE-DVM-CONF'})
            covered = bool(ss or vs)
            if not covered:
                gaps.append(r)
            rows.append('| %s | %s | %s | %s | %s | %s | %s |' % (
                title, r, prio, methods or '—', ', '.join(ds[:6]) + (' …' if len(ds) > 6 else '') or '—',
                ', '.join(ss[:4]) + (' …' if len(ss) > 4 else '') or '—', ', '.join(vs) or '—'))
        must = [r for r in mine if reqs[r][1] == 'Must']
        summary[ph] = (len(mine), len(must), sum(1 for r in mine if realised.get(r) or by_std.get(r)),
                       len(mine) - len(gaps), [g for g in gaps if reqs[g][1] == 'Must'], gaps)
        lines = ['---', 'id: UBS-VER-2%d' % (n + 1), 'title: Traceability — %s (generated)' % ph, 'status: draft',
                 'phase: %s' % ph, 'depends_on: [UBS-VER-20]', '---', '',
                 '# Traceability — %s' % ph, '',
                 '> Generated by `docs/tools/gen_traceability.py`. Do not edit by hand.', '',
                 'Columns: requirement; priority; verification methods stated by the requirement; design items and',
                 'standard clauses realising it (DSN **Origin**, STD **Satisfies**); suites that verify them; verification items',
                 'naming the requirement directly.', '',
                 '| Title | Requirement | Priority | Methods | Realised by | Verified in suites | Direct VER items |',
                 '|---|---|---|---|---|---|---|'] + rows + ['']
        if gaps:
            lines += ['## Gaps', '', 'Requirements without a realising design item verified by a suite and without a direct',
                      'verification item. A Must gap blocks the phase exit (EXIT-%d-* traceability criterion); a Should or' % n,
                      'Could gap is closed or carried over by decision (UBS-PH-00 §3.7).', '']
            lines += ['- %s (%s)' % (g, reqs[g][1]) for g in gaps] + ['']
        with open(os.path.join(OUT, '2%d-trace-ph%d.md' % (n + 1, n)), 'w', encoding='utf-8') as fh:
            fh.write('\n'.join(lines))

    lines = ['---', 'id: UBS-VER-20', 'title: Traceability — Summary (generated)', 'status: draft', 'phase: ALL',
             'depends_on: [UBS-VER-00, UBS-VER-04]', '---', '', '# Traceability — Summary', '',
             '> Generated by `docs/tools/gen_traceability.py`. Do not edit by hand.', '',
             '| Phase | Requirements | Must | Realised by design | Covered by suites or items | Must gaps |',
             '|---|---|---|---|---|---|']
    for ph, (total, must, real, cov, mgaps, gaps) in summary.items():
        lines.append('| %s | %d | %d | %d | %d | %d |' % (ph, total, must, real, cov, len(mgaps)))
    lines += ['', 'Per-phase files: `21-trace-ph0.md` … `26-trace-ph5.md`.', '',
              '## How coverage is computed', '',
              '1. A requirement is **realised** when at least one DSN item names it in **Origin**, or a standard',
              '   clause names it in **Satisfies** (standard clauses are verified by their conformance vectors in',
              '   SUITE-DVM-CONF).',
              '2. A realised requirement is **covered** when a suite of the DSN\'s owning system runs one of the',
              '   DSN\'s verification methods (suite registry UBS-VER-04).',
              '3. A requirement is also covered when a verification item names it in **Verifies**.',
              '4. Gaps are listed per phase and must be closed before that phase\'s traceability exit criterion.', '']
    with open(os.path.join(OUT, '20-traceability.md'), 'w', encoding='utf-8') as fh:
        fh.write('\n'.join(lines))
    for ph, v in summary.items():
        print(ph, 'total', v[0], 'must', v[1], 'realised', v[2], 'covered', v[3], 'must-gaps', len(v[4]))


if __name__ == '__main__':
    main()
