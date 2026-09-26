#!/usr/bin/env python3
"""T42 v5 private MinIO proxy with fail-closed held-PUT retries.

Copy or import from /tmp into a future exact-source owned runner. Importing this
module does not open a socket. The proxy never logs a raw request target,
header, query value, object key, policy body, credential or response body.
"""

from collections import Counter
from dataclasses import dataclass, field
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import http.client
import json
import re
import socket
import threading
import time
from urllib.parse import unquote, urlsplit


METHODS = frozenset(('GET', 'HEAD', 'PUT', 'POST', 'DELETE'))
CATEGORIES = frozenset(('POLICY', 'ACL', 'OBJECT', 'BUCKET', 'OTHER'))
ALIAS = re.compile(r'[a-z][a-z0-9_]{0,31}\Z')
MAX_BODY = 2_000_000
HOLD_MAX_SECONDS = 60
FAULT_MODES = frozenset(('NORMAL', 'DROP_PUT_RESPONSE', 'HOLD_PUT_FORWARD', 'HOLD_CANCELLED'))


@dataclass
class HoldGate:
    event: threading.Event = field(default_factory=threading.Event)
    released: bool = False
    cancelled: bool = False


@dataclass(frozen=True, order=True)
class SafeFact:
    method: str
    category: str
    bucket_alias: str
    signed: bool
    ranged: bool
    status: int

    def __post_init__(self):
        if (self.method not in METHODS or self.category not in CATEGORIES
            or not ALIAS.fullmatch(self.bucket_alias) or not 100 <= self.status <= 599):
            raise ValueError('unsafe proxy fact')

    def row(self, count):
        return {'method': self.method, 'category': self.category,
                'bucket_alias': self.bucket_alias, 'signed': self.signed,
                'range': self.ranged, 'status': self.status, 'count': count}


def classify(method, request_target, headers, bucket_aliases, status):
    """Use only query *names* and auth/range presence; discard all values."""
    if method not in METHODS or type(request_target) is not str or len(request_target) > 8192:
        raise ValueError('unsupported request metadata')
    parsed = urlsplit(request_target)
    if parsed.scheme or parsed.netloc or parsed.fragment or not parsed.path.startswith('/'):
        raise ValueError('invalid proxy request target')
    parts = parsed.path.split('/', 2)
    bucket_name = unquote(parts[1]) if len(parts) > 1 else ''
    bucket_alias = bucket_aliases.get(bucket_name, 'other')
    if not ALIAS.fullmatch(bucket_alias):
        raise ValueError('unsafe bucket alias')
    query_names = {unquote(piece.split('=', 1)[0]).lower()
                   for piece in parsed.query.split('&') if piece}
    if 'policy' in query_names and 'acl' not in query_names:
        category = 'POLICY'
    elif 'acl' in query_names and 'policy' not in query_names:
        category = 'ACL'
    elif 'policy' in query_names and 'acl' in query_names:
        category = 'OTHER'
    elif len(parts) > 2 and parts[2]:
        category = 'OBJECT'
    elif query_names:
        category = 'OTHER'
    else:
        category = 'BUCKET'
    # Header values and query values stay transient; no token or key is retained.
    signed = bool(headers.get('Authorization')) or 'x-amz-signature' in query_names
    ranged = 'Range' in headers
    return SafeFact(method, category, bucket_alias, signed, ranged, status)


def safe_rows(snapshot):
    if any(type(count) is not int or count < 0 for count in snapshot.values()):
        raise ValueError('invalid proxy count')
    return [fact.row(count) for fact, count in sorted(snapshot.items()) if count]


def safe_delta(before, after):
    if any(after.get(fact, 0) < count for fact, count in before.items()):
        raise ValueError('proxy counters went backward')
    return safe_rows({fact: after.get(fact, 0) - before.get(fact, 0)
                      for fact in set(before) | set(after)})


