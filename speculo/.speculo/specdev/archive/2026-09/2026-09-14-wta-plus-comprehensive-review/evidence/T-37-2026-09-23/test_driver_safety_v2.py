"""Synthetic checks only: every Docker/Maven boundary is mocked or parser-only."""

import importlib.util
from pathlib import Path
import subprocess
import tempfile
import time
import unittest
from unittest.mock import Mock, patch

DRIVER = Path('/tmp/wta-t37/run-notify-retry-integration-v2.py')
spec = importlib.util.spec_from_file_location('t37_driver', DRIVER)
driver = importlib.util.module_from_spec(spec)
spec.loader.exec_module(driver)


class DriverSafetyTest(unittest.TestCase):
    def test_failed_run_captures_volume_names_before_cleanup(self):
        self.assertIn("report['owned']['captured_anonymous_volume_names'] = list(volumes)", DRIVER.read_text())

    def test_owned_mysql_labels_loopback_and_trigger_option(self):
        args = driver.mysql_run_args('run123', Path('/tmp/owned.env'))
        self.assertIn('namewta.test.owner=T-37', args)
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
                '--filter', 'label=namewta.test.owner=T-37', '--filter', 'label=namewta.test.run=run123'))

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

    def test_both_exact_fresh_classes_positive_zero_skip(self):
        with tempfile.TemporaryDirectory() as directory:
            reports = []
            started = time.time_ns()
            for klass in (driver.TEST_CLASS, driver.REDIS_CLASS):
                path = Path(directory) / ('TEST-' + klass + '.xml')
                path.write_text(f'<testsuite name="{klass}" tests="2" failures="0" errors="0" skipped="0"><testcase name="ok"/></testsuite>')
                self.assertEqual(driver.xml_verdict_for(klass, path, started)['tests'], 2)
                reports.append(path)
            reports[1].write_text(f'<testsuite name="{driver.REDIS_CLASS}" tests="2" failures="0" errors="0" skipped="1"/>')
            with self.assertRaisesRegex(RuntimeError, 'zero tests'):
                driver.xml_verdict_for(driver.REDIS_CLASS, reports[1], started)
            with self.assertRaisesRegex(RuntimeError, 'absent or stale'):
                driver.xml_counts_for(driver.TEST_CLASS, reports[0], time.time_ns() + 1_000_000_000)

    def test_exited_leader_still_checks_process_group(self):
        proc = Mock(pid=1234)
        with patch.object(driver, 'live_group_members', side_effect=[[4321], []]), \
             patch.object(driver.os, 'killpg') as killpg:
            self.assertEqual(driver.stop_group(proc), [])
        killpg.assert_called_once_with(1234, driver.signal.SIGTERM)

    def test_password_only_child_environment(self):
        self.assertEqual(driver.safe_env(T35_MYSQL_PASSWORD='private')['T35_MYSQL_PASSWORD'], 'private')
        self.assertNotIn('T35_MYSQL_PASSWORD', driver.safe_env())
        self.assertEqual(driver.TEST_CLASS, 'org.namewta.test.notify.NotifySmsDispatchIntegrationTest')
        self.assertEqual(driver.REDIS_CLASS, 'org.namewta.test.notify.idempotency.RedisNotifyIdempotencyStoreIntegrationTest')


if __name__ == '__main__':
    unittest.main()
