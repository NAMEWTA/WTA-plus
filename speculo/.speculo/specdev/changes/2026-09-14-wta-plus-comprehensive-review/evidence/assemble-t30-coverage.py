#!/usr/bin/env python3
"""Read actual serial results and freeze their coverage without changing historical evidence."""
from pathlib import Path
import datetime, hashlib, json, re, subprocess
root=Path(__file__).resolve().parents[6]; e=Path(__file__).resolve().parent
now=datetime.datetime.now(datetime.timezone.utc).isoformat()
def read(name):return json.loads((e/name).read_text())
def write(name,data):(e/name).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
source=read('T-30-source-current.json'); files=source['files']; fingerprint=source['source_fingerprint']
assert all((digest(root/p) if (root/p).is_file() else None)==v for p,v in files.items())
assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip()==source['parent_before']
assert not subprocess.check_output(['git','diff','--cached','--name-only'],cwd=root)
originals=read('T-30-fixture-originals.json');assert len(originals)==6
comparisons=[]
for old_name in ('T-30-source-pre-init-fix.json','T-30-source-pre-fixture-fix.json','T-30-source-pre-profile-tagged-fix.json'):
 old=read(old_name);changed={p:dict(before=old['files'].get(p),after=files.get(p)) for p in sorted(set(files)|set(old['files'])) if files.get(p)!=old['files'].get(p)}
 allowed=set(originals)|{'release-artifacts/scripts/init-mysql-container.sh','release-artifacts/tests/release-integration-contract.test.mjs','release-artifacts/tests/release-config.test.mjs'}
 assert set(changed)<=allowed
 comparisons.append(dict(from_snapshot=old_name,from_fingerprint=old['source_fingerprint'],to_fingerprint=fingerprint,changed=changed,unchanged_paths=len(set(files)&set(old['files']))-len(set(changed)&set(old['files'])),production_backend_unchanged=all(not p.startswith('backend/') or '/src/test/' in p for p in changed),frontend_unchanged=all(not p.startswith('frontend/') for p in changed)))
write('T-30-final-input-equivalence.json',dict(captured_at=now,current_fingerprint=fingerprint,comparisons=comparisons,rule='Historical results retain original fingerprints. Only byte-identical production/frontend inputs are carried forward. All six changed tests ran after correction; latest default Maven and full/core rebuild use the final fingerprint.'))
# The 18 planned commands, each backed by a real exit code; browser conditions are closed separately below.
core=[]
for filename in ('T-30-v2-static.json','T-30-v2-frontend-builds.json','T-30-v2-default-browser.json','T-30-v3-backend-tests.json','T-30-v2-backend-bundles.json','T-30-v2-release.json','T-30-v2-external.json'):
 for row in read(filename):
  assert row['exit_code']==0 and row['source_unchanged']
  core.append({k:row[k] for k in ('number','command','cwd','exit_code','source_fingerprint','log')}|dict(evidence=filename))
assert sorted(x['number'] for x in core)==list(range(1,19))
# Collect successful JUnit reports. A zero-tests record or a failed outer fixture never proves a suite.
suites={}
for path in sorted(e.glob('T-30-*.json')):
 data=json.loads(path.read_text())
 for record in data if isinstance(data,list) else [data]:
  if not isinstance(record,dict) or record.get('exit_code')!=0 or record.get('source_unchanged') is False or record.get('resources_restored') is False:continue
  reports=record.get('suites',[])+record.get('reports',[])
  for report in reports:
   if isinstance(report,str):
    assert len(reports)==1
    name=Path(report).name.removeprefix('TEST-').removesuffix('.xml'); counts=record.get('counts',{})
   else:name=report.get('name','');counts=report
   if not name or counts.get('tests',0)<=counts.get('skipped',0) or any(counts.get(k,0) for k in ('failures','errors')):continue
   suites.setdefault(name,[]).append(dict(evidence=path.name,tests=counts['tests'],skipped=counts.get('skipped',0),source_fingerprint=record.get('source_fingerprint')))
source_classes=[];environment=[]
for path in sorted((root/'backend').rglob('src/test/**/*.java')):
 content=path.read_text()
 if not re.search(r'@(?:Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate)\b',content):continue
 package=re.search(r'^package ([\w.]+);',content,re.M).group(1)
 declared=re.findall(r'^(?:(?:public|abstract|final|static)\s+)*class (\w+)',content,re.M)
 matches={n:r for n,r in suites.items() if any(n==package+'.'+c or n.startswith(package+'.'+c+'$') for c in declared)}
 assert matches, str(path.relative_to(root))+' lacks a successful actual suite'
 row=dict(path=str(path.relative_to(root)),sha256=digest(path),declared_classes=declared,reports=matches)
 source_classes.append(row)
 if any(x in content for x in ('assumeTrue(', '@EnabledIfSystemProperty','@EnabledIfEnvironmentVariable','@Tag("e2e")')):
  assert any(r['skipped']==0 for rows in matches.values() for r in rows)
  environment.append(row)
