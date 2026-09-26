from pathlib import Path
import json,subprocess,sys
root=Path('/srv/WTA-plus');out=Path('/tmp/wta-t50');expected=sys.argv[1]
def identity():
 g=lambda *a:subprocess.check_output(['git',*a],cwd=root,text=True).strip()
 assert g('rev-parse','HEAD')==expected and not g('status','--porcelain=v1','--untracked-files=all')
 return dict(head=expected,tree=g('rev-parse','HEAD^{tree}'),clean=True)
with (out/'c1-static-source-before.json').open('x') as f:json.dump(identity(),f,indent=2);f.write('\n')
for name,args in [
 ('openapi-check',['node','frontend/tooling/openapi/src/cli.mjs','check']),
 ('skill-facts',['node','.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs']),
 ('fullstack-facts',['node','.agents/skills/namewta-fullstack-development/scripts/validate-skill.mjs']),
 ('notify-layered',['node','.agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs','backend/wta-modules/wta-notify','--mode','layered']),
 ('fm',['node','docs/fm/scripts/validate.mjs']),
 ('handbooks',['node','scripts/ci/verify-agent-handbooks.mjs']),
 ('diff-check',['git','diff','--check'])]:
 result=subprocess.run(['python3','/tmp/wta-check.py',str(out),'c1-'+name,str(root),*args]);assert result.returncode==0,name
 identity()
with (out/'c1-static-source-after.json').open('x') as f:json.dump(identity(),f,indent=2);f.write('\n')
print('C1 post-recovery static gates complete')
