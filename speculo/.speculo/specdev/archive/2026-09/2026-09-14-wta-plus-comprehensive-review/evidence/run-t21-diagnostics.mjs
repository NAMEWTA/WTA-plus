import { createRequire } from 'node:module';
import { mkdir, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
const folder = dirname(fileURLToPath(import.meta.url));
const root = resolve(folder, '../../../../../..');
const { chromium } = createRequire(resolve(root, 'frontend/package.json'))('@playwright/test');
const name = process.argv[2];
if (!/^T-21-[a-z-]+$/.test(name)) throw new Error('Owned evidence name required');
const shotDir = resolve(folder, name); await mkdir(shotDir, { recursive: true });
const browser = await chromium.launch({ channel: 'chrome' });
const output = [];
const policy = { minimumLength: 8, maximumLength: 30, requiredCharacterClasses: ['UPPERCASE', 'LOWERCASE', 'DIGIT', 'SPECIAL'], allowedSpecialCharacters: '@$!%*?&' };
try {
  const context = await browser.newContext({ ignoreHTTPSErrors: true });
  const page = await context.newPage();
  const cdp = await context.newCDPSession(page);
  await page.route('**/prod-api/**', route => {
    const path = new URL(route.request().url()).pathname;
    const data = path.endsWith('/auth/client/context') ? { clientEnabled: true, registerEnabled: true, passwordPolicy: policy, authMode: 'both', ssoEnabled: true, ssoAuthorizeUrl: process.env.A11Y_SSO_ORIGIN + '/authorize' }
      : path.endsWith('/auth/code') ? { captchaEnabled: true, img: 'R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==', uuid: 'owned' } : [];
    return route.fulfill({ json: { code: 200, data } });
  });
  await page.route('**/sso/session', route => route.fulfill({ json: { code: 401, msg: '未登录' } }));
  for (const [app, path, selector] of [['home', '/login', '.identity-login'], ['home', '/register', '.register-page'], ['home', '/sso/callback', '.sso-callback'], ['sso', '/authorize', '.sso-card']]) {
    for (const width of [320, 768, 1440]) {
      for (const zoom of [1, 2]) {
        const cssWidth = width / zoom; const cssHeight = 900 / zoom;
        await page.setViewportSize({ width: cssWidth, height: cssHeight });
        await cdp.send('Emulation.setDeviceMetricsOverride', { width: cssWidth, height: cssHeight, deviceScaleFactor: zoom, mobile: false });
        await page.goto(process.env[`A11Y_${app.toUpperCase()}_ORIGIN`] + path);
        await page.locator(selector).waitFor();
        if (path === '/login' || path === '/register') await page.locator('input[name=username]:enabled').waitFor({state: 'visible'});
        if (app === 'sso') await page.locator('input[name=password]').waitFor({state: 'visible'});
        const value = await page.evaluate(selector => {
          const scope = document.querySelector(selector);
          const box = scope.getBoundingClientRect();
          const styles = [...scope.querySelectorAll('h1,p,label,input,button,a')].map(node => {
            const style = getComputedStyle(node); const rect = node.getBoundingClientRect();
            return { tag: node.tagName, text: node.textContent?.trim(), color: style.color, background: style.backgroundColor, outline: style.outline, fontSize: style.fontSize, rect: { left: rect.left, right: rect.right, width: rect.width } };
          });
          return { viewport: innerWidth, scrollWidth: document.documentElement.scrollWidth, bodyWidth: document.body.scrollWidth, elementWidth: box.width, left: box.left, right: box.right, token: getComputedStyle(scope).getPropertyValue('--client-surface'), styles };
        }, selector);
        const shot = `${app}-${path.slice(1).replaceAll('/', '-')}-${width}-${zoom}x.png`;
        await page.screenshot({ path: resolve(shotDir, shot), fullPage: true });
        output.push({ app, path, viewportWidth: width, zoom, zoomMethod: 'CDP half CSS viewport + DPR2 for 200% desktop reflow; no CSS zoom', ...value, screenshot: shot });
      }
    }
  }
  await context.close();
} finally { await browser.close(); }
await writeFile(resolve(folder, `${name}-measurements.json`), JSON.stringify(output, null, 2) + '\n');
console.log(JSON.stringify(output.map(({app,path,viewportWidth,viewport,zoom,scrollWidth,bodyWidth,token})=>({app,path,viewportWidth,viewport,zoom,scrollWidth,bodyWidth,token})), null, 2));
