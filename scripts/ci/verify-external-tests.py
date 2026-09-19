#!/usr/bin/env python3
"""Fail CI when any requested real-service test did not run successfully this time."""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

backend, since_ns, *expected = sys.argv[1:]
if not expected:
    raise SystemExit('No required external-service tests were declared')
reports = list(Path(backend).glob('**/target/surefire-reports/TEST-*.xml'))
total = 0
for name in expected:
    matching = [path for path in reports if path.name.endswith(f'.{name}.xml')
                and path.stat().st_mtime_ns >= int(since_ns)]
    if len(matching) != 1:
        raise SystemExit(f'{name}: expected one fresh report, found {len(matching)}')
    suite = ET.parse(matching[0]).getroot()
    tests = int(suite.attrib['tests'])
    if tests <= 0 or any(int(suite.attrib.get(key, '0')) for key in ['failures', 'errors', 'skipped']):
        raise SystemExit(f'{name}: required tests must execute with zero failures, errors and skips')
    total += tests
print(f'External-service evidence: {len(expected)} required classes, {total} passed tests, zero skipped')
