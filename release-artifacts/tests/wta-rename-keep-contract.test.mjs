import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const releaseRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const adjacentRoot = path.resolve(releaseRoot, '..');
const workspaceRoot = fs.existsSync(path.join(adjacentRoot, 'backend'))
  ? adjacentRoot
  : (process.env.WTA_PLUS_ROOT || '/workspace/vp-dev/WTA-plus');
const backendRoot = path.join(workspaceRoot, 'backend');
const skipDirNames = new Set(['node_modules', 'target', 'dist', '.git', 'speculo']);

function walk(directory, files = []) {
  if (!fs.existsSync(directory)) return files;
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    if (entry.name.startsWith('.git')) continue;
    const absolute = path.join(directory, entry.name);
    const rel = path.relative(workspaceRoot, absolute).replaceAll('\\', '/');
    if (entry.isDirectory()) {
      if (skipDirNames.has(entry.name) || rel.includes('openapi/revisions/')) continue;
      walk(absolute, files);
      continue;
    }
    files.push({ absolute, rel });
  }
  return files;
}

function read(rel) {
  return fs.readFileSync(path.join(workspaceRoot, rel), 'utf8');
}

test('KEEP third-party dromara coordinates still occur as Maven groupIds and Java imports', () => {
  const files = walk(backendRoot);
  const joined = files
    .filter((file) => /\.(xml|java)$/.test(file.rel))
    .map((file) => fs.readFileSync(file.absolute, 'utf8'))
    .join('\n');
  for (const keep of [
    'org.dromara.sms4j',
    'org.dromara.warm',
    'org.dromara.easyes',
    'org.dromara.easy-es',
    'org.dromara.mica.mqtt',
    'org.dromara.mica-mqtt',
  ]) {
    assert.match(joined, new RegExp(keep.replaceAll('.', '\\.')), `KEEP string missing: ${keep}`);
  }
  assert.doesNotMatch(joined, /org\.namewta\.sms4j/);
  assert.doesNotMatch(joined, /org\.namewta\.warm/);
  assert.doesNotMatch(joined, /org\.namewta\.easyes/);
  assert.doesNotMatch(joined, /org\.namewta\.mica/);
});

test('first-party backend POM groupId is org.namewta and owned modules are wta-*', () => {
  const pom = read('backend/pom.xml');
  const groupId = pom.match(/<groupId>([^<]+)<\/groupId>/);
  const artifactId = pom.match(/<artifactId>([^<]+)<\/artifactId>/);
  assert.equal(groupId?.[1], 'org.namewta');
  assert.equal(artifactId?.[1], 'wta-vue-plus');
  assert.match(pom, /<module>wta-admin<\/module>/);
  assert.doesNotMatch(pom, /<groupId>org\.dromara<\/groupId>/);
  assert.doesNotMatch(pom, /<module>ruoyi-/);
  assert.ok(fs.existsSync(path.join(workspaceRoot, 'backend')));
  assert.ok(fs.existsSync(path.join(workspaceRoot, 'frontend')));
  assert.ok(fs.existsSync(path.join(backendRoot, 'wta-admin')));
  assert.equal(fs.existsSync(path.join(backendRoot, 'ruoyi-admin')), false);
  assert.equal(fs.existsSync(path.join(workspaceRoot, 'wta-vue-plus-namewta')), false);
  assert.equal(fs.existsSync(path.join(workspaceRoot, 'plus-ui-namewta')), false);
});

