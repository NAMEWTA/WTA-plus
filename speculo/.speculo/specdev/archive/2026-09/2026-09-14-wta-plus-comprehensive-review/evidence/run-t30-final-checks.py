from pathlib import Path
import json,hashlib,subprocess,sys,time
root=Path(__file__).resolve().parents[6];e=Path(__file__).resolve().parent;c=e.parent
mode,name=sys.argv[1:3];assert name.startswith('T-30-') and '/' not in name
assert not (e/(name+'.json')).exists()
source=json.loads((e/'T-30-source-current.json').read_text())
def source_ok():return all((hashlib.sha256((root/p).read_bytes()).hexdigest() if (root/p).is_file() else None)==h for p,h in source['files'].items())
if mode=='quality':
 commands=[['node','.agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs',p,'--mode','layered'] for p in ['backend/wta-modules/wta-sso','backend/wta-modules/wta-notify','backend/wta-modules/wta-third','backend/wta-modules/wta-profile/wta-profile-person','backend/wta-modules/wta-profile/wta-profile-enterprise']]
 commands+=[['node','.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs'],['node','--test','.agents/skills/engineering-standards/scripts/validate-skill-facts.test.mjs','.agents/skills/namewta-fullstack-development/scripts/validate-module-mode.test.mjs']]
else:
 assert mode=='governance'
 commands=[['node','speculo/workflows/specdev/common/tools/validate-specdev.mjs','--stage',s,'--repo',str(root),str(c)] for s in ['tickets','goal-plan']]
 commands+=[['node','speculo/workflows/specdev/common/tools/ticket-control.mjs','--map',str(c/'tickets-map.md'),'--repo',str(root)],['git','diff','--check']]
results=[]
for index,command in enumerate(commands,1):
 assert source_ok();log=e/(name+'-'+str(index)+'.log');start=time.time()
 with log.open('w') as out:r=subprocess.run(command,cwd=root,stdout=out,stderr=subprocess.STDOUT)
 row=dict(command=command,cwd='.',exit_code=r.returncode,log=log.name,seconds=round(time.time()-start,2),source_fingerprint=source['source_fingerprint'],source_unchanged=source_ok());results.append(row)
 (e/(name+'.json')).write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
 print(index,'exit',r.returncode,flush=True)
 if r.returncode or not row['source_unchanged']:print(log.read_text()[-8000:]);raise SystemExit(1)
assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip()==source['parent_before']
assert not subprocess.check_output(['git','diff','--cached','--name-only'],cwd=root)
