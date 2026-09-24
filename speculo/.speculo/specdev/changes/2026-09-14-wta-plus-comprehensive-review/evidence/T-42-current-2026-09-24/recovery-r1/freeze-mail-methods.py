"""Inventory only: freeze all plain JUnit tests for an exact clean T42 source."""
from pathlib import Path
import hashlib,json,os,re,subprocess,sys
root=Path('/srv/WTA-plus')
expected,destination=sys.argv[1:]
if not re.fullmatch(r'[0-9a-f]{40}',expected):raise SystemExit('Full commit required')
def git(*args):return subprocess.check_output(['git',*args],cwd=root,text=True).strip()
def clean():
 if git('rev-parse','HEAD')!=expected or git('status','--porcelain','--untracked-files=all'):raise SystemExit('Exact clean candidate required')
clean()
name='org.namewta.test.notify.NotifyMailAttachmentIntegrationTest'
path='backend/wta-admin/src/test/java/'+name.replace('.','/')+'.java'
raw=subprocess.check_output(['git','show',expected+':'+path],cwd=root)
src=raw.decode()
if re.search(r'@(ParameterizedTest|TestFactory|RepeatedTest|Nested)\b',src):raise SystemExit('Explicit invocation inventory required for non-plain tests')
methods=re.findall(r'@Test\b(?:(?!@Test\b).)*?\bvoid\s+(\w+)\s*\(',src,re.S)
if not methods or len(methods)!=len(set(methods)) or len(methods)!=len(re.findall(r'@Test\b',src)):raise SystemExit('Incomplete or ambiguous method inventory')
clean()
p=Path(destination)
if p.parent.resolve()!=Path('/tmp/wta-t42'):raise SystemExit('Private output required')
record=dict(candidate_head=expected,source_path=path,source_sha256=hashlib.sha256(raw).hexdigest(),suite=name,tests=len(methods),methods=methods,expected_methods=','.join(methods),status='INVENTORY_ONLY_NOT_EXECUTED')
fd=os.open(p,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
with os.fdopen(fd,'w') as f:json.dump(record,f,indent=2);f.write('\n')
print(json.dumps(dict(tests=len(methods),manifest=str(p),status=record['status'])))
