import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  testMatch: 'browser-https-transport.spec.ts',
  outputDir: './tests/e2e/reports/transport-results',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: 'line',
  use: { ...devices['Desktop Chrome'], channel: 'chrome', ignoreHTTPSErrors: true, trace: 'off', screenshot: 'off' }
});
