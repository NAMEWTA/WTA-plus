#!/usr/bin/env node

import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { spawnSync } from 'node:child_process';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

import {
  SyncError,
  advanceRequestedRefs,
  loadState,
  mergeTree,
  statePath,
} from './upstream-sync.mjs';

const SCRIPT_PATH = join(dirname(fileURLToPath(import.meta.url)), 'upstream-sync.mjs');

function command(cwd, ...args) {
  const result = spawnSync(args[0], args.slice(1), { cwd, encoding: 'utf8' });
  if (result.status !== 0) {
    throw new Error(`command failed (${result.status}) in ${cwd}: ${args.join(' ')}\nstdout=${result.stdout}\nstderr=${result.stderr}`);
  }
  return result.stdout.trim();
}

function runCli(...args) {
  return spawnSync(process.execPath, [SCRIPT_PATH, ...args], { encoding: 'utf8' });
}

function initRepo(path) {
  mkdirSync(path, { recursive: true });
  command(path, 'git', 'init', '-b', 'main');
  command(path, 'git', 'config', 'user.name', 'Skill Test');
  command(path, 'git', 'config', 'user.email', 'skill-test@example.invalid');
}

function commitFile(repo, relativePath, content, message) {
  const target = join(repo, relativePath);
  mkdirSync(dirname(target), { recursive: true });
  writeFileSync(target, content, 'utf8');
  command(repo, 'git', 'add', relativePath);
  command(repo, 'git', 'commit', '-m', message);
  return command(repo, 'git', 'rev-parse', 'HEAD');
}

function setTrackingRefs(repo, productSha, upstreamRef, upstreamSha) {
  command(repo, 'git', 'update-ref', 'refs/remotes/origin/main', productSha);
  command(repo, 'git', 'update-ref', upstreamRef, upstreamSha);
}

class WorkspaceFixture {
  constructor(root) {
    this.root = root;
    initRepo(root);
    this.parentSha = commitFile(root, 'parent.txt', 'parent\n', 'parent base');
    command(root, 'git', 'update-ref', 'refs/remotes/origin/main', this.parentSha);
    this.backend = join(root, 'backend');
    this.frontend = join(root, 'frontend');
    this.createBackend();
    this.createFrontend();
  }

  createBackend() {
    initRepo(this.backend);
    const base = commitFile(this.backend, 'base.txt', 'base\n', 'base');
    command(this.backend, 'git', 'tag', 'namewta-base-upstream-6x', base);
    command(this.backend, 'git', 'branch', '6.X', base);
    command(this.backend, 'git', 'switch', '6.X');
    this.backendUpstream1 = commitFile(this.backend, 'upstream-one.txt', 'one\n', 'upstream one');
    command(this.backend, 'git', 'switch', 'main');
    commitFile(this.backend, 'product.txt', 'product\n', 'product change');
    command(this.backend, 'git', 'merge', '--no-ff', this.backendUpstream1, '-m', 'merge upstream one');
    this.backendMerge = command(this.backend, 'git', 'rev-parse', 'HEAD');
    command(this.backend, 'git', 'switch', '6.X');
    this.backendUpstream2 = commitFile(this.backend, 'upstream-two.txt', 'two\n', 'upstream two');
    command(this.backend, 'git', 'switch', 'main');
    const productSha = command(this.backend, 'git', 'rev-parse', 'HEAD');
    setTrackingRefs(this.backend, productSha, 'refs/remotes/upstream/6.X', this.backendUpstream2);
  }

  createFrontend() {
    initRepo(this.frontend);
    this.frontendBase = commitFile(this.frontend, 'base.txt', 'base\n', 'base');
    command(this.frontend, 'git', 'tag', 'namewta-base-upstream-6x-vue', this.frontendBase);
    command(this.frontend, 'git', 'branch', '6.X-Vue', this.frontendBase);
    const productSha = commitFile(this.frontend, 'product.txt', 'product\n', 'product change');
    setTrackingRefs(this.frontend, productSha, 'refs/remotes/upstream/6.X-Vue', this.frontendBase);
  }
}

function withFixture(t) {
  const temporary = mkdtempSync(join(tmpdir(), 'upstream-sync-test-'));
  t.after(() => rmSync(temporary, { recursive: true, force: true }));
  const root = join(temporary, 'workspace');
  return new WorkspaceFixture(root);
}

