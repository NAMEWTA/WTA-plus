import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  testMatch: ['sso-three-gates.spec.ts', 'sso-admin-config.spec.ts', 'sso-callback-journey.spec.ts', 'sso-release-origin.spec.ts'],
  outputDir: './tests/e2e/reports/sso-results',
  fullyParallel: false,
  workers: 1,
  reporter: 'line',
  use: {
    baseURL: 'http://127.0.0.1:4174',
    trace: process.env.SSO_TEST_HTTPS_ORIGIN ? 'off' : 'retain-on-failure',
    ignoreHTTPSErrors: true
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        ...(process.env.SSO_E2E_CHROME === '0' ? {} : { channel: 'chrome' as const })
      }
    }
  ]
});
