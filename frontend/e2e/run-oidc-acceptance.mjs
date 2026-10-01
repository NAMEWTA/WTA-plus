import { chromium } from '@playwright/test';
/** 真实 MySQL/Redis 后端和两个机密 RP 的隔离验收；从不打印 token、密钥或协议查询。 */
import { createHash, createPublicKey, randomBytes, verify } from 'node:crypto';
import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { request as httpRequest } from 'node:http';
import { createServer, request as httpsRequest } from 'node:https';
import { join } from 'node:path';
import { startFirstPartyAuthorization } from './first-party-sso-fixture.mjs';

const directory = process.env.OIDC_ACCEPTANCE_DIR;
if (!directory?.startsWith('/tmp/namewta-oidc-real-')) throw new Error('Owned acceptance directory required');
const issuer = 'https://sso.localhost:19443';
const random = () => randomBytes(32).toString('base64url');
const hash = value => createHash('sha256').update(value).digest('base64url');
const requireValue = (condition, label) => {
  if (!condition) throw new Error(label);
};
const evidence = [];
const pass = label => {
  evidence.push(label);
  console.log(`PASS ${label}`);
};
let step = 'initialization';
let browser;
let activePage;
const diagnostics = [];
const createdApplications = [];
const servers = [];

/** 固定连接自有 loopback 服务；TLS 仅信任本次现场证书。 */
function request(url, { method = 'GET', headers = {}, body } = {}) {
  const parsed = new URL(url);
  requireValue(
    ['127.0.0.1', 'sso.localhost', 'admin.localhost', 'home.localhost'].includes(parsed.hostname),
    'fixture request origin'
  );
  const transport = parsed.protocol === 'https:' ? httpsRequest : httpRequest;
  return new Promise((ok, fail) => {
    const req = transport(
      parsed,
      {
        method,
        headers,
        ca: certificate,
        lookup: (_host, opts, callback) =>
          opts.all ? callback(null, [{ address: '127.0.0.1', family: 4 }]) : callback(null, '127.0.0.1', 4)
      },
      res => {
        const chunks = [];
        res.on('data', part => chunks.push(part));
        res.on('end', () => {
          const text = Buffer.concat(chunks).toString();
          let data;
          try {
            data = JSON.parse(text);
          } catch {
            /* HTML is valid for redirects/interaction. */
          }
          ok({ status: res.statusCode, headers: res.headers, data, text });
        });
      }
    );
    req.setTimeout(15000, () => req.destroy(new Error('fixture request timeout')));
    req.on('error', fail);
    req.end(body);
  });
}
const certificate = await readFile(join(directory, 'gateway/cert.pem'));
const tls = { cert: certificate, key: await readFile(join(directory, 'gateway/key.pem')) };
const credentials = JSON.parse(await readFile(join(directory, 'browser-credentials.json'), 'utf8'));
let adminToken;
async function admin(path, body) {
  const result = await request(`http://127.0.0.1:18888${path}`, {
    method: body ? 'POST' : 'GET',
    headers: {
      clientid: credentials.clientId,
      ...(adminToken ? { Authorization: `Bearer ${adminToken}` } : {}),
      'Content-Type': 'application/json'
    },
    body: body ? JSON.stringify(body) : undefined
  });
  requireValue(result.status === 200 && result.data?.code === 200, `admin API ${path.replace(/\d+/g, ':id')}`);
  return result.data.data;
}
function clientRequest(app, endpoint, params) {
  const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
  const body = new URLSearchParams(params);
  if (app.application.clientAuthenticationMethod === 'client_secret_post') {
    body.set('client_id', app.application.clientId);
    body.set('client_secret', app.clientSecret);
  } else
    headers.Authorization = `Basic ${Buffer.from(`${app.application.clientId}:${app.clientSecret}`).toString('base64')}`;
  return request(`${issuer}/oidc/${endpoint}`, { method: 'POST', headers, body: body.toString() });
}
async function token(app, params) {
  return clientRequest(app, 'token', params);
}
async function userinfo(accessToken) {
  return request(`${issuer}/oidc/userinfo`, { headers: { Authorization: `Bearer ${accessToken}` } });
}

