#!/usr/bin/env python3
"""Read-only T-29 link, source inventory, ownership and rule preservation audit."""
from pathlib import Path
import hashlib
import json
import re
import subprocess
import urllib.parse

root = Path(__file__).resolve().parents[6]
evidence = Path(__file__).resolve().parent
originals = json.loads((evidence/'T-29-originals.json').read_text())
manifest = json.loads((evidence/'T-29-agents-dispositions.json').read_text())
report = dict(links=[], errors=[], commands=[], module_rows=[], nonliteral_code_span_review=[])

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else None

def fail(message):
    report['errors'].append(message)

for relative in originals:
    file = root/relative
    if not file.exists(): continue
    content = file.read_text()
    for url in re.findall(r'\]\(([^\s)]+)\)', content):
        if '://' in url or url.startswith('mailto:'): continue
        part, _, anchor = url.partition('#')
        target = (file.parent/urllib.parse.unquote(part)).resolve() if part else file
        row = dict(file=relative, url=url, exists=target.exists())
        if not row['exists']: fail('Missing link '+relative+': '+url)
        elif anchor and target.is_file() and target.suffix == '.md':
            headings = re.findall(r'^#{1,6}\s+(.+)$', target.read_text(), re.M)
            slugs = [re.sub(r'[^\w\-\s]', '', h.lower()).replace(' ', '-') for h in headings]
            row['anchor_exists'] = urllib.parse.unquote(anchor) in slugs
            if not row['anchor_exists']: fail('Missing anchor '+relative+': '+url)
        report['links'].append(row)

# Literal module paths and their test roots are checked independently of prose links.
module_map = (root/'.agents/skills/engineering-standards/references/project/01-module-map.md').read_text()
body = module_map.split('## 后端 Maven 模块')[1].split('## 依赖方向')[0]
rows = {}
for line in body.splitlines():
    match = re.match(r'\| `([^`]+)` \|', line)
    if match: rows[match.group(1)] = line
source_modules = {str(p.parent.relative_to(root/'backend')) for p in (root/'backend').rglob('pom.xml') if 'target' not in p.parts and p.parent != root/'backend'}
if set(rows) != source_modules: fail('Module map and POM inventory differ: '+str(set(rows)^source_modules))
for module, line in rows.items():
    actual = bool(list((root/'backend'/module/'src/test/java').rglob('*.java')))
    declared = '`src/test/java`' in line
    if actual != declared: fail('Test root mismatch: '+module)
    report['module_rows'].append(dict(module=module, actual_test_root=actual, declared_test_root=declared))

# Inventory commands are copied from the current canonical project profile.
commands = [
    ['rg', '--files', 'backend', '-g', 'pom.xml', '-g', '!**/target/**'],
    ['rg', '--files', 'frontend/apps', '-g', 'package.json', '-g', '!**/node_modules/**', '-g', '!**/dist/**'],
    ['rg', '--files', 'backend', '-g', '**/src/test/java/**/*.java', '-g', '!**/target/**'],
]
for command in commands:
    result = subprocess.run(command, cwd=root, capture_output=True, text=True)
    report['commands'].append(dict(command=command, cwd='.', exit_code=result.returncode, count=len(result.stdout.splitlines())))
    if result.returncode: fail('Inventory command failed: '+str(command))

report['special_rules_checked'] = 0
removed = {row['path'] for row in manifest if row['decision'] == 'REMOVE'}
for row in manifest:
    path = root/row['path']
    if row['path'] in removed:
        if path.exists(): fail('Removed leaf still exists: '+row['path'])
        if not (root/row['owner']).exists(): fail('Missing inherited owner: '+row['owner'])
        if row['preserved_rule'] not in (root/row['rule_destination']).read_text(): fail('Lost shared MUST')
    else:
        if not path.is_file(): fail('Missing retained handbook: '+row['path']); continue
        for rule in row.get('unique_rules', []):
            report['special_rules_checked'] += 1
            if rule not in path.read_text(): fail('Lost special rule: '+row['path'])

