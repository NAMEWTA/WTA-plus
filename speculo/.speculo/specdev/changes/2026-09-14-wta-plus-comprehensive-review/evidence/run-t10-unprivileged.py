from pathlib import Path
import subprocess,shutil,tempfile,os,json,time,sys
root=Path(__file__).resolve().parents[6]
evidence=Path(__file__).parent
name=sys.argv[1]
with tempfile.TemporaryDirectory(prefix='wta-t10-tools-') as temporary:
 tools=Path(temporary);tools.chmod(0o755)
 shutil.copy2(shutil.which('node'),tools/'node');(tools/'node').chmod(0o755)
 jdk=Path(shutil.which('jar')).resolve().parents[1];shutil.copytree(jdk,tools/'jdk',symlinks=True)
 command=['runuser','-u','nobody','--',str(tools/'node'),'--test','--test-concurrency=1','release-artifacts/tests/atomic-release.test.mjs']
 started=time.monotonic()
 with (evidence/(name+'.log')).open('w') as log:
  result=subprocess.run(command,cwd=root,env={**os.environ,'PATH':temporary+':'+temporary+'/jdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin'},stdout=log,stderr=subprocess.STDOUT)
 record={'command':'runuser -u nobody -- <owned-copy-of-node-24.21.0> --test --test-concurrency=1 release-artifacts/tests/atomic-release.test.mjs','exitCode':result.returncode,'seconds':round(time.monotonic()-started,2),'toolSetup':'copy existing Node and JDK 21.0.12.1 to owned temporary executable directory; no permission changes to /root and no global Git safe.directory; removed afterward','log':name+'.log'}
 (evidence/(name+'.json')).write_text(json.dumps(record,indent=2)+'\n')
 print(json.dumps(record),flush=True)
 sys.exit(result.returncode)
