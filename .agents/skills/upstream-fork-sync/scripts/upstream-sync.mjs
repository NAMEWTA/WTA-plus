#!/usr/bin/env node

import {
  closeSync,
  existsSync,
  fsyncSync,
  mkdirSync,
  openSync,
  readFileSync,
  renameSync,
  statSync,
  unlinkSync,
  writeFileSync,
} from 'node:fs';
import { basename, dirname, isAbsolute, join, relative, resolve as resolvePath } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

export const SCHEMA_VERSION = 2;
const STATE_RELATIVE_PATH = 'docs/upstream/upstream-sync-state.json';
export const REPOSITORIES = {
  backend: {
    path: 'backend',
    productRef: 'refs/heads/main',
    originProductRef: 'refs/remotes/origin/main',
    upstreamRef: 'refs/remotes/upstream/6.X',
    mirrorRef: 'refs/heads/6.X',
    baselineTagRef: 'refs/tags/namewta-base-upstream-6x',
  },
  frontend: {
    path: 'frontend',
    productRef: 'refs/heads/main',
    originProductRef: 'refs/remotes/origin/main',
    upstreamRef: 'refs/remotes/upstream/6.X-Vue',
    mirrorRef: 'refs/heads/6.X-Vue',
    baselineTagRef: 'refs/tags/namewta-base-upstream-6x-vue',
  },
};

const RISK_RULES = [
  ['authentication/session', /(^|\/)([^/]*(Auth|Login|Register|Session|SaToken)[^/]*|(auth|login|register|session|satoken)([._/-]|$))/],
  ['Client/RBAC/menu', /(^|\/)([^/]*(Client|Role|Permission|Menu|UserType|SysUser)[^/]*|(client|role|permission|menu|user|userType)([._/-]|$))/],
  ['OSS/upload', /(^|\/)([^/]*(OSS|Oss|Upload|FileUpload|ImageUpload)[^/]*|(oss|upload|fileupload|imageupload)([._/-]|$))/],
  ['notification', /(^|\/)([^/]*(Notify|Notice|Mail|SMS|Sms)[^/]*|(notify|notice|mail|sms)([._/-]|$))/],
  ['workflow', /(^|\/)([^/]*(Workflow|WarmFlow)[^/]*|(workflow|warmflow)([._/-]|$))/],
  ['SQL/data migration', /(^|\/)(script\/sql|[^/]+\.sql$)/i],
  ['build/dependency', /(^|\/)(pom\.xml|package\.json|pnpm-lock\.yaml|mvnw|\.mvn)(\/|$)/i],
];

export class SyncError extends Error {}

