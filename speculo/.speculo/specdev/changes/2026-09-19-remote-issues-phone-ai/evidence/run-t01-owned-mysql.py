from pathlib import Path
import os, re, subprocess, time, uuid
ROOT = Path(__file__).resolve().parents[6]
OWNER = uuid.uuid4().hex
PASSWORD = uuid.uuid4().hex
SCHEMA = 'namewta_phone_test_' + OWNER[:12]
CID = None
REDIS = None

def run(args, **kwargs):
    return subprocess.run(args, text=True, check=True, **kwargs)

def sql(statement):
    return run(['docker','exec','-i','-e','MYSQL_PWD='+PASSWORD,CID,'mysql','-uroot','-N'],input=statement,stdout=subprocess.PIPE).stdout.strip()

try:
    CID = run(['docker','run','-d','--label','namewta.phone.owner='+OWNER,'-e','MYSQL_ROOT_PASSWORD='+PASSWORD,'-p','127.0.0.1::3306','mysql:8.4'], stdout=subprocess.PIPE).stdout.strip()
    for _ in range(90):
        probe=subprocess.run(['docker','exec','-e','MYSQL_PWD='+PASSWORD,CID,'mysql','-uroot','-N','-e','SELECT 1'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        if probe.returncode == 0: break
        time.sleep(1)
    else: raise RuntimeError('owned MySQL did not start')
    port=run(['docker','port',CID,'3306/tcp'],stdout=subprocess.PIPE).stdout.strip().rsplit(':',1)[1]
    ddl=(ROOT/'release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql').read_text()
    tables=[]
    for name in ['sys_user','sys_user_type','sys_user_type_rel']:
        match=re.search(r'create table '+name+r'\s*\(.*?\) engine=innodb[^;]*;',ddl,re.I|re.S)
        if not match: raise RuntimeError('missing DDL '+name)
        tables.append(match.group())
    sql('CREATE DATABASE '+SCHEMA+'; USE '+SCHEMA+';\n'+'\n'.join(tables)+"\nCREATE TABLE owned_phone_fixture(id int primary key, owner_token varchar(32) not null); INSERT INTO owned_phone_fixture VALUES(1,'"+OWNER+"');")
    REDIS = run(['docker','run','-d','--label','namewta.phone.owner='+OWNER,'-p','127.0.0.1::6379','redis:7.4-alpine'],stdout=subprocess.PIPE).stdout.strip()
    redis_port=run(['docker','port',REDIS,'6379/tcp'],stdout=subprocess.PIPE).stdout.strip().rsplit(':',1)[1]
    command=['./mvnw','-pl','wta-admin','-am','test','-Dtest=PhoneRegistrationMySqlIntegrationTest,PhoneTestDatabaseGuardTest,SysRegisterServiceRegistrationUnitTest,PasswordAuthStrategyTemporaryUnitTest','-Dsurefire.failIfNoSpecifiedTests=false','-Dphone.mysql.integration=true','-Dphone.redis.integration.port='+redis_port,'-Dphone.mysql.integration.url=jdbc:mysql://127.0.0.1:'+port+'/'+SCHEMA+'?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai','-Dphone.mysql.integration.password='+PASSWORD,'-Dphone.mysql.integration.owner='+OWNER]
    with open('/tmp/wta-phone-owned-mysql-green.log','w') as log:
        result=subprocess.run(command,cwd=ROOT/'backend',stdout=log,stderr=subprocess.STDOUT)
    if result.returncode: raise RuntimeError('positive Maven gate failed; inspect log')
    sql('USE '+SCHEMA+"; INSERT INTO sys_user(user_id,user_name,nick_name) VALUES(123456,'guard-sentinel','guard-sentinel');")
    wrong=command.copy();wrong[wrong.index('-Dtest=PhoneRegistrationMySqlIntegrationTest,PhoneTestDatabaseGuardTest,SysRegisterServiceRegistrationUnitTest,PasswordAuthStrategyTemporaryUnitTest')]='-Dtest=PhoneRegistrationMySqlIntegrationTest';wrong[-1]='-Dphone.mysql.integration.owner='+uuid.uuid4().hex
    with open('/tmp/wta-phone-owned-mysql-owner-negative.log','w') as log:
        result=subprocess.run(wrong,cwd=ROOT/'backend',stdout=log,stderr=subprocess.STDOUT)
    if result.returncode == 0: raise RuntimeError('wrong owner unexpectedly passed')
    if sql('USE '+SCHEMA+'; SELECT count(*) FROM sys_user WHERE user_id=123456;') != '1': raise RuntimeError('ownership failure changed sentinel')
    print('positive 28 tests passed; wrong owner rejected before cleanup; sentinel preserved', flush=True)
finally:
    if REDIS:
        assert run(['docker','inspect','--format','{{ index .Config.Labels \"namewta.phone.owner\" }}',REDIS],stdout=subprocess.PIPE).stdout.strip() == OWNER
        run(['docker','rm','-f',REDIS],stdout=subprocess.DEVNULL)
    if CID:
        owner=run(['docker','inspect','--format','{{ index .Config.Labels "namewta.phone.owner" }}',CID],stdout=subprocess.PIPE).stdout.strip()
        if owner != OWNER: raise RuntimeError('refusing to remove non-owned container')
        run(['docker','rm','-f',CID],stdout=subprocess.DEVNULL)
        print('owned container removed',flush=True)
