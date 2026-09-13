import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const source = readFileSync(resolve(dirname(fileURLToPath(import.meta.url)), 'ClientPage.vue'), 'utf8');

describe('ClientPage SSO bind surface', () => {
  it('shows bind success vs 没有接入 and is not the create path', () => {
    expect(source).toContain('data-testid="sso-access-success"');
    expect(source).toContain('已接入');
    expect(source).toContain('data-testid="sso-access-missing"');
    expect(source).toContain('没有接入');
    expect(source).toContain('data-testid="sso-bind-own-app"');
    expect(source).toContain('SSO 管理');
    expect(source).toContain('bindSsoAccess');
    expect(source).toContain('ssoAccessState');
    expect(source).not.toContain('创建 SSO 应用');
    expect(source).not.toContain('请立即保存 SSO 密钥');
    expect(source).not.toContain('ssoApps.add');
  });
});
