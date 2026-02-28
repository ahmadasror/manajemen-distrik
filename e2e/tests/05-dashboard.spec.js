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
    // Use saved admin state so we don't log in each test
    await page.goto('/login');
    await page.getByPlaceholder('Username').fill('admin');
    await page.getByPlaceholder('Password').fill('admin123');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await page.waitForURL('**/dashboard');

    // Four stat cards must be visible
    const statCards = page.locator('.ant-statistic, [class*="statistic"], .ant-card');
    await expect(statCards.first()).toBeVisible({ timeout: 8_000 });

    // The dashboard should show at least these four labels (scope to main to avoid sidebar matches)
    const main = page.locator('main, [role="main"], .ant-layout-content').first();
    await expect(main.getByText(/total users/i)).toBeVisible();
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

    // Statistics cards must render for a Viewer too
    await expect(page.locator('.ant-statistic, .ant-card').first()).toBeVisible({ timeout: 8_000 });
  });
});

// ── Negative Cases ────────────────────────────────────────────────────────

test.describe('5. Dashboard — Negative', () => {
  test('5.3 User accesses dashboard without logging in', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login/, { timeout: 8_000 });
  });
});
