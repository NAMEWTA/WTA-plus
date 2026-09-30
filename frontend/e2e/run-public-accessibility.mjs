import { spawn } from 'node:child_process';
import { cp, mkdtemp, mkdir, readFile, rm, stat } from 'node:fs/promises';
import { createServer } from 'node:https';
import { tmpdir } from 'node:os';
import { dirname, extname, join, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const temporary = await mkdtemp(join(tmpdir(), 'namewta-ui-'));
const servers = [];
const types = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript',
  '.css': 'text/css',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
  '.json': 'application/json',
  '.woff2': 'font/woff2'
};
const run = (command, args, env = process.env) =>
  new Promise((resolveRun, reject) => {
    const child = spawn(command, args, { cwd: root, env, stdio: 'inherit' });
    child.once('error', reject);
    child.once('exit', code => (code === 0 ? resolveRun() : reject(new Error(`${command} exited ${code}`))));
  });

try {
  for (const app of ['admin', 'home', 'sso']) {
    const source = join(root, 'apps', `${app}-web`, 'dist');
    await stat(join(source, 'index.html'));
    // 并行开发可能重建 dist；每次浏览器验收只消费开始时的自有副本。
    await cp(source, join(temporary, app), { recursive: true });
  }
  await run('openssl', [
    'req',
    '-x509',
    '-newkey',
    'rsa:2048',
    '-nodes',
    '-keyout',
    join(temporary, 'key.pem'),
    '-out',
    join(temporary, 'cert.pem'),
    '-days',
    '1',
    '-subj',
    '/CN=127.0.0.1',
    '-addext',
    'subjectAltName=IP:127.0.0.1'
  ]);
  const options = {
    key: await readFile(join(temporary, 'key.pem')),
    cert: await readFile(join(temporary, 'cert.pem'))
  };
  const evidenceRoot = process.env.A11Y_EVIDENCE_DIR || join(root, 'tests/e2e/reports/accessibility-evidence');
  await mkdir(evidenceRoot, { recursive: true });
  const evidence = await mkdtemp(join(evidenceRoot, 'run-'));
  console.log(`Accessibility evidence: ${evidence}`);
  const env = { ...process.env, A11Y_EVIDENCE_DIR: evidence };
  for (const app of ['admin', 'home', 'sso']) {
    const directory = join(temporary, app);
    const server = createServer(options, (request, response) => {
      void (async () => {
        const path = resolve(
          directory,
          '.' + decodeURIComponent(new URL(request.url || '/', 'https://127.0.0.1').pathname)
        );
        if (path !== directory && !path.startsWith(directory + sep)) {
          response.writeHead(403).end();
          return;
        }
        const file = await stat(path)
          .then(value => (value.isFile() ? path : join(directory, 'index.html')))
          .catch(() => join(directory, 'index.html'));
        response.writeHead(200, { 'Content-Type': types[extname(file)] || 'application/octet-stream' });
        response.end(await readFile(file));
      })().catch(() => response.writeHead(500).end());
    });
    await new Promise((resolveListen, reject) => {
      server.once('error', reject);
      server.listen(0, '127.0.0.1', resolveListen);
    });
    servers.push(server);
    const address = server.address();
    if (!address || typeof address === 'string') throw new Error('Missing owned fixture address');
    env[`A11Y_${app.toUpperCase()}_ORIGIN`] = `https://127.0.0.1:${address.port}`;
  }
  await run(
    'pnpm',
    [
      'exec',
      'playwright',
      'test',
      '--config',
      process.env.UI_PLAYWRIGHT_CONFIG || 'playwright.accessibility.config.ts',
      ...process.argv.slice(2)
    ],
    env
  );
} finally {
  await Promise.all(
    servers.map(
      server =>
        new Promise(resolveClose => {
          server.close(resolveClose);
          server.closeAllConnections();
        })
    )
  );
  await rm(temporary, { recursive: true, force: true });
}
