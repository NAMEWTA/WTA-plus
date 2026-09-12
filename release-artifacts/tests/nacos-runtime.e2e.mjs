import assert from 'node:assert/strict';
import { execFile } from 'node:child_process';
import { createHash } from 'node:crypto';
import { promisify } from 'node:util';
import test from 'node:test';

const execFileAsync = promisify(execFile);
const enabled = process.env.NACOS_RUNTIME_E2E === '1';

function required(name) {
  const value = process.env[name];
  assert.ok(value, `${name} is required`);
  return value;
}

const delay = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));
const digest = (content) => createHash('sha256').update(content).digest('hex');
const phase = (message) => process.stderr.write(`[E2E] ${message}\n`);

async function request(url, options = {}) {
  const response = await fetch(url, { signal: AbortSignal.timeout(10_000), ...options });
  const body = await response.text();
  return { response, body };
}

async function eventually(label, action, predicate, timeoutMs = 90_000) {
  const deadline = Date.now() + timeoutMs;
  let lastError;
  while (Date.now() < deadline) {
    try {
      const value = await action();
      if (predicate(value)) return value;
      lastError = new Error(`${label} returned an unexpected value`);
    } catch (error) {
      lastError = error;
    }
    await delay(1_000);
  }
  throw new Error(`${label} timed out: ${lastError?.message ?? 'no result'}`);
}

