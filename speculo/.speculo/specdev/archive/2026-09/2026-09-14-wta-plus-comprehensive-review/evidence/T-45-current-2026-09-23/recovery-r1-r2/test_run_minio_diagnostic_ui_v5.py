#!/usr/bin/env python3
"""Offline UI runner contracts; no browser, containers, HTTP or build."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import stat
import tempfile
import unittest
from unittest.mock import patch


path = Path('/tmp/wta-t45/run-minio-diagnostic-ui-v5.py')
spec = importlib.util.spec_from_file_location('t45_ui_v5_runner_offline', path)
driver = importlib.util.module_from_spec(spec)
spec.loader.exec_module(driver)


def reporter():
    return {'stats': {'expected': 2, 'unexpected': 0, 'skipped': 0, 'flaky': 0},
            'suites': [{'file': str(driver.UI_SPEC), 'specs': [
                {'file': str(driver.UI_SPEC), 'title': title,
                 'tests': [{'projectName': 'chromium', 'results': [{'status': 'passed'}]}]}
                for title in driver.UI_TITLES]}]}


class Offline(unittest.TestCase):
    def test_owned_page_gate_and_safe_result_excludes_secret(self):
        bad_id = 8_305_000_000_000_000_001
        key_bad = 'xabcdef123456'
        rows = [
            {'ossConfigId': '1', 'configKey': 'image', 'accessPolicy': '2'},
            {'ossConfigId': '2', 'configKey': 'minio', 'accessPolicy': '0'},
            {'ossConfigId': str(bad_id), 'configKey': key_bad, 'accessPolicy': '9',
             'secretKey': 'synthetic-credential-must-not-retain'},
        ]
        before, exact = driver.safe_oss_config_page(
            200, {'code': 200, 'data': {'total': 3, 'rows': rows}},
            bad_id, key_bad, 1, 2)
        driver.require_owned_bad_before(before, exact)
        self.assertEqual(set(before), {'status', 'code', 'total', 'rowcount',
                                     'bad_present', 'public_present', 'private_present',
                                     'unknown_policy_count'})
        self.assertNotIn('synthetic-credential-must-not-retain', str(before))
        after, exact_after = driver.safe_oss_config_page(
            200, {'code': 200, 'data': {'total': 2, 'rows': rows[:2]}},
            bad_id, key_bad, 1, 2)
        self.assertFalse(exact_after)
        driver.require_owned_bad_after(before, after)

    def test_wrong_page_or_policy_never_authorizes_delete(self):
        bad_id = 8_305_000_000_000_000_001
        key_bad = 'xabcdef123456'
        rows = [
            {'ossConfigId': '1', 'configKey': 'image', 'accessPolicy': '2'},
            {'ossConfigId': '2', 'configKey': 'minio', 'accessPolicy': '0'},
            {'ossConfigId': str(bad_id), 'configKey': key_bad, 'accessPolicy': '9'},
        ]
        with self.assertRaisesRegex(RuntimeError, 'first page incomplete'):
            driver.safe_oss_config_page(200, {'code': 200, 'data': {'total': 4, 'rows': rows}},
                                        bad_id, key_bad, 1, 2)
        wrong = [dict(row) for row in rows]
        wrong[2]['configKey'] = 'unowned'
        with self.assertRaisesRegex(RuntimeError, 'unrelated invalid policy'):
            driver.safe_oss_config_page(200, {'code': 200, 'data': {'total': 3, 'rows': wrong}},
                                        bad_id, key_bad, 1, 2)
        absent, exact = driver.safe_oss_config_page(
            200, {'code': 200, 'data': {'total': 2, 'rows': rows[:2]}},
            bad_id, key_bad, 1, 2)
        with self.assertRaisesRegex(RuntimeError, 'precondition differs'):
            driver.require_owned_bad_before(absent, exact)

    def test_delete_sql_is_exact_owned_id_key_and_invalid_policy(self):
        bad_id = 8_305_000_000_000_000_001
        base = driver.load_base()
        sql = driver.exact_owned_bad_delete_sql(base.compared_hexsql,
                                                bad_id, 'xabcdef123456')
        self.assertIn(f'WHERE oss_config_id={bad_id}', sql)
        self.assertIn('AND config_key=CONVERT(0x', sql)
        self.assertIn(' USING utf8mb4) COLLATE utf8mb4_general_ci', sql)
        self.assertIn("AND access_policy='9'", sql)
        self.assertIn('SELECT ROW_COUNT();', sql)
        with self.assertRaisesRegex(RuntimeError, 'delete identity differs'):
            driver.exact_owned_bad_delete_sql(base.compared_hexsql, 1, 'xabcdef123456')
        with self.assertRaisesRegex(RuntimeError, 'delete identity differs'):
            driver.exact_owned_bad_delete_sql(base.compared_hexsql, bad_id, 'unowned')
        with self.assertRaisesRegex(RuntimeError, 'comparison collation differs'):
            driver.exact_owned_bad_delete_sql(base.hexsql, bad_id, 'xabcdef123456')

    def test_owned_schema_and_hex_expression_have_different_default_collations(self):
        ddl = (driver.ROOT / 'release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql').read_text()
        table = ddl.split('create table sys_oss_config (', 1)[1].split(') engine=innodb', 1)[0]
        self.assertIn('config_key      varchar(20)', table)
        self.assertNotIn('collate', table.lower())
        source = path.read_text()
        self.assertIn("'--collation-server=utf8mb4_general_ci'", source)
        self.assertTrue(driver.load_base().hexsql('xabcdef123456').endswith(' USING utf8mb4)'))

    def test_c3_product_words_are_bound_and_changed_source_rejected(self):
        self.assertEqual(driver.validate_product_ui_contract()['bound_product_files'], 5)
        with patch.dict(driver.PRODUCT_UI_INPUTS,
                        {next(iter(driver.PRODUCT_UI_INPUTS)): '0' * 64}):
            with self.assertRaisesRegex(RuntimeError, 'product UI input differs'):
                driver.validate_product_ui_contract()

    def test_safe_failure_location_only_preserves_fixed_case_and_numbers(self):
        failed = reporter()
        failed['stats'] = {'expected': 1, 'unexpected': 1, 'skipped': 0, 'flaky': 0}
        failed['suites'][0]['specs'][0]['tests'][0]['results'] = [{
            'status': 'failed', 'errors': [{
                'location': {'file': str(driver.UI_SPEC), 'line': 56, 'column': 22},
                'message': 'synthetic-secret-in-message',
                'stack': 'synthetic-secret-in-stack',
            }],
        }]
        self.assertEqual(driver.safe_browser_failures(failed), [{
            'title': driver.UI_TITLES[0], 'status': 'failed', 'line': 56, 'column': 22,
        }])
        error = failed['suites'][0]['specs'][0]['tests'][0]['results'][0]['errors'][0]
        error['location'] = {'file': '/tmp/foreign.spec.ts', 'line': 1, 'column': 1}
        error['stack'] = f'synthetic-secret-in-stack\n at {driver.UI_SPEC}:62:7'
        self.assertEqual(driver.safe_browser_failures(failed)[0], {
            'title': driver.UI_TITLES[0], 'status': 'failed', 'line': 62, 'column': 7,
        })
        error['stack'] = 'at /tmp/foreign.spec.ts:62:7 synthetic-secret-in-stack'
        self.assertEqual(driver.safe_browser_failures(failed)[0], {
            'title': driver.UI_TITLES[0], 'status': 'failed',
        })

    def test_public_admin_env_and_local_override_fail_closed(self):
        with tempfile.TemporaryDirectory(prefix='ui-env-offline-', dir='/tmp/wta-t45') as tmp:
            root = Path(tmp)
            env = root / '.env.production'
            env.write_text('\n'.join((
                'VITE_APP_CONTEXT_PATH=/', 'VITE_APP_BASE_API=/prod-api',
                'VITE_APP_MESSAGE_ENABLED=false',
                'VITE_APP_CLIENT_ID=e5cd7e4891bf95d1d19206ce24a7b32e', '')))
            with patch.object(driver, 'ADMIN_APP_ROOT', root):
                self.assertEqual(driver.validate_public_admin_env(), driver.base_sha(env))
                (root / '.env.production.local').write_text('VITE_APP_BASE_API=/wrong\n')
                with self.assertRaisesRegex(RuntimeError, 'override exists'):
                    driver.validate_public_admin_env()
                (root / '.env.production.local').unlink()
                env.write_text(env.read_text().replace('/prod-api', '/wrong'))
                with self.assertRaisesRegex(RuntimeError, 'environment differs'):
                    driver.validate_public_admin_env()

    def test_exact_two_real_chrome_cases(self):
        value = driver.browser_report(reporter())
        self.assertEqual(value['counts']['expected'], 2)
        self.assertEqual(value['attempts'], 2)

    def test_skip_retry_wrong_source_and_case_fail_closed(self):
        changes = []
        skipped = reporter(); skipped['stats']['skipped'] = 1; changes.append(skipped)
        retry = reporter(); retry['suites'][0]['specs'][0]['tests'][0]['results'].append({'status': 'passed'}); changes.append(retry)
        wrong = reporter(); wrong['suites'][0]['file'] = '/tmp/unowned.spec.ts';
        wrong['suites'][0]['specs'][0]['file'] = '/tmp/unowned.spec.ts'; changes.append(wrong)
        title = reporter(); title['suites'][0]['specs'][0]['title'] = 'unowned title'; changes.append(title)
        for item in changes:
            with self.subTest(item=item['stats']):
                with self.assertRaises(RuntimeError):
                    driver.browser_report(item)

    def test_private_sources_and_browser_capture_protection(self):
        for label, source in (('spec', driver.UI_SPEC), ('config', driver.UI_CONFIG),
                              ('server', driver.UI_SERVER)):
            self.assertEqual(driver.base_sha(source), driver.UI_HASHES[label])
        content = driver.UI_CONFIG.read_text()
        for name in ('trace', 'video', 'screenshot'):
            self.assertIn(f"{name}: 'off'", content)
        spec_source = driver.UI_SPEC.read_text()
        for forbidden in ('storageState', 'recordHar', 'recordVideo', 'routeFromHAR'):
            self.assertNotIn(forbidden, spec_source)
        self.assertIn("'观察到允许'", spec_source)
        self.assertIn("'观察到拒绝'", spec_source)
        self.assertNotIn("'允许 / 可读'", spec_source)

    def test_dist_manifest_exact_inventory_and_wrong_hash(self):
        with tempfile.TemporaryDirectory(prefix='ui-offline-', dir='/tmp/wta-t45') as tmp:
            root = Path(tmp); dist = root / 'dist'; dist.mkdir()
            (dist / 'assets').mkdir()
            (dist / 'build-mode.json').write_text('{"app":"admin-web","mode":"production"}')
            (dist / 'index.html').write_text('<html></html>')
            for i in range(8): (dist / 'assets' / f'{i}.js').write_text(str(i))
            files = {item.relative_to(dist).as_posix(): driver.base_sha(item)
                     for item in dist.rglob('*') if item.is_file()}
            log = root / 'build.log'; log.write_text('synthetic local build proof\n'); log.chmod(0o600)
            head, tree = 'a' * 40, 'b' * 40
            manifest = {'schema_version': 1, 'source_head': head, 'source_tree': tree,
                        'dist_root': 'frontend/apps/admin-web/dist', 'build_exit_code': 0,
                        'build_log_path': str(log), 'build_log_sha256': driver.base_sha(log),
                        'files': files}
            name = root / 'manifest.json'
            with os.fdopen(os.open(name, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'w') as out:
                json.dump(manifest, out)
            with patch.object(driver, 'DIST_ROOT', dist):
                self.assertEqual(driver.validate_dist_manifest(name, head, tree)['file_count'], 10)
                (dist / 'assets' / '0.js').write_text('changed')
                with self.assertRaisesRegex(RuntimeError, 'inventory differs'):
                    driver.validate_dist_manifest(name, head, tree)


if __name__ == '__main__':
    unittest.main(verbosity=2)
