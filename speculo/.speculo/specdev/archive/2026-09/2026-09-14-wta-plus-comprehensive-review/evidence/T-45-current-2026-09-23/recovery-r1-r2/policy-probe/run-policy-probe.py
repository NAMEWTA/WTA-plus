#!/usr/bin/env python3
"""Owned single-MinIO policy shape probe. Inert without --execute and exact clean HEAD."""

import argparse
import datetime as dt
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import string
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path('/srv/WTA-plus')
HERE = Path('/tmp/wta-t45/policy-probe')
RUN_ROOT = HERE / 'runs'
SOURCE = HERE / 'T45PolicyProbe.java'
SOURCE_SHA = 'dae1f2ff739e161e043b7ae779061837def2297f8ac61dbe1c777c76bbda68ef'
TWO_RUNNER = Path('/tmp/wta-t45/run-oss-two-integration.py')
TWO_SHA = '6a473403d68d302b300c0e99c49f96b308b9afcf4f6b8dfe9c7882d1749c966f'
CLASSPATH_XML = Path('/tmp/wta-t45/oss-two-runs/0627f8329450e8f7/xml/'
                     'TEST-org.namewta.test.oss.readiness.OssStorageReadinessMinioIntegrationTest.xml')
CLASSPATH_XML_SHA = '6e6cb9154e89115237b4b8fea12e6e0a06edd2b1fdca99dbd9d9b09b58f874d6'
OWNER = 'T-45-POLICY-PROBE'
SUBJECTS = ('POLICY_READ', 'POLICY_WRITE', 'ACL_LIST', 'ACL_WRITE_RISK', 'OBJECT_HEAD', 'OBJECT_GET')
BOOL_FIELDS = ('statement_array', 'principal_aws_array', 'principal_aws_star', 'action_array',
               'action_get', 'resource_array', 'resource_match', 'has_condition', 'has_not',
               'submitted_equal')
SHAPE_FIELDS = ('statement_array', 'statement_count', 'principal_aws_array',
                'principal_aws_star', 'action_array', 'action_get', 'resource_array',
                'resource_match', 'has_condition', 'has_not', 'submitted_equal')
OBSERVATIONS = frozenset(('ALLOWED', 'DENIED', 'UNKNOWN'))
SCOPES = frozenset(('BUCKET', 'OBJECT'))
BASES = frozenset(('POLICY_ALLOW', 'POLICY_DENY', 'NO_SUCH_POLICY', 'POLICY_UNREADABLE',
    'COMPLEX_POLICY', 'INVALID_POLICY', 'ACL_GRANT', 'ACL_NO_GRANT', 'ACL_UNREADABLE',
    'HTTP_SUCCESS', 'HTTP_DENIED', 'HTTP_NOT_FOUND', 'REDIRECT', 'HTTP_ERROR', 'TIMEOUT',
    'NETWORK_ERROR', 'INTERRUPTED', 'NOT_EVALUATED', 'UNSUPPORTED'))
