from pathlib import Path
import json,xml.etree.ElementTree as ET,datetime,hashlib,subprocess,sys
folder=Path(sys.argv[1]);name=sys.argv[2];selection=Path(sys.argv[3]);root=Path('/srv/WTA-plus/backend')
record=json.loads((folder/(name+'.json')).read_text());assert record['exit_code']==0
started=datetime.datetime.fromisoformat(record['started_at']).timestamp();rows=[]
for item in json.loads(selection.read_text())['classes']:
 matches=list(root.glob('**/target/surefire-reports/TEST-'+item['class']+'.xml'))
 assert len(matches)==1,(item['class'],'missing or ambiguous XML')
 path=matches[0];assert path.stat().st_mtime>=started,(item['class'],'stale XML')
 xml=ET.parse(path).getroot();counts={k:int(xml.attrib[k]) for k in ['tests','failures','errors','skipped']}
 assert counts['tests']>0 and not any(counts[k] for k in ['failures','errors','skipped']),(item['class'],counts)
 rows.append({'class':item['class'],'path':str(path.relative_to(root)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),**counts,'methods':[x.attrib['name'] for x in xml.findall('testcase')]})
result={'source':subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),'classes':rows,'totals':{k:sum(row[k] for row in rows) for k in ['tests','failures','errors','skipped']}}
out=folder/(name+'-counts.json');assert not out.exists();out.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({'classes':len(rows),**result['totals']}))
