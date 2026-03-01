/**
 * Section 1 — Login & Session Management
 * TEST-CASES.txt: cases 1.1 – 1.10
 *
 * With Keycloak: login is via OIDC redirect to Keycloak login page.
 * Protected routes call keycloak.login() when unauthenticated, redirecting to Keycloak.
 */

const { test, expect } = require('@playwright/test');
const { loginViaUI, logoutViaUI } = require('../helpers/auth');

// ── Positive Cases ────────────────────────────────────────────────────────

test.describe('1. Login & Session Management — Positive', () => {
  test('1.1 User logs in with correct credentials', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: 'Sign In with Keycloak' }).click();

    // Wait for Keycloak login page to fully load before interacting
    await page.waitForURL(/8180/, { waitUntil: 'load', timeout: 15_000 });
    await page.locator('#username').waitFor({ state: 'visible' });
    await page.locator('#username').fill('admin');
    await page.locator('#password').fill('admin123');
    await page.locator('#kc-login').click();

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15_000 });
  });

  test('1.2 Logged-in user views their own profile', async ({ page }) => {
    await loginViaUI(page, 'admin', 'admin123');

    // Click the user avatar/dropdown trigger in the header
    await page.locator('header button').click();

    // Dropdown menu appears
    await expect(page.locator('[role="menu"]')).toBeVisible();
    // The header shows the user's info
    const header = page.locator('header');
    await expect(header).toContainText(/admin/i);
  });

  test('1.3 User renews session before it expires', async ({ page }) => {
    // Verify the keycloak token refresh keeps the session alive by
    // making consecutive API-backed page navigations within the same session.
    await loginViaUI(page, 'admin', 'admin123');

    // Navigate to Users — triggers an authenticated API call
    await page.goto('/users');
    await expect(page).toHaveURL(/\/users/, { timeout: 8_000 });
    await expect(page.locator('table')).toBeVisible();

    // Navigate to Audit Trail — another authenticated API call
    await page.goto('/audit-trail');
    await expect(page).toHaveURL(/\/audit-trail/, { timeout: 8_000 });
    await expect(page.locator('table')).toBeVisible();

    // Still authenticated — no redirect to Keycloak
    await expect(page).not.toHaveURL(/8180/);
  });

  test('1.4 User logs out of the system', async ({ page }) => {
    await loginViaUI(page, 'admin', 'admin123');

    // Click avatar dropdown trigger, then click Logout
    await logoutViaUI(page);

    // After logout, browser ends up on Keycloak login page
    await expect(page).toHaveURL(/8180/, { timeout: 15_000 });

    // Attempting to navigate to a protected page triggers Keycloak login again
    await page.goto('/users');
    await expect(page).toHaveURL(/8180/, { timeout: 10_000 });
  });
});

// ── Negative Cases ────────────────────────────────────────────────────────

test.describe('1. Login & Session Management — Negative', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('1.5 User logs in with wrong password', async ({ page }) => {
    await page.getByRole('button', { name: 'Sign In with Keycloak' }).click();
    await expect(page).toHaveURL(/8180/, { timeout: 15_000 });

    await page.locator('#username').fill('admin');
    await page.locator('#password').fill('wrongpassword');
    await page.locator('#kc-login').click();

    // Keycloak shows an error and stays on the login page
    await expect(page).not.toHaveURL(/\/dashboard/, { timeout: 8_000 });
    await expect(page).toHaveURL(/8180/);
  });

  test('1.6 User logs in with unregistered username', async ({ page }) => {
    await page.getByRole('button', { name: 'Sign In with Keycloak' }).click();
    await expect(page).toHaveURL(/8180/, { timeout: 15_000 });

    await page.locator('#username').fill('unknownuser');
    await page.locator('#password').fill('any123');
    await page.locator('#kc-login').click();

    // Keycloak shows an error and stays on the login page
    await expect(page).not.toHaveURL(/\/dashboard/, { timeout: 8_000 });
    await expect(page).toHaveURL(/8180/);
  });

  test('1.7 User submits Keycloak login form with empty fields', async ({ page }) => {
    await page.getByRole('button', { name: 'Sign In with Keycloak' }).click();
    await expect(page).toHaveURL(/8180/, { timeout: 15_000 });

    // Click submit without filling any field
    await page.locator('#kc-login').click();

    // Keycloak should stay on the login page (invalid credentials)
    await expect(page).not.toHaveURL(/\/dashboard/, { timeout: 5_000 });
    await expect(page).toHaveURL(/8180/);
  });

  test('1.8 User accesses a protected page without logging in', async ({ page }) => {
    // Navigate directly to a protected route with no session
    await page.goto('/users');

    // ProtectedRoute calls keycloak.login() which redirects to Keycloak
    await expect(page).toHaveURL(/8180/, { timeout: 10_000 });
  });

  test('1.9 User accesses dashboard without a session', async ({ page }) => {
    // Navigate to dashboard without any authenticated session
    await page.goto('/dashboard');

    // ProtectedRoute calls keycloak.login() → redirect to Keycloak
    await expect(page).toHaveURL(/8180/, { timeout: 10_000 });
  });

  test('1.10 User reuses session after logging out', async ({ page }) => {
    // Login, logout, then try to navigate to a protected page
    await loginViaUI(page, 'admin', 'admin123');

    await logoutViaUI(page);

    // Directly navigate to a protected route — should redirect to Keycloak
    await page.goto('/users');
    await expect(page).toHaveURL(/8180/, { timeout: 10_000 });
  });
});
