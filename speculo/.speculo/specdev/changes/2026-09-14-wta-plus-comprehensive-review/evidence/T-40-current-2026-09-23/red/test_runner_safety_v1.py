#!/usr/bin/env python3
"""Offline-only checks for the T40 owned integration runner draft."""

import hashlib
import importlib.util
import io
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

RUNNER = Path('/tmp/wta-t40/run-notify-notice-retraction-integration-v1.py')
spec = importlib.util.spec_from_file_location('t40_owned_runner', RUNNER)
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)


class RunnerSafety(unittest.TestCase):
    def test_default_does_not_launch(self):
        result = subprocess.run(['python3', str(RUNNER)], capture_output=True,
                                text=True, check=False)
        self.assertEqual(result.returncode, 0)
        self.assertIn('"execution": "not_started"', result.stdout)

    def test_bad_execute_head_rejected_before_docker(self):
        result = subprocess.run(['python3', str(RUNNER), '--execute', '--expected-head', 'abc'],
                                capture_output=True, text=True, check=False)
        self.assertEqual(result.returncode, 2)
        self.assertIn('no launch', result.stderr)

    def test_preflight_requires_owned_seams(self):
        with tempfile.TemporaryDirectory() as tmp:
            source = Path(tmp) / 'Test.java'
            valid = ('@EnabledIfSystemProperty(named = "notify.notice.retraction.integration", '
                     'matches = "true") class NotifyNoticeRetractionIntegrationTest { '
                     '@Test void retractBeforeWorkerClaimStopsCurrentVersionAfterPositiveDeliveryControl() {} '
                     'T40_MYSQL_PASSWORD; T36_MYSQL_PASSWORD; notify.mysql.integration.url; '
                     'notify.redis.integration.port; }')
            with patch.object(runner, 'TEST_SOURCE', source):
                source.write_text(valid)
                self.assertEqual(runner.source_preflight()['class'], runner.TEST_CLASS)
                for missing in ('T40_MYSQL_PASSWORD', 'notify.notice.retraction.integration'):
                    source.write_text(valid.replace(missing, 'missing'))
                    with self.assertRaises(RuntimeError):
                        runner.source_preflight()

    def test_mysql_error_keeps_only_safe_code(self):
        secret = 'private-SQL-password'
        failed = subprocess.CompletedProcess([], 1, b'',
            b'ERROR 1267 (HY000): illegal mix with ' + secret.encode())
        with patch.object(runner, 'run', return_value=failed):
            with self.assertRaises(runner.OwnedSqlError) as caught:
                runner.mysql('a' * 64, 'owned_db', 'SELECT 1;', 'table_count')
        self.assertEqual(caught.exception.safe['mysql_error_number'], 1267)
        self.assertEqual(caught.exception.safe['sqlstate'], 'HY000')
        self.assertNotIn(secret, str(caught.exception.safe))

    def test_surefire_xml_exact_raw_when_no_secret(self):
        raw = (b'<?xml version="1.0"?><testsuite name="' +
               runner.TEST_CLASS.encode() +
               b'" tests="1" failures="1" errors="0" skipped="0">'
               b'<properties><property name="fixture" value="kept"/></properties>'
               b'<testcase name="retractBeforeWorkerClaimStopsCurrentVersionAfterPositiveDeliveryControl" classname="exact">'
               b'<failure type="AssertionError" message="expected delivered">real stack detail</failure>'
               b'</testcase><system-out>diagnostic preserved</system-out></testsuite>')
        with tempfile.TemporaryDirectory() as tmp:
            target = Path(tmp) / 'TEST.xml'
            target.write_bytes(raw)
            counts, methods, retained, provenance = runner.sanitized_xml(target, 0, ('alpha', 'beta'))
            self.assertEqual(retained, raw)
            self.assertEqual(target.read_bytes(), raw)
            self.assertEqual(counts, {'tests': 1, 'failures': 1, 'errors': 0, 'skipped': 0})
            self.assertEqual(methods[0]['failure_types'], ['AssertionError'])
            self.assertEqual(provenance['exact_owned_secret_replacements'], 0)
            self.assertEqual(provenance['source_sha256'], hashlib.sha256(raw).hexdigest())
            self.assertEqual(provenance['retained_sha256'], provenance['source_sha256'])

    def test_xml_replaces_only_exact_secret_and_keeps_failure_details(self):
        secret = 'owned-secret-X123'
        raw = (b'<testsuite name="' + runner.TEST_CLASS.encode() +
               b'" tests="1" failures="1" errors="0" skipped="0">'
               b'<testcase name="retractBeforeWorkerClaimStopsCurrentVersionAfterPositiveDeliveryControl"><failure type="AssertionError" message="' +
               secret.encode() + b' failed">keep stack lines</failure></testcase>'
               b'<system-out>' + secret.encode() + b'</system-out></testsuite>')
        with tempfile.TemporaryDirectory() as tmp:
            target = Path(tmp) / 'TEST.xml'
            target.write_bytes(raw)
            _, methods, retained, provenance = runner.sanitized_xml(target, 0, (secret,))
            self.assertEqual(provenance['exact_owned_secret_replacements'], 2)
            self.assertNotIn(secret.encode(), retained)
            self.assertIn(b'keep stack lines', retained)
            self.assertIn(b'message="[REDACTED-OWNED-SECRET] failed"', retained)
            self.assertIn(b'<system-out>[REDACTED-OWNED-SECRET]</system-out>', retained)
            self.assertEqual(target.read_bytes(), retained)
            self.assertEqual(methods[0]['failure_types'], ['AssertionError'])

    def test_malformed_xml_still_scrubs_owned_secret(self):
        with tempfile.TemporaryDirectory() as tmp:
            target = Path(tmp) / 'TEST.xml'
            target.write_bytes(b'<testsuite>bad-secret')
            with self.assertRaises(Exception):
                runner.sanitized_xml(target, 0, ('bad-secret',))
            self.assertNotIn(b'bad-secret', target.read_bytes())

    def test_log_hashes_and_retains_nonsecret_diagnostics(self):
        with tempfile.TemporaryDirectory() as tmp:
            raw = Path(tmp) / 'raw.log'
            clean = Path(tmp) / 'clean.log'
            raw.write_bytes(b'assertion details\nowned-secret\n')
            provenance = runner.sanitize_log(raw, clean, ('owned-secret',))
            self.assertFalse(raw.exists())
            self.assertEqual(provenance['exact_owned_secret_replacements'], 1)
            self.assertNotEqual(provenance['source_sha256'], provenance['retained_sha256'])
            self.assertIn(b'assertion details', clean.read_bytes())
            self.assertNotIn(b'owned-secret', clean.read_bytes())

    def test_dual_label_full_id_discovery(self):
        cid = 'a' * 64
        with patch.object(runner, 'docker', return_value=cid) as mocked:
            self.assertEqual(runner.owned_ids('deadbeef'), [cid])
            self.assertEqual(mocked.call_args.args[0:3], ('ps', '-aq', '--no-trunc'))
            self.assertIn('label=namewta.test.owner=' + runner.OWNER, mocked.call_args.args)
            self.assertIn('label=namewta.test.run=deadbeef', mocked.call_args.args)
        with patch.object(runner, 'docker', return_value='a' * 12):
            with self.assertRaises(RuntimeError):
                runner.owned_ids('deadbeef')

    def test_volume_absence_case_insensitive(self):
        name = 'a' * 64
        absent = subprocess.CompletedProcess([], 1, b'', ('Error: No Such Volume: ' + name).encode())
        with patch.object(runner, 'run', return_value=absent):
            self.assertTrue(runner.volume_absent(name))

    def test_exact_clean_source_guard(self):
        with patch.object(runner, 'git', side_effect=['f' * 40, '', 'a' * 40]):
            self.assertEqual(runner.source_identity('f' * 40)['tree'], 'a' * 40)
        with patch.object(runner, 'git', side_effect=['f' * 40, ' M test.java']):
            with self.assertRaises(RuntimeError):
                runner.source_identity('f' * 40)


if __name__ == '__main__':
    unittest.main(verbosity=2)
