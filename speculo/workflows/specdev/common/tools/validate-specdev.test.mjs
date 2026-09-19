import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, renameSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { basename, dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';
import { validateChange as validate } from './validate-specdev.mjs';

const validator = fileURLToPath(new URL('./validate-specdev.mjs', import.meta.url));
const statusTemplate = JSON.parse(readFileSync(new URL('../../I-init-setup/change-status-template.json', import.meta.url)));
const configTemplate = JSON.parse(readFileSync(new URL('../../I-init-setup/config-template.json', import.meta.url)));
const timestamp = '2026-09-18T00:00:00.000Z';

function validateChange(directory, stage = null) {
  const originalDirectory = process.cwd();
  try {
    // Resolve the synthetic legacy workspace without discovering the caller's repository.
    process.chdir(resolve(directory, '../../../..'));
    return validate(directory, stage);
  } finally {
    process.chdir(originalDirectory);
  }
}

function write(file, content) {
  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, content);
}
function artifact(file, meta, body = '') {
  write(file, `---\n${Object.entries(meta).map(([key, value]) => `${key}: ${JSON.stringify(value)}`).join('\n')}\n---\n\n${body}\n`);
}
function status(directory, work) {
  const value = structuredClone(statusTemplate);
  Object.assign(value, { change: basename(directory), current_work: work, created_at: timestamp, updated_at: timestamp });
  Object.assign(value.leadership, { current: 'test-lead', assigned_at: timestamp });
  write(join(directory, '.status.json'), JSON.stringify(value));
}
function workspace(t) {
  const root = mkdtempSync(join(tmpdir(), 'specdev-goal-routing-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  write(join(root, '.speculo/specdev/config.json'), JSON.stringify(configTemplate));
  return join(root, '.speculo/specdev/changes');
}
function single(root, name = '2026-09-18-single') {
  const directory = join(root, name);
  status(directory, 'specdev/goal-plan');
  artifact(join(directory, 'spec.md'), {
    schema_version: 3, artifact: 'spec', change: name, status: 'ready', ready_for_tickets: true,
    sources: ['USER-DECISION: test fixture'],
  }, `## 1. 问题与目标\nVerify goal routing.\n## 2. 解决方案与外部行为\nCheck artifacts.\n## 4. 验收合同\nAC-001\n## 5. 范围\nLocal fixture.\n## 9. 验证策略\nUnit tests.`);
  artifact(join(directory, 'tickets-map.md'), {
    schema_version: 3, artifact: 'tickets-map', change: name, status: 'ready', plan_contract_version: 1,
    plan_revision: 1, requested_deliverables: [], deliverable_policy: 'Verify routing without a requested artifact count.',
  }, `### 总体实施背景
Validate a fixture.
### 项目 Skill 读取矩阵
| Applies To | Project Skill | Trigger / Scope | Read Timing | Purpose |
|---|---|---|---|---|
| ALL | 无；已扫描fixture目录 | fixture only | before test | no project Skill in synthetic repository |
## 2. 执行清单
T-01
## 3. 依赖 DAG
T-01
## 4. 合同覆盖矩阵
AC-001 T-01
## 5. 并行与路径所有权
Single writer.
## 9. 总控与恢复
Read the fixture.`);
  artifact(join(directory, 'ticket/01-example.md'), {
    schema_version: 3, artifact: 'ticket', change: name, id: 'T-01', title: 'Routing fixture',
    status: 'ready', planning_depth: 'lite', planning_depth_reason: 'Unit fixture only', ready: true, risk: 'low',
    blocked_by: [], contract_ids: ['AC-001'], owner: 'test-lead', expected_changes: [`<Path>${name}/file.txt</Path>`],
    writable_paths: [`<Path>${name}/file.txt</Path>`], read_only_paths: [], shared_paths: [], shared_path_owners: [],
    plan_contract_version: 1, skill_scan: 'Synthetic repository scanned; no project skills.', skill_bindings: [], resource_claims: [],
  }, `## 1. 战略与来源
AC-001 routing fixture
## 2. 决策状态
### 未决问题
无。
## 3. 范围边界
Local fixture
## 4. 要构建什么
Validate goal routing
## 7. 路径访问契约
Single writer
## 8. 验证矩阵
| case | expected |
|---|---|
| E2E disposition: not-required, current-workspace | unit fixture passes |
## 10. 验收标准
- [ ] AC-001 passes
## 11. SKILL 调用计划
No skills in synthetic repository.
## 12. 停止、检查点与交付
No side effects.`);
  artifact(join(directory, 'goal-plan.md'), {
    schema_version: 6, artifact: 'goal-plan', change: name, status: 'blocked', modes: [],
    orchestration: 'lead-directed', lead: 'test-lead', implementation_agent_limit: 1, integration_attempt_limit: 3,
    ticket_workspace_policy: 'current', integration_gate: 'direct-parent', ready_for_execution: false,
  });
  return directory;
}
function parent(t) {
  const root = workspace(t);
  const members = ['2026-09-18-member-a', '2026-09-18-member-b'];
  for (const member of members) {
    const child = single(root, member);
    status(child, 'specdev/implement');
  }
  const directory = join(root, '2026-09-18-parent');
  status(directory, 'specdev/goal-plan');
  const map = {
    schema_version: 1, artifact: 'implementation-map', change: basename(directory), status: 'blocked', revision: 1,
    members, tasks: members.map(name => `${name}::T-01`), dependencies: [], serializations: [],
  };
  const mapBody = ['## 1. Members and Source Authority', '## 2. Composite Ticket Inventory',
    '## 3. Implementation Super-DAG', '## 4. Conflict and Serialization',
    '## 5. Contract and Path Coverage', '## 6. Revision Log'].join('\n');
  const plan = {
    schema_version: 1, artifact: 'implementation-plan', change: basename(directory), status: 'blocked', source_map_revision: 1,
    orchestration: 'lead-directed', lead: 'test-lead', implementation_agent_limit: 1, integration_attempt_limit: 3,
    ticket_workspace_policy: 'current', integration_gate: 'direct-parent', ready_for_execution: false,
  };
  const planBody = ['## 1. Outcome and Authority', '## 2. Ready Frontier and Waves',
    '## 3. Workspace and Dispatch Contract', '## 4. Repository Integration Queue',
    '## 5. Gates and Aggregate Verification', '## 6. Conflict, Drift and Recovery', '## 7. Progress and Decisions'].join('\n');
  artifact(join(directory, 'implementation-map.md'), map, mapBody);
  artifact(join(directory, 'implementation-plan.md'), plan, planBody);
  artifact(join(directory, 'tickets-map.md'), {
    schema_version: 1, artifact: 'goal-tickets-map', change: basename(directory),
    implementation_map: `<Path>{roots.state}/specdev/changes/${basename(directory)}/implementation-map.md</Path>`,
    implementation_plan: `<Path>{roots.state}/specdev/changes/${basename(directory)}/implementation-plan.md</Path>`,
  });
  return { directory, root, map, mapBody, plan, planBody };
}

test('single-change goal-plan accepts only its own Spec, Tickets Map, Tickets and Goal', t => {
  const directory = single(workspace(t));
  const result = validateChange(directory, 'goal-plan');
  assert.deepEqual(result.errors, []);
  const cli = spawnSync(process.execPath, [validator, '--stage', 'goal-plan', directory], {
    encoding: 'utf8', cwd: resolve(directory, '../../../..'),
  });
  assert.equal(cli.status, 0, cli.stderr);
});

test('single-change goal-plan rejects a missing Goal rather than silently accepting null', t => {
  const directory = single(workspace(t));
  rmSync(join(directory, 'goal-plan.md'));
  assert.deepEqual(validateChange(directory, 'goal-plan').errors, ['goal-plan stage requires goal-plan.md for a single change']);
});

test('single-change planning still rejects missing Tickets Map and Ticket directory', t => {
  const directory = single(workspace(t));
  rmSync(join(directory, 'tickets-map.md'));
  rmSync(join(directory, 'ticket'), { recursive: true });
  const errors = validateChange(directory, 'goal-plan').errors;
  assert.ok(errors.includes('missing Tickets Map'), errors.join('\n'));
  assert.ok(errors.includes('missing Ticket directory'), errors.join('\n'));
  assert.ok(!errors.some(error => /requires implementation-/.test(error)), errors.join('\n'));
});

test('parent accepts two ready members without inventing a single-change Goal', t => {
  const { directory } = parent(t);
  assert.deepEqual(validateChange(directory, 'goal-plan').errors, []);
});

for (const missing of ['implementation-map.md', 'implementation-plan.md']) {
  test(`parent rejects missing ${missing} even without a goal-map entry`, t => {
    const { directory } = parent(t);
    rmSync(join(directory, missing));
    rmSync(join(directory, 'tickets-map.md'));
    assert.deepEqual(validateChange(directory, 'goal-plan').errors, [`goal-plan stage requires ${missing}`]);
  });
}

test('parent goal-map entry cannot bypass both missing implementation artifacts', t => {
  const { directory } = parent(t);
  rmSync(join(directory, 'implementation-map.md'));
  rmSync(join(directory, 'implementation-plan.md'));
  assert.deepEqual(validateChange(directory, 'goal-plan').errors, [
    'goal-plan stage requires implementation-map.md', 'goal-plan stage requires implementation-plan.md',
    'tickets-map.md: goal-tickets-map missing real implementation-map.md',
    'tickets-map.md: goal-tickets-map missing real implementation-plan.md',
  ]);
});

test('parent member readiness is still enforced', t => {
  const { directory, root, map } = parent(t);
  const file = join(root, map.members[0], 'spec.md');
  write(file, readFileSync(file, 'utf8').replace('ready_for_tickets: true', 'ready_for_tickets: false'));
  assert.ok(validateChange(directory, 'goal-plan').errors.some(error => /Spec must have status=ready/.test(error)));
});

test('parent cycle detection remains active', t => {
  const { directory, map, mapBody } = parent(t);
  map.dependencies = [`${map.tasks[0]} <- ${map.tasks[1]}`, `${map.tasks[1]} <- ${map.tasks[0]}`];
  artifact(join(directory, 'implementation-map.md'), map, mapBody);
  assert.ok(validateChange(directory, 'goal-plan').errors.some(error => /cycle/i.test(error)));
});

test('parent configured agent cap remains active', t => {
  const { directory, plan, planBody } = parent(t);
  plan.implementation_agent_limit = 4;
  artifact(join(directory, 'implementation-plan.md'), plan, planBody);
  assert.ok(validateChange(directory, 'goal-plan').errors.some(error => /exceeds config max_implementation_agents/.test(error)));
});

test('partial parent remains invalid outside goal-plan stage', t => {
  const { directory } = parent(t);
  rmSync(join(directory, 'implementation-map.md'));
  assert.deepEqual(validateChange(directory).errors, [
    'goal-plan stage requires implementation-map.md',
    'tickets-map.md: goal-tickets-map missing real implementation-map.md',
  ]);
});

function gitFixture(t) {
  const directory = single(workspace(t));
  const root = resolve(directory, '../../../..');
  const git = (...args) => {
    const result = spawnSync('git', ['-C', root, ...args], { encoding: 'utf8' });
    assert.equal(result.status, 0, result.stderr);
    return result.stdout.trim();
  };
  git('init', '-b', 'main');
  git('config', 'user.name', 'SpecDev fixture');
  git('config', 'user.email', 'fixture@example.invalid');
  git('config', 'commit.gpgsign', 'false');
  const commit = message => {
    git('add', '.');
    git('commit', '-m', message);
    return git('rev-parse', 'HEAD');
  };
  const product = join(root, basename(directory), 'file.txt');
  write(product, 'baseline\n');
  const base = commit('fixture baseline');
  const ticketPath = join(directory, 'ticket/01-example.md');
  const ticketSource = readFileSync(ticketPath, 'utf8');
  const record = (entries, completed = false) => {
    const value = JSON.parse(readFileSync(join(directory, '.status.json'), 'utf8'));
    Object.assign(value, {
      worktrees: entries, change_status: completed ? 'completed' : 'active',
      current_work: completed ? null : 'specdev/implement', completed_at: completed ? timestamp : null,
    });
    write(join(directory, '.status.json'), JSON.stringify(value));
  };
  const finishTicket = (id = 'T-01', source = ticketSource) => {
    const path = id === 'T-01' ? ticketPath : join(directory, `ticket/${id.slice(2)}-example.md`);
    write(path, source.replace(/^status: .*$/m, 'status: "done"').replace(/^ready: .*$/m, 'ready: false'));
    if (id !== 'T-01') {
      const map = join(directory, 'tickets-map.md');
      write(map, readFileSync(map, 'utf8') + `\n${id}\n`);
    }
    const headings = ['## 2. Lead Dispatch And Candidate Return', '## 3. 修改范围与路径所有权',
      '## 4. 验收与合同映射', '## 5. Workspace Verification', '## 6. 双轴审查',
      '## 7. Integration Verification', '## 8. 偏差与决策', '## 9. 残余风险与交付定位'];
    write(join(directory, `evidence/${id}.md`), headings.map(heading => `${heading}\ncurrent-workspace\n`).join('\n'));
  };
  const entry = (before, result, id = 'T-01') => ({
    ticket_id: id, owner: 'test-lead', implementation_owner: 'test-lead', integration_owner: 'test-lead',
    provider: 'git', base_sha: before, parent_branch: 'main', branch: 'main', workspace_ref: 'current',
    source_checkpoint: result, status: 'integrated', updated_at: timestamp,
    integration: {
      status: 'passed', parent_ref: 'main', parent_before_sha: before, source_sha: result,
      candidate_sha: null, candidate_tree_sha: null, candidate_branch: null, candidate_workspace_ref: null,
      result_sha: result, method: 'direct-parent', conflict_paths: [], verification: 'passed',
      full_suite: { required: false, status: 'not-required', reason: 'Git fixture only', evidence: null },
      e2e: { required: false, status: 'not-required', reason: 'Git fixture only', evidence: null },
      evidence: `<Path>{roots.state}/specdev/changes/${basename(directory)}/evidence/${id}.md</Path>`,
      attempts: 1, promotion_status: 'applied',
    },
  });
  const implement = () => {
    write(product, 'implemented\n');
    return commit('T-01 implementation');
  };
  return { directory, root, git, commit, base, product, ticketPath, ticketSource, record, finishTicket, entry, implement,
    check: (stage = 'implement') => validate(directory, stage, root).errors };
}

test('current historical result survives tracked evidence, governance commits and the next Ticket edits', t => {
  const f = gitFixture(t);
  const result = f.implement();
  f.finishTicket();
  f.record([f.entry(f.base, result)]);
  assert.deepEqual(f.check(), []);
  f.commit('record acceptance without changing implementation SHA');
  assert.notEqual(f.git('rev-parse', 'HEAD'), result);
  assert.deepEqual(f.check(), []);
  write(f.product, 'next Ticket work\n');
  write(join(f.root, 'next-ticket-untracked.txt'), 'next Ticket fixture\n');
  assert.deepEqual(f.check(), []);
});

test('two serial current Tickets retain distinct immutable implementation results', t => {
  const f = gitFixture(t);
  const first = f.implement();
  f.finishTicket();
  f.record([f.entry(f.base, first)]);
  const before = f.commit('T-01 acceptance');
  const secondSource = f.ticketSource.replaceAll('T-01', 'T-02').replaceAll('/file.txt', '/second.txt');
  write(join(f.directory, 'ticket/02-example.md'), secondSource);
  write(join(f.root, basename(f.directory), 'second.txt'), 'second implementation\n');
  const second = f.commit('T-02 implementation');
  f.finishTicket('T-02', secondSource);
  f.record([f.entry(f.base, first), f.entry(before, second, 'T-02')]);
  f.commit('T-02 acceptance');
  assert.deepEqual(f.check(), []);
});

for (const checkpoint of ['main', 'short']) {
  test(`current execution rejects ${checkpoint} instead of an immutable full commit SHA`, t => {
    const f = gitFixture(t);
    const result = f.implement();
    f.finishTicket();
    f.record([f.entry(f.base, checkpoint === 'short' ? result.slice(0, 12) : checkpoint)]);
    assert.ok(f.check().some(error => /canonical full commit SHA/.test(error)));
  });
}

test('current history rejects a result absent from the parent branch', t => {
  const f = gitFixture(t);
  f.git('switch', '-c', 'unintegrated');
  const result = f.implement();
  f.git('switch', 'main');
  f.finishTicket();
  f.record([f.entry(f.base, result)]);
  assert.ok(f.check().some(error => /result_sha must be an ancestor of parent branch/.test(error)));
});

test('current history rejects a parent-before checkpoint after its result', t => {
  const f = gitFixture(t);
  const result = f.implement();
  write(f.product, 'later\n');
  const later = f.commit('later change');
  f.finishTicket();
  const entry = f.entry(f.base, result);
  entry.integration.parent_before_sha = later;
  f.record([entry]);
  assert.ok(f.check().some(error => /parent_before_sha must be an ancestor of result_sha/.test(error)));
});

for (const kind of ['same checkpoint', 'empty commit', 'Evidence only', 'outside writable paths']) {
  test(`current history rejects a non-implementation result: ${kind}`, t => {
    const f = gitFixture(t);
    let result = f.base;
    if (kind === 'empty commit') {
      f.git('commit', '--allow-empty', '-m', 'empty');
      result = f.git('rev-parse', 'HEAD');
    } else if (kind === 'Evidence only') {
      write(join(f.directory, 'evidence/T-01.md'), 'governance only\n');
      result = f.commit('evidence only');
    } else if (kind === 'outside writable paths') {
      write(join(f.root, 'unowned.txt'), 'unrelated change\n');
      result = f.commit('unrelated');
    }
    f.finishTicket();
    f.record([f.entry(f.base, result)]);
    assert.ok(f.check().some(error => /non-empty implementation diff/.test(error)));
  });
}

test('current Tickets cannot claim the same cumulative result for separate serial implementations', t => {
  const f = gitFixture(t);
  const first = f.implement();
  const secondSource = f.ticketSource.replaceAll('T-01', 'T-02').replaceAll('/file.txt', '/second.txt');
  write(join(f.root, basename(f.directory), 'second.txt'), 'second implementation\n');
  const second = f.commit('second product');
  f.finishTicket();
  f.finishTicket('T-02', secondSource);
  f.record([f.entry(f.base, second), f.entry(first, second, 'T-02')]);
  assert.ok(f.check().some(error => /current Ticket implementation intervals must be serial/.test(error)));
});

for (const kind of ['empty', 'governance']) {
  test(`current result cannot replace its product commit with a later ${kind} commit`, t => {
    const f = gitFixture(t);
    f.implement();
    if (kind === 'empty') f.git('commit', '--allow-empty', '-m', 'empty suffix');
    else {
      write(join(f.directory, 'evidence/T-01.md'), 'record product\n');
      f.commit('governance suffix');
    }
    f.finishTicket();
    f.record([f.entry(f.base, f.git('rev-parse', 'HEAD'))]);
    assert.ok(f.check().some(error => /non-empty implementation diff/.test(error)));
  });
}

test('current documentation Tickets have a real implementation without application source changes', t => {
  const f = gitFixture(t);
  const source = f.ticketSource.replaceAll(`${basename(f.directory)}/file.txt`, 'docs/guide.md');
  write(f.ticketPath, source);
  write(join(f.root, 'docs/guide.md'), 'Document the actual product contract.\n');
  const result = f.commit('documentation implementation');
  f.finishTicket('T-01', source);
  f.record([f.entry(f.base, result)]);
  assert.deepEqual(f.check(), []);
});

test('current result still must equal its implementation source checkpoint', t => {
  const f = gitFixture(t);
  const result = f.implement();
  f.finishTicket();
  const entry = f.entry(f.base, result);
  entry.integration.source_sha = f.base;
  f.record([entry]);
  assert.ok(f.check().some(error => /source_sha must equal source_checkpoint/.test(error)));
});

test('Git status read failures cannot stand in for a clean completion', t => {
  const f = gitFixture(t);
  const result = f.implement();
  f.finishTicket();
  f.record([f.entry(f.base, result)], true);
  f.commit('completed fixture');
  write(join(f.root, '.git/index'), 'invalid index');
  assert.ok(f.check('complete').includes('cannot read Git working tree status'));
});

test('completed change requires clean tracked and untracked files after the status commit', t => {
  const f = gitFixture(t);
  const result = f.implement();
  f.finishTicket();
  f.record([f.entry(f.base, result)], true);
  assert.ok(f.check('complete').some(error => /completed change requires a clean repository/.test(error)));
  f.commit('completed status and evidence');
  assert.deepEqual(f.check('complete'), []);
  write(join(f.root, 'untracked.txt'), 'untracked\n');
  assert.ok(f.check('complete').some(error => /completed change requires a clean repository/.test(error)));
  rmSync(join(f.root, 'untracked.txt'));
  write(f.product, 'dirty tracked\n');
  assert.ok(f.check('complete').some(error => /completed change requires a clean repository/.test(error)));
});

test('required workspace records retain their existing dirty repository gate', t => {
  const f = gitFixture(t);
  const result = f.implement();
  f.finishTicket();
  const entry = f.entry(f.base, result);
  entry.workspace_ref = `specdev-worktree/${basename(f.directory)}/T-01`;
  entry.branch = 'ticket-branch';
  Object.assign(entry.integration, { method: 'fast-forward', candidate_sha: result,
    candidate_tree_sha: f.git('rev-parse', `${result}^{tree}`), candidate_branch: `speculo/integration/${basename(f.directory)}/T-01`,
    candidate_workspace_ref: `specdev-worktree/.integration/${basename(f.directory)}/T-01` });
  f.record([entry]);
  const goal = join(f.directory, 'goal-plan.md');
  write(goal, readFileSync(goal, 'utf8').replace('ticket_workspace_policy: "current"', 'ticket_workspace_policy: "required"')
    .replace('integration_gate: "direct-parent"', 'integration_gate: "candidate-merge"'));
  assert.ok(f.check().some(error => /repository is dirty while Ticket is recorded as integrated/.test(error)));
  f.commit('required fixture record');
  assert.deepEqual(f.check(), []);
});

test('implement stage does not invent execution records for unstarted ready Tickets', t => {
  const directory = single(workspace(t));
  assert.deepEqual(validateChange(directory, 'implement').errors, []);
});

for (const phase of ['in_progress', 'review', 'blocked', 'deviated', 'done']) {
  test(`implement stage still requires an execution record for ${phase}`, t => {
    const directory = single(workspace(t));
    const path = join(directory, 'ticket/01-example.md');
    write(path, readFileSync(path, 'utf8').replace('status: "ready"', `status: "${phase}"`)
      .replace('ready: true', 'ready: false'));
    assert.ok(validateChange(directory, 'implement').errors.includes('T-01: Implement stage requires one Ticket workspace execution record'));
  });
}

function archivedFixture(t, { strict = true, governanceOnly = false } = {}) {
  const f = gitFixture(t);
  if (strict) {
    write(join(f.root, '.speculo/workspace.json'), JSON.stringify({
      schema_version: 1, path_base: 'project-root', roots: { state: '.speculo' },
    }));
  }
  let result;
  if (governanceOnly) {
    write(join(f.directory, 'evidence/T-01.md'), 'historical governance only\n');
    result = f.commit('not an implementation');
    f.finishTicket('T-01', f.ticketSource.replaceAll(`${basename(f.directory)}/file.txt`, '.speculo/specdev'));
  } else {
    result = f.implement();
    f.finishTicket();
  }
  f.record([f.entry(f.base, result)], true);
  f.commit('completed acceptance');
  const archive = join(f.root, '.speculo/specdev/archive/2026-09', basename(f.directory));
  mkdirSync(dirname(archive), { recursive: true });
  renameSync(f.directory, archive);
  const statePath = join(archive, '.status.json');
  const state = JSON.parse(readFileSync(statePath, 'utf8'));
  Object.assign(state, { change_status: 'archived', archived: true,
    archive_path: `<Path>{roots.state}/specdev/archive/2026-09/${basename(archive)}</Path>` });
  const save = () => write(statePath, JSON.stringify(state));
  save();
  f.commit('archive completed fixture');
  return { ...f, archive, state, save, check: () => validate(archive, 'complete', f.root).errors };
}

for (const strict of [false, true]) {
  test(`complete accepts an actual archived change in a ${strict ? 'declared' : 'legacy'} workspace`, t => {
    const f = archivedFixture(t, { strict });
    assert.deepEqual(f.check(), []);
    const cli = spawnSync(process.execPath, [validator, '--stage', 'complete', '--repo', f.root, f.archive], {
      encoding: 'utf8', cwd: f.root,
    });
    assert.equal(cli.status, 0, cli.stderr);
  });
}

test('an active-directory change cannot claim archived terminal state', t => {
  const f = archivedFixture(t);
  renameSync(f.archive, f.directory);
  f.commit('invalid archive state in active directory');
  assert.ok(validate(f.directory, 'complete', f.root).errors.some(error => /archived change must live/.test(error)));
});

test('archive month must follow the dated change name even when archive_path matches its location', t => {
  const f = archivedFixture(t);
  const wrongMonth = join(f.root, '.speculo/specdev/archive/2026-08', basename(f.archive));
  mkdirSync(dirname(wrongMonth), { recursive: true });
  renameSync(f.archive, wrongMonth);
  f.state.archive_path = `<Path>{roots.state}/specdev/archive/2026-08/${basename(wrongMonth)}</Path>`;
  write(join(wrongMonth, '.status.json'), JSON.stringify(f.state));
  f.commit('archive in a month inconsistent with change date');
  assert.ok(validate(wrongMonth, 'complete', f.root).errors.some(error => /archive month must match the change name/.test(error)));
});

for (const [label, update, expected] of [
  ['false archived flag', { archived: false }, /requires archived=true/],
  ['missing archive path', { archive_path: null }, /requires a rooted archive_path/],
  ['different archive month', { archive_path: '<Path>{roots.state}/specdev/archive/2026-08/2026-09-18-single</Path>' }, /archive_path must match/],
  ['different archive change', { archive_path: '<Path>{roots.state}/specdev/archive/2026-09/2026-09-18-other</Path>' }, /archive_path must match/],
]) {
  test(`archived complete rejects ${label}`, t => {
    const f = archivedFixture(t);
    Object.assign(f.state, update);
    f.save();
    f.commit('invalid archive metadata');
    assert.ok(f.check().some(error => expected.test(error)));
  });
}

test('archived complete still rejects an unfinished Ticket', t => {
  const f = archivedFixture(t);
  write(join(f.archive, 'ticket/01-example.md'), f.ticketSource);
  f.commit('unfinished archived Ticket');
  assert.ok(f.check().some(error => /planned Tickets remain unfinished/.test(error)));
});

test('archived complete still requires integrated Ticket evidence', t => {
  const f = archivedFixture(t);
  f.state.worktrees = [];
  f.save();
  f.commit('missing archived integration');
  assert.ok(f.check().some(error => /no integrated or removed Ticket worktree exists/.test(error)));
});

test('archived complete still authenticates recorded Git commits', t => {
  const f = archivedFixture(t);
  const missing = '1'.repeat(40);
  f.state.worktrees[0].source_checkpoint = missing;
  Object.assign(f.state.worktrees[0].integration, { source_sha: missing, result_sha: missing });
  f.save();
  f.commit('forged archived implementation');
  assert.ok(f.check().some(error => /not a resolvable Git commit/.test(error)));
});

test('archived complete retains the clean tracked and untracked repository gate', t => {
  const f = archivedFixture(t);
  write(f.product, 'dirty tracked\n');
  assert.ok(f.check().some(error => /archived change requires a clean repository/.test(error)));
  f.git('restore', '--', f.product);
  write(join(f.root, 'untracked.txt'), 'dirty untracked\n');
  assert.ok(f.check().some(error => /archived change requires a clean repository/.test(error)));
});

test('archive movement cannot turn historical governance into Ticket implementation', t => {
  const f = archivedFixture(t, { governanceOnly: true });
  assert.ok(f.check().some(error => /non-empty implementation diff/.test(error)));
});
