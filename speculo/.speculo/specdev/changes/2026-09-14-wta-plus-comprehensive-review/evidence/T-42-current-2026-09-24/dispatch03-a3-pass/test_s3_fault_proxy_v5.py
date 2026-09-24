#!/usr/bin/env python3
"""Pure in-memory T42 v5 fail-closed fault checks; no socket/process."""

import importlib.util
import hashlib
import io
import json
from pathlib import Path
from types import SimpleNamespace
import threading
import time
from unittest import TestCase, main, mock


PATH = Path('/tmp/wta-t42/safe_s3_count_proxy_v5.py')
RUNNER = Path('/tmp/wta-t42/run-notify-mail-attachment-integration-v5.py')
SPEC = importlib.util.spec_from_file_location('t42_fault_proxy_offline', PATH)
proxy = importlib.util.module_from_spec(SPEC)
import sys
sys.modules[SPEC.name] = proxy
SPEC.loader.exec_module(proxy)
RUNNER_SPEC = importlib.util.spec_from_file_location('t42_fault_runner_offline', RUNNER)
runner = importlib.util.module_from_spec(RUNNER_SPEC)
sys.modules[RUNNER_SPEC.name] = runner
RUNNER_SPEC.loader.exec_module(runner)


def server():
    # Bypass HTTPServer.__init__: these tests never bind or start a service.
    value = object.__new__(proxy.SafeS3CountServer)
    value.metrics_lock = threading.Lock()
    value.arrivals = 0
    value.fault_condition = threading.Condition()
    value.fault_mode = 'NORMAL'
    value.hold_gate = None
    value.active_holds = 0
    value.dropped_put_success = 0
    value.held_puts = 0
    value.released_puts = 0
    value.upstream_port = 1
    return value


def control(s3, path, method='GET', *, host='127.0.0.1', headers=None):
    answers = []
    handler = SimpleNamespace(client_address=(host, 1234), path=path, headers=headers or {},
        server=SimpleNamespace(s3_proxy=s3),
        _reply=lambda status, body=b'', content_type='text/plain; charset=US-ASCII':
            answers.append((status, body, content_type)))
    getattr(proxy.SafeCountReadHandler, 'do_' + method)(handler)
    return answers


class FakeUpstream:
    def __init__(self, events):
        self.events = events

    def putrequest(self, method, target, **kwargs):
        self.events.append(('request', method))

    def putheader(self, *args):
        pass

    def endheaders(self, body):
        self.events.append(('forwarded_bytes', body))

    def getresponse(self):
        self.events.append(('provider_result', 200))
        return SimpleNamespace(status=200, read=lambda bound: b'ok',
                               getheaders=lambda: [], getheader=lambda name: None)

    def close(self):
        pass


def s3_put(s3, events, body=b'data'):
    connection = mock.Mock()
    response = []
    handler = SimpleNamespace(command='PUT', path='/owned-bucket/opaque-key',
        headers={'Content-Length': '4'}, rfile=io.BytesIO(body),
        server=s3, connection=connection, close_connection=False,
        _record=lambda status: events.append(('record', status)),
        _reply_empty=lambda status: response.append(('empty', status)),
        send_response_only=lambda status: response.append(('http', status)),
        send_header=lambda *args: None, end_headers=lambda: None,
        wfile=io.BytesIO())
    with mock.patch.object(proxy.http.client, 'HTTPConnection', return_value=FakeUpstream(events)):
        proxy.SafeCountProxy._proxy(handler)
    return connection, response


