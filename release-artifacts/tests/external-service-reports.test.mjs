import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, mkdirSync, rmSync, utimesSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
for (const scenario of ['success', 'missing', 'stale', 'zero', 'skipped', 'failure', 'error', 'duplicate']) {
  test(`external-service report gate: ${scenario}`, () => {
    const directory = mkdtempSync(path.join(tmpdir(), 'namewta-test-reports-'));
    try {
      const since = BigInt(Date.now() - 1000) * 1000000n;
      if (scenario !== 'missing') {
        for (const module of scenario === 'duplicate' ? ['one', 'two'] : ['one']) {
          const target = path.join(directory, module, 'target/surefire-reports');
          mkdirSync(target, { recursive: true });
          const report = path.join(target, 'TEST-example.RequiredTest.xml');
          writeFileSync(report, `<testsuite tests="${scenario === 'zero' ? 0 : 2}" failures="${scenario === 'failure' ? 1 : 0}" errors="${scenario === 'error' ? 1 : 0}" skipped="${scenario === 'skipped' ? 1 : 0}"/>`);
          if (scenario === 'stale') utimesSync(report, new Date(0), new Date(0));
        }
      }
      const result = spawnSync('python3', ['scripts/ci/verify-external-tests.py', directory, String(since), 'RequiredTest'],
        { cwd: root, encoding: 'utf8' });
      assert.equal(result.status === 0, scenario === 'success', result.stdout + result.stderr);
    } finally { rmSync(directory, { recursive: true, force: true }); }
  });
}