test('dry-run derives graph merge and merge base', (t) => {
  const fixture = withFixture(t);
  const result = runCli('assess', '--root', fixture.root, '--topic', 'fixture', '--date', '2026-08-24', '--dry-run');
  assert.equal(result.status, 0, result.stderr);
  const snapshot = JSON.parse(result.stdout);
  const backend = snapshot.repositories.backend;
  const frontend = snapshot.repositories.frontend;
  assert.equal(backend.last_confirmed_integration.upstream_sha, fixture.backendUpstream1);
  assert.equal(backend.last_confirmed_integration.product_merge_commit_sha, fixture.backendMerge);
  assert.equal(backend.last_confirmed_integration.source, 'graph_merge');
  assert.deepEqual(backend.upstream_commits.map((item) => item.sha), [fixture.backendUpstream2]);
  assert.equal(frontend.last_confirmed_integration.source, 'derived_merge_base');
  assert.equal(frontend.last_confirmed_integration.upstream_sha, fixture.frontendBase);
});

test('assess replaces current report and compact global state', (t) => {
  const fixture = withFixture(t);
  for (let run = 0; run < 2; run += 1) {
    const result = runCli('assess', '--root', fixture.root, '--topic', 'fixture', '--date', '2026-08-24');
    assert.equal(result.status, 0, result.stderr);
    const reportDir = JSON.parse(result.stdout).report_dir;
    assert.equal(reportDir, join(fixture.root, 'docs/upstream/current'));
    const changeState = JSON.parse(readFileSync(join(reportDir, 'state.json'), 'utf8'));
    assert.deepEqual(Object.keys(changeState).sort(), ['created_at', 'repositories']);
    assert.deepEqual(changeState.repositories, {
      backend: { upstream_sha: fixture.backendUpstream2, main_merge_sha: null },
    });
    assert.match(readFileSync(join(reportDir, 'diff_report.md'), 'utf8'), new RegExp(fixture.backendUpstream2));
    assert.match(readFileSync(join(reportDir, 'diff_report.md'), 'utf8'), /## 现状 Merge 清单/);
    assert.match(readFileSync(join(reportDir, 'conflict_report.md'), 'utf8'), /Git 确认冲突数 \| `0`/);
  }
  const globalPath = join(fixture.root, 'docs/upstream/upstream-sync-state.json');
  const state = JSON.parse(readFileSync(globalPath, 'utf8'));
  assert.equal(state.schema_version, 2);
  assert.equal(state.current_change, 'current');
  assert.ok(!('runs' in state));
  assert.ok(!('integration_events' in state));
  assert.ok(statSync(globalPath).size < 2_000);
  assert.equal(state.repositories.backend.integrated_upstream_sha, fixture.backendUpstream1);
  assert.deepEqual(Object.keys(state.repositories.backend).sort(), [
    'integrated_upstream_sha', 'main_merge_sha', 'observed_upstream_sha',
  ]);
});

test('record-integration requires reachable exact merge parent and updates both states', (t) => {
  const fixture = withFixture(t);
  const first = runCli('assess', '--root', fixture.root, '--topic', 'record', '--date', '2026-08-24');
  assert.equal(first.status, 0, first.stderr);
  command(fixture.backend, 'git', 'merge', '--no-ff', fixture.backendUpstream2, '-m', 'merge upstream two');
  const mergeSha = command(fixture.backend, 'git', 'rev-parse', 'HEAD');
  const result = runCli(
    'record-integration', '--root', fixture.root, '--repository', 'backend',
    '--merge-commit', mergeSha, '--upstream-sha', fixture.backendUpstream2,
    '--verification', 'tests: exit 0',
  );
  assert.equal(result.status, 0, result.stderr);
  const state = JSON.parse(readFileSync(join(fixture.root, 'docs/upstream/upstream-sync-state.json'), 'utf8'));
  assert.equal(state.repositories.backend.integrated_upstream_sha, fixture.backendUpstream2);
  assert.equal(state.repositories.backend.main_merge_sha, mergeSha);
  assert.ok(!('integration_events' in state));
  const changeState = JSON.parse(readFileSync(join(fixture.root, 'docs/upstream/current/state.json'), 'utf8'));
  assert.deepEqual(changeState.repositories.backend, { upstream_sha: fixture.backendUpstream2, main_merge_sha: mergeSha });
});

test('loadState migrates schema v1 to compact v2', (t) => {
  const fixture = withFixture(t);
  const path = join(fixture.root, 'legacy-state.json');
  writeFileSync(path, JSON.stringify({
    schema_version: 1,
    updated_at: '2026-08-24T12:00:00+08:00',
    repositories: {
      backend: {
        last_confirmed_integration: {
          upstream_sha: fixture.backendUpstream1,
          product_merge_commit_sha: fixture.backendMerge,
        },
        last_observation: { observed_upstream_sha: fixture.backendUpstream2 },
      },
    },
    runs: [{ report_dir: 'docs/upstream/2026-08-24_legacy', workspace: { dirty_paths: ['large-list'] } }],
    integration_events: [{ large: 'history' }],
  }), 'utf8');
  const state = loadState(path);
  assert.equal(state.schema_version, 2);
  assert.equal(state.current_change, 'current');
  assert.deepEqual(state.repositories.backend, {
    integrated_upstream_sha: fixture.backendUpstream1,
    main_merge_sha: fixture.backendMerge,
    observed_upstream_sha: fixture.backendUpstream2,
  });
  assert.ok(!('runs' in state));
});

test('mergeTree reports a content conflict', (t) => {
  const fixture = withFixture(t);
  const repo = join(dirname(fixture.root), 'conflict');
  initRepo(repo);
  const base = commitFile(repo, 'shared.txt', 'base\n', 'base');
  command(repo, 'git', 'branch', 'upstream', base);
  commitFile(repo, 'shared.txt', 'product\n', 'product');
  const productSha = command(repo, 'git', 'rev-parse', 'HEAD');
  command(repo, 'git', 'switch', 'upstream');
  const upstreamSha = commitFile(repo, 'shared.txt', 'upstream\n', 'upstream');
  const result = mergeTree(repo, productSha, upstreamSha);
  assert.equal(result.status, 'conflicted');
  assert.equal(result.exit_code, 1);
  assert.deepEqual(result.conflict_paths, ['shared.txt']);
});

test('mirror advances only by fast-forward', (t) => {
  const fixture = withFixture(t);
  command(fixture.backend, 'git', 'branch', '-f', '6.X', fixture.backendUpstream1);
  const results = advanceRequestedRefs(fixture.root, true, false);
  assert.equal(command(fixture.backend, 'git', 'rev-parse', 'refs/heads/6.X'), fixture.backendUpstream2);
  assert.equal(results.find((item) => item.label === 'backend mirror').status, 'advanced');
});

test('product advance refuses a dirty checked-out branch', (t) => {
  const fixture = withFixture(t);
  const productSha = command(fixture.backend, 'git', 'rev-parse', 'refs/heads/main');
  const treeSha = command(fixture.backend, 'git', 'rev-parse', `${productSha}^{tree}`);
  const originSha = command(fixture.backend, 'git', 'commit-tree', treeSha, '-p', productSha, '-m', 'origin ahead');
  command(fixture.backend, 'git', 'update-ref', 'refs/remotes/origin/main', originSha);
  writeFileSync(join(fixture.backend, 'dirty.txt'), 'dirty\n', 'utf8');
  assert.throws(() => advanceRequestedRefs(fixture.root, false, true), SyncError);
  assert.equal(command(fixture.backend, 'git', 'rev-parse', 'refs/heads/main'), productSha);
});

test('diverged mirror stops before ref updates', (t) => {
  const fixture = withFixture(t);
  const base = command(fixture.backend, 'git', 'rev-parse', 'namewta-base-upstream-6x');
  const treeSha = command(fixture.backend, 'git', 'rev-parse', `${base}^{tree}`);
  const divergentSha = command(fixture.backend, 'git', 'commit-tree', treeSha, '-p', base, '-m', 'diverged mirror');
  command(fixture.backend, 'git', 'update-ref', 'refs/heads/6.X', divergentSha);
  assert.throws(() => advanceRequestedRefs(fixture.root, true, false), SyncError);
  assert.equal(command(fixture.backend, 'git', 'rev-parse', 'refs/heads/6.X'), divergentSha);
});

test('state path cannot escape repository', (t) => {
  const fixture = withFixture(t);
  assert.throws(() => statePath(fixture.root, '../outside.json'), SyncError);
});
