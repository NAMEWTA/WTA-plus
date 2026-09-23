#!/usr/bin/env python3
"""Read only the fixed public Git tree and historical T29 evidence; write only /tmp audit JSON."""
from __future__ import annotations
import hashlib
import json
import posixpath
import re
import subprocess
import urllib.parse
from pathlib import Path

ROOT = Path('/srv/WTA-plus')
OUT = Path('/tmp/wta-t29')
HEAD = '9f055ba15d9d5a828fb08cdfb0b24efa32642889'
BASE = '7a1810288d3292ceeb4987f488b13353af1a1286'
HISTORICAL_T29 = '70eed51acb703ed19a48a47d182f2476f3eabcf7'
EVIDENCE = ROOT / 'speculo/.speculo/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence'
DOC_SCOPES = ('docs/', 'backend/', 'frontend/', 'scripts/', 'release-artifacts/', '.agents/skills/')
EXCLUDE_SEGMENTS = {'node_modules', 'target', 'dist', 'coverage'}
LINK_PATTERN = re.compile(r'\]\((?:<([^>]+)>|([^\s)]+))(?:\s+"[^"]*")?\)')

def git(*args: str) -> bytes:
    return subprocess.check_output(['git', *args], cwd=ROOT)

def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def write_json(name: str, value: object) -> None:
    path = OUT / name
    with path.open('w', encoding='utf8') as handle:
        json.dump(value, handle, ensure_ascii=False, indent=2)
        handle.write('\n')
    path.chmod(0o600)

OUT.mkdir(mode=0o700, exist_ok=True)
OUT.chmod(0o700)
assert git('rev-parse', HEAD).decode().strip() == HEAD
assert git('merge-base', BASE, HEAD).decode().strip() == BASE
entries = git('ls-tree', '-r', '-z', HEAD).split(b'\0')
tracked: dict[str, str] = {}
for entry in entries:
    if not entry:
        continue
    metadata, raw_path = entry.split(b'\t', 1)
    mode, kind, object_id = metadata.decode().split(' ')
    if kind == 'blob':
        tracked[raw_path.decode('utf8')] = object_id
# Explicit prefix set is clearer than assuming a local generated directory is a Git-tree directory.
tracked_dirs = set()
for path in tracked:
    parent = posixpath.dirname(path)
    while parent and parent != '.':
        tracked_dirs.add(parent)
        parent = posixpath.dirname(parent)

cache: dict[str, bytes] = {}
def blob(path: str) -> bytes | None:
    object_id = tracked.get(path)
    if object_id is None:
        return None
    if path not in cache:
        cache[path] = git('cat-file', 'blob', object_id)
    return cache[path]

def forbidden_private(path: str) -> bool:
    return path.startswith('temp/') or '/temp/' in path or path.startswith('release-artifacts/builds/')

def local_links(path: str) -> list[dict[str, object]]:
    data = blob(path)
    assert data is not None
    result = []
    text = data.decode('utf8')
    for match in LINK_PATTERN.finditer(text):
        literal = match.group(1) or match.group(2)
        if literal.startswith('#') or literal.startswith('//') or re.match(r'^[a-z][a-z\d+.-]*:', literal, re.I):
            continue
        raw_target = urllib.parse.unquote(literal.split('#', 1)[0])
        target = posixpath.normpath(posixpath.join(posixpath.dirname(path), raw_target))
        outside = target == '..' or target.startswith('../') or target.startswith('/')
        exists = not outside and (target in tracked or target in tracked_dirs)
        result.append({'source': path, 'line': text.count('\n', 0, match.start()) + 1,
                       'literal': literal, 'target': target, 'target_kind':
                       ('outside' if outside else 'file' if target in tracked else 'directory' if target in tracked_dirs else 'missing'),
                       'exists': bool(exists), 'anchor_checked': False})
    return result

docs = sorted(path for path in tracked if path.endswith('.md')
              and (path == 'README.md' or path.startswith(DOC_SCOPES))
              and not any(part in EXCLUDE_SEGMENTS for part in path.split('/'))
              and not forbidden_private(path))
links = [row for path in docs for row in local_links(path)]
modified_docs = set(git('diff', '--name-only', BASE + '...' + HEAD, '--', '.agents/skills').decode().splitlines())
modified_docs = {path for path in modified_docs if path.endswith('.md')}
affected_links = [row for row in links if row['source'] in modified_docs]
link_report = {'fixed_base': BASE, 'fixed_head': HEAD, 'scope': 'tracked Git-tree product/docs/Skill Markdown; excludes generated builds and private temp',
               'document_count': len(docs), 'relative_link_count': len(links),
               'missing_link_count': sum(not row['exists'] for row in links),
               'affected_document_count': len(modified_docs), 'affected_relative_link_count': len(affected_links),
               'anchor_validation': 'not performed in this all-doc scan; historical targeted audit separately validates captured anchors',
               'documents': docs, 'links': links, 'affected_links': affected_links}
write_json('current-document-links-9f055ba.json', link_report)

v4 = json.loads((EVIDENCE / 'T-29-checkpoint-v4.json').read_text())
assert len(v4['files']) == 72
v4_rows = []
for path, previous in sorted(v4['files'].items()):
    assert not forbidden_private(path)
    data = blob(path)
    current = sha(data) if data is not None else None
    v4_rows.append({'path': path, 'v4_sha256': previous, 'current_sha256': current,
                    'status': 'same' if current == previous else 'changed'})