function nowRfc3339() {
  const date = new Date();
  const pad = (value) => String(value).padStart(2, '0');
  const offsetMinutes = -date.getTimezoneOffset();
  const sign = offsetMinutes >= 0 ? '+' : '-';
  const absoluteOffset = Math.abs(offsetMinutes);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
    + `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    + `${sign}${pad(Math.floor(absoluteOffset / 60))}:${pad(absoluteOffset % 60)}`;
}

function runCommand(cwd, command, args, { allowed = [0], timeout = 120_000 } = {}) {
  const result = spawnSync(command, args, {
    cwd,
    encoding: 'utf8',
    maxBuffer: 64 * 1024 * 1024,
    timeout,
  });
  if (result.error) {
    const reason = result.error.code === 'ETIMEDOUT' ? 'command timed out' : result.error.message;
    throw new SyncError(`${reason} in ${cwd}: ${[command, ...args].join(' ')}`);
  }
  const status = result.status ?? 1;
  if (!allowed.includes(status)) {
    const detail = (result.stderr || result.stdout || 'no output').trim();
    throw new SyncError(`command failed (${status}) in ${cwd}: ${[command, ...args].join(' ')}\n${detail}`);
  }
  return { returncode: status, stdout: result.stdout ?? '', stderr: result.stderr ?? '' };
}

function git(repo, ...args) {
  let options = {};
  if (args.length && typeof args.at(-1) === 'object') options = args.pop();
  return runCommand(repo, 'git', args, options);
}

function ensureRepository(path) {
  if (git(path, 'rev-parse', '--is-inside-work-tree').stdout.trim() !== 'true') {
    throw new SyncError(`not a Git worktree: ${path}`);
  }
}

export function resolveCommit(repo, ref, required = true) {
  const result = git(repo, 'rev-parse', '--verify', `${ref}^{commit}`, { allowed: [0, 128] });
  if (result.returncode === 0) return result.stdout.trim();
  if (required) throw new SyncError(`missing commit ref in ${repo}: ${ref}`);
  return null;
}

function isAncestor(repo, ancestor, descendant) {
  return git(repo, 'merge-base', '--is-ancestor', ancestor, descendant, { allowed: [0, 1] }).returncode === 0;
}

function uniqueMergeBase(repo, left, right) {
  const bases = git(repo, 'merge-base', '--all', left, right).stdout.split('\n').filter(Boolean);
  if (bases.length !== 1) {
    throw new SyncError(`expected one merge base in ${repo}, found ${bases.length} for ${left} and ${right}`);
  }
  return bases[0];
}

function commitParents(repo, commit) {
  const output = git(repo, 'show', '-s', '--format=%P', commit).stdout.trim();
  return output ? output.split(/\s+/) : [];
}

export function dirtyPaths(repo) {
  const tokens = git(repo, 'status', '--porcelain=v1', '-z').stdout.split('\0');
  const paths = new Set();
  for (let index = 0; index < tokens.length;) {
    const token = tokens[index++];
    if (!token || token.length < 4) continue;
    const status = token.slice(0, 2);
    paths.add(token.slice(3));
    if ((status.includes('R') || status.includes('C')) && tokens[index]) paths.add(tokens[index++]);
  }
  return [...paths].sort();
}

function revCounts(repo, left, right) {
  const values = git(repo, 'rev-list', '--left-right', '--count', `${left}...${right}`).stdout.trim().split(/\s+/);
  if (values.length !== 2) throw new SyncError(`unexpected rev-list count output in ${repo}`);
  return { left_only: Number(values[0]), right_only: Number(values[1]) };
}

function fetchRemote(repo, remote, tags) {
  const args = ['fetch', '--prune', ...(tags ? ['--tags'] : []), remote];
  const result = git(repo, ...args, { allowed: Array.from({ length: 256 }, (_, i) => i), timeout: 180_000 });
  return result.returncode === 0
    ? [true, null]
    : [false, (result.stderr || result.stdout || `exit ${result.returncode}`).trim()];
}

function refreshRefs(root) {
  const results = {};
  const [parentOk, parentError] = fetchRemote(root, 'origin', false);
  results.workspace = { origin: parentOk, origin_error: parentError };
  for (const [repository, config] of Object.entries(REPOSITORIES)) {
    const repo = join(root, config.path);
    const [originOk, originError] = fetchRemote(repo, 'origin', false);
    const [upstreamOk, upstreamError] = fetchRemote(repo, 'upstream', true);
    results[repository] = {
      origin: originOk,
      origin_error: originError,
      upstream: upstreamOk,
      upstream_error: upstreamError,
    };
  }
  return results;
}

function checkedOutWorktree(repo, branchRef) {
  let currentPath = null;
  for (const line of git(repo, 'worktree', 'list', '--porcelain').stdout.split('\n')) {
    if (line.startsWith('worktree ')) currentPath = line.slice('worktree '.length);
    else if (line === `branch ${branchRef}` && currentPath) return currentPath;
    else if (!line) currentPath = null;
  }
  return null;
}

function preflightAdvances(items) {
  return items.map(({ repo, localRef, targetRef, label }) => {
    const localSha = resolveCommit(repo, localRef);
    const targetSha = resolveCommit(repo, targetRef);
    if (localSha === targetSha) return { label, status: 'already-current', repo };
    if (!isAncestor(repo, localSha, targetSha)) {
      throw new SyncError(`ref is not fast-forwardable for ${label}: ${localRef}=${localSha}, ${targetRef}=${targetSha}`);
    }
    const worktree = checkedOutWorktree(repo, localRef);
    if (worktree && dirtyPaths(worktree).length) {
      throw new SyncError(`checked-out branch is dirty for ${label}: ${worktree}`);
    }
    return { label, status: 'pending', repo, localRef, targetRef, localSha, targetSha, worktree };
  });
}

function applyAdvances(actions) {
  return actions.map((action) => {
    if (action.status === 'already-current') return { label: action.label, status: action.status };
    if (action.worktree) git(action.worktree, 'merge', '--ff-only', action.targetRef);
    else git(action.repo, 'update-ref', action.localRef, action.targetSha, action.localSha);
    return { label: action.label, status: 'advanced', from: action.localSha, to: action.targetSha };
  });
}

export function advanceRequestedRefs(root, mirrors, products) {
  const items = [];
  if (mirrors) {
    for (const [repository, config] of Object.entries(REPOSITORIES)) {
      items.push({ repo: join(root, config.path), localRef: config.mirrorRef, targetRef: config.upstreamRef, label: `${repository} mirror` });
    }
  }
  if (products) {
    items.push({ repo: root, localRef: 'refs/heads/main', targetRef: 'refs/remotes/origin/main', label: 'workspace product' });
    for (const [repository, config] of Object.entries(REPOSITORIES)) {
      items.push({ repo: join(root, config.path), localRef: config.productRef, targetRef: config.originProductRef, label: `${repository} product` });
    }
  }
  return applyAdvances(preflightAdvances(items));
}

function bootstrapIntegration(repo, productSha, upstreamSha, baselineTagSha, timestamp) {
  const merges = git(repo, 'rev-list', '--first-parent', '--merges', `${baselineTagSha}..${productSha}`).stdout.split('\n').filter(Boolean);
  for (const mergeCommit of merges) {
    for (const parent of commitParents(repo, mergeCommit).slice(1)) {
      if (isAncestor(repo, parent, upstreamSha)) {
        return { upstream_sha: parent, product_merge_commit_sha: mergeCommit, source: 'graph_merge', confirmed_at: timestamp };
      }
    }
  }
  return {
    upstream_sha: uniqueMergeBase(repo, productSha, upstreamSha),
    product_merge_commit_sha: null,
    source: 'derived_merge_base',
    confirmed_at: timestamp,
  };
}

function validateSavedIntegration(repo, integration, productSha, upstreamSha) {
  const integratedSha = integration.upstream_sha;
  if (typeof integratedSha !== 'string' || !integratedSha) throw new SyncError(`malformed saved integration in ${repo}`);
  resolveCommit(repo, integratedSha);
  if (!isAncestor(repo, integratedSha, productSha)) throw new SyncError(`saved upstream checkpoint is no longer in product history in ${repo}: ${integratedSha}`);
  if (!isAncestor(repo, integratedSha, upstreamSha)) throw new SyncError(`observed upstream history does not contain saved checkpoint in ${repo}: ${integratedSha} !<= ${upstreamSha}`);
  if (integration.product_merge_commit_sha) {
    resolveCommit(repo, integration.product_merge_commit_sha);
    if (!isAncestor(repo, integration.product_merge_commit_sha, productSha)) {
      throw new SyncError(`saved integration merge is no longer in product history in ${repo}: ${integration.product_merge_commit_sha}`);
    }
  }
}

function parseCommits(repo, start, end) {
  if (start === end) return [];
  const format = '%H%x1f%h%x1f%ad%x1f%s';
  return git(repo, 'log', '--reverse', `--format=${format}`, '--date=short', `${start}..${end}`).stdout
    .split('\n').filter(Boolean).map((line) => {
      const [sha, short_sha, date, subject] = line.split('\x1f', 4);
      return { sha, short_sha, date, subject };
    });
}

function parseNameStatus(repo, start, end) {
  if (start === end) return [];
  const tokens = git(repo, 'diff', '--name-status', '-z', '--find-renames', start, end).stdout.split('\0');
  const files = [];
  for (let index = 0; index < tokens.length;) {
    const status = tokens[index++];
    if (!status) continue;
    const firstPath = tokens[index++];
    if (status.startsWith('R') || status.startsWith('C')) files.push({ status, old_path: firstPath, path: tokens[index++] });
    else files.push({ status, old_path: null, path: firstPath });
  }
  return files;
}

function parseNumstat(repo, start, end) {
  if (start === end) return {};
  const tokens = git(repo, 'diff', '--numstat', '-z', '--find-renames', start, end).stdout.split('\0');
  const stats = {};
  for (let index = 0; index < tokens.length;) {
    const token = tokens[index++];
    if (!token) continue;
    const [additions, deletions, path] = token.split('\t', 3);
    if (path) stats[path] = { additions, deletions };
    else {
      index += 1;
      const newPath = tokens[index++];
      stats[newPath] = { additions, deletions };
    }
  }
  return stats;
}

function riskCategories(path) {
  return RISK_RULES.filter(([, pattern]) => pattern.test(path)).map(([name]) => name);
}

export function mergeTree(repo, productSha, upstreamSha) {
  const result = git(repo, 'merge-tree', '--write-tree', '--messages', productSha, upstreamSha, { allowed: [0, 1] });
  const lines = result.stdout.split('\n');
  const treeSha = /^[0-9a-f]{40,64}$/.test(lines[0]?.trim()) ? lines[0].trim() : null;
  const conflictPaths = new Set();
  const messages = [];
  for (const line of lines.slice(1)) {
    const match = line.match(/^\d{6} [0-9a-f]{40,64} [123]\t(.+)$/);
    if (match) conflictPaths.add(match[1]);
    else if (line.startsWith('CONFLICT') || line.startsWith('Auto-merging')) messages.push(line);
  }
  let status = result.returncode === 0 ? 'clean' : 'conflicted';
  if (result.returncode === 1 && !conflictPaths.size) status = 'conflicted-unparsed';
  return { status, exit_code: result.returncode, tree_sha: treeSha, conflict_paths: [...conflictPaths].sort(), messages };
}

function validateCompactRepository(path, repository, item) {
  const keys = Object.keys(item ?? {}).sort().join(',');
  if (!(repository in REPOSITORIES) || keys !== 'integrated_upstream_sha,main_merge_sha,observed_upstream_sha') {
    throw new SyncError(`malformed repository state in ${path}: ${repository}`);
  }
  if (typeof item.integrated_upstream_sha !== 'string' || typeof item.observed_upstream_sha !== 'string'
    || (item.main_merge_sha !== null && typeof item.main_merge_sha !== 'string')) {
    throw new SyncError(`malformed repository SHA state in ${path}: ${repository}`);
  }
}

export function loadState(path) {
  if (!existsSync(path)) return { schema_version: SCHEMA_VERSION, updated_at: null, current_change: null, repositories: {} };
  let data;
  try { data = JSON.parse(readFileSync(path, 'utf8')); }
  catch (error) { throw new SyncError(`cannot read state file ${path}: ${error.message}`); }
  if (data.schema_version === 1) {
    const lastRun = Array.isArray(data.runs) ? data.runs.at(-1) : null;
    const repositories = {};
    for (const [repository, item] of Object.entries(data.repositories ?? {})) {
      const integrated = item?.last_confirmed_integration?.upstream_sha;
      const observed = item?.last_observation?.observed_upstream_sha;
      if (integrated && observed) repositories[repository] = {
        integrated_upstream_sha: integrated,
        main_merge_sha: item.last_confirmed_integration.product_merge_commit_sha ?? null,
        observed_upstream_sha: observed,
      };
    }
    data = {
      schema_version: SCHEMA_VERSION,
      updated_at: data.updated_at ?? null,
      current_change: typeof lastRun?.report_dir === 'string' ? 'current' : null,
      repositories,
    };
  }
  if (data.schema_version !== SCHEMA_VERSION || typeof data.repositories !== 'object' || Array.isArray(data.repositories)) {
    throw new SyncError(`unsupported or malformed state schema in ${path}: ${data.schema_version}`);
  }
  if (Object.keys(data).sort().join(',') !== 'current_change,repositories,schema_version,updated_at') {
    throw new SyncError(`malformed state file ${path}: unexpected top-level fields`);
  }
  if (data.updated_at !== null && typeof data.updated_at !== 'string') throw new SyncError(`malformed state file ${path}: updated_at`);
  if (data.current_change !== null && typeof data.current_change !== 'string') throw new SyncError(`malformed state file ${path}: current_change`);
  for (const [repository, item] of Object.entries(data.repositories)) validateCompactRepository(path, repository, item);
  return data;
}

function loadChangeState(path) {
  let data;
  try { data = JSON.parse(readFileSync(path, 'utf8')); }
  catch (error) { throw new SyncError(`cannot read change state file ${path}: ${error.message}`); }
  if (Object.keys(data).sort().join(',') !== 'created_at,repositories' || typeof data.created_at !== 'string'
    || typeof data.repositories !== 'object' || Array.isArray(data.repositories)) throw new SyncError(`malformed change state file: ${path}`);
  for (const [repository, item] of Object.entries(data.repositories)) {
    if (!(repository in REPOSITORIES) || Object.keys(item ?? {}).sort().join(',') !== 'main_merge_sha,upstream_sha'
      || typeof item.upstream_sha !== 'string' || (item.main_merge_sha !== null && typeof item.main_merge_sha !== 'string')) {
      throw new SyncError(`malformed change repository state: ${repository}`);
    }
  }
  return data;
}

function atomicWriteText(path, content) {
  mkdirSync(dirname(path), { recursive: true });
  const temporary = join(dirname(path), `.${basename(path)}.${process.pid}.${Date.now()}.tmp`);
  let descriptor;
  try {
    descriptor = openSync(temporary, 'wx');
    writeFileSync(descriptor, content, 'utf8');
    fsyncSync(descriptor);
    closeSync(descriptor);
    descriptor = undefined;
    renameSync(temporary, path);
  } finally {
    if (descriptor !== undefined) closeSync(descriptor);
    if (existsSync(temporary)) unlinkSync(temporary);
  }
}

function atomicWriteJson(path, data) {
  atomicWriteText(path, `${JSON.stringify(data, null, 2)}\n`);
}

function sanitizeTopic(topic) {
  const normalized = topic.trim().toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
  if (!normalized) throw new SyncError('topic must contain ASCII letters or digits');
  return normalized;
}

function currentReportDir(root) {
  return join(root, 'docs/upstream/current');
}

function changedPathSet(repo, start, end) {
  return new Set(parseNameStatus(repo, start, end).map((item) => item.path));
}

function collectRepository(root, repository, config, state, freshness, fetchError, timestamp) {
  const repo = join(root, config.path);
  ensureRepository(repo);
  const productSha = resolveCommit(repo, config.productRef);
  const originProductSha = resolveCommit(repo, config.originProductRef, false);
  const upstreamSha = resolveCommit(repo, config.upstreamRef);
  const mirrorSha = resolveCommit(repo, config.mirrorRef);
  const baselineTagSha = resolveCommit(repo, config.baselineTagRef);
  const saved = state.repositories[repository];
  const integration = saved
    ? {
        upstream_sha: saved.integrated_upstream_sha,
        product_merge_commit_sha: saved.main_merge_sha,
        source: saved.main_merge_sha ? 'recorded_merge' : 'derived_merge_base',
        confirmed_at: state.updated_at,
      }
    : bootstrapIntegration(repo, productSha, upstreamSha, baselineTagSha, timestamp);
  if (saved) validateSavedIntegration(repo, integration, productSha, upstreamSha);
  const integratedSha = integration.upstream_sha;
  const mergeBaseSha = uniqueMergeBase(repo, productSha, upstreamSha);
  const commits = parseCommits(repo, integratedSha, upstreamSha);
  const files = parseNameStatus(repo, integratedSha, upstreamSha);
  const stats = parseNumstat(repo, integratedSha, upstreamSha);
  for (const file of files) Object.assign(file, stats[file.path] ?? { additions: '?', deletions: '?' }, { risk_categories: riskCategories(file.path) });
  const upstreamPaths = new Set(files.map((item) => item.path));
  const productPaths = changedPathSet(repo, integratedSha, productSha);
  const dirty = dirtyPaths(repo);
  const conflict = mergeTree(repo, productSha, upstreamSha);
  const overlapPaths = [...upstreamPaths].filter((path) => productPaths.has(path)).sort();
  const conflictPaths = new Set(conflict.conflict_paths);
  const automaticOverlapPaths = overlapPaths.filter((path) => !conflictPaths.has(path));
  const dirtySet = new Set(dirty);
  const dirtyOverlapPaths = [...upstreamPaths].filter((path) => dirtySet.has(path)).sort();
  const riskPaths = files.filter((item) => item.risk_categories.length).map((item) => ({ path: item.path, categories: item.risk_categories }));
  const shortstat = integratedSha === upstreamSha ? '' : git(repo, 'diff', '--shortstat', integratedSha, upstreamSha).stdout.trim();
  return [{
    repository,
    path: config.path,
    freshness,
    fetch_error: fetchError,
    product_sha: productSha,
    origin_product_sha: originProductSha,
    mirror_sha: mirrorSha,
    observed_upstream_sha: upstreamSha,
    baseline_tag_sha: baselineTagSha,
    merge_base_sha: mergeBaseSha,
    last_confirmed_integration: integration,
    product_vs_upstream: revCounts(repo, productSha, upstreamSha),
    product_vs_origin: originProductSha ? revCounts(repo, productSha, originProductSha) : null,
    dirty_paths: dirty,
    upstream_commits: commits,
    upstream_files: files,
    upstream_shortstat: shortstat,
    product_overlap_paths: overlapPaths,
    automatic_overlap_paths: automaticOverlapPaths,
    dirty_overlap_paths: dirtyOverlapPaths,
    risk_paths: riskPaths,
    merge_tree: conflict,
  }, {
    integrated_upstream_sha: integratedSha,
    main_merge_sha: integration.product_merge_commit_sha,
    observed_upstream_sha: upstreamSha,
  }];
}

function markdownText(value) { return String(value).replaceAll('|', '\\|').replaceAll('\n', ' '); }
function markdownPath(value) { return `\`${String(value).replaceAll('`', '').replaceAll('|', '\\|').replaceAll('\n', ' ')}\``; }

function renderDiffReport(run) {
  const lines = [
    '# 上游增量 Diff 报告', '', `- 运行 ID：\`${run.run_id}\``, `- 生成时间：\`${run.created_at}\``,
    `- 主题：\`${run.topic}\``, '- 口径：已确认集成上游点到本次观测上游点；不以镜像分支位置替代集成点。', '',
  ];
  for (const repository of ['backend', 'frontend']) {
    const item = run.repositories[repository];
    const integration = item.last_confirmed_integration;
    lines.push(
      `## ${repository}`, '', '| 固定项 | 值 |', '|---|---|', `| 产品 SHA | \`${item.product_sha}\` |`,
      `| 已集成上游 SHA | \`${integration.upstream_sha}\` |`, `| 集成识别方式 | \`${integration.source}\` |`,
      `| 产品 merge commit | \`${integration.product_merge_commit_sha ?? 'null'}\` |`, `| 观测上游 SHA | \`${item.observed_upstream_sha}\` |`,
      `| merge-base | \`${item.merge_base_sha}\` |`, `| mirror SHA | \`${item.mirror_sha}\` |`, `| freshness | \`${item.freshness}\` |`, '',
    );
    if (item.fetch_error) lines.push(`> 上游刷新失败：${markdownText(item.fetch_error)}`, '');
    lines.push(`### 上游新增提交（${item.upstream_commits.length}）`, '');
    if (item.upstream_commits.length) {
      for (const commit of item.upstream_commits) lines.push(`- \`${commit.short_sha}\` ${commit.date} ${markdownText(commit.subject)}`);
    } else lines.push('- 无新增上游提交。');
    lines.push('', '### 文件 Diff', '', `统计：${item.upstream_shortstat || '0 files changed'}`, '');
    if (item.upstream_files.length) {
      lines.push('| 状态 | 文件 | 新增 | 删除 | 风险分类 |', '|---|---|---:|---:|---|');
      for (const changed of item.upstream_files) {
        const displayPath = changed.old_path ? `${changed.old_path} -> ${changed.path}` : changed.path;
        lines.push(`| \`${changed.status}\` | ${markdownPath(displayPath)} | ${changed.additions} | ${changed.deletions} | ${markdownText(changed.risk_categories.join(', ') || '-')} |`);
      }
    } else lines.push('无文件变化。');
    lines.push('', '### 产品重叠面', '');
    if (item.product_overlap_paths.length) lines.push(...item.product_overlap_paths.map((path) => `- ${markdownPath(path)}`));
    else lines.push('- 上游增量与产品自集成点后的文件变化无路径重叠。');
    lines.push('', '### 高风险上游路径', '');
    if (item.risk_paths.length) lines.push(...item.risk_paths.map((risk) => `- ${markdownPath(risk.path)}: ${markdownText(risk.categories.join(', '))}`));
    else lines.push('- 未命中内置热点分类；仍需按 customization map 复核长期不变量。');
    lines.push('', '### 复现命令', '', '```bash',
      `git -C ${item.path} log --oneline ${integration.upstream_sha}..${item.observed_upstream_sha}`,
      `git -C ${item.path} diff --name-status ${integration.upstream_sha}..${item.observed_upstream_sha}`,
      `git -C ${item.path} diff ${integration.upstream_sha}..${item.observed_upstream_sha} -- <path>`, '```', '');
  }
  lines.push('## 现状 Merge 清单', '', '| 仓库 | 上游增量 | Git 冲突 | 定制风险路径 | 当前处置 |', '|---|---:|---:|---:|---|');
  for (const repository of ['backend', 'frontend']) {
    const item = run.repositories[repository];
    const disposition = item.upstream_commits.length
      ? '待按路径级 diff 完成语义复核与质量门禁'
      : '无上游增量，无需合并';
    lines.push(`| ${repository} | ${item.upstream_commits.length} commits / ${item.upstream_files.length} files | ${item.merge_tree.conflict_paths.length} | ${item.risk_paths.length} | ${disposition} |`);
  }
  lines.push('');
  lines.push('## 结论边界', '', '本报告提供完整文件清单与可复现固定点，不内嵌无限制完整 patch。代码级结论必须使用上述固定 SHA 的路径级 diff 补证。', '');
  return lines.join('\n');
}

function appendPathList(lines, paths, emptyText) {
  if (paths.length) lines.push(...paths.map((path) => `- ${markdownPath(path)}`));
  else lines.push(`- ${emptyText}`);
}

function renderConflictReport(run) {
  const lines = [
    '# 上游合并冲突客观报告', '', `- 运行 ID：\`${run.run_id}\``, `- 生成时间：\`${run.created_at}\``,
    '- 模拟方式：`git merge-tree --write-tree --messages <product-sha> <upstream-sha>`',
    '- 工作树说明：模拟只使用提交固定点；未提交修改不进入 merge-tree。', '',
  ];
  for (const repository of ['backend', 'frontend']) {
    const item = run.repositories[repository];
    const tree = item.merge_tree;
    lines.push(`## ${repository}`, '', '| 固定项 | 值 |', '|---|---|', `| 产品 SHA | \`${item.product_sha}\` |`,
      `| 上游 SHA | \`${item.observed_upstream_sha}\` |`, `| merge-base | \`${item.merge_base_sha}\` |`,
      `| merge-tree 状态 | \`${tree.status}\` |`, `| merge-tree exit code | \`${tree.exit_code}\` |`,
      `| 结果 tree | \`${tree.tree_sha ?? 'null'}\` |`, `| Git 确认冲突数 | \`${tree.conflict_paths.length}\` |`, '',
      '### Git 确认冲突', '');
    appendPathList(lines, tree.conflict_paths, 'Git 未报告文本或树冲突。');
    if (tree.messages.length) lines.push('', 'merge-tree 消息：', '', '```text', ...tree.messages, '```', '');
    else lines.push('');
    lines.push('### 可自动合并的双方重叠', '');
    appendPathList(lines, item.automatic_overlap_paths, '没有双方同时修改但可自动合并的路径。');
    lines.push('', '### 定制合同风险', '');
    if (item.risk_paths.length) lines.push(...item.risk_paths.map((risk) => `- ${markdownPath(risk.path)}: ${markdownText(risk.categories.join(', '))}`));
    else lines.push('- 未命中内置热点分类；仍须核对 customization map。');
    lines.push('', '### 未提交工作树重叠', '');
    appendPathList(lines, item.dirty_overlap_paths, '未提交路径与本次上游增量无交集。');
    lines.push('', '### 工作树状态', '');
    appendPathList(lines, item.dirty_paths, '工作树 clean。');
    lines.push('', '### 复现命令', '', '```bash',
      `git -C ${item.path} merge-tree --write-tree --messages ${item.product_sha} ${item.observed_upstream_sha}`, '```', '');
  }
  lines.push('## 局限', '', '`merge-tree` 只能描述冻结提交的 Git 文本/树合并结果。零文本冲突不代表编译、运行时、API、权限、SQL 迁移或业务语义安全；必须继续执行 customization map 复核与项目质量门禁。', '');
  return lines.join('\n');
}

function workspaceObservation(root) {
  ensureRepository(root);
  const productSha = resolveCommit(root, 'refs/heads/main');
  const originSha = resolveCommit(root, 'refs/remotes/origin/main', false);
  return {
    product_sha: productSha,
    origin_product_sha: originSha,
    product_vs_origin: originSha ? revCounts(root, productSha, originSha) : null,
    dirty_paths: dirtyPaths(root),
  };
}

export function statePath(root, value) {
  const path = resolvePath(root, value ?? STATE_RELATIVE_PATH);
  const relation = relative(resolvePath(root), path);
  if (relation.startsWith('..') || isAbsolute(relation)) throw new SyncError(`state file must stay under repository root: ${path}`);
  return path;
}

function changeStatePath(root, state, value) {
  const change = value ?? state.current_change;
  if (typeof change !== 'string' || !change) throw new SyncError('current change is missing; run assess first or pass --change');
  if (basename(change) !== change || change === '.' || change === '..') throw new SyncError(`change must be a directory name under docs/upstream: ${change}`);
  return join(root, 'docs/upstream', change, 'state.json');
}

function parseArguments(argv) {
  const command = argv[0];
  if (!['assess', 'record-integration'].includes(command)) throw new SyncError('expected command: assess or record-integration');
  const options = { root: '.', verification: [] };
  const booleans = new Set(['fetch', 'advance-mirrors', 'advance-products', 'dry-run']);
  for (let index = 1; index < argv.length; index += 1) {
    const token = argv[index];
    if (!token.startsWith('--')) throw new SyncError(`unexpected argument: ${token}`);
    const key = token.slice(2);
    if (booleans.has(key)) options[key] = true;
    else {
      const value = argv[++index];
      if (value === undefined || value.startsWith('--')) throw new SyncError(`missing value for --${key}`);
      if (key === 'verification') options.verification.push(value);
      else options[key] = value;
    }
  }
  return { command, options };
}

export function assess(options) {
  const root = resolvePath(options.root);
  ensureRepository(root);
  for (const config of Object.values(REPOSITORIES)) ensureRepository(join(root, config.path));
  if (!options.topic) throw new SyncError('--topic is required');
  const topic = sanitizeTopic(options.topic);
  const timestamp = nowRfc3339();
  const date = options.date ?? timestamp.slice(0, 10);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) throw new SyncError(`date must use YYYY-MM-DD: ${date}`);
  const fetchResults = options.fetch ? refreshRefs(root) : {};
  const advanceResults = advanceRequestedRefs(root, Boolean(options['advance-mirrors']), Boolean(options['advance-products']));
  const path = statePath(root, options['state-file']);
  const state = loadState(path);
  const repositoryObservations = {};
  const newRepositoryStates = {};
  for (const [repository, config] of Object.entries(REPOSITORIES)) {
    const fetched = fetchResults[repository] ?? {};
    const freshness = !options.fetch ? 'cached' : fetched.upstream ? 'fresh' : 'stale';
    const fetchError = freshness === 'stale' ? fetched.upstream_error ?? 'upstream fetch failed' : null;
    [repositoryObservations[repository], newRepositoryStates[repository]] = collectRepository(
      root, repository, config, state, freshness, fetchError, timestamp,
    );
  }
  const reportDir = currentReportDir(root);
  const run = {
    run_id: `${timestamp.replaceAll(':', '')}-${basename(reportDir)}`,
    created_at: timestamp,
    topic,
    report_dir: relative(root, reportDir),
    fetch_requested: Boolean(options.fetch),
    fetch_results: fetchResults,
    advance_results: advanceResults,
    workspace: workspaceObservation(root),
    repositories: repositoryObservations,
  };
  if (options['dry-run']) return { dryRun: true, result: run };
  mkdirSync(reportDir, { recursive: true });
  atomicWriteText(join(reportDir, 'diff_report.md'), renderDiffReport(run));
  atomicWriteText(join(reportDir, 'conflict_report.md'), renderConflictReport(run));
  const changeRepositories = {};
  for (const [repository, observation] of Object.entries(repositoryObservations)) {
    if (observation.last_confirmed_integration.upstream_sha !== observation.observed_upstream_sha) {
      changeRepositories[repository] = { upstream_sha: observation.observed_upstream_sha, main_merge_sha: null };
    }
  }
  atomicWriteJson(join(reportDir, 'state.json'), { created_at: timestamp, repositories: changeRepositories });
  atomicWriteJson(path, {
    schema_version: SCHEMA_VERSION,
    updated_at: timestamp,
    current_change: basename(reportDir),
    repositories: newRepositoryStates,
  });
  return { dryRun: false, result: { state: path, report_dir: reportDir } };
}

