import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const source = readFileSync(resolve(dirname(fileURLToPath(import.meta.url)), 'ClientPage.vue'), 'utf8');

describe('ClientPage external identity entry and legacy compatibility', () => {
  it('shows configured/enabled provider counts while labelling the old SSO state as compatibility only', () => {
    expect(source).toContain('外部登录接入');
    expect(source).toContain('enabledProviderCount');
    expect(source).toContain('providerCount');
    expect(source).toContain('clientOptions({ clientIds }');
    expect(source).toContain("path: '/system/externalAuthRegistration'");
    expect(source).toContain('旧第一方 SSO（兼容）');
    expect(source).toContain('data-testid="sso-access-success"');
    expect(source).toContain('已接入');
    expect(source).toContain('data-testid="sso-access-missing"');
    expect(source).toContain('没有接入');
    expect(source).toContain('data-testid="sso-bind-own-app"');
    expect(source).toContain('不代表当前 OIDC 外部登录接入状态');
    expect(source).toContain('bindSsoAccess');
    expect(source).toContain('ssoAccessState');
    expect(source).not.toContain('创建 SSO 应用');
    expect(source).not.toContain('请立即保存 SSO 密钥');
    expect(source).not.toContain('ssoApps.add');
  });
});
