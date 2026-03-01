/**
 * UI auth helpers — reusable login/logout actions for test specs.
 */

const { expect } = require('@playwright/test');

/** Storage state paths per role */
const STATE_PATH = {
  admin: './helpers/.auth/admin.json',
  viewer: './helpers/.auth/viewer.json',
  maker: './helpers/.auth/maker.json',
  checker: './helpers/.auth/checker.json',
};

/**
 * Log in via the UI.
 * Clicks "Sign In with Keycloak" → fills credentials on Keycloak login page
 * → waits for redirect to /dashboard.
 * @param {import('@playwright/test').Page} page
 * @param {string} username
 * @param {string} password
 */
async function loginViaUI(page, username, password) {
  await page.goto('/login');
  await page.getByRole('button', { name: 'Sign In with Keycloak' }).click();
  // Wait for Keycloak login page to fully load before interacting
  await page.waitForURL(/8180/, { waitUntil: 'load', timeout: 15_000 });
  await page.locator('#username').waitFor({ state: 'visible' });
  await page.locator('#username').fill(username);
  await page.locator('#password').fill(password);
  await page.locator('#kc-login').click();
  await page.waitForURL('**/dashboard', { timeout: 15_000 });
}

/**
 * Log out via the header user menu.
 * Clicks the avatar/user button → clicks Logout.
 * @param {import('@playwright/test').Page} page
 */
async function logoutViaUI(page) {
  // Click the user dropdown trigger button in the header
  await page.locator('header button').click();
  await page.getByText('Logout').click();
  // Wait for redirect away from the app (Keycloak logout flow)
  await page.waitForURL(/8180|\/login/, { timeout: 15_000 });
}

/**
 * Assert that a sonner success toast is visible.
 * @param {import('@playwright/test').Page} page
 * @param {string|RegExp} [textContains]
 */
async function expectSuccessMessage(page, textContains) {
  const msg = page.locator('[data-sonner-toast][data-type="success"]').first();
  await expect(msg).toBeVisible({ timeout: 8_000 });
  if (textContains) await expect(msg).toContainText(textContains, { timeout: 8_000 });
}

/**
 * Assert that a sonner error toast is visible.
 * @param {import('@playwright/test').Page} page
 * @param {string|RegExp} [textContains]
 */
async function expectErrorMessage(page, textContains) {
  const msg = page.locator('[data-sonner-toast][data-type="error"]').first();
  await expect(msg).toBeVisible({ timeout: 8_000 });
  if (textContains) await expect(msg).toContainText(textContains, { timeout: 8_000 });
}

/**
 * Select roles in the UserFormPage by clicking the role toggle buttons.
 * @param {import('@playwright/test').Page} page
 * @param {string} label       — ignored (kept for API compatibility)
 * @param {string|string[]} values — One or more role names to click (e.g. 'MAKER', ['ADMIN','VIEWER'])
 */
async function fillAntSelect(page, label, values) {
  const items = Array.isArray(values) ? values : [values];
  for (const val of items) {
    await page.getByRole('button', { name: val, exact: true }).click();
  }
}

module.exports = { STATE_PATH, loginViaUI, logoutViaUI, expectSuccessMessage, expectErrorMessage, fillAntSelect };
