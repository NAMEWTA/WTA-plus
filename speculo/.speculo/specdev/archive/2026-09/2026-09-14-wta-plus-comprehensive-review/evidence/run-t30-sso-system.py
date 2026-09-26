#!/usr/bin/env python3
"""Serial HTTP/browser gates on owned loopback fixtures, with source and fresh report guards."""
import datetime
import hashlib
import json
import os
import pathlib
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
root = pathlib.Path(__file__).resolve().parents[6]
evidence = pathlib.Path(__file__).resolve().parent
import re, secrets, shutil, signal, socket, tempfile, urllib.request, uuid
name=sys.argv[1]
assert name.startswith("T-30-") and "/" not in name
def source_snapshot():
    paths = subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=root).decode().split('\0')
    prefixes = ('.agents/', '.github/', 'backend/', 'frontend/', 'scripts/', 'release-artifacts/', 'docs/', 'speculo/workflows/')
    files = {p: hashlib.sha256((root/p).read_bytes()).hexdigest() if (root/p).is_file() else None
             for p in sorted(set(paths)) if p and (p.startswith(prefixes) or '/' not in p)}
    return files, hashlib.sha256(json.dumps(files, sort_keys=True, separators=(',', ':')).encode()).hexdigest()

source_files, source_fingerprint = source_snapshot()
assert source_files == json.loads((evidence/'T-30-source-current.json').read_text())['files']
assert not (evidence/(name+'.json')).exists(), 'preserve earlier results'
assert not (evidence/(name+'.log')).exists(), 'preserve earlier logs'

def inventory():
    return {kind: sorted(subprocess.check_output(command, text=True).splitlines()) for kind, command in {
        'containers': ['docker', 'ps', '-aq', '--no-trunc'], 'networks': ['docker', 'network', 'ls', '-q', '--no-trunc'],
        'volumes': ['docker', 'volume', 'ls', '-q']}.items()}
record = dict(captured_at=datetime.datetime.now(datetime.timezone.utc).isoformat(), before=inventory(),
              source_fingerprint=source_fingerprint, cwd='backend')
(evidence/(name+'-source.json')).write_text(json.dumps(dict(files=source_files, source_fingerprint=source_fingerprint), indent=2)+'\n')

owned=pathlib.Path(tempfile.mkdtemp(prefix='t30-sso-system-',dir=root/'temp/team/lead'))
containers=[];backend=None;backend_log=None;code=1;started=time.time();suffix=uuid.uuid4().hex[:12]
ports=[18080,4174,4175,4176]
for value in ports:
    with socket.socket() as sock:
        sock.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1);sock.bind(('127.0.0.1',value))
def docker(*args):return subprocess.check_output(['docker',*args],text=True,stderr=subprocess.STDOUT).strip()
def wait_url(url):
    for _ in range(150):
        if backend is not None and backend.poll() is not None:raise RuntimeError('Owned backend exited before readiness')
        try:
            with urllib.request.urlopen(url,timeout=2) as response:
                if response.status==200:return
        except Exception:pass
        time.sleep(1)
    raise RuntimeError('Owned HTTP readiness timeout')
