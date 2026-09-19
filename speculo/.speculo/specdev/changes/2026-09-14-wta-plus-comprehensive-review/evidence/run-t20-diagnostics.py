#!/usr/bin/env python3
"""Read-only strict diagnostics, one package/compiler process at a time."""
from pathlib import Path
import json, subprocess, time, re, sys
root=Path(__file__).resolve().parents[6]; folder=Path(__file__).resolve().parent
name=sys.argv[1]; assert name.startswith('T-20-') and '/' not in name
areas=['packages/platform/http','packages/adapters/axios-browser','packages/domains/admin','packages/domains/system','packages/domains/profile','packages/domains/third','packages/adapters/oss-upload-browser','packages/platform/auth','apps/home-web']
if len(sys.argv) > 2: areas = sys.argv[2:]
results=[]
for area in areas:
 package=json.loads((root/'frontend'/area/'package.json').read_text())['name']
 command=['corepack','pnpm','--filter',package,'exec','vue-tsc','--noEmit','-p','tsconfig.json','--noImplicitAny','--strictFunctionTypes','--strictNullChecks']
 start=time.time(); log=folder/(name+'-'+area.replace('/','-')+'.log'); print('START',package,flush=True)
 with log.open('w') as output: result=subprocess.run(command,cwd=root/'frontend',stdout=output,stderr=subprocess.STDOUT)
 text=log.read_text(); errors=re.findall(r'^(.+?)\((\d+),(\d+)\): error (TS\d+): (.*)$',text,re.M)
 row={'package':package,'area':area,'command':command,'cwd':'frontend','exit_code':result.returncode,'seconds':round(time.time()-start,2),'diagnostic_count':len(errors),'diagnostics':[dict(file=f,line=int(l),column=int(c),code=k,message=m) for f,l,c,k,m in errors],'log':str(log.relative_to(root))};results.append(row)
 (folder/(name+'.json')).write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n');print(package,result.returncode,len(errors),flush=True)
