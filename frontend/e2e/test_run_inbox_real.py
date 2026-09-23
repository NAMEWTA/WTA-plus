"""No-Docker regressions for the owned T-34 browser gate."""
import importlib.util
import json
import os
import pathlib
import signal
import subprocess
import sys
import tempfile
import time
import unittest
from types import SimpleNamespace
from unittest import mock


SCRIPT = pathlib.Path(__file__).with_name('run-inbox-real.py')
SPEC = importlib.util.spec_from_file_location('run_inbox_real', SCRIPT)
runner = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(runner)


def report():
    return {'exit_code': 0, 'cleanup': {}, 'source_after': {'head': 'fixed'}}


class ProcessCleanupTest(unittest.TestCase):
    def test_exited_leader_does_not_hide_live_child(self):
        child = 'import time; time.sleep(30)'
        leader = subprocess.Popen(
            [sys.executable, '-c',
             'import subprocess,sys; subprocess.Popen([sys.executable,"-c",sys.argv[1]])', child],
            stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
            start_new_session=True)
        try:
            leader.wait(timeout=5)
            deadline = time.monotonic() + 5
            while not runner.live_group_members(leader.pid) and time.monotonic() < deadline:
                time.sleep(0.01)
            self.assertTrue(runner.live_group_members(leader.pid), 'child must survive its leader')
            runner.stop(leader, term_seconds=1, kill_seconds=1)
            self.assertEqual(runner.live_group_members(leader.pid), [])
        finally:
            if runner.live_group_members(leader.pid):
                os.killpg(leader.pid, signal.SIGKILL)
            leader.poll()

    def test_cleanup_error_forces_failed_gate_even_if_test_passed(self):
        result = report()
        self.assertEqual(runner.gate_exit_code(result, ['process_error_123'], {'head': 'fixed'}), 1)
        self.assertEqual(runner.gate_exit_code(result, [], {'head': 'fixed'}), 0)


class IdentityTest(unittest.TestCase):
    def fixture(self, *, file='inbox-real.e2e.ts', title=None):
        return {'suites': [{'file': file, 'specs': [{
            'title': title or runner.TEST_NAME,
            'tests': [{'projectName': 'chromium', 'results': [{'status': 'passed'}]}]
        }]}]}

    def test_fixed_source_title_and_single_case(self):
        identity = runner.playwright_identity(self.fixture(), runner.ROOT)
        self.assertEqual(identity['file'], runner.TEST_PATH)
        self.assertEqual(identity['title'], runner.TEST_NAME)

    def test_wrong_source_title_or_extra_case_fails(self):
        for data in (self.fixture(file='other.e2e.ts'), self.fixture(title='other'),
                     self.fixture(file=''), self.fixture()):
            if data['suites'][0]['file'] == 'inbox-real.e2e.ts' and data['suites'][0]['specs'][0]['title'] == runner.TEST_NAME:
                data['suites'][0]['specs'].append(data['suites'][0]['specs'][0])
            with self.subTest(data=data['suites'][0]['file']):
                with self.assertRaises(RuntimeError):
                    runner.playwright_identity(data, runner.ROOT)


