import { describe, expect, it } from 'vitest';
import { createSSRApp, h } from 'vue';
import { renderToString } from 'vue/server-renderer';
import AppShell from './AppShell.vue';
import { createIconResolver } from './iconRegistry';
describe('shared authenticated shell', () => {
  it.each([false, true])(
    'preserves host content and optional tabs=%s without a second content wrapper',
    async showTabs => {
      const html = await renderToString(
        createSSRApp({
          render: () =>
            h(
              AppShell,
              { showTabs, fixedHeader: true },
              {
                sidebar: () => h('aside', { class: 'sidebar-container' }, '菜单'),
                navbar: () => h('nav', '账户'),
                tabs: () => h('div', { id: 'tags-view-container' }, '页签'),
                default: ({ contentClass }: { contentClass: Record<string, boolean> }) =>
                  h('section', { class: ['app-main', contentClass] }, '内容')
              }
            )
        })
      );
      expect(html.match(/class="app-main/g)).toHaveLength(1);
      expect(html).toContain('main-container');
      expect(html).toContain('fixed-header');
      expect(html.includes('tags-view-container')).toBe(showTabs);
      expect(html.includes('with-tags-view')).toBe(showTabs);
    }
  );
  it('exposes an accessible mobile close action and honors hidden sidebar', async () => {
    const html = await renderToString(
      createSSRApp({ render: () => h(AppShell, { mobile: true, sidebarOpened: true }) })
    );
    expect(html).toContain('aria-label="关闭导航菜单"');
    const hidden = await renderToString(
      createSSRApp({
        render: () =>
          h(
            AppShell,
            { mobile: true, sidebarOpened: true, sidebarVisible: false },
            { sidebar: () => h('aside', '不可见导航') }
          )
      })
    );
    expect(hidden).not.toContain('不可见导航');
    expect(hidden).not.toContain('drawer-bg');
    expect(hidden).toContain('sidebarHide');
  });
  it('gives local sprites priority and shares offline Tabler fallback', () => {
    const resolve = createIconResolver(['user']);
    expect(resolve('user').kind).toBe('local');
    expect(resolve('tabler:user').value).toBe('tabler:user');
    expect(resolve('building').value).toBe('tabler:building');
    expect(resolve('does-not-exist')).toMatchObject({ value: 'tabler:help-circle', fallback: true });
  });
});
