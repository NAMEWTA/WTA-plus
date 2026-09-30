/** 隔离验收专用 HTTPS 网关；只代理指定本机后端，不记录协议查询和认证信息。 */
import { spawnSync } from 'node:child_process';
import { cp, mkdir, readFile, stat } from 'node:fs/promises';
import { request as httpRequest } from 'node:http';
import { createServer } from 'node:https';
import { dirname, extname, join, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const directory = process.env.OIDC_ACCEPTANCE_DIR;
if (!directory || !directory.startsWith('/tmp/namewta-oidc-real-'))
  throw new Error('Owned acceptance directory required');
const tls = join(directory, 'gateway');
await mkdir(tls, { mode: 0o700, recursive: true });
const cert = join(tls, 'cert.pem');
const key = join(tls, 'key.pem');
const generated = spawnSync(
  'openssl',
  [
    'req',
    '-x509',
    '-newkey',
    'rsa:2048',
    '-nodes',
    '-keyout',
    key,
    '-out',
    cert,
    '-days',
    '1',
    '-subj',
    '/CN=localhost',
    '-addext',
    'subjectAltName=DNS:admin.localhost,DNS:home.localhost,DNS:sso.localhost,DNS:rp1.localhost,DNS:rp2.localhost'
  ],
  { stdio: 'ignore' }
);
if (generated.status !== 0) throw new Error('Fixture certificate creation failed');
const options = { key: await readFile(key), cert: await readFile(cert) };
const mime = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript',
  '.css': 'text/css',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.json': 'application/json',
  '.woff2': 'font/woff2',
  '.ico': 'image/x-icon'
};
const servers = [];
for (const [app, port] of [
  ['admin', 19441],
  ['home', 19442],
  ['sso', 19443]
]) {
  const dist = join(tls, app);
  await cp(join(root, 'apps', `${app}-web`, 'dist'), dist, { recursive: true });
  const origin = `https://${app}.localhost:${port}`;
  const server = createServer(options, (request, response) => {
    void (async () => {
      const url = new URL(request.url, origin);
      const api =
        app === 'sso'
          ? /^\/(?:sso(?:\/|$)|oidc(?:\/|$)|\.well-known\/)/.test(url.pathname)
          : url.pathname.startsWith('/prod-api/');
      if (api) {
        const path = app === 'sso' ? url.pathname + url.search : url.pathname.slice('/prod-api'.length) + url.search;
        const upstream = httpRequest(
          {
            hostname: '127.0.0.1',
            port: 18888,
            path,
            method: request.method,
            headers: {
              ...request.headers,
              host: new URL(origin).host,
              'x-forwarded-proto': 'https',
              'x-forwarded-host': new URL(origin).host,
              'x-forwarded-port': String(port),
              'x-forwarded-for': '127.0.0.1'
            }
          },
          res => {
            response.writeHead(res.statusCode, res.headers);
            res.pipe(response);
          }
        );
        upstream.on('error', () => {
          if (!response.headersSent) response.writeHead(502);
          response.end();
        });
        request.pipe(upstream);
        return;
      }
      const path = resolve(dist, '.' + decodeURIComponent(url.pathname));
      if (path !== dist && !path.startsWith(dist + sep)) {
        response.writeHead(403).end();
        return;
      }
      const file = await stat(path)
        .then(value => (value.isFile() ? path : join(dist, 'index.html')))
        .catch(() => join(dist, 'index.html'));
      response.writeHead(200, {
        'Content-Type': mime[extname(file)] ?? 'application/octet-stream',
        'Cache-Control': 'no-store'
      });
      response.end(await readFile(file));
    })().catch(() => {
      if (!response.headersSent) response.writeHead(500);
      response.end();
    });
  });
  await new Promise((ok, fail) => {
    server.once('error', fail);
    server.listen(port, '127.0.0.1', ok);
  });
  servers.push(server);
}
console.log('Owned Admin/Home/SSO HTTPS gateways ready on 19441/19442/19443');
for (const signal of ['SIGINT', 'SIGTERM'])
  process.once(signal, () => {
    for (const server of servers) {
      server.close();
      server.closeAllConnections();
    }
  });