class ContainerCleanupTest(unittest.TestCase):
    def test_process_stop_failure_still_attempts_container_removal(self):
        cid = 'e' * 64
        present = {cid}
        def fake_docker(*args, **kwargs):
            if args[0] == 'ps':
                return '\n'.join(present)
            if args[0] == 'inspect':
                return json.dumps({'namewta.test.owner': 'T-34', 'namewta.test.run': 'run123'})
            if args[0] == 'rm':
                present.remove(cid)
                return cid
            raise AssertionError(args)
        with tempfile.TemporaryDirectory() as temporary, mock.patch.object(runner, 'docker', fake_docker), \
                mock.patch.object(runner, 'stop', side_effect=OSError('injected stop failure')), \
                mock.patch.object(runner, 'live_group_members', return_value=[123]):
            result = report()
            errors = runner.cleanup_resources([SimpleNamespace(pid=123)], [], [],
                                              pathlib.Path(temporary), 'run123', (), result)
        self.assertIn('process_error_123', errors)
        self.assertIn('process_group_remaining_123', errors)
        self.assertEqual(result['cleanup'][cid], 'removed')
        self.assertEqual(runner.gate_exit_code(result, errors, {'head': 'fixed'}), 1)

    def test_recovers_uncaptured_container_from_both_exact_labels(self):
        cid = 'a' * 64
        calls = []
        present = {cid}
        def fake_docker(*args, **kwargs):
            calls.append(args)
            if args[:2] == ('ps', '-aq'):
                self.assertEqual(args[2:], ('--filter', 'label=' + runner.OWNER_LABEL,
                                            '--filter', 'label=' + runner.RUN_LABEL + 'run123'))
                return '\n'.join(present)
            if args[0] == 'inspect':
                self.assertEqual(args[-1], cid)
                return json.dumps({'namewta.test.owner': 'T-34', 'namewta.test.run': 'run123'})
            if args[0] == 'rm':
                self.assertEqual(args[-1], cid)
                present.remove(cid)
                return cid
            raise AssertionError(args)
        with tempfile.TemporaryDirectory() as temporary, mock.patch.object(runner, 'docker', fake_docker):
            result = report()
            errors = runner.cleanup_resources([], [], [], pathlib.Path(temporary), 'run123', (), result)
        self.assertEqual(errors, [])
        self.assertEqual(result['cleanup'][cid], 'removed')
        self.assertEqual(result['cleanup']['owned_containers_remaining'], [])
        self.assertEqual(sum(call[0] == 'ps' for call in calls), 2)

    def test_mismatched_label_cannot_be_removed(self):
        cid = 'b' * 64
        calls = []
        def fake_docker(*args, **kwargs):
            calls.append(args)
            if args[0] == 'ps':
                return cid
            if args[0] == 'inspect':
                return json.dumps({'namewta.test.owner': 'T-34', 'namewta.test.run': 'another-run'})
            raise AssertionError('must not remove a container without both exact labels')
        with tempfile.TemporaryDirectory() as temporary, mock.patch.object(runner, 'docker', fake_docker):
            result = report()
            errors = runner.cleanup_resources([], [], [], pathlib.Path(temporary), 'run123', (), result)
        self.assertTrue(errors)
        self.assertEqual(result['cleanup']['owned_containers_remaining'], [cid])
        self.assertFalse(any(call[0] == 'rm' for call in calls))

    def test_logger_failure_still_attempts_container_removal(self):
        cid = 'c' * 64
        calls = []
        class BrokenLog:
            def close(self):
                raise OSError('injected close failure')
        def fake_docker(*args, **kwargs):
            calls.append(args)
            if args[0] == 'ps':
                return cid
            if args[0] == 'inspect':
                return json.dumps({'namewta.test.owner': 'T-34', 'namewta.test.run': 'run123'})
            if args[0] == 'rm':
                raise RuntimeError('injected Docker failure')
            raise AssertionError(args)
        with tempfile.TemporaryDirectory() as temporary, mock.patch.object(runner, 'docker', fake_docker):
            result = report()
            errors = runner.cleanup_resources([], [BrokenLog()], [], pathlib.Path(temporary), 'run123', (), result)
        self.assertIn('log_close_error_0', errors)
        self.assertIn('container_remove_error', errors)
        self.assertTrue(any(call[0] == 'rm' for call in calls))
        self.assertEqual(runner.gate_exit_code(result, errors, {'head': 'fixed'}), 1)

    def test_redaction_failure_still_attempts_container_removal(self):
        cid = 'd' * 64
        present = {cid}
        original_write = pathlib.Path.write_text
        def failing_write(path, content, *args, **kwargs):
            if path.name == 'backend.log':
                raise OSError('injected redaction failure')
            return original_write(path, content, *args, **kwargs)
        def fake_docker(*args, **kwargs):
            if args[0] == 'ps':
                return '\n'.join(present)
            if args[0] == 'inspect':
                return json.dumps({'namewta.test.owner': 'T-34', 'namewta.test.run': 'run123'})
            if args[0] == 'rm':
                present.remove(cid)
                return cid
            raise AssertionError(args)
        with tempfile.TemporaryDirectory() as temporary:
            run = pathlib.Path(temporary)
            (run / 'backend.raw.log').write_text('synthetic secret')
            with mock.patch.object(runner, 'docker', fake_docker), mock.patch.object(pathlib.Path, 'write_text', failing_write):
                result = report()
                errors = runner.cleanup_resources([], [], [], run, 'run123', ('synthetic secret',), result)
            self.assertFalse((run / 'backend.raw.log').exists())
        self.assertIn('log_redaction_error_backend', errors)
        self.assertEqual(result['cleanup'][cid], 'removed')
        self.assertEqual(runner.gate_exit_code(result, errors, {'head': 'fixed'}), 1)


if __name__ == '__main__':
    unittest.main()
