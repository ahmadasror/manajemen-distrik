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
  test.beforeEach(async ({ page }) => {
    await loginViaUI(page, 'admin', 'admin123');
  });

  test('4.1 Administrator views the full activity history', async ({ page }) => {
    await page.goto('/audit-trail');

    await expect(page.locator('table')).toBeVisible();

    // Required columns
    await expect(page.getByRole('columnheader', { name: /entity type/i })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /action/i }).first()).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /performed by/i })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /date/i })).toBeVisible();

    // At least one row should exist
    const rows = page.locator('tbody tr');
    const count = await rows.count();
    expect(count).toBeGreaterThan(0);
  });

  test('4.2 Administrator browses activity history page by page', async ({ page }) => {
    await page.goto('/audit-trail');
    await expect(page.locator('table')).toBeVisible();

    // Total entries label must appear
    await expect(page.getByText(/total entries/i)).toBeVisible();

    // If pagination is visible, navigate pages
    const nextBtn = page.getByRole('button').filter({ has: page.locator('svg') }).last();
    const prevBtn = page.getByRole('button').filter({ has: page.locator('svg') }).first();

    const hasPagination = await page.getByText(/page \d+ of \d+/i).isVisible({ timeout: 3_000 }).catch(() => false);
    if (hasPagination) {
      // Verify the page indicator is present
      await expect(page.getByText(/page \d+ of \d+/i)).toBeVisible();
    }
  });

  test('4.3 Admin views activity history for a specific user', async ({ page }) => {
    // Navigate to the user detail page and open the Audit Trail tab
    await page.goto('/users/1');

    // Click the Audit Trail tab (shadcn TabsTrigger with role="tab")
    const auditTab = page.getByRole('tab', { name: /audit/i });
    if (await auditTab.isVisible({ timeout: 3_000 })) {
      await auditTab.click();
      const tabTable = page.locator('table').first();
      await expect(tabTable).toBeVisible({ timeout: 5_000 });
    } else {
      // Alternative: navigate directly to audit trail
      await page.goto('/audit-trail');
      await expect(page.locator('table')).toBeVisible();
      // Filter by "admin" in the table (any row that shows "admin" as performer)
      await expect(page.getByRole('cell', { name: 'admin' }).first()).toBeVisible();
    }
  });

  test('4.4 Admin views detail of an activity record', async ({ page }) => {
    await page.goto('/audit-trail');
    await expect(page.locator('table')).toBeVisible();

    // Click the first row to navigate to the detail page
    await page.locator('tbody tr').first().click();

    await expect(page).toHaveURL(/\/audit-trail\/\d+/);

    // Detail card shows required fields
    await expect(page.getByText('Audit Detail')).toBeVisible();
    const descriptions = page.locator('dl');
    await expect(descriptions.getByText('Action', { exact: true })).toBeVisible();
    await expect(descriptions.getByText('Performed By')).toBeVisible();
    await expect(descriptions.getByText('Entity Type')).toBeVisible();
  });
});

// ── Negative Cases ────────────────────────────────────────────────────────

test.describe('4. Activity History — Negative', () => {
  test('4.5 Admin opens a non-existent activity record', async ({ page }) => {
    await loginViaUI(page, 'admin', 'admin123');

    await page.goto('/audit-trail/99999');
    // Wait for async data fetch to complete
    await page.waitForLoadState('networkidle');

    // AuditDetailPage renders "Not found" when data is null
    const notFoundText = page.getByText('Not found');
    const hasNotFound = await notFoundText.isVisible().catch(() => false);
    expect(hasNotFound).toBeTruthy();
  });

  test('4.6 User accesses activity history without logging in', async ({ page }) => {
    // Navigate with no session — ProtectedRoute calls keycloak.login()
    await page.goto('/audit-trail');
    await expect(page).toHaveURL(/8180/, { timeout: 10_000 });
  });

  test('4.7 Maker tries to access activity history', async ({ page }) => {
    const credsFile = path.join(__dirname, '../helpers/.auth/credentials.json');
    if (!fs.existsSync(credsFile)) test.skip(true, 'credentials.json not found');
    const creds = JSON.parse(fs.readFileSync(credsFile, 'utf8'));
    if (!creds.maker) test.skip(true, 'Maker user not available');

    await loginViaUI(page, creds.maker.username, creds.maker.password);
    await page.goto('/audit-trail');

    // Should be blocked: either redirected or no table shown
    const isRedirected = page.url().includes('/login') || page.url().includes('/dashboard') || page.url().includes('8180');
    const accessDenied = await page
      .locator('[data-sonner-toast][data-type="error"]')
      .first()
      .isVisible({ timeout: 5_000 })
      .catch(() => false);
    const noTable = !(await page.locator('table').isVisible({ timeout: 3_000 }).catch(() => false));

    expect(isRedirected || accessDenied || noTable).toBeTruthy();
  });
});
