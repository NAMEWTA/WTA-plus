from pathlib import Path
import os,json,subprocess,hashlib,sys
r=Path('/srv/WTA-plus');t=Path('/tmp/wta-t45');head,label=sys.argv[1:];prior='be35584b0d8dcfbb560dc9f338c303dafc77fbfd'
assert len(head)==40 and label.isalnum()
def git(*args):return subprocess.check_output(['git',*args],cwd=r,text=True).strip()
def identity():
 assert git('rev-parse','HEAD')==head and not git('status','--porcelain','--untracked-files=all')
 return dict(head=head,tree=git('rev-parse','HEAD^{tree}'),clean=True)
def save(name,data):
 with (t/(label+'-'+name+'.json')).open('x') as f:json.dump(data,f,indent=2);f.write('\n')
equivalence={path:{'prior':git('rev-parse',prior+':'+path),'current':git('rev-parse',head+':'+path)} for path in ['frontend','.agents','scripts']}
assert all(x['prior']==x['current'] for x in equivalence.values())
records=[]
for name in ['architecture','architecture-tests','openapi','lint','typecheck','test']:
 p=t/('r1-frontend-'+name+'.json');d=json.loads(p.read_text());assert d['exit_code']==0
 records.append(dict(file=p.name,sha256=hashlib.sha256(p.read_bytes()).hexdigest()))
save('frontend-evidence-reuse',dict(prior_head=prior,current=identity(),input_trees=equivalence,records=records,statement='Reuse exact unchanged frontend input gates; production build rerun at current source below'))
env=os.environ.copy();env['PATH']='/tmp/wta-t02-c1/tool-bin:'+env['PATH']
assert subprocess.check_output(['corepack','pnpm','--version'],cwd=r/'frontend',env=env,text=True).strip()=='10.34.5'
subprocess.run(['python3','/tmp/wta-check.py',str(t),label+'-frontend-build',str(r/'frontend'),'env','VITE_APP_CONTEXT_PATH=/','VITE_APP_BASE_API=/prod-api','corepack','pnpm','build:prod'],env=env,check=True)
save('frontend-source-after',identity())
for helper in ['record-frontend-artifacts.py','prepare-ui-dist-proof.py']:
 subprocess.run(['python3',str(t/helper),head,label],check=True)
