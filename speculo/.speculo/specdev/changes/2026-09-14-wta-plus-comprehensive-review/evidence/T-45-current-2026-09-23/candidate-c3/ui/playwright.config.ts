import { defineConfig, devices } from '@playwright/test';

// All identities and S3 requests are owned by the private runner. No browser
// network, login body, token, screenshot or trace may be retained as evidence.
export default defineConfig({
  testDir: '.',
  testMatch: 'oss-diagnostic-real.spec.ts',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: 'line',
  use: {
    ...devices['Desktop Chrome'],
    channel: 'chrome',
    trace: 'off',
    video: 'off',
    screenshot: 'off'
  },
  projects: [{ name: 'chromium', use: {} }]
});