v4_report = {'fixed_head': HEAD, 'historical_checkpoint': 'T-29-checkpoint-v4.json',
             'paths': len(v4_rows), 'same': sum(row['status'] == 'same' for row in v4_rows),
             'changed': [row for row in v4_rows if row['status'] == 'changed'], 'all_paths': v4_rows}
write_json('current-v4-delta-9f055ba.json', v4_report)

protected = json.loads((EVIDENCE / 'T-29-protected-checkpoint.json').read_text())
assert len(protected) == 855 and all(not forbidden_private(path) for path in protected)
protected_rows = []
for path, previous in sorted(protected.items()):
    data = blob(path)
    current = sha(data) if data is not None else None
    protected_rows.append({'path': path, 'historical_sha256': previous, 'current_sha256': current,
                           'exact_match': current == previous})
protected_changed = [row for row in protected_rows if not row['exact_match']]
root_blank_only = False
if len(protected_changed) == 1 and protected_changed[0]['path'] == 'AGENTS.md':
    old = git('show', HISTORICAL_T29 + ':AGENTS.md')
    new = blob('AGENTS.md')
    root_blank_only = sha(old) == protected['AGENTS.md'] and new is not None and (
        [line for line in old.splitlines(keepends=True) if line.strip()] ==
        [line for line in new.splitlines(keepends=True) if line.strip()])
protected_report = {'fixed_head': HEAD, 'historical_checkpoint': 'T-29-protected-checkpoint.json',
                    'paths': len(protected_rows), 'exact_matches': len(protected_rows) - len(protected_changed),
                    'changed_paths': [row['path'] for row in protected_changed],
                    'root_AGENTS_blank_lines_only_reviewed': root_blank_only,
                    'files': protected_rows}
write_json('current-protected-9f055ba.json', protected_report)

manifest = json.loads((EVIDENCE / 'T-29-agents-dispositions.json').read_text())
originals = json.loads((EVIDENCE / 'T-29-originals.json').read_text())
removed_rows = []
for row in manifest:
    if row['decision'] != 'REMOVE':
        continue
    path = row['path']
    original = originals.get(path)
    original_valid = isinstance(original, dict) and isinstance(original.get('content'), str) and (
        sha(original['content'].encode('utf8')) == row['before_sha256'] == original.get('sha256'))
    parent = posixpath.dirname(path)
    effective = None
    while parent and parent != '.':
        candidate = posixpath.join(parent, 'AGENTS.md')
        if candidate in tracked:
            effective = candidate
            break
        parent = posixpath.dirname(parent)
    destination = row['rule_destination']
    destination_blob = blob(destination)
    rule_valid = destination_blob is not None and row['preserved_rule'] in destination_blob.decode('utf8')
    removed_rows.append({'path': path, 'still_removed': path not in tracked,
                         'original_sha_valid': bool(original_valid),
                         'historical_owner': row['owner'], 'effective_owner': effective,
                         'rule_destination': destination, 'shared_rule_present': bool(rule_valid),
                         'navigation_destination': row['navigation_destination'],
                         'navigation_exists': row['navigation_destination'] in tracked})
owner_report = {'fixed_head': HEAD, 'historical_manifest': 'T-29-agents-dispositions.json',
                'removed_count': len(removed_rows),
                'effective_owner_deltas': [row for row in removed_rows if row['effective_owner'] != row['historical_owner']],
                'rows': removed_rows}
write_json('current-owner-9f055ba.json', owner_report)

errors = []
if link_report['missing_link_count']:
    errors.append('missing relative link target')
if link_report['affected_relative_link_count'] != 14:
    errors.append('affected links differ from reviewed 14-link input')
if v4_report['paths'] != 72:
    errors.append('v4 inventory changed')
if protected_changed and not root_blank_only:
    errors.append('unreviewed protected-file change')
if len(removed_rows) != 37 or any(not row['still_removed'] or not row['original_sha_valid'] or
                                   not row['effective_owner'] or not row['shared_rule_present'] or
                                   not row['navigation_exists'] for row in removed_rows):
    errors.append('removed handbook contract failure')
expected_effective_owner_deltas = {
    'backend/wta-modules/wta-profile/wta-profile-bom/AGENTS.md':
        'backend/wta-modules/wta-profile/AGENTS.md',
}
for row in owner_report['effective_owner_deltas']:
    if expected_effective_owner_deltas.get(row['path']) != row['effective_owner']:
        errors.append('unreviewed effective owner delta: ' + row['path'])
summary = {'fixed_base': BASE, 'fixed_head': HEAD, 'exit_code': 1 if errors else 0,
           'errors': errors, 'tracked_markdown': len(docs), 'relative_links': len(links),
           'affected_relative_links': len(affected_links), 'missing_links': link_report['missing_link_count'],
           'v4_same': v4_report['same'], 'v4_changed': len(v4_report['changed']),
           'protected_exact': protected_report['exact_matches'],
           'protected_changed_paths': protected_report['changed_paths'],
           'root_agents_blank_only_reviewed': root_blank_only,
           'removed_rows': len(removed_rows), 'effective_owner_deltas': len(owner_report['effective_owner_deltas']),
           'generated_builds_excluded': True, 'anchors_not_checked_in_all_doc_scan': True}
write_json('current-doc-audit-summary-9f055ba.json', summary)
print(json.dumps(summary, ensure_ascii=False))
raise SystemExit(summary['exit_code'])