/** 验证算法、kid、公钥签名、issuer/audience/nonce/有效期和 UserInfo subject。 */
async function validateIdToken(app, tokens, nonce) {
  requireValue(typeof tokens.id_token === 'string' && typeof tokens.access_token === 'string', 'token response fields');
  requireValue(!tokens.refresh_token && !tokens.access_token.includes('.'), 'opaque access token without refresh');
  const parts = tokens.id_token.split('.');
  requireValue(parts.length === 3, 'JWT shape');
  const header = JSON.parse(Buffer.from(parts[0], 'base64url'));
  const claims = JSON.parse(Buffer.from(parts[1], 'base64url'));
  const jwks = (await request(`${issuer}/oidc/jwks`)).data;
  requireValue(header.alg === 'RS256' && jwks.keys.every(key => !key.d && !key.p && !key.q), 'public RS256 JWKS');
  const key = jwks.keys.find(value => value.kid === header.kid);
  requireValue(
    key &&
      verify(
        'RSA-SHA256',
        Buffer.from(`${parts[0]}.${parts[1]}`),
        createPublicKey({ key, format: 'jwk' }),
        Buffer.from(parts[2], 'base64url')
      ),
    'ID token signature'
  );
  requireValue(
    claims.iss === issuer &&
      (Array.isArray(claims.aud)
        ? claims.aud.includes(app.application.clientId)
        : claims.aud === app.application.clientId),
    'ID token issuer audience'
  );
  requireValue(
    claims.nonce === nonce && claims.exp > Date.now() / 1000 && claims.exp - claims.iat === 600,
    'ID token nonce and lifetime'
  );
  requireValue(
    typeof claims.sub === 'string' && claims.sub.length >= 32 && Number.isFinite(claims.auth_time),
    'stable subject and auth time'
  );
  return claims;
}
async function createRp(app, index) {
  const origin = `https://rp${index}.localhost:${19443 + index}`;
  const transactions = new Map();
  const rp = { app, origin, latest: undefined, failure: undefined, captured: undefined };
  const server = createServer(tls, (req, res) => {
    void (async () => {
      const url = new URL(req.url, origin);
      res.setHeader('Cache-Control', 'no-store');
      res.setHeader('Referrer-Policy', 'no-referrer');
      if (url.pathname === '/login') {
        rp.failure = undefined;
        const state = random(),
          nonce = random(),
          verifier = random();
        transactions.set(state, { nonce, verifier, capture: url.searchParams.has('capture') });
        res.setHeader('Set-Cookie', `__Host-rp-${index}=${state}; Path=/; Secure; HttpOnly; SameSite=Lax`);
        const params = new URLSearchParams({
          client_id: app.application.clientId,
          redirect_uri: `${origin}/callback`,
          response_type: 'code',
          scope: url.searchParams.get('scope') ?? 'openid profile email',
          state,
          nonce,
          code_challenge: hash(verifier),
          code_challenge_method: 'S256'
        });
        for (const key of ['prompt', 'max_age'])
          if (url.searchParams.has(key)) params.set(key, url.searchParams.get(key));
        res.writeHead(302, { Location: `${issuer}/oidc/authorize?${params}` }).end();
        return;
      }
      if (url.pathname === '/callback') {
        const state = url.searchParams.get('state');
        const transaction = transactions.get(state);
        requireValue(
          transaction && req.headers.cookie?.includes(`__Host-rp-${index}=${state}`),
          'RP state cookie binding'
        );
        transactions.delete(state);
        if (url.searchParams.has('error')) {
          rp.failure = url.searchParams.get('error');
          res.writeHead(303, { Location: '/error' }).end();
          return;
        }
        const code = url.searchParams.get('code');
        if (transaction.capture) {
          rp.captured = { code, ...transaction };
          res.writeHead(303, { Location: '/captured' }).end();
          return;
        }
        const exchange = await token(app, {
          grant_type: 'authorization_code',
          code,
          redirect_uri: `${origin}/callback`,
          code_verifier: transaction.verifier
        });
        requireValue(exchange.status === 200, 'RP code exchange');
        const identity = await validateIdToken(app, exchange.data, transaction.nonce);
        const profile = await userinfo(exchange.data.access_token);
        requireValue(profile.status === 200 && profile.data.sub === identity.sub, 'RP UserInfo subject');
        rp.latest = { tokens: exchange.data, identity, profile: profile.data, code, verifier: transaction.verifier };
        res.writeHead(303, { Location: '/profile' }).end();
        return;
      }
      if (url.pathname === '/logout') {
        requireValue(rp.latest, 'RP local session');
        res
          .writeHead(303, {
            Location: `${issuer}/oidc/logout?${new URLSearchParams({ id_token_hint: rp.latest.tokens.id_token, client_id: app.application.clientId, post_logout_redirect_uri: `${origin}/signed-out`, state: 'fixture-logout' })}`
          })
          .end();
        return;
      }
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(
        `<!doctype html><html lang="zh-CN"><title>OIDC acceptance RP ${index}</title><h1>${url.pathname === '/profile' ? '登录成功' : url.pathname.slice(1)}</h1></html>`
      );
    })().catch(error => {
      rp.failure = error.message;
      if (!res.headersSent) res.writeHead(500);
      res.end('Fixture validation failed');
    });
  });
  await new Promise((ok, fail) => {
    server.once('error', fail);
    server.listen(19443 + index, '127.0.0.1', ok);
  });
  servers.push(server);
  return rp;
}
async function login(page, rp, loginCredentials = credentials) {
  await page.goto(`${rp.origin}/login`);
  await page.locator('input[name="username"]').waitFor({ timeout: 20000 });
  await page.locator('input[name="username"]').fill(loginCredentials.username);
  await page.locator('input[name="password"]').fill(loginCredentials.password);
  await page.getByRole('button', { name: '登录并继续' }).click();
  await page.waitForURL(`${rp.origin}/profile`, { timeout: 30000 });
}

