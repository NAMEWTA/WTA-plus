#!/usr/bin/env python3
"""Serial HTTP/browser gates on owned loopback fixtures, with source and fresh report guards."""
import datetime
import hashlib
import json
import os
import pathlib
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
root = pathlib.Path(__file__).resolve().parents[6]
evidence = pathlib.Path(__file__).resolve().parent
name = sys.argv[1]
assert name.startswith("T-30-") and "/" not in name
def source_snapshot():
    paths = subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=root).decode().split('\0')
    prefixes = ('.agents/', '.github/', 'backend/', 'frontend/', 'scripts/', 'release-artifacts/', 'docs/', 'speculo/workflows/')
    files = {p: hashlib.sha256((root/p).read_bytes()).hexdigest() if (root/p).is_file() else None
             for p in sorted(set(paths)) if p and (p.startswith(prefixes) or '/' not in p)}
    return files, hashlib.sha256(json.dumps(files, sort_keys=True, separators=(',', ':')).encode()).hexdigest()

source_files, source_fingerprint = source_snapshot()
assert source_files == json.loads((evidence/'T-30-source-current.json').read_text())['files']
assert not (evidence/(name+'.json')).exists(), 'preserve earlier results'
assert not (evidence/(name+'.log')).exists(), 'preserve earlier logs'

def inventory():
    return {kind: sorted(subprocess.check_output(command, text=True).splitlines()) for kind, command in {
        'containers': ['docker', 'ps', '-aq', '--no-trunc'], 'networks': ['docker', 'network', 'ls', '-q', '--no-trunc'],
        'volumes': ['docker', 'volume', 'ls', '-q']}.items()}
record = dict(captured_at=datetime.datetime.now(datetime.timezone.utc).isoformat(), before=inventory(),
              source_fingerprint=source_fingerprint, cwd='backend')
(evidence/(name+'-source.json')).write_text(json.dumps(dict(files=source_files, source_fingerprint=source_fingerprint), indent=2)+'\n')
record['results'] = []
started = time.time()
for kind, command in [('unprivileged',[sys.executable,str(evidence/'run-t10-unprivileged.py'),name+'-unprivileged']),('container',['node',str(evidence/'run-t10-container.mjs'),name+'-container'])]:
    print('START', ' '.join(command),flush=True)
    with (evidence/(name+'-'+kind+'-driver.log')).open('w') as log:
        result = subprocess.run(command,cwd=root,stdout=log,stderr=subprocess.STDOUT)
    child = json.loads((evidence/(name+'-'+kind+'.json')).read_text())
    record['results'].append(dict(command=command, exit_code=result.returncode, evidence=name+'-'+kind+'.json'))
    print(kind, result.returncode,flush=True)
    if result.returncode: break
record.update(after=inventory(),source_unchanged=source_snapshot()[0]==source_files,seconds=round(time.time()-started,2))
record['resources_restored'] = record['before']==record['after']
record['exit_code'] = 0 if len(record['results'])==2 and all(x['exit_code']==0 for x in record['results']) and record['resources_restored'] and record['source_unchanged'] else 1
(evidence/(name+'.json')).write_text(json.dumps(record,indent=2)+'\n')
print(json.dumps({k:record[k] for k in ('exit_code','resources_restored','source_unchanged','seconds')}),flush=True)
raise SystemExit(record['exit_code'])
