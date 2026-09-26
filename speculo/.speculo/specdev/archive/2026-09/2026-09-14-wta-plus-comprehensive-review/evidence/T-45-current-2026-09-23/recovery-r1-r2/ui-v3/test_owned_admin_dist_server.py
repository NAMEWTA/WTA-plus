#!/usr/bin/env python3
"""Pure path guard checks; never open a socket or start a service."""
import importlib.util
import io
from pathlib import Path
import tempfile
import threading
import unittest
from unittest.mock import patch


path = Path('/tmp/wta-t45/ui/owned_admin_dist_server.py')
spec = importlib.util.spec_from_file_location('owned_admin_dist_server', path)
server = importlib.util.module_from_spec(spec)
spec.loader.exec_module(server)


class DistPath(unittest.TestCase):
    def test_routes_and_assets_remain_under_owned_dist(self):
        with tempfile.TemporaryDirectory(prefix='t45-dist-offline-') as tmp:
            root = Path(tmp)
            (root / 'index.html').write_text('html')
            (root / 'assets').mkdir()
            (root / 'assets' / 'app.js').write_text('js')
            self.assertEqual(server.dist_asset(root, '/system/oss-config/index'), root / 'index.html')
            self.assertEqual(server.dist_asset(root, '/assets/app.js?x=1'), root / 'assets/app.js')
            self.assertIsNone(server.dist_asset(root, '/assets/missing.js'))

    def test_traversal_and_symlink_escape_are_rejected(self):
        with tempfile.TemporaryDirectory(prefix='t45-dist-offline-') as tmp:
            root = Path(tmp) / 'dist'; root.mkdir()
            (root / 'index.html').write_text('html')
            (root / 'escape').symlink_to(Path(tmp))
            for target in ('/../secret', '/%2e%2e/secret', 'http://external.example/a',
                           '/escape/private', '/%00'):
                with self.subTest(target=target):
                    with self.assertRaises(ValueError):
                        server.dist_asset(root, target)

    def test_idle_check_rejects_unfinished_request(self):
        instance = object.__new__(server.OwnedAdminDistServer)
        instance.active_condition = threading.Condition()
        instance.active_requests = 1
        self.assertFalse(instance.wait_idle(.001))
        instance.active_requests = 0
        self.assertTrue(instance.wait_idle(.001))

    def test_prod_api_forwards_to_owned_backend_without_raw_logging(self):
        calls = []
        response = type('Response', (), {'status': 200,
            'read': lambda self, size: b'{"code":403}',
            'getheaders': lambda self: [('Content-Type', 'application/json')]})()
        upstream = type('Upstream', (), {'request': lambda self, method, target, body=None,
            headers=None: calls.append((method, target, body, headers)),
            'getresponse': lambda self: response,
            'close': lambda self: None})()
        handler = object.__new__(server.OwnedAdminHandler)
        handler.server = type('Owned', (), {'backend_port': 12345})()
        handler.command = 'POST'
        handler.path = '/prod-api/resource/oss/config/diagnose/1'
        handler.headers = {'Authorization': 'Bearer synthetic-private',
                           'Content-Length': '0', 'clientid': 'owned-client'}
        handler.wfile = io.BytesIO()
        handler.send_response_only = lambda status: calls.append(('status', status))
        handler.send_header = lambda key, value: None
        handler.end_headers = lambda: None
        with patch.object(server.http.client, 'HTTPConnection', return_value=upstream):
            handler._api()
        method, target, body, headers = calls[0]
        self.assertEqual((method, target, body), ('POST', '/resource/oss/config/diagnose/1', None))
        self.assertEqual(headers['Authorization'], 'Bearer synthetic-private')
        self.assertEqual(calls[1], ('status', 200))
        self.assertEqual(handler.wfile.getvalue(), b'{"code":403}')


if __name__ == '__main__':
    unittest.main(verbosity=2)
