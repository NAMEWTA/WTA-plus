"""Offline synthetic protection for the T-41 private runners; launches no services."""

import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest
from unittest.mock import Mock, patch


DRIVER = Path('/tmp/wta-t41/run-notify-inbox-paging-integration-v1.py')
CAPTURE = Path('/tmp/wta-t41/capture-live-openapi-v1.py')
spec = importlib.util.spec_from_file_location('t41_driver', DRIVER)
driver = importlib.util.module_from_spec(spec)
spec.loader.exec_module(driver)
capture_spec = importlib.util.spec_from_file_location('t41_capture', CAPTURE)
capture = importlib.util.module_from_spec(capture_spec)
capture_spec.loader.exec_module(capture)


class T41DriverSafety(unittest.TestCase):
    def test_only_one_exact_report_and_selector(self):
        self.assertEqual(driver.SUITES, ((driver.TEST_CLASS, driver.TEST_REPORT),))
        self.assertEqual(driver.TEST_CLASS, 'org.namewta.test.notify.NotifyInboxPagingIntegrationTest')
        source = DRIVER.read_text()
        self.assertIn("selectors = 'NotifyInboxPagingIntegrationTest'", source)
        self.assertIn("'-Dnotify.inbox.paging.integration=true'", source)
        self.assertIn("'-DforkCount=1', '-DreuseForks=false'", source)
        self.assertIn('T41_MYSQL_PASSWORD=app_password', source)
        self.assertIn('T36_MYSQL_PASSWORD=app_password', source)
        self.assertNotIn('NotifySupportedModeIntegrationTest,', source)

    def test_source_preflight_requires_exact_opt_in_and_owned_fixture(self):
        base = ('@EnabledIfSystemProperty(named = "notify.inbox.paging.integration", matches = "true") '
                'class NotifyInboxPagingIntegrationTest { '
                'NotifyAtomicResultIntegrationTest fixture; void test() { fixture.open(); } }')
        atomic = 'T36_MYSQL_PASSWORD notify.mysql.integration.url notify.redis.integration.port'
        driver.validate_inbox_source(base, atomic)
        direct = base.replace('NotifyAtomicResultIntegrationTest fixture; void test() { fixture.open(); }',
                              'T41_MYSQL_PASSWORD notify.mysql.integration.url notify.redis.integration.port')
        driver.validate_inbox_source(direct, '')
        for altered, atomic_text in ((base.replace('NotifyInboxPagingIntegrationTest', 'WrongClass'), atomic),
                                     (base.replace('notify.inbox.paging.integration', 'other.flag'), atomic),
                                     (base.replace('.open()', '.close()'), atomic),
                                     (base, atomic.replace('T36_MYSQL_PASSWORD', 'WRONG'))):
            with self.assertRaises(RuntimeError):
                driver.validate_inbox_source(altered, atomic_text)

    def test_cli_denies_implicit_launch(self):
        for path in (DRIVER, CAPTURE):
            result = subprocess.run((sys.executable, str(path)), capture_output=True,
                                    text=True, timeout=5, check=False)
            self.assertEqual(result.returncode, 2)
            self.assertIn('no launch without', result.stderr)

    def test_owned_mysql_is_loopback_labelled_and_not_argv_secret(self):
        args = driver.mysql_run_args('run123', Path('/tmp/owned.env'))
        self.assertIn('namewta.test.owner=T-41', args)
        self.assertIn('namewta.test.run=run123', args)
        self.assertIn('127.0.0.1::3306', args)
        self.assertIn('--env-file', args)
        self.assertIn('--log-bin-trust-function-creators=1', args)
        self.assertNotIn('--privileged', args)
        self.assertNotIn('owned-password', ' '.join(args))

    def test_full_id_dual_label_cleanup_rejects_foreign(self):
        full = 'b' * 64
        self.assertEqual(driver.full_id(full), full)
        with self.assertRaises(RuntimeError):
            driver.full_id(full[:12])
        with patch.object(driver, 'docker', return_value=full) as docker:
            self.assertEqual(driver.owned_ids('run123'), [full])
            self.assertEqual(docker.call_args.args,
                             ('ps', '-aq', '--no-trunc', '--filter',
                              'label=namewta.test.owner=T-41', '--filter',
                              'label=namewta.test.run=run123'))
        with patch.object(driver, 'owned_ids', side_effect=[[full], [full]]), \
             patch.object(driver, 'assert_owned', side_effect=RuntimeError('foreign')), \
             patch.object(driver, 'docker') as docker:
            remaining, errors = driver.cleanup_containers('run123', [full])
        self.assertEqual(remaining, [full])
        self.assertIn('owned_container_remove_failed', errors)
        docker.assert_not_called()

    def test_anonymous_volume_capture_and_absence(self):
        name = 'c' * 64
        with patch.object(driver, 'docker', return_value='[{"Type":"volume","Name":"' + name + '"}]'):
            self.assertEqual(driver.volume_names('owned'), [name])
        with patch.object(driver, 'docker', return_value='[{"Type":"volume","Name":"shared"}]'):
            with self.assertRaises(RuntimeError):
                driver.volume_names('owned')
        result = subprocess.CompletedProcess([], 1, b'', ('Error: get ' + name + ': No such volume').encode())
        with patch.object(driver, 'run', return_value=result):
            self.assertTrue(driver.volume_absent(name))

    def test_fresh_xml_positive_and_fail_skip_stale_are_not_green(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / 'report.xml'
            path.write_text(f'<testsuite name="{driver.TEST_CLASS}" tests="2" failures="0" errors="0" skipped="0">'
                            '<testcase name="page501"/><testcase name="ownUser"/></testsuite>')
            started = path.stat().st_mtime_ns - 1
            self.assertEqual(driver.xml_verdict_for(driver.TEST_CLASS, path, started)['tests'], 2)
            self.assertEqual(driver.xml_methods_for(path), ['page501', 'ownUser'])
            for bad in ('failures="1"', 'skipped="1"'):
                path.write_text(f'<testsuite name="{driver.TEST_CLASS}" tests="2" failures="0" errors="0" skipped="0"/>'.replace(
                    'failures="0"' if bad.startswith('failures') else 'skipped="0"', bad))
                with self.assertRaises(RuntimeError):
                    driver.xml_verdict_for(driver.TEST_CLASS, path, started)
            with self.assertRaises(RuntimeError):
                driver.xml_counts_for(driver.TEST_CLASS, path, time.time_ns() + 1_000_000_000)

    def test_failed_maven_cannot_be_accepted_and_red_xml_is_retained_for_diagnosis(self):
        source = DRIVER.read_text()
        self.assertIn("if report['maven_exit_code']:\n            raise RuntimeError", source)
        self.assertIn("report['acceptance'] = bool(report['exit_code'] == 0)", source)
        self.assertLess(source.index("report['counts'] = {}"), source.index("if report['maven_exit_code']"))
        self.assertIn("report['owned']['captured_anonymous_volume_names'] = list(volumes)", source)
        self.assertIn("report['retained_xml'] = [item for test_class, path in SUITES", source)
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            report = root / ('TEST-' + driver.TEST_CLASS + '.xml')
            report.write_text(f'<testsuite name="{driver.TEST_CLASS}" tests="1" failures="1" errors="0" skipped="0"/>')
            retained = driver.retain_fresh_xml(root, driver.TEST_CLASS, report, report.stat().st_mtime_ns - 1)
            self.assertEqual(retained['file'], 'xml/' + report.name)
            self.assertEqual((root / retained['file']).read_bytes(), report.read_bytes())
            self.assertEqual((root / retained['file']).stat().st_mode & 0o777, 0o600)

    def test_exited_leader_still_checks_process_group(self):
        proc = Mock(pid=1234)
        with patch.object(driver, 'live_group_members', side_effect=[[4321], []]), \
             patch.object(driver.os, 'killpg') as killpg:
            self.assertEqual(driver.stop_group(proc), [])
        killpg.assert_called_once_with(1234, driver.signal.SIGTERM)

    def test_secret_only_child_env_and_redaction(self):
        self.assertEqual(driver.safe_env(T41_MYSQL_PASSWORD='private')['T41_MYSQL_PASSWORD'], 'private')
        self.assertNotIn('T41_MYSQL_PASSWORD', driver.safe_env())
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / 'fresh.xml'
            started = time.time_ns()
            path.write_text('<testsuite>owned-secret</testsuite>')
            self.assertTrue(driver.redact_fresh_xml(path, started, ('owned-secret',)))
            self.assertNotIn('owned-secret', path.read_text())

    def test_capture_has_separate_owner_and_pinned_images(self):
        self.assertEqual(capture.OWNER, 'T-41-OPENAPI')
        self.assertEqual(capture.RUN_ROOT, Path('/tmp/wta-t41/openapi-live'))
        self.assertIn('@sha256:', capture.MINIO_IMAGE)
        source = CAPTURE.read_text()
        self.assertIn("'mysql:8.4.9'", source)
        self.assertIn("'redis:8.6.3'", source)
        self.assertIn("'--env-file', str(mysql_env)", source)
        self.assertIn("'--env-file', str(minio_env)", source)
        self.assertIn("helper.volume_absent(name)", source)

    def test_capture_retains_raw_http_before_strict_loss_failure(self):
        baseline = {'paths': {'/old': {'get': {}}}, 'components': {'schemas': {'Old': {}}}}
        live = {'openapi': '3.1.0', 'paths': {'/new': {'get': {}}},
                'components': {'schemas': {'New': {}}}}
        with tempfile.TemporaryDirectory() as temp:
            report = {}
            raw = json.dumps(live).encode()
            with self.assertRaises(capture.OpenApiLossError):
                capture.persist_and_validate_openapi(Path(temp), raw, 'application/json', baseline, report)
            self.assertEqual((Path(temp) / 'source.json').read_bytes(), raw)
            self.assertEqual(report['openapi_missing']['paths'], ['/old'])
            self.assertEqual(report['openapi_missing']['schemas'], ['Old'])
            self.assertEqual(report['openapi_raw']['bytes'], len(raw))


if __name__ == '__main__':
    unittest.main()
