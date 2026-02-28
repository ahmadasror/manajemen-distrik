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
 * Log in via the UI login form.
 * Waits for redirect to /dashboard before returning.
 * @param {import('@playwright/test').Page} page
 * @param {string} username
 * @param {string} password
 */
async function loginViaUI(page, username, password) {
  await page.goto('/login');
  await page.getByPlaceholder('Username').fill(username);
  await page.getByPlaceholder('Password').fill(password);
  await page.getByRole('button', { name: 'Sign In' }).click();
  await page.waitForURL('**/dashboard', { timeout: 10_000 });
}

/**
 * Log out via the header user menu.
 * Waits for redirect to /login before returning.
 * @param {import('@playwright/test').Page} page
 */
async function logoutViaUI(page) {
  // Click the user avatar/name in the header to open dropdown
  await page.locator('.ant-avatar, [class*="avatar"]').first().click();
  await page.getByText('Logout').click();
  await page.waitForURL('**/login', { timeout: 10_000 });
}

/**
 * Assert that an Ant Design success message is visible.
 * @param {import('@playwright/test').Page} page
 * @param {string} [textContains]
 */
async function expectSuccessMessage(page, textContains) {
  const msg = page.locator('.ant-message-success, [class*="ant-message-success"]').first();
  await expect(msg).toBeVisible({ timeout: 8_000 });
  if (textContains) await expect(msg).toContainText(textContains, { timeout: 8_000 });
}

/**
 * Assert that an Ant Design error message is visible.
 * @param {import('@playwright/test').Page} page
 * @param {string} [textContains]
 */
async function expectErrorMessage(page, textContains) {
  const msg = page.locator('.ant-message-error, [class*="ant-message-error"]').first();
  await expect(msg).toBeVisible({ timeout: 8_000 });
  if (textContains) await expect(msg).toContainText(textContains, { timeout: 8_000 });
}

/**
 * Fill an Ant Design Select component identified by its form label.
 * @param {import('@playwright/test').Page} page
 * @param {string} label       — The label text of the Form.Item
 * @param {string|string[]} values — One or more option texts to select
 */
async function fillAntSelect(page, label, values) {
  const items = Array.isArray(values) ? values : [values];
  // Find the Select by the label above it, then click the selector div
  const formItem = page
    .locator('.ant-form-item')
    .filter({ has: page.locator(`label:has-text("${label}")`) });
  // AntD 6.x uses .ant-select-content instead of .ant-select-selector
  await formItem.locator('.ant-select-content').click();
  await page.locator('.ant-select-dropdown').waitFor({ state: 'visible' });
  for (const val of items) {
    await page
      .locator('.ant-select-item-option')
      .filter({ hasText: val })
      .click();
  }
  // Close the dropdown
  await page.keyboard.press('Escape');
}

module.exports = { STATE_PATH, loginViaUI, logoutViaUI, expectSuccessMessage, expectErrorMessage, fillAntSelect };
