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
import base64, re, secrets, shutil, tempfile, urllib.request, urllib.parse, uuid
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

owned=pathlib.Path(tempfile.mkdtemp(prefix='t30-nacos-',dir=root/'temp/team/lead'))
uid=uuid.uuid4().hex[:12];network='t30-nacos-'+uid;containers=[];created_network=False
record.update(boundary='Official Nacos 2.5.4 with fresh embedded Derby and enabled authentication, actual repository LB Nginx template. No WTA identity/API stub is used by the selected console-only browser case; unused LB upstream aliases do not serve business APIs.')
code=1;started=time.time()
def docker(*args):
    return subprocess.check_output(['docker',*args],text=True,stderr=subprocess.STDOUT).strip()
def port(container,inside):return int(docker('port',container,str(inside)+'/tcp').rsplit(':',1)[1])
def ready(url):
    for _ in range(150):
        try:
            with urllib.request.urlopen(url,timeout=2) as response:
                if response.status==200:return
        except Exception:pass
        time.sleep(1)
    raise RuntimeError('Owned service readiness failed: '+url)
try:
    for img in ('nacos/nacos-server:v2.5.4','nginx:1.31.1'):
        if subprocess.run(['docker','image','inspect',img],capture_output=True).returncode:
            print('PULL',img,flush=True)
            with (evidence/(name+'-pull.log')).open('a') as out:subprocess.run(['docker','pull',img],stdout=out,stderr=subprocess.STDOUT,check=True)
    record['images']={img:json.loads(docker('image','inspect',img))[0]['RepoDigests'] for img in ('nacos/nacos-server:v2.5.4','nginx:1.31.1')}
    docker('network','create','--label','namewta.test.owner=T-30',network);created_network=True
    token=base64.b64encode(secrets.token_bytes(48)).decode()
    aliases=['nacos','namewta-nginx-admin-web','namewta-nginx-home-web','namewta-monitor-admin','namewta-snailjob-server','namewta-snailai-server']
    args=['run','-d','--name',network+'-server','--label','namewta.test.owner=T-30','--network',network,'-p','127.0.0.1::8848']
    for alias in aliases:args+=['--network-alias',alias]
    for key,val in dict(MODE='standalone',NACOS_AUTH_ENABLE='true',NACOS_AUTH_TOKEN=token,NACOS_AUTH_IDENTITY_KEY='t30identity',NACOS_AUTH_IDENTITY_VALUE=secrets.token_hex(20),NACOS_AUTH_USER_AGENT_AUTH_WHITE_ENABLE='false',JVM_XMS='256m',JVM_XMX='512m',JVM_XMN='128m').items():args+=['-e',key+'='+val]
    nacos=docker(*args,'nacos/nacos-server:v2.5.4');containers.append(nacos)
    origin='http://127.0.0.1:'+str(port(nacos,8848));ready(origin+'/nacos/v1/console/health/readiness')
    print('NACOS READY',flush=True)
    data=urllib.parse.urlencode({'password':'T30'+secrets.token_hex(16)+'a1'}).encode()
    with urllib.request.urlopen(urllib.request.Request(origin+'/nacos/v1/auth/users/admin',data=data),timeout=10) as response:assert response.status==200
    template=root/'release-artifacts/docker/frontend/nginx/lb/nginx-lb-http.conf.template'
    conf=template.read_text()
    for key,val in dict(LB_SERVER_NAME='_',APP_ADMIN_WEB_PREFIX='admin-app',APP_HOME_WEB_PREFIX='home-app').items():conf=conf.replace('${'+key+'}',val)
    (owned/'default.conf').write_text(conf)
    gateway=docker('run','-d','--name',network+'-nginx','--label','namewta.test.owner=T-30','--network',network,'-p','127.0.0.1::80','--mount','type=bind,src='+str(owned/'default.conf')+',dst=/etc/nginx/conf.d/default.conf,readonly','nginx:1.31.1');containers.append(gateway)
    live='http://127.0.0.1:'+str(port(gateway,80));ready(live+'/nacos/')
    config=owned/'playwright.config.mjs'
    config.write_text('export default '+json.dumps(dict(testDir=str(root/'frontend/e2e'),testMatch='nacos-console.spec.ts',outputDir=str(evidence/(name+'-artifacts')),workers=1,retries=0,reporter='line',use=dict(channel='chrome',trace='off')))+';\n')
    command=['corepack','pnpm','exec','playwright','test','--config',str(config),'--grep','live proxy','--workers=1']
    record.update(command=command,cwd='frontend',nginx_template_sha256=hashlib.sha256(template.read_bytes()).hexdigest())
    with (evidence/(name+'.log')).open('w') as out:r=subprocess.run(command,cwd=root/'frontend',env={**os.environ,'NACOS_LIVE_BASE_URL':live},stdout=out,stderr=subprocess.STDOUT)
    record['counts']={key:sum(map(int,re.findall(r'(\d+) '+key,(evidence/(name+'.log')).read_text()))) for key in ('passed','failed','skipped')}
    code=r.returncode
    if record['counts'] != {'passed':1,'failed':0,'skipped':0}:code=1
except Exception as failure:
    record['error']=str(failure);print('FAIL',type(failure).__name__,flush=True)
finally:
    for container in reversed(containers):docker('rm','-fv',container)
    if created_network:docker('network','rm',network)
    shutil.rmtree(owned)
    record.update(after=inventory(),source_unchanged=source_snapshot()[0]==source_files,seconds=round(time.time()-started,2))
    record['resources_restored']=record['before']==record['after']
    if not record['resources_restored'] or not record['source_unchanged']:code=1
    record['exit_code']=code
    (evidence/(name+'.json')).write_text(json.dumps(record,indent=2)+'\n')
print(json.dumps({k:record.get(k) for k in ('exit_code','counts','resources_restored','source_unchanged','seconds','error')}),flush=True)
raise SystemExit(code)
