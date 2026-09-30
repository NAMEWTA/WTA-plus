import { describe, expect, it } from 'vitest';
import { createSSRApp, h } from 'vue';
import { renderToString } from 'vue/server-renderer';
import AuthPanel from './AuthPanel.vue';
import StatusPanel from './StatusPanel.vue';

describe('shared identity presentation', () => {
  it('gives each panel its own accessible heading and retains the host form action', async () => {
    const app = createSSRApp({
      render: () =>
        h('main', [
          h(
            AuthPanel,
            { title: '登录' },
            { default: () => h('form', { 'data-owner': 'login' }, h('button', { type: 'submit' }, '继续')) }
          ),
          h(AuthPanel, { title: '注册' })
        ])
    });
    const html = await renderToString(app);
    const labelled = [...html.matchAll(/aria-labelledby="([^"]+)"/g)].map(match => match[1]);
    const headings = [...html.matchAll(/<h1 id="([^"]+)"/g)].map(match => match[1]);
    expect(labelled).toEqual(headings);
    expect(new Set(headings).size).toBe(2);
    expect(html).toContain('data-owner="login"');
    expect(html).toContain('type="submit"');
    expect(html).toContain('继续');
  });

  it.each([false, true])('announces authorization error=%s using the correct live role', async error => {
    const html = await renderToString(
      createSSRApp({
        render: () =>
          h(
            StatusPanel,
            { title: '授权', message: error ? '请重试' : '正在检查', error },
            { default: () => h('button', '重试') }
          )
      })
    );
    expect(html).toContain(`role="${error ? 'alert' : 'status'}"`);
    expect(html).toContain('aria-live="polite"');
    expect(html).toContain(error ? '请重试' : '正在检查');
    expect(html).toContain('<button');
  });
});