default=read('T-30-v3-backend-tests.json')[0]
skipped=[]
for row in default['suites']:
 if not row['skipped']:continue
 matches=[r for r in suites[row['name']] if r['skipped']==0]
 assert matches,row['name']
 skipped.append(dict(name=row['name'],default_skipped=row['skipped'],zero_skip_evidence=matches))
assert len(source_classes)==264 and len(environment)==46 and sum(x['default_skipped'] for x in skipped)==117
# All ten persistent browser configurations plus the actual workflow component harness.
browser_specs=[
 ('default',54,[('T-30-v2-default-e2e.log',53,1),('T-30-nacos-browser-v1.log',1,0)]),
 ('accessibility',29,[('T-30-browser-v1-accessibility.log',29,0)]),
 ('lifecycle',20,[('T-30-browser-v1-lifecycle.log',20,0)]),
 ('profile',8,[('T-30-profile-v1.log',8,0)]),
 ('registration',10,[('T-30-browser-v1-registration.log',10,0)]),
 ('sso',19,[('T-30-sso-v1-journey.log',12,0),('T-30-release-origin-v1-fixture-6.log',5,0),('T-30-sso-system-v2.log',2,0)]),
 ('transfer',5,[('T-30-browser-v1-transfer.log',5,0)]),
 ('transport',6,[('T-30-http-v1.log',6,0)]),
 ('upload',10,[('T-30-oss-v1.log',10,0)]),
 ('sso-security',2,[('T-30-sso-v1-security.log',2,0)]),
 ('workflow-dialog',10,[('T-30-dialog-v1-2.log',10,0)])]
browsers=[]
for name,expected,logs in browser_specs:
 records=[]
 for log,passed,skip in logs:
  text=re.sub(r'\x1b\[[0-9;]*[A-Za-z]','',(e/log).read_text())
  actual=sum(map(int,re.findall(r'^\s*(\d+) passed \(',text,re.M)))
  assert actual==passed,(log,actual,passed)
  assert not re.search(r'^\s*\d+ failed',text,re.M),log
  records.append(dict(log=log,sha256=digest(e/log),passed=actual,skipped=skip))
 assert sum(r['passed'] for r in records)==expected
 browsers.append(dict(group=name,passed=expected,evidence=records,conditional_skip_closed_by='T-30-nacos-browser-v1.json' if name=='default' else None))
assert sum(b['passed'] for b in browsers)==173
unit_text=re.sub(r'\x1b\[[0-9;]*[A-Za-z]','',(e/'T-30-v2-static-5.log').read_text())
frontend_unit=sum(map(int,re.findall(r'^\s*Tests\s+(\d+) passed',unit_text,re.M)))+sum(map(int,re.findall(r'^[ℹ#] pass (\d+)',unit_text,re.M)))
assert frontend_unit==722
coverage=dict(captured_at=now,source_fingerprint=fingerprint,parent_before=source['parent_before'],implementation_commit=None,result_sha=None,candidate=None,core_commands=sorted(core,key=lambda x:x['number']),frontend_unit_passed=frontend_unit,default_backend=default['counts'],nondefault_backend=read('T-30-nondefault-v1.json')['counts'],source_test_file_count=len(source_classes),source_test_files=source_classes,environment_class_count=len(environment),environment_classes=environment,default_skip_closures=skipped,browser_groups=browsers,persistent_browser_passed=163,workflow_dialog_passed=10,required_not_run=[],required_failed=[],declared_nonacceptance_skip='DemoUnitTest.testDisabled: existing intentionally disabled JUnit teaching placeholder',source_input_equivalence='T-30-final-input-equivalence.json')
write('T-30-coverage-final.json',coverage)
inputs=read('T-30-input-current.json');upstream=read('T-30-input-pre-fixture-fix.json')['files'];own={p:files[p] for p in originals}
overlap=set(upstream)&set(own);nonoverlap=set(upstream)-set(own)
assert all((digest(root/p) if (root/p).is_file() else None)==upstream[p] for p in nonoverlap)
assert all((digest(root/p) if (root/p).is_file() else None)==v for p,v in inputs['files'].items())
write('T-30-checkpoint.json',dict(captured_at=now,status='local-verification-complete-pending-governance',parent_before=source['parent_before'],implementation_commit=None,result_sha=None,candidate=None,files=own,source_fingerprint=fingerprint,input_checkpoint='T-30-input-pre-fixture-fix.json',verified_nonoverlapping_upstream=len(nonoverlap),overlapping_upstream=sorted(overlap),cumulative_file_count=len(inputs['files']),diff='T-30-owned.diff',coverage='T-30-coverage-final.json'))
print(json.dumps(dict(core_commands=len(core),frontend_unit_passed=frontend_unit,source_test_files=len(source_classes),environment_classes=len(environment),default_skipped_closed=sum(x['default_skipped'] for x in skipped),browser_passed=173,owned_paths=len(own),overlap=len(overlap),nonoverlap=len(nonoverlap),cumulative=len(inputs['files']))))
