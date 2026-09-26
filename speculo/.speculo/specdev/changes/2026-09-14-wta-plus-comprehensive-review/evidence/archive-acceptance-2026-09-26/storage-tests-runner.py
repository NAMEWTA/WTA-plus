import pathlib,subprocess,time,json,uuid,urllib.request,os,xml.etree.ElementTree as ET
root=pathlib.Path('/srv/WTA-plus'); out=pathlib.Path('/tmp/wta-archive-services-'+uuid.uuid4().hex[:8]);out.mkdir();os.chmod(out,0o700)
owned=[];record={};code=1

def run(*args): return subprocess.check_output(args,text=True,stderr=subprocess.STDOUT).strip()
def start(image,port,*args):
 cid=run('docker','run','-d','--pull=never','--label','namewta.test.owner=adversarial-review-20260926','-p','127.0.0.1::'+str(port),*args,image,*({'mysql:8.4.9':['--character-set-server=utf8mb4','--collation-server=utf8mb4_general_ci'],'quay.io/minio/minio:RELEASE.2025-04-22T22-12-26Z':['server','/data']}.get(image,[])))
 owned.append(cid); return cid,run('docker','port',cid,str(port)+'/tcp').split(':')[-1]
def identity():
 status=run('git','-C',str(root),'status','--porcelain=v1');assert not status,status
 return {'head':run('git','-C',str(root),'rev-parse','HEAD'),'tree':run('git','-C',str(root),'rev-parse','HEAD^{tree}'),'clean':True}
try:
 record['source_before']=identity()
 mysql,mp=start('mysql:8.4.9',3306,'-e','MYSQL_ROOT_PASSWORD=owned-review-only','-e','MYSQL_DATABASE=owned_review')
 minio,op=start('quay.io/minio/minio:RELEASE.2025-04-22T22-12-26Z',9000,'-e','MINIO_ROOT_USER=namewta','-e','MINIO_ROOT_PASSWORD=namewta123')
 for i in range(90):
  try:run('docker','exec',mysql,'mysql','-uroot','-powned-review-only','-e','SELECT 1');break
  except subprocess.CalledProcessError:time.sleep(1)
 else:raise RuntimeError('owned mysql unavailable')
 endpoint='http://127.0.0.1:'+op
 for i in range(60):
  try:urllib.request.urlopen(endpoint+'/minio/health/ready',timeout=1).close();break
  except OSError:time.sleep(1)
 else:raise RuntimeError('owned minio unavailable')
 props=out/'test.properties'; props.write_text('\n'.join([f'oss.{kind}.mysql.integration.{key}={value}' for kind in ['lifecycle','migration'] for key,value in [('url',f'jdbc:mysql://127.0.0.1:{mp}/owned_review?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'),('username','root'),('password','owned-review-only')]])+'\noss.minio.integration.endpoint='+endpoint+'\nnamewta.repo.root='+str(root)+'\n');os.chmod(props,0o600)
 selectors='OssCleanupMySqlIntegrationTest,OssStorageMigrationIntegrationTest,MinioOssClientIntegrationTest,OssUploadPropertiesUnitTest,OssUploadStorageRoutingMinioIntegrationTest,AttachmentFreeSubmitDoesNotTouchOssTest'
 cmd=['./mvnw','-B','-ntp','-pl','wta-admin','-am','-Dtest='+selectors,'-Dsurefire.failIfNoSpecifiedTests=false','-Dsurefire.systemPropertiesFile='+str(props),'test']
 began=time.time(); record.update(command=cmd,cwd='backend',containers=owned)
 with (out/'maven.log').open('w') as log:code=subprocess.call(cmd,cwd=root/'backend',stdout=log,stderr=subprocess.STDOUT)
 counts={k:0 for k in ['tests','failures','errors','skipped']}; suites=[]
 for p in (root/'backend/wta-admin/target/surefire-reports').glob('TEST-*.xml'):
  if p.stat().st_mtime<began:continue
  x=ET.parse(p).getroot(); c={k:int(x.get(k,0)) for k in counts};suites.append({'name':x.get('name'),**c});
  for k,v in c.items():counts[k]+=v
 record.update(counts=counts,suites=suites)
 record['source_after']=identity();assert record['source_after']==record['source_before']
 if counts['tests']==0 or any(counts[k] for k in ['failures','errors','skipped']):code=1
finally:
 cleanup=[]
 for cid in owned:cleanup.append(subprocess.call(['docker','rm','-fv',cid],stdout=subprocess.DEVNULL))
 if any(cleanup):code=1
 record.update(exit_code=code,cleanup=cleanup);(out/'summary.json').write_text(json.dumps(record,indent=2)+'\n');print(out);print(json.dumps(record.get('counts',{})));print('exit',code)
raise SystemExit(code)
