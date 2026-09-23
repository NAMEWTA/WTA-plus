import { defineConfig, devices } from '@playwright/test';

// 隔离环境由专用 runner 持有；关闭会保存登录凭据或 Bearer 的浏览器工件。
export default defineConfig({
  testDir: '.',
  testMatch: 'inbox-paged-real.e2e.ts',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: 'line',
  use: {
    ...devices['Desktop Chrome'],
    channel: 'chrome',
    trace: 'off',
    video: 'off',
    screenshot: 'off'
  },
  projects: [{ name: 'chromium', use: {} }]
});
