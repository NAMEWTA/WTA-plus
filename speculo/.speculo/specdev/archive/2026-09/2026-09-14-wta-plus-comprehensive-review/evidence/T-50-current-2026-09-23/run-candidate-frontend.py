import os, subprocess, json, pathlib, sys, hashlib
root=pathlib.Path('/srv/WTA-plus'); folder=pathlib.Path('/tmp/wta-t50')
head,label=sys.argv[1:]
assert len(head)==40 and label.isalnum()
assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip()==head
assert not subprocess.check_output(['git','status','--porcelain','--untracked-files=all'],cwd=root,text=True)
env=os.environ.copy();env['PATH']='/tmp/wta-t02-c1/tool-bin:'+env['PATH']
version=subprocess.check_output(['corepack','pnpm','--version'],cwd=root/'frontend',env=env,text=True).strip()
assert version=='10.34.5',version
(folder/(label+'-frontend-environment.json')).write_text(json.dumps({'head':head,'pnpm':version,'path_prefix':'/tmp/wta-t02-c1/tool-bin','shim_sha256':hashlib.sha256(pathlib.Path('/tmp/wta-t02-c1/tool-bin/pnpm').read_bytes()).hexdigest(),'build_environment':{'VITE_APP_CONTEXT_PATH':'/','VITE_APP_BASE_API':'/prod-api'}},indent=2)+'\n')
for name,command in [('architecture',['corepack','pnpm','architecture:check']),('lint',['corepack','pnpm','lint']),('typecheck',['corepack','pnpm','typecheck']),('test',['corepack','pnpm','test']),('build',['env','VITE_APP_CONTEXT_PATH=/','VITE_APP_BASE_API=/prod-api','corepack','pnpm','build:prod'])]:
 result=subprocess.run(['python3','/tmp/wta-check.py',str(folder),label+'-frontend-'+name,str(root/'frontend'),*command],env=env)
 if result.returncode: sys.exit(result.returncode)
assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip()==head
assert not subprocess.check_output(['git','status','--porcelain','--untracked-files=all'],cwd=root,text=True)
print('Candidate B frontend gates complete')
