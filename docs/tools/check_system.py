#!/usr/bin/env python3
"""Validator for the system specification volume (docs/50-system).

Checks
  1. front matter: id, title, status, phase, depends_on present; document IDs unique
  2. item definitions: unique across the volume
  3. references: every referenced ID is defined (kinds listed in PENDING are reported
     as warnings until their defining batch is written)
  4. requirement blocks (FR/NR/CR/DSN): mandatory fields, valid priority, phase,
     system codes, verification codes, domain/area codes
  5. file size: warning above 800 lines
Usage: python3 docs/tools/check_system.py [--json] [--strict]
Exit code 1 on any error (and on warnings with --strict)."""
import re, glob, os, sys, json, collections

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '50-system')
STRICT = '--strict' in sys.argv

DOMAINS = set('MODEL VER TIME LEDG TXN LOGIC RULE FLOW EVT QRY IAM TEN AUD FILE LOC UX OFFICE AI PKG INT ANL PROOF SYNC OPS BILL DEV MIG NTF SIGN STD'.split())
SYSTEMS = set('BPA DVM FRG NOD CTL WSP STU AGT EXC FED NTY BRG SDK'.split())
NR_AREAS = set('PERF SCAL AVAIL DUR DET SEC PRIV OBS OPER USE ACC PORT COMPAT MAINT COST LOC'.split())
CR_REGS = set('SOC2 SEC FINRA SOX HIPAA GDPR CCPA ESIGN WCAG NIST'.split())
METHODS = set('CONF PROP SIM FAULT FORM SCN BENCH SEC USE PILOT INSP AUDIT'.split())
PRIORITIES = {'Must', 'Should', 'Could'}
PHASES = {f'PH-{i}' for i in range(6)}
STATUSES = {'draft', 'review', 'complete', 'stable'}

# Kinds whose defining batch is not written yet: undefined references are warnings.
PENDING = ('UBS-PH-', 'UBS-VER-', 'UBS-IDX-',
           'VER-', 'SUITE-', 'EXIT-', 'MET-', 'RSK-', 'ENV-')

ID_RE = (r'UBS-[A-Z]+(?:-[A-Z]+)?-\d{1,3}|UBS-README'
         r'|PER-[A-Z][A-Za-z0-9]+|CAP-[A-Z]+-\d{2}|SCN-\d{3}'
         r'|FR-[A-Z]+-\d{3}|NR-[A-Z]+-\d{3}|CR-[A-Z0-9]+-\d{3}|CST-\d{3}|ASM-\d{3}'
         r'|STD-[A-Z]+-\d{3}|DSN-[A-Z]+-\d{3}|IF-[A-Z]+-\d{3}|DAT-[A-Z][A-Za-z0-9]+'
         r'|GL-[A-Z][A-Za-z0-9]+|EXIT-\d-\d{2}|VER-[A-Z]+-\d{4}|SUITE-[A-Z0-9-]+[A-Z0-9]'
         r'|MET-[A-Z]+-\d{3}|RSK-\d{3}|AR-\d{3}|CTR-\d{3}|DEC-\d{3}|OQ-\d{3}|IMP-\d{2}|IN-\d{2}|ENV-[A-Z0-9-]+[A-Z0-9]'
         r'|EXT-(?:WP|RFC|TRI|BP|BPU)')
REF = re.compile(r'(?<![A-Za-z0-9_-])(' + ID_RE + r')(?![A-Za-z0-9_])')
DEF_PATTERNS = [
    r'^#{3,4} (' + ID_RE + r')\b',            # heading definitions (### or ####)
    r'^\| (' + ID_RE + r') \|',               # first table cell definitions
]
BUILTIN = PHASES | {'EXT-WP', 'EXT-RFC', 'EXT-TRI', 'EXT-BP', 'EXT-BPU'}
EXAMPLES = {  # placeholders used in writing rules and templates
    'UBS-REQ-05', 'UBS-SYS-DVM-03', 'PER-FundAccountant', 'CAP-VER-03', 'SCN-012', 'FR-TIME-004',
    'NR-PERF-010', 'CR-SEC-003', 'CST-004', 'ASM-002', 'STD-ISA-012', 'DSN-DVM-101', 'IF-NOD-004',
    'DAT-Commit', 'GL-Commit', 'EXIT-1-07', 'VER-CONF-0042', 'SUITE-DVM-CORE', 'MET-PERF-003',
    'RSK-017', 'AR-004', 'CTR-012', 'DEC-011', 'OQ-004', 'IMP-02', 'PH-2'}

def md_files():
    return sorted(f for f in glob.glob(os.path.join(ROOT, '**', '*.md'), recursive=True)
                  if '/INDEX/' not in f.replace(os.sep, '/'))

def strip_code(text):
    """Remove fenced code blocks (templates/examples) but keep yaml DAT definitions."""
    return re.sub(r'```.*?```', '', text, flags=re.S)

