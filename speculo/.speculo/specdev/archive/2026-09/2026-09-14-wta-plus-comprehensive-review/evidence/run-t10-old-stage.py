from pathlib import Path
import tempfile,shutil,subprocess,hashlib,json
root=Path(__file__).resolve().parents[6]
out=Path(__file__).with_name('T-10-old-stage-failure.json')
with tempfile.TemporaryDirectory(prefix='wta-t10-old-stage-') as d:
 r=Path(d)/'release-artifacts'
 (r/'scripts').mkdir(parents=True)
 shutil.copy2(root/'release-artifacts/scripts/release-manage.sh',r/'scripts/release-manage.sh')
 current=r/'builds/current_prod'
 for name in ['wta-admin','wta-monitor-admin','wta-snailjob-server','wta-snailai-server']:
  (current/name).mkdir(parents=True)
  (current/name/(name+'.jar')).write_bytes(b'new-'+name.encode())
  p=r/'docker/backend/images'/name/'app.jar';p.parent.mkdir(parents=True);p.write_bytes(b'old-'+name.encode())
 (current/'admin-web').mkdir();(current/'admin-web/index.html').write_text('new-frontend')
 p=r/'docker/frontend/nginx/html/admin-web/index.html';p.parent.mkdir(parents=True);p.write_text('old-frontend')
 def hashes():
  return {str(p.relative_to(r)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(r.glob('docker/**/*')) if p.is_file()}
 before=hashes()
 result=subprocess.run(['bash',str(r/'scripts/release-manage.sh'),'stage','--env','prod'],capture_output=True,text=True)
 after=hashes()
 data={'command':'isolated old release-manage.sh stage --env prod','failure':'missing admin-web nginx template','exitCode':result.returncode,'diagnostic':result.stderr.replace(d,'<fixture>'),'contextsBefore':before,'contextsAfter':after,'mutatedPaths':[p for p in before if before[p]!=after[p]]}
 assert result.returncode!=0 and len(data['mutatedPaths'])==4
 out.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps({'exitCode':result.returncode,'mutatedBackendContexts':len(data['mutatedPaths'])}))
