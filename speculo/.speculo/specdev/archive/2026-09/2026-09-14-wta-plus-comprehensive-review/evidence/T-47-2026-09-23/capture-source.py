import subprocess,json,datetime,sys,pathlib
root='/srv/WTA-plus'
def git(*a):
 r=subprocess.run(['git',*a],cwd=root,text=True,capture_output=True)
 if r.returncode: raise RuntimeError('git inspection failed')
 return r.stdout.strip()
record={'timestamp':datetime.datetime.now(datetime.timezone.utc).isoformat(),'head':git('rev-parse','HEAD'),'tree':git('rev-parse','HEAD^{tree}'),'status':git('status','--porcelain=v1','--untracked-files=all')}
record['clean']=not record['status']
path=pathlib.Path('/tmp/wta-t47')/(sys.argv[1]+'.json')
if path.exists(): raise RuntimeError('refusing evidence overwrite')
path.write_text(json.dumps(record,indent=2)+'\n')
print(json.dumps(record))
if not record['clean']: sys.exit(1)
