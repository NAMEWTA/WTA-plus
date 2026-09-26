#!/usr/bin/env python3
"""Private loopback Admin dist server with a same-origin /prod-api backend proxy.

No request target, header, body or query value is logged or retained. Importing
this module does not open a socket. Only the T45 owned browser runner starts it.
"""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import http.client
import mimetypes
from pathlib import Path
import threading
import time
from urllib.parse import unquote, urlsplit


MAX_API_BODY = 1_000_000
MAX_API_RESPONSE = 8_000_000
MAX_ASSET = 32_000_000


def dist_asset(root: Path, target: str):
    parsed = urlsplit(target)
    if parsed.scheme or parsed.netloc or parsed.fragment or not parsed.path.startswith('/'):
        raise ValueError('invalid local target')
    decoded = unquote(parsed.path)
    if '\x00' in decoded or any(part in ('.', '..') for part in decoded.split('/')):
        raise ValueError('unsafe local target')
    root = root.resolve(strict=True)
    relative = decoded.lstrip('/')
    asset = (root / relative).resolve(strict=False)
    if not asset.is_relative_to(root):
        raise ValueError('unsafe local target')
    if asset.is_file():
        return asset
    if '.' not in Path(relative).name:
        return root / 'index.html'
    return None


class OwnedAdminHandler(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'

    def log_message(self, *_):
        pass

    def do_GET(self): self._dispatch()
    def do_HEAD(self): self._dispatch()
    def do_POST(self): self._dispatch()
    def do_PUT(self): self._dispatch()
    def do_DELETE(self): self._dispatch()

    def _empty(self, status):
        self.send_response_only(status)
        self.send_header('Content-Length', '0')
        self.send_header('Connection', 'close')
        self.end_headers()

    def _dispatch(self):
        with self.server.active_condition:
            self.server.active_requests += 1
        try:
            self.connection.settimeout(25)
            if self.path.startswith('/prod-api/'):
                self._api()
            elif self.command in ('GET', 'HEAD'):
                self._asset()
            else:
                self._empty(405)
        finally:
            self.close_connection = True
            with self.server.active_condition:
                self.server.active_requests -= 1
                self.server.active_condition.notify_all()

    def _asset(self):
        try:
            asset = dist_asset(self.server.dist_root, self.path)
            if asset is None or not asset.is_file() or asset.stat().st_size > MAX_ASSET:
                self._empty(404)
                return
            payload = asset.read_bytes()
            self.send_response_only(200)
            self.send_header('Content-Type', mimetypes.guess_type(asset.name)[0] or 'application/octet-stream')
            self.send_header('Content-Length', str(len(payload)))
            self.send_header('Connection', 'close')
            self.end_headers()
            if self.command != 'HEAD':
                self.wfile.write(payload)
        except BrokenPipeError:
            pass
        except (OSError, ValueError):
            try: self._empty(404)
            except OSError: pass

    def _api(self):
        try:
            size = int(self.headers.get('Content-Length', '0'))
        except ValueError:
            size = -1
        if size < 0 or size > MAX_API_BODY or self.headers.get('Transfer-Encoding'):
            self._empty(413)
            return
        upstream = http.client.HTTPConnection('127.0.0.1', self.server.backend_port, timeout=20)
        try:
            body = self.rfile.read(size) if size else None
            target = self.path[len('/prod-api'):]
            if not target.startswith('/') or urlsplit(target).scheme or urlsplit(target).netloc:
                self._empty(400)
                return
            headers = {key: value for key, value in self.headers.items()
                       if key.lower() not in ('host', 'connection', 'proxy-connection',
                                              'transfer-encoding', 'content-length')}
            upstream.request(self.command, target, body=body, headers=headers)
            response = upstream.getresponse()
            payload = response.read(MAX_API_RESPONSE + 1)
            if len(payload) > MAX_API_RESPONSE:
                self._empty(502)
                return
            self.send_response_only(response.status)
            for key, value in response.getheaders():
                if key.lower() not in ('connection', 'proxy-connection', 'transfer-encoding',
                                       'content-length'):
                    self.send_header(key, value)
            self.send_header('Content-Length', str(len(payload)))
            self.send_header('Connection', 'close')
            self.end_headers()
            if self.command != 'HEAD':
                self.wfile.write(payload)
        except (OSError, http.client.HTTPException):
            try: self._empty(502)
            except OSError: pass
        finally:
            upstream.close()


class OwnedAdminDistServer(ThreadingHTTPServer):
    daemon_threads = True

    def __init__(self, dist_root: Path, backend_port: int):
        if not (dist_root / 'index.html').is_file() or not 1 <= backend_port <= 65535:
            raise ValueError('invalid owned Admin dist inputs')
        self.dist_root = dist_root.resolve(strict=True)
        self.backend_port = backend_port
        self.active_condition = threading.Condition()
        self.active_requests = 0
        super().__init__(('127.0.0.1', 0), OwnedAdminHandler)

    def wait_idle(self, timeout_seconds=5.0):
        deadline = time.monotonic() + timeout_seconds
        with self.active_condition:
            while self.active_requests:
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    return False
                self.active_condition.wait(remaining)
        return True

    def handle_error(self, request, client_address):
        # No raw browser URL, token or request body in server stderr.
        pass
