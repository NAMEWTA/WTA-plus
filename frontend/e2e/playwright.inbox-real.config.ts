import { defineConfig, devices } from '@playwright/test';

// 专用选集由隔离环境驱动提供前后端；默认模拟 API 的选集不发现 .e2e.ts。
export default defineConfig({
  testDir: '.',
  testMatch: 'inbox-real.e2e.ts',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: 'line',
  use: { ...devices['Desktop Chrome'], channel: 'chrome', trace: 'retain-on-failure' }
});
