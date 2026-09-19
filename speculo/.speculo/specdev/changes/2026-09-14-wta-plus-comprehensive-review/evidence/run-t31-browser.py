#!/usr/bin/env python3
"""Build Home, serve only its owned local preview, run the transfer browser matrix."""
from pathlib import Path
import http.server
import json
import os
import subprocess
import sys
import threading
import time

root = Path(__file__).resolve().parents[6]
evidence = Path(__file__).resolve().parent
name = sys.argv[1]
assert name.startswith('T-31-') and '/' not in name
rows = []
def run(command, label, env=None):
    start = time.time(); log = evidence/(name+'-'+label+'.log')
    with log.open('w') as output:
        result = subprocess.run(command, cwd=root/'frontend', env=env, stdout=output, stderr=subprocess.STDOUT)
    row = dict(command=command, cwd='frontend', exit_code=result.returncode, seconds=round(time.time()-start, 2), log=log.name)
    rows.append(row); (evidence/(name+'.json')).write_text(json.dumps(rows, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps(row), flush=True)
    if result.returncode: print(log.read_text()[-6000:], flush=True)
    return result.returncode

if run(['corepack','pnpm','--filter','@namewta/home-web','build:prod'], 'build'): raise SystemExit(1)
class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs): super().__init__(*args, directory=str(root/'frontend/apps/home-web/dist'), **kwargs)
    def do_GET(self):
        if not Path(self.translate_path(self.path)).exists() and '.' not in self.path.rsplit('/',1)[-1]: self.path='/index.html'
        super().do_GET()
    def log_message(self, *_args): pass
server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), Handler)
thread = threading.Thread(target=server.serve_forever, daemon=True); thread.start()
try:
    env = {**os.environ, 'TRANSFER_EVIDENCE_DIR': str(evidence), 'TRANSFER_TEST_ORIGIN': 'http://127.0.0.1:'+str(server.server_port)}
    code = run(['corepack','pnpm','exec','playwright','test','--config','playwright.transfer.config.ts'], 'browser', env)
finally:
    server.shutdown(); server.server_close(); thread.join(timeout=5)
print('Owned Home server closed', flush=True)
raise SystemExit(code)
