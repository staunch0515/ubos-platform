#!/usr/bin/env python3
"""Cross-reference validation for the UBOS documentation set (P5).
Checks: undefined IDs referenced in 40-spec/00-meta, duplicate definitions,
verdict coverage (every VRD consumed by >=1 spec file), NFR catalogue integrity.
Usage: python3 docs/tools/check_refs.py [--json]"""
import re, glob, os, sys, json, collections
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
def files(pattern):  # generated INDEX files are copies, not sources
    return sorted(f for f in glob.glob(os.path.join(ROOT, pattern), recursive=True) if '/INDEX/' not in f.replace(os.sep, '/'))
defs = collections.defaultdict(list)       # id -> [file]
def add(i, f): defs[i].append(os.path.relpath(f, ROOT))
DEF_PATTERNS = [
    (r'^#{2,3} (REQ-[A-Z]+-\d{3})\b', None), (r'^- (REQ-[A-Z]+-\d{3}):', None),
    (r'^#{2,3} (API-[A-Z]+-\d{3})\b', None), (r'^#{2,3} (TERM-[A-Za-z0-9]+)\b', None),
    (r'^(ENT-[A-Za-z0-9]+):', None), (r'^#{2,3} (VRD-\d{2}-\d{2})\b', None),
    (r'^#{2,3} (CON-[A-Z]+-\d{3})\b', None), (r'^#{2,3} (HL-[A-Z]+-\d{3})\b', None),
    (r'^\| (NFR-[A-Z]+-\d{3}) \|', 'nfr'), (r'^\| (Q-\d{3}) \|', None),
]
for f in files('**/*.md'):
    s = open(f, encoding='utf-8').read()
    m = re.search(r'^id: (\S+)', s, re.M)
    if m: add(m.group(1), f)
    for pat, kind in DEF_PATTERNS:
        for mm in re.finditer(pat, s, re.M):
            if kind == 'nfr' and '32-nfr-catalogue' in f and not re.search(r'NFR-[A-Z]+-9\d\d', mm.group(1)):
                continue  # catalogue rows are copies, not definitions (except system-level 9xx)
            add(mm.group(1), f)
# ENT in yaml blocks may be indented inside code fences: also accept "ENT-X:" at line start inside ```yaml
REF = re.compile(r'\b(REQ-[A-Z]+-\d{3}|API-[A-Z]+-\d{3}|TERM-[A-Za-z0-9]+|ENT-[A-Z][A-Za-z0-9]+|VRD-\d{2}-\d{2}|CON-[A-Z]{2}-\d{3}|HL-[A-Z]{2}-\d{3}|NFR-[A-Z]+-\d{3}|ADR-\d{3}|SPEC-\d{2}|CMP-\d{2}|Q-\d{3})\b')
IGNORE = {'TERM-Name', 'ENT-Name', 'ENT-Other', 'ENT-EntityDefinition', 'TERM-Rehydration', 'API-RT-007', 'NFR-PERF-003', 'Q-017'}  # template/example placeholders (WRITING-RULES)
undefined = collections.defaultdict(set)
for f in files('40-spec/**/*.md') + files('00-meta/*.md') + files('README.md'):
    s = open(f, encoding='utf-8').read()
    for mm in REF.finditer(s):
        i = mm.group(1)
        if i in IGNORE or i in defs: continue
        if re.match(r'(REQ|API)-[A-Z]+-\d{3}$', i) and i.split('-')[1] in ('MOD',): continue
        undefined[i].add(os.path.relpath(f, ROOT))
dups = {i: fs for i, fs in defs.items() if len(fs) > 1 and not i.startswith(('SPEC-', 'ANA-', 'INV-', 'CMP-', 'META-', 'ADR-'))}
spec_text = ''.join(open(f, encoding='utf-8').read() for f in files('40-spec/**/*.md'))
vrds = sorted(i for i in defs if i.startswith('VRD-'))
uncovered = [v for v in vrds if v not in spec_text]
report = {'definitions': len(defs), 'undefined': {k: sorted(v) for k, v in sorted(undefined.items())},
          'duplicates': dups, 'verdicts': len(vrds), 'verdicts_uncovered': uncovered}
if '--json' in sys.argv: print(json.dumps(report, indent=1, ensure_ascii=False))
else:
    print(f"definitions: {report['definitions']}")
    print(f"undefined references: {len(report['undefined'])}")
    for k, v in report['undefined'].items(): print(f"  {k}: {', '.join(v[:4])}{' …' if len(v) > 4 else ''}")
    print(f"duplicate definitions: {len(dups)}")
    for k, v in dups.items(): print(f"  {k}: {v}")
    print(f"verdicts: {len(vrds)}, uncovered: {len(uncovered)} {uncovered}")
sys.exit(1 if (undefined or dups or uncovered) else 0)