export function recordIntegration(options) {
  const root = resolvePath(options.root);
  if (!(options.repository in REPOSITORIES)) throw new SyncError('--repository must be backend or frontend');
  if (!options['merge-commit'] || !options['upstream-sha']) throw new SyncError('--merge-commit and --upstream-sha are required');
  if (!options.verification?.length) throw new SyncError('at least one --verification evidence string is required');
  const config = REPOSITORIES[options.repository];
  const repo = join(root, config.path);
  ensureRepository(repo);
  const path = statePath(root, options['state-file']);
  if (!existsSync(path)) throw new SyncError(`state file does not exist; run assess first: ${path}`);
  const state = loadState(path);
  const mergeSha = resolveCommit(repo, options['merge-commit']);
  const upstreamSha = resolveCommit(repo, options['upstream-sha']);
  const productSha = resolveCommit(repo, config.productRef);
  const observedUpstreamSha = resolveCommit(repo, config.upstreamRef);
  if (!isAncestor(repo, mergeSha, productSha)) throw new SyncError(`merge commit is not reachable from product main: ${mergeSha}`);
  const parents = commitParents(repo, mergeSha);
  if (parents.length < 2) throw new SyncError(`integration commit is not a merge commit: ${mergeSha}`);
  if (!parents.slice(1).includes(upstreamSha)) throw new SyncError(`upstream SHA is not an exact non-first parent of merge commit: ${upstreamSha}`);
  if (!isAncestor(repo, upstreamSha, observedUpstreamSha)) throw new SyncError(`recorded upstream SHA is not in current upstream history: ${upstreamSha}`);
  const repositoryState = state.repositories[options.repository];
  if (!repositoryState) throw new SyncError(`repository state missing; run assess first: ${options.repository}`);
  const changePath = changeStatePath(root, state, options.change);
  if (!existsSync(changePath)) throw new SyncError(`change state file does not exist: ${changePath}`);
  const changeState = loadChangeState(changePath);
  const changeRepository = changeState.repositories[options.repository];
  if (!changeRepository) throw new SyncError(`repository is not pending in change state: ${options.repository}`);
  if (changeRepository.upstream_sha !== upstreamSha) throw new SyncError(`upstream SHA does not match the frozen change target: ${upstreamSha} != ${changeRepository.upstream_sha}`);
  const timestamp = nowRfc3339();
  changeRepository.main_merge_sha = mergeSha;
  repositoryState.integrated_upstream_sha = upstreamSha;
  repositoryState.main_merge_sha = mergeSha;
  state.updated_at = timestamp;
  atomicWriteJson(changePath, changeState);
  atomicWriteJson(path, state);
  return {
    repository: options.repository,
    recorded_at: timestamp,
    product_sha: productSha,
    merge_commit_sha: mergeSha,
    upstream_sha: upstreamSha,
    verification: options.verification,
  };
}

export function main(argv = process.argv.slice(2)) {
  try {
    const { command, options } = parseArguments(argv);
    if (command === 'assess') {
      const output = assess(options);
      process.stdout.write(`${JSON.stringify(output.result, null, output.dryRun ? 2 : 0)}\n`);
    } else process.stdout.write(`${JSON.stringify(recordIntegration(options), null, 2)}\n`);
    return 0;
  } catch (error) {
    process.stderr.write(`error: ${error.message}\n`);
    return 2;
  }
}

const invokedPath = process.argv[1] ? resolvePath(process.argv[1]) : null;
if (invokedPath === fileURLToPath(import.meta.url)) process.exitCode = main();
