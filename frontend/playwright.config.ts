import { defineConfig, devices } from '@playwright/test';

const adminWebUrl = 'http://127.0.0.1:4173';

export default defineConfig({
  testDir: './e2e',
  testIgnore: [/required-phone\.spec\.ts/, /enterprise-transfer-queue\.spec\.ts/, /public-accessibility\.spec\.ts/, /workflow-task-integrity\.spec\.ts/, /sso-(three-gates|admin-config|callback-journey|release-origin)\.spec\.ts/, /browser-https-transport\.spec\.ts/, /session-navigation-lifecycle\.spec\.ts/, /recoverable-registration\.spec\.ts/, /upload-import-lifecycle\.spec\.ts/, /profile-self-materials\.spec\.ts/],
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
