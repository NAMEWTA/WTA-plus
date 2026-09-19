from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import os, signal, ssl, subprocess, tempfile, threading
root=Path(__file__).resolve().parents[6]
servers=[]; threads=[]; process=None
class Handler(SimpleHTTPRequestHandler):
 def do_GET(self):
  if not Path(self.translate_path(self.path)).is_file(): self.path='/index.html'
  super().do_GET()
 def log_message(self,*args): pass
with tempfile.TemporaryDirectory(prefix='phone-ai-T01-https-') as scratch:
 scratch=Path(scratch)
 try:
  subprocess.run(['openssl','req','-x509','-newkey','rsa:2048','-nodes','-days','1','-subj','/CN=127.0.0.1','-addext','subjectAltName=IP:127.0.0.1','-keyout',str(scratch/'key.pem'),'-out',str(scratch/'cert.pem')],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
  environment=os.environ.copy()
  for app in ('admin','home'):
   dist=root/'frontend/apps'/f'{app}-web/dist'
   assert (dist/'index.html').is_file()
   server=ThreadingHTTPServer(('127.0.0.1',0),partial(Handler,directory=str(dist)))
   tls=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);tls.load_cert_chain(scratch/'cert.pem',scratch/'key.pem')
   server.socket=tls.wrap_socket(server.socket,server_side=True)
   thread=threading.Thread(target=server.serve_forever,daemon=True)
   servers.append(server);threads.append(thread);thread.start()
   environment[f'REGISTRATION_{app.upper()}_ORIGIN']=f'https://127.0.0.1:{server.server_port}'
  process=subprocess.Popen(['corepack','pnpm','exec','playwright','test','--config','playwright.registration.config.ts'],cwd=root/'frontend',env=environment,start_new_session=True)
  raise SystemExit(process.wait(timeout=420))
 finally:
  if process is not None and process.poll() is None: os.killpg(process.pid,signal.SIGKILL);process.wait()
  for server in servers: server.shutdown();server.server_close()
  for thread in threads: thread.join(timeout=5)
