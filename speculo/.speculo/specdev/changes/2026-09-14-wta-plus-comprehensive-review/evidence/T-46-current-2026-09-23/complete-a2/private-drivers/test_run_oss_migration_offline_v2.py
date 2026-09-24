"""Pure offline guards for the T46 draft runner; no Docker or Maven calls."""

import hashlib
import importlib.util
from pathlib import Path
import unittest


SCRIPT = Path('/tmp/wta-t46/run-oss-migration-integration-v2.py')
spec = importlib.util.spec_from_file_location('t46_runner_offline', SCRIPT)
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)
OLD = 'migratesWithProductionStoreAndDualBucketsThenCleansUpOrRollsBack'
NEW = 'restoreAndCleanupSerializeOnRealObjectRowAndPreserveCurrentSource'


def xml(methods, *, skip=0, marker='owned-run'):
    cases = ''.join('<testcase name="' + method + '"/>' for method in methods)
    return ('<testsuite name="' + runner.SUITES[0] + '" tests="' + str(len(methods))
            + '" failures="0" errors="0" skipped="' + str(skip) + '">'
            + '<properties><property name="t46.owned.run" value="' + marker + '"/></properties>'
            + cases + '</testsuite>').encode()


class DraftRunnerOfflineTest(unittest.TestCase):
    def test_only_my_sql_and_minio_owned_resources(self):
        data = SCRIPT.read_text()
        self.assertEqual(len(runner.SUITES), 1)
        self.assertNotIn('redis:', data.lower())
        self.assertNotIn('oss.upload.redis.integration.port', data)
        self.assertEqual(runner.OWNER, 'T-46-OSS-MIGRATION')
        self.assertEqual(hashlib.sha256(runner.BASE_PATH.read_bytes()).hexdigest(), runner.BASE_SHA)

    def test_old_single_case_cannot_pass(self):
        selected = runner.parse_expected_methods(OLD + ',' + NEW)
        summary = runner.safe_xml_summary(runner.SUITES[0], xml((OLD,)), 'owned-run')
        summary['fresh'] = True
        self.assertFalse(runner.exact_suite_gate([summary], selected))

    def test_exact_two_cases_can_pass(self):
        selected = runner.parse_expected_methods(OLD + ',' + NEW)
        summary = runner.safe_xml_summary(runner.SUITES[0], xml((OLD, NEW)), 'owned-run')
        summary['fresh'] = True
        self.assertTrue(runner.exact_suite_gate([summary], selected))

    def test_skip_or_wrong_case_or_missing_marker_rejected(self):
        selected = runner.parse_expected_methods(OLD + ',' + NEW)
        skipped = runner.safe_xml_summary(runner.SUITES[0], xml((OLD, NEW), skip=1), 'owned-run')
        skipped['fresh'] = True
        self.assertFalse(runner.exact_suite_gate([skipped], selected))
        wrong = runner.safe_xml_summary(runner.SUITES[0], xml((OLD, 'otherCase')), 'owned-run')
        wrong['fresh'] = True
        self.assertFalse(runner.exact_suite_gate([wrong], selected))
        with self.assertRaises(RuntimeError):
            runner.safe_xml_summary(runner.SUITES[0], xml((OLD, NEW), marker='wrong'), 'owned-run')

    def test_expected_methods_must_be_new_and_unique(self):
        for value in (OLD, OLD + ',' + OLD, NEW + ',bad;name', OLD + ',' + NEW + ',' + NEW,
                      NEW + ',' + OLD, OLD + ',otherCase'):
            with self.subTest(value=value), self.assertRaises(ValueError):
                runner.parse_expected_methods(value)

    def test_frozen_methods_match_exact_candidate_source(self):
        source = Path('/srv/WTA-plus/backend/wta-admin/src/test/java/org/namewta/test/oss/migration/OssStorageMigrationIntegrationTest.java').read_text()
        import re
        found = tuple(re.findall(r'(?m)^    @Test\s*\n    void ([A-Za-z_$][A-Za-z0-9_$]*)\s*\(', source))
        self.assertEqual(found, runner.EXPECTED_METHODS)

    def test_generated_secret_redaction(self):
        raw = b'ownedsecret https://owned/?X-Amz-Signature=abc&x=1'
        clean, count = runner.sanitize_bytes(raw, ['ownedsecret'])
        self.assertNotIn(b'ownedsecret', clean)
        self.assertNotIn(b'abc', clean)
        self.assertEqual(count, 2)


if __name__ == '__main__':
    unittest.main()
