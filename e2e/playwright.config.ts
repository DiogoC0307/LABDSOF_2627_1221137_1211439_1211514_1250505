import { defineConfig, devices } from '@playwright/test';

/**
 * The end-to-end test of the principal journey (SCRUM-100), in a browser against the Docker Compose stack: `web` on
 * port 8000 in front of `escala-api`, with `elegibilidade-worker` evaluating for real. Synthetic data only.
 *
 * - Lisbon time and a Portuguese locale, whatever the machine's zone: "Next week", Saturday and a night shift that
 *   crosses midnight must not change with the zone of the CI runner (UTC).
 * - The service worker is blocked, so the PWA's cache never serves the app of an earlier stack.
 * - No retries: a test that passes only on the second try is a problem to fix, not to hide. The trace and a screenshot
 *   are kept when a test fails, in the report that CI uploads as an artifact.
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  forbidOnly: !!process.env['CI'],
  // the whole run must stay under 5 minutes (SCRUM-100)
  globalTimeout: 5 * 60_000,
  timeout: 2 * 60_000,
  expect: { timeout: 10_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:8000',
    timezoneId: 'Europe/Lisbon',
    locale: 'pt-PT',
    serviceWorkers: 'block',
    viewport: { width: 1280, height: 900 },
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 900 } } }],
});