class SafeCountProxy(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'

    def log_message(self, *_):
        pass

    def do_GET(self): self._proxy()
    def do_HEAD(self): self._proxy()
    def do_PUT(self): self._proxy()
    def do_POST(self): self._proxy()
    def do_DELETE(self): self._proxy()

    def _reply_empty(self, status):
        self.send_response_only(status)
        self.send_header('Content-Length', '0')
        self.send_header('Connection', 'close')
        self.end_headers()

    def _record(self, status):
        try:
            fact = classify(self.command, self.path, self.headers,
                            self.server.bucket_aliases, status)
        except ValueError:
            fact = SafeFact(self.command, 'OTHER', 'other', False, False, status)
        with self.server.metrics_condition:
            self.server.metrics[fact] += 1
            self.server.metrics_condition.notify_all()

    def _proxy(self):
        # Count arrival before an upstream call can block. Startup-zero must
        # reject even a probe that has not produced a status yet.
        with self.server.metrics_lock:
            self.server.arrivals += 1
        try:
            size = int(self.headers.get('Content-Length', '0'))
        except ValueError:
            size = -1
        if size < 0 or size > MAX_BODY or self.headers.get('Transfer-Encoding'):
            try:
                self._reply_empty(413)
            finally:
                self._record(413)
                self.close_connection = True
            return
        self.connection.settimeout(10)  # bounded synthetic body read, including held PUTs
        upstream = http.client.HTTPConnection('127.0.0.1', self.server.upstream_port, timeout=4)
        status = 502
        recorded = False
        try:
            body = self.rfile.read(size) if size else None
            if size and len(body) != size:
                status = 400
                self._reply_empty(status)
                return
            if self.command == 'PUT':
                gate = self.server.begin_put_hold()
                if gate is not None and not self.server.await_put_release(gate):
                    status = 504
                    self._reply_empty(status)
                    return
            # Preserve original Host for SigV4, without persisting it or the signature.
            upstream.putrequest(self.command, self.path, skip_host=True,
                                skip_accept_encoding=True)
            for key, value in self.headers.items():
                if key.lower() not in ('connection', 'proxy-connection', 'transfer-encoding'):
                    upstream.putheader(key, value)
            upstream.putheader('Connection', 'close')
            upstream.endheaders(body)
            response = upstream.getresponse()
            payload = response.read(MAX_BODY + 1)
            if len(payload) > MAX_BODY:
                raise RuntimeError('bounded upstream body exceeded')
            status = response.status
            # Count the actual provider result before forwarding. A canceled
            # client may close the socket during write, but did not turn the
            # provider's completed 200/206 into a 502.
            self._record(status)
            recorded = True
            if (self.command == 'PUT' and 200 <= status < 300
                    and self.server.drop_successful_put_response()):
                # The provider has committed the write, but every successful
                # response while armed becomes an EOF to the SDK. Retried PUTs
                # are also dropped until an explicit disarm.
                self.close_connection = True
                try:
                    self.connection.shutdown(socket.SHUT_RDWR)
                except OSError:
                    pass
                self.connection.close()
                return
            self.send_response_only(status)
            for key, value in response.getheaders():
                if key.lower() not in ('connection', 'transfer-encoding', 'content-length'):
                    self.send_header(key, value)
            length = response.getheader('Content-Length') if self.command == 'HEAD' else str(len(payload))
            if length is not None:
                self.send_header('Content-Length', length)
            self.send_header('Connection', 'close')
            self.end_headers()
            if self.command != 'HEAD':
                self.wfile.write(payload)
        except Exception:
            if not recorded:
                try:
                    self._reply_empty(status)
                except Exception:
                    pass
        finally:
            if not recorded:
                self._record(status)
            upstream.close()
            self.close_connection = True


class SafeS3CountServer(ThreadingHTTPServer):
    daemon_threads = True

    def __init__(self, upstream_port, bucket_aliases):
        if type(upstream_port) is not int or not 1 <= upstream_port <= 65535:
            raise ValueError('invalid owned upstream port')
        if (type(bucket_aliases) is not dict or not bucket_aliases
            or any(type(bucket) is not str or not bucket or not ALIAS.fullmatch(alias)
                   for bucket, alias in bucket_aliases.items())):
            raise ValueError('invalid owned bucket aliases')
        self.upstream_port = upstream_port
        self.bucket_aliases = dict(bucket_aliases)
        self.metrics_lock = threading.Lock()
        self.metrics_condition = threading.Condition(self.metrics_lock)
        self.metrics = Counter()
        self.arrivals = 0
        self.fault_condition = threading.Condition()
        self.fault_mode = 'NORMAL'
        self.hold_gate = None
        self.active_holds = 0
        self.dropped_put_success = 0
        self.held_puts = 0
        self.released_puts = 0
        super().__init__(('127.0.0.1', 0), SafeCountProxy)

    def arm_drop_put(self):
        with self.fault_condition:
            if self.fault_mode != 'NORMAL':
                return False
            self.fault_mode = 'DROP_PUT_RESPONSE'
            return True

    def disarm_drop_put(self):
        with self.fault_condition:
            # A completed hold release already enters NORMAL. The test's
            # finally cleanup may repeat disarm only after all held PUTs drain.
            if self.fault_mode == 'NORMAL':
                return self.active_holds == 0
            if self.fault_mode not in ('DROP_PUT_RESPONSE', 'HOLD_CANCELLED'):
                return False
            if self.fault_mode == 'HOLD_CANCELLED' and self.active_holds:
                return False
            self.fault_mode = 'NORMAL'
            return True

    def drop_successful_put_response(self):
        with self.fault_condition:
            if self.fault_mode != 'DROP_PUT_RESPONSE':
                return False
            self.dropped_put_success += 1
            return True

    def arm_hold_put(self):
        with self.fault_condition:
            if self.fault_mode != 'NORMAL':
                return False
            self.hold_gate = HoldGate()
            self.fault_mode = 'HOLD_PUT_FORWARD'
            return True

    def begin_put_hold(self):
        with self.fault_condition:
            if self.fault_mode not in ('HOLD_PUT_FORWARD', 'HOLD_CANCELLED'):
                return None
            self.active_holds += 1
            self.held_puts += 1
            return self.hold_gate

    def await_put_release(self, gate):
        opened = gate.event.wait(HOLD_MAX_SECONDS)
        with self.fault_condition:
            if not opened:
                gate.cancelled = True
                gate.event.set()
                if self.hold_gate is gate:
                    # A later SDK retry must not become an unheld physical PUT.
                    # Even a release racing this timeout loses to the fail-close.
                    self.fault_mode = 'HOLD_CANCELLED'
            allowed = gate.released and not gate.cancelled
            if allowed:
                self.released_puts += 1
            self.active_holds -= 1
            self.fault_condition.notify_all()
            return allowed

    def release_hold(self):
        with self.fault_condition:
            if self.fault_mode != 'HOLD_PUT_FORWARD':
                return False
            self.fault_mode = 'NORMAL'
            self.hold_gate.released = True
            self.hold_gate.event.set()
            return True

    def cancel_holds(self):
        with self.fault_condition:
            if self.hold_gate is not None:
                self.fault_mode = 'HOLD_CANCELLED'
                if not self.hold_gate.released:
                    self.hold_gate.cancelled = True
                    self.hold_gate.event.set()

    def wait_holds_drained(self, seconds=10):
        deadline = time.monotonic() + seconds
        with self.fault_condition:
            while self.active_holds:
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    return False
                self.fault_condition.wait(remaining)
            return True

    def fault_status(self):
        with self.fault_condition:
            return {'mode': self.fault_mode, 'dropped': self.dropped_put_success,
                    'held': self.held_puts, 'released': self.released_puts}

    def snapshot(self):
        with self.metrics_lock:
            return dict(self.metrics)

    def snapshot_arrivals(self):
        with self.metrics_lock:
            return self.arrivals

    def snapshot_drained(self, timeout_seconds=2.0):
        """Return completed result plus arrivals only when no request is in flight."""
        if type(timeout_seconds) not in (int, float) or not 0 < timeout_seconds <= 10:
            raise ValueError('invalid bounded proxy wait')
        deadline = time.monotonic() + timeout_seconds
        with self.metrics_condition:
            while self.arrivals != sum(self.metrics.values()):
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    raise RuntimeError('provider request still in flight')
                self.metrics_condition.wait(remaining)
            return dict(self.metrics), self.arrivals


class SafeCountReadHandler(BaseHTTPRequestHandler):
    """A separate control port; these requests never enter S3 request counters."""

    protocol_version = 'HTTP/1.1'

    def log_message(self, *_):
        pass

    def _reply(self, status, body=b'', content_type='text/plain; charset=US-ASCII'):
        self.send_response_only(status)
        self.send_header('Content-Type', content_type)
        self.send_header('Cache-Control', 'no-store')
        self.send_header('Content-Length', str(len(body)))
        self.send_header('Connection', 'close')
        self.end_headers()
        if body:
            self.wfile.write(body)
        self.close_connection = True

    def do_GET(self):
        if (self.client_address[0] != '127.0.0.1' or self.path not in ('/count', '/status')
                or self.headers.get('Content-Length') not in (None, '0')
                or self.headers.get('Transfer-Encoding')):
            self._reply(404)
            return
        if self.path == '/status':
            try:
                status = self.server.s3_proxy.fault_status()
            except Exception:
                self._reply(503)
                return
            if status['mode'] not in FAULT_MODES:
                self._reply(503)
                return
            self._reply(200, json.dumps(status, sort_keys=True, separators=(',', ':')).encode('ascii'),
                        'application/json')
            return
        try:
            _, arrivals = self.server.s3_proxy.snapshot_drained(5)
        except Exception:
            self._reply(503)
            return
        # Only a nonnegative decimal count crosses this interface. No URL,
        # headers, object name, bucket name, status facts or credentials.
        self._reply(200, str(arrivals).encode('ascii'))

    def do_POST(self):
        if (self.client_address[0] != '127.0.0.1'
                or self.headers.get('Content-Length') not in (None, '0')
                or self.headers.get('Transfer-Encoding')):
            self._reply(404)
            return
        actions = {'/arm-drop-put': ('arm_drop_put', 'DROP_PUT_RESPONSE'),
                   '/disarm': ('disarm_drop_put', 'NORMAL'),
                   '/arm-hold-put': ('arm_hold_put', 'HOLD_PUT_FORWARD'),
                   '/release-hold': ('release_hold', 'NORMAL')}
        action = actions.get(self.path)
        if action is None:
            self._reply(404)
            return
        try:
            allowed = getattr(self.server.s3_proxy, action[0])()
        except Exception:
            self._reply(503)
            return
        if not allowed:
            self._reply(409)
            return
        self._reply(200, action[1].encode('ascii'))

    def do_HEAD(self): self._reply(405)
    def do_PUT(self): self._reply(405)
    def do_DELETE(self): self._reply(405)


class SafeS3CountReadServer(ThreadingHTTPServer):
    daemon_threads = True

    def __init__(self, s3_proxy):
        if not isinstance(s3_proxy, SafeS3CountServer):
            raise ValueError('count reader requires an owned S3 proxy')
        self.s3_proxy = s3_proxy
        super().__init__(('127.0.0.1', 0), SafeCountReadHandler)
