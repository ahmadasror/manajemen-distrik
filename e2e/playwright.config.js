// @ts-check
const { defineConfig, devices } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './tests',

  /* Maximum time for each test */
  timeout: 30_000,

  /* Assertion timeout */
  expect: { timeout: 10_000 },

  /* Run tests sequentially — maker-checker workflow requires ordered state */
  fullyParallel: false,
  workers: 1,
  retries: 0,

  /* Reporter */
  reporter: [
    ['html', { open: 'never', outputFolder: 'playwright-report' }],
    ['list'],
    ['./reporters/dashboard-reporter.js'],
  ],

  /* Global setup: saves admin auth state before tests run */
  globalSetup: require.resolve('./global-setup'),

  use: {
    /* Frontend dev server */
    baseURL: 'https://localhost:5173',

    /* Accept self-signed mkcert certificates */
    ignoreHTTPSErrors: true,

    /* Backend API base URL (used in helpers) */
    extraHTTPHeaders: {
      'Content-Type': 'application/json',
    },

    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'off',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