VERIFICATIONS = frozenset(('VERIFIED', 'MISMATCH', 'UNVERIFIED'))
REASONS = frozenset(('READY', 'INVALID_REQUEST', 'UNSUPPORTED', 'DIAGNOSTIC_OBJECT_MISSING',
    'POLICY_UNREADABLE', 'POLICY_MISMATCH', 'ANONYMOUS_READ_MISMATCH',
    'ANONYMOUS_WRITE_ALLOWED', 'INSUFFICIENT_EVIDENCE', 'TIMEOUT', 'PROVIDER_ERROR'))


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def private(path, value):
    with os.fdopen(os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as out:
        out.write(value)


def load_owned_helper():
    if sha(TWO_RUNNER) != TWO_SHA:
        raise RuntimeError('frozen two-suite runner changed')
    spec = importlib.util.spec_from_file_location('t45_two_for_policy_probe', TWO_RUNNER)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    base = module.load_base()
    base.OWNER = OWNER
    return base, module


def checked_classpath():
    if sha(CLASSPATH_XML) != CLASSPATH_XML_SHA:
        raise RuntimeError('frozen sanitized classpath source changed')
    root = ET.parse(CLASSPATH_XML).getroot()
    values = [item.get('value') for item in root.find('properties').findall('property')
              if item.get('name') == 'surefire.test.class.path']
    if len(values) != 1 or not values[0]:
        raise RuntimeError('exact compiled test classpath is unavailable')
    entries = values[0].split(os.pathsep)
    if len(entries) < 100 or any(not Path(path).exists() for path in entries):
        raise RuntimeError('compiled test classpath has missing entries')
    if not any((Path(path) / 'org/namewta/common/oss/client/AbstractOssClientImpl.class').is_file()
               for path in entries):
        raise RuntimeError('compiled production diagnostic class is unavailable')
    return values[0]


def parse_safe_output(raw):
    if len(raw) > 16_384:
        raise RuntimeError('probe output exceeded fixed bound')
    lines = raw.decode('ascii', errors='strict').splitlines()
    if len(lines) != 8:
        raise RuntimeError('probe did not emit exact fixed summary')

    def fields(line, kind, expected):
        parts = line.split(' ')
        if len(parts) != len(expected) + 1 or parts[0] != kind:
            raise RuntimeError('probe summary shape differs')
        pairs = [part.split('=', 1) for part in parts[1:]]
        if any(len(pair) != 2 or not re.fullmatch(r'[A-Z_a-z0-9]+', pair[1]) for pair in pairs):
            raise RuntimeError('probe emitted non-enum data')
        if tuple(pair[0] for pair in pairs) != expected:
            raise RuntimeError('probe summary fields differ')
        return {key: value for key, value in pairs}

    shape = fields(lines[0], 'SHAPE', SHAPE_FIELDS)
    if any(shape[key] not in ('true', 'false') for key in BOOL_FIELDS):
        raise RuntimeError('probe shape contains a non-boolean value')
    if not shape['statement_count'].isdigit() or not 0 <= int(shape['statement_count']) <= 100:
        raise RuntimeError('probe statement count is outside bound')
    summary = fields(lines[1], 'SUMMARY', ('verification', 'reason'))
    if summary['verification'] not in VERIFICATIONS or summary['reason'] not in REASONS:
        raise RuntimeError('probe summary enum is unknown')
    facts = {}
    for index, subject in enumerate(SUBJECTS, start=2):
        fact = fields(lines[index], 'FACT', ('subject', 'observation', 'basis', 'scope'))
        if fact['subject'] != subject or fact['observation'] not in OBSERVATIONS \
                or fact['basis'] not in BASES or fact['scope'] not in SCOPES:
            raise RuntimeError('probe fact enum is unknown or out of order')
        facts[subject] = {key: fact[key] for key in ('observation', 'basis', 'scope')}
    return {'shape': {key: (int(value) if key == 'statement_count' else value == 'true')
                      for key, value in shape.items()},
            'summary': summary, 'facts': facts}


def owned_child(argv, env, cwd, timeout, base):
    child = subprocess.Popen(argv, cwd=cwd, env=env, stdout=subprocess.PIPE,
                             stderr=subprocess.DEVNULL, start_new_session=True)
    try:
        stdout, _ = child.communicate(timeout=timeout)
        if child.returncode != 0:
            raise RuntimeError('owned compiler or probe exited nonzero')
        return stdout
    finally:
        if base.stop_group(child):
            raise RuntimeError('owned child process group did not stop')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--execute', action='store_true')
    parser.add_argument('--expected-head')
    args = parser.parse_args()
    if not args.execute or not args.expected_head or not re.fullmatch(r'[0-9a-f]{40}', args.expected_head):
        parser.error('requires --execute --expected-head <exact clean 40-hex SHA>')
    os.umask(0o077)
    run_id = secrets.token_hex(8)
    run_dir = RUN_ROOT / run_id
    run_dir.mkdir(parents=True, mode=0o700)
    scratch = run_dir / 'scratch'
    scratch.mkdir(mode=0o700)
    secret_file = run_dir / 'minio.env'
    base = None
    before = None
    container_ids, volumes, ports = [], [], []
    docker_touched = False
    report = {'gate': 'T45 one owned MinIO policy shape', 'run_id': run_id,
              'probe': None, 'source_same_clean': False, 'owned_cleanup': False,
              'exit_code': 1, 'phase': 'preflight', 'started_utc': dt.datetime.now(dt.timezone.utc).isoformat()}
    try:
        if sha(SOURCE) != SOURCE_SHA:
            raise RuntimeError('frozen Java probe source changed')
        classpath = checked_classpath()
        base, two = load_owned_helper()
        before = base.source_identity(args.expected_head)
        alphabet = string.ascii_letters + string.digits
        password = ''.join(secrets.choice(alphabet) for _ in range(40))
        user = 't45probe' + run_id
        private(secret_file, ('MINIO_ROOT_USER=' + user + '\nMINIO_ROOT_PASSWORD=' + password + '\n').encode())
        report['phase'] = 'owned_minio'
        docker_touched = True
        cid = base.full_id(base.docker('run', '--pull=never', '-d', '--name',
            't45-policy-' + run_id, '--label', 'namewta.test.owner=' + OWNER,
            '--label', 'namewta.test.run=' + run_id, '-p', '127.0.0.1::9000',
            '--env-file', str(secret_file), two.MINIO_IMAGE, 'server', '--address', ':9000', '/data'))
        container_ids.append(cid)
        base.assert_owned(cid, run_id)
        volumes.extend(base.volume_names(cid))
        port = base.mapped_port(cid, '9000')
        ports.append(port)
        base.wait_minio(port)
        report['phase'] = 'compile'
        classes = scratch / 'classes'
        classes.mkdir(mode=0o700)
        clean_env = base.safe_env(TMPDIR=str(scratch))
        owned_child(('javac', '-d', str(classes), '-cp', classpath, str(SOURCE)),
                    clean_env, scratch, 90, base)
        report['phase'] = 'probe'
        run_env = base.safe_env(TMPDIR=str(scratch), T45_PROBE_ENDPOINT='http://127.0.0.1:' + str(port),
            T45_PROBE_ACCESS_KEY=user, T45_PROBE_SECRET_KEY=password)
        raw = owned_child(('java', '-Xms64m', '-Xmx512m', '-Djava.io.tmpdir=' + str(scratch),
                           '-cp', str(classes) + os.pathsep + classpath, 'T45PolicyProbe'),
                          run_env, scratch, 120, base)
        report['probe'] = parse_safe_output(raw)
        report['phase'] = 'cleanup'
    except BaseException as exc:
        report['failure_type'] = type(exc).__name__
    finally:
        errors = []
        try:
            secret_file.unlink(missing_ok=True)
            shutil.rmtree(scratch)
        except Exception:
            errors.append('owned_private_file_cleanup')
        if base is not None and docker_touched:
            try:
                remaining, container_errors = base.cleanup_containers(run_id, container_ids)
                if remaining or container_errors:
                    errors.append('owned_container_cleanup')
            except Exception:
                errors.append('owned_container_cleanup')
            try:
                if not all(base.volume_absent(name) for name in dict.fromkeys(volumes)):
                    errors.append('owned_volume_cleanup')
            except Exception:
                errors.append('owned_volume_check')
            try:
                if not all(base.closed(port) for port in ports):
                    errors.append('owned_port_cleanup')
            except Exception:
                errors.append('owned_port_check')
        if base is not None and before is not None:
            try:
                report['source_same_clean'] = before == base.source_identity(args.expected_head)
            except Exception:
                errors.append('source_identity_check')
        report['owned_cleanup'] = not errors
        report['cleanup_error_count'] = len(errors)
        if report['probe'] is not None and report['source_same_clean'] and not errors:
            report['exit_code'] = 0
        report['finished_utc'] = dt.datetime.now(dt.timezone.utc).isoformat()
        private(run_dir / 'result.json', json.dumps(report, indent=2, sort_keys=True).encode())
        print(str(run_dir / 'result.json'))
    return report['exit_code']


if __name__ == '__main__':
    sys.exit(main())
