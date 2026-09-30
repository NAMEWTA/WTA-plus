import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { systemSsoAppResource } from '@namewta/domain-system';

const source = readFileSync(resolve(dirname(fileURLToPath(import.meta.url)), 'SsoAppPage.vue'), 'utf8');

describe('SSO 管理 page', () => {
  it('is an independent create-and-deliver surface on /system/ssoApp', () => {
    expect(systemSsoAppResource.basePath).toBe('/system/ssoApp');
    expect(systemSsoAppResource.controller).toBe('SysSsoAppController');
    expect(source).toContain('data-testid="sso-admin-title"');
    expect(source).toContain('data-testid="sso-admin-create"');
    expect(source).toContain('data-testid="sso-admin-deliver"');
    expect(source).toContain('自有应用 SSO');
    expect(source).toContain('创建应用');
    expect(source).toContain('拿配置');
    expect(source).toContain('精确回调');
    expect(source).toContain('为 Admin、Home 等自有客户端配置登录回调与接入信息');
    expect(source).toContain('runtime.service.ssoApps');
    expect(source).toContain("openDialog('创建 SSO 应用')");
    expect(source).not.toContain('没有接入');
    expect(source).not.toContain('/system/client');
    expect(source).not.toContain('runtime.service.clients');
  });
});
