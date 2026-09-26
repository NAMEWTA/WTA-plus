from pathlib import Path
import json,xml.etree.ElementTree as ET,datetime,hashlib,subprocess,sys
folder=Path(sys.argv[1]);name=sys.argv[2];record=json.loads((folder/(name+'.json')).read_text()); started=datetime.datetime.fromisoformat(record['started_at']).timestamp();root=Path('/srv/WTA-plus');rows=[]
for path in sorted((root/'backend').glob('**/target/surefire-reports/TEST-*.xml')):
 if path.stat().st_mtime < started: continue
 xml=ET.parse(path).getroot(); counts={k:int(xml.attrib[k]) for k in ['tests','failures','errors','skipped']}
 rows.append(dict(classname=xml.get('name'),path=str(path.relative_to(root)),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),counts=counts,methods=[dict(name=t.get('name'),skipped=t.find('skipped') is not None,failure=t.find('failure') is not None,error=t.find('error') is not None) for t in xml.findall('testcase')]))
total={k:sum(r['counts'][k] for r in rows) for k in ['tests','failures','errors','skipped']}
assert rows and total['tests']>0
result=dict(command_record=name+'.json',head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),fresh_classes=len(rows),counts=total,classes=rows,all_environment_tests_enabled=False)
(folder/(name+'-counts.json')).write_text(json.dumps(result,indent=2)+'\n'); print(json.dumps(dict(fresh_classes=len(rows),counts=total,exit_code=record['exit_code'])))
assert record['exit_code']==0 and total['failures']==total['errors']==0
