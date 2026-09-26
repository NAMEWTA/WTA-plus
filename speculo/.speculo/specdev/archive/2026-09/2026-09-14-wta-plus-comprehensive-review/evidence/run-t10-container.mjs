import { fixture } from '../../../../../../release-artifacts/tests/fixtures/atomic-release-fixture.mjs';
import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const evidenceName = process.argv[2] ?? 'T-10-container';
const evidence = path.dirname(fileURLToPath(import.meta.url));
const docker = (...args) => execFileSync('docker', args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim();
const snapshot = () => ({
  containers: docker('ps', '-aq', '--no-trunc').split('\n').filter(Boolean).sort(),
  networks: docker('network', 'ls', '-q', '--no-trunc').split('\n').filter(Boolean).sort(),
  volumes: docker('volume', 'ls', '-q').split('\n').filter(Boolean).sort(),
});
const before = snapshot(), f = fixture();
const owned = new Set(), observations = [];
const image = 'nginx:1.31.1';
const imageInfo = JSON.parse(docker('image', 'inspect', image))[0];
let result = { command: 'node evidence/run-t10-container.mjs', image, imageId: imageInfo.Id, imageDigests: imageInfo.RepoDigests, before };
const html = (id) => path.join(f.version(id), 'docker/frontend/nginx/html/admin-web');
let active;
const start = (version) => {
  const name = 'wta-t10-' + process.pid + '-' + observations.length;
  const id = docker('create', '--name', name, '--label', 'wta.test.owner=T-10-' + process.pid,
    '-p', '127.0.0.1::80', '--mount', 'type=bind,src=' + html(version) + ',dst=/usr/share/nginx/html,readonly', image);
  owned.add(id); active = id; docker('start', id);
  return id;
};
const stop = (id) => { docker('rm', '-fv', id); owned.delete(id); };
async function observe(expected, label) {
  const state = JSON.parse(docker('inspect', active))[0];
  const port = state.NetworkSettings.Ports['80/tcp'][0].HostPort;
  let text, lastError;
  for (let attempt = 0; attempt < 100; attempt++) {
    try {
      const response = await fetch('http://127.0.0.1:' + port + '/index.html', { signal: AbortSignal.timeout(1000) });
      assert.equal(response.status, 200); text = await response.text(); break;
    } catch (error) { lastError = error; await new Promise((resolve) => setTimeout(resolve, 50)); }
  }
  assert.equal(text, expected, String(lastError));
  observations.push({ label, containerId: active, body: text, mountSource: state.Mounts.find((m) => m.Destination === '/usr/share/nginx/html').Source.replace(f.scratch, '<fixture>'), selected: fs.readlinkSync(f.current) });
}
try {
  const a = f.build(); f.stage(a);
  fs.writeFileSync(path.join(f.root, 'marker.txt'), 'version B'); f.commit(); const b = f.build();
  start(a); await observe('version A', 'initial version A');
  f.stage(b); await observe('version A', 'pointer B does not change existing fixed bind mount A');
  stop(active); start(b); await observe('version B', 'explicit recreate uses B');
  f.stage(a); await observe('version B', 'rollback pointer A still requires recreation');
  stop(active); start(a); await observe('version A', 'explicit recreate restores A');
  result = { ...result, releaseA: a, releaseB: b, observations, exitCode: 0,
    boundary: 'Real Bash/Python release state and Git fixtures; compiler outputs synthetic. Real Nginx Docker engine, fixed bind mounts and loopback HTTP; no existing stack touched.' };
} catch (error) {
  result = { ...result, observations, exitCode: 1, error: String(error) };
  process.exitCode = 1;
} finally {
  for (const id of owned) stop(id);
  result.after = snapshot();
  try { assert.deepEqual(result.after, before); result.ownedResourcesCleaned = true; }
  catch (error) { result.ownedResourcesCleaned = false; result.cleanupError = String(error); process.exitCode = 1; }
  f.dispose();
  fs.writeFileSync(path.join(evidence, evidenceName + '.json'), JSON.stringify(result, null, 2) + '\n');
  console.log(JSON.stringify({ exitCode: result.exitCode, observations: observations.length, ownedResourcesCleaned: result.ownedResourcesCleaned }));
}
