import { defineConfig, devices } from '@playwright/test';

// 临时登录会话、Bearer 和 HTTP 正文不能进入浏览器工件。
export default defineConfig({
  testDir: '.',
  testMatch: 'notice-retraction-real.e2e.ts',
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