test('product-owned upstream-fork-sync skill and docs/upstream workflow tree are absent', () => {
  assert.equal(fs.existsSync(path.join(workspaceRoot, '.agents/skills/upstream-fork-sync')), false);
  // speculo/skills is vendor-owned tooling, outside the product namespace cutover.
  assert.equal(fs.existsSync(path.join(workspaceRoot, 'docs/upstream')), false);
  const agents = read('AGENTS.md');
  assert.doesNotMatch(agents, /upstream-fork-sync/);
  const readme = read('README.md');
  const ruoyiLines = readme.split('\n').filter((line) => line.includes('RuoYi-Vue-Plus'));
  assert.equal(ruoyiLines.length, 1, readme);
  const skillRoot = path.join(workspaceRoot, '.agents/skills');
  const dead = walk(skillRoot)
    .filter((file) => /\.(md|yml|yaml|json|mjs|ts)$/.test(file.rel))
    .filter((file) => {
      try {
        return fs.readFileSync(file.absolute, 'utf8').includes('docs/upstream');
      } catch {
        return false;
      }
    })
    .map((file) => file.rel);
  assert.deepEqual(dead, []);
});

test('owned filesystem paths use org/namewta not org/dromara', () => {
  const files = walk(workspaceRoot).filter((file) =>
    /\.(java|json|md|mjs|sh|yml|yaml|xml|vue|ts)$/.test(file.rel),
  );
  const leftovers = [];
  for (const file of files) {
    let text;
    try {
      text = fs.readFileSync(file.absolute, 'utf8');
    } catch {
      continue;
    }
    const owned = text
      .replaceAll('org/dromara/sms4j', '')
      .replaceAll('org/dromara/warm', '')
      .replaceAll('org/dromara/easy-es', '')
      .replaceAll('org/dromara/mica-mqtt', '')
      .replaceAll('org\\dromara\\sms4j', '')
      .replaceAll('org\\dromara\\warm', '')
      .replaceAll('org\\dromara\\easy-es', '')
      .replaceAll('org\\dromara\\mica-mqtt', '');
    if (/org[/\\]dromara[/\\]/.test(owned)) leftovers.push(file.rel);
  }
  assert.deepEqual(leftovers, []);
});

test('live admin home and docs widget do not depend on dromara product URLs', () => {
  const index = read('frontend/apps/admin-web/src/views/index.vue');
  const doc = read('frontend/apps/admin-web/src/components/WTADoc/index.vue');
  assert.match(index, /https:\/\/github\.com\/NAMEWTA\/WTA-plus/);
  assert.doesNotMatch(index, /plus-doc\.dromara\.org/);
  assert.doesNotMatch(index, /github\.com\/dromara/);
  assert.doesNotMatch(doc, /plus-doc\.dromara\.org/);
  assert.match(doc, /https:\/\/github\.com\/NAMEWTA\/WTA-plus/);
});

test('README clones the WTA-plus monorepo without git submodule delivery', () => {
  const readme = read('README.md');
  assert.match(readme, /git clone https:\/\/github\.com\/NAMEWTA\/WTA-plus\.git/);
  assert.doesNotMatch(readme, /recurse-submodules/);
  assert.doesNotMatch(readme, /git submodule update/);
  assert.doesNotMatch(readme, /wta-vue-plus-docs\.git/);
});

test('shipped Nacos runtime reads only the new data-id (hard-cut, no dual-read loader)', () => {
  const constants = read(
    'backend/wta-common/wta-common-nacos/src/main/java/org/namewta/common/nacos/NacosConfigConstants.java',
  );
  const settings = read(
    'backend/wta-common/wta-common-nacos/src/main/java/org/namewta/common/nacos/NacosConfigSettings.java',
  );
  const application = read('backend/wta-admin/src/main/resources/application.yml');
  assert.match(constants, /DEFAULT_DATA_ID = "wta-namewta.yml"/);
  assert.match(application, /data-id:\s*wta-namewta\.yml/);
  assert.doesNotMatch(application, /ruoyi-namewta\.yml/);
  assert.doesNotMatch(settings, /oldDataId|legacyDataId|fallbackDataId|dataIds\s*=/);
  assert.match(settings, /environment\.getProperty\("nacos\.config\.data-id"\)/);
  const dualReadHint = /ruoyi-namewta\.yml[\s\S]{0,200}wta-namewta\.yml|wta-namewta\.yml[\s\S]{0,200}ruoyi-namewta\.yml/;
  assert.doesNotMatch(settings, dualReadHint);
});
