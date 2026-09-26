#!/usr/bin/env python3
"""Offline T42 HTTP runner guards; no Docker, JVM, network or product writes."""
import importlib.util
import json
from pathlib import Path
import re
import subprocess
import unittest
from unittest import mock

PATH = Path('/tmp/wta-t42/capture-live-openapi-v5.py')
spec = importlib.util.spec_from_file_location('t42_http_v5_offline', PATH)
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)

class OfflineSafety(unittest.TestCase):
    def test_owned_login_config_keeps_password_auth_and_explicit_captcha_fixture(self):
        config=runner.isolated_config(33001,33002,'owned-user','owned-pass','owned-redis',Path('/tmp/owned-t42'))
        self.assertIs(config['captcha.enable'], False)
        self.assertEqual(config['server.address'], '127.0.0.1')
        source=PATH.read_text()
        self.assertIn("captcha['data'].get('captchaEnabled') is not False", source)
        self.assertIn("'grantType': 'password'", source)
        self.assertNotIn("'token': admin_token", source)

    def test_identity_and_frozen_dependencies(self):
        self.assertEqual(runner.OWNER, 'T-42-HTTP')
        self.assertEqual(runner.RUN_ROOT, Path('/tmp/wta-t42/openapi-http-live'))
        for path, expected in ((runner.HELPER_PATH, runner.HELPER_EXPECTED_SHA),
                               (runner.MINIO_AUX_PATH, runner.MINIO_AUX_SHA),
                               (runner.MC_PATH, runner.MC_SHA),
                               (runner.S3_PROXY_PATH, runner.S3_PROXY_SHA)):
            self.assertEqual(runner.sha_file(path), expected)

    def test_large_id_is_quoted_and_omission_distinct_from_empty(self):
        id_value = '9007199254740993'
        arguments = ('0123456789abcdef', 'exact-id', '2030-01-01T00:00:00Z')
        omitted = runner.submit_body(*arguments[:2], None, arguments[2])
        empty = runner.submit_body(*arguments[:2], [], arguments[2])
        positive = runner.submit_body(*arguments[:2], [id_value], arguments[2])
        self.assertNotIn('attachmentOssIds', omitted)
        self.assertEqual(empty['attachmentOssIds'], [])
        encoded = json.loads(json.dumps(positive))
        self.assertEqual(encoded['attachmentOssIds'], [id_value])
        self.assertIs(type(encoded['attachmentOssIds'][0]), str)
        self.assertEqual(positive['priority'], 0)
        self.assertEqual(positive['mode'], 'ASYNC')
        self.assertEqual(positive['strategy'], 'ALL')
        self.assertEqual(positive['recipientType'], 'EMAIL')
        self.assertRegex(positive['idempotencyKey'], r'^t42-http-')

    def test_http_rejects_nonlocal_and_unbounded_paths_without_socket(self):
        for path in ('https://example.invalid/notify/notification',
                     '//example.invalid/notify/notification',
                     '/notify/notification?token=unsafe', '../notify/notification'):
            with self.assertRaises(RuntimeError):
                runner.owned_http(12345, 'POST', path, None, {})

    def test_sql_text_uses_hex_and_only_fixed_tables(self):
        self.assertEqual(runner.hex_bytes('minio'), '0x6d696e696f')
        with mock.patch.object(runner.helper, 'mysql', return_value='0') as mysql:
            result = runner.app_counts('owned-cid', 't42-http-0123456789abcdef', 9007199254740993)
        self.assertEqual(set(result), {'intent', 'relation', 'recipient', 'delivery', 'outbox', 'source_ref'})
        self.assertEqual(mysql.call_count, 6)
        for call in mysql.call_args_list:
            self.assertEqual(call.args[1], 'wta-plus')
            self.assertNotIn('example.invalid', call.args[2])

    def test_numeric_json_rejection_stays_zero_write(self):
        before = {key: 3 for key in ('intent', 'relation', 'recipient', 'delivery', 'outbox', 'source_ref')}
        observation = runner.numeric_json_outcome(200, {'code': 400}, before, before, 0, True)
        self.assertEqual(observation['binding'], 'rejected')
        self.assertTrue(observation['zero_write'])
        with self.assertRaises(RuntimeError):
            runner.numeric_json_outcome(200, {'code': 400}, before,
                                        {**before, 'intent': 4}, 0, True)
        with self.assertRaises(RuntimeError):
            runner.numeric_json_outcome(200, {'code': 400}, before, before, 0, False)

    def test_numeric_json_acceptance_requires_exact_single_relation_and_ref(self):
        before = {key: 3 for key in ('intent', 'relation', 'recipient', 'delivery', 'outbox', 'source_ref')}
        after = {key: 4 for key in before}
        observation = runner.numeric_json_outcome(200, {'code': 200}, before, after, 1, True)
        self.assertEqual(observation['binding'], 'accepted_exact')
        self.assertTrue(observation['single_reference'])
        for changed_after, match, same_s3 in ((after, 0, True),
                                                ({**after, 'relation': 5}, 1, True),
                                                ({**after, 'source_ref': 3}, 1, True),
                                                (after, 1, False)):
            with self.assertRaises(RuntimeError):
                runner.numeric_json_outcome(200, {'code': 200}, before,
                                            changed_after, match, same_s3)
        with self.assertRaises(RuntimeError):
            runner.numeric_json_outcome(403, {'code': 200}, before, before, 0, True)

    def test_exact_relation_query_keeps_large_decimal_and_actor_client(self):
        source = '9007199254740993'
        with mock.patch.object(runner, 'sql_number', return_value=1) as count:
            self.assertEqual(runner.exact_source_relation_count(
                'owned-cid', '9007199254740995', source, 1761100000000000001,
                1762000000000000001), 1)
        sql = count.call_args.args[1]
        self.assertIn('a.source_oss_id=' + source, sql)
        self.assertIn('r.oss_id=' + source, sql)
        self.assertIn('i.attachment_actor_user_id=1761100000000000001', sql)
        self.assertIn('i.attachment_actor_client_pk=1762000000000000001', sql)
        self.assertIn('BINARY r.ref_id=BINARY CAST(a.intent_attachment_id AS CHAR)', sql)
        self.assertNotIn('float', sql)

    def test_cleanup_and_proof_guards_not_removed(self):
        source = PATH.read_text()
        for marker in ('validate_package_proof(', 'validate_full_jar(',
                       'helper.source_identity(args.expected_head)',
                       'helper.stop_group(app_proc)', 'cleanup_containers(run_id, captured)',
                       'helper.volume_absent(name)', 'helper.closed(port)',
                       's3_proxy.snapshot_drained(5)', 's3_proxy.shutdown()',
                       "'http_contract'", 'real_http_attachment_checks('):
            self.assertIn(marker, source)
        self.assertNotIn("HOME=str(run_dir)", source)
        self.assertIn("'notify.outbox.poll-delay-ms'] = 3600000", source)
        self.assertNotIn("report['java_command_redacted'] = list(java_argv + (", source)

    def test_no_exact_proof_refuses_launch_before_services(self):
        result = subprocess.run(['python3', str(PATH), '--execute'],
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                check=False, timeout=10)
        self.assertEqual(result.returncode, 2)
        self.assertNotIn(b'credential', result.stdout.lower())

if __name__ == '__main__':
    unittest.main(verbosity=2)