def hash_tree(directory):return {str(p.relative_to(directory)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(directory.rglob('*')) if p.is_file()}
try:
    jar=root/'backend/wta-admin/target/wta-admin.jar';jar_hash=hashlib.sha256(jar.read_bytes()).hexdigest()
    bundles=json.loads((evidence/'T-30-v2-backend-bundles.json').read_text());assert jar_hash==bundles[-1]['artifact']['sha256']
    record.update(jar=dict(path=str(jar.relative_to(root)),sha256=jar_hash,bundle='core',build_source_fingerprint=bundles[-1]['source_fingerprint']),dist={app:hash_tree(root/f'frontend/apps/{app}-web/dist') for app in ('admin','home','sso')})
    prior=json.loads((evidence/'T-30-source-pre-fixture-fix.json').read_text())['files']
    changed=[path for path in set(source_files)|set(prior) if source_files.get(path)!=prior.get(path)]
    assert all(path in json.loads((evidence/'T-30-fixture-originals.json').read_text()) and '/src/test/' in path for path in changed)
    record['test_only_changes_since_pre_fixture_source']=changed
    password=secrets.token_hex(20)
    mysql=docker('run','-d','--name','t30-sso-mysql-'+suffix,'--label','namewta.test.owner=T-30','-p','127.0.0.1::3306','-e','MYSQL_ROOT_PASSWORD='+password,'-e','MYSQL_DATABASE=wta-plus','mysql:8.4.9','--character-set-server=utf8mb4','--collation-server=utf8mb4_general_ci');containers.append(mysql)
    for _ in range(90):
        if subprocess.run(['docker','exec',mysql,'mysqladmin','ping','-h','127.0.0.1','-uroot','-p'+password,'--silent'],capture_output=True).returncode==0:break
        time.sleep(1)
    else:raise RuntimeError('Owned MySQL timeout')
    mysql_port=docker('port',mysql,'3306/tcp').rsplit(':',1)[1]
    record['sql_baselines']=[]
    sql_root=root/'release-artifacts/docker/infrastructure/mysql/init'
    for filename in ['10-cde-base-ddl.sql','20-cde-job.sql','30-cde-workflow.sql','40-cde-ai.sql','50-cde-base-dml.sql']:
        source=sql_root/filename
        result=subprocess.run(['docker','exec','-i',mysql,'mysql','--default-character-set=utf8mb4','-uroot','-p'+password,'wta-plus'],input=source.read_bytes(),capture_output=True)
        record['sql_baselines'].append(dict(file=str(source.relative_to(root)),sha256=hashlib.sha256(source.read_bytes()).hexdigest(),exit_code=result.returncode))
        if result.returncode:raise RuntimeError('Baseline failed: '+filename+' '+result.stderr.decode().replace(password,'[REDACTED]'))
    print('OWNED MYSQL BASELINE READY',flush=True)
    redis=docker('run','-d','--name','t30-sso-redis-'+suffix,'--label','namewta.test.owner=T-30','-p','127.0.0.1::6379','redis:8.6.3','redis-server','--save','','--appendonly','no','--requirepass',password);containers.append(redis)
    redis_port=docker('port',redis,'6379/tcp').rsplit(':',1)[1]
    (owned/'uploads').mkdir()
    env={**os.environ,'SPRING_PROFILES_ACTIVE':'dev','SERVER_ADDRESS':'127.0.0.1','SERVER_PORT':'18080',
      'SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL':'jdbc:mysql://127.0.0.1:'+mysql_port+'/wta-plus?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai',
      'SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME':'root','SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD':password,
      'SPRING_DATA_REDIS_HOST':'127.0.0.1','SPRING_DATA_REDIS_PORT':redis_port,'SPRING_DATA_REDIS_PASSWORD':password,
      'SPRING_BOOT_ADMIN_CLIENT_ENABLED':'false','SPRING_BOOT_ADMIN_CLIENT_USERNAME':'owned-monitor','SPRING_BOOT_ADMIN_CLIENT_PASSWORD':secrets.token_hex(16),
      'SSO_COOKIE_SECURE':'false','SSO_WEB_ORIGIN':'http://127.0.0.1:4176','WEB_CORS_ALLOWED_ORIGINS':'http://127.0.0.1:4174,http://127.0.0.1:4175,http://127.0.0.1:4176',
      'CAPTCHA_ENABLE':'false','NACOS_CONFIG_ENABLED':'false','SNAIL_JOB_ENABLED':'false','SNAIL_AI_ENABLED':'false',
      'MYBATIS_PLUS_SQL_LOG_ENABLED':'false','SPRING_SERVLET_MULTIPART_LOCATION':str(owned/'uploads')}
    command=['java','-Xms128m','-Xmx768m','-jar',str(jar)]
    backend_log=(owned/'backend.log').open('w')
    backend=subprocess.Popen(command,cwd=owned,env=env,stdout=backend_log,stderr=subprocess.STDOUT,start_new_session=True)
    record['backend']=dict(command=command,profile='dev',http_exception='Explicit local/dev SSO Secure=false, separate production HTTPS evidence already passed')
    wait_url('http://127.0.0.1:18080/auth/code');print('FULL SYSTEM BACKEND READY',flush=True)
    servers=[]
    for app,port in zip(('admin','home','sso'),(4174,4175,4176)):
        extra='location ^~ /sso/ { proxy_pass http://127.0.0.1:18080; proxy_set_header Host $http_host; }' if app=='sso' else ''
        servers.append('server { listen 127.0.0.1:'+str(port)+'; root /usr/share/nginx/html/'+app+'; access_log off; '+extra+' location /prod-api/ { rewrite ^/prod-api/(.*)$ /$1 break; proxy_pass http://127.0.0.1:18080; proxy_set_header Host $http_host; proxy_buffering off; } location / { try_files $uri $uri/ /index.html; } }')
    (owned/'nginx.conf').write_text('\n'.join(servers)+'\n')
    args=['run','-d','--name','t30-sso-nginx-'+suffix,'--label','namewta.test.owner=T-30','--network','host','--mount','type=bind,src='+str(owned/'nginx.conf')+',dst=/etc/nginx/conf.d/default.conf,readonly']
    for app in ('admin','home','sso'):args+=['--mount','type=bind,src='+str(root/f'frontend/apps/{app}-web/dist')+',dst=/usr/share/nginx/html/'+app+',readonly']
    nginx=docker(*args,'nginx:1.31.1');containers.append(nginx)
    for port in (4174,4175,4176):wait_url('http://127.0.0.1:'+str(port)+'/')
    # Retain original tests and assertions; isolate their fixed screenshot output in an owned cwd.
    config=owned/'playwright.config.mjs'
    config.write_text('export default '+json.dumps(dict(testDir=str(root/'frontend/e2e'),testMatch=['sso-three-gates.spec.ts','sso-admin-config.spec.ts'],outputDir=str(evidence/(name+'-artifacts')),workers=1,retries=0,reporter='line',use=dict(channel='chrome',trace='off')))+';\n')
    (owned/'run').mkdir()
    command=['node',str(root/'frontend/node_modules/@playwright/test/cli.js'),'test','--config',str(config),'--workers=1']
    record['command']=command
    with (evidence/(name+'.log')).open('w') as out:result=subprocess.run(command,cwd=owned/'run',env={**os.environ,'ADMIN_WEB_URL':'http://127.0.0.1:4174','HOME_WEB_URL':'http://127.0.0.1:4175'},stdout=out,stderr=subprocess.STDOUT)
    record['counts']={key:sum(map(int,re.findall(r'(\d+) '+key,(evidence/(name+'.log')).read_text()))) for key in ('passed','failed','skipped')}
    code=result.returncode
    if record['counts']!={'passed':2,'failed':0,'skipped':0}:code=1
except Exception as failure:
    record['error']=str(failure);print('FAILED:',record['error'],flush=True)
finally:
    if backend is not None and backend.poll() is None:
        os.killpg(backend.pid,signal.SIGTERM)
        try:backend.wait(timeout=20)
        except subprocess.TimeoutExpired:os.killpg(backend.pid,signal.SIGKILL);backend.wait()
    if backend_log:backend_log.close()
    if (owned/'backend.log').exists():
        text=(owned/'backend.log').read_text(errors='replace')
        for value in (env.get(k) for k in env if 'PASSWORD' in k):
            if value:text=text.replace(value,'[REDACTED]')
        text=re.sub(r'([?&](?:code|state|token|code_verifier|code_challenge|access_token)=)[^&\s"\']+',r'\1[REDACTED]',text)
        (evidence/(name+'-backend.log')).write_text(text)
    for container in reversed(containers):docker('rm','-fv',container)
    shutil.rmtree(owned)
    record.update(after=inventory(),source_unchanged=source_snapshot()[0]==source_files,seconds=round(time.time()-started,2))
    record['resources_restored']=record['before']==record['after'];record['ports_closed']={port:False for port in ports}
    for port in ports:
        with socket.socket() as sock:record['ports_closed'][port]=sock.connect_ex(('127.0.0.1',port))!=0
    if not record['resources_restored'] or not record['source_unchanged'] or not all(record['ports_closed'].values()):code=1
    record['exit_code']=code
    (evidence/(name+'.json')).write_text(json.dumps(record,indent=2)+'\n')
print(json.dumps({k:record.get(k) for k in ('exit_code','counts','resources_restored','source_unchanged','seconds','error')}),flush=True)
raise SystemExit(code)
