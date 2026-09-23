#!/usr/bin/env python3
"""Offline-only safety checks for diagnose-browser-seed-sql.py.

The fixture helpers here are synthetic; no Docker, MySQL, Maven, JVM or HTTP.
"""

import importlib.util
import json
from pathlib import Path
import subprocess
import unittest

SCRIPT = Path('/tmp/wta-t41/diagnose-browser-seed-sql.py')
spec = importlib.util.spec_from_file_location('t41_sql_diagnostic', SCRIPT)
diagnostic = importlib.util.module_from_spec(spec)
spec.loader.exec_module(diagnostic)


class FakeHelper:
    DOCKER = ('docker',)

    def __init__(self, responses):
        self.responses = list(responses)
        self.calls = []

    def run(self, argv, *, data, timeout):
        self.calls.append((argv, data, timeout))
        return self.responses.pop(0)


def completed(code, stdout=b'', stderr=b''):
    return subprocess.CompletedProcess(('docker',), code, stdout, stderr)


class OfflineSafety(unittest.TestCase):
    def test_frozen_source_preflight_does_not_launch(self):
        helper = diagnostic.load_helper()
        result = diagnostic.preflight(helper)
        self.assertEqual(result['source']['head'], diagnostic.EXPECTED_HEAD)
        self.assertEqual(result['helper_sha256'], diagnostic.HELPER_SHA)
        self.assertEqual(len(result['six_sql_sha256']), 6)

    def test_mysql_error_classifier_retains_only_number_and_sqlstate(self):
        raw = b'ERROR 1267 (HY000) credential-canary-secret complete SQL: SELECT password'
        safe = diagnostic.sql_error(raw)
        self.assertEqual(safe, {'mysql_error_number': 1267, 'sqlstate': 'HY000'})
        self.assertNotIn('credential', json.dumps(safe))
        self.assertEqual(diagnostic.sql_error(b'password SECRET'),
                         {'mysql_error_number': None, 'sqlstate': None})

    def test_query_failure_records_no_sql_or_stderr(self):
        helper = FakeHelper([completed(1, stderr=b'ERROR 1267 (HY000) password SECRET')])
        report = {'stages': []}
        with self.assertRaises(diagnostic.StageFailure):
            diagnostic.query(helper, 'f' * 64, 'SELECT password FROM secret',
                             'lookup_A_original', report)
        serialized = json.dumps(report)
        self.assertEqual(report['stages'], [{'stage': 'lookup_A_original', 'exit_code': 1,
                                              'mysql_error_number': 1267, 'sqlstate': 'HY000'}])
        self.assertNotIn('password', serialized)
        self.assertNotIn('SECRET', serialized)
        self.assertIn(b'SELECT password', helper.calls[0][1])  # memory-only input

    def test_original_failure_then_explicit_collation_control(self):
        helper = FakeHelper([completed(1, stderr=b'ERROR 1267 (HY000) secret'),
                             completed(0, stdout=b'123\n')])
        report = {'stages': []}
        sql = 'SELECT user_id FROM sys_user WHERE user_name=CONVERT(0x41 USING utf8mb4);'
        value = diagnostic.lookup(helper, 'f' * 64, sql, 'lookup_A', report)
        self.assertEqual(value, 123)
        self.assertEqual([item['stage'] for item in report['stages']],
                         ['lookup_A_original', 'lookup_A_collated'])
        self.assertIn(b'COLLATE utf8mb4_general_ci', helper.calls[1][1])
        self.assertNotIn('secret', json.dumps(report))

    def test_non_sql_failure_does_not_turn_into_a_false_control_pass(self):
        helper = FakeHelper([completed(0, stdout=b'')])
        report = {'stages': []}
        sql = 'SELECT user_id FROM sys_user WHERE user_name=CONVERT(0x41 USING utf8mb4);'
        with self.assertRaises(diagnostic.StageFailure) as context:
            diagnostic.lookup(helper, 'f' * 64, sql, 'lookup_A', report)
        self.assertEqual(context.exception.stage, 'lookup_A_unexpected_result_shape')
        self.assertEqual(len(helper.calls), 1)

    def test_collation_control_requires_exact_hex_expression(self):
        with self.assertRaisesRegex(RuntimeError, 'comparison shape'):
            diagnostic.collated('SELECT 1;')
        self.assertEqual(diagnostic.collated('SELECT 1 LIKE CONVERT(0x61 USING utf8mb4);'),
                         'SELECT 1 LIKE CONVERT(0x61 USING utf8mb4) COLLATE utf8mb4_general_ci;')

    def test_default_entrypoint_is_read_only(self):
        result = subprocess.run(('python3', str(SCRIPT)), capture_output=True, text=True,
                                timeout=15, check=False)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(result.stdout)['execution'], 'not_started')


if __name__ == '__main__':
    unittest.main(verbosity=2)
