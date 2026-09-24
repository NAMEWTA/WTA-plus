"""No-service safety checks for the owned T45 policy probe."""
import importlib.util
from pathlib import Path
import subprocess
import sys
import unittest

HERE = Path('/tmp/wta-t45/policy-probe')
SPEC = importlib.util.spec_from_file_location('t45_policy_probe_offline', HERE / 'run-policy-probe.py')
probe = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(probe)


def valid_output():
    shape = ('SHAPE statement_array=true statement_count=1 principal_aws_array=true '
             'principal_aws_star=true action_array=true action_get=true resource_array=true '
             'resource_match=true has_condition=false has_not=false submitted_equal=false')
    summary = 'SUMMARY verification=UNVERIFIED reason=INSUFFICIENT_EVIDENCE'
    facts = [f'FACT subject={subject} observation=UNKNOWN basis=COMPLEX_POLICY scope=BUCKET'
             for subject in probe.SUBJECTS]
    return ('\n'.join([shape, summary, *facts]) + '\n').encode()


class Offline(unittest.TestCase):
    def test_fixed_sources_and_compiled_classpath_exist(self):
        self.assertEqual(probe.sha(probe.SOURCE), probe.SOURCE_SHA)
        self.assertEqual(probe.sha(probe.TWO_RUNNER), probe.TWO_SHA)
        self.assertGreater(len(probe.checked_classpath().split(':')), 100)

    def test_only_bounded_boolean_count_and_known_enums_leave_probe(self):
        result = probe.parse_safe_output(valid_output())
        self.assertEqual(result['shape']['statement_count'], 1)
        self.assertTrue(result['shape']['resource_match'])
        self.assertFalse(result['shape']['submitted_equal'])
        self.assertEqual(list(result['facts']), list(probe.SUBJECTS))
        self.assertEqual(result['facts']['POLICY_READ']['basis'], 'COMPLEX_POLICY')

    def test_canary_unknown_enum_and_missing_fact_are_rejected(self):
        for altered in (valid_output().replace(b'COMPLEX_POLICY', b'canary-secret', 1),
                        valid_output().replace(b'UNKNOWN', b'UNLISTED', 1),
                        valid_output().replace(b'statement_count=1', b'statement_count=9999', 1),
                        b'\n'.join(valid_output().splitlines()[:-1])):
            with self.subTest(altered=altered[:20]), self.assertRaises(RuntimeError):
                probe.parse_safe_output(altered)

    def test_no_arguments_cannot_touch_services(self):
        result = subprocess.run([sys.executable, str(HERE / 'run-policy-probe.py')],
                                capture_output=True, check=False, timeout=10)
        self.assertEqual(result.returncode, 2)
        self.assertNotIn(b'owned_minio', result.stdout)


if __name__ == '__main__':
    unittest.main(verbosity=2)
