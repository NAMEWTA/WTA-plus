#!/usr/bin/env python3
"""Owned loopback HTTPS servers, built Apps, controlled browser HTTP faults; no deployment."""
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
import json
import hashlib
import re
import os
from pathlib import Path
import shutil
import ssl
import subprocess
import sys
import tempfile
import threading
import time

root = Path(__file__).resolve().parents[6]
evidence = Path(__file__).resolve().parent
name = sys.argv[1]
assert name.startswith('T-30-') and '/' not in name
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

owned = Path(tempfile.mkdtemp(prefix='t30-registration-', dir=root / 'temp/team/lead'))
servers = []
threads = []
stop_streams = threading.Event()
stream_count = 0

class Handler(SimpleHTTPRequestHandler):
    def do_GET(self):
        global stream_count
        if self.path.startswith('/prod-api/resource/message?'):
            stream_count += 1
            self.send_response(200)
            self.send_header('Content-Type', 'text/event-stream')
            self.send_header('Cache-Control', 'no-store')
            self.end_headers()
            try:
                while not stop_streams.is_set():
                    self.wfile.write(b': owned lifecycle heartbeat\n\n')
                    self.wfile.flush()
                    stop_streams.wait(0.2)
            except (BrokenPipeError, ConnectionResetError, ssl.SSLError):
                pass
            return
        if not Path(self.translate_path(self.path)).is_file():
            self.path = '/index.html'
        return super().do_GET()

    def log_message(self, *args):
        pass

try:
    subprocess.run(['openssl', 'req', '-x509', '-newkey', 'rsa:2048', '-nodes', '-days', '1',
                    '-subj', '/CN=127.0.0.1', '-addext', 'subjectAltName=IP:127.0.0.1',
                    '-keyout', str(owned/'key.pem'), '-out', str(owned/'cert.pem')],
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    env = os.environ.copy()
    env.update(npm_config_workspace_concurrency='1', RAYON_NUM_THREADS='1')
    for app in ('home',):
        dist = root / 'frontend/apps' / (app+'-web') / 'dist'
        assert (dist/'index.html').is_file()
        server = ThreadingHTTPServer(('127.0.0.1', 0), partial(Handler, directory=str(dist)))
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        servers.append(server); threads.append(thread); thread.start()
        env['TRANSFER_TEST_ORIGIN'] = f'http://127.0.0.1:{server.server_port}'
        env['TRANSFER_EVIDENCE_DIR'] = str(evidence/(name+'-artifacts'))
    command = ['corepack', 'pnpm', 'exec', 'playwright', 'test', '--config', 'playwright.transfer.config.ts', *sys.argv[2:]]
    log = evidence/(name+'.log')
    start = time.time()
    print('START', ' '.join(command), flush=True)
    with log.open('w') as output:
        result = subprocess.run(command, cwd=root/'frontend', env=env, stdout=output, stderr=subprocess.STDOUT)
    row = dict(command=command, cwd='frontend', exit_code=result.returncode, seconds=round(time.time()-start, 2),
               log=str(log.relative_to(root)), owned_origins=[env['TRANSFER_TEST_ORIGIN'] for app in ('home',)],
               boundary='Real built Apps/Chrome/TLS/Axios/Pinia/Router; explicit Playwright API identity and fault fixtures, no backend/database claim')
finally:
    stop_streams.set()
    for server in servers: server.shutdown(); server.server_close()
    for thread in threads: thread.join(timeout=5)
    shutil.rmtree(owned)
if 'row' in locals():
    clean_log = re.sub(r'\x1b\[[0-9;]*m', '', log.read_text())
    row['counts'] = {key: sum(int(n) for n in re.findall(r'^\s*(\d+) '+key+r'\b', clean_log, re.M)) for key in ('passed','failed','skipped')}
    row['expected_tests'] = 5
    row['source_fingerprint'] = source_fingerprint
    row['source_unchanged'] = source_snapshot()[0] == source_files
    row['owned_cleanup'] = all(not thread.is_alive() for thread in threads) and not owned.exists()
    row['real_https_sse_connections'] = stream_count
    row['verification_exit_code'] = 0 if row['exit_code'] == 0 and row['counts'] == {'passed': row['expected_tests'], 'failed': 0, 'skipped': 0} and row['source_unchanged'] and row['owned_cleanup'] else 1
    (evidence/(name+'.json')).write_text(json.dumps(row, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps(row, ensure_ascii=False), flush=True)
    print(log.read_text()[-12000:], flush=True)
    raise SystemExit(row['verification_exit_code'])