try {
  step = 'admin login';
  adminToken = (await admin('/auth/login', { ...credentials, grantType: 'password' })).access_token;
  requireValue(adminToken, 'admin token');
  step = 'discovery';
  const discovery = await request(`${issuer}/.well-known/openid-configuration`);
  requireValue(
    discovery.status === 200 && discovery.data.issuer === issuer && !('code' in discovery.data),
    'discovery raw protocol response'
  );
  requireValue(
    discovery.data.authorization_endpoint === `${issuer}/oidc/authorize` &&
      discovery.data.grant_types_supported.every(value => value === 'authorization_code'),
    'minimal protocol metadata'
  );
  pass('Discovery and minimal grant metadata');
  step = 'application creation';
  const applications = createdApplications;
  for (const index of [1, 2]) {
    const origin = `https://rp${index}.localhost:${19443 + index}`;
    applications.push(
      await admin('/oidc/admin/applications/create', {
        name: `Acceptance RP ${index} ${Date.now()}`,
        redirectUris: [`${origin}/callback`],
        postLogoutRedirectUris: [`${origin}/signed-out`],
        pkceRequired: true,
        clientAuthenticationMethod: index === 1 ? 'client_secret_basic' : 'client_secret_post'
      })
    );
  }
  const [rp1, rp2] = await Promise.all(applications.map((app, index) => createRp(app, index + 1)));
  diagnostics.push(rp1, rp2);
  step = 'invalid callback';
  const invalidRedirect = await request(
    `${issuer}/oidc/authorize?${new URLSearchParams({
      client_id: rp1.app.application.clientId,
      redirect_uri: 'https://unregistered.example.invalid/callback',
      response_type: 'code',
      scope: 'openid',
      state: random(),
      code_challenge: hash(random()),
      code_challenge_method: 'S256'
    })}`
  );
  requireValue(
    invalidRedirect.status === 400 && !invalidRedirect.headers.location,
    'unregistered callback must not redirect'
  );
  pass('Unregistered callback rejected locally');
  browser = await chromium.launch({
    headless: true,
    channel: 'chrome',
    args: ['--no-sandbox', '--host-resolver-rules=MAP *.localhost 127.0.0.1']
  });
  const context = await browser.newContext({ ignoreHTTPSErrors: true });
  const page = await context.newPage();
  activePage = page;
  page.setDefaultNavigationTimeout(30000);
  page.on('response', response => {
    const url = new URL(response.url());
    if (url.hostname === 'sso.localhost' && !url.pathname.startsWith('/assets/'))
      console.log(`HTTP ${response.status()} ${url.pathname} [${[...url.searchParams.keys()].join(',')}]`);
  });
  step = 'first RP browser login';
  await login(page, rp1);
  pass('RP1 real password login / code + S256 / RS256 ID token / UserInfo');
  const first = rp1.latest;
  step = 'second RP SSO';
  await page.goto(`${rp2.origin}/login`);
  await page.waitForURL(`${rp2.origin}/profile`, { timeout: 30000 });
  requireValue(rp2.latest.identity.sub === first.identity.sub, 'cross-app stable sub');
  pass('RP2 reuses central cookie with stable subject');
  step = 'token boundary';
  const business = await request('http://127.0.0.1:18888/system/user/getInfo', {
    headers: { Authorization: `Bearer ${first.tokens.access_token}`, clientid: credentials.clientId }
  });
  requireValue(business.data?.code !== 200, 'OIDC cannot authenticate business API');
  const wrong = await userinfo(adminToken);
  requireValue(wrong.status === 401, 'business token cannot authenticate UserInfo');
  pass('OIDC and business token isolation');
  step = 'cross-client introspection';
  const foreign = await clientRequest(rp2.app, 'introspect', { token: first.tokens.access_token });
  requireValue(foreign.data?.active === false, 'foreign token ownership');
  pass('Cross-client introspection isolation');
  step = 'authorization code replay';
  const replay = await token(rp1.app, {
    grant_type: 'authorization_code',
    code: first.code,
    redirect_uri: `${rp1.origin}/callback`,
    code_verifier: first.verifier
  });
  requireValue(replay.status === 400 && replay.data?.error === 'invalid_grant', 'replay invalid_grant');
  requireValue((await userinfo(first.tokens.access_token)).status === 401, 'replay revokes grant');
  pass('One-time code replay rejected and previous grant revoked');
  step = 'PKCE wrong verifier';
  await page.goto(`${rp1.origin}/login?capture=1`);
  await page.waitForURL(`${rp1.origin}/captured`, { timeout: 30000 });
  const invalidPkce = await token(rp1.app, {
    grant_type: 'authorization_code',
    code: rp1.captured.code,
    redirect_uri: `${rp1.origin}/callback`,
    code_verifier: random()
  });
  requireValue(invalidPkce.data?.error === 'invalid_grant', 'PKCE invalid_grant');
  pass('Wrong PKCE verifier rejected');
  step = 'application policy intersection';
  await page.goto(`${rp1.origin}/login`);
  await page.waitForURL(`${rp1.origin}/profile`, { timeout: 30000 });
  let app = await admin(`/oidc/admin/applications/${rp1.app.application.applicationId}`);
  app = await admin('/oidc/admin/applications/update', {
    ...app,
    allowedFields: ['preferred_username', 'phone_number']
  });
  const narrowed = await userinfo(rp1.latest.tokens.access_token);
  requireValue(
    narrowed.status === 200 &&
      !('email' in narrowed.data) &&
      !('nickname' in narrowed.data) &&
      !('phone_number' in narrowed.data),
    'policy intersection'
  );
  pass('Current policy removes claims immediately and old grants cannot gain fields');
  step = 'application disabled';
  app = await admin(`/oidc/admin/applications/${app.applicationId}/status`, { version: app.version, enabled: false });
  requireValue((await userinfo(rp1.latest.tokens.access_token)).status === 401, 'disabled app UserInfo');
  pass('Application disable revokes live token');
  step = 'RP initiated logout';
  await page.goto(`${rp2.origin}/logout`);
  const confirm = page.getByRole('button', { name: /退出|确认/ });
  if (await confirm.count()) await confirm.first().click();
  await page.waitForURL(url => url.origin === rp2.origin && url.pathname === '/signed-out', { timeout: 30000 });
  requireValue((await userinfo(rp2.latest.tokens.access_token)).status === 401, 'logout invalidates session grants');
  pass('RP initiated central logout');
  step = 'prompt none after logout';
  await page.goto(`${rp2.origin}/login?prompt=none`);
  await page.waitForURL(`${rp2.origin}/error`, { timeout: 30000 });
  requireValue(rp2.failure === 'login_required', 'prompt none requires login');
  pass('prompt=none correctly reports login_required');
  const normalFile = join(directory, 'normal-credentials.json');
  step = 'normal account independent of first-party RBAC';
  const normal = JSON.parse(await readFile(normalFile, 'utf8'));
  await login(page, rp2, normal);
  requireValue(rp2.latest.identity.sub !== first.identity.sub, 'normal account independent sub');
  pass('Normal account without first-party RBAC can log in to OIDC');
  step = 'nested profile disclosure';
  const expected = JSON.parse(await readFile(join(directory, 'expected-claims.json'), 'utf8'));
  let profileApp = await admin(`/oidc/admin/applications/${rp2.app.application.applicationId}`);
  const profileFields = [
    'preferred_username',
    'person_verified',
    'person_full_name',
    'person_document_number_masked',
    'enterprise_verified',
    'enterprise_name',
    'enterprise_legal_document_number_masked'
  ];
  profileApp = await admin('/oidc/admin/applications/update', { ...profileApp, allowedFields: profileFields });
  await page.goto(`${rp2.origin}/login?scope=${encodeURIComponent('openid profile wta_person wta_enterprise')}`);
  await page.waitForURL(`${rp2.origin}/profile`, { timeout: 30000 });
  const masked = rp2.latest.profile;
  requireValue(
    masked.wta_person?.document_number_masked === expected.personDocumentNumberMasked &&
      masked.wta_enterprise?.legal_document_number_masked === expected.enterpriseDocumentNumberMasked &&
      !masked.wta_person.document_number &&
      !masked.wta_enterprise.legal_document_number,
    'masked-only profile projection'
  );
  pass('Current personal/enterprise profiles nested and masked-only by policy');
  step = 'explicit full document disclosure';
  profileApp = await admin('/oidc/admin/applications/update', {
    ...profileApp,
    allowedFields: [...profileFields, 'person_document_number', 'enterprise_legal_document_number']
  });
  const oldProfile = await userinfo(rp2.latest.tokens.access_token);
  requireValue(
    !oldProfile.data.wta_person.document_number && !oldProfile.data.wta_enterprise.legal_document_number,
    'new sensitive fields cannot expand old snapshot'
  );
  await page.goto(`${rp2.origin}/login?scope=${encodeURIComponent('openid profile wta_person wta_enterprise')}`);
  await page.waitForURL(`${rp2.origin}/profile`, { timeout: 30000 });
  requireValue(
    rp2.latest.profile.wta_person?.document_number === expected.personDocumentNumber &&
      rp2.latest.profile.wta_enterprise?.legal_document_number === expected.enterpriseDocumentNumber,
    'explicit full documents'
  );
  pass('Full documents require explicit policy plus newly issued authorization');
  step = 'forced fresh authentication';
  await page.goto(`${rp2.origin}/login?prompt=login&scope=openid`);
  await page.locator('input[name="password"]').waitFor({ timeout: 20000 });
  await page.locator('input[name="username"]').fill(normal.username);
  await page.locator('input[name="password"]').fill(normal.password);
  await page.getByRole('button', { name: '登录并继续' }).click();
  await page.waitForURL(`${rp2.origin}/profile`, { timeout: 30000 });
  requireValue(Object.keys(rp2.latest.profile).length === 1 && rp2.latest.profile.sub, 'openid-only scope');
  pass('prompt=login requires password again and openid-only emits only sub');
  step = 'secret rotation';
  const oldSecret = rp2.app.clientSecret;
  const rotated = await admin(`/oidc/admin/applications/${profileApp.applicationId}/rotate-secret`, {
    version: profileApp.version
  });
  requireValue(
    rotated.clientSecret !== oldSecret && (await userinfo(rp2.latest.tokens.access_token)).status === 401,
    'rotation invalidation'
  );
  const oldAuthentication = await clientRequest(rp2.app, 'introspect', { token: rp2.latest.tokens.access_token });
  requireValue(
    [400, 401].includes(oldAuthentication.status) && oldAuthentication.data?.error === 'invalid_client',
    'old secret invalid_client'
  );
  pass('Secret rotation invalidates old credential and grants');
  step = 'first-party Admin SSO compatibility';
  const firstParty = await browser.newContext({ ignoreHTTPSErrors: true });
  const adminPage = await firstParty.newPage();
  await startFirstPartyAuthorization(adminPage, {
    app: 'admin',
    appOrigin: 'https://admin.localhost:19441',
    clientId: 'e5cd7e4891bf95d1d19206ce24a7b32e',
    authorizeUrl: `${issuer}/authorize`,
    returnTo: '/index'
  });
  await adminPage.locator('input[name="username"]').waitFor({ timeout: 20000 });
  await adminPage.locator('input[name="username"]').fill(credentials.username);
  await adminPage.locator('input[name="password"]').fill(credentials.password);
  await adminPage.getByRole('button', { name: '登录并继续' }).click();
  await adminPage.waitForURL(
    url => url.origin === 'https://admin.localhost:19441' && !['/login', '/sso/callback'].includes(url.pathname),
    { timeout: 30000 }
  );
  const adminSession = await adminPage.evaluate(() => localStorage.getItem('Admin-Token'));
  requireValue(adminSession, 'first-party Admin token');
  pass('Original Admin SSO browser login remains compatible');
  step = 'first-party Home central session reuse';
  const homePage = await firstParty.newPage();
  await startFirstPartyAuthorization(homePage, {
    app: 'home',
    appOrigin: 'https://home.localhost:19442',
    clientId: '428a8310cd442757ae699df5d894f051',
    authorizeUrl: `${issuer}/authorize`,
    returnTo: '/profile'
  });
  await homePage.waitForURL(
    url => url.origin === 'https://home.localhost:19442' && !['/login', '/sso/callback'].includes(url.pathname),
    { timeout: 30000 }
  );
  const homeSession = await homePage.evaluate(() => localStorage.getItem('Home-Token'));
  requireValue(
    homeSession && homeSession !== adminSession && (await homePage.locator('input[name="password"]').count()) === 0,
    'first-party Home session reuse'
  );
  const crossClient = await request('http://127.0.0.1:18888/system/user/getInfo', {
    headers: { Authorization: `Bearer ${adminSession}`, clientid: '428a8310cd442757ae699df5d894f051' }
  });
  requireValue(crossClient.data?.code !== 200, 'first-party client isolation');
  pass('Original Home SSO reuses session and preserves first-party client isolation');
  await firstParty.close();
  await mkdir(join(directory, 'evidence'), { recursive: true });
  await page.screenshot({ path: join(directory, 'evidence/rp-success.png') });
} catch (error) {
  console.error(`FAIL ${step}: ${error.name === 'TimeoutError' ? 'browser timeout' : error.message.split('\n')[0]}`);
  for (const rp of diagnostics) if (rp.failure) console.error(`RP validation: ${rp.failure}`);
  if (activePage) {
    const url = new URL(activePage.url());
    console.error(`Browser stopped at ${url.origin}${url.pathname}`);
    console.error(`Status: ${(await activePage.locator('[role="status"]').allTextContents()).join(' ')}`);
    await activePage
      .screenshot({ path: join(directory, 'failure.png'), mask: [activePage.locator('input')] })
      .catch(() => {});
  }
  process.exitCode = 1;
} finally {
  for (const delivery of createdApplications) {
    try {
      const current = await admin(`/oidc/admin/applications/${delivery.application.applicationId}`);
      await admin(`/oidc/admin/applications/${current.applicationId}/delete`, { version: current.version });
    } catch {
      console.error('Fixture cleanup failed for an owned application');
      process.exitCode = 1;
    }
  }
  await writeFile(
    join(directory, 'oidc-acceptance-results.json'),
    JSON.stringify({ passed: evidence, failedStep: process.exitCode ? step : null }, null, 2),
    { mode: 0o600 }
  );
  await browser?.close();
  await Promise.all(
    servers.map(
      server =>
        new Promise(ok => {
          server.close(ok);
          server.closeAllConnections();
        })
    )
  );
}