test('isolated Nacos runtime converges and fails safely', { skip: !enabled }, async () => {
  const nacosUrl = required('NACOS_E2E_NACOS_URL');
  const appUrls = [required('NACOS_E2E_APP1_URL'), required('NACOS_E2E_APP2_URL')];
  const appContainers = [required('NACOS_E2E_APP1_CONTAINER'), required('NACOS_E2E_APP2_CONTAINER')];
  const nacosContainer = required('NACOS_E2E_NACOS_CONTAINER');
  const username = required('NACOS_E2E_NACOS_USERNAME');
  const password = required('NACOS_E2E_NACOS_PASSWORD');
  const namespace = required('NACOS_E2E_NAMESPACE');
  const monitorUsername = required('NACOS_E2E_MONITOR_USERNAME');
  const monitorPassword = required('NACOS_E2E_MONITOR_PASSWORD');
  const dataId = 'wta-namewta.yml';
  const group = 'DEFAULT_GROUP';
  const basic = `Basic ${Buffer.from(`${monitorUsername}:${monitorPassword}`).toString('base64')}`;
  let accessToken;

  async function login() {
    const body = new URLSearchParams({ username, password });
    const result = await request(`${nacosUrl}/nacos/v1/auth/login`, {
      method: 'POST',
      headers: { 'content-type': 'application/x-www-form-urlencoded' },
      body,
    });
    assert.equal(result.response.status, 200, 'Nacos login must succeed');
    const parsed = JSON.parse(result.body);
    accessToken = parsed.accessToken;
    assert.ok(accessToken, 'Nacos login must return an access token');
  }

  async function nacosApi(path, fields = {}, method = 'POST') {
    if (!accessToken) await login();
    const params = new URLSearchParams({ ...fields, accessToken });
    const url = method === 'GET'
      ? `${nacosUrl}${path}?${params}`
      : `${nacosUrl}${path}`;
    const result = await request(url, {
      method,
      headers: method === 'GET' ? undefined : { 'content-type': 'application/x-www-form-urlencoded' },
      body: method === 'GET' ? undefined : params,
    });
    if (result.response.status === 401 || result.response.status === 403) {
      accessToken = undefined;
      await login();
      return nacosApi(path, fields, method);
    }
    assert.ok(result.response.ok, `Nacos API ${path} failed with ${result.response.status}`);
    return result.body;
  }

  async function publish(content) {
    const body = await nacosApi('/nacos/v1/cs/configs', {
      dataId, group, tenant: namespace, type: 'yaml', content,
    });
    assert.equal(body.trim(), 'true', 'configuration publish must succeed');
    return digest(content);
  }

  async function removeConfig() {
    const body = await nacosApi('/nacos/v1/cs/configs', { dataId, group, tenant: namespace }, 'DELETE');
    assert.equal(body.trim(), 'true', 'configuration deletion must succeed');
  }

  async function persistedConfig() {
    return nacosApi('/nacos/v1/cs/configs', { dataId, group, tenant: namespace }, 'GET');
  }

  async function stateAt(url) {
    const result = await request(`${url}/actuator/info`, { headers: { authorization: basic } });
    assert.equal(result.response.status, 200, 'actuator info must require valid Basic credentials');
    return JSON.parse(result.body).nacosConfig;
  }

  async function captchaAt(url) {
    const result = await request(`${url}/auth/code`);
    assert.equal(result.response.status, 200, 'captcha endpoint must remain available');
    const parsed = JSON.parse(result.body);
    assert.ok(parsed.data, `captcha endpoint returned application code ${parsed.code}`);
    return parsed.data.captchaEnabled;
  }

  async function waitStates(predicate, label, urls = appUrls, timeoutMs = 90_000) {
    return eventually(label, async () => Promise.all(urls.map(stateAt)),
      (states) => states.every((state, index) => state && predicate(state, index)), timeoutMs);
  }

  async function assertCaptchas(expected) {
    const actual = await Promise.all(appUrls.map(captchaAt));
    assert.deepEqual(actual, expected);
  }

  async function docker(action, container) {
    await execFileAsync('docker', [action, container], { timeout: 120_000 });
  }

  phase('validating authentication boundaries');
  const anonymous = await request(`${nacosUrl}/nacos/v1/cs/configs?dataId=${dataId}&group=${group}&tenant=${namespace}`);
  assert.equal(anonymous.response.status, 403);
  const unauthenticatedInfo = await request(`${appUrls[0]}/actuator/info`);
  assert.equal(unauthenticatedInfo.response.status, 401);

  phase('publishing sparse override and deployment precedence');
  const sparse = 'captcha:\n  enable: false\n';
  const sparseDigest = await publish(sparse);
  await waitStates((state) => state.result === 'APPLIED' && state.digest === sparseDigest
    && state.immediateKeyCount === 1 && state.restartKeyCount === 0, 'sparse override');
  await assertCaptchas([true, false]);

  phase('publishing mixed immediate and restart-required keys');
  const mixed = [
    'captcha:', '  enable: true', '  type: char', '  charLength: 5',
    'notify:', '  idempotency:', '    defaultWindow: 3m', '    minWindow: 1m', '    maxWindow: 5m',
    'oss:', '  lifecycle:', '    download-ttl: 1m',
    'feature:', '  restart-marker: t06', '',
  ].join('\n');
  const mixedDigest = await publish(mixed);
  await waitStates((state) => state.result === 'APPLIED' && state.digest === mixedDigest
    && state.immediateKeyCount === 7 && state.restartKeyCount === 1, 'mixed override');
  await assertCaptchas([true, true]);

  const rejectedCases = [
    ['invalid YAML', 'captcha:\n  enable: [unterminated\n', 'INVALID_YAML'],
    ['protected key', 'nacos:\n  config:\n    enabled: false\n', 'PROTECTED_KEY'],
    ['known type', 'captcha:\n  charLength: 0\n', 'KNOWN_TYPE_INVALID'],
    ['participant', 'notify:\n  idempotency:\n    defaultWindow: 7m\n    minWindow: 10m\n    maxWindow: 5m\n', 'PARTICIPANT_REJECTED'],
  ];
  for (const [label, content, code] of rejectedCases) {
    phase(`rejecting ${label}`);
    await publish(content);
    await waitStates((state) => state.result === 'REJECTED' && state.errorCode === code
      && state.digest === mixedDigest, `${label} rejection`);
  }
  await assertCaptchas([true, true]);

  phase('deleting the remote overlay');
  await removeConfig();
  await waitStates((state) => state.result === 'APPLIED' && !state.digest
    && state.immediateKeyCount === 0 && state.restartKeyCount === 0, 'remote deletion');
  await assertCaptchas([true, true]);

  phase('locating one stopped instance and converging it after restart');
  const stableV1 = 'captcha:\n  enable: false\nfeature:\n  restart-marker: stable-v1\n';
  await publish(stableV1);
  await waitStates((state) => state.digest === digest(stableV1), 'stable v1');
  await docker('stop', appContainers[1]);
  const stableV2 = 'captcha:\n  enable: true\nfeature:\n  restart-marker: stable-v2\n';
  const stableV2Digest = await publish(stableV2);
  await waitStates((state) => state.digest === stableV2Digest, 'single live instance', [appUrls[0]]);
  await assert.rejects(() => stateAt(appUrls[1]));
  await docker('start', appContainers[1]);
  await waitStates((state) => state.digest === stableV2Digest,
    'restarted instance convergence', appUrls, 180_000);

  phase('stopping Nacos and retaining the last in-memory configuration');
  await docker('stop', nacosContainer);
  await waitStates((state) => state.connected === false && state.digest === stableV2Digest,
    'runtime disconnect', appUrls, 120_000);
  await assertCaptchas([true, true]);

  phase('restarting one application offline without a disk snapshot');
  await docker('restart', appContainers[1]);
  await waitStates((state) => state.connected === false && state.result === 'LOCAL_BASELINE'
    && !state.digest && state.immediateKeyCount === 0 && state.restartKeyCount === 0,
  'offline local baseline', [appUrls[1]], 180_000);
  assert.equal(await captchaAt(appUrls[1]), true);

  phase('restoring Nacos and converging both instances');
  await docker('start', nacosContainer);
  await eventually('Nacos readiness', async () => (await request(`${nacosUrl}/nacos/v1/console/health/readiness`)).response.status,
    (status) => status === 200, 180_000);
  accessToken = undefined;
  await waitStates((state) => state.connected === true && state.digest === stableV2Digest,
    'post-outage convergence', appUrls, 180_000);

  phase('restarting Nacos and verifying MySQL-backed persistence');
  await docker('restart', nacosContainer);
  await eventually('Nacos restart readiness', async () => (await request(`${nacosUrl}/nacos/v1/console/health/readiness`)).response.status,
    (status) => status === 200, 180_000);
  accessToken = undefined;
  const persisted = await eventually('persisted configuration', persistedConfig,
    (content) => digest(content) === stableV2Digest, 90_000);
  assert.equal(digest(persisted), stableV2Digest);
  await waitStates((state) => state.connected === true && state.digest === stableV2Digest,
    'post-restart convergence', appUrls, 180_000);

  phase('restoring the local baseline');
  await removeConfig();
  await waitStates((state) => state.result === 'APPLIED' && !state.digest, 'final local baseline');
  await assertCaptchas([true, true]);
  phase('runtime matrix passed');
});
