#!/usr/bin/env python3
"""Run one Maven phase and preserve fresh report counts plus a source fingerprint."""
import argparse
import datetime
import hashlib
import json
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('--evidence-name', required=True)
parser.add_argument('phase', choices=['test', 'full', 'core'])
args = parser.parse_args()
root = Path(__file__).resolve().parents[6]
evidence = Path(__file__).resolve().parent
command = {
    'test': ['./mvnw', 'test'],
    'full': ['./mvnw', 'clean', 'package', '-DskipTests'],
    'core': ['./mvnw', 'clean', 'package', '-Pbundle-core', '-Dmaven.test.skip=true'],
}[args.phase]
network_options = '-Daether.connector.requestTimeout=60000 -Daether.connector.connectTimeout=10000 -Daether.connector.basic.threads=1 -Dmaven.artifact.threads=1'
environment = os.environ.copy()
environment['MAVEN_OPTS'] = environment.get('MAVEN_OPTS', '') + ' ' + network_options


def sources():
    result = subprocess.run(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z',
                             'backend', 'release-artifacts/docker/infrastructure/mysql/init'],
                            cwd=root, capture_output=True, check=True)
    files = {}
    for name in sorted(set(result.stdout.decode().strip('\0').split('\0'))):
        path = root / name
        if path.is_file():
            files[name] = hashlib.sha256(path.read_bytes()).hexdigest()
    return {'sha256': hashlib.sha256(json.dumps(files, sort_keys=True).encode()).hexdigest(), 'files': files}


before = sources()
start = time.time()
with (evidence / f'{args.evidence_name}.log').open('w') as log:
    process = subprocess.Popen(command, cwd=root / 'backend', env=environment,
                               stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    for line in process.stdout:
        log.write(line)
        log.flush()
        if re.search(r'\[ERROR\]|BUILD (SUCCESS|FAILURE)|\[INFO\] Building ', line):
            print(line.rstrip(), flush=True)
    exit_code = process.wait()
reports = []
for path in (root / 'backend').glob('**/target/surefire-reports/TEST-*.xml'):
    if path.stat().st_mtime < start:
        continue
    suite = ET.parse(path).getroot()
    reports.append({'file': str(path.relative_to(root)), 'name': suite.attrib['name'],
                    **{key: int(suite.attrib[key]) for key in ['tests', 'failures', 'errors', 'skipped']}})
after = sources()
result = {
    'command': command, 'cwd': 'backend', 'exit_code': exit_code,
    'per_command_network_options': network_options,
    'started_at': datetime.datetime.fromtimestamp(start, datetime.timezone.utc).isoformat(),
    'seconds': round(time.time() - start, 2), 'input_sources_sha256': before['sha256'],
    'output_sources_sha256': after['sha256'], 'sources_unchanged': before['sha256'] == after['sha256'],
    'totals': {key: sum(row[key] for row in reports) for key in ['tests', 'failures', 'errors', 'skipped']},
    'reports': reports,
}
(evidence / f'{args.evidence_name}-sources.json').write_text(json.dumps(before, indent=2) + '\n')
(evidence / f'{args.evidence_name}.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps({key: result[key] for key in ['exit_code', 'seconds', 'totals', 'sources_unchanged']}), flush=True)
raise SystemExit(exit_code if exit_code else (0 if result['sources_unchanged'] else 1))
