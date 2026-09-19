import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  testMatch: 'enterprise-transfer-queue.spec.ts',
  outputDir: './tests/e2e/reports/transfer-results',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: 'line',
  use: { ...devices['Desktop Chrome'], channel: 'chrome', trace: 'off', screenshot: 'off' }
});
