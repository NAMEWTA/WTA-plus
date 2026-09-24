"""Lead-only, immutable source and fresh XML capture for a selected T42 gate."""
from pathlib import Path
import datetime,hashlib,json,os,shutil,subprocess,sys,xml.etree.ElementTree as ET
r=Path('/srv/WTA-plus');out=Path('/tmp/wta-t42')
expected,label,selector=sys.argv[1:]
assert len(expected)==40 and label.replace('-','').isalnum()
def identity():
 h=subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip()
 t=subprocess.check_output(['git','rev-parse','HEAD^{tree}'],cwd=r,text=True).strip()
 d=subprocess.check_output(['git','status','--porcelain=v1','--untracked-files=all'],cwd=r,text=True).strip()
 assert h==expected and not d,'source changed or dirty'
 return dict(head=h,tree=t,clean=True)
before=identity();began=datetime.datetime.now(datetime.timezone.utc)
cmd=['env','JAVA_TOOL_OPTIONS=-Xms128m -Xmx1536m','./mvnw','-B','-ntp','-Pdev','-pl','wta-admin','-am','-Dtest='+selector,'-Dsurefire.failIfNoSpecifiedTests=false','test']
result=subprocess.run(['python3','/tmp/wta-check.py',str(out),label,str(r/'backend'),*cmd])
after=identity();target=out/(label+'-reports');target.mkdir(exist_ok=False);reports=[]
for p in sorted((r/'backend').glob('**/target/surefire-reports/TEST-*.xml')):
 if p.stat().st_mtime<began.timestamp():continue
 q=target/p.relative_to(r/'backend');q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q)
 root=ET.parse(p).getroot();cases=[]
 for case in root.findall('testcase'):
  failure=case.find('failure');error=case.find('error');node=failure if failure is not None else error
  if node is not None:cases.append(dict(name=case.attrib['name'],kind=node.tag,type=node.attrib.get('type')))
 reports.append(dict(file=str(q.relative_to(out)),sha256=hashlib.sha256(q.read_bytes()).hexdigest(),suite=root.attrib.get('name'),counts={k:int(root.attrib.get(k,0)) for k in ['tests','failures','errors','skipped']},failed_cases=cases))
record=dict(source_before=before,source_after=after,exit_code=result.returncode,reports=reports,counts={k:sum(x['counts'][k] for x in reports) for k in ['tests','failures','errors','skipped']})
with (out/(label+'-source-and-counts.json')).open('x') as f:json.dump(record,f,indent=2);f.write('\n')
print(json.dumps(dict(exit_code=result.returncode,counts=record['counts'],suites=len(reports))))
sys.exit(result.returncode)
