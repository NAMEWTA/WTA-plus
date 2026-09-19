#!/usr/bin/env python3
"""Build the real dialog in an owned harness and exercise it with installed Chrome, serially."""
from pathlib import Path
import hashlib, json, os, re, shutil, socket, subprocess, sys, tempfile, time
root=Path(__file__).resolve().parents[6]; evidence=Path(__file__).resolve().parent
name=sys.argv[1]; assert name.startswith('T-30-') and '/' not in name
def source_snapshot():
    paths = subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=root).decode().split('\0')
    prefixes = ('.agents/', '.github/', 'backend/', 'frontend/', 'scripts/', 'release-artifacts/', 'docs/', 'speculo/workflows/')
    files = {p: hashlib.sha256((root/p).read_bytes()).hexdigest() if (root/p).is_file() else None
             for p in sorted(set(paths)) if p and (p.startswith(prefixes) or '/' not in p)}
    return files, hashlib.sha256(json.dumps(files, sort_keys=True, separators=(',', ':')).encode()).hexdigest()

source_files, source_fingerprint = source_snapshot()
assert source_files == json.loads((evidence/'T-30-source-current.json').read_text())['files']
assert not (evidence/(name+'.json')).exists(), 'preserve earlier results'
assert not (evidence/(name+'-1.log')).exists(), 'preserve earlier logs'


owned=Path(tempfile.mkdtemp(prefix='t30-dialog-',dir=root/'temp/team/lead'))
with socket.socket() as sock:sock.bind(('127.0.0.1',0));port=sock.getsockname()[1]
web=root/'frontend'; package=web/'packages/web-domains/workflow'; fixture=package/'src/components/fixtures/task-integrity'
aliases={name:str(package/'node_modules'/name) for name in ['vue','element-plus','@namewta/domain-workflow']}
config=owned/'vite.config.mjs'; config.write_text('import vue from '+json.dumps(str(web/'node_modules/@vitejs/plugin-vue/dist/index.mjs'))+';\nexport default '+json.dumps(dict(root=str(web),resolve=dict(alias=aliases),build=dict(outDir=str(owned/'dist'),emptyOutDir=True,rollupOptions=dict(input=str(fixture/'index.html')))),ensure_ascii=False).replace('"build":','"plugins": [vue()], "build":')+';\n')
pw=owned/'playwright.config.mjs';pw.write_text('export default '+json.dumps(dict(testDir=str(web/'e2e'),testMatch='workflow-task-integrity.spec.ts',outputDir=str(evidence/(name+'-artifacts')),workers=1,retries=0,maxFailures=1,reporter='line',use=dict(baseURL=f'http://127.0.0.1:{port}',channel='chrome'),webServer=dict(command=f'corepack pnpm exec vite preview --config {config} --host 127.0.0.1 --port {port} --strictPort',url=f'http://127.0.0.1:{port}/packages/web-domains/workflow/src/components/fixtures/task-integrity/index.html',reuseExistingServer=False,cwd=str(web))),ensure_ascii=False)+';\n')
results=[]; env=os.environ.copy();env.update(npm_config_workspace_concurrency='1',RAYON_NUM_THREADS='1')
try:
 for i,cmd in enumerate([['corepack','pnpm','exec','vite','build','--config',str(config)],['corepack','pnpm','exec','playwright','test','--config',str(pw),'--workers=1']]):
  log=evidence/f'{name}-{i+1}.log';start=time.time();print('START',' '.join(cmd),flush=True)
  with log.open('w') as output:r=subprocess.run(cmd,cwd=web,env=env,stdout=output,stderr=subprocess.STDOUT)
  results.append(dict(command=cmd,cwd='frontend',exit_code=r.returncode,seconds=round(time.time()-start,2),log=str(log.relative_to(root))))
  print(json.dumps(results[-1]),flush=True)
  if r.returncode: print(log.read_text()[-7000:],flush=True);break
finally:
 record=dict(source_fingerprint=source_fingerprint,source_unchanged=source_snapshot()[0]==source_files,results=results,vite_config=config.read_text(),playwright_config=pw.read_text());shutil.rmtree(owned);record['owned_cleanup']=not owned.exists()
 with socket.socket() as sock:record['preview_closed']=sock.connect_ex(('127.0.0.1',port))!=0
 record['counts']={key:sum(map(int,re.findall(r'(\d+) '+key, (evidence/f'{name}-2.log').read_text()))) for key in ('passed','failed','skipped')}
 (evidence/f'{name}.json').write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n')
sys.exit(0 if len(results)==2 and all(r['exit_code']==0 for r in results) and record['preview_closed'] and record['source_unchanged'] and record['counts']['passed'] > 0 and record['counts']['failed']==0 and record['counts']['skipped']==0 else 1)
