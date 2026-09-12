import { defineConfig, devices } from '@playwright/test';

const adminWebUrl = 'http://127.0.0.1:4173';

export default defineConfig({
  testDir: './e2e',
  outputDir: './tests/e2e/reports/results',
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? [['line'], ['html', { outputFolder: './tests/e2e/reports/html', open: 'never' }]] : 'line',
  use: {
    baseURL: adminWebUrl,
    trace: 'on-first-retry'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ],
  webServer: {
    command: 'pnpm build:prod && pnpm --filter @namewta/admin-web preview',
    url: `${adminWebUrl}/login`,
    reuseExistingServer: !process.env.CI,
    timeout: 180_000
  }
});
