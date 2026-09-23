"""Synthetic-only safety checks; no Docker/Maven/service is launched."""

import importlib.util
from pathlib import Path
import tempfile
import time
import unittest
from unittest.mock import Mock, patch


DRIVER = Path('/tmp/wta-t35/run-notify-sms-integration.py')
spec = importlib.util.spec_from_file_location('t35_driver', DRIVER)
driver = importlib.util.module_from_spec(spec)
spec.loader.exec_module(driver)


class DriverSafetyTest(unittest.TestCase):
    def test_only_full_ids_are_accepted(self):
        full = 'a' * 64
        self.assertEqual(driver.full_id(full), full)
        with self.assertRaisesRegex(RuntimeError, 'full container ID'):
            driver.full_id(full[:12])

    def test_discovery_uses_both_labels_and_full_ids(self):
        full = 'b' * 64
        with patch.object(driver, 'docker', return_value=full) as docker:
            self.assertEqual(driver.owned_ids('run123'), [full])
            self.assertEqual(docker.call_args.args, (
                'ps', '-aq', '--no-trunc', '--filter', 'label=namewta.test.owner=T-35',
                '--filter', 'label=namewta.test.run=run123'))

    def test_failure_cleanup_removes_only_verified_owned_full_ids(self):
        full = 'c' * 64
        with patch.object(driver, 'owned_ids', side_effect=[[full], []]), \
             patch.object(driver, 'assert_owned') as owned, \
             patch.object(driver, 'docker', return_value='') as docker:
            remaining, errors = driver.cleanup_containers('run123', [full])
        self.assertEqual((remaining, errors), ([], []))
        owned.assert_called_once_with(full, 'run123')
        docker.assert_called_once_with('rm', '-fv', full, timeout=60)

    def test_foreign_label_never_removed_and_cleanup_fails(self):
        full = 'd' * 64
        with patch.object(driver, 'owned_ids', side_effect=[[full], [full]]), \
             patch.object(driver, 'assert_owned', side_effect=RuntimeError('foreign')), \
             patch.object(driver, 'docker') as docker:
            remaining, errors = driver.cleanup_containers('run123', [full])
        self.assertEqual(remaining, [full])
        self.assertIn('owned_container_remove_failed', errors)
        self.assertIn('owned_containers_remain', errors)
        docker.assert_not_called()

    def test_surefire_must_be_exact_fresh_positive_and_unskipped(self):
        with tempfile.TemporaryDirectory() as directory:
            xml = Path(directory) / 'TEST-class.xml'
            with patch.object(driver, 'TEST_REPORT', xml):
                started = time.time_ns()
                xml.write_text('<testsuite name="' + driver.TEST_CLASS + '" tests="2" failures="0" errors="0" skipped="0"/>')
                self.assertEqual(driver.xml_verdict(started)['tests'], 2)
                xml.write_text('<testsuite name="other.Class" tests="2" failures="0" errors="0" skipped="0"/>')
                with self.assertRaisesRegex(RuntimeError, 'exact selected'):
                    driver.xml_verdict(started)
                xml.write_text('<testsuite name="' + driver.TEST_CLASS + '" tests="2" failures="0" errors="0" skipped="1"/>')
                with self.assertRaisesRegex(RuntimeError, 'zero tests'):
                    driver.xml_verdict(started)
                with self.assertRaisesRegex(RuntimeError, 'absent or stale'):
                    driver.xml_verdict(time.time_ns() + 1_000_000_000)

    def test_exited_leader_does_not_skip_live_group_cleanup(self):
        proc = Mock(pid=1234)
        with patch.object(driver, 'live_group_members', side_effect=[[4321], []]), \
             patch.object(driver.os, 'killpg') as killpg:
            self.assertEqual(driver.stop_group(proc), [])
        killpg.assert_called_once_with(1234, driver.signal.SIGTERM)


if __name__ == '__main__':
    unittest.main()
