import { describe, expect, it } from 'vitest';
import page from './OssConfigPage.vue?raw';

describe('OSS config access policy page', () => {
  it('offers only the supported semantic access policies', () => {
    expect(page).toContain('<el-radio value="PRIVATE">PRIVATE</el-radio>');
    expect(page).toContain('<el-radio value="PUBLIC_READ">PUBLIC_READ</el-radio>');
    expect(page).not.toMatch(/custom|value="1"/i);
  });

  it('keeps public configs non-default and requires their production domain', () => {
    expect(page).toContain(':disabled="scope.row.accessPolicy === \'PUBLIC_READ\'"');
    expect(page).toContain("if (isPublic) form.value.status = 'N'");
    expect(page).toContain("form.value.accessPolicy === 'PUBLIC_READ' && !value?.trim()");
    expect(page).toContain('PUBLIC_READ 在生产环境必须配置可公开访问的 domainUrl');
  });

  it('uses the default-state wording and labels icon-only row commands', () => {
    expect(page).toContain('label="是否默认"');
    expect(page).toContain('aria-label="修改"');
    expect(page).toContain('aria-label="删除"');
  });
});
