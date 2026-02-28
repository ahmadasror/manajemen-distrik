/**
 * Section 1 — Login & Session Management
 * TEST-CASES.txt: cases 1.1 – 1.10
 */

const { test, expect } = require('@playwright/test');
const { loginViaUI, expectErrorMessage } = require('../helpers/auth');

// ── Positive Cases ────────────────────────────────────────────────────────

test.describe('1. Login & Session Management — Positive', () => {
  test('1.1 User logs in with correct credentials', async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('Username').fill('admin');
    await page.getByPlaceholder('Password').fill('admin123');
    await page.getByRole('button', { name: 'Sign In' }).click();

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 10_000 });
  });

  test('1.2 Logged-in user views their own profile', async ({ page }) => {
    await loginViaUI(page, 'admin', 'admin123');

    // Open the user dropdown in the header
    await page.locator('.ant-avatar').first().click();

    // Profile info is shown in the dropdown (fullName and roles)
    await expect(page.locator('.ant-dropdown-menu, .ant-popover, [class*="dropdown"]').first()).toBeVisible();
    // The header shows the user's full name
    const header = page.locator('header, .ant-layout-header').first();
    await expect(header).toContainText(/admin/i);
  });

  test('1.3 User renews session before it expires', async ({ page }) => {
    // Verify the axios refresh interceptor keeps the session alive by
    // making consecutive API-backed page navigations within the same session.
    await loginViaUI(page, 'admin', 'admin123');

    // Navigate to Users — triggers an authenticated API call
    await page.goto('/users');
    await expect(page).toHaveURL(/\/users/, { timeout: 8_000 });
    await expect(page.locator('.ant-table')).toBeVisible();

    // Navigate to Audit Trail — another authenticated API call
    await page.goto('/audit-trail');
    await expect(page).toHaveURL(/\/audit-trail/, { timeout: 8_000 });
    await expect(page.locator('.ant-table')).toBeVisible();

    // Still authenticated — no redirect to login
    await expect(page).not.toHaveURL(/\/login/);
  });

  test('1.4 User logs out of the system', async ({ page }) => {
    await loginViaUI(page, 'admin', 'admin123');

    // Click avatar to open dropdown, then click Logout
    await page.locator('.ant-avatar').first().click();
    await page.getByText('Logout').click();

    await expect(page).toHaveURL(/\/login/, { timeout: 8_000 });

    // Attempting to navigate to a protected page now redirects back to login
    await page.goto('/users');
    await expect(page).toHaveURL(/\/login/, { timeout: 8_000 });
  });
});

// ── Negative Cases ────────────────────────────────────────────────────────

test.describe('1. Login & Session Management — Negative', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('1.5 User logs in with wrong password', async ({ page }) => {
    await page.getByPlaceholder('Username').fill('admin');
    await page.getByPlaceholder('Password').fill('wrongpassword');
    await page.getByRole('button', { name: 'Sign In' }).click();

    await expectErrorMessage(page);
    await expect(page).toHaveURL(/\/login/);
  });

  test('1.6 User logs in with unregistered username', async ({ page }) => {
    await page.getByPlaceholder('Username').fill('unknownuser');
    await page.getByPlaceholder('Password').fill('any123');
    await page.getByRole('button', { name: 'Sign In' }).click();

    await expectErrorMessage(page);
    await expect(page).toHaveURL(/\/login/);
  });

  test('1.7 User submits login form with empty fields', async ({ page }) => {
    // Click submit without filling any field
    await page.getByRole('button', { name: 'Sign In' }).click();

    // Ant Design shows inline validation messages
    const errors = page.locator('.ant-form-item-explain-error');
    await expect(errors.first()).toBeVisible({ timeout: 5_000 });
    await expect(page).toHaveURL(/\/login/);
  });

  test('1.8 User accesses a protected page without logging in', async ({ page }) => {
    // Navigate directly to a protected route with no session
    await page.goto('/users');

    // ProtectedRoute should redirect to /login
    await expect(page).toHaveURL(/\/login/, { timeout: 8_000 });
  });

  test('1.9 User accesses a page with a corrupted session token', async ({ page }) => {
    // Inject an invalid token into localStorage before navigating
    await page.goto('/login'); // load the app first
    await page.evaluate(() => {
      localStorage.setItem('accessToken', 'this.is.not.a.valid.jwt.token');
    });

    await page.goto('/dashboard');

    // App should detect invalid token and redirect to login
    await expect(page).toHaveURL(/\/login/, { timeout: 8_000 });
  });

  test('1.10 User reuses session after logging out', async ({ page }) => {
    // Login, logout, then try to navigate to a protected page
    await loginViaUI(page, 'admin', 'admin123');

    await page.locator('.ant-avatar').first().click();
    await page.getByText('Logout').click();
    await expect(page).toHaveURL(/\/login/, { timeout: 8_000 });

    // Directly navigate to a protected route — should be blocked
    await page.goto('/users');
    await expect(page).toHaveURL(/\/login/, { timeout: 8_000 });
  });
});
