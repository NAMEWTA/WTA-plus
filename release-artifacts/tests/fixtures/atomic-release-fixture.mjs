import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { execFileSync, spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const releaseRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repoRoot = path.dirname(releaseRoot);
function thaw(directory) {
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const file = path.join(directory, entry.name);
    if (entry.isSymbolicLink()) continue;
    fs.chmodSync(file, entry.isDirectory() ? 0o755 : 0o644);
    if (entry.isDirectory()) thaw(file);
  }
  fs.chmodSync(directory, 0o755);
}

const sha = (value) => crypto.createHash('sha256').update(value).digest('hex');
const write = (file, text, mode) => {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, text, mode ? { mode } : undefined);
};

// Real Git archive / Bash entry / Python state machine / jar verifier; compilers and Docker
// config are bounded stubs. These synthetic fixture commits never alter the project history.
export function fixture() {
  const scratch = fs.mkdtempSync(path.join(os.tmpdir(), 'wta-release-contract-'));
  const root = path.join(scratch, 'repo');
  const release = path.join(root, 'release-artifacts');
  fs.mkdirSync(release, { recursive: true });
  for (const name of ['scripts', 'docker', 'skills']) {
    fs.cpSync(path.join(releaseRoot, name), path.join(release, name), { recursive: true,
      filter: (source) => !source.includes('/__pycache__') && !source.endsWith('/app.jar') });
  }
  for (const name of ['README.md', '.env.example', 'apps.json']) fs.copyFileSync(path.join(releaseRoot, name), path.join(release, name));
  write(path.join(root, '.gitignore'), 'release-artifacts/builds/\nrelease-artifacts/bundles/\nrelease-artifacts/.env\n**/target/\n**/dist/\n**/__pycache__/\n');
  write(path.join(root, 'marker.txt'), 'version A');
  fs.cpSync(path.join(repoRoot, 'scripts/ci/verify-admin-bundle.sh'), path.join(root, 'scripts/ci/verify-admin-bundle.sh'), { recursive: true });
  for (const app of ['admin-web', 'home-web', 'sso-web']) write(path.join(root, 'frontend/apps', app, 'package.json'), JSON.stringify({ name: '@namewta/' + app, scripts: { 'build:dev': 'vite', 'build:prod': 'vite' } }));
  write(path.join(release, '.env'), 'ADMIN_WEB_PREFIX=admin-app\nHOME_WEB_PREFIX=home-app\nSSO_WEB_PREFIX=sso-app\nADMIN_WEB_ORIGIN=https://localhost:4441\nHOME_WEB_ORIGIN=https://localhost:4442\nSSO_WEB_ORIGIN=https://sso.localhost:4443\nRUNTIME_CANARY=must-not-be-packaged\n');
  write(path.join(root, 'backend/mvnw'), `#!/usr/bin/env python3
from pathlib import Path
import os,zipfile,time
root=Path.cwd()
if os.environ.get('BUILD_WAIT'):
 Path(os.environ['BUILD_WAIT']).write_text(str(os.getpid()))
 while not Path(os.environ['BUILD_WAIT']+'.continue').exists(): time.sleep(.02)
marker=(root.parent/'marker.txt').read_bytes()
base=['wta-system','wta-common-notify','wta-common-oss','wta-third','wta-sso','wta-notify','wta-profile-person','wta-profile-enterprise']
extra=[] if any('bundle-core' in v for v in __import__('sys').argv) else ['wta-job','wta-ai','wta-common-ai','wta-demo','wta-workflow']
for name in ['wta-admin','wta-monitor-admin','wta-snailjob-server']:
 p=root/('' if name=='wta-admin' else 'wta-extend')/name/'target'/(name+'.jar');p.parent.mkdir(parents=True,exist_ok=True)
 with zipfile.ZipFile(p,'w') as z:
  z.writestr('META-INF/MANIFEST.MF','Main-Class: org.springframework.boot.loader.launch.JarLauncher\\nStart-Class: fixture.Main\\n')
  z.writestr('BOOT-INF/classes/marker.txt',marker)
  if name=='wta-admin':
   for lib in base+extra:z.writestr('BOOT-INF/lib/'+lib+'-1.0.jar','fixture')
`, 0o755);
  const bin = path.join(scratch, 'bin');
  write(path.join(bin, 'pnpm'), `#!/usr/bin/env python3
from pathlib import Path
import json,sys
if '--filter' in sys.argv:
 app=sys.argv[sys.argv.index('--filter')+1].split('/')[-1]
 p=Path('apps')/app/'dist';p.mkdir(parents=True,exist_ok=True)
 (p/'index.html').write_text((Path.cwd().parent/'marker.txt').read_text())
 (p/'build-mode.json').write_text(json.dumps({'app':app,'mode':'production' if sys.argv[-1]=='build:prod' else 'development'}))
`, 0o755);
  write(path.join(bin, 'docker'), `#!/usr/bin/env python3
import json,os,sys
from pathlib import Path
if os.environ.get('DOCKER_CALLS'):
 with open(os.environ['DOCKER_CALLS'],'a') as f:f.write(json.dumps({'args':sys.argv[1:],'image':os.getenv('NAMEWTA_ADMIN_IMAGE'),'data':os.getenv('NAMEWTA_DATA_ROOT'),'cors':os.getenv('WEB_CORS_ALLOWED_ORIGINS'),'sso':os.getenv('SSO_WEB_ORIGIN'),'ssoBase':os.getenv('SSO_WEB_BASE_PATH')})+'\\n')
# Switch the pointer between Compose calls to prove the manager pins one resolved version.
if '-f' in sys.argv and os.environ.get('SWITCH_TO'):
 current=Path(os.environ['CURRENT_POINTER'])
 if current.readlink().name!=os.environ['SWITCH_TO']:
  tmp=current.with_name('.test-pointer');tmp.symlink_to('versions/'+os.environ['SWITCH_TO']);tmp.replace(current)
print(' '.join(sys.argv[1:]))
`, 0o755);
  const env = { ...process.env, PATH: bin + ':' + process.env.PATH };
  // Avoid unrelated host prefix/image overrides affecting isolated fixtures.
  for (const key of Object.keys(env)) if (key.endsWith('_PREFIX')) delete env[key];
  const git = (...args) => execFileSync('git', ['-C', root, ...args], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim();
  git('init', '-q'); git('config', 'user.name', 'release fixture'); git('config', 'user.email', 'fixture@example.invalid');
  const commit = () => { git('add', '.'); git('-c', 'core.hooksPath=/dev/null', 'commit', '-qm', 'synthetic release fixture'); return git('rev-parse', 'HEAD'); };
  commit();
  const cli = (...args) => spawnSync('bash', [path.join(release, 'scripts/release-manage.sh'), ...args], { env, encoding: 'utf8' });
  const ok = (...args) => { const result = cli(...args); assert.equal(result.status, 0, result.stderr); return result.stdout.trim().split('\n').at(-1); };
  const build = () => ok('build', '--target', 'all', '--env', 'prod');
  const stage = (id) => ok('stage', '--env', 'prod', '--release', id);
  const version = (id) => path.join(release, 'builds/versions', id);
  const current = path.join(release, 'builds/current_prod');
  const dispose = () => { thaw(scratch); fs.rmSync(scratch, { recursive: true, force: true }); };
  return { scratch, root, release, env, git, commit, cli, ok, build, stage, version, current, dispose };
}

export function snapshot(root) {
  const found = {};
  const walk = (directory) => {
    for (const name of fs.readdirSync(directory).sort()) {
      const file = path.join(directory, name), stat = fs.lstatSync(file);
      if (stat.isSymbolicLink()) found[path.relative(root, file)] = 'link:' + fs.readlinkSync(file);
      else if (stat.isDirectory()) walk(file);
      else found[path.relative(root, file)] = sha(fs.readFileSync(file));
    }
  };
  walk(root);
  return found;
}
