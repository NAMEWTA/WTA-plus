#!/usr/bin/env python3
"""Synthetic only; never launches Vite, Chrome, Docker, HTTP or a build."""

import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

PREVIEW = Path('/tmp/wta-t41/run-inbox-api-controlled-dist-v2.py')
EQUIVALENCE = Path('/tmp/wta-t41/verify-e-prod-dist-equivalence.py')


def load(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


preview = load(PREVIEW, 'preview_v2')
equivalence = load(EQUIVALENCE, 'e_equivalence')


class OfflineV2(unittest.TestCase):
    def test_exact_e_source_pinned_and_default_does_not_launch(self):
        self.assertEqual(preview.EXPECTED_HEAD, 'a0dcbac8fd33a754c74c154551f7090a8878fb48')
        self.assertEqual(preview.source_identity()['head'], preview.EXPECTED_HEAD)
        result = subprocess.run(('python3', str(PREVIEW)), capture_output=True, text=True,
                                timeout=15, check=False)
        self.assertEqual(result.returncode, 0)
        self.assertIn('--execute is required', result.stdout)

    def test_c_to_e_allowlist_is_exact_and_v1_kept_separate(self):
        self.assertEqual(preview.EXPECTED_FRONTEND_DELTA,
                         [equivalence.REGISTRY, equivalence.E_RUNNER, equivalence.E_TEST])
        self.assertNotEqual(preview.RUN_ROOT, Path('/tmp/wta-t41/api-controlled-runs'))
        self.assertEqual(preview.EXPECTED_C_BUILD, equivalence.C)
        self.assertEqual(preview.EXPECTED_MANIFEST_SHA, equivalence.MANIFEST_SHA)

    def test_numeric_failure_location_does_not_copy_sensitive_errors(self):
        with tempfile.TemporaryDirectory() as place:
            case_file = Path(place) / 'inbox-without-realtime.spec.ts'
            case_file.write_text('line\n' * 12)
            suites = [{'file': str(case_file), 'specs': []}]
            for index, title in enumerate(preview.TITLES):
                result = {'status': 'failed' if index == 0 else 'passed',
                          'errorLocation': {'file': str(case_file), 'line': 8, 'column': 4},
                          'errors': [{'message': 'credential-canary-private',
                                      'stack': 'credential-canary-private'}],
                          'attachments': [{'body': 'credential-canary-private'}]}
                suites[0]['specs'].append({'file': str(case_file), 'title': title,
                                           'line': 3, 'column': 2,
                                           'tests': [{'projectName': 'chromium',
                                                      'results': [result]}]})
            raw = {'stats': {'expected': 4, 'unexpected': 1, 'skipped': 0, 'flaky': 0},
                   'suites': suites}
            with patch.object(preview, 'TEST', case_file):
                summary = preview.case_summary(raw)
            self.assertEqual(summary['cases'][0]['declaration_location'], {'line': 3, 'column': 2})
            self.assertEqual(summary['cases'][0]['assertion_location'], {'line': 8, 'column': 4})
            self.assertFalse(preview.accepted_cases(summary))
            self.assertNotIn('credential-canary-private', json.dumps(summary))
            self.assertNotIn('errors', json.dumps(summary))

    def test_location_rejects_foreign_file_and_bad_bounds(self):
        with tempfile.TemporaryDirectory() as place:
            case_file = Path(place) / 'fixed.ts'
            case_file.write_text('line\n' * 4)
            with patch.object(preview, 'TEST', case_file):
                self.assertIsNone(preview.safe_location({'file': str(Path(place) / 'foreign.ts'),
                                                         'line': 2, 'column': 1}))
                self.assertIsNone(preview.safe_location({'file': str(case_file),
                                                         'line': 5, 'column': 1}))
                self.assertIsNone(preview.safe_location({'file': str(case_file),
                                                         'line': True, 'column': 1}))
                self.assertEqual(preview.safe_location({'file': str(case_file),
                                                        'line': 4, 'column': 1}),
                                 {'line': 4, 'column': 1})

    def test_equivalence_refuses_unreviewed_frontend_delta_before_artifact_read(self):
        e = 'e' * 40
        def fake_git(*args):
            if args == ('rev-parse', 'HEAD'):
                return e
            if args == ('status', '--porcelain', '--untracked-files=all'):
                return ''
            raise AssertionError('Unexpected Git read')
        with patch.object(equivalence, 'git', side_effect=fake_git), \
             patch.object(equivalence, 'ancestor'), \
             patch.object(equivalence, 'changed', side_effect=[
                 [equivalence.REGISTRY], [equivalence.E_RUNNER, 'frontend/src/unreviewed.ts'],
                 [equivalence.REGISTRY, equivalence.E_RUNNER, 'frontend/src/unreviewed.ts']]), \
             patch.object(equivalence, 'sha', side_effect=AssertionError('Artifact read too early')):
            with self.assertRaisesRegex(RuntimeError, 'allowlist differs'):
                equivalence.verify(e)

    def test_equivalence_requires_full_lowercase_e_sha(self):
        for invalid in ('pending', 'A' * 40, 'a' * 39):
            with self.assertRaisesRegex(RuntimeError, '40 lower-case hex'):
                equivalence.verify(invalid)


if __name__ == '__main__':
    unittest.main(verbosity=2)
