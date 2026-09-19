"""Lead-only smoke test of the two retained release JARs, with a new owned DB."""
from pathlib import Path
import base64, hashlib, json, os, secrets, shutil, signal, socket, subprocess, sys, tempfile, time, urllib.request
root=Path('/srv/WTA-plus');release=Path(sys.argv[1]).resolve();output=Path(sys.argv[2]);output.mkdir(exist_ok=False)
owner='retained-'+secrets.token_hex(12);password=secrets.token_hex(20);cid=None;processes=[];scratch=Path(tempfile.mkdtemp(prefix='wta-retained-'))
env={'PATH':os.environ['PATH'],'LANG':'C.UTF-8','TZ':'Asia/Shanghai'}
result={'release_id':release.name,'owner':owner,'services':{}}
def run(args,**kwargs):return subprocess.run([str(a) for a in args],check=True,text=True,capture_output=True,timeout=60,**kwargs).stdout.strip()
def port():
 with socket.socket() as s:s.bind(('127.0.0.1',0));return s.getsockname()[1]
def stop():
 for proc in processes:
  if proc.poll() is None:
   proc.terminate()
   try:proc.wait(timeout=20)
   except subprocess.TimeoutExpired:proc.kill();proc.wait(timeout=10)
try:
 cid=run(['docker','create','--label','namewta.test.owner='+owner,'-p','127.0.0.1::3306','-e','MYSQL_ROOT_PASSWORD='+password,'mysql:8.4.9'])
 (output/'owned-resources.json').write_text(json.dumps({'owner':owner,'ids':[cid]}));run(['docker','start',cid]);mysqlport=int(run(['docker','port',cid,'3306/tcp']).rsplit(':',1)[1])
 for _ in range(100):
  try:
   run(['docker','exec',cid,'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --protocol=TCP -h127.0.0.1 -uroot -e "SELECT 1"']);break
  except subprocess.CalledProcessError:time.sleep(1)
 else:raise RuntimeError('owned MySQL readiness failed')
 f=scratch/'init.env';f.write_text(f'MYSQL_DATABASE=wta-plus\nMYSQL_APP_USER=ownedapp\nMYSQL_APP_PASSWORD={password}\nMINIO_ROOT_USER=owned\nMINIO_ROOT_PASSWORD={password}\nMINIO_ENDPOINT=127.0.0.1:9\nMINIO_BUCKET=wta\n');f.chmod(0o600)
 run(['bash',release/'scripts/init-mysql-container.sh','--container',cid,'--env-file',f,'--sql-dir',release/'docker/infrastructure/mysql/init'])
 for name,health,marker in [('wta-monitor-admin','/actuator/health','Started MonitorAdminApplication'),('wta-snailjob-server','/snail-job/actuator/health','Started SnailJobServerApplication')]:
  httpport=port();rpcport=port();jar=release/'docker/backend/images'/name/'app.jar';assert jar.is_file()
  cfg={'server.address':'127.0.0.1','server.port':httpport,'spring.boot.admin.client.enabled':False,'spring.boot.admin.client.username':'owned','spring.boot.admin.client.password':password,'spring.security.user.name':'owned','spring.security.user.password':password,'spring.datasource.url':f'jdbc:mysql://127.0.0.1:{mysqlport}/wta-plus?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai','spring.datasource.username':'ownedapp','spring.datasource.password':password,'snail-job.server-host':'127.0.0.1','snail-job.server-port':rpcport}
  f=scratch/(name+'.yml');f.write_text(json.dumps(cfg));f.chmod(0o600)
  with (output/(name+'.log')).open('w') as log:
   proc=subprocess.Popen(['java','-Xms128m','-Xmx512m','-jar',str(jar),'--spring.profiles.active=prod','--spring.config.additional-location=file:'+str(f)],cwd=scratch,env=env,stdout=log,stderr=subprocess.STDOUT)
  processes.append(proc)
  for _ in range(120):
   assert proc.poll() is None,name+' exited'
   try:
    req=urllib.request.Request(f'http://127.0.0.1:{httpport}'+health,headers={'Authorization':'Basic '+base64.b64encode(('owned:'+password).encode()).decode()})
    with urllib.request.urlopen(req,timeout=3) as response:body=json.load(response)
    if body['status']=='UP' and marker in (output/(name+'.log')).read_text():break
   except (OSError,ValueError):pass
   time.sleep(1)
  else:raise RuntimeError(name+' readiness failed')
  result['services'][name]={'health':'UP','jar_sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'started_marker':marker}
  proc.terminate();proc.wait(timeout=30)
finally:
 signal.signal(signal.SIGTERM,signal.SIG_IGN);signal.signal(signal.SIGINT,signal.SIG_IGN)
 stop()
 if cid:
  assert run(['docker','inspect','--format','{{ index .Config.Labels "namewta.test.owner" }}',cid])==owner
  run(['docker','rm','-fv',cid]);result['container_removed']=cid
 shutil.rmtree(scratch)
 (output/'attempt.json').write_text(json.dumps(result,indent=2)+'\n')
assert len(result['services'])==2
(output/'acceptance.json').write_text(json.dumps(result,indent=2)+'\n');print('Retained Monitor and SnailJob release JARs: startup and authenticated health UP')
