import subprocess,sys,json,pathlib,datetime
folder,name,cwd,*cmd=sys.argv[1:];p=pathlib.Path(folder);p.mkdir(exist_ok=True)
log=p/(name+'.log');record=p/(name+'.json')
if log.exists() or record.exists(): raise RuntimeError('refusing to overwrite prior evidence')
start=datetime.datetime.now(datetime.timezone.utc).isoformat()
with log.open('w') as out:r=subprocess.run(cmd,cwd=cwd,stdout=out,stderr=subprocess.STDOUT,text=True)
record.write_text(json.dumps({'command':cmd,'cwd':cwd,'started_at':start,'finished_at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'exit_code':r.returncode,'log':log.name},indent=2)+'\n')
print(name,'exit',r.returncode);print(log.read_text()[-1200:]);sys.exit(r.returncode)