# Current Markdown/source/config references only; historical Speculo records intentionally keep original paths.
files = subprocess.check_output(['rg', '--files', '--hidden', '-g', '*.md', '-g', '*.mjs', '-g', '*.json', '-g', '!speculo/**', '-g', '!temp/**', '-g', '!**/node_modules/**', '-g', '!**/target/**', '-g', '!.git/**'], cwd=root, text=True).splitlines()
report['remaining_removed_references'] = []
for relative in files:
    file = root/relative
    try: content = file.read_text()
    except UnicodeError: continue
    for path in removed:
        if path in content: report['remaining_removed_references'].append(dict(file=relative, target=path))
    for url in re.findall(r'\]\(([^\s)#]+)(?:#[^)]*)?\)', content):
        if '://' in url: continue
        target = (file.parent/url).resolve()
        if target.is_relative_to(root) and str(target.relative_to(root)) in removed:
            report['remaining_removed_references'].append(dict(file=relative, target=str(target.relative_to(root))))
if report['remaining_removed_references']: fail('References to removed handbooks remain')

protected = json.loads((evidence/'T-29-protected-checkpoint.json').read_text())
report['protected_files_count'] = len(protected)
report['protected_files_unchanged'] = all(sha(root/path) == expected for path, expected in protected.items())
if not report['protected_files_unchanged']: fail('Protected file changed')

# Every documented concrete Maven selector and filtered pnpm script must have an actual owner.
selectors = set()
for relative in originals:
    path = root/relative
    if not path.exists(): continue
    content = path.read_text()
    for selector in re.findall(r'-pl\s+([\w/,.-]+)', content):
        for module in selector.split(','):
            selectors.add(module)
            if not (root/'backend'/module/'pom.xml').exists(): fail('Missing Maven selector '+module)
    for package, script in re.findall(r'pnpm --filter (@[\w/-]+) ([\w:-]+)', content):
        matches = []
        for candidate in (root/'frontend').glob('**/package.json'):
            if 'node_modules' in candidate.parts or 'dist' in candidate.parts: continue
            data = json.loads(candidate.read_text())
            if data.get('name') == package: matches.append(data)
        if len(matches) != 1 or script not in matches[0].get('scripts', {}): fail('Missing pnpm script '+package+' '+script)
report['maven_selectors'] = sorted(selectors)

# Nonliteral spans flagged in the first scan are classified explicitly, not silently counted as files.
report['nonliteral_code_span_review'] = [
    dict(kind='module_relative', examples=['src/main/java', 'src/main/resources', 'src/test/java', 'controller/admin', 'controller/anonymous', 'adapter/http', 'adapter/gateway'], proof='Maven table checks real test roots; Notify/SSO named directories are beneath their source package; aggregate src/main is explicitly negated'),
    dict(kind='generated_or_operator_input', examples=['dist/build-mode.json', 'release-artifacts/.env', 'docker/runtime'], proof='Per-App build metadata or ignored env/runtime destination; not required checked-in source'),
    dict(kind='explicit_absence_or_prohibition', examples=['.vscode/settings.json', '.claude/.codex', 'admin-web/src/api'], proof='Current text says absent/not required/do not copy; no command requires these sources'),
    dict(kind='protocol_schema_or_notation', examples=['GET/HEAD', 'PUT/POST/DELETE', 'UNVERIFIED/NOT_SERVING', 'bundle-full/core', 'clean/package/install', 'version/create_dept/create_time/create_by/update_time/update_by/del_flag', 'nacos/nacos-server:v2.5.4', 'pgsty/minio:RELEASE.2026-04-17T00-00-00Z'], proof='HTTP methods, status enum, profile list, field list, or image coordinates; not filesystem references'),
    dict(kind='scope_or_abbreviation', examples=['path:frontend/packages/adapters/axios-browser/src/index.ts', 'wta-common-web/.../logging', 'service/impl', 'domain/model/read'], proof='path: resolves after removing scope prefix; ellipsis and directory roles explicitly describe module-relative source contracts'),
]
for path in ['backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin', 'backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/anonymous', 'backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/adapter/http', 'backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/adapter/gateway', 'frontend/packages/adapters/axios-browser/src/index.ts']:
    if not (root/path).exists(): fail('Semantic path missing '+path)
report['exit_code'] = 1 if report['errors'] else 0
(evidence/'T-29-audit-final.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
print(json.dumps({key: report[key] for key in ['exit_code', 'errors', 'special_rules_checked', 'protected_files_count', 'protected_files_unchanged', 'commands', 'maven_selectors']}, ensure_ascii=False, indent=2))
print('links', len(report['links']), 'module rows', len(report['module_rows']), 'removed references', len(report['remaining_removed_references']))
raise SystemExit(report['exit_code'])
