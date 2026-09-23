#!/usr/bin/env python3
"""Offline safety checks only: never start Docker, JVM, Maven or a browser."""

import datetime as dt
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import types
import unittest
from unittest.mock import patch

import bcrypt


SCRIPT = Path(__file__).with_name('run-inbox-paged-real.py')
spec = importlib.util.spec_from_file_location('t41_browser', SCRIPT)
driver = importlib.util.module_from_spec(spec)
spec.loader.exec_module(driver)


class T41OfflineSafety(unittest.TestCase):
    def test_requires_explicit_exact_execute(self):
        result = subprocess.run([sys.executable, str(SCRIPT)], capture_output=True, text=True)
        self.assertEqual(result.returncode, 2)
        self.assertIn('no launch without', result.stderr)

    def test_seed_is_501_plus_two_and_interleaved(self):
        sql, manifest = driver.seed_plan('0123456789abcdef', 101, 202,
                                         base=8_000_000_000_000_000_000)
        self.assertEqual((manifest['a']['total'], manifest['a']['unread']), (501, 481))
        self.assertEqual((manifest['b']['total'], manifest['b']['unread']), (2, 2))
        self.assertEqual(manifest['pageNum'], 26)
        self.assertEqual(manifest['pageSize'], 20)
        self.assertEqual(manifest['aOldest']['messageId'], '8000000000000000002')
        self.assertEqual(manifest['bOnly']['messageId'], '8000000000000000007')
        self.assertEqual(manifest['shared']['messageId'], '8000000000000000502')
        text = sql.decode()
        self.assertEqual(text.count('JSON_ARRAY(\'IN_APP\')'), 502)
        messages, recipients = text.split('INSERT INTO notify_message_recipient', 1)
        self.assertEqual(messages.count(',@stamp)'), 502)
        self.assertEqual(recipients.count(',101,@stamp)'), 503)
        self.assertEqual(recipients.count(',@old,101,@stamp)'), 20)
        self.assertEqual(recipients.count(',202,NULL,NULL,101,@stamp)'), 2)
        self.assertIn(',101,@old,NULL,101,@stamp)', recipients)
        self.assertIn('SET @old = DATE_SUB(@stamp, INTERVAL 1 DAY);', text)
        self.assertEqual(len(manifest['preReadMessageIds']), 20)
        self.assertTrue(text.endswith('COMMIT;'))

    def test_seed_rejects_same_or_invalid_identity(self):
        with self.assertRaises(RuntimeError):
            driver.seed_plan('0123456789abcdef', 101, 101)
        with self.assertRaises(RuntimeError):
            driver.seed_plan('bad', 101, 202)

    def test_volume_absence_requires_exact_lower_or_upper_error(self):
        name = 'a' * 64
        with patch.object(driver, 'run', return_value=types.SimpleNamespace(
                returncode=1, stderr=('Error response from daemon: No Such Volume: ' + name).encode())):
            self.assertTrue(driver.volume_absent(name))
        with patch.object(driver, 'run', return_value=types.SimpleNamespace(returncode=0, stderr=b'')):
            self.assertFalse(driver.volume_absent(name))
        with patch.object(driver, 'run', return_value=types.SimpleNamespace(returncode=1, stderr=b'permission denied')):
            with self.assertRaises(RuntimeError):
                driver.volume_absent(name)

    def test_cleanup_removes_only_discovered_owned_full_ids(self):
        ids = ['a' * 64, 'b' * 64]
        discoveries = [ids, []]
        calls = []
        with patch.object(driver, 'owned_ids', side_effect=discoveries), \
             patch.object(driver, 'assert_owned', side_effect=lambda cid, run: calls.append(('assert', cid))), \
             patch.object(driver, 'docker', side_effect=lambda *argv, **kwargs: calls.append(argv)):
            remaining, errors = driver.cleanup_containers('0123456789abcdef', ids)
        self.assertEqual((remaining, errors), ([], []))
        self.assertEqual([call[2] for call in calls if call[0] == 'rm'], ids[::-1])
        self.assertEqual(len([call for call in calls if call[0] == 'assert']), 2)

    def test_cleanup_missing_captured_full_id_blocks_acceptance(self):
        with patch.object(driver, 'owned_ids', side_effect=[[], []]):
            remaining, errors = driver.cleanup_containers('0123456789abcdef', ['a' * 64])
        self.assertEqual(remaining, [])
        self.assertIn('captured_container_missing_from_dual_label_discovery', errors)

    def test_docker_label_filter_is_exact(self):
        with patch.object(driver, 'docker', return_value='a' * 64) as call:
            self.assertEqual(driver.owned_ids('0123456789abcdef'), ['a' * 64])
        self.assertIn('label=namewta.test.owner=T-41-BROWSER', call.call_args.args)
        self.assertIn('label=namewta.test.run=0123456789abcdef', call.call_args.args)
        self.assertIn('--no-trunc', call.call_args.args)

    def test_reporter_requires_one_chromium_case_and_one_pass(self):
        with tempfile.TemporaryDirectory() as place:
            root = Path(place)
            path = root / driver.TEST_PATH
            path.parent.mkdir(parents=True)
            path.write_text('synthetic')
            report = {'suites': [{'file': str(path), 'specs': [{'title': driver.TEST_TITLE,
                      'tests': [{'projectName': 'chromium', 'results': [{'status': 'passed'}]}]}]}]}
            with patch.object(driver, 'ROOT', root):
                self.assertEqual(driver.playwright_identity(report)['attempts'], 1)
                report['suites'][0]['specs'][0]['tests'][0]['results'][0]['status'] = 'failed'
                with self.assertRaises(RuntimeError):
                    driver.playwright_identity(report)
                report['suites'][0]['specs'][0]['tests'][0]['results'] = [
                    {'status': 'passed'}, {'status': 'passed'}]
                with self.assertRaises(RuntimeError):
                    driver.playwright_identity(report)

    def test_failed_reporter_retains_only_exact_numeric_location(self):
        with tempfile.TemporaryDirectory() as place:
            root = Path(place)
            path = root / driver.TEST_PATH
            path.parent.mkdir(parents=True)
            path.write_text('first line\n  failed assertion here\n')
            sensitive = 'credential-canary-private'
            failed = {
                'stats': {'expected': 0, 'unexpected': 1, 'skipped': 0, 'flaky': 0},
                'suites': [{'file': str(path), 'specs': [{
                    'file': str(path), 'title': driver.TEST_TITLE, 'line': 2, 'column': 3,
                    'tests': [{'projectName': 'chromium', 'results': [{
                        'status': 'failed',
                        'errorLocation': {'file': str(path), 'line': 2, 'column': 3},
                        'errors': [{'message': sensitive, 'stack': sensitive}],
                        'attachments': [{'body': sensitive}]
                    }]}]
                }]}]
            }
            with patch.object(driver, 'ROOT', root):
                diagnostic = driver.playwright_diagnostic(failed)
                self.assertEqual(diagnostic['case_declaration'], {'line': 2, 'column': 3})
                self.assertEqual(diagnostic['assertion_location'], {'line': 2, 'column': 3})
                self.assertEqual(diagnostic['status'], 'failed')
                self.assertNotIn(sensitive, json.dumps(diagnostic))
                with self.assertRaises(RuntimeError):
                    driver.parse_counts(failed)
                result = failed['suites'][0]['specs'][0]['tests'][0]['results'][0]
                result['errorLocation'] = {'file': str(root / 'other.ts'), 'line': 2, 'column': 3}
                result['errors'] = [{'location': {'file': str(path), 'line': 99, 'column': 3},
                                     'message': sensitive}]
                self.assertIsNone(driver.playwright_diagnostic(failed)['assertion_location'])

    def test_reporter_counts_reject_skip_and_empty(self):
        good = {'stats': {'expected': 1, 'unexpected': 0, 'skipped': 0, 'flaky': 0}}
        self.assertEqual(driver.parse_counts(good)['expected'], 1)
        for bad in ({'stats': {'expected': 0}},
                    {'stats': {'expected': 1, 'skipped': 1}},
                    {'stats': {'expected': 1, 'flaky': 1}}):
            with self.assertRaises(RuntimeError):
                driver.parse_counts(bad)

    def test_source_identity_requires_clean_exact_head(self):
        sha = 'a' * 40
        with patch.object(driver, 'git', side_effect=[sha, '', 'b' * 40]):
            self.assertEqual(driver.source_identity(sha), {'head': sha, 'tree': 'b' * 40, 'clean': True})
        with patch.object(driver, 'git', side_effect=[sha, ' M frontend/file']):
            with self.assertRaises(RuntimeError):
                driver.source_identity(sha)

    def test_private_file_is_0600_and_exclusive(self):
        with tempfile.TemporaryDirectory() as place:
            path = Path(place) / 'owned.env'
            driver.private_bytes(path, b'owned')
            self.assertEqual(path.stat().st_mode & 0o777, 0o600)
            with self.assertRaises(FileExistsError):
                driver.private_bytes(path, b'overwrite')

    def test_isolated_overlay_uses_owned_addresses_and_no_push(self):
        config = driver.isolated_config(32801, 32802, 32803, 32804,
                                        'owned_app', 'privateA', 'privateB', Path('/tmp/owned'))
        self.assertEqual(config['server.address'], '127.0.0.1')
        self.assertIn('127.0.0.1:32801/wta-plus', config['spring.datasource.dynamic.datasource.master.url'])
        self.assertEqual(config['spring.datasource.dynamic.datasource.master.username'], 'owned_app')
        self.assertEqual(config['spring.data.redis.password'], 'privateB')
        self.assertEqual(config['namewta.sso.web-origin'], 'http://127.0.0.1:32804')
        self.assertEqual(config['web.cors.allowed-origins'], ['http://127.0.0.1:32804'])
        self.assertFalse(config['namewta.sso.cookie-secure'])
        self.assertFalse(config['message.enabled'])
        self.assertFalse(config['captcha.enable'])

    def test_owned_bcrypt_matches_application_variant_without_plaintext_sql(self):
        password = 'A' * 20 + '7' * 20
        password_hash = driver.owned_bcrypt_hash(password)
        self.assertTrue(password_hash.startswith('$2a$10$'))
        self.assertTrue(bcrypt.checkpw(password.encode(), password_hash.encode()))
        with patch.object(driver, 'mysql', return_value='1') as call:
            driver.rotate_owned_login('a' * 64, 101, password)
        sql = call.call_args.args[1]
        self.assertNotIn(password, sql)
        self.assertIn('CONVERT(0x', sql)
        self.assertIn('SELECT ROW_COUNT()', sql)

    def test_owned_b_role_requires_empty_prior_role_and_exact_four_menus(self):
        calls = []
        replies = iter(('0', '4', '0', '', '4'))
        with patch.object(driver, 'mysql', side_effect=lambda _cid, sql: (calls.append(sql), next(replies))[1]), \
             patch.object(driver.secrets, 'randbelow', return_value=123):
            role_id = driver.create_owned_b_role('a' * 64, '0123456789abcdef', 101, 202, 303)
        self.assertEqual(role_id, 8_200_000_000_000_000_123)
        self.assertIn('INSERT INTO sys_role', calls[3])
        self.assertIn('INSERT INTO sys_user_role', calls[3])
        self.assertIn('INSERT INTO sys_role_menu', calls[3])
        self.assertNotIn('1761300000000000004', calls[3])
        with patch.object(driver, 'mysql', return_value='1'):
            with self.assertRaises(RuntimeError):
                driver.create_owned_b_role('a' * 64, '0123456789abcdef', 101, 202, 303)

    def test_read_and_seen_preservation_snapshot_requires_20_real_rows(self):
        _, manifest = driver.seed_plan('0123456789abcdef', 101, 202,
                                       base=8_000_000_000_000_000_000)
        rows = '\n'.join(item + '\t2026-09-23 10:00:00' for item in manifest['preReadMessageIds'])
        b_rows = '\n'.join(item + '\tNULL\tNULL' for item in
                           (manifest['shared']['messageId'], manifest['bOnly']['messageId']))
        with patch.object(driver, 'mysql', side_effect=[rows, '2026-09-23 10:00:00', b_rows]):
            snapshot = driver.preservation_snapshot('a' * 64, manifest)
        self.assertEqual(len(snapshot['top20_read_rows']), 20)
        self.assertEqual(snapshot['oldest_seen_time'], '2026-09-23 10:00:00')
        self.assertEqual(len(snapshot['b_rows']), 2)
        with patch.object(driver, 'mysql', side_effect=[rows.replace('\t2026-09-23 10:00:00', '\tNULL', 1),
                                                        '2026-09-23 10:00:00', b_rows]):
            with self.assertRaises(RuntimeError):
                driver.preservation_snapshot('a' * 64, manifest)
        with patch.object(driver, 'mysql', side_effect=[rows, '2026-09-23 10:00:00',
                                                        b_rows.replace('\tNULL\tNULL', '\tNOW\tNULL', 1)]):
            with self.assertRaises(RuntimeError):
                driver.preservation_snapshot('a' * 64, manifest)

    def test_browser_config_disables_sensitive_recorders(self):
        good = "use: {trace: 'off', video: 'off', screenshot: 'off'}"
        driver.validate_browser_inputs('test("synthetic", () => {})', good)
        for bad in ("use: {trace: 'on', video: 'off', screenshot: 'off'}",
                    "use: {trace: 'off', video: 'on', screenshot: 'off'}",
                    good + '\nconst secret = context.storageState();',
                    good + '\nrecordHar: {}'):
            with self.assertRaises(RuntimeError):
                driver.validate_browser_inputs('test("synthetic", () => {})', bad)

    def test_package_proof_rejects_wrong_source_before_files(self):
        with self.assertRaises(RuntimeError):
            driver.validate_package_proof({'command': ['mvn', 'package']}, 'a' * 40, 'b' * 40,
                                          'c' * 64, 100)

    def test_static_script_has_no_private_helper_and_requires_pinned_images(self):
        source = SCRIPT.read_text()
        self.assertNotIn('/tmp/wta-t38', source)
        self.assertNotIn('importlib.util', source)
        self.assertIn('--pull=never', source)
        self.assertIn('T41_SEED_MANIFEST', source)
        self.assertIn('captured_anonymous_volume_names', source)
        self.assertIn('source_after_not_clean_exact_head', source)
        self.assertNotIn("'--requirepass'", source)


if __name__ == '__main__':
    unittest.main(verbosity=2)
