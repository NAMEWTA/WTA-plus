import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  testMatch: 'session-security.spec.ts',
  outputDir: '../../tests/e2e/reports/sso-security',
  fullyParallel: false,
  workers: 1,
  reporter: 'line',
  use: {
    browserName: 'chromium',
    channel: 'chrome',
    ignoreHTTPSErrors: true,
    trace: 'off'
  }
});
