import { defineConfig, devices } from '@playwright/test';
export default defineConfig({
  testDir: './e2e',
  testMatch: ['authenticated-profile.spec.ts', 'profile-management.spec.ts', 'enterprise-transfer-queue.spec.ts'],
  outputDir: './tests/e2e/reports/profile-ui-results',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 30_000,
  reporter: 'line',
  use: {
    baseURL: process.env.A11Y_ADMIN_ORIGIN,
    ...devices['Desktop Chrome'],
    channel: 'chrome',
    ignoreHTTPSErrors: true,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure'
  }
});
