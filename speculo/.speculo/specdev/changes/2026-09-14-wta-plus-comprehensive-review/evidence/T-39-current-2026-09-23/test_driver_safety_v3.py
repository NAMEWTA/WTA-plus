"""Synthetic checks only: every Docker/Maven boundary is mocked or parser-only."""

import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest
from unittest.mock import Mock, patch

DRIVER = Path('/tmp/wta-t39/run-notify-deadline-integration-v3.py')
spec = importlib.util.spec_from_file_location('t39_driver_v3', DRIVER)
driver = importlib.util.module_from_spec(spec)
spec.loader.exec_module(driver)


class DriverSafetyTest(unittest.TestCase):
    def test_failed_run_captures_volume_names_before_cleanup(self):
        self.assertIn("report['owned']['captured_anonymous_volume_names'] = list(volumes)", DRIVER.read_text())

    def test_owned_mysql_labels_loopback_and_trigger_option(self):
        args = driver.mysql_run_args('run123', Path('/tmp/owned.env'))
        self.assertIn('namewta.test.owner=T-39', args)
        self.assertIn('namewta.test.run=run123', args)
        self.assertIn('127.0.0.1::3306', args)
        self.assertIn('--log-bin-trust-function-creators=1', args)
        self.assertNotIn('--privileged', args)

    def test_full_id_and_dual_label_discovery(self):
        full = 'a' * 64
        self.assertEqual(driver.full_id(full), full)
        with self.assertRaisesRegex(RuntimeError, 'full container ID'):
            driver.full_id(full[:12])
        with patch.object(driver, 'docker', return_value=full) as docker:
            self.assertEqual(driver.owned_ids('run123'), [full])
            self.assertEqual(docker.call_args.args, ('ps', '-aq', '--no-trunc',
                '--filter', 'label=namewta.test.owner=T-39', '--filter', 'label=namewta.test.run=run123'))

    def test_only_verified_full_id_is_removed(self):
        full = 'b' * 64
        with patch.object(driver, 'owned_ids', side_effect=[[full], []]), \
             patch.object(driver, 'assert_owned') as owned, \
             patch.object(driver, 'docker', return_value='') as docker:
            self.assertEqual(driver.cleanup_containers('run123', [full]), ([], []))
        owned.assert_called_once_with(full, 'run123')
        docker.assert_called_once_with('rm', '-fv', full, timeout=60)
        with patch.object(driver, 'owned_ids', side_effect=[[full], [full]]), \
             patch.object(driver, 'assert_owned', side_effect=RuntimeError('foreign')), \
             patch.object(driver, 'docker') as docker:
            remaining, errors = driver.cleanup_containers('run123', [full])
        self.assertEqual(remaining, [full])
        self.assertIn('owned_container_remove_failed', errors)
        docker.assert_not_called()

    def test_volume_capture_and_absence_verdict(self):
        name = 'c' * 64
        with patch.object(driver, 'docker', return_value='[{"Type":"volume","Name":"' + name + '"}]'):
            self.assertEqual(driver.volume_names('container'), [name])
        with patch.object(driver, 'docker', return_value='[{"Type":"volume","Name":"shared"}]'):
            with self.assertRaisesRegex(RuntimeError, 'non-anonymous'):
                driver.volume_names('container')
        with patch.object(driver, 'run', return_value=subprocess.CompletedProcess([], 1, b'', ('Error response from daemon: get ' + name + ': No such volume').encode())):
            self.assertTrue(driver.volume_absent(name))
        with patch.object(driver, 'run', return_value=subprocess.CompletedProcess([], 1, b'[]', ('Error response from daemon: get ' + name + ': no such volume').encode())):
            self.assertTrue(driver.volume_absent(name))
        with patch.object(driver, 'run', return_value=subprocess.CompletedProcess([], 1, b'[]', b'Error response from daemon: get other: no such volume')):
            with self.assertRaisesRegex(RuntimeError, 'could not be verified'):
                driver.volume_absent(name)
        with patch.object(driver, 'run', return_value=subprocess.CompletedProcess([], 1, b'[]', b'permission denied')):
            with self.assertRaisesRegex(RuntimeError, 'could not be verified'):
                driver.volume_absent(name)
        with patch.object(driver, 'run', return_value=subprocess.CompletedProcess([], 0, b'[]', b'')):
            self.assertFalse(driver.volume_absent(name))

    def test_eight_exact_fresh_classes_positive_zero_skip(self):
        with tempfile.TemporaryDirectory() as directory:
            started = time.time_ns()
            paths = []
            for klass, _ in driver.SUITES:
                path = Path(directory) / ('TEST-' + klass + '.xml')
                path.write_text(f'<testsuite name="{klass}" tests="2" failures="0" errors="0" skipped="0"><testcase name="retrySameIntent"/><testcase name="retryConflict"/></testsuite>')
                self.assertEqual(driver.xml_verdict_for(klass, path, started)['tests'], 2)
                self.assertEqual(driver.xml_methods_for(path), ['retrySameIntent', 'retryConflict'])
                paths.append(path)
            paths[3].write_text(f'<testsuite name="{driver.REDIS_CLASS}" tests="2" failures="0" errors="0" skipped="1"/>')
            with self.assertRaisesRegex(RuntimeError, 'zero tests'):
                driver.xml_verdict_for(driver.REDIS_CLASS, paths[3], started)
            with self.assertRaisesRegex(RuntimeError, 'absent or stale'):
                driver.xml_counts_for(driver.TEST_CLASS, paths[0], time.time_ns() + 1_000_000_000)

    def test_xml_owned_secret_redacted_and_gate_reported(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'fresh.xml'
            started = time.time_ns()
            path.write_text('<testsuite>owned-secret</testsuite>')
            self.assertTrue(driver.redact_fresh_xml(path, started, ('owned-secret',)))
            self.assertNotIn('owned-secret', path.read_text())
            self.assertFalse(driver.redact_fresh_xml(path, started, ('owned-secret',)))

    def test_source_and_eight_class_selector(self):
        source = DRIVER.read_text()
        self.assertIn("'NotifyWakeIntegrationTest,RedisUtilsDeadlineIntegrationTest,'", source)
        self.assertIn("'-Dnotify.deadline.integration=true'", source)
        self.assertIn("'-Dprofile.notify.integration=true'", source)
        self.assertIn("'-Dnotify.wake.integration=true'", source)
        self.assertIn("'-Dnotify.deadline.redis.integration=true'", source)
        self.assertIn("'-DforkCount=1', '-DreuseForks=false'", source)
        self.assertIn('backend/wta-common/wta-common-redis/target/surefire-reports/TEST-', source)
        self.assertLess(source.index('redis_text = REDIS_SOURCE.read_text()'),
                        source.index('mysql_id = full_id(docker(*mysql_run_args'))
        self.assertIn('notify\\.deadline\\.redis\\.integration', source)
        self.assertIn('NotifyAtomicResultIntegrationTest,NotifyManualRetryIntegrationTest,', source)
        self.assertIn('NotifySmsDispatchIntegrationTest,RedisNotifyIdempotencyStoreIntegrationTest', source)
        for flag in ('notify.atomic.integration', 'notify.manual.retry.integration', 'notify.sms.integration'):
            self.assertIn("'-D" + flag + "=true'", source)
        self.assertEqual(len(driver.SUITES), 8)
        self.assertEqual(len({name for name, _ in driver.SUITES}), 8)
        self.assertEqual(len({path for _, path in driver.SUITES}), 8)
        self.assertLess(source.index('idempotency_text = IDEMPOTENCY_SOURCE.read_text()'),
                        source.index('mysql_id = full_id(docker(*mysql_run_args'))

    def test_common_report_is_required_from_its_own_module(self):
        self.assertEqual(driver.REDIS_REPORT, driver.ROOT / (
            'backend/wta-common/wta-common-redis/target/surefire-reports/TEST-' + driver.REDIS_CLASS + '.xml'))
        with tempfile.TemporaryDirectory() as directory:
            stale = Path(directory) / 'stale-common.xml'
            with self.assertRaisesRegex(RuntimeError, 'absent or stale'):
                driver.xml_counts_for(driver.REDIS_CLASS, stale, time.time_ns())

    def test_cli_requires_opt_in_and_exact_sha_without_touching_services(self):
        for args in ((), ('--execute',), ('--execute', '--expected-head', 'bad')):
            result = subprocess.run((sys.executable, str(DRIVER), *args),
                                    capture_output=True, text=True, timeout=5, check=False)
            self.assertEqual(result.returncode, 2)
            self.assertIn('no launch without', result.stderr)

    def test_source_identity_checks_both_sha_and_clean_tree(self):
        expected = 'e' * 40
        with patch.object(driver, 'git', side_effect=[expected, '', 'f' * 40]):
            self.assertEqual(driver.source_identity(expected),
                             {'head': expected, 'tree': 'f' * 40, 'clean': True})
        with patch.object(driver, 'git', side_effect=[expected, ' M changed.java']):
            with self.assertRaisesRegex(RuntimeError, 'exact expected clean HEAD'):
                driver.source_identity(expected)

    def test_exited_leader_still_checks_process_group(self):
        proc = Mock(pid=1234)
        with patch.object(driver, 'live_group_members', side_effect=[[4321], []]), \
             patch.object(driver.os, 'killpg') as killpg:
            self.assertEqual(driver.stop_group(proc), [])
        killpg.assert_called_once_with(1234, driver.signal.SIGTERM)

    def test_password_only_child_environment(self):
        self.assertEqual(driver.safe_env(T39_MYSQL_PASSWORD='private')['T39_MYSQL_PASSWORD'], 'private')
        self.assertNotIn('T39_MYSQL_PASSWORD', driver.safe_env())
        self.assertEqual(driver.TEST_CLASS, 'org.namewta.test.notify.NotifyDeadlineIntegrationTest')
        self.assertEqual(driver.PROFILE_CLASS, 'org.namewta.test.notify.EnterpriseQueuedNotificationIntegrationTest')
        self.assertEqual(driver.WAKE_CLASS, 'org.namewta.test.notify.NotifyWakeIntegrationTest')
        self.assertEqual(driver.REDIS_CLASS, 'org.namewta.common.redis.utils.RedisUtilsDeadlineIntegrationTest')
        self.assertEqual(driver.ATOMIC_CLASS, 'org.namewta.test.notify.NotifyAtomicResultIntegrationTest')
        self.assertEqual(driver.MANUAL_CLASS, 'org.namewta.test.notify.NotifyManualRetryIntegrationTest')
        self.assertEqual(driver.SMS_CLASS, 'org.namewta.test.notify.NotifySmsDispatchIntegrationTest')
        self.assertEqual(driver.IDEMPOTENCY_CLASS, 'org.namewta.test.notify.idempotency.RedisNotifyIdempotencyStoreIntegrationTest')
        source = DRIVER.read_text()
        for name in ('T39_MYSQL_PASSWORD', 'T36_MYSQL_PASSWORD', 'T38_MYSQL_PASSWORD', 'T35_MYSQL_PASSWORD'):
            self.assertIn(name + '=app_password', source)


if __name__ == '__main__':
    unittest.main()