errors, warnings = [], []
defs = collections.defaultdict(list)
texts = {}
for f in md_files():
    rel = os.path.relpath(f, ROOT)
    s = open(f, encoding='utf-8').read()
    texts[rel] = s
    fm = re.match(r'^---\n(.*?)\n---\n', s, re.S)
    if not fm:
        errors.append(f'{rel}: missing front matter'); continue
    meta = dict(re.findall(r'^(\w+): *(.*)$', fm.group(1), re.M))
    for k in ('id', 'title', 'status', 'phase', 'depends_on'):
        if k not in meta: errors.append(f'{rel}: front matter lacks "{k}"')
    if meta.get('status') not in STATUSES: errors.append(f'{rel}: bad status {meta.get("status")}')
    if meta.get('phase') not in PHASES | {'ALL'}: errors.append(f'{rel}: bad phase {meta.get("phase")}')
    if 'id' in meta: defs[meta['id']].append(rel)
    body = strip_code(s)
    for pat in DEF_PATTERNS:
        for m in re.finditer(pat, body, re.M):
            if m.group(1).startswith('UBS-'): continue
            if 'writing-rules' in rel or 'templates' in rel: continue
            defs[m.group(1)].append(rel)
    for m in re.finditer(r'^(DAT-[A-Z][A-Za-z0-9]+):', s, re.M):  # yaml DAT definitions
        if 'templates' not in rel: defs[m.group(1)].append(rel)
    n = s.count('\n')
    if n > 800: warnings.append(f'{rel}: {n} lines (> 800, consider splitting)')

# duplicates (a DAT defined by heading and yaml in the same file counts once)
for i, fs in defs.items():
    if len(set(fs)) > 1: errors.append(f'duplicate definition {i}: {sorted(set(fs))}')

# references
undefined = collections.defaultdict(set)
for rel, s in texts.items():
    if 'writing-rules' in rel or 'templates' in rel: continue
    for m in REF.finditer(s):
        i = m.group(1)
        if i in defs or i in BUILTIN or i in EXAMPLES: continue
        undefined[i].add(rel)
for i, fs in sorted(undefined.items()):
    msg = f'undefined {i} in {", ".join(sorted(fs)[:3])}{" …" if len(fs) > 3 else ""}'
    (warnings if i.startswith(PENDING) else errors).append(msg)

# requirement blocks
BLOCK = re.compile(r'^### ((FR|NR|CR|DSN)-([A-Z0-9]+)-\d{3}) — .+?(?=^#{1,3} |\Z)', re.M | re.S)
FIELDS = ['Statement', 'Rationale', 'Priority', 'Phase', 'Systems', 'Personas', 'Acceptance', 'Verification', 'Origin']
counts = collections.Counter()
for rel, s in texts.items():
    if 'writing-rules' in rel or 'templates' in rel: continue
    for m in BLOCK.finditer(strip_code(s)):
        rid, kind, code, blk = m.group(1), m.group(2), m.group(3), m.group(0)
        counts[kind] += 1
        need = FIELDS + (['Target'] if kind == 'NR' else []) + (['Control reference'] if kind == 'CR' else [])
        for fld in need:
            if f'**{fld}:**' not in blk: errors.append(f'{rel}: {rid} lacks field {fld}')
        if kind == 'FR' and code not in DOMAINS: errors.append(f'{rel}: {rid} unknown domain {code}')
        if kind == 'FR':
            cap = f'CAP-{code}-{rid[-3:-1]}'
            if cap not in defs or rid[-1] == '0': errors.append(f'{rel}: {rid} does not map to an existing capability ({cap}, k=1..9)')
        if kind == 'NR' and code not in NR_AREAS: errors.append(f'{rel}: {rid} unknown NR area {code}')
        if kind == 'CR' and code not in CR_REGS: errors.append(f'{rel}: {rid} unknown regulation {code}')
        if kind == 'DSN' and code not in SYSTEMS: errors.append(f'{rel}: {rid} unknown system {code}')
        pm = re.search(r'\*\*Priority:\*\* (\w+)', blk)
        if pm and pm.group(1) not in PRIORITIES: errors.append(f'{rel}: {rid} bad priority {pm.group(1)}')
        ph = re.search(r'\*\*Phase:\*\* (PH-\d)', blk)
        if not ph or ph.group(1) not in PHASES: errors.append(f'{rel}: {rid} bad or missing phase')
        sy = re.search(r'\*\*Systems:\*\* ([^\n]+)', blk)
        if sy:
            for c in re.split(r'[,\s]+', sy.group(1).strip()):
                if c and c not in SYSTEMS: errors.append(f'{rel}: {rid} unknown system code {c}')
        ve = re.search(r'\*\*Verification:\*\* ([^\n]+)', blk)
        if ve:
            for c in re.split(r'[,\s]+', ve.group(1).strip()):
                if c and c not in METHODS: errors.append(f'{rel}: {rid} unknown verification method {c}')
        ac = re.search(r'\*\*Acceptance:\*\*\s*\n\s+1\. ', blk)
        if not ac: errors.append(f'{rel}: {rid} acceptance must be a numbered list')

kinds = collections.Counter(re.match(r'([A-Z]+)-', i).group(1) for i in defs if not i.startswith('UBS-'))
report = {'documents': sum(1 for i in defs if i.startswith('UBS-')), 'items_by_kind': dict(sorted(kinds.items())),
          'requirement_blocks': dict(counts), 'errors': errors, 'warnings': warnings}
if '--json' in sys.argv:
    print(json.dumps(report, indent=1))
else:
    print(f"documents: {report['documents']}")
    print('items: ' + ', '.join(f'{k}={v}' for k, v in report['items_by_kind'].items()))
    print('requirement blocks: ' + ', '.join(f'{k}={v}' for k, v in sorted(counts.items())))
    print(f'errors: {len(errors)}')
    for e in errors[:200]: print('  E ' + e)
    print(f'warnings: {len(warnings)}')
    for w in warnings[:60]: print('  W ' + w)
    if len(warnings) > 60: print(f'  … {len(warnings) - 60} more')
sys.exit(1 if errors or (STRICT and warnings) else 0)
