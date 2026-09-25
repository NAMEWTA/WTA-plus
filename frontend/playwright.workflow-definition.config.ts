import { defineConfig, devices } from '@playwright/test';

const adminWebUrl = 'http://127.0.0.1:4173';

export default defineConfig({
  testDir: './e2e',
  testMatch: 'workflow-definition.spec.ts',
  outputDir: './tests/e2e/reports/workflow-definition-results',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: 'line',
  use: {
    baseURL: adminWebUrl,
    ...devices['Desktop Chrome'],
    channel: 'chrome',
    trace: 'off',
    screenshot: 'off'
  },
  webServer: {
    command: 'pnpm build:prod && pnpm --filter @namewta/admin-web preview',
    url: `${adminWebUrl}/login`,
    reuseExistingServer: false,
    timeout: 180_000
  }
});