class FaultSafety(TestCase):
    def test_drop_arms_persistently_and_provider_commits_before_eof(self):
        s3 = server()
        self.assertEqual(control(s3, '/arm-drop-put', 'POST')[0][:2], (200, b'DROP_PUT_RESPONSE'))
        for expected in (1, 2):
            events = []
            connection, response = s3_put(s3, events)
            self.assertEqual(events[:3], [('request', 'PUT'), ('forwarded_bytes', b'data'),
                                          ('provider_result', 200)])
            self.assertEqual(events[3], ('record', 200))
            self.assertEqual(response, [])
            connection.close.assert_called_once()
            self.assertEqual(s3.fault_status()['dropped'], expected)
        self.assertEqual(control(s3, '/disarm', 'POST')[0][:2], (200, b'NORMAL'))
        events = []
        _, response = s3_put(s3, events)
        self.assertEqual(response, [('http', 200)])
        self.assertEqual(s3.fault_status()['dropped'], 2)

    def test_hold_has_no_upstream_write_until_release(self):
        s3 = server()
        self.assertEqual(control(s3, '/arm-hold-put', 'POST')[0][:2], (200, b'HOLD_PUT_FORWARD'))
        events, outcomes = [], []
        thread = threading.Thread(target=lambda: outcomes.append(s3_put(s3, events)))
        thread.start()
        deadline = time.monotonic() + 2
        while s3.fault_status()['held'] == 0 and time.monotonic() < deadline:
            time.sleep(.001)
        self.assertEqual(s3.fault_status()['held'], 1)
        self.assertEqual(events, [])  # body received, no upstream PUT yet
        self.assertEqual(control(s3, '/release-hold', 'POST')[0][:2], (200, b'NORMAL'))
        thread.join(timeout=2)
        self.assertFalse(thread.is_alive())
        self.assertEqual(events[0:2], [('request', 'PUT'), ('forwarded_bytes', b'data')])
        self.assertEqual(outcomes[0][1], [('http', 200)])
        self.assertEqual(s3.fault_status()['released'], 1)
        self.assertTrue(s3.wait_holds_drained(.1))

    def test_cleanup_cancels_hold_without_forwarding(self):
        s3 = server()
        s3.arm_hold_put()
        events, outcomes = [], []
        thread = threading.Thread(target=lambda: outcomes.append(s3_put(s3, events)))
        thread.start()
        deadline = time.monotonic() + 2
        while s3.fault_status()['held'] == 0 and time.monotonic() < deadline:
            time.sleep(.001)
        s3.cancel_holds()
        thread.join(timeout=2)
        self.assertFalse(thread.is_alive())
        self.assertTrue(s3.wait_holds_drained(.1))
        self.assertEqual(events, [('record', 504)])
        self.assertEqual(outcomes[0][1], [('empty', 504)])
        self.assertEqual(s3.fault_status()['released'], 0)

    def test_hold_timeout_fails_closed_without_forwarding(self):
        s3 = server()
        self.assertTrue(s3.arm_hold_put())
        gate = s3.begin_put_hold()
        with mock.patch.object(proxy, 'HOLD_MAX_SECONDS', 0):
            self.assertFalse(s3.await_put_release(gate))
        self.assertEqual(s3.fault_status(),
                         {'mode': 'HOLD_CANCELLED', 'dropped': 0, 'held': 1, 'released': 0})
        self.assertTrue(s3.wait_holds_drained(.1))

    def test_timeout_blocks_later_sdk_retry_until_explicit_disarm(self):
        s3 = server()
        s3.arm_hold_put()
        gate = s3.begin_put_hold()
        with mock.patch.object(proxy, 'HOLD_MAX_SECONDS', 0):
            self.assertFalse(s3.await_put_release(gate))
        events = []
        _, reply = s3_put(s3, events)
        self.assertEqual(events, [('record', 504)])
        self.assertEqual(reply, [('empty', 504)])
        self.assertEqual(s3.fault_status()['mode'], 'HOLD_CANCELLED')
        self.assertEqual(control(s3, '/release-hold', 'POST')[0][0], 409)
        self.assertEqual(control(s3, '/disarm', 'POST')[0][:2], (200, b'NORMAL'))
        events = []
        _, reply = s3_put(s3, events)
        self.assertIn(('forwarded_bytes', b'data'), events)
        self.assertEqual(reply, [('http', 200)])

    def test_cleanup_cancel_blocks_later_sdk_retry(self):
        s3 = server()
        s3.arm_hold_put()
        s3.cancel_holds()
        self.assertEqual(s3.fault_status()['mode'], 'HOLD_CANCELLED')
        events = []
        _, reply = s3_put(s3, events)
        self.assertEqual(events, [('record', 504)])
        self.assertEqual(reply, [('empty', 504)])
        self.assertEqual(s3.fault_status()['released'], 0)

    def test_incomplete_body_is_never_forwarded_or_held(self):
        s3 = server()
        self.assertTrue(s3.arm_hold_put())
        events = []
        _, replies = s3_put(s3, events, body=b'da')
        self.assertEqual(events, [('record', 400)])
        self.assertEqual(replies, [('empty', 400)])
        self.assertEqual(s3.fault_status()['held'], 0)

    def test_status_only_fixed_enum_and_numbers(self):
        s3 = server()
        answer = control(s3, '/status')[0]
        self.assertEqual(answer[0], 200)
        self.assertEqual(answer[2], 'application/json')
        self.assertEqual(json.loads(answer[1]),
                         {'mode': 'NORMAL', 'dropped': 0, 'held': 0, 'released': 0})
        self.assertNotIn(b'key', answer[1])
        self.assertNotIn(b'bucket', answer[1])

    def test_count_stays_numeric_and_control_does_not_increment(self):
        s3 = server()
        s3.snapshot_drained = lambda timeout: ({}, 7)
        self.assertEqual(control(s3, '/count')[0][:2], (200, b'7'))
        self.assertEqual(control(s3, '/count')[0][:2], (200, b'7'))

    def test_only_fixed_empty_body_loopback_commands(self):
        s3 = server()
        for path in ('/arm-drop-put?bucket=x', '/bad', '/count'):
            self.assertEqual(control(s3, path, 'POST')[0][0], 404)
        self.assertEqual(control(s3, '/arm-drop-put', 'POST', host='192.0.2.1')[0][0], 404)
        self.assertEqual(control(s3, '/arm-drop-put', 'POST', headers={'Content-Length': '3'})[0][0], 404)
        self.assertEqual(control(s3, '/arm-drop-put', 'POST', headers={'Transfer-Encoding': 'chunked'})[0][0], 404)
        self.assertEqual(s3.fault_status()['mode'], 'NORMAL')

    def test_invalid_mode_transition_is_conflict(self):
        s3 = server()
        self.assertEqual(control(s3, '/arm-drop-put', 'POST')[0][0], 200)
        self.assertEqual(control(s3, '/arm-hold-put', 'POST')[0][0], 409)
        self.assertEqual(control(s3, '/release-hold', 'POST')[0][0], 409)
        self.assertEqual(control(s3, '/disarm', 'POST')[0][0], 200)

    def test_runner_pins_v5_and_preserves_count_property(self):
        self.assertEqual(hashlib.sha256(PATH.read_bytes()).hexdigest(), runner.PROXY_SHA)
        self.assertIn('t42.s3.count-url', runner.PROPERTY_KEYS)
        self.assertEqual(runner.load_proxy().SafeS3CountReadServer.__name__,
                         proxy.SafeS3CountReadServer.__name__)

    def test_normal_disarm_is_idempotent_only_without_active_hold(self):
        s3 = server()
        self.assertEqual(control(s3, '/disarm', 'POST')[0][:2], (200, b'NORMAL'))
        self.assertEqual(control(s3, '/arm-hold-put', 'POST')[0][0], 200)
        gate = s3.begin_put_hold()
        self.assertEqual(control(s3, '/release-hold', 'POST')[0][:2], (200, b'NORMAL'))
        self.assertEqual(s3.fault_status()['mode'], 'NORMAL')
        self.assertEqual(control(s3, '/disarm', 'POST')[0][0], 409)
        self.assertTrue(s3.await_put_release(gate))
        self.assertTrue(s3.wait_holds_drained(.1))
        self.assertEqual(control(s3, '/disarm', 'POST')[0][:2], (200, b'NORMAL'))

    def test_cancelled_mode_cannot_disarm_while_hold_waiter_active(self):
        s3 = server()
        s3.arm_hold_put()
        gate = s3.begin_put_hold()
        s3.cancel_holds()
        self.assertEqual(control(s3, '/disarm', 'POST')[0][0], 409)
        self.assertFalse(s3.await_put_release(gate))
        self.assertEqual(control(s3, '/disarm', 'POST')[0][:2], (200, b'NORMAL'))


if __name__ == '__main__':
    main(verbosity=2)
