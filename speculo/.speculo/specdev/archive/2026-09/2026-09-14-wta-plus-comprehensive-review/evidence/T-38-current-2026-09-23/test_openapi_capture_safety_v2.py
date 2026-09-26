#!/usr/bin/env python3
"""Offline checks for T-38 owned full-JAR OpenAPI capture; no service launch."""

import datetime as dt
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest import mock


SCRIPT = Path('/tmp/wta-t38/capture-live-openapi-v2.py')
spec = importlib.util.spec_from_file_location('t38_openapi_capture', SCRIPT)
capture = importlib.util.module_from_spec(spec)
spec.loader.exec_module(capture)


def sample_spec():
    return {
        'openapi': '3.0.3',
        'paths': {
            '/system/user/list': {'get': {'responses': {'200': {}}}},
            '/notify/notification/{notificationId}/retry': {'post': {'responses': {'200': {}}}},
            '/notify/notification/{notificationId}/cancel': {'post': {'responses': {'200': {}}}},
        },
        'components': {'schemas': {
            'SysUserVo': {'type': 'object'},
            'RetryReceipt': {'type': 'object', 'properties': {
                'queuedCount': {'type': 'integer', 'format': 'int32'}}},
        }},
    }


class CaptureSafetyTest(unittest.TestCase):
    def test_cli_requires_explicit_exact_source_and_jar(self):
        result = subprocess.run((sys.executable, str(SCRIPT)), capture_output=True, text=True)
        self.assertEqual(result.returncode, 2)
        self.assertIn('no launch without', result.stderr)

    def test_package_proof_binds_clean_command_log_time_and_jar(self):
        with tempfile.TemporaryDirectory() as directory:
            jar = Path(directory) / 'wta-admin.jar'
            jar.write_bytes(b'synthetic-full-bundle-placeholder')
            log = Path(directory) / 'build.log'
            log.write_text('BUILD SUCCESS\n')
            current = dt.datetime.fromtimestamp(jar.stat().st_mtime, dt.timezone.utc)
            proof = {
                'command': ['./mvnw', '-B', '-ntp', 'clean', 'package', '-DskipTests'],
                'cwd': 'backend', 'exit_code': 0, 'source_clean_at_build': True,
                'source_head': '1' * 40, 'source_tree': '2' * 40,
                'started_utc': (current - dt.timedelta(seconds=2)).isoformat(),
                'finished_utc': (current + dt.timedelta(seconds=2)).isoformat(),
                'build_log': {'path': str(log), 'sha256': hashlib.sha256(log.read_bytes()).hexdigest()},
                'artifact': {'path': 'backend/wta-admin/target/wta-admin.jar',
                             'sha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
                             'size_bytes': jar.stat().st_size},
            }
            with mock.patch.object(capture, 'ARTIFACT', jar):
                capture.validate_package_proof(proof, '1' * 40, '2' * 40,
                                               proof['artifact']['sha256'], jar.stat().st_size)
                for changed in (
                    {'source_head': '3' * 40},
                    {'source_clean_at_build': False},
                    {'command': ['./mvnw', 'package', '-DskipTests']},
                    {'started_utc': (current + dt.timedelta(seconds=3)).isoformat()},
                    {'build_log': {'path': str(log), 'sha256': '0' * 64}},
                ):
                    bad = {**proof, **changed}
                    with self.subTest(changed=tuple(changed)):
                        with self.assertRaises(RuntimeError):
                            capture.validate_package_proof(bad, '1' * 40, '2' * 40,
                                                           proof['artifact']['sha256'], jar.stat().st_size)

    def test_full_spec_preserves_every_path_method_schema_and_queued_count(self):
        old = sample_spec()
        new = sample_spec()
        verdict = capture.validate_full_openapi(json.dumps(new).encode(), old)
        self.assertEqual(verdict['queued_count_type'], 'integer')
        self.assertEqual(verdict['missing_paths'], 0)
        changed = json.loads(json.dumps(new))
        changed['paths'].pop('/system/user/list')
        with self.assertRaises(RuntimeError):
            capture.validate_full_openapi(json.dumps(changed).encode(), old)
        changed = json.loads(json.dumps(new))
        changed['paths']['/system/user/list'].pop('get')
        with self.assertRaises(RuntimeError):
            capture.validate_full_openapi(json.dumps(changed).encode(), old)
        changed = json.loads(json.dumps(new))
        changed['components']['schemas'].pop('SysUserVo')
        with self.assertRaises(RuntimeError):
            capture.validate_full_openapi(json.dumps(changed).encode(), old)
        changed = json.loads(json.dumps(new))
        changed['components']['schemas']['RetryReceipt']['properties']['queuedCount']['type'] = 'string'
        with self.assertRaises(RuntimeError):
            capture.validate_full_openapi(json.dumps(changed).encode(), old)

    def test_owned_config_overrides_external_urls_and_local_paths(self):
        with tempfile.TemporaryDirectory() as directory:
            config = capture.isolated_config(34217, 45631, 'owned_app', 'opaque-test-only',
                                             'r' * 40, Path(directory))
            self.assertEqual(config['server.address'], '127.0.0.1')
            self.assertEqual(config['server.port'], 0)
            self.assertIn('127.0.0.1:34217', config['spring.datasource.dynamic.datasource.master.url'])
            self.assertEqual(config['spring.data.redis.port'], 45631)
            self.assertEqual(config['spring.data.redis.password'], 'r' * 40)
            self.assertEqual(config['justauth.type.maxkey.server-url'], 'http://127.0.0.1:1')
            self.assertFalse(config['spring.boot.admin.client.enabled'])
            self.assertFalse(config['snail-job.enabled'])
            self.assertFalse(config['spring.ai.mcp.client.enabled'])
            self.assertTrue(config['springdoc.api-docs.enabled'])
            self.assertTrue(config['spring.servlet.multipart.location'].startswith(directory))
            self.assertTrue(config['management.endpoint.logfile.external-file'].startswith(directory))

    def test_0600_private_bytes_and_runtime_port_scope(self):
        with tempfile.TemporaryDirectory() as directory:
            secret = Path(directory) / 'owned.yml'
            capture.private_bytes(secret, b'opaque-test-only')
            self.assertEqual(secret.stat().st_mode & 0o777, 0o600)
        self.assertEqual(capture.runtime_port('访问地址：http://127.0.0.1:43417/'), 43417)
        self.assertIsNone(capture.runtime_port('访问地址：http://0.0.0.0:43417/'))
        self.assertIsNone(capture.runtime_port('访问地址：http://127.0.0.1:70000/'))

    def test_no_vendor_work_fresh_db_preflight(self):
        values = {'business_tables': 103, 'claimable_outboxes': 0, 'all_outboxes': 0,
                  'accepted_sms': 0, 'all_external_deliveries': 0,
                  'enabled_external_accounts': 0, 'private_default_oss': 1}
        queries = []
        def mysql(_cid, _db, query):
            queries.append(query)
            return str(values[list(values)[len(queries) - 1]])
        with mock.patch.object(capture.helper, 'mysql', mysql):
            self.assertEqual(capture.database_counts('f' * 64), values)
        self.assertEqual(len(queries), len(values))
        values['enabled_external_accounts'] = 1
        queries.clear()
        with mock.patch.object(capture.helper, 'mysql', mysql):
            with self.assertRaises(RuntimeError):
                capture.database_counts('f' * 64)

    def test_owned_redis_config_is_private_and_argv_has_no_secret(self):
        password = 'q' * 40
        content = capture.redis_config(password)
        self.assertIn(b'requirepass ', content)
        self.assertIn(password.encode(), content)
        self.assertIn(b'save ""', content)
        with self.assertRaises(RuntimeError):
            capture.redis_config('invalid')
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'redis.conf'
            capture.private_bytes(path, content)
            self.assertEqual(path.stat().st_mode & 0o777, 0o600)
        script = SCRIPT.read_text()
        self.assertIn("'--mount', 'type=bind,source=' + str(redis_conf)", script)
        self.assertIn("'redis-server', '/etc/redis/redis.conf'", script)
        self.assertNotIn("'--requirepass', redis_password", script)

    def test_redis_health_uses_config_loaded_auth_env_not_argv_password(self):
        seen = []
        def fake_run(argv, **_kwargs):
            seen.append(argv)
            return subprocess.CompletedProcess(argv, 0, b'PONG\n', b'')
        with mock.patch.object(capture.helper, 'run', fake_run):
            capture.wait_owned_redis('f' * 64, seconds=1)
        joined = ' '.join(seen[0])
        self.assertIn('REDISCLI_AUTH=', joined)
        self.assertIn('/etc/redis/redis.conf', joined)
        self.assertNotIn('q' * 40, joined)

    def test_raw_http_bytes_survive_strict_loss_with_specific_names(self):
        current = sample_spec()
        missing = sample_spec()
        missing['paths'].pop('/system/user/list')
        missing['components']['schemas'].pop('SysUserVo')
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)
            report = {}
            raw = json.dumps(missing, separators=(',', ':')).encode()
            with self.assertRaises(capture.OpenApiLossError):
                capture.persist_and_validate_openapi(path, raw, 'application/json', current, report)
            self.assertEqual((path / 'source.json').read_bytes(), raw)
            self.assertEqual((path / 'source.json').stat().st_mode & 0o777, 0o600)
            self.assertEqual(report['openapi_raw']['raw_sha256'], hashlib.sha256(raw).hexdigest())
            self.assertEqual(report['openapi_missing']['paths'], ['/system/user/list'])
            self.assertEqual(report['openapi_missing']['schemas'], ['SysUserVo'])
            self.assertEqual(report['openapi_missing']['operations'], [])

    def test_owned_dual_labels_and_full_id_cleanup(self):
        ids = ['a' * 64, 'b' * 64]
        calls = []
        def docker(*args, **_kwargs):
            calls.append(args)
            if args[:2] == ('ps', '-aq'):
                return '\n'.join(ids) if len([x for x in calls if x[:2] == ('ps', '-aq')]) == 1 else ''
            if args[:2] == ('inspect', '--format'):
                return json.dumps({'namewta.test.owner': capture.OWNER,
                                   'namewta.test.run': 'run-id'})
            if args[:2] == ('rm', '-fv'):
                return args[-1]
            raise AssertionError(args)
        with mock.patch.object(capture.helper, 'docker', docker):
            remaining, errors = capture.cleanup_containers('run-id', ids)
        self.assertEqual((remaining, errors), ([], []))
        self.assertTrue(any('label=namewta.test.owner=' + capture.OWNER in c for c in calls))
        self.assertTrue(any('label=namewta.test.run=run-id' in c for c in calls))
        self.assertEqual([c[-1] for c in calls if c[:2] == ('rm', '-fv')], ids[::-1])

    def test_label_mismatch_cannot_remove_container(self):
        cid = 'd' * 64
        calls = []
        def docker(*args, **_kwargs):
            calls.append(args)
            if args[:2] == ('ps', '-aq'):
                return cid
            if args[:2] == ('inspect', '--format'):
                return json.dumps({'namewta.test.owner': capture.OWNER,
                                   'namewta.test.run': 'other-run'})
            raise AssertionError('Unexpected Docker mutation')
        with mock.patch.object(capture.helper, 'docker', docker):
            remaining, errors = capture.cleanup_containers('run-id', [cid])
        self.assertEqual(remaining, [cid])
        self.assertIn('owned_container_remove_failed', errors)
        self.assertIn('owned_containers_remain', errors)
        self.assertFalse(any(c[:2] == ('rm', '-fv') for c in calls))

    def test_http_capture_is_direct_local_bounded_and_requires_200(self):
        seen = []
        class Response:
            status = 200
            def read(self, amount):
                seen.append(amount)
                return b'{"openapi":"3.0.3"}'
            def getheader(self, name, default=''):
                return 'application/json'
        class Connection:
            def __init__(self, host, port, timeout):
                seen.append((host, port, timeout))
            def request(self, method, path, headers):
                seen.append((method, path, headers))
            def getresponse(self):
                return Response()
            def close(self):
                pass
        with mock.patch.object(capture.http.client, 'HTTPConnection', Connection):
            body, _type = capture.fetch_local_bytes(43511)
        self.assertEqual(body, b'{"openapi":"3.0.3"}')
        self.assertEqual(seen[0], ('127.0.0.1', 43511, 10))
        self.assertEqual(seen[1][0:2], ('GET', '/v3/api-docs'))
        self.assertEqual(seen[2], capture.MAX_OPENAPI_BYTES + 1)

    def test_http_status_and_size_failure_gate(self):
        class Response:
            status = 503
            def read(self, _amount):
                return b''
            def getheader(self, _name, default=''):
                return default
        class Connection:
            def __init__(self, *_args, **_kwargs):
                pass
            def request(self, *_args, **_kwargs):
                pass
            def getresponse(self):
                return Response()
            def close(self):
                pass
        with mock.patch.object(capture.http.client, 'HTTPConnection', Connection):
            with self.assertRaisesRegex(RuntimeError, 'non-200'):
                capture.fetch_local_bytes(43000)
            Response.status = 200
            Response.read = lambda _self, amount: b'x' * amount
            with self.assertRaisesRegex(RuntimeError, 'bounded capture size'):
                capture.fetch_local_bytes(43000)


if __name__ == '__main__':
    unittest.main(verbosity=2)
