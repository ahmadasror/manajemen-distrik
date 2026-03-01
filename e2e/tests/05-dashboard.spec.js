/**
 * Section 5 — Dashboard
 * TEST-CASES.txt: cases 5.1 – 5.3
 */

const { test, expect } = require('@playwright/test');
const fs = require('fs');
const path = require('path');
const { loginViaUI } = require('../helpers/auth');

const ADMIN_STATE = path.join(__dirname, '../helpers/.auth/admin.json');

// ── Positive Cases ────────────────────────────────────────────────────────

test.describe('5. Dashboard — Positive', () => {
  test('5.1 Administrator views dashboard statistics', async ({ page }) => {
    // Login via Keycloak
    await page.goto('/login');
    await page.getByRole('button', { name: 'Sign In with Keycloak' }).click();
    await page.waitForURL(/8180/, { waitUntil: 'load', timeout: 15_000 });
    await page.locator('#username').waitFor({ state: 'visible' });
    await page.locator('#username').fill('admin');
    await page.locator('#password').fill('admin123');
    await page.locator('#kc-login').click();
    await page.waitForURL('**/dashboard', { timeout: 15_000 });

    // The dashboard should show at least these four stat card labels
    const main = page.locator('main').first();
    await expect(main.getByText(/total users/i)).toBeVisible({ timeout: 8_000 });
    await expect(main.getByText(/active users/i)).toBeVisible();
    await expect(main.getByText(/pending actions/i)).toBeVisible();
    await expect(main.getByText(/audit/i).first()).toBeVisible();
  });

  test('5.2 Viewer views dashboard statistics', async ({ page }) => {
    const credsFile = path.join(__dirname, '../helpers/.auth/credentials.json');
    if (!fs.existsSync(credsFile)) test.skip(true, 'credentials.json not found');
    const creds = JSON.parse(fs.readFileSync(credsFile, 'utf8'));
    if (!creds.viewer) test.skip(true, 'Viewer user not available — approve via a second admin first');

    await loginViaUI(page, creds.viewer.username, creds.viewer.password);
    await expect(page).toHaveURL(/\/dashboard/);

    // Statistics must render for a Viewer too
    const main = page.locator('main').first();
    await expect(main.getByText(/total users/i)).toBeVisible({ timeout: 8_000 });
  });
});

// ── Negative Cases ────────────────────────────────────────────────────────

test.describe('5. Dashboard — Negative', () => {
  test('5.3 User accesses dashboard without logging in', async ({ page }) => {
    await page.goto('/dashboard');
    // ProtectedRoute calls keycloak.login() → redirect to Keycloak
    await expect(page).toHaveURL(/8180/, { timeout: 10_000 });
  });
});
