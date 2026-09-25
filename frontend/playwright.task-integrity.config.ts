import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  testMatch: 'workflow-task-integrity.spec.ts',
  outputDir: './tests/e2e/reports/task-integrity-results',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: 'line',
  use: {
    baseURL: 'http://127.0.0.1:4199',
    ...devices['Desktop Chrome'],
    channel: 'chrome',
    trace: 'off',
    screenshot: 'off'
  },
  webServer: {
    command: 'pnpm exec vite --config vite.task-integrity.config.ts',
    url: 'http://127.0.0.1:4199/packages/web-domains/workflow/src/components/fixtures/task-integrity/index.html',
    timeout: 120_000,
    reuseExistingServer: false
  }
});
