"""Synthetic-only checks. No Docker, Maven, HTTP or database service is started."""

import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest
from unittest.mock import Mock, patch

DRIVER = Path('/tmp/wta-t02/run-log-redaction-integration.py')
spec = importlib.util.spec_from_file_location('t02_driver', DRIVER)
driver = importlib.util.module_from_spec(spec)
spec.loader.exec_module(driver)


class DriverSafetyTest(unittest.TestCase):
    def test_requires_explicit_launch_and_full_clean_head_before_services(self):
        result = subprocess.run([sys.executable, str(DRIVER)], capture_output=True, text=True)
        self.assertEqual(result.returncode, 2)
        self.assertIn('no launch without --execute', result.stderr)
        with patch.object(driver, 'git', side_effect=['f' * 40, ' M backend/example.java']):
            with self.assertRaisesRegex(RuntimeError, 'exact expected clean'):
                driver.source_identity('f' * 40)

    def test_baseline_green_cannot_claim_ticket_canary(self):
        self.assertFalse(driver.ticket_acceptance(0, None))
        self.assertFalse(driver.ticket_acceptance(1, 'newTokenPathMethod'))
        self.assertTrue(driver.ticket_acceptance(0, 'newTokenPathMethod'))
        self.assertEqual(len(driver.TESTS), 7)
        self.assertIn(driver.HTTP_TEST, driver.TESTS)

    def test_owned_loopback_mysql_flags_and_full_ids(self):
        args = driver.mysql_run_args('run123', Path('/tmp/owned.env'))
        self.assertIn('namewta.test.owner=T-02', args)
        self.assertIn('namewta.test.run=run123', args)
        self.assertIn('127.0.0.1::3306', args)
        self.assertNotIn('--privileged', args)
        full = 'a' * 64
        self.assertEqual(driver.full_id(full), full)
        with self.assertRaisesRegex(RuntimeError, 'full container ID'):
            driver.full_id(full[:12])
        with patch.object(driver, 'docker', return_value=full) as docker:
            self.assertEqual(driver.owned_ids('run123'), [full])
            self.assertEqual(docker.call_args.args, ('ps', '-aq', '--no-trunc',
                '--filter', 'label=namewta.test.owner=T-02', '--filter', 'label=namewta.test.run=run123'))

    def test_failure_cleanup_only_verifies_and_removes_owned_full_ids(self):
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
        with patch.object(driver, 'owned_ids', side_effect=[[], []]):
            _, errors = driver.cleanup_containers('run123', [full])
        self.assertIn('captured_container_missing_from_dual_label_discovery', errors)

    def test_anonymous_volume_must_be_absent(self):
        name = 'c' * 64
        with patch.object(driver, 'docker', return_value='[{"Type":"volume","Name":"' + name + '"}]'):
            self.assertEqual(driver.volume_names('container'), [name])
        with patch.object(driver, 'docker', return_value='[{"Type":"volume","Name":"shared"}]'):
            with self.assertRaisesRegex(RuntimeError, 'non-anonymous'):
                driver.volume_names('container')
        with patch.object(driver, 'run', return_value=subprocess.CompletedProcess([], 1, b'', b'No such volume')):
            self.assertTrue(driver.volume_absent(name))
        with patch.object(driver, 'run', return_value=subprocess.CompletedProcess([], 0, b'[]', b'')):
            self.assertFalse(driver.volume_absent(name))

    def test_seven_exact_reports_must_be_fresh_positive_and_zero_skip(self):
        with tempfile.TemporaryDirectory() as directory:
            started = time.time_ns()
            paths = {}
            for klass in driver.TESTS:
                path = Path(directory) / ('TEST-' + klass + '.xml')
                path.write_text(f'<testsuite name="{klass}" tests="2" failures="0" errors="0" skipped="0"><testcase name="case"/></testsuite>')
                self.assertEqual(driver.xml_counts_for(klass, path, started)['tests'], 2)
                paths[klass] = path
            target = paths[driver.HTTP_TEST]
            target.write_text(f'<testsuite name="{driver.HTTP_TEST}" tests="2" failures="0" errors="0" skipped="1"/>')
            with self.assertRaisesRegex(RuntimeError, 'zero tests'):
                driver.xml_verdict_for(driver.HTTP_TEST, target, started)
            with self.assertRaisesRegex(RuntimeError, 'absent or stale'):
                driver.xml_counts_for(next(iter(paths)), next(iter(paths.values())), time.time_ns() + 1_000_000_000)

    def test_exited_maven_leader_does_not_skip_group_cleanup(self):
        proc = Mock(pid=1234)
        with patch.object(driver, 'live_group_members', side_effect=[[4321], []]), \
             patch.object(driver.os, 'killpg') as killpg:
            self.assertEqual(driver.stop_group(proc), [])
        killpg.assert_called_once_with(1234, driver.signal.SIGTERM)

    def test_private_log_redacts_passwords_and_known_canary(self):
        with tempfile.TemporaryDirectory() as directory:
            raw = Path(directory) / 'raw'
            clean = Path(directory) / 'clean'
            raw.write_text('root-secret\ncredential-canary-1234-abc\n')
            driver.sanitize_log(raw, clean, ('root-secret',))
            self.assertFalse(raw.exists())
            content = clean.read_text()
            self.assertNotIn('root-secret', content)
            self.assertNotIn('credential-canary-1234-abc', content)
            self.assertIn('[CANARY_REDACTED]', content)

    def test_maven_test_has_no_application_secret_property(self):
        source = DRIVER.read_text()
        self.assertNotIn("JAVA_TOOL_OPTIONS='-Dlog.mysql.integration.password=", source)
        self.assertIn("IDENTIFIED BY '';", source)
        self.assertIn('GRANT CREATE, DROP, INDEX, SELECT, INSERT', source)
        self.assertIn('T02_MYSQL_PASSWORD', source)  # future fixture gate allows env migration



if __name__ == '__main__':
    unittest.main()
