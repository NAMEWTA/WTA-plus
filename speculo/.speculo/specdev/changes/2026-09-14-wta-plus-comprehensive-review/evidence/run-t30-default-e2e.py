#!/usr/bin/env python3
"""Run the unchanged default suite with the installed Chrome and one owned Vite server."""
import json
import signal
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import os
from pathlib import Path
import shutil
import socket
import subprocess
import sys
import tempfile
import threading
import time

root = Path(__file__).resolve().parents[6]
evidence = Path(__file__).resolve().parent
name = sys.argv[1]
assert name.startswith('T-30-') and '/' not in name
with socket.socket() as check:
    check.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    check.bind(('127.0.0.1', 4173))
owned = Path(tempfile.mkdtemp(prefix='t30-e2e-', dir=root/'temp/team/lead'))
(owned/'pnpm').write_text('#!/bin/sh\nexec corepack pnpm "$@"\n')
(owned/'pnpm').chmod(0o700)
arguments = sys.argv[2:]
reuse_build = '--reuse-build' in arguments
arguments = [value for value in arguments if value != '--reuse-build']
config = owned/'playwright.config.mjs'
config.write_text('import base from '+json.dumps(str(root/'frontend/playwright.config.ts'))+';\n'
    'export default { ...base, testDir: '+json.dumps(str(root/'frontend/e2e'))+',\n'
    'outputDir: '+json.dumps(str(root/'frontend/tests/e2e/reports/t30-regression'))+',\n'
    'workers: 1, retries: 0, reporter: "line",\n'
    'projects: base.projects.map(project => ({ ...project, use: { ...project.use, channel: "chrome" } })),\n'
    'webServer: { ...base.webServer, '+('command: \"corepack pnpm --filter @namewta/admin-web preview\", ' if reuse_build else '')+'reuseExistingServer: false, cwd: '+json.dumps(str(root/'frontend'))+' } };\n')
class RejectUnmocked(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(503); self.end_headers()
    do_POST = do_GET
    def log_message(self, *args):
        pass

fallback = ThreadingHTTPServer(('127.0.0.1', 0), RejectUnmocked)
fallback_thread = threading.Thread(target=fallback.serve_forever, daemon=True)
fallback_thread.start()
env = os.environ.copy()
env['NAMEWTA_T30_DEFAULT_RUN'] = owned.name
env.update(PATH=str(owned)+os.pathsep+env['PATH'], npm_config_workspace_concurrency='1', RAYON_NUM_THREADS='1',
           VITE_APP_CONTEXT_PATH='/', VITE_APP_BASE_API='/prod-api', VITE_APP_MESSAGE_ENABLED='true',
           VITE_APP_NACOS_ADMIN='/nacos/', VITE_APP_MONITOR_ADMIN='/applications',
           VITE_APP_PROXY_TARGET=f'http://127.0.0.1:{fallback.server_port}')
command = ['corepack', 'pnpm', 'test:e2e', '--config', str(config), '--workers=1', *arguments]
log = evidence/(name+'.log')
start = time.time()
interrupted = None
process = None
code = 1
row = dict(command=command, cwd='frontend', config=config.read_text(), log=str(log.relative_to(root)))
def on_signal(number, _frame):
    global interrupted
    interrupted = number
    raise SystemExit(128 + number)
signal.signal(signal.SIGTERM, on_signal)
signal.signal(signal.SIGINT, on_signal)
try:
    print('START', ' '.join(command), flush=True)
    with log.open('w') as output:
        process = subprocess.Popen(command, cwd=root/'frontend', env=env, stdout=output, stderr=subprocess.STDOUT)
        code = process.wait()
except SystemExit as failure:
    code = int(failure.code)
finally:
    if process is not None and process.poll() is None:
        process.terminate()
        try: process.wait(timeout=5)
        except subprocess.TimeoutExpired: process.kill(); process.wait()
    # Only processes bearing this exact per-run environment marker are owned here.
    marker = ('NAMEWTA_T30_DEFAULT_RUN=' + owned.name).encode()
    owned_children = []
    for entry in Path('/proc').iterdir():
        if not entry.name.isdecimal() or int(entry.name) == os.getpid(): continue
        try:
            if marker in (entry/'environ').read_bytes().split(b'\0'):
                owned_children.append(int(entry.name)); os.kill(int(entry.name), signal.SIGTERM)
        except (FileNotFoundError, ProcessLookupError, PermissionError): pass
    row['terminated_owned_children'] = owned_children
    fallback.shutdown(); fallback.server_close(); fallback_thread.join(timeout=5)
    shutil.rmtree(owned)
row.update(exit_code=code, seconds=round(time.time()-start,2), interrupted_signal=interrupted, owned_config_cleanup=not owned.exists())
for _ in range(50):
    with socket.socket() as check:
        row['owned_preview_closed'] = check.connect_ex(('127.0.0.1',4173)) != 0
    if row['owned_preview_closed']: break
    time.sleep(.1)
if not row['owned_preview_closed']: row['exit_code'] = code = 1
(evidence/(name+'.json')).write_text(json.dumps(row, ensure_ascii=False, indent=2)+'\n')
print(json.dumps(row, ensure_ascii=False), flush=True)
print(log.read_text()[-10000:], flush=True)
raise SystemExit(code)
