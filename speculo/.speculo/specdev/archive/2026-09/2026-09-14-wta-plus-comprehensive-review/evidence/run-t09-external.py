#!/usr/bin/env python3
"""Run the real CI service entry serially and retain test/resource evidence."""
import argparse
import datetime
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('--evidence-name', required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parents[6]
evidence = Path(__file__).resolve().parent


def ids(arguments):
    result = subprocess.run(['docker', *arguments], capture_output=True, text=True, check=True)
    return sorted(result.stdout.splitlines())


def snapshot():
    return {
        'containers': ids(['ps', '-aq', '--no-trunc']),
        'networks': ids(['network', 'ls', '-q', '--no-trunc']),
        'volumes': ids(['volume', 'ls', '-q']),
    }


before = snapshot()
start = time.time()
command = ['bash', 'scripts/ci/run-external-services.sh']
with (evidence / f'{args.evidence_name}.log').open('w') as log:
    process = subprocess.Popen(command, cwd=root, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    for line in process.stdout:
        log.write(line)
        if re.search(r'ERROR|Tests run:|BUILD (SUCCESS|FAILURE)|\[INFO\].*(?:tables|database|initialized)', line):
            print(line.rstrip(), flush=True)
    exit_code = process.wait()
after = snapshot()
images = []
for image in ['redis:8.6.3', 'mysql:8.4.9', 'pgsty/minio:RELEASE.2026-04-17T00-00-00Z']:
    result = subprocess.run(['docker', 'image', 'inspect', image, '--format', '{{json .RepoDigests}}'],
                            capture_output=True, text=True)
    images.append({'image': image, 'exit_code': result.returncode,
                   'digests': json.loads(result.stdout) if result.returncode == 0 else None})
reports = []
for report in (root / 'backend').glob('**/target/surefire-reports/TEST-*.xml'):
    if report.stat().st_mtime < start:
        continue
    suite = ET.parse(report).getroot()
    reports.append({key: suite.attrib[key] for key in ['name', 'tests', 'failures', 'errors', 'skipped']})
result = {
    'command': command, 'cwd': '.', 'started_at': datetime.datetime.fromtimestamp(start, datetime.timezone.utc).isoformat(),
    'exit_code': exit_code, 'seconds': round(time.time() - start, 2), 'images': images,
    'resource_before': before, 'resource_after': after, 'resources_unchanged': before == after, 'reports': reports,
}
(evidence / f'{args.evidence_name}.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps({key: result[key] for key in ['exit_code', 'seconds', 'resources_unchanged', 'reports']}), flush=True)
raise SystemExit(exit_code if exit_code else (0 if before == after else 1))
