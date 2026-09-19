import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  testMatch: 'recoverable-registration.spec.ts',
  outputDir: './tests/e2e/reports/registration-results',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 30_000,
  reporter: 'line',
  use: { ...devices['Desktop Chrome'], channel: 'chrome', ignoreHTTPSErrors: true, trace: 'off', screenshot: 'off' }
});
