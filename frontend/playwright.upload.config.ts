import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  testMatch: 'upload-import-lifecycle.spec.ts',
  outputDir: './tests/e2e/reports/upload-results',
  workers: 1,
  fullyParallel: false,
  retries: 0,
  timeout: 35_000,
  reporter: 'line',
  use: { ...devices['Desktop Chrome'], channel: 'chrome', trace: 'off', screenshot: 'off' }
});
