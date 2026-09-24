#!/usr/bin/env python3
"""No Docker/Maven: pure parser, sanitization and output ownership checks."""
import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET


RUNNER = Path('/tmp/wta-t44/run-oss-seven-integration.py')
spec = importlib.util.spec_from_file_location('t44_oss_seven_runner', RUNNER)
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)


def xml(browser=False, marker='owned-run'):
    suite = ET.Element('testsuite', name=runner.BROWSER_SUITE if browser else runner.SUITES[0],
                       tests='1', failures='0', errors='0', skipped='0')
    props = ET.SubElement(suite, 'properties')
    if marker:
        ET.SubElement(props, 'property', name='t44.owned.run', value=marker)
    case = ET.SubElement(suite, 'testcase', name='runsRealGate')
    if browser:
        out = ET.SubElement(case, 'system-out')
        out.text = ('Running 10 tests using 1 worker\n'
                    + '\n'.join(f'[{index}/10] T-18 owned case {index}' for index in range(1, 11))
                    + '\n10 passed (5s)\n')
    return ET.tostring(suite)


class Offline(unittest.TestCase):
    def test_parser_accepts_nested_browser_stdout_and_all_ten(self):
        result = runner.safe_xml_summary(runner.BROWSER_SUITE, xml(True), 'owned-run')
        self.assertEqual(result['browser']['indices'], list(range(1, 11)))
        self.assertTrue(result['private_properties_marker'])

    def test_parser_rejects_missing_private_property(self):
        with self.assertRaises(RuntimeError):
            runner.safe_xml_summary(runner.SUITES[0], xml(marker=None), 'owned-run')

    def test_parser_rejects_incomplete_browser_or_skip(self):
        data = xml(True).replace(b'[10/10]', b'[9/10]')
        with self.assertRaises(RuntimeError):
            runner.safe_xml_summary(runner.BROWSER_SUITE, data, 'owned-run')
        data = xml(True).replace(b'10 passed', b'9 passed, 1 skipped')
        with self.assertRaises(RuntimeError):
            runner.safe_xml_summary(runner.BROWSER_SUITE, data, 'owned-run')

    def test_redacts_raw_and_xml_escaped_sigv4_queries(self):
        source = b'<x>secret &amp;X-Amz-Signature=derivedABC &X-Amz-Credential=ownedABC ?X-Amz-Date=now</x>'
        clean, count = runner.sanitize_bytes(source, ['secret'])
        self.assertEqual(count, 4)
        self.assertNotIn(b'secret', clean)
        self.assertNotIn(b'derivedABC', clean)
        self.assertNotIn(b'ownedABC', clean)
        self.assertNotIn(b'X-Amz-Date=now', clean)

    def test_browser_output_restores_previous_exact_bytes(self):
        original = runner.BROWSER_OUTPUT
        with tempfile.TemporaryDirectory() as root:
            path = Path(root) / 'upload-results'
            path.mkdir()
            before = b'{"status":"old"}\n'
            (path / '.last-run.json').write_bytes(before)
            runner.BROWSER_OUTPUT = path
            try:
                snapshot = runner.browser_output_before()
                (path / '.last-run.json').write_bytes(b'{"status":"new"}\n')
                runner.restore_browser_output(snapshot)
                self.assertEqual((path / '.last-run.json').read_bytes(), before)
            finally:
                runner.BROWSER_OUTPUT = original

    def test_no_execute_is_inert(self):
        done = subprocess.run((sys.executable, str(RUNNER), '--expected-head', 'a' * 40),
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=5)
        self.assertEqual(done.returncode, 2)

    def test_unique_tmp_marker_has_no_unowned_process(self):
        marker = Path('/tmp/wta-t44/no-such-owned-run-' + 'f' * 32)
        self.assertEqual(runner.live_owned_processes(marker), [])
        self.assertEqual(runner.stop_owned_processes(marker), [])

    def test_all_fresh_secret_surefire_temp_files_are_removed(self):
        old_root, old_reports = runner.ROOT, runner.REPORT_ROOT
        with tempfile.TemporaryDirectory() as root:
            runner.ROOT = Path(root)
            runner.REPORT_ROOT = runner.ROOT / 'backend/wta-admin/target/surefire-reports'
            tmp = runner.ROOT / 'backend/wta-admin/target/surefire'
            tmp.mkdir(parents=True)
            runner.REPORT_ROOT.mkdir()
            (runner.ROOT / 'xml').mkdir()
            for index in (1, 2):
                (tmp / f'surefire-{index}.tmp').write_bytes(b'owned-secret')
            try:
                with self.assertRaises(RuntimeError):
                    runner.retain_fresh_text_and_temp(0, runner.ROOT, ['owned-secret'])
                self.assertEqual(list(tmp.iterdir()), [])
            finally:
                runner.ROOT, runner.REPORT_ROOT = old_root, old_reports


if __name__ == '__main__':
    unittest.main(verbosity=2)
