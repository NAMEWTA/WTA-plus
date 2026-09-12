import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { createNotifyWebDomain } from './index';

describe('通知 Web Domain', () => {
  it('显式注册监控页面和权限', () => {
    const manifest = createNotifyWebDomain({
      service: {} as never,
      hasPermission: () => true,
      navigate: () => undefined,
      dicts: () => ({}),
      directory: {
        searchUsers: async () => ({ data: { rows: [], total: 0 } }),
        usersByIds: async () => ({ data: [] }),
        userTypes: async () => ({ data: [] })
      }
    });
    expect(manifest.id).toBe('web-domain-notify');
    expect(manifest.registrations.map(item => item.componentKey)).toEqual([
      'notify/monitor/index',
      'notify/notice/index',
      'notify/inbox/index',
      'notify/config/index'
    ]);
    expect(manifest.permissions[0]?.permissions).toContain('notify:monitor:list');
    expect(manifest.permissions.map(item => item.id)).toContain('notify-config');
    expect(manifest.permissions.find(item => item.id === 'notify-config')?.permissions).toEqual([
      'notify:config:list',
      'notify:config:query',
      'notify:config:add',
      'notify:config:edit',
      'notify:config:remove',
      'notify:config:test'
    ]);
  });

  it('配置页提供邮件/短信 TAB、变量占用和试发，且短信无自由正文', () => {
    const page = readFileSync(join(dirname(fileURLToPath(import.meta.url)), 'ConfigPage.vue'), 'utf8');
    expect(page).toContain('label="邮件" name="MAIL"');
    expect(page).toContain('label="短信" name="SMS"');
    expect(page).toContain('已占用变量');
    expect(page).toContain('测试发送');
    expect(page).toContain('notify:config:test');
    expect(page).toContain('smsTemplateCode');
    expect(page).toContain('禁止自由正文');
    const smsSceneBlock = page.slice(page.lastIndexOf('<template v-else>'));
    expect(smsSceneBlock).toContain('smsTemplateCode');
    expect(smsSceneBlock).not.toContain('type="textarea"');
  });
});
