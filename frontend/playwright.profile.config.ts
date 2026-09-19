import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e', testMatch: 'profile-self-materials.spec.ts',
  outputDir: './tests/e2e/reports/profile-results',
  workers: 1, fullyParallel: false, retries: 0, maxFailures: 1, timeout: 45_000, reporter: 'line',
  use: { ...devices['Desktop Chrome'], channel: 'chrome', trace: 'off', screenshot: 'off' }
});
