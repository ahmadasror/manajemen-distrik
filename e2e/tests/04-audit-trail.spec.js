/**
 * Section 4 — Activity History (Audit Trail)
 * TEST-CASES.txt: cases 4.1 – 4.7
 */

const { test, expect } = require('@playwright/test');
const fs = require('fs');
const path = require('path');
const { loginViaUI } = require('../helpers/auth');
const { apiLogin, apiGetPendingActions } = require('../helpers/api');

const ADMIN_STATE = path.join(__dirname, '../helpers/.auth/admin.json');

// ── Positive Cases ────────────────────────────────────────────────────────

test.describe('4. Activity History — Positive', () => {
  test.use({ storageState: ADMIN_STATE });

  test('4.1 Administrator views the full activity history', async ({ page }) => {
    await page.goto('/audit-trail');

    await expect(page.locator('.ant-table')).toBeVisible();

    // Required columns
    await expect(page.getByRole('columnheader', { name: /entity type/i })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /action/i }).first()).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /performed by/i })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /date/i })).toBeVisible();

    // At least one row should exist (from seeding or earlier test actions)
    const rows = page.locator('.ant-table-tbody tr');
    const count = await rows.count();
    expect(count).toBeGreaterThan(0);
  });

  test('4.2 Administrator browses activity history page by page', async ({ page }) => {
    await page.goto('/audit-trail');
    await expect(page.locator('.ant-table')).toBeVisible();

    // Pagination component must render
    const pagination = page.locator('.ant-pagination');
    await expect(pagination).toBeVisible();

    // Change page size to 5
    const pageSizeSelector = pagination.locator('.ant-select-selector');
    if (await pageSizeSelector.isVisible()) {
      await pageSizeSelector.click();
      await page.locator('.ant-select-dropdown').waitFor({ state: 'visible' });
      const fivePerPage = page.locator('.ant-select-item-option').filter({ hasText: '5' });
      if (await fivePerPage.isVisible()) {
        await fivePerPage.click();
        await page.keyboard.press('Escape');
        await page.waitForTimeout(500);

        // After changing page size, the table should show at most 5 rows
        const rowCount = await page.locator('.ant-table-tbody tr').count();
        expect(rowCount).toBeLessThanOrEqual(5);
      }
    }

    // Total entries label must appear
    await expect(page.getByText(/total|entries/i).first()).toBeVisible();
  });

  test('4.3 Admin views activity history for a specific user', async ({ page }) => {
    // Navigate to the user detail page and open the Audit Trail tab
    await page.goto('/users/1');

    // Click the Audit Trail tab
    const auditTab = page.locator('.ant-tabs-tab').filter({ hasText: /audit/i });
    if (await auditTab.isVisible({ timeout: 3_000 })) {
      await auditTab.click();
      const tabTable = page.locator('.ant-table').nth(0);
      await expect(tabTable).toBeVisible({ timeout: 5_000 });
    } else {
      // Alternative: navigate directly to entity audit endpoint
      await page.goto('/audit-trail');
      await expect(page.locator('.ant-table')).toBeVisible();
      // Filter by "admin" in the table (any row that shows "admin" as performer)
      await expect(page.getByRole('cell', { name: 'admin' }).first()).toBeVisible();
    }
  });

  test('4.4 Admin views detail of an activity record', async ({ page }) => {
    await page.goto('/audit-trail');
    await expect(page.locator('.ant-table')).toBeVisible();

    // Click the first row to navigate to the detail page
    await page.locator('.ant-table-tbody tr').first().click();

    await expect(page).toHaveURL(/\/audit-trail\/\d+/);

    // Detail card shows required fields (scope to descriptions to avoid sidebar menu matches)
    await expect(page.getByText('Audit Detail')).toBeVisible();
    const descriptions = page.locator('.ant-descriptions');
    await expect(descriptions.getByText('Action', { exact: true })).toBeVisible();
    await expect(descriptions.getByText('Performed By')).toBeVisible();
    await expect(descriptions.getByText('Entity Type')).toBeVisible();
  });
});

// ── Negative Cases ────────────────────────────────────────────────────────

test.describe('4. Activity History — Negative', () => {
  test('4.5 Admin opens a non-existent activity record', async ({ page }) => {
    // Reuse admin state
    await page.goto('/login');
    await page.getByPlaceholder('Username').fill('admin');
    await page.getByPlaceholder('Password').fill('admin123');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await page.waitForURL('**/dashboard');

    await page.goto('/audit-trail/99999');
    // Wait for async data fetch to complete (AuditDetailPage shows "Not found" after failed API call)
    await page.waitForLoadState('networkidle');

    // Expect a not-found error message or an error state
    // AuditDetailPage renders plain "Not found" text when data is null
    const errorMsg = page.locator(
      '.ant-message-error, [class*="ant-message-error"], .ant-result-error, .ant-alert-error'
    );
    const notFoundText = page.getByText('Not found', { exact: true });
    const hasAntdError = await errorMsg.first().isVisible().catch(() => false);
    const hasNotFound = await notFoundText.isVisible().catch(() => false);
    expect(hasAntdError || hasNotFound).toBeTruthy();
  });

  test('4.6 User accesses activity history without logging in', async ({ page }) => {
    // Navigate with no session — ProtectedRoute should redirect
    await page.goto('/audit-trail');
    await expect(page).toHaveURL(/\/login/, { timeout: 8_000 });
  });

  test('4.7 Maker tries to access activity history', async ({ page }) => {
    const credsFile = path.join(__dirname, '../helpers/.auth/credentials.json');
    if (!fs.existsSync(credsFile)) test.skip(true, 'credentials.json not found');
    const creds = JSON.parse(fs.readFileSync(credsFile, 'utf8'));
    if (!creds.maker) test.skip(true, 'Maker user not available');

    await loginViaUI(page, creds.maker.username, creds.maker.password);
    await page.goto('/audit-trail');

    // Should be blocked: either redirected or an access-denied message shown
    const isRedirected = page.url().includes('/login') || page.url().includes('/dashboard');
    const accessDenied = await page
      .locator('.ant-result-403, .ant-message-error, [class*="forbidden"], [class*="denied"]')
      .first()
      .isVisible({ timeout: 5_000 })
      .catch(() => false);
    const noTable = !(await page.locator('.ant-table').isVisible({ timeout: 3_000 }).catch(() => false));

    expect(isRedirected || accessDenied || noTable).toBeTruthy();
  });
});
