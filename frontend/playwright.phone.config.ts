import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  testMatch: 'required-phone.spec.ts',
  outputDir: './tests/e2e/reports/phone-results',
  workers: 1,
  retries: 0,
  reporter: 'line',
  use: { ...devices['Desktop Chrome'], channel: 'chrome', trace: 'off' },
  webServer: [
    {
      command:
        'corepack pnpm --filter @namewta/admin-web build:prod && corepack pnpm --filter @namewta/admin-web preview',
      url: 'http://127.0.0.1:4173/login',
      timeout: 180_000
    },
    {
      command:
        'corepack pnpm --filter @namewta/home-web build:prod && corepack pnpm --filter @namewta/home-web exec vite preview --host 127.0.0.1 --port 4174 --strictPort',
      url: 'http://127.0.0.1:4174/login',
      timeout: 180_000
    }
  ]
});
