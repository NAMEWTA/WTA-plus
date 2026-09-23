#!/usr/bin/env python3
"""T40 runner safety and fixture tests only; never starts a service or Docker."""

import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import types
import unittest
from unittest.mock import patch

import bcrypt


SCRIPT = Path(__file__).with_name('run-notice-retraction-real.py')
spec = importlib.util.spec_from_file_location('t40_browser', SCRIPT)
driver = importlib.util.module_from_spec(spec)
spec.loader.exec_module(driver)


class T40OfflineSafety(unittest.TestCase):
    def test_explicit_clean_source_and_package_are_required_before_launch(self):
        result = subprocess.run([sys.executable, str(SCRIPT)], capture_output=True, text=True)
        self.assertEqual(result.returncode, 2)
        self.assertIn('no launch without', result.stderr)
        with patch.object(driver, 'git', side_effect=['a' * 40, ' M frontend/e2e/spec']):
            with self.assertRaises(RuntimeError):
                driver.source_identity('a' * 40)
        with self.assertRaises(RuntimeError):
            driver.validate_package_proof({'command': ['mvn', 'package']},
                                          'a' * 40, 'b' * 40, 'c' * 64, 100)

    def test_real_v1_is_not_in_synthetic_seed_and_legacy_stays_top_ten(self):
        sql, manifest = driver.seed_plan('0123456789abcdef', 101, 202, 701, 601,
                                         'real title', 'real complete body',
                                         base=8_000_000_000_000_000_000)
        text = sql.decode()
        self.assertEqual(manifest['v1']['messageId'], '701')
        self.assertEqual(manifest['v1']['path'], '/notify/inbox?messageId=701')
        self.assertEqual(manifest['synthetic_filler_count'], 20)
        self.assertEqual(manifest['legacy']['messageId'], '8000000000000000021')
        self.assertEqual(manifest['absentId'], '8000000000000000023')
        self.assertNotIn('(701,', text)
        self.assertEqual(text.count("JSON_ARRAY('IN_APP')"), 22)
        self.assertEqual(text.count(',202,NULL,NULL,101,@stamp)'), 1)
        self.assertIn(driver.hexsql('/notify/notice?noticeId=' + manifest['legacy']['noticeId']), text)
        self.assertTrue(text.endswith('COMMIT;'))

    def test_seed_rejects_aliases_and_duplicate_user(self):
        with self.assertRaises(RuntimeError):
            driver.seed_plan('bad', 101, 202, 701, 601, 'a', 'b')
        with self.assertRaises(RuntimeError):
            driver.seed_plan('0123456789abcdef', 101, 101, 701, 601, 'a', 'b')

    def test_seed_checks_exact_real_and_absent_rows(self):
        _, manifest = driver.seed_plan('0123456789abcdef', 101, 202, 701, 601,
                                       'a', 'b', base=8_000_000_000_000_000_000)
        top = '\n'.join(str(8_000_000_000_000_000_000 + index)
                        for index in range(21, 1, -1))
        with patch.object(driver, 'mysql', side_effect=[top, '22\t1\t22\t1\t0']):
            self.assertTrue(driver.verify_seed('a' * 64, manifest, stage='counts_seed')['v1_off_page_one'])
        with patch.object(driver, 'mysql', side_effect=[top, '22\t1\t22\t1\t1']):
            with self.assertRaises(RuntimeError):
                driver.verify_seed('a' * 64, manifest, stage='counts_seed')

    def test_notice_version_fact_requires_exact_snapshot_identity(self):
        valid = json.dumps({'noticeId': 601, 'snapshotId': 901, 'version': 1, 'retracted': False})
        with patch.object(driver, 'mysql', side_effect=['901', valid]):
            self.assertFalse(driver.notice_version_fact('a' * 64, 601, 701,
                                                        stage='real_identity')['retracted'])
        with patch.object(driver, 'mysql', side_effect=['901', valid.replace('901', '902')]):
            with self.assertRaises(RuntimeError):
                driver.notice_version_fact('a' * 64, 601, 701, stage='real_identity')

    def test_two_exact_chrome_cases_and_zero_skip_are_mandatory(self):
        with tempfile.TemporaryDirectory() as place:
            root = Path(place)
            path = root / driver.TEST_PATH
            path.parent.mkdir(parents=True)
            path.write_text('first line\nsecond line\n')
            report = {'stats': {'expected': 2, 'unexpected': 0, 'skipped': 0, 'flaky': 0},
                      'suites': [{'file': str(path), 'specs': [
                          {'title': title, 'line': 1, 'column': 1,
                           'tests': [{'projectName': 'chromium', 'results': [{'status': 'passed'}]}]}
                          for title in driver.TEST_TITLES]}]}
            with patch.object(driver, 'ROOT', root):
                self.assertEqual(driver.playwright_identity(report)['attempts'], 2)
                self.assertEqual(driver.parse_counts(report)['expected'], 2)
                report['suites'][0]['specs'][1]['title'] = driver.TEST_TITLES[0]
                with self.assertRaises(RuntimeError):
                    driver.playwright_identity(report)
                report['suites'][0]['specs'][1]['title'] = driver.TEST_TITLES[1]
                report['suites'][0]['specs'][1]['tests'][0]['results'].append({'status': 'passed'})
                with self.assertRaises(RuntimeError):
                    driver.playwright_identity(report)
                report['stats']['skipped'] = 1
                with self.assertRaises(RuntimeError):
                    driver.parse_counts(report)

    def test_failed_reporter_keeps_only_exact_numeric_location(self):
        with tempfile.TemporaryDirectory() as place:
            root = Path(place)
            path = root / driver.TEST_PATH
            path.parent.mkdir(parents=True)
            path.write_text('a\n  b\n')
            canary = 'credential-canary-private'
            case = {'title': driver.TEST_TITLES[0], 'line': 2, 'column': 3,
                    'tests': [{'projectName': 'chromium', 'results': [{
                        'status': 'failed', 'errorLocation': {'file': str(path), 'line': 2, 'column': 3},
                        'errors': [{'message': canary, 'stack': canary}],
                        'attachments': [{'body': canary}]}]}]}
            report = {'suites': [{'file': str(path), 'specs': [case]}]}
            with patch.object(driver, 'ROOT', root):
                diagnostic = driver.playwright_diagnostic(report)
                self.assertEqual(diagnostic[0]['assertion_location'], {'line': 2, 'column': 3})
                self.assertNotIn(canary, json.dumps(diagnostic))
                case['tests'][0]['results'][0]['errorLocation']['file'] = str(root / 'other.ts')
                self.assertIsNone(driver.playwright_diagnostic(report)[0]['assertion_location'])

    def test_owned_resource_labels_and_cleanup_do_not_touch_other_containers(self):
        ids = ['a' * 64, 'b' * 64]
        with patch.object(driver, 'docker', return_value='a' * 64) as query:
            self.assertEqual(driver.owned_ids('0123456789abcdef'), ['a' * 64])
        self.assertIn('label=namewta.test.owner=T-40-BROWSER', query.call_args.args)
        self.assertIn('label=namewta.test.run=0123456789abcdef', query.call_args.args)
        with patch.object(driver, 'owned_ids', side_effect=[ids, []]), \
             patch.object(driver, 'assert_owned'), patch.object(driver, 'docker') as docker:
            self.assertEqual(driver.cleanup_containers('0123456789abcdef', ids), ([], []))
        self.assertEqual([call.args[2] for call in docker.call_args_list if call.args[0] == 'rm'], ids[::-1])
        with patch.object(driver, 'owned_ids', side_effect=[[], []]):
            self.assertIn('captured_container_missing_from_dual_label_discovery',
                          driver.cleanup_containers('0123456789abcdef', [ids[0]])[1])

    def test_owned_recipient_roles_have_only_inbox_menus(self):
        replies = iter(('0', '4', '0', '', '4', '0'))
        calls = []
        with patch.object(driver, 'mysql', side_effect=lambda _cid, sql, **_kw: (calls.append(sql), next(replies))[1]), \
             patch.object(driver.secrets, 'randbelow', return_value=123):
            driver.create_owned_recipient_role('a' * 64, '0123456789abcdef', 1, 101, 303, 'a')
        self.assertIn('INSERT INTO sys_user_role', calls[3])
        self.assertIn('2100600000000000042', calls[3])
        self.assertNotIn('2100600000000000033', calls[3])
        self.assertIn("m.perms LIKE 'notify:notice:%'", calls[-1])
        with patch.object(driver, 'mysql', return_value='1'):
            with self.assertRaises(RuntimeError):
                driver.create_owned_recipient_role('a' * 64, '0123456789abcdef', 1, 101, 303, 'a')

    def test_control_http_keeps_password_and_bearer_out_of_errors(self):
        canary = 'credential-canary-private'
        with patch.object(driver.http.client, 'HTTPConnection', side_effect=OSError(canary)):
            try:
                driver.control_request(32801, 'login_control', '/auth/login',
                                       body={'password': canary})
            except driver.OwnedControlFailure as error:
                failure = driver.safe_failure(error, 'login_control')
                self.assertNotIn(canary, str(error))
            else:
                self.fail('control request must preserve its fixed transport failure')
        self.assertNotIn(canary, json.dumps(driver.safe_failure(RuntimeError(canary))))
        self.assertEqual(driver.safe_failure(RuntimeError(canary))['error_type'], 'RuntimeError')
        self.assertEqual(failure['control_failure'], {
            'stage': 'login_control', 'kind': 'transport',
            'http_status': None, 'business_code': None})
        self.assertIsInstance(failure['runner_line'], int)
        self.assertNotIn(canary, json.dumps(failure))
        with self.assertRaises(RuntimeError):
            driver.control_request(32801, 'unlisted', '/auth/login')

    def test_control_login_records_only_fixed_stage_and_numeric_http_business_codes(self):
        canary = 'credential-canary-private'
        token = 'token-' + canary
        replies = ((401, {'code': 403, 'msg': canary, 'data': {'secret': token}},
                    'login_rejected', 401, 403),
                   (200, {'code': 401, 'msg': canary, 'data': {'secret': token}},
                    'login_rejected', 200, 401),
                   (200, {'code': 200, 'msg': canary, 'data': {}},
                    'token_missing', 200, 200),
                   (200, {'code': True, 'msg': canary},
                    'login_rejected', 200, None))
        for status, body, kind, expected_status, expected_code in replies:
            with self.subTest(kind=kind, status=status, code=body['code']):
                with patch.object(driver, 'control_request', return_value=(status, body)):
                    with self.assertRaises(driver.OwnedControlFailure) as raised:
                        driver.control_login(32801, 'WTA', canary, 'login_control')
                summary = driver.safe_failure(raised.exception, 'login_control')
                self.assertEqual(summary['control_failure'], {
                    'stage': 'login_control', 'kind': kind,
                    'http_status': expected_status, 'business_code': expected_code})
                self.assertNotIn(canary, json.dumps(summary))
        with patch.object(driver, 'control_request', return_value=(200, {
                'code': 200, 'data': {'access_token': token}})):
            self.assertEqual(driver.control_login(32801, 'WTA', canary, 'login_control'), token)
        with self.assertRaises(RuntimeError):
            driver.control_login(32801, 'WTA', canary, canary)
        self.assertEqual(driver.OwnedControlFailure(canary, canary, True, 10**30).safe_details(), {
            'stage': None, 'kind': 'transport', 'http_status': None, 'business_code': None})

    def test_control_response_parse_failures_and_backend_exit_keep_no_payload(self):
        canary = 'credential-canary-private'

        def connection(status, payload):
            response = types.SimpleNamespace(status=status, read=lambda _size: payload)
            return types.SimpleNamespace(request=lambda *_args, **_kwargs: None,
                                         getresponse=lambda: response, close=lambda: None)

        for status, payload, kind in ((200, b'{}' + canary.encode() * 100_000,
                                      'response_too_large'),
                                      (401, canary.encode(), 'invalid_json'),
                                      (200, b'[]', 'invalid_shape')):
            with self.subTest(kind=kind), \
                 patch.object(driver.http.client, 'HTTPConnection', return_value=connection(status, payload)):
                with self.assertRaises(driver.OwnedControlFailure) as raised:
                    driver.control_request(32801, 'login_a', '/auth/login', body={'password': canary})
                summary = driver.safe_failure(raised.exception, 'login_a')
                self.assertEqual(summary['control_failure']['kind'], kind)
                self.assertEqual(summary['control_failure']['http_status'], status)
                self.assertNotIn(canary, json.dumps(summary))
        with patch.object(driver.http.client, 'HTTPConnection', return_value=connection(
                200, b'{"code":200,"data":{"access_token":"owned-token-123456"}}')):
            status, body = driver.control_request(32801, 'login_b', '/auth/login')
        self.assertEqual((status, body['code']), (200, 200))
        self.assertEqual(driver.failed_backend_exit_code([
            ('backend', types.SimpleNamespace(poll=lambda: 7))]), 7)
        self.assertIsNone(driver.failed_backend_exit_code([
            ('backend', types.SimpleNamespace(poll=lambda: None))]))
        exited = types.SimpleNamespace(poll=lambda: 7)
        with self.assertRaises(RuntimeError) as probe:
            driver.wait_http(32801, '/auth/code', exited, 1)
        self.assertIn('exited before readiness', str(probe.exception))
        self.assertEqual(driver.failed_backend_exit_code([('backend', exited)]), 7)
        self.assertIsNone(driver.safe_failure(RuntimeError(canary), canary)['phase'])
        self.assertIsNone(driver.safe_failure(RuntimeError(canary))['runner_line'])

    def test_sql_error_retains_only_allowlisted_stage_and_numeric_diagnostics(self):
        canary = 'credential-canary-private'
        failure = types.SimpleNamespace(returncode=1, stdout=b'', stderr=(
            'ERROR 1267 (HY000): Illegal mix near ' + canary).encode())
        with patch.object(driver, 'run', return_value=failure):
            with self.assertRaises(driver.OwnedSqlError) as raised:
                driver.mysql('a' * 64, 'SELECT ' + canary, stage='notice_lookup')
        self.assertEqual(raised.exception.safe_details(), {
            'stage': 'notice_lookup', 'exit_code': 1,
            'mysql_error_number': 1267, 'sqlstate': 'HY000'})
        self.assertNotIn(canary, str(raised.exception) + json.dumps(raised.exception.safe_details()))
        with patch.object(driver, 'run', return_value=types.SimpleNamespace(
                returncode=0, stdout=b'101\t202\n', stderr=b'')):
            self.assertEqual(driver.mysql('a' * 64, 'SELECT 1', stage='counts_before'), '101\t202')
        timeout = subprocess.TimeoutExpired(('docker', 'exec'), 180,
                                            output=canary.encode(), stderr=canary.encode())
        with patch.object(driver, 'run', side_effect=timeout):
            with self.assertRaises(driver.OwnedSqlError) as timed_out:
                driver.mysql('a' * 64, 'SELECT 1', stage='counts_before')
        self.assertEqual(timed_out.exception.safe_details(), {
            'stage': 'counts_before', 'exit_code': None,
            'mysql_error_number': None, 'sqlstate': None})
        self.assertNotIn(canary, str(timed_out.exception))
        with self.assertRaises(ValueError):
            driver.mysql('a' * 64, 'SELECT 1', stage=canary)

    def test_private_file_mode_and_exclusive_creation(self):
        with tempfile.TemporaryDirectory() as place:
            path = Path(place) / 'owned.env'
            driver.private_bytes(path, b'owned')
            self.assertEqual(path.stat().st_mode & 0o777, 0o600)
            with self.assertRaises(FileExistsError):
                driver.private_bytes(path, b'overwrite')

    def test_bcrypt_login_password_is_private_and_within_http_limit(self):
        password = driver.random_login_password()
        self.assertRegex(password, r'^[A-Za-z0-9]{24}$')
        password_hash = driver.owned_bcrypt_hash(password)
        self.assertTrue(bcrypt.checkpw(password.encode(), password_hash.encode()))
        with patch.object(driver, 'mysql', return_value='1') as mysql:
            driver.rotate_owned_login('a' * 64, 101, password)
        self.assertNotIn(password, mysql.call_args.args[1])
        self.assertIn('CONVERT(0x', mysql.call_args.args[1])

    def test_isolated_overlay_still_runs_real_worker_without_realtime(self):
        config = driver.isolated_config(32801, 32802, 32803, 32804,
                                        'owned_app', 'privateA', 'privateB', Path('/tmp/owned'))
        self.assertEqual(config['server.address'], '127.0.0.1')
        self.assertFalse(config['message.enabled'])
        self.assertEqual(config['notify.outbox.poll-delay-ms'], 1000)
        self.assertEqual(config['spring.data.redis.password'], 'privateB')
        self.assertEqual(config['web.cors.allowed-origins'], ['http://127.0.0.1:32804'])

    def test_browser_config_rejects_credential_artifacts(self):
        good = "use: {trace: 'off', video: 'off', screenshot: 'off'}"
        driver.validate_browser_inputs('test("synthetic", () => {})', good)
        for bad in ("use: {trace: 'on', video: 'off', screenshot: 'off'}",
                    good + '\ncontext.storageState()', good + '\nrecordHar: {}'):
            with self.assertRaises(RuntimeError):
                driver.validate_browser_inputs('test("synthetic", () => {})', bad)


if __name__ == '__main__':
    unittest.main()
