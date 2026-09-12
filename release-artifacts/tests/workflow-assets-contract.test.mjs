import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const releaseRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const workspaceRoot = path.resolve(releaseRoot, '..');
const workflowRoot = path.join(releaseRoot, 'workflow/leave');
const expected = {
  'leave1.json': '780272ec2221e0890893aa6d5147c4ff7ed5609eafa61397ea9711d87ee1df8d',
  'leave2.json': '736a93178517d94a3144921f7c23eff2cf56882ad81ecd6f423eacbcc3051754',
  'leave3.json': '341ca8345b0bd4ede919d93e8415735261282f897f180506e964027e40c5ffed',
  'leave4.json': 'bfaf26bbd8deca08ea0535ff0f123aa6a6b6a53e689e5e276bf9cdb7837307d7',
  'leave5.json': '09a5ac2039dab19e060fd64cf630083b210781a83615e61a43a92bffa144273e',
  'leave6.json': 'f93b616d9d531b2a171dbfde06f7fdd7ffe23db843eb9c3133cd27111653aea3',
};

test('the six leave workflow assets are exact, parseable, and tracked', () => {
  assert.deepEqual(fs.readdirSync(workflowRoot).sort(), Object.keys(expected));
  const tracked = execFileSync('git', ['ls-files', '--', 'release-artifacts/workflow/leave'], {
    cwd: workspaceRoot,
    encoding: 'utf8',
  });

  for (const [filename, expectedDigest] of Object.entries(expected)) {
    const bytes = fs.readFileSync(path.join(workflowRoot, filename));
    assert.doesNotThrow(() => JSON.parse(bytes.toString('utf8')), `${filename} must contain valid JSON`);
    assert.equal(
      crypto.createHash('sha256').update(bytes.toString('utf8').replace(/\r\n/g, '\n')).digest('hex'),
      expectedDigest,
    );
    assert.match(tracked, new RegExp(`${filename.replaceAll('.', '\\.')}\\n`));
  }
});
